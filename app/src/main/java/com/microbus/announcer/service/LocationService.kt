package com.microbus.announcer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.microbus.announcer.MainActivity
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.model.StationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class LocationService : Service() {

    private lateinit var utils: Utils
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    private val CHANNEL_ID = "location_service_channel"
    private val NOTIFICATION_ID = 1001

    // 创建 Handler，并与主线程的 Looper 绑定
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        utils = Utils(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updNotification()
        startForeground(NOTIFICATION_ID, notification)
        handler.post(object : Runnable {
            override fun run() {
                Log.d("PeriodicService", "任务执行了，时间：${System.currentTimeMillis()}")
                updNotification()
                handler.postDelayed(this, 2000)
            }
        }
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {


        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Required for location tracking in background"
            setShowBadge(false)
        }

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

//    private fun createNotification(): Notification {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            notificationIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        return Notification.Builder(this, CHANNEL_ID)
//            .setContentTitle("Location Service")
//            .setContentText("Tracking location...")
//            .setContentIntent(pendingIntent)
//            .setOngoing(true)
//            .build()
//    }

    private fun updNotification() {

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        // title
        var title = "currentLine.name"

        // text
        var text = "binding.currentStationState.text"


        val notificationBuilder =
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.an)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())


        // Android 16.1+
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {

            notificationBuilder.setRequestPromotedOngoing(true)

            val progressStyle = Notification.ProgressStyle().apply {
                setStyledByProgress(false)
                setProgress(100) // 设置总进度
                // 添加分段 (Segment)
                setProgressSegments(
                    listOf(
                        Notification.ProgressStyle.Segment(20)
                            .setColor("#B6B6B6".toColorInt()),  // 已过
                        Notification.ProgressStyle
                            .Segment(80)
                            .setColor("#37B267".toColorInt())     // 前方站
                    )
                )

                setProgress(0)
            }

            notificationBuilder.setStyle(progressStyle)

        }

        notification = notificationBuilder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)

    }

}