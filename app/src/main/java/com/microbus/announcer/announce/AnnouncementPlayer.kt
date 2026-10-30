package com.microbus.announcer.announce

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.microbus.announcer.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEachIndexed
import kotlin.time.Duration.Companion.milliseconds

class AnnouncementPlayer @OptIn(UnstableApi::class)
private constructor(val context: Context) {


    private var utils = Utils(context)

    private var audioFocusRequest: AudioFocusRequest
    private val audioManager: AudioManager
    private var player: ExoPlayer
    private var audioSessionId: Int? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    val anResolver = AnnouncementProvider.getInstance(context)


    /** 外部可用来订阅当前正在播放的字幕文本 */
    val playingText: StateFlow<String>
        field = MutableStateFlow("")

    /** 开始播放的系统时间（毫秒，SystemClock.elapsedRealtime），未播放时为 0 */
    val playbackStartTime: StateFlow<Long>
        field = MutableStateFlow(0L)

    /** 当前媒体总时长（毫秒），未知时为 C.TIME_UNSET（-9223372036854775807L） */
    val playbackDuration: StateFlow<Long>
        field = MutableStateFlow(0L)


    init {


        //设置音频属性
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

        audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        //长时间丢失焦点
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            //mediaPlayer!!.release()
                        }
                        //短暂失去焦点
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            //mediaPlayer!!.pause()
                        }
                    }
                }.build()

        // 获取系统音频管理
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 设置音频格式
        val exoAudioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        @OptIn(UnstableApi::class)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 50000,  // 最小缓冲时长，建议提高
                /* maxBufferMs = */ 60000,  // 最大缓冲时长
                /* bufferForPlaybackMs = */ 2500, // 开始播放所需最小缓冲
                /* bufferForPlaybackAfterRebufferMs = */ 5000 // 恢复播放所需最小缓冲
            )
            .build()

        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)   // <--- 在这里设置
            .setAudioAttributes(exoAudioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {

                    // 播放项播放准备就绪
                    Player.STATE_READY -> {

                        val dur = player.duration
                        if (dur != C.TIME_UNSET) {
                            playbackDuration.value = dur
                        }

                        playbackStartTime.value = SystemClock.elapsedRealtime()

                        if (utils.getAnSubtitle()) {
                            showAnSubtitle()
                        }
                        val intent = Intent()
                            .setAction(utils.ON_PLAYER_STATE_READY)
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(intent)
                    }

                    // 播放项播放完成
                    Player.STATE_ENDED -> {
                        Log.d("L1946", "STATE_ENDED")
                        stopAnnounce(false)
                        val intent = Intent()
                            .setAction(utils.ON_PLAYER_STATE_ENDED)
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(intent)
                    }

                    Player.STATE_BUFFERING -> {}

                    Player.STATE_IDLE -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                Log.d("L1966", "${player.currentMediaItemIndex} $reason")

                val dur = player.duration
                if (dur != C.TIME_UNSET) {
                    playbackDuration.value = dur
                }

                playbackStartTime.value = SystemClock.elapsedRealtime()

                if (utils.getAnSubtitle()) {
                    if (reason == 3) {
                        return
                    }
                    if (player.currentMediaItemIndex >= player.mediaItemCount) {
                        return
                    }
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

        // 开启预加载，目标时长为 30 秒 (30,000,000 微秒)
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

    var playJob: Job? = null

    val ttsTimeoutMs = 30_000L

    val netWorkTimeoutMs = 15_000L


    /**
     * 播放音频
     * @param mediaItemList 音频列表
     * */
    @OptIn(UnstableApi::class)
    fun play(mediaItemList: MutableList<AnnouncementProvider.MediaItem>) {

        mediaItemList.forEach {
            Log.d("L176", it.text ?: "")
        }

        if (playJob != null) {
            playJob?.cancel()
        }

        playJob = CoroutineScope(Dispatchers.IO).launch {

            stopAnnounce(cancelPlayJob = false).join()

            mediaItemList.forEachIndexed { i, mediaItem ->

                // 等待TTS合成完成
                if (mediaItem.source == AnnouncementProvider.AudioSourceType.TTS) {

                    if (!isActive) {
                        stopAnnounce()
                        playJob?.cancel()
                        playJob = null
                        return@launch
                    }

                    waitTTSDoneWithTimeoutToBlank(mediaItem)

                }
                // 等待HTTP下载完成
                if (mediaItem.source == AnnouncementProvider.AudioSourceType.NETWORK) {

                    if (!isActive) {
                        stopAnnounce()
                        playJob?.cancel()
                        playJob = null
                        return@launch
                    }

                    val start = SystemClock.elapsedRealtime()
                    var isTimeout = false
                    while (!anResolver.hasNetworkVoiceDone(mediaItem.filePath)) {
                        currentCoroutineContext().ensureActive()
                        if (SystemClock.elapsedRealtime() - start > netWorkTimeoutMs) {
                            withContext(Dispatchers.Main) { utils.showMsg("GET音频超时：${mediaItem.text}") }
                            isTimeout = true
                        }
                        delay(50.milliseconds)
                    }

                    // TODO 超时或失败回退
                    Log.d(
                        "L222",
                        "${System.identityHashCode(mediaItem)} ${mediaItem.text} ${mediaItem.status}"
                    )
                    if (isTimeout || mediaItem.status == AnnouncementProvider.MediaItemStatus.FAILED) {
                        Log.d("L223", "GET音频超时或失败回退")
                        mediaItem.source = AnnouncementProvider.AudioSourceType.TTS
                        mediaItem.status = AnnouncementProvider.MediaItemStatus.LOADING
                        val ttsFilePath = anResolver.buildTTSMedia(mediaItem.text ?: "")
                        mediaItem.filePath = ttsFilePath
                        if (anResolver.hasTTSDone(ttsFilePath)) {
                            mediaItem.status = AnnouncementProvider.MediaItemStatus.COMPLETED
                        }
                        waitTTSDoneWithTimeoutToBlank(mediaItem)
                    }

                }

                Log.d("L201", "play ${mediaItem.filePath}")

                val mediaItem = MediaItem.Builder()
                    .setUri(mediaItem.filePath)
                    .setMediaId(mediaItem.filePath)
                    .build()

                addMediaItemAndPlay(mediaItem, i)

            }
        }
    }

    private suspend fun waitTTSDoneWithTimeoutToBlank(
        mediaItem: AnnouncementProvider.MediaItem
    ) {
        val start = SystemClock.elapsedRealtime()
//        while (!anResolver.hasTTSDone(mediaItem.filePath)) {
        while (mediaItem.status != AnnouncementProvider.MediaItemStatus.COMPLETED) {
            currentCoroutineContext().ensureActive()
            if (SystemClock.elapsedRealtime() - start > ttsTimeoutMs) {
                withContext(Dispatchers.Main) { utils.showMsg("TTS合成超时") }
                // 超时回退
                mediaItem.source = AnnouncementProvider.AudioSourceType.BLANK
                val blankFilePath = anResolver.buildBlankMedia("/blank/10.pcm")
                mediaItem.filePath = blankFilePath
                mediaItem.status = AnnouncementProvider.MediaItemStatus.COMPLETED
                return
            }
            delay(50.milliseconds)
        }
        return
    }


    @OptIn(UnstableApi::class)
    private suspend fun addMediaItemAndPlay(mediaItem: MediaItem, i: Int) {
        withContext(Dispatchers.Main) {

            Log.d("L201", "play ${player.playbackState}")

            player.addMediaItem(i, mediaItem)

            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(i, 0L)
            }

            Log.d("L201", "requestAudioFocus")
            audioManager.requestAudioFocus(audioFocusRequest)
            player.prepare()
            player.play()
        }
    }

    fun showAnSubtitle() {

        val mediaItem = player.currentMediaItem ?: return

        val path = mediaItem.mediaId
        // blank 播报间隔：不显示字幕
        if (Regex("^.*/blank/[^/]+\\.pcm$").matches(path))
            return

        val fileName = path.split('/').last()
        val lastDotIndex = fileName.lastIndexOf(".")
        val text = fileName.take(lastDotIndex)

        utils.showMsg(text, true)
        playingText.value = text
    }

    // TODO 改为stop
    fun stopAnnounce(cancelPlayJob: Boolean = true): Job {
        Log.d("L308", "pauseAnnounce")
        return CoroutineScope(Dispatchers.Main).launch {

            Log.d("L201", "abandonAudioFocus")
            audioManager.abandonAudioFocusRequest(audioFocusRequest)

            player.stop()
            player.clearMediaItems()
            if (playJob != null && cancelPlayJob) {
                playJob?.cancel()
            }

            playingText.value = ""
            playbackStartTime.value = 0L

            val intent = Intent()
                .setAction(utils.ON_PLAYER_STOP)
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(intent)
        }
    }

    fun setTargetGain() {

        if (audioSessionId == null) {
            return
        }

        val loudnessEnhancer = LoudnessEnhancer(audioSessionId!!)

        // 使用audioSessionId创建音量增强器
        loudnessEnhancer.setTargetGain(utils.getLoudnessBoostAmount()) // 毫分贝
        loudnessEnhancer.enabled = true

        // 注意：需要将loudnessEnhancer保存在成员变量中防止被GC回收
        this@AnnouncementPlayer.loudnessEnhancer = loudnessEnhancer

    }
}