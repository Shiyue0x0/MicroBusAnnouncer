package com.microbus.announcer.announce

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEachIndexed
import kotlin.time.Duration.Companion.milliseconds

class AnnouncementPlayer
@OptIn(UnstableApi::class) constructor(val context: Context) {

    private var utils = Utils(context)


    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null
    private var player: ExoPlayer
    private var audioSessionId: Int? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null


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
            .setAudioAttributes(exoAudioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {

                    // 播放准备就绪
                    Player.STATE_READY -> {
                        audioManager?.requestAudioFocus(audioFocusRequest!!)
                        if (utils.getAnSubtitle()) {
                            showAnSubtitle()
                        }
                        val intent = Intent()
                            .setAction(utils.ON_PLAYER_STATE_READY)
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(intent)
                    }

                    // 播放完成
                    Player.STATE_ENDED -> {
//                        if (player.currentMediaItemIndex >= filePathList.size) {
                        Log.d("L1946", "STATE_ENDED")
                        val intent = Intent()
                            .setAction(utils.ON_PLAYER_STATE_ENDED)
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(intent)
//                        }
                    }

                    Player.STATE_BUFFERING -> {
                    }

                    Player.STATE_IDLE -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                Log.d("L1966", "${player.currentMediaItemIndex} $reason")
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
                utils.showMsg("播放异常: ${error.message}")
                player.release()
                pauseAnnounce()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                this@AnnouncementPlayer.audioSessionId = audioSessionId
                setTargetGain()
            }

        })

        // 开启预加载，目标时长为 30 秒 (30,000,000 微秒)
        player.preloadConfiguration = ExoPlayer.PreloadConfiguration(30_000_000L)

    }

    var filePathList = ArrayList<AnnouncementResolver.MediaItem>()
    lateinit var playJob: Job

    fun play(
        mediaItemList: ArrayList<AnnouncementResolver.MediaItem>,
        anResolver: AnnouncementResolver
    ) {

        playJob = CoroutineScope(Dispatchers.IO).launch {

            pauseAnnounce(cancelPlayJob = false).join()

            filePathList = mediaItemList
            filePathList.forEachIndexed { i, mediaItem ->

                val timeoutMs = 15_000L

                // 等待TTS合成/Network下载完成
                if (mediaItem.source == AnnouncementResolver.AudioSource.TTS) {

                    if (!isActive) {
                        pauseAnnounce()
                        return@launch
                    }

                    val start = SystemClock.elapsedRealtime()
                    while (!anResolver.hasTTSDone(mediaItem.filePath)) {
                        currentCoroutineContext().ensureActive()
                        if (SystemClock.elapsedRealtime() - start > timeoutMs) {
                            withContext(Dispatchers.Main) { utils.showMsg("TTS合成超时") }
                            pauseAnnounce()
                            return@launch
                        }
                        delay(50.milliseconds)
                    }
                }

                // 等待HTTP下载完成
                if (mediaItem.source == AnnouncementResolver.AudioSource.NETWORK) {

                    if (!isActive) {
                        pauseAnnounce()
                        return@launch
                    }

                    val start = SystemClock.elapsedRealtime()
                    while (!anResolver.hasNetworkDone(mediaItem.filePath)) {
                        currentCoroutineContext().ensureActive()
                        if (SystemClock.elapsedRealtime() - start > timeoutMs) {
                            withContext(Dispatchers.Main) { utils.showMsg("GET音频超时") }
                            return@launch
                        }
                        delay(50.milliseconds)
                    }
                }

                withContext(Dispatchers.Main) {

                    Log.d("L3863", "ok ${mediaItem.filePath}")
                    Log.d("L201", "play ${player.playbackState}")

                    val mediaItem = MediaItem.Builder()
                        .setUri(mediaItem.filePath)
                        .setMediaId(mediaItem.filePath)
                        .build()

                    player.addMediaItem(i, mediaItem)

                    if (player.playbackState == STATE_ENDED) {
                        player.seekTo(i, 0L)
                    }

                    player.prepare()
                    player.play()
                }

            }
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
        utils.showMsg(
            fileName.take(lastDotIndex), true
        )
    }

    fun pauseAnnounce(cancelPlayJob: Boolean = true): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)

            player.stop()
            player.clearMediaItems()
            if (this@AnnouncementPlayer::playJob.isInitialized && cancelPlayJob) {
                playJob.cancel()
            }

            val intent = Intent()
                .setAction(utils.ON_PLAYER_PAUSE)
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