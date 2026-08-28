package com.microbus.announcer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.microbus.announcer.Utils

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