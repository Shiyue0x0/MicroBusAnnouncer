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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 播报播放器（基于 AndroidX Media3 ExoPlayer）。
 *
 * 调度规则：
 * 1. INTERRUPT 和 APPEND 使用独立队列。
 * 2. INTERRUPT 始终优先；INTERRUPT 队列清空后才处理 APPEND。
 * 3. 新的 INTERRUPT 会清空旧 INTERRUPT 队列并打断当前任务。
 * 4. APPEND 按每次 play() 调用作为一个批次保存。
 * 5. 如果 INTERRUPT 打断正在处理的 APPEND，整个 APPEND mediaItemList 会重新放回队首，恢复时从头播放。
 * 6. 每次只向 ExoPlayer 提交一个 AnMediaItem，由唯一调度协程负责串行调度。
 */
class AnnouncementPlayer @OptIn(UnstableApi::class)
private constructor(val context: Context) {

    private val utils = Utils(context)

    private var audioFocusRequest: AudioFocusRequest
    private val audioManager: AudioManager
    private var player: ExoPlayer
    private var audioSessionId: Int? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    val anResolver = AnnouncementProvider.getInstance(context)

    /** 每次 play() 对应一个独立批次，便于精确执行播放超时清理。 */
    private class PlayBatch(val items: List<AnMediaItem>) {
        var started: Boolean = false
        var timeoutJob: Job? = null
    }

    private val interruptQueue = ArrayDeque<PlayBatch>()
    private val appendQueue = ArrayDeque<PlayBatch>()

    private var activeInterruptBatch: PlayBatch? = null
    private var activeInterruptIndex: Int = 0

    private var activeAppendBatch: PlayBatch? = null
    private var activeAppendIndex: Int = 0

    private enum class Owner {
        INTERRUPT,
        APPEND
    }

    /** 当前正在准备或播放的项目及其来源队列。 */
    private var currentItem: AnMediaItem? = null
    private var currentOwner: Owner? = null
    private var currentBatch: PlayBatch? = null

    /** 唯一调度协程。 */
    private var dispatcherJob: Job? = null

    /** 当前 ExoPlayer 单项播放的完成等待器。 */
    private var playbackWaiter: CancellableContinuation<Unit>? = null

    /** 所有队列和 ExoPlayer 操作均由 Main 线程上的 scope 执行。 */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val ttsTimeoutMs = 30_000L
    private val netWorkTimeoutMs = 15_000L

    /** 外部可用来订阅当前正在播放的字幕文本。 */
    private val _playingText = MutableStateFlow("")
    val playingText: StateFlow<String> = _playingText

    /** 开始播放的系统时间（毫秒），未播放时为 0。 */
    private val _playbackStartTime = MutableStateFlow(0L)
    val playbackStartTime: StateFlow<Long> = _playbackStartTime

    /** 当前媒体总时长（毫秒），未知时为 0。 */
    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration

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

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    currentBatch?.started = true
                    currentBatch?.timeoutJob?.cancel()
                    currentBatch?.timeoutJob = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = player.duration
                        if (duration != C.TIME_UNSET) {
                            _playbackDuration.value = duration
                        }

                        _playbackStartTime.value = SystemClock.elapsedRealtime()

                        if (utils.getAnSubtitle()) {
                            showAnSubtitle()
                        }

                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(
                                Intent().setAction(utils.ON_PLAYER_STATE_READY)
                            )
                    }

                    Player.STATE_ENDED -> {
                        // 只通知当前单项播放协程结束，不在 Listener 中调度队列。
                        val waiter = playbackWaiter
                        if (waiter?.isActive == true) {
                            waiter.resumeWith(Result.success(Unit))
                        }
                    }

                    Player.STATE_BUFFERING -> Unit
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val duration = player.duration
                if (duration != C.TIME_UNSET) {
                    _playbackDuration.value = duration
                }
                _playbackStartTime.value = SystemClock.elapsedRealtime()

                if (utils.getAnSubtitle()) {
                    if (reason == 3) return
                    if (player.currentMediaItemIndex >= player.mediaItemCount) return
                    showAnSubtitle()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("AnnouncementPlayer", "播放异常: ${error.message}", error)
                utils.showMsg("播放异常: ${error.message}")

                val waiter = playbackWaiter
                if (waiter?.isActive == true) {
                    waiter.resumeWith(Result.failure(error))
                }
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

    enum class PlayMode {
        /** 立即播放 */
        INTERRUPT,
        /** 追加播放，会等待INTERRUPT队列播放完成 */
        APPEND
    }

    // =========================================================================================
    // 公共 API
    // =========================================================================================

    /**
     * 播放一组媒体项目。
     *
     * INTERRUPT：清空旧 INTERRUPT 队列，打断当前任务并优先播放新队列。
     * APPEND：追加到 APPEND 队列，不打断当前播放。
     */
    /**
     * @param playTimeoutSeconds 播放超时秒数；null 表示不启用。
     * 从调用 play() 开始计时，若本批次尚未真正开始播放，则清空本批次。
     */
    fun play(
        mediaItemList: MutableList<AnMediaItem>,
        mode: PlayMode = PlayMode.INTERRUPT,
        playTimeoutSeconds: Int? = null
    ) {
        if (mediaItemList.isEmpty()) return
        require(playTimeoutSeconds == null || playTimeoutSeconds >= 0) {
            "playTimeoutSeconds 不能小于 0"
        }

        val batch = PlayBatch(mediaItemList.toList())
        val timeoutDeadline = playTimeoutSeconds?.let {
            SystemClock.elapsedRealtime() + it * 1000L
        }

        scope.launch {
            when (mode) {
                PlayMode.INTERRUPT -> {
                    interruptQueue.forEach { it.timeoutJob?.cancel() }
                    interruptQueue.clear()
                    interruptQueue.addLast(batch)

                    if (currentOwner == Owner.APPEND) {
                        activeAppendBatch?.let { appendQueue.addFirst(it) }
                        activeAppendBatch = null
                        activeAppendIndex = 0
                    }

                    activeInterruptBatch?.timeoutJob?.cancel()
                    activeInterruptBatch = null
                    activeInterruptIndex = 0

                    val oldJob = dispatcherJob
                    dispatcherJob = null
                    oldJob?.cancelAndJoin()

                    player.stop()
                    player.clearMediaItems()
                    playbackWaiter = null
                    currentItem = null
                    currentOwner = null
                    currentBatch = null
                    activeInterruptBatch = null
                    activeInterruptIndex = 0
                    _playingText.value = ""
                    _playbackStartTime.value = 0L
                    _playbackDuration.value = 0L
                    requestDispatch()
                }

                PlayMode.APPEND -> {
                    appendQueue.forEach { it.timeoutJob?.cancel() }
                    appendQueue.clear()
                    appendQueue.addLast(batch)
                    requestDispatch()
                }
            }

            if (playTimeoutSeconds != null && !batch.started) {
                batch.timeoutJob = scope.launch {
                    val remaining = (timeoutDeadline!! - SystemClock.elapsedRealtime())
                        .coerceAtLeast(0L)
                    delay(remaining.milliseconds)
                    clearBatchIfNotStarted(batch)
                }
            }
        }
    }

    private suspend fun clearBatchIfNotStarted(batch: PlayBatch) {
        if (batch.started) return

        interruptQueue.remove(batch)
        appendQueue.remove(batch)

        val isActiveBatch =
            activeInterruptBatch === batch || activeAppendBatch === batch

        if (!isActiveBatch) {
            batch.timeoutJob = null
            return
        }

        batch.timeoutJob = null

        val wasCurrent = currentBatch === batch

        if (activeInterruptBatch === batch) {
            activeInterruptBatch = null
            activeInterruptIndex = 0
        }
        if (activeAppendBatch === batch) {
            activeAppendBatch = null
            activeAppendIndex = 0
        }

        if (wasCurrent) {
            val oldJob = dispatcherJob
            dispatcherJob = null
            oldJob?.cancelAndJoin()

            player.stop()
            player.clearMediaItems()
            playbackWaiter = null
            currentItem = null
            currentOwner = null
            currentBatch = null
            _playingText.value = ""
            _playbackStartTime.value = 0L
            _playbackDuration.value = 0L
        }

        Log.d("AnnouncementPlayer", "播放超时，已清空本次 play() 队列")
        requestDispatch()
    }

    fun stopAnnounce(cancelPlayJob: Boolean = true): Job {
        return scope.launch {
            stopAnnounceInternal(
                clearQueues = true,
                cancelPlayJob = cancelPlayJob
            )
        }
    }

    // =========================================================================================
    // 调度核心
    // =========================================================================================

    /**
     * 启动唯一调度协程。
     * 调度协程始终先取 INTERRUPT；只有 INTERRUPT 队列为空才取 APPEND。
     */
    private fun requestDispatch() {
        if (dispatcherJob?.isActive == true) return

        dispatcherJob = scope.launch {
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()

                    val item: AnMediaItem
                    val owner: Owner
                    val batch: PlayBatch

                    when {
                        interruptQueue.isNotEmpty() || activeInterruptBatch != null -> {
                            if (activeInterruptBatch == null) {
                                activeInterruptBatch = interruptQueue.removeFirst()
                                activeInterruptIndex = 0
                            }
                            val active = activeInterruptBatch!!
                            if (activeInterruptIndex < active.items.size) {
                                batch = active
                                item = active.items[activeInterruptIndex++]
                                owner = Owner.INTERRUPT
                            } else {
                                active.timeoutJob?.cancel()
                                activeInterruptBatch = null
                                activeInterruptIndex = 0
                                continue
                            }
                        }

                        else -> {
                            if (activeAppendBatch == null && appendQueue.isNotEmpty()) {
                                activeAppendBatch = appendQueue.removeFirst()
                                activeAppendIndex = 0
                            }
                            val active = activeAppendBatch
                            if (active != null && activeAppendIndex < active.items.size) {
                                batch = active
                                item = active.items[activeAppendIndex++]
                                owner = Owner.APPEND
                            } else {
                                activeAppendBatch = null
                                activeAppendIndex = 0
                                finishIfIdle()
                                break
                            }
                        }
                    }

                    currentItem = item
                    currentOwner = owner
                    currentBatch = batch

                    try {
                        prepareItem(item)
                        currentCoroutineContext().ensureActive()
                        playPreparedItem(item)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        Log.e("AnnouncementPlayer", "播报项目失败: ${item.text}", error)
                    } finally {
                        player.stop()
                        player.clearMediaItems()
                        playbackWaiter = null

                        if (currentItem === item) {
                            currentItem = null
                            currentOwner = null
                            currentBatch = null
                        }
                        _playingText.value = ""
                        _playbackStartTime.value = 0L
                    }

                    if (owner == Owner.INTERRUPT) {
                        val active = activeInterruptBatch
                        if (active != null && activeInterruptIndex >= active.items.size) {
                            active.timeoutJob?.cancel()
                            activeInterruptBatch = null
                            activeInterruptIndex = 0
                        }
                    } else {
                        val active = activeAppendBatch
                        if (active != null && activeAppendIndex >= active.items.size) {
                            active.timeoutJob?.cancel()
                            activeAppendBatch = null
                            activeAppendIndex = 0
                        }
                    }
                }
            } finally {
                dispatcherJob = null
            }
        }
    }

    private suspend fun playPreparedItem(item: AnMediaItem) {
        currentCoroutineContext().ensureActive()

        val exoItem = MediaItem.Builder()
            .setUri(item.filePath)
            .setMediaId(item.filePath)
            .build()

        suspendCancellableCoroutine { continuation ->
            playbackWaiter = continuation

            continuation.invokeOnCancellation {
                if (playbackWaiter === continuation) {
                    playbackWaiter = null
                }
            }

            try {
                player.setMediaItem(exoItem)
                player.prepare()

                audioManager.requestAudioFocus(audioFocusRequest)
                player.play()

            } catch (error: Exception) {
                if (playbackWaiter === continuation) {
                    playbackWaiter = null
                }
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(error))
                }
            }
        }

        if (playbackWaiter?.isActive == false) {
            playbackWaiter = null
        }
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
    // 收尾 / 停止
    // =========================================================================================

    /** 只有两条队列均空且当前没有处理项目时才发送完成广播。 */
    private fun finishIfIdle() {
        if (interruptQueue.isNotEmpty()) return
        if (appendQueue.isNotEmpty()) return
        if (currentItem != null) return

        Log.d("AnnouncementPlayer", "finishIfIdle")

        audioManager.abandonAudioFocusRequest(audioFocusRequest)

        _playingText.value = ""
        _playbackStartTime.value = 0L
        _playbackDuration.value = 0L

        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(
                Intent().setAction(utils.ON_PLAYER_STATE_ENDED)
            )
    }

    private suspend fun stopAnnounceInternal(
        clearQueues: Boolean,
        cancelPlayJob: Boolean = true
    ) {
        Log.d("AnnouncementPlayer", "stopAnnounce")

        if (clearQueues) {
            interruptQueue.forEach { it.timeoutJob?.cancel() }
            appendQueue.forEach { it.timeoutJob?.cancel() }
            activeInterruptBatch?.timeoutJob?.cancel()
            activeAppendBatch?.timeoutJob?.cancel()
            interruptQueue.clear()
            appendQueue.clear()
            activeInterruptBatch = null
            activeInterruptIndex = 0
            activeAppendBatch = null
            activeAppendIndex = 0
        }

        if (cancelPlayJob) {
            val oldJob = dispatcherJob
            dispatcherJob = null
            oldJob?.cancelAndJoin()
        } else {
            // 不取消调度协程时，唤醒当前等待器，让它检查队列状态后自然结束。
            val waiter = playbackWaiter
            if (waiter?.isActive == true) {
                waiter.resumeWith(Result.success(Unit))
            }
        }

        playbackWaiter = null
        currentItem = null
        currentOwner = null
        currentBatch = null

        audioManager.abandonAudioFocusRequest(audioFocusRequest)

        player.stop()
        player.clearMediaItems()

        _playingText.value = ""
        _playbackStartTime.value = 0L
        _playbackDuration.value = 0L

        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(
                Intent().setAction(utils.ON_PLAYER_STOP)
            )
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
        _playingText.value = text
    }

    fun setTargetGain() {
        val sessionId = audioSessionId ?: return
        val enhancer = LoudnessEnhancer(sessionId)
        enhancer.setTargetGain(utils.getLoudnessBoostAmount()) // 毫分贝
        enhancer.enabled = true
        this@AnnouncementPlayer.loudnessEnhancer = enhancer
    }
}