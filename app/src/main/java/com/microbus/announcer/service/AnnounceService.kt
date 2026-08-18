package com.microbus.announcer.service

import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.PLAYSTATE_PLAYING
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.fragment.MainFragment.PcmWithInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ArrayBlockingQueue
import kotlin.random.Random

class AnnounceService : Service() {

    private lateinit var utils: Utils


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        utils = Utils(this)


        // 执行任务
        return START_STICKY
    }


}