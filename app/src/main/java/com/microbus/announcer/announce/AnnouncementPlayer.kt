package com.microbus.announcer.announce

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.AnMediaItem
import com.microbus.announcer.bean.AudioSourceType
import com.microbus.announcer.bean.MediaItemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 播报播放器（基于 ExoPlayer3）。
 *
 * 调度模型：
 *  - 两条队列：[interruptQueue]（打断优先）与 [appendQueue]（追加，等 INTERRUPT 播完）。
 *  - 唯一调度协程 [dispatcherJob] 串行执行 [dispatchLoop]，每次调用 [runOneStage] 推进一步。
 *  - 推进信号：
 *      1) [play] 被调用时 [requestDispatch]；
 *      2) ExoPlayer 触发 [Player.STATE_ENDED] 时 [requestDispatch]。
 *  - 阶段顺序（严格）：
 *      INTERRUPT 队列 → （player 内 INTERRUPT 项播完）→ APPEND 队列 → （player 内 APPEND 项播完）→ Idle + 完成回调。
 *  - INTERRUPT 队列非空时，APPEND 绝不灌入 player。
 *
 * 元素元信息：
 *  - [playerItemMeta] 保存 player 中每个 mediaId 对应的「原始 AnMediaItem + 所属流」，
 *    使得 INTERRUPT 打断时能把未播的 APPEND 项原样回收回 [appendQueue]（保留 source / status）。
 */
class AnnouncementPlayer @OptIn(UnstableApi::class)
private constructor(val context: Context) {

    private var utils = Utils(context)

    private var audioFocusRequest: AudioFocusRequest
    private val audioManager: AudioManager
    private var player: ExoPlayer
    private var audioSessionId: Int? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    val anResolver = AnnouncementProvider.getInstance(context)

    // ===== 双队列模型 =====
    private val interruptQueue = ArrayDeque<AnMediaItem>()
    private val appendQueue = ArrayDeque<AnMediaItem>()

    /** 所属流类型 */
    private enum class Owner { INTERRUPT, APPEND }

    /**
     * player 内每个 mediaId 对应的原始 item + 所属流。
     * 用于：
     *  1) INTERRUPT 打断时回收未播的 APPEND 项（保留 source/status）；
     *  2) 判断 player 里是否还有 INTERRUPT/APPEND 的项在播/待播。
     */
    private val playerItemMeta = mutableMapOf<String, Pair<AnMediaItem, Owner>>()

    /** 唯一的调度协程 */
    private var dispatcherJob: Job? = null

    /** 有新的调度需求，当前 dispatcher 结束后应再跑一轮 */
    private var pendingKick = false

    /** 调度协程作用域：用 Main，所有对 player 的写操作都在 Main 上 */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val ttsTimeoutMs = 30_000L
    private val netWorkTimeoutMs = 15_000L

    /** 外部可用来订阅当前正在播放的字幕文本 */
    val playingText: StateFlow<String>
        field = MutableStateFlow("")

    /** 开始播放的系统时间（毫秒，SystemClock.elapsedRealtime），未播放时为 0 */
    val playbackStartTime: StateFlow<Long>
        field = MutableStateFlow(0L)

    /** 当前媒体总时长（毫秒），未知时为 C.TIME_UNSET */
    val playbackDuration: StateFlow<Long>
        field = MutableStateFlow(0L)

    init {

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { /* 保持原有空实现 */ }
                .build()

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val exoAudioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 50000,
                /* maxBufferMs = */ 60000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 5000
            )
            .build()

        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(exoAudioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {

                    Player.STATE_READY -> {
                        val dur = player.duration
                        if (dur != C.TIME_UNSET) playbackDuration.value = dur
                        playbackStartTime.value = SystemClock.elapsedRealtime()

                        if (utils.getAnSubtitle()) showAnSubtitle()
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(Intent().setAction(utils.ON_PLAYER_STATE_READY))
                    }

                    Player.STATE_ENDED -> {
                        Log.d("L1946", "STATE_ENDED")
                        Log.d(
                            "L144",
                            "interruptQueue=${interruptQueue.size}, " +
                                    "appendQueue=${appendQueue.size}, " +
                                    "playerItems=${player.mediaItemCount}"
                        )
                        // 清理已播完的元信息：player 里不再存在的 mediaId
                        prunePlayerItemMeta()

                        // 若两条队列都空，说明不会再有任何后续播报，手动停止播报
                        if (interruptQueue.isEmpty() && appendQueue.isEmpty()) {
                            stopAnnounce()
                        }

                        // 交给调度器推进：INTERRUPT 播完 → 尝试 APPEND → 或收尾
                        requestDispatch()
                    }

                    Player.STATE_BUFFERING -> {}
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val dur = player.duration
                if (dur != C.TIME_UNSET) playbackDuration.value = dur
                playbackStartTime.value = SystemClock.elapsedRealtime()

                if (utils.getAnSubtitle()) {
                    if (reason == 3) return
                    if (player.currentMediaItemIndex >= player.mediaItemCount) return
                    showAnSubtitle()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("L1946", "播放异常: ${error.message}", error.cause)
                utils.showMsg("播放异常: ${error.message}")
                stopAnnounce()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                this@AnnouncementPlayer.audioSessionId = audioSessionId
                setTargetGain()
            }
        })

        player.preloadConfiguration = ExoPlayer.PreloadConfiguration(30_000_000L)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AnnouncementPlayer? = null

        fun getInstance(context: Context): AnnouncementPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnnouncementPlayer(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    enum class PlayMode { INTERRUPT, APPEND }

    // =========================================================================================
    // 公共 API
    // =========================================================================================

    /**
     * 播放音频。
     *  - INTERRUPT：清空 interruptQueue（保留 appendQueue），打断当前播放，重新灌入。
     *  - APPEND：仅追加到 appendQueue，等 INTERRUPT 播完后再播。
     */
    @OptIn(UnstableApi::class)
    fun play(mediaItemList: MutableList<AnMediaItem>, mode: PlayMode = PlayMode.INTERRUPT) {
        mediaItemList.forEach { Log.d("L176", it.text ?: "") }

        when (mode) {
            PlayMode.INTERRUPT -> {
                // 1) 清空 INTERRUPT 队列，装入新项
                interruptQueue.clear()
                interruptQueue.addAll(mediaItemList)

                // 2) 在调度协程内串行处理：取消旧调度 → 回收 APPEND → 清空 player → 重新灌
                scope.launch {
                    val old = dispatcherJob
                    dispatcherJob = null
                    old?.cancelAndJoin()

                    recycleUnplayedAppendItemsToQueue()

                    player.stop()
                    player.clearMediaItems()
                    playerItemMeta.clear()
                    playingText.value = ""
                    playbackStartTime.value = 0L

                    requestDispatch()
                }
            }

            PlayMode.APPEND -> {
                appendQueue.addAll(mediaItemList)
                // 不打断任何东西；若 INTERRUPT 还在播/待播，runOneStage 会自行 return
                requestDispatch()
            }
        }
    }

    /** 对外：停止一切（含两路队列 + 调度器） */
    fun stopAnnounce(cancelPlayJob: Boolean = true): Job {
        return scope.launch {
            stopAnnounceInternal(clearQueues = true, cancelPlayJob = cancelPlayJob)
        }
    }

    // =========================================================================================
    // 调度核心
    // =========================================================================================

    /**
     * 请求一次调度。若已有 dispatcher 在跑，只置 [pendingKick]，
     * 由 dispatcherLoop 结束当前阶段后再跑一轮，保证串行。
     */
    private fun requestDispatch() {
        if (dispatcherJob?.isActive == true) {
            pendingKick = true
            return
        }
        dispatcherJob = scope.launch {
            try {
                do {
                    pendingKick = false
                    runOneStage()
                } while (pendingKick)
            } finally {
                dispatcherJob = null
            }
        }
    }

    /**
     * 推进一个阶段。阶段顺序：
     *  1) INTERRUPT 队列非空 → 灌满 INTERRUPT，然后 return（等 ENDED）
     *  2) player 里仍有 INTERRUPT 项 → return（等 ENDED）
     *  3) APPEND 队列非空 → 灌满 APPEND，然后 return（等 ENDED）
     *  4) player 里仍有 APPEND 项 → return（等 ENDED）
     *  5) 全空 → 收尾
     */
    private suspend fun runOneStage() {

        Log.d("L315", "runOneStage")

        if (interruptQueue.isNotEmpty()) {
            pumpQueue(interruptQueue, Owner.INTERRUPT)
            return
        }

        if (playerHasOwnedItems(Owner.INTERRUPT)) {
            Log.d("L324", "playerHasOwnedItems INTERRUPT")
            // INTERRUPT 的项还在 player 里播放/待播，等 ENDED 再推进
            return
        }

        if (appendQueue.isNotEmpty()) {
            Log.d("L324", "appendQueue.isNotEmpty")
            pumpQueue(appendQueue, Owner.APPEND)
            return
        }

        if (playerHasOwnedItems(Owner.APPEND)) {
            // APPEND 的项还在 player 里播放/待播，等 ENDED 再推进
            return
        }

        // 全部播完
        finishIfIdle()
    }

    /** 把队列里的项依次 prepare 后灌入 player */
    private suspend fun pumpQueue(queue: ArrayDeque<AnMediaItem>, owner: Owner) {
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val item = queue.removeFirst()
            if (!prepareItem(item)) {
                // 被 cancel
                return
            }
            enqueueItemToPlayer(item, owner)
        }
    }

    /**
     * 把已准备好的 item 追加到 player 队尾并 kick 播放。
     * 注意：一律用 append 语义（加到队尾），保证顺序；是否打断由调用者决定。
     */
    private suspend fun enqueueItemToPlayer(item: AnMediaItem, owner: Owner) =
        withContext(Dispatchers.Main) {
            Log.d("L201", "play ${item.filePath}")
            val exoItem = MediaItem.Builder()
                .setUri(item.filePath)
                .setMediaId(item.filePath)
                .build()

            // 记录元信息（用 mediaId 作为 key；若 filePath 重复则以后写入的为准）
            playerItemMeta[item.filePath] = item to owner

            val wasEmpty = player.mediaItemCount == 0
            player.addMediaItem(exoItem)

            audioManager.requestAudioFocus(audioFocusRequest)

            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            if (wasEmpty) {
                player.seekTo(0, 0L)
            }
            player.play()
        }

    /** player 里是否还有属于指定流的项（未播完） */
    private fun playerHasOwnedItems(owner: Owner): Boolean {
        if (player.mediaItemCount == 0) return false
        // playerItemMeta 只保存当前 player 内的项，key 唯一即可
        return playerItemMeta.values.any { it.second == owner }
    }

    /**
     * 清理 [playerItemMeta]：只保留 player 中当前仍存在的 mediaId。
     * 由于 mediaId == filePath 可能重复，这里用 mediaId 集合近似：
     * 对每个 meta key，如果 player 中不存在该 mediaId，则移除。
     */
    private fun prunePlayerItemMeta() {
        if (playerItemMeta.isEmpty()) return
        val aliveIds = HashSet<String>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            aliveIds.add(player.getMediaItemAt(i).mediaId)
        }
        playerItemMeta.keys.retainAll(aliveIds)
    }

    // =========================================================================================
    // 准备（TTS / NETWORK）
    // =========================================================================================

    /**
     * 等待 TTS / 网络下载完成，返回 false 表示被 cancel。
     */
    private suspend fun prepareItem(item: AnMediaItem): Boolean {
        // ---- 等待 TTS ----
        if (item.source == AudioSourceType.TTS) {
            if (!currentCoroutineContext().isActive) return false
            waitTTSDoneWithTimeoutToBlank(item)
        }

        // ---- 等待网络下载（含超时回退 TTS） ----
        if (item.source == AudioSourceType.NETWORK) {
            if (!currentCoroutineContext().isActive) return false

            val start = SystemClock.elapsedRealtime()
            var isTimeout = false
            while (!anResolver.hasNetworkVoiceDone(item.filePath)) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() - start > netWorkTimeoutMs) {
                    withContext(Dispatchers.Main) { utils.showMsg("GET音频超时：${item.text}") }
                    isTimeout = true
                    break
                }
                delay(50.milliseconds)
            }

            if (isTimeout || item.status == MediaItemStatus.FAILED) {
                Log.d("L223", "GET音频超时或失败回退")
                item.source = AudioSourceType.TTS
                item.status = MediaItemStatus.LOADING
                val ttsFilePath = anResolver.buildTTSMedia(item.text ?: "")
                item.filePath = ttsFilePath
                if (anResolver.hasTTSDone(ttsFilePath)) {
                    item.status = MediaItemStatus.COMPLETED
                }
                waitTTSDoneWithTimeoutToBlank(item)
            }
        }
        return true
    }

    private suspend fun waitTTSDoneWithTimeoutToBlank(mediaItem: AnMediaItem) {
        val start = SystemClock.elapsedRealtime()
        while (mediaItem.status != MediaItemStatus.COMPLETED) {
            currentCoroutineContext().ensureActive()
            if (SystemClock.elapsedRealtime() - start > ttsTimeoutMs) {
                withContext(Dispatchers.Main) { utils.showMsg("TTS合成超时") }
                // 超时回退
                mediaItem.source = AudioSourceType.BLANK
                val blankFilePath = anResolver.buildBlankMedia("/blank/10.pcm")
                mediaItem.filePath = blankFilePath
                mediaItem.status = MediaItemStatus.COMPLETED
                return
            }
            delay(50.milliseconds)
        }
    }

    // =========================================================================================
    // 打断前的回收
    // =========================================================================================

    /**
     * 在打断前，把 player 中「属于 APPEND 且尚未播放」的项回收回 [appendQueue] 队首，
     * 保证 INTERRUPT 播完后这些项还能继续播。
     *
     * 使用 [playerItemMeta] 取原始 AnMediaItem，保留 source / status / networkInfo。
     *
     * 判定「尚未播放」：
     *  - 当前项之前的项：一定已播完，跳过；
     *  - 当前项：currentPosition > 0 视为已开始，不回收；否则回收；
     *  - 当前项之后的项：一定未播，全部回收。
     */
    private fun recycleUnplayedAppendItemsToQueue() {
        val count = player.mediaItemCount
        if (count == 0) return

        val currentIndex = player.currentMediaItemIndex
        val currentPos = if (currentIndex in 0 until count) player.currentPosition else 0L

        val toRecycle = mutableListOf<AnMediaItem>()

        for (i in 0 until count) {
            if (i < currentIndex) continue   // 已播完
            if (i == currentIndex) {
                val started = currentPos > 0L && currentPos != C.TIME_UNSET
                if (started) continue
            }
            val mi = player.getMediaItemAt(i)
            val meta = playerItemMeta[mi.mediaId] ?: continue
            val (item, owner) = meta
            // 只回收 APPEND 流；INTERRUPT 流直接丢弃（因为已被新 INTERRUPT 取代）
            if (owner == Owner.APPEND) {
                toRecycle.add(item)
            }
        }

        // 逆序 addFirst，保持原顺序
        for (i in toRecycle.indices.reversed()) {
            appendQueue.addFirst(toRecycle[i])
        }
    }

    // =========================================================================================
    // 收尾 / 停止
    // =========================================================================================

    private fun finishIfIdle() {
        // 只有 player 彻底空、两条队列都空时才收尾
        if (interruptQueue.isNotEmpty() || appendQueue.isNotEmpty()) return
        if (player.mediaItemCount != 0) return

        Log.d("L308", "finishIfIdle")
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        playingText.value = ""
        playbackStartTime.value = 0L
        playerItemMeta.clear()

        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(Intent().setAction(utils.ON_PLAYER_STATE_ENDED))
    }

    private suspend fun stopAnnounceInternal(
        clearQueues: Boolean,
        cancelPlayJob: Boolean = true
    ) {
        Log.d("L308", "stopAnnounce")

        if (clearQueues) {
            interruptQueue.clear()
            appendQueue.clear()
            playerItemMeta.clear()
            if (cancelPlayJob) {
                val old = dispatcherJob
                dispatcherJob = null
                old?.cancelAndJoin()
            }
        }

        Log.d("L201", "abandonAudioFocus")
        audioManager.abandonAudioFocusRequest(audioFocusRequest)

        player.stop()
        player.clearMediaItems()

        playingText.value = ""
        playbackStartTime.value = 0L

        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(Intent().setAction(utils.ON_PLAYER_STOP))
    }

    // =========================================================================================
    // 字幕 / 增益
    // =========================================================================================

    fun showAnSubtitle() {
        val mediaItem = player.currentMediaItem ?: return

        val path = mediaItem.mediaId
        // blank 播报间隔：不显示字幕
        if (Regex("^.*/blank/[^/]+\\.pcm$").matches(path)) return

        val fileName = path.split('/').last()
        val lastDotIndex = fileName.lastIndexOf(".")
        val text = fileName.take(lastDotIndex)

        utils.showMsg(text, true)
        playingText.value = text
    }

    fun setTargetGain() {
        val sessionId = audioSessionId ?: return
        val enhancer = LoudnessEnhancer(sessionId)
        enhancer.setTargetGain(utils.getLoudnessBoostAmount()) // 毫分贝
        enhancer.enabled = true
        this@AnnouncementPlayer.loudnessEnhancer = enhancer
    }
}