package com.microbus.announcer.service
//
//import android.app.Service
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.ServiceInfo
//import android.location.Location
//import android.media.AudioAttributes
//import android.media.AudioFocusRequest
//import android.media.AudioFormat
//import android.media.AudioManager
//import android.media.AudioTrack
//import android.media.MediaMetadataRetriever
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.IBinder
//import android.os.Looper
//import android.speech.tts.TextToSpeech
//import android.speech.tts.UtteranceProgressListener
//import android.util.Log
//import androidx.core.app.NotificationCompat.Builder
//import androidx.core.app.ServiceCompat.startForeground
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.amap.api.location.AMapLocationClient
//import com.amap.api.location.AMapLocationClientOption
//import com.amap.api.maps.model.LatLng
//import com.arthenica.mobileffmpeg.FFmpeg
//import com.microbus.announcer.R
//import com.microbus.announcer.SensorHelper
//import com.microbus.announcer.Utils
//import com.microbus.announcer.bean.Line
//import com.microbus.announcer.bean.Station
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import java.io.File
//import java.io.FileInputStream
//import java.io.FileWriter
//import java.util.Locale
//import java.util.UUID
//
//
//class LineRunningService : Service() {
//
//    val serviceId = 1
//    var tag: String = javaClass.simpleName
//    private lateinit var utils: Utils
//
//    /**正前往下一站标志*/
//    private val onNext = 0
//
//    /**正前往下一站标志*/
//    private val onWillArrive = 1
//
//    /**已到达站点标志*/
//    private val onArrive = 2
//
//    /**路线上行标志*/
//    private val onUp = 0
//
//    /**路线下行标志*/
//    private val onDown = 1
//
//    /**路线卡显示下一站或到站信息标志*/
//    private val onNextOrArrive = 0
//
//    /**路线卡显示首末站信息标志*/
//    private val onStartAndTerminal = 1
//
//    /**路线卡显示欢迎信息标志*/
//    private val onWel = 2
//
//    /**路线卡显示速度标志*/
//    private val onSpeed = 3
//
//    val actionNameLocationChange = "location.change"
//    val actionNameLineReverse = "line.reverse"
//
//    val actionLineLoad = "line.load"
//
//    var locationInterval = 1000
//    lateinit var mainHandler: Handler
//    lateinit var mainRunnable: Runnable
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//
//        receiveData(intent)
//        initService()
//        initLocation()
//        initBroadcastReceive()
//        initAnnouncement()
//
//        startRunning()
//
//        return START_STICKY
//    }
//
//    fun receiveData(intent: Intent) {
//        locationInterval = intent.getIntExtra("locationInterval", 1000)
//    }
//
//    fun initService() {
//        val notification = Builder(this, "default_channel")
//            .setContentTitle("Line is running")
//            .build()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(
//                this,
//                serviceId,
//                notification,
//                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
//            )
//        } else {
//            startForeground(serviceId, notification)
//        }
//        utils = Utils(applicationContext)
//        sensorHelper = SensorHelper(applicationContext)
//
//    }
//
////    fun initRunnable() {
////        mainHandler = Handler(Looper.getMainLooper())
////        mainRunnable = object : Runnable {
////            override fun run() {
////                mainHandler.postDelayed(this, locationInterval.toLong())
////                Log.d(tag, "run")
////                val intent = Intent(actionNameLocationChange).setClassName(
////                    packageName,
////                    "com.microbus.announcer.broadcast.LineRunningBroadcastReceiver"
////                )
////                    .putExtra("location", "这是一个应用内广播！")
////                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
////            }
////        }
////    }
//
//
//    /**初始位置：广西桂林市秀峰区十字街*/
//    private var lastLngLat = LatLng(25.278617, 110.295833)
//    private var currentLngLat = lastLngLat
//    lateinit var locationClient: AMapLocationClient
//
//    /**
//     * 初始化定位
//     */
//    fun initLocation() {
//
//        val sharedPreferences = applicationContext.getSharedPreferences("location", MODE_PRIVATE)
//        val latitude = sharedPreferences.getFloat("latitude", 0f)
//        val longitude = sharedPreferences.getFloat("longitude", 0f)
//
////        utils.showMsg(latitude.toString())
//
//        if (latitude != 0f) {
//            lastLngLat = LatLng(latitude.toDouble(), longitude.toDouble())
//            currentLngLat = lastLngLat
//        }
//
//        locationClient = AMapLocationClient(applicationContext)
//
//        val option = AMapLocationClientOption()
//        option.interval = utils.getLocationInterval().toLong()
//        option.isSensorEnable = true
//        locationClient.setLocationOption(option)
//
//        locationClient.setLocationListener { location ->
//            onMyLocationChange(location)
//            Log.d(tag, "${location.latitude}")
//        }
//    }
//
//    private var lastTimeMillis = System.currentTimeMillis()
//    private var currentTimeMillis = System.currentTimeMillis()
//    lateinit var sensorHelper: SensorHelper
//    private var currentDistanceToCurrentStation = 100.0
//    private var lastDistanceToCurrentStation = 100.0
//
//    /**当前路线站点*/
//    private var currentLineStation =
//        Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
//
//    private var currentSpeedKmH = -1.0
//
//    private var currentLineStationList = ArrayList<Station>()
//    private var currentReverseLineStationList = ArrayList<Station>()
//
//    var lastDistanceToStationList = ArrayList<Double>()
//    var currentDistanceToStationList = ArrayList<Double>()
//
//    val reverseLastDistanceToStationList = ArrayList<Double>()
//    val reverseCurrentDistanceToStationList = ArrayList<Double>()
//
//    /**
//     * 定位更新回调
//     */
//    fun onMyLocationChange(location: Location) {
//
//        //更新位置与时间
//        lastTimeMillis = currentTimeMillis
//        currentTimeMillis = System.currentTimeMillis()
//
//        lastLngLat = currentLngLat
//        currentLngLat = LatLng(location.latitude, location.longitude)
//
//        // 更新地图
////        CoroutineScope(Dispatchers.IO).launch {
////            if (binding.switchFollowLocation.isChecked) {
////                CoroutineScope(Dispatchers.Main).launch {
////                    aMap.stopAnimation()
////                    aMap.animateCamera(CameraUpdateFactory.changeLatLng(lastLngLat))
////                }
////                Thread.sleep(250L)
////                CoroutineScope(Dispatchers.Main).launch {
////                    aMap.stopAnimation()
////                    aMap.animateCamera(
////                        CameraUpdateFactory.changeBearing(
////                            sensorHelper.getAzimuth().toFloat()
////                        )
////                    )
////                }
////            }
////        }
//
////        //更新定位标点
////        if (!this::locationMarker.isInitialized) {
////            locationMarker = aMap.addMarker(
////                MarkerOptions().position(currentLngLat).setFlat(true)
////                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.location_marker))
////            )
////            locationMarker.setAnchor(0.5F, 0.57F)
////        }
////
////        locationMarker.rotateAngle = -sensorHelper.getAzimuth().toFloat()
////
////        val anim = TranslateAnimation(currentLngLat)
////        anim.setDuration(100L)
////        anim.setInterpolator(DecelerateInterpolator())
////        locationMarker.setAnimation(anim)
////        locationMarker.startAnimation()
////
//        // 更新距离当前到站距离
//        lastDistanceToCurrentStation = currentDistanceToCurrentStation
//        currentDistanceToCurrentStation = utils.calculateDistance(
//            currentLngLat.longitude,
//            currentLngLat.latitude,
//            currentLineStation.longitude,
//            currentLineStation.latitude
//        )
////
////
////        // 距离格式化
////        if (currentLine.name == "") {
////            binding.currentDistanceToCurrentStationValue.text = "-"
////        } else if (currentDistanceToCurrentStation >= 100000) {
////            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
////            binding.currentDistanceToCurrentStationValue.text =
////                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation / 1000)
////        } else if (currentDistanceToCurrentStation >= 10000) {
////            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
////            binding.currentDistanceToCurrentStationValue.text =
////                String.format(Locale.CHINA, "%.2f", currentDistanceToCurrentStation / 1000)
////        } else if (currentDistanceToCurrentStation >= 1000) {
////            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
////            binding.currentDistanceToCurrentStationValue.text =
////                String.format(Locale.CHINA, "%.3f", currentDistanceToCurrentStation / 1000)
////        } else {
////            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.m)
////            binding.currentDistanceToCurrentStationValue.text =
////                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation)
////        }
////
//        //更新速度
//        val distance = utils.calculateDistance(
//            currentLngLat.longitude,
//            currentLngLat.latitude,
//            lastLngLat.longitude,
//            lastLngLat.latitude
//        )
//
//        currentSpeedKmH = if (currentSpeedKmH < 0) 0.0
//        else (distance / 1000.0) / ((currentTimeMillis - lastTimeMillis) / 1000.0 / 60.0 / 60.0)
//
////        binding.speedValue.text = String.format(Locale.CHINA, "%.1f", currentSpeedKmH)
//
//        // 计算正向距离
//        for (i in currentLineStationList.indices) {
//            lastDistanceToStationList[i] = currentDistanceToStationList[i]
//            currentDistanceToStationList[i] = utils.calculateDistance(
//                currentLngLat.longitude,
//                currentLngLat.latitude,
//                currentLineStationList[i].longitude,
//                currentLineStationList[i].latitude
//            )
//        }
//
//        // 计算反向距离
//        for (i in currentReverseLineStationList.indices) {
//            reverseLastDistanceToStationList[i] = reverseCurrentDistanceToStationList[i]
//            reverseCurrentDistanceToStationList[i] = utils.calculateDistance(
//                currentLngLat.longitude,
//                currentLngLat.latitude,
//                currentReverseLineStationList[i].longitude,
//                currentReverseLineStationList[i].latitude
//            )
//        }
//
//        //遍历当前方向路线所有站点，先遍历正向，如果没有符合的站点，再遍历反向（Beta）
//        if (!findMatchStation(false)) {
//            findMatchStation(true)
//        }
//
//        val intent = Intent(actionNameLocationChange)
//            .putExtra("latitude", location.latitude)
//            .putExtra("longitude", location.longitude)
//            .putExtra("azimuth", sensorHelper.getAzimuth())
//            .putExtra("distanceToCurrentStation", currentDistanceToCurrentStation)
//            .putExtra("speedKmH", currentSpeedKmH)
//        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//
//    }
//
//
//    var matchCount = 0
//    private var currentLineStationState: Int = onNext
//    private lateinit var currentLine: Line
//    private var lineArriveStationIdList = ArrayList<Int>()
//
//    private var currentLineDirection = onUp
//
//    /**
//     * 遍历站点列表，检查是否符合进站、出站、即将到站条件，并切换站点然后报站
//     * @return 当前站点是否更改
//     */
//    private fun findMatchStation(isReverseLine: Boolean): Boolean {
//
//        matchCount = (matchCount + 1) % Int.MAX_VALUE
//        if (matchCount < 2) return false
//
//        val arriveStationDistance = utils.getArriveStationDistance()    // 进站临界点
//        val willArriveStationDistance = arriveStationDistance + 50    // 进站临界点
//
//        val lineStationList = when (isReverseLine) {
//            true -> currentReverseLineStationList
//            false -> currentLineStationList
//        }
//
//        val lastDistanceToStationList = when (isReverseLine) {
//            true -> reverseLastDistanceToStationList
//            false -> lastDistanceToStationList
//        }
//
//        val currentDistanceToStationList = when (isReverseLine) {
//            true -> reverseCurrentDistanceToStationList
//            false -> currentDistanceToStationList
//        }
//
//        for (i in lineStationList.indices) {
//            //进站条件：现在定位在这个站点内
//            if (currentDistanceToStationList[i] <= arriveStationDistance) {
//
//                //当前站点及状态相同，直接返回
//                if ((lineStationList[i].id == currentLineStation.id && currentLineStationState == onArrive)) {
//                    return true
//                }
//
//                Log.d(
//                    tag,
//                    "到达站：${lineStationList[i].cnName} for ${currentDistanceToStationList[i]} <= $arriveStationDistance"
//                )
//
//                if (isReverseLine) {
////                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
//                    val intent = Intent(actionNameLineReverse)
//                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//                } else {
//                    setStationAndState(i, onArrive)
//                }
//
//                // 自动切换线路方向
//                if (utils.getIsAutoSwitchLineDirection() && currentLine.name != resources.getString(
//                        R.string.line_all
//                    )
//                ) {
//                    lineArriveStationIdList.add(i)
//                    // 如果切换到了之前（已经过）的站点
//                    if (lineArriveStationIdList.size > 1
//                        && lineArriveStationIdList.last() < lineArriveStationIdList[lineArriveStationIdList.size - 2]
//                    ) {
//                        lineArriveStationIdList.clear()
////                        binding.lineDirectionSwitch.isChecked =
////                            !binding.lineDirectionSwitch.isChecked
//                        val intent = Intent(actionNameLineReverse)
//                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//                    }
//                }
//
//                announce()
//                utils.longHaptic()
//                return true
//            }
//            //即将进站条件：上次位于即将进站范围外，现在位于即将进站范围内，且现在不位于进站进站内
//            else if (lastDistanceToStationList[i] > willArriveStationDistance
//                && currentDistanceToStationList[i] <= willArriveStationDistance
//                && currentDistanceToStationList[i] > arriveStationDistance
//            ) {
//
//                Log.d(
//                    tag,
//                    "即将进站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
//                )
//
//                if (isReverseLine) {
////                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
//                    val intent = Intent(actionNameLineReverse)
//                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//                } else if (currentLineStationState != onNext || lineStationList[i].id != currentLineStation.id) {
//                    setStationAndState(i, onNext)
//                } else {
//                    setStationAndState(i, onWillArrive)
//                }
//
//                announce(1)
//                utils.longHaptic()
//                return true
//            }
//            //出站条件：上次位于某站点内，现在位于这个站点外（进站范围）
//            else if (lastDistanceToStationList[i] < arriveStationDistance && currentDistanceToStationList[i] > arriveStationDistance && currentLine.name != resources.getString(
//                    R.string.line_all
//                )
//            ) {
//
//                Log.d(
//                    tag,
//                    "${lineStationList[i].cnName} 出站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
//                )
//
//                if (isReverseLine) {
////                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
//                    val intent = Intent(actionNameLineReverse)
//                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//                }
//                // 上行终点站出站
//                else if (i >= lineStationList.size - 1 && currentLineDirection == onUp) {
////                    binding.lineDirectionSwitch.isChecked =
////                        !binding.lineDirectionSwitch.isChecked
//                    val intent = Intent(actionNameLineReverse)
//                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//                    setStationAndState(1, onNext)
//                } else if (i < lineStationList.size - 1) {
//                    setStationAndState(i + 1, onNext)
//                }
//
//                announce()
//                utils.longHaptic()
//
//                return true
//            }
//        }
//        return false
//    }
//
//
//    private var currentLineStationCount = 0
//
//    /**
//     * 切换站点及站点状态
//     * @param stationCount 要切换到的站点在路线中的序号
//     * @param stationState 要切换的站点状态
//     */
//    private fun setStationAndState(stationCount: Int, stationState: Int) {
//
//        if (stationCount < 0 || stationCount >= currentLineStationList.size) return
//
//        currentLineStationState = stationState
//
//        currentLineStation = currentLineStationList[stationCount]
//        currentLineStationCount = stationCount
//
//        val intent = Intent(actionNameLocationChange)
//            .putExtra("currentLineStationState", currentLineStationState)
//            .putExtra("currentLineStation", currentLineStation)
//
//        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//
//        //仅显示当前路线站点时，刷新地图标记
//        if (utils.getMapStationShowType() == 2) {
//            showCurrentStation()
//        }
//
//        //更新路线站点显示、小卡片和通知
//        refreshLineStationList()
//
//        //刷新路线头屏
//        refreshLineHeadDisplay()
//
//        //更新路线站点更新信息和系统通知
//        refreshLineStationChangeInfo()
//
//        //刷新站点标点
//        refreshStationMarker()
//
//        //设置高亮度15s屏幕唤醒锁
//        wakeLock.acquire(15 * 1000L)
//    }
//
//    fun initBroadcastReceive() {
//        val receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                when (intent.action) {
//                    actionLineLoad -> {
//                        currentLine.name = intent.getStringExtra("line")!!
//                        currentLine.id = intent.getIntExtra("lineId", 0)
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                            currentLineStationList = intent.getSerializableExtra("currentLineStationList", ArrayList::class.java) as ArrayList<Station>
//                            currentReverseLineStationList = intent.getSerializableExtra("currentReverseLineStationList", ArrayList::class.java) as ArrayList<Station>
//                            lastDistanceToStationList = intent.getSerializableExtra("lastDistanceToStationList", ArrayList::class.java) as ArrayList<Double>
//                            currentDistanceToStationList = intent.getSerializableExtra("currentDistanceToStationList", ArrayList::class.java) as ArrayList<Double>
//                            lineArriveStationIdList = intent.getSerializableExtra("lineArriveStationIdList", ArrayList::class.java) as ArrayList<Int>
//
//                        } else {
//                            currentLineStationList = intent.getSerializableExtra("currentLineStationList") as ArrayList<Station>
//                            currentReverseLineStationList = intent.getSerializableExtra("currentReverseLineStationList") as ArrayList<Station>
//                            lastDistanceToStationList = intent.getSerializableExtra("lastDistanceToStationList") as ArrayList<Double>
//                            currentDistanceToStationList = intent.getSerializableExtra("currentDistanceToStationList") as ArrayList<Double>
//                            lineArriveStationIdList = intent.getSerializableExtra("lineArriveStationIdList") as ArrayList<Int>
//                        }
//
//                        switchToNearestStation()
//                    }
//                }
//            }
//        }
//        val intentFilter = IntentFilter(LineRunningService().actionNameLocationChange)
//        LocalBroadcastManager.getInstance(applicationContext)
//            .registerReceiver(receiver, intentFilter)
//
//    }
//
//    private fun switchToNearestStation() {
//        var minDistance = Double.MAX_VALUE
//        var distance: Double
//        for (i in currentLineStationList.indices) {
//            distance = utils.calculateDistance(
//                currentLngLat.longitude,
//                currentLngLat.latitude,
//                currentLineStationList[i].longitude,
//                currentLineStationList[i].latitude
//            )
//            if (distance < minDistance) {
//                minDistance = distance
//                currentLineStationCount = i
//                currentLineStation = currentLineStationList[i]
//            }
//        }
//
//    }
//
//    private lateinit var tts: TextToSpeech
//    /**
//     * 初始化报站
//     */
//    lateinit var audioAttributes: AudioAttributes
//    lateinit var audioFormat: AudioFormat
//    var bufferSizeInBytes = 0
//    private fun initAnnouncement() {
//        tts = TextToSpeech(applicationContext) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                tts.language = Locale.CHINA
//            }
//        }
//        //设置音频属性
//        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
//            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
//
//        audioFocusRequest =
//            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
//                .setAudioAttributes(attributes).setOnAudioFocusChangeListener { focusChange ->
//                    when (focusChange) {
//                        //长时间丢失焦点
//                        AudioManager.AUDIOFOCUS_LOSS -> {
//                            //mediaPlayer!!.release()
//                        }
//                        //短暂失去焦点
//                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
//                            //mediaPlayer!!.pause()
//                        }
//                    }
//                }.build()
//
//        // 获取系统音频管理
//        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//        // 设置音频格式
//        audioAttributes =
//            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
//                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
//        audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//            .setSampleRate(32000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
//        bufferSizeInBytes = AudioTrack.getMinBufferSize(
//            32000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
//        )
//    }
//
//    private var audioManager: AudioManager? = null
//    private var audioFocusRequest: AudioFocusRequest? = null
//    /**
//     * 语音播报
//     * 音频格式标准
//     * wav 单声道 32000HZ 192kbps
//     * @param type 0：报进站或出站，1：报即将到站，2：单独报站名
//     * @param station type为2时要播报站点
//     * @param lang type为2时要播报的语种，cn，en，或者all（先报中文，再报英文）
//     */
//    fun announce(type: Int = 0, station: Station = Station(
//        null,
//        "MicroBus 欢迎您",
//        "MicroBus",
//        0.0,
//        0.0
//    ), lang: String = "") {
//
//        if (currentLineStationList.isEmpty())
//            return
//
//        Log.d(tag, "开始报站")
//
//        //如果没有管理外部存储的权限，请求授予
//
//        if (!requestManageFilesAccessPermission())
//            return
//
//        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
//
//        val voiceList = ArrayList<String>()
//        val filePathList = ArrayList<String>()
//        val tempFilePath = applicationContext.getExternalFilesDir("")?.path
//        val tempFile = File("$tempFilePath/tempAudio.wav")
//
//        // 列车进站/出站播报
//        if (type == 0) {
//
//            if (utils.getIsVoiceAnnouncements()) {
//
//
//                //普通话报站
//                if (currentLineStationState == onNext) {
//                    voiceList.add(voiceList.size, "/cn/thisTrain")
//                    voiceList.addAll(utils.getNumOrLetterVoiceList(currentLine.name))
//                    voiceList.add(voiceList.size, "/cn/isBoundFor")
//
//                    //终点站名称
//                    voiceList.add(
//                        voiceList.size,
//                        "/cn/station/${currentLineStationList.last().cnName}"
//                    )
//                } else if (currentLineStationState == onArrive && utils.getIsArriveTimeAnnouncements()) {
//                    voiceList.addAll(utils.getTimeVoiceList())
//                }
//
//
//                //当前到站 或 下一站
//                when (currentLineStationState) {
//                    onArrive -> voiceList.add(voiceList.size, "/cn/arriveStation")
//                    onNext -> voiceList.add(voiceList.size, "/cn/nextStation")
//                }
//
//                //起点站 或 终点站
//                if (currentLineStationCount == 0) voiceList.add(
//                    voiceList.size,
//                    "/cn/startingStation"
//                )
//                if (currentLineStationCount == currentLineStationList.size - 1) voiceList.add(
//                    voiceList.size, "/cn/terminal"
//                )
//
//                //站点名称
//                voiceList.add(voiceList.size, "/cn/station/" + currentLineStation.cnName)
//
////        when (currentLineStationState) {
////            onNext -> {
////                //欢迎乘坐
////                voiceList.add(voiceList.size, "welcomeToTake")
////                //路线名称
////                voiceList.add(voiceList.size, "/line/${currentLine.name}")
////
////            }
////        }
//            }
//
//            //英语报站
//            if (utils.getIsEnVoiceAnnouncements()) {
//                if (currentLineStationState == onNext) {
//                    //本次列车开往
//                    voiceList.add(voiceList.size, "/en/thisTrainIsBoundFor")
//
//                    //终点站名称
//                    if (File("$appRootPath/Media/en/station/${currentLineStationList.last().enName}.wav").exists()) voiceList.add(
//                        voiceList.size, "/en/station/${currentLineStationList.last().enName}"
//                    )
//                    else {
//                        if (utils.getIsUseTTS()) {
//                            voiceList.add(
//                                voiceList.size,
//                                "/cn/station/${currentLineStationList.last().cnName}"
//                            )
//                        } else {
////                        utils.showMsg("报站音频缺失\nen/station/$currentLineTerminalEnName.wav")
//                            voiceList.add(
//                                voiceList.size,
//                                "/cn/station/${currentLineStationList.last().cnName}"
//                            )
//                        }
//
//                    }
//                }
//
//                //列车到达 或 下一站
//                when (currentLineStationState) {
//                    onArrive -> voiceList.add(voiceList.size, "/en/arriveStation")
//                    onNext -> voiceList.add(voiceList.size, "/en/nextStation")
//                }
//
//                //起点站 或 终点站
//                if (currentLineStationCount == 0) voiceList.add(
//                    voiceList.size, "/en/startingStation"
//                )
//                if (currentLineStationCount == currentLineStationList.size - 1) voiceList.add(
//                    voiceList.size, "/en/terminal"
//                )
//
//                //站点名称
//                if (File("$appRootPath/Media/en/station/${currentLineStation.enName}.wav").exists()) voiceList.add(
//                    voiceList.size, "/en/station/${currentLineStation.enName}"
//                )
//                else {
//                    if (utils.getIsUseTTS()) {
//                        voiceList.add(
//                            voiceList.size,
//                            "/cn/station/${currentLineStation.cnName}"
//                        )
//                    } else {
////                    utils.showMsg("报站音频缺失\nen/station/${currentLineStation.enName}.wav")
//                        voiceList.add(
//                            voiceList.size,
//                            "/cn/station/${currentLineStation.cnName}"
//                        )
//                    }
//                }
//            }
//
//
//            if (utils.getIsSpeedAnnouncements() && currentLineStationState == onNext
//                && currentSpeedKmH >= 10 && currentSpeedKmH < 100
//            ) {
//                voiceList.add(voiceList.size, "/cn/当前时速")
//                voiceList.addAll(
//                    utils.intOrLetterToCnReading(
//                        currentSpeedKmH.toInt().toString(),
//                        "/cn/time/"
//                    )
//                )
//            }
//        }
////        报即将到站
//        else if (type == 1) {
//            voiceList.add(voiceList.size, "/cn/距离前方到站还有")
////            voiceList.add(voiceList.size, currentDistanceToCurrentStation.toInt().toString() + "米")
//            voiceList.add(voiceList.size, "/cn/station/" + currentLineStation.cnName)
////        报站名
//        } else if (type == 2) {
//            if (lang == "cn" || lang == "all") voiceList.add(
//                voiceList.size,
//                "/cn/station/${station.cnName}"
//            )
//            if (lang == "en" || lang == "all") voiceList.add(
//                voiceList.size,
//                "/en/station/${station.enName}"
//            )
//        }
//
//        //合成报站音频
//        if (this::audioScope.isInitialized) {
//            audioScope.cancel()
//        }
//
//        audioScope = CoroutineScope(Dispatchers.IO).launch {
//
////            val id = UUID.randomUUID().toString().substring(4)
//
//            if (!this.isActive) return@launch
//
//            //新建缓存文件目录
//            File(tempFilePath!!).mkdirs()
//            //创建缓存文件
//            tempFile.createNewFile()
//            //清除缓存文件内容
//            val writer = FileWriter(tempFile)
//            writer.write("")
//            writer.close()
//
//            val doneUtteranceIdList = ArrayList<String>()
//
//            if (utils.getIsUseTTS()) {
//
//                File("$tempFilePath/tts").walkTopDown().forEach {
//                    it.delete()
//                }
//                File("$tempFilePath/tts").mkdirs()
//
//                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//                    override fun onStart(utteranceId: String) {
//                    }
//
//                    override fun onDone(utteranceId: String) {
//                        doneUtteranceIdList.add(utteranceId)
//                    }
//
//                    override fun onError(utteranceId: String?) {
//                    }
//
//                })
//
//            }
//
//            // 合成音频
//            var isHasVoice = false
//            for (i in voiceList.indices) {
//                //寻找报站音频资源
//                if (!File("$appRootPath/Media/${voiceList[i]}.wav").exists()) {
//                    // 启用TTS，使用TTS音频
//                    if (utils.getIsUseTTS()) {
//                        filePathList.add("$tempFilePath/tts/" + voiceList[i].split('/').last())
//                    }
//                    // 不启用TTS，忽略该文本报站
//                    else {
//                        continue
//                    }
//                } else {
//                    isHasVoice = true
//                    filePathList.add("$appRootPath/Media/${voiceList[i]}.wav")
//                }
//            }
//
//            if (!isHasVoice) return@launch
//
//            //启动报站
//            if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
//                audioTrack.stop()
//                audioTrack.release()
//            }
//
//            audioTrack = AudioTrack(
//                audioAttributes, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, 1
//            )
//
//            audioTrack.play()
//
//
//            for (file in filePathList)
//                Log.d(tag, file)
//
//            // TTS文件的<原文件名，合成音频文件名>
//            val ttsMap = HashMap<String, String>()
//
//            //预合成tts
//            for (file in filePathList) {
//                if (file.split("/").reversed()[1] == "tts") {
//                    // 合成TTS
//                    val ttsFileName = "ttsFile" + UUID.randomUUID().toString() + ".wav"
//                    ttsMap[file] = ttsFileName
//                    val ttsFile = File("$tempFilePath/tts/", ttsFileName)
//                    val params = Bundle()
//                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsFileName)
//                    tts.synthesizeToFile(
//                        file.split('/').last(),
//                        params,
//                        ttsFile,
//                        ttsFileName
//                    )
//                }
//            }
//
//            //读取pcm，tts转码，加入播放流
//            for (file in filePathList) {
//                if (!isActive) {
//                    return@launch
//                }
//                val fis: FileInputStream
//                val fisPath: String
//                // TTS合成音频
//                if (file.split("/").reversed()[1] == "tts") {
//
//                    val ttsFileName = ttsMap[file]
//                    val ttsFile = File("$tempFilePath/tts/", ttsFileName!!)
//
//                    while (true) {
//                        if (!isActive) return@launch
//                        Thread.sleep(100)
////                    Log.d(tag, "$id $ttsDoneNum/$ttsTotalNum")
//                        if (doneUtteranceIdList.contains(ttsFileName)) break
//                    }
//
//                    //44100Hz -> 32000Hz
//                    val ttsFilePath =
//                        tempFilePath + "/tts/ttsFile" + UUID.randomUUID().toString() + ".wav"
//                    val command = "-i $ttsFile -ar 32000 -ac 1 $ttsFilePath"
//                    FFmpeg.cancel()
//                    FFmpeg.execute(command)
//                    if (!File(ttsFilePath).exists()) {
//                        return@launch
//                    }
//                    fisPath = ttsFilePath
//                }
//                // 本地音频
//                else {
//                    fisPath = file
//                }
//                fis = FileInputStream(fisPath)
//
//                audioManager!!.requestAudioFocus(audioFocusRequest!!)
//
//                val audioStream = FileInputStream(fisPath)
//                audioStream.skip(44 * 2)
//                val audioData = utils.convertToByteArray(audioStream)
//                audioTrack.write(audioData!!, 0, audioData.size)
//
//
//                // 播放完毕后，放弃音频焦点
//                try {
//                    val retriever = MediaMetadataRetriever()
//                    retriever.setDataSource(context, Uri.fromFile(File(fisPath)))
//                    val time =
//                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
//                    }, time!!.toLong())
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//
//                fis.close()
//            }
//
//            // 合成WAV音频 结束
//        }
//    }
//
//    fun startRunning() {
////        mainHandler.postDelayed(mainRunnable, 0)
//        matchCount = 0
//        locationClient.startLocation()
//    }
//
//    fun stopRunning() {
////        mainHandler.removeCallbacksAndMessages(null)
//        locationClient.stopLocation()
//
//    }
//}