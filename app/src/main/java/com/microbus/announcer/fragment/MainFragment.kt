package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.UiModeManager
import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.UI_MODE_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.MAP_TYPE_NIGHT
import com.amap.api.maps.AMap.MAP_TYPE_NORMAL
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.UiSettings
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MultiPointItem
import com.amap.api.maps.model.MultiPointOverlay
import com.amap.api.maps.model.MultiPointOverlayOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.maps.model.Text
import com.amap.api.maps.model.TextOptions
import com.amap.api.maps.model.animation.TranslateAnimation
import com.amap.api.services.busline.BusLineQuery
import com.amap.api.services.busline.BusLineResult
import com.amap.api.services.busline.BusLineSearch
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.microbus.announcer.MainActivity
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.SensorHelper
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.LineOfSearchAdapter
import com.microbus.announcer.adapter.StationOfLineAdapter
import com.microbus.announcer.adapter.StationOfRunningInfoAdapter
import com.microbus.announcer.bean.EsItem
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.RunningInfo
import com.microbus.announcer.bean.Station
import com.microbus.announcer.bean.TrajectoryPoint
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogLineSwitchBinding
import com.microbus.announcer.databinding.DialogLoadingBinding
import com.microbus.announcer.databinding.DialogRunningInfoBinding
import com.microbus.announcer.databinding.FragmentMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ArrayBlockingQueue
import java.util.stream.Collectors
import kotlin.random.Random


class MainFragment : Fragment() {

    private var tag = javaClass.simpleName

    private val appRootPath =
        Environment.getExternalStorageDirectory().absolutePath + "/Announcer"

    /**正前往下一站标志*/
    private val onNext = 0

    /**正前往下一站标志*/
    private val onWillArrive = 1

    /**已到达站点标志*/
    private val onArrive = 2

    /**路线上行标志*/
    val onUp = 0

    /**路线下行标志*/
    val onDown = 1

    val lastDistanceToStationList = ArrayList<Double>()
    val currentDistanceToStationList = ArrayList<Double>()

    val reverseLastDistanceToStationList = ArrayList<Double>()
    val reverseCurrentDistanceToStationList = ArrayList<Double>()

    private lateinit var utils: Utils

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

    private var lastTimeMillis = System.currentTimeMillis()
    private var currentTimeMillis = System.currentTimeMillis()

    private val mLooper: Looper = Looper.getMainLooper()

    private val mMapHandler: Handler = Handler(mLooper)
    private lateinit var mapRunnable: Runnable


    private lateinit var aMapView: MapView
    private lateinit var aMap: AMap

    /**0灰色已通过；1蓝色当前站点；2绿色前方站点*/
    private val multiPointOverlayList = ArrayList<MultiPointOverlay>()

    private lateinit var aMapUiSettings: UiSettings
    private lateinit var aMapStationClickText: Text
    private var aMapStationTextList = ArrayList<Text>()
    private var multiPointCustomerId = ""
    private var aMapLastZoom = 0F
    private var aMapCurrentZoom = 0F
    private val aMapZoomPoint = 16F


    private lateinit var lineDatabaseHelper: LineDatabaseHelper
    private lateinit var stationDatabaseHelper: StationDatabaseHelper

    private lateinit var audioTrack: AudioTrack

    /**初始位置：广西桂林市秀峰区十字街*/
    private var lastLngLat = LatLng(25.278617, 110.295833)
    var currentLngLat = lastLngLat

    private var currentDistanceToCurrentStation = 100.0
    private var lastDistanceToCurrentStation = 100.0

    var originLine = Line()

    private var currentLine = Line()

    /**当前方向路线站点下标序列*/
//    private var currentLineStationIdList = ArrayList<Int>()

    /**当前路线站点运行方向（上下行）*/
    var currentLineDirection = onUp

    /**当前路线运行方向站点列表*/
    private var currentLineStationList = ArrayList<Station>()

    /**当前路线运行反向站点列表*/
    private var currentReverseLineStationList = ArrayList<Station>()

    /**当前路线站点*/
    private var currentLineStation =
        Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)

    /**当前路线运行站点计数，对应currentLineStation的下标*/
    private var currentLineStationCount = 0
    private var currentLineStationState: Int = onNext

    //    private var markerList = ArrayList<Marker>()
    private var circleList = ArrayList<Circle>()
    private val polylineList = ArrayList<Polyline>()

//    /**上次定位路线站点距离列表*/
//    private val lastDistanceToStationList = ArrayList<Double>()
//
//    /**本次定位路线站点距离列表*/
//    private val currentDistanceToStationList = ArrayList<Double>()

    /**当前速度*/
    private var currentSpeedKmH = -1.0

    /**路线到站序列*/
    private var lineArriveStationIdList = ArrayList<Int>()

    //路线电显显示序列
    private var lineHeadCardShowList: MutableSet<String>? = null

    //路线电显当前显示下标
    private var lineHeadCardCurrentShowIndex = 0


    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: NotificationCompat.Builder

    // 当前上行线路区间始发站下标（-1为未设置）
    private var currentUpLineStartingIndex = -1

    // 当前上行线路区间终点站下标（MAX_VALUE为未设置）
    private var currentUpLineTerminalIndex = Int.MAX_VALUE

    // 当前下行线路区间始发站下标（-1为未设置）
    private var currentDownLineStartingIndex = -1

    // 当前下行线路区间终点站下标（MAX_VALUE为未设置）
    private var currentDownLineTerminalIndex = Int.MAX_VALUE

    //操作锁定
    var isOperationLock = false

    /**屏幕唤醒锁*/
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock

    /**TTS*/
    private lateinit var tts: TextToSpeech

    private lateinit var audioStreamScope: Job

    private lateinit var audioPlayScope: Job

    lateinit var locationClient: AMapLocationClient

    lateinit var locationMarker: Marker

    lateinit var sensorHelper: SensorHelper

    private val cloudStationList = ArrayList<Station>()

    val lastStationHandler = Handler(mLooper)
    val nextStationHandler = Handler(mLooper)

    private val audioReleaseHandler = Handler(mLooper)

    private lateinit var announcementLangList: ArrayList<String>

    private lateinit var permissionManager: PermissionManager
    private var hasInitNotice = false

    var matchCount = 0

    var esList = ArrayList<EsItem>()
    var esPlayIndex = -1

    var userLocationOpen = false
    var userMapOpen = false

    val runningInfoList = ArrayList<RunningInfo>()

    val locationInfoList = ArrayList<String>()

    var isAnnouncing = false

    val simRunningHandler = Handler(mLooper)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {


        if (_binding != null) return binding.root

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

        sensorHelper = SensorHelper(requireContext())

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        lineDatabaseHelper = LineDatabaseHelper(context)
        stationDatabaseHelper = StationDatabaseHelper(context)

        currentDistanceToCurrentStation =
            utils.getStationRangeByLineType(currentLine.type).toDouble() + 1
        lastDistanceToCurrentStation =
            utils.getStationRangeByLineType(currentLine.type).toDouble() + 1

        /**设置屏幕唤醒锁*/
        powerManager =
            requireActivity().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, tag)

        //设置状态栏填充高度
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        permissionManager = PermissionManager(requireContext(), requireActivity())

        //初始化定位
        initLocation()

        //初始化地图
        initMap()

        //初始化通知
        if (utils.getNotice()) {
            initNotification()
        }

        //初始化路线
        initLine()

        // 初始化按钮回调
        initButton()

        // 初始化电显
        initEs()

        // 初始化报站
        initAnnouncement()

        // 初始化路线规划
        initLinePlan()

        // 初始化本地广播
        initLocalBroadcast()

        // 初始化路线运行服务
//        initLineRunningService()

//        binding.lineStationListContainer.setScrollView(binding.lineStationList)


        announcementLangList = utils.getLangList()

        return binding.root
    }

    /* 与用户交互时 */
    override fun onResume() {
        super.onResume()

//        Log.d(tag, "onResume")

        if (userMapOpen)
            binding.mapBtnGroup.check(binding.mapBtnGroup.id)

        if (userLocationOpen)
            binding.locationBtnGroup.check(binding.locationBtn.id)

        (binding.lineStationList.adapter as StationOfLineAdapter).isShown = true


    }

    /* 不再与用户交互时 */
    override fun onPause() {
//        Log.d(tag, "onPause")

        binding.mapBtnGroup.uncheck(binding.mapBtnGroup.id)

        userMapOpen = binding.mapBtn.isChecked
        userLocationOpen = binding.locationBtn.isChecked

        binding.locationBtnGroup.uncheck(binding.locationBtn.id)

        (binding.lineStationList.adapter as StationOfLineAdapter).isShown = false

        // 保存历史位置
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("location", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putFloat("latitude", currentLngLat.latitude.toFloat())
            putFloat("longitude", currentLngLat.longitude.toFloat())
        }

        super.onPause()

    }


    override fun onStop() {
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        aMapView.onSaveInstanceState(outState)
    }

    val client = OkHttpClient()


    /**
     * 加载路线
     */
    fun loadLine(line: Line) {

//        Log.d(tag, "lineName: ${line.name}")
//        Log.d(tag, "upLineStation: ${line.upLineStation}")
//        Log.d(tag, "downLineStation: ${line.downLineStation}")

//        speedRefreshHandler.removeCallbacksAndMessages(null)

        //切换当前路线
        currentLine = line

        // 写入历史
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putString("lineName", currentLine.name)
        }

        //加载路线名称
//        binding.headerMiddle.text = currentLine.name
        binding.headerMiddleNew.showText(currentLine.name)
        binding.headerMiddleNew.requestLayout()


        //获取当前方向路线站点下标（String形式）序列
        val currentLineStationIndexStrList = when (currentLineDirection) {
            onUp -> currentLine.upLineStation.split(' ')
            onDown -> currentLine.downLineStation.split(' ')
            else -> List(0) { "" }
        }

        //获取当前反向向路线站点下标（String形式）序列
        val currentReverseLineStationIndexStrList = when (currentLineDirection) {
            onUp -> currentLine.downLineStation.split(' ')
            onDown -> currentLine.upLineStation.split(' ')
            else -> List(0) { "" }
        }

        //获取当前方向路线站点列表
        currentLineStationList.clear()
        for (strIndex in currentLineStationIndexStrList.toMutableList()) {
            // 本地路线
            if (strIndex.toIntOrNull() != null && strIndex.toInt() > 0) {
                val lineStationList = stationDatabaseHelper.queryById(strIndex.toInt())
                if (lineStationList.isNotEmpty())
                    currentLineStationList.add(lineStationList.first())
                else
                    currentLineStationList.add(
                        Station(
                            id = Int.MAX_VALUE,
                            cnName = "未知站点",
                            enName = "unknown"
                        )
                    )
            }
            // 云端路线
            else if (strIndex.toIntOrNull() != null) {
//                Log.d(
//                    tag,
//                    "strIndex: ${strIndex.toInt()} ${cloudStationList.find { it.id == strIndex.toInt() }!!.cnName}"
//                )
                currentLineStationList.add(cloudStationList.find { it.id == strIndex.toInt() }!!)
            }
        }

        //获取当前反向路线站点列表
        currentReverseLineStationList.clear()
        for (strIndex in currentReverseLineStationIndexStrList.toMutableList()) {
            // 本地路线
            if (strIndex.toIntOrNull() != null && strIndex.toInt() > 0) {
                val lineStationList = stationDatabaseHelper.queryById(strIndex.toInt())
                if (lineStationList.isNotEmpty()) {
                    // 在正向站点中寻找同名站点
                    val sameNameStation = currentLineStationList.find { station ->
                        station.cnName == lineStationList.first().cnName
                    }
                    // 没有找不到同名站点，即该站仅存在于反向站点，加如此站
                    if (sameNameStation == null)
                        currentReverseLineStationList.add(lineStationList.first())
                }
            }
        }

        // 初始化距离站点距离
        lastDistanceToStationList.clear()
        currentDistanceToStationList.clear()
        currentLineStationList.forEach { _ ->
            lastDistanceToStationList.add(Double.MAX_VALUE)
            currentDistanceToStationList.add(Double.MAX_VALUE)
        }

        reverseLastDistanceToStationList.clear()
        reverseCurrentDistanceToStationList.clear()
        currentReverseLineStationList.forEach { _ ->
            reverseLastDistanceToStationList.add(Double.MAX_VALUE)
            reverseCurrentDistanceToStationList.add(Double.MAX_VALUE)
        }

        if (currentLineStationList.isNotEmpty()) {

            //更新终点站卡片
            binding.terminalName.text = if (utils.getUILang() == "zh")
                currentLineStationList.last().cnName
            else
                currentLineStationList.last().enName

        } else {
            binding.terminalName.text = getString(R.string.main_to_station_name)
        }

        lineArriveStationIdList.clear()

        //切换当前站点为最近站点
        switchToNearestStation()

        val viewList = ArrayList<View>()
        viewList.add(binding.lineDirectionBtnGroup)
        viewList.add(binding.startingStation)
        viewList.add(binding.lastStation)
        viewList.add(binding.nextStation)
        viewList.add(binding.terminal)
        viewList.add(binding.terminalCard)
        viewList.add(binding.navCard)
        //显示|隐藏路线站点框和全站点路线按钮（渐出动画）
        if (currentLine.name != resources.getString(R.string.line_all) && currentLine.name != resources.getString(
                R.string.main_line_0
            )
        ) {
//            Log.d(tag, "出现")
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            fadeIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    binding.lineStationCard.visibility = INVISIBLE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    binding.lineStationCard.visibility = VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            binding.lineStationCard.startAnimation(fadeIn)
            viewList.forEach { view ->
                view.visibility = VISIBLE
            }
        } else {
//            Log.d(tag, "隐藏")
            val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (binding.lineStationCard.isVisible)
                        binding.lineStationCard.visibility = INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            binding.lineStationCard.startAnimation(fadeOut)
            viewList.forEach { view ->
                view.visibility = GONE
            }
        }

        //更新路线站点卡片
        val adapter = binding.lineStationList.adapter as StationOfLineAdapter
        adapter.stationList = currentLineStationList
        adapter.stationCount = currentLineStationCount
        adapter.stationState = currentLineStationState
        adapter.mHandler.removeCallbacksAndMessages(null)

        @SuppressLint("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()

        try {
            CoroutineScope(Dispatchers.IO).launch {
                // 获取路线轨迹（纠偏）
                if (utils.getIsLineTrajectoryCorrection() && currentLine.name != resources.getString(
                        R.string.line_all
                    )
                ) {

                    val trajectoryPointList = ArrayList<TrajectoryPoint>()
                    for (i in currentLineStationList.indices) {
                        val ag = if (i < currentLineStationList.size - 1)
                            utils.calculateBearing(
                                currentLineStationList[i].latitude,
                                currentLineStationList[i].longitude,
                                currentLineStationList[i + 1].latitude,
                                currentLineStationList[i + 1].longitude
                            ).toInt()
                        else
                            0

                        val sp = 1

                        val tm = if (i == 0) {
                            1735704000  //2025-01-01 12:00:00
                        } else {
                            // 米
                            val distance = utils.calculateDistance(
                                currentLineStationList[i].latitude,
                                currentLineStationList[i].longitude,
                                currentLineStationList[i - 1].latitude,
                                currentLineStationList[i - 1].longitude
                            )
                            (distance / 1000 / sp * 3600).toInt()
                        }

                        trajectoryPointList.add(
                            TrajectoryPoint(
                                x = "%.6f".format(currentLineStationList[i].longitude)
                                    .toDouble(),
                                y = "%.6f".format(currentLineStationList[i].latitude)
                                    .toDouble(),
                                ag = ag,
                                tm = tm,
                                sp = sp
                            )
                        )

                    }

                    val gson = Gson()
                    val json = gson.toJson(trajectoryPointList)
                    Log.d(tag, "aMapRes: json: $json")

                    val traceRequest = Request.Builder()
                        .url("https://restapi.amap.com/v4/grasproad/driving?key=${getString(R.string.amapKey_web)}")
                        .post(
                            json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        )
                        .build()
                    try {
                        val res = client.newCall(traceRequest).execute()
                        if (res.isSuccessful) {
                            synchronized(traceResJsonStr) {
                                traceResJsonStr = res.body.string()
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                utils.showMsg("轨迹获取失败")
                            }
                        }
                    } catch (e: UnknownHostException) {
                        e.printStackTrace()
                        requireActivity().runOnUiThread {
                            utils.showMsg("轨迹获取失败\n（无网络或连接异常）")
                        }
                    }
                }

                requireActivity().runOnUiThread {
//                CoroutineScope(Dispatchers.Main).launch {
                    //移除所有轨迹
                    for (line in polylineList) {
                        line.remove()
                    }
                    polylineList.clear()
                    lineWithTypeMap.clear()

                    //移除所有站点范围圆
                    for (circle in circleList) {
                        circle.remove()
                    }
                    circleList.clear()
                    circleWithStationMap.clear()

                    refreshUI(isRefreshEs = false)
                    refreshEsToStaringAndTerminal()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            utils.showMsg("路线加载异常")
        }

    }

    /**
     * 初始化定位
     */
    fun initLocation() {

        val sharedPreferences = requireContext().getSharedPreferences("location", MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat("latitude", 0f)
        val longitude = sharedPreferences.getFloat("longitude", 0f)


        if (latitude != 0f) {
            lastLngLat = LatLng(latitude.toDouble(), longitude.toDouble())
            currentLngLat = lastLngLat
        }

        locationClient = AMapLocationClient(requireContext())

        val option = AMapLocationClientOption()
        option.interval = utils.getLocationInterval().toLong()
        option.isSensorEnable = true
        option.isNeedAddress = true
        locationClient.setLocationOption(option)

        locationClient.setLocationListener { location ->
            if (location.errorCode == 0)
                onMyLocationChange(location)
        }

        locationClient.startLocation()

    }

    /**
     * 初始化按钮回调
     */
    fun initButton() {

        binding.mapBtnGroup.check(binding.mapBtn.id)
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)


        //单击电显切换路线
        binding.headerNew.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            val dialogBinding = DialogLineSwitchBinding.inflate(LayoutInflater.from(context))

            val alertDialog =
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setView(dialogBinding.root)
//                    .setTitle(resources.getString(R.string.switch_line))
                    .setNeutralButton(resources.getString(R.string.line_all)) { dialog, which ->
                        loadLineAll()
                    }
                    .setPositiveButton(
                        resources.getString(R.string.out_line_running)
                    ) { dialog, which ->
                        val line = Line(name = getString(R.string.main_line_0))
                        originLine = line
                        initLineInterval()
                        currentLineStationState = onNext
                        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                        loadLine(line)
                        utils.haptic(binding.headerMiddleNew)
                        notificationManager.cancelAll()
                    }
                    .setNegativeButton(
                        getString(R.string.search_by_category)
                    ) { dialog, which ->
                        var lineTypeList: Array<String?>
                        val stationList = stationDatabaseHelper.queryAll()
                        val lineList = lineDatabaseHelper.queryAll()
                        if (stationList.size < 2) {
                            lineTypeList = arrayOfNulls(1)
                            lineTypeList[0] = getString(R.string.station_not_enough_tip)
                        } else {
                            lineTypeList = arrayOfNulls(6)
                            lineTypeList[0] = resources.getString(R.string.line_normal_bus)
                            lineTypeList[1] = resources.getString(R.string.line_comm_bus)
                            lineTypeList[2] = resources.getString(R.string.line_metro)
                            lineTypeList[3] = resources.getString(R.string.line_train)
                            lineTypeList[4] = resources.getString(R.string.line_other)
                            lineTypeList[5] = resources.getString(R.string.line_all)
                        }

                        val matchLists = ArrayList<ArrayList<Line>>()
                        matchLists.add(
                            getMatchedLines(
                                lineList,
                                "^(\\d|[Kk]).*$".toRegex(),
                            )
                        )
                        matchLists.add(
                            getMatchedLines(
                                lineList,
                                "^[Uu]\\d{3}$".toRegex()
                            )
                        )
                        matchLists.add(
                            getMatchedLines(
                                lineList,
                                "^[Nn][Nn][Uu].+$".toRegex()
                            )
                        )
                        matchLists.add(
                            getMatchedLines(
                                lineList,
                                "^[DdGgTtZz].+$".toRegex()
                            )
                        )

                        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                            .setTitle(resources.getString(R.string.switch_line))
                            .setItems(lineTypeList) { _, which ->
                                when (which) {

                                    // 普通公交（数字或K开头）
                                    0 -> {
                                        showLinesChoosesDialog(matchLists[which], which)
                                    }

                                    // 社区公交（U开头，且后跟3位数字）
                                    1 -> {
                                        showLinesChoosesDialog(matchLists[which], which)
                                    }

                                    // 轨道交通（NNU开头）
                                    2 -> {
                                        showLinesChoosesDialog(matchLists[which], which)
                                    }

                                    // 火车车次（D|G|T|Z开头）
                                    3 -> {
                                        showLinesChoosesDialog(matchLists[which], which)
                                    }

                                    //其他路线（均不属于以上类型）
                                    4 -> {
                                        // 不属于其他的路线
                                        var matchSet = hashSetOf<Line>()

                                        for (lines in matchLists) {
                                            matchSet =
                                                matchSet.union(lines.toSet()) as HashSet<Line>
                                        }

                                        val allLineSet = lineDatabaseHelper.queryAll().toSet()

                                        val otherLineList = ArrayList<Line>()
                                        otherLineList.addAll(allLineSet.subtract(matchSet))

                                        val lineInfoList =
                                            arrayOfNulls<String>(otherLineList.size)

                                        for (i in otherLineList.indices) {
                                            val lineStationIndexListStr =
                                                otherLineList[i].upLineStation.split(' ')

                                            val lineStartingStation =
                                                stationDatabaseHelper.queryById(
                                                    lineStationIndexListStr.first().toInt()
                                                )
                                            val lineTerminal =
                                                stationDatabaseHelper.queryById(
                                                    lineStationIndexListStr.last().toInt()
                                                )

                                            val lineStartingStationCnName =
                                                if (lineStartingStation.isNotEmpty()) lineStartingStation.first().cnName
                                                else "-"

                                            val lineTerminalCnName =
                                                if (lineTerminal.isNotEmpty()) lineTerminal.first().cnName
                                                else "-"

                                            val lineStartingStationEnName =
                                                if (lineStartingStation.isNotEmpty()) lineStartingStation.first().enName
                                                else "-"

                                            val lineTerminalEnName =
                                                if (lineTerminal.isNotEmpty()) lineTerminal.first().enName
                                                else "-"

                                            lineInfoList[i] =
                                                if (utils.getUILang() == "zh")
                                                    "${otherLineList[i].name}  $lineStartingStationCnName - $lineTerminalCnName"
                                                else
                                                    "${otherLineList[i].name}  $lineStartingStationEnName - $lineTerminalEnName"
                                        }



                                        MaterialAlertDialogBuilder(
                                            requireContext(),
                                            R.style.CustomAlertDialogStyle
                                        )
                                            .setTitle(resources.getString(R.string.line_other))
                                            .setItems(lineInfoList) { _, which ->
                                                if (lineInfoList[which] != "") {
                                                    originLine = otherLineList[which]
                                                    initLineInterval()
                                                    currentLineStationState = onNext
                                                    binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                                                    loadLine(otherLineList[which])
                                                    utils.haptic(binding.headerMiddleNew)
                                                }
                                            }.create()
                                            .show()

                                    }

                                    // 全站点路线
                                    5 -> {
                                        loadLineAll()
                                    }
                                }
                            }
                            .create()
                            .show()

                        utils.haptic(binding.headerMiddleNew)
                    }
                    .show()

            dialogBinding.lineNameInput.setRawInputType(InputType.TYPE_CLASS_NUMBER)
            dialogBinding.lineNameInput.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, dialogBinding.lineNameInput)
                .show(WindowInsetsCompat.Type.ime())

            searchLine("", dialogBinding, alertDialog)

            dialogBinding.lineNameInput.addTextChangedListener { text ->
                searchLine(text.toString(), dialogBinding, alertDialog)
            }

            dialogBinding.onlineSearch.setOnClickListener {

                if (dialogBinding.lineNameInput.text.toString() == "") {
                    utils.showMsg("请输入要搜索的内容")
                    return@setOnClickListener
                }

                val loadingDialogBinding =
                    DialogLoadingBinding.inflate(LayoutInflater.from(context))
                loadingDialogBinding.title.text = getString(
                    R.string.now_search,
                    utils.getCity(),
                    dialogBinding.lineNameInput.text
                )

                val loadingDialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                )
                    .setView(loadingDialogBinding.root)
                    .show()


                val busLineQuery = BusLineQuery(
                    dialogBinding.lineNameInput.text.toString(),
                    BusLineQuery.SearchType.BY_LINE_NAME,
                    utils.getCity()
                )
                busLineQuery.pageNumber = 0
                busLineQuery.extensions = "all"
                busLineQuery.pageSize = 999999
                val busLineSearch = BusLineSearch(requireContext(), busLineQuery)
                busLineSearch.setOnBusLineSearchListener { res, rCode ->
                    loadingDialog.dismiss()
                    if (rCode != 1000) {
                        utils.showMsg("搜索失败，请检查网络连接")
                        return@setOnBusLineSearchListener
                    }
                    if (res.busLines.isEmpty()) {
                        utils.showMsg("暂时查找不到${utils.getCity()}${dialogBinding.lineNameInput.text}路线")
                        return@setOnBusLineSearchListener
                    }
                    findOnlineLine(res, alertDialog)
                }
                busLineSearch.searchBusLineAsyn()
            }

            dialogBinding.setAsLineName.setOnClickListener {
                //                    Log.d(tag, "设为临时路线")
                currentLine = Line(name = dialogBinding.lineNameInput.text.toString())
                initLineInterval()
                originLine = currentLine
                currentLineStationState = onNext
                binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                loadLine(currentLine)
                alertDialog.cancel()
            }


            return@setOnClickListener

        }

        //单击地图定位按钮，地图移动到当前位置
        binding.mapLocation.setOnClickListener {

            if (!permissionManager.hasLocationPermission()) {
                utils.showRequestLocationPermissionDialog(permissionManager)
                return@setOnClickListener
            }

            // 开启定位和地图
            binding.locationBtnGroup.check(binding.locationBtn.id)
            binding.mapBtnGroup.check(binding.mapBtn.id)

            // 立即地图移动到当前位置
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(lastLngLat))

            //点击复制当前经纬度
            if (utils.getIsClickLocationButtonToCopyLngLat()) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText(
                    "text", "${currentLngLat.longitude} ${currentLngLat.latitude}"
                )
                clipboard.setPrimaryClip(clipData)
                utils.showMsg("${currentLngLat.longitude}\n${currentLngLat.latitude}")
            }
        }

        //单击运行信息
        binding.runningInfo.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
//            val textView = TextView(requireContext())
//            textView.text = binding.lineStationChangeInfo.text
//            textView.setLineSpacing(100f, 0f)
//            textView.setPadding(100, 50, 100, 50)
//            val scrollView = ScrollView(requireContext())
//            scrollView.addView(textView)
//            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
//                .setTitle(getString(R.string.running_info))
//                .setView(scrollView).create()
//                .show()

            val dialogBinding = DialogRunningInfoBinding.inflate(LayoutInflater.from(context))

//            for (info in runningInfoList) {
//                Log.d(tag, info.stationName)
//            }

            dialogBinding.recyclerView.adapter =
                StationOfRunningInfoAdapter(requireContext(), runningInfoList)

            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                .setTitle(getString(R.string.running_info))
                .setView(dialogBinding.root)
                .show()
        }

        //长按地图定位按钮，开启/关闭操作锁定
        binding.mapLocation.setOnLongClickListener {
            run {
                isOperationLock = !isOperationLock
                if (isOperationLock) utils.showMsg(getString(R.string.operation_lock_switch_to_on_tip))
                else utils.showMsg(getString(R.string.operation_lock_switch_to_off_tip))
                utils.haptic(requireView())
            }
            true
        }

        // 启用定位按钮
        binding.locationBtn.addOnCheckedChangeListener { button, isChecked ->

            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                button.isChecked = !button.isChecked
                return@addOnCheckedChangeListener
            }

            if (isChecked) {
                if (!permissionManager.hasLocationPermission()) {
                    button.isChecked = false
                    utils.showRequestLocationPermissionDialog(permissionManager)
                    return@addOnCheckedChangeListener
                }
                locationClient.startLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f
                binding.navStationCard.visibility = VISIBLE
                binding.navSpeedCard.visibility = VISIBLE
            } else {
                locationClient.stopLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 0f
                matchCount = 0
                binding.currentDistanceToCurrentStationValue.text
                getString(R.string.main_distance_value)
                currentSpeedKmH = -1.0
                binding.speedValue.text =
                    getString(R.string.main_speed_value)
                binding.navStationCard.visibility = GONE
                binding.navSpeedCard.visibility = GONE
            }
        }

        binding.mapBtn.addOnCheckedChangeListener { button, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                button.isChecked = !button.isChecked
                return@addOnCheckedChangeListener
            }
            if (isChecked) {
                aMapView.onResume()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f
            } else {
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 0f
                aMapView.onPause()

            }
        }

        //切换上下行开关
        binding.lineDirectionBtnGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->

            if (!isChecked)
                return@addOnButtonCheckedListener

            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                group.uncheck(checkedId)
                return@addOnButtonCheckedListener
            }

            currentLineDirection = if (checkedId == binding.lineDirectionBtnUp.id) {
                onUp
            } else {
                onDown
            }

            if (currentLine.id == null) return@addOnButtonCheckedListener

            val lastLineStation = currentLineStation

            loadLine(currentLine)

            //若切换后站点仍在列表中，切换为该站
            for (stationIndex in currentLineStationList.indices) {
                if (currentLineStationList[stationIndex].cnName == lastLineStation.cnName) {
                    currentLineStationCount = stationIndex
                    currentLineStation = lastLineStation
                    break
                }
            }

            refreshUI()
        }


        //起点站按钮
        binding.startingStation.scaleX = -1f
        binding.startingStation.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            if (currentLineStationCount != 0) {
                setStationAndState(0, onNext)
                utils.haptic(binding.startingStation)
            }

        }

        //终点站按钮
        binding.terminal.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            if (currentLineStationCount != currentLineStationList.size - 1) {
                setStationAndState(currentLineStationList.size - 1, onArrive)
                utils.haptic(binding.terminal)
            }

        }

        //上站按钮
        binding.lastStation.setOnClickListener {
            val lastLineStationState = currentLineStationState
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            lastStation()
            if (currentLineStationState != lastLineStationState) utils.haptic(binding.lastStation)
        }
        binding.lastStation.setOnLongClickListener {
            return@setOnLongClickListener true
        }
        binding.lastStation.setOnTouchListener { v, event ->
            val lastStationRunnable = object : Runnable {
                override fun run() {
                    binding.lastStation.performClick()
                    lastStationHandler.postDelayed(this, 100L)
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 触摸按下
                    lastStationHandler.postDelayed(lastStationRunnable, 100L)
                    return@setOnTouchListener false
                }

                MotionEvent.ACTION_UP -> {
                    // 触摸松开
                    lastStationHandler.removeCallbacksAndMessages(null)
                    return@setOnTouchListener false
                }

                MotionEvent.ACTION_CANCEL -> {
                    // 触摸松开
                    lastStationHandler.removeCallbacksAndMessages(null)
                    return@setOnTouchListener false
                }

                else -> {
                    return@setOnTouchListener false
                }
            }
        }


        //下站按钮
        binding.nextStation.setOnClickListener {
            val lastLineStationState = currentLineStationState

            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            nextStation()
            if (currentLineStationState != lastLineStationState) utils.haptic(binding.nextStation)

        }
        binding.nextStation.setOnLongClickListener {
            return@setOnLongClickListener true
        }
        binding.nextStation.setOnTouchListener { v, event ->
            val nextStationRunnable = object : Runnable {
                override fun run() {
                    binding.nextStation.performClick()
                    nextStationHandler.postDelayed(this, 100L)
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 触摸按下
                    nextStationHandler.postDelayed(nextStationRunnable, 100L)
                    return@setOnTouchListener false
                }

                MotionEvent.ACTION_UP -> {
                    // 触摸松开
                    nextStationHandler.removeCallbacksAndMessages(null)
                    return@setOnTouchListener false
                }

                MotionEvent.ACTION_CANCEL -> {
                    // 触摸松开
                    nextStationHandler.removeCallbacksAndMessages(null)
                    return@setOnTouchListener false
                }

                else -> {
                    return@setOnTouchListener false
                }
            }
        }


        //报本站按钮
        binding.voiceAnnouncement.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            //语音播报当前站点
            announce()

            utils.haptic(requireView())
        }

        //模拟运行播报
        binding.voiceAnnouncement.setOnLongClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnLongClickListener true
            }

            // 关闭定位
            binding.locationBtnGroup.uncheck(binding.locationBtn.id)

            //关闭报站
            pauseAnnounce()

            // 切换到起点站
            if (currentLineStationCount != 0) {
                setStationAndState(0, onNext)
                utils.haptic(binding.startingStation)
            }

            var lastAnFinishTimestamp = Long.MIN_VALUE
            var lastIsAnnouncing = false
            val simRunningRunnable = object : Runnable {
                override fun run() {

//                    Log.d(tag, "simRunningRunnable running")

                    if (!isAnnouncing) {

                        if (lastIsAnnouncing) {
                            lastAnFinishTimestamp = System.currentTimeMillis()
                        }

                        if (System.currentTimeMillis() >= lastAnFinishTimestamp + utils.getAutoAnInterval() * 1000L) {
                            if (nextStation()) {
                                isAnnouncing = true
                                announce()

                            } else {
                                simRunningHandler.removeCallbacksAndMessages(null)
                            }
                        }
                    }

                    lastIsAnnouncing = isAnnouncing

                    simRunningHandler.postDelayed(this, 100L)

                }
            }
            simRunningHandler.postDelayed(simRunningRunnable, 0L)

            utils.haptic(requireView())

            true
        }


        // 终点站卡片
        binding.terminalCard.setOnClickListener {
            utils.showMsg("${binding.terminalTitle.text} ${binding.terminalName.text}")
        }

        // 当前站点卡片
        binding.currentStationCard.setOnClickListener {
            utils.showMsg("${binding.currentStationState.text} ${binding.currentStationName.text}")
            utils.showMsg("${binding.currentDistanceToCurrentStationValue.text}${binding.currentDistanceToCurrentStationUnit.text}")
        }

        // 单击速度卡片
        binding.speedCard.setOnClickListener {
            utils.showMsg("${binding.speedValue.text}${binding.speedUnit.text}")
        }

        // 长按速度卡片
        binding.speedCard.setOnLongClickListener {

            val textView = TextView(requireContext())

            val nestedScrollView = NestedScrollView(requireContext())
            nestedScrollView.setPadding(utils.dp2px(24F))
            nestedScrollView.addView(textView)

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("历史定位信息")
                .setView(nestedScrollView)
                .show()

            val infoUpdateHandler = Handler(mLooper)
            val infoUpdateRunnable = object : Runnable {
                override fun run() {
                    var listStr = ""
                    locationInfoList.forEachIndexed { index, locationInfo ->
                        listStr += locationInfo
                        if (index < locationInfoList.size - 1)
                            listStr += "\n"
                    }
                    textView.text = listStr
                    infoUpdateHandler.postDelayed(this, utils.getLocationInterval().toLong())
                }
            }
            infoUpdateHandler.postDelayed(infoUpdateRunnable, 0L)

            dialog.setOnDismissListener {
                infoUpdateHandler.removeCallbacksAndMessages(null)
            }

            return@setOnLongClickListener true
        }

        // 完成编辑
        binding.finishEditLine.setOnClickListener {

            val stationNameList = java.util.ArrayList<String>()
            lineEditorStationList.forEachIndexed { i, station ->
                stationNameList.add("(${i + 1})${station.cnName}[${station.id}]")
            }

            val editorModeStr = when (lineEditorMode) {
                "update" -> "更新"
                "new" -> "新增"
                else -> ""
            }

            val directionStr = when (lineEditorLineDirection) {
                onUp -> "上行"
                onDown -> "下行"
                else -> ""
            }

            val dialog =
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("正在${editorModeStr}路线 $lineEditorLineName $directionStr")
                    .setItems(stationNameList.toTypedArray(), null)
                    .setNegativeButton("继续编辑", null)
                    .setPositiveButton("提交", null)
                    .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                if (lineEditorStationList.size < 2) {
                    utils.showMsg("请至少添加2个站点")
                    return@setOnClickListener
                }

                var stationIdListStr = ""
                lineEditorStationList.forEachIndexed { i, station ->
                    stationIdListStr += station.id.toString()
                    if (i < lineEditorStationList.size - 1) {
                        stationIdListStr += " "
                    }
                }

                // 添加路线
                if (lineEditorMode == "new") {

                    // 上行
                    if (lineEditorLineDirection == onUp) {
                        val continueEditDownDialog = MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.CustomAlertDialogStyle
                        )
                            .setTitle("继续编辑下行站点")
                            .setMessage("如果上下行走向差不多，且对向站点距离都较近，可直接反转上行站点作为下行站点")
                            .setNegativeButton("继续录入下行", null)
                            .setPositiveButton("反转上行作为下行", null)
                            .show()

                        // 继续录入下行
                        continueEditDownDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setOnClickListener {
                                continueEditDownDialog.dismiss()
                                // todo
                            }

                        //反转上行作为下行
                        continueEditDownDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener {

                                continueEditDownDialog.dismiss()

                                // 上行
                                lineEditorUpLineStationListStr = stationIdListStr

                                // 下行（反转上行得到）
                                var stationIdDownListStr = ""
                                lineEditorStationList.reversed().forEachIndexed { i, station ->
                                    stationIdDownListStr += station.id.toString()
                                    if (i < lineEditorStationList.size - 1) {
                                        stationIdDownListStr += " "
                                    }
                                }
                                lineEditorDownLineStationListStr = stationIdDownListStr

                                val newLine = Line(
                                    name = lineEditorLineName,
                                    upLineStation = lineEditorUpLineStationListStr,
                                    downLineStation = lineEditorDownLineStationListStr
                                )

                                lineDatabaseHelper.insert(newLine)

                                utils.showMsg("路线 $lineEditorLineName 添加成功")

                                // todo 通知LineFrag 刷新

                                binding.finishEditLine.visibility = GONE
                                lineEditorLineId = -1
                                lineEditorStationList.clear()
                                lineEditorLineDirection = onUp

                                dialog.dismiss()
                            }

                    }
                    // 下行
                    else if (lineEditorLineDirection == onDown) {
                        // todo
                        lineEditorDownLineStationListStr = stationIdListStr
                    }

//                    val clipboard =
//                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                    val clipData = ClipData.newPlainText("text", stationIdListStr)
//                    clipboard.setPrimaryClip(clipData)
//
//                    utils.showMsg("当前路线站点序列已复制到剪切板")
//                    utils.showMsg("现在可以前往“路线”添加路线，或继续规划路线")
                }
                //  更新路线
                else if (lineEditorMode == "update") {
                    val oldLine = lineDatabaseHelper.queryById(lineEditorLineId).first()

                    when (lineEditorLineDirection) {
                        onUp -> {
                            oldLine.upLineStation = stationIdListStr
                        }

                        onDown -> {
                            oldLine.downLineStation = stationIdListStr
                        }

                        else -> {
                        }
                    }

                    lineDatabaseHelper.updateById(oldLine.id ?: -1, oldLine)
                    utils.showMsg("路线 ${oldLine.name} ${directionStr}已更新")

                    // todo 通知LineFrag 刷新

                    binding.finishEditLine.visibility = GONE
                    lineEditorLineId = -1
                    lineEditorStationList.clear()
                    lineEditorLineDirection = onUp

                    dialog.dismiss()
                }


            }

        }

        // 停止播报
        binding.stopAnnouncement.setOnClickListener {
            pauseAnnounce()
        }

        binding.serviceBtn.setOnClickListener {

            //如果没有管理外部存储的权限，请求授予
            if (!utils.isGrantManageFilesAccessPermission()) {
                utils.requestManageFilesAccessPermission(requireActivity())
                return@setOnClickListener
            }

            val serviceLangList = utils.getServiceLanguageStr().split("\n")

            MaterialAlertDialogBuilder(
                requireContext(),
                R.style.CustomAlertDialogStyle
            ).setTitle("播报服务语").setSingleChoiceItems(
                serviceLangList.toTypedArray(), -1
            ) { dialog, which ->
                announce(serviceLangList[which])
                dialog.cancel()
            }.show()

        }
    }

    /**
     * 初始化电显
     */
    private fun initEs() {

        binding.headerLeftNew.visibility =
            if (utils.getIsOpenLeftEs())
                VISIBLE
            else
                GONE

        binding.headerMiddleNew.visibility =
            if (utils.getIsOpenMidEs())
                VISIBLE
            else
                GONE

        esList = utils.getEsList(utils.getEsText())

        //路线电显显示序列
        lineHeadCardShowList = utils.getHeadSignShowInfo()
        //路线电显当前显示下标
        lineHeadCardCurrentShowIndex = 0
        //路线电显当前显示

        val esRefreshHandler = Handler(mLooper)
        var isRefreshing = false
        var esRefreshCount = 0
        val esRefreshRunnable = object : Runnable {

            override fun run() {

                esRefreshHandler.postDelayed(this, 100L)

                if (!isAdded)
                    return

                val esSpeed = utils.getEsSpeed()
                binding.headerLeftNew.pixelMovePerSecond = esSpeed
                binding.headerRightNew.pixelMovePerSecond = esSpeed
                binding.headerMiddleNew.pixelMovePerSecond = esSpeed

                val pos = utils.getEsFinishPositionOfLastWord()
                binding.headerLeftNew.finishPositionOfLastWord = pos
                binding.headerRightNew.finishPositionOfLastWord = pos
                binding.headerMiddleNew.finishPositionOfLastWord = pos

                binding.headerLeftNew.visibility = if (utils.getIsOpenLeftEs())
                    VISIBLE
                else
                    GONE

                binding.headerMiddleNew.visibility = if (utils.getIsOpenMidEs())
                    VISIBLE
                else
                    GONE

                if (esPlayIndex >= 0 && esPlayIndex < esList.size &&
                    esList[esPlayIndex].type.contains("R") && esRefreshCount % 10 == 0
                ) {
                    refreshEsOnlyText(true)
                }


                val isLeftFinish = binding.headerLeftNew.isShowFinish || !utils.getIsOpenLeftEs()
                val isRightFinish = binding.headerRightNew.isShowFinish
//                Log.d(tag, "Finished: $isLeftFinish $isRightFinish")
                if (!isRefreshing && isLeftFinish && isRightFinish) {
                    isRefreshing = true
                    esPlayNext()
                    refreshEs()
                    isRefreshing = false
                }
                if (binding.headerMiddleNew.isShowFinish) {
                    binding.headerMiddleNew.showText(currentLine.name)
                }

                if (::aMap.isInitialized && aMap.isTrafficEnabled != utils.getIsMapTrafficEnabled()) {
                    aMap.isTrafficEnabled = utils.getIsMapTrafficEnabled()
                }

                esRefreshCount++

                val navCardVisibility =
                    if (utils.getIsNavMode() &&
                        !(currentLine.name == resources.getString(R.string.line_all) && utils.getIsMapEditLineMode()
                                )
                    )
                        VISIBLE
                    else
                        GONE

                if (binding.navCard.visibility != navCardVisibility) {
                    binding.navCard.visibility = navCardVisibility
                    binding.fill3.visibility = navCardVisibility
                }

            }
        }
        esRefreshHandler.postDelayed(esRefreshRunnable, 0L)

////        var speedRefreshCount = 0
//        val speedRefreshRunnable = object : Runnable {
//            override fun run() {
////                currentSpeedKmH = speedRefreshCount.toDouble()
////                speedRefreshCount++
//
//
//
//                speedRefreshHandler.postDelayed(this, 1000L)
//            }
//        }
//        speedRefreshHandler.postDelayed(speedRefreshRunnable, 0L)

        binding.headerLeftNew.minShowTimeMs = 500
        binding.headerRightNew.minShowTimeMs = 500

        binding.navStationName.finishPositionOfLastWord = 0.0F

    }

    /**
     * 初始化报站
     */
    lateinit var audioAttributes: AudioAttributes
    lateinit var audioFormat: AudioFormat
    var bufferSizeInBytes = 0
    private fun initAnnouncement() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.CHINA
            }
        }

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
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 设置音频格式
        audioAttributes =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

        audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(0).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()

        bufferSizeInBytes = AudioTrack.getMinBufferSize(
            0, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )

    }

    /**
     * 初始化路线规划
     */
    lateinit var mRouteSearch: RouteSearch
    lateinit var planFrom: LatLng
    lateinit var planTo: LatLng
    lateinit var planFromMarker: Marker
    lateinit var planToMarker: Marker
    private fun initLinePlan() {
        mRouteSearch = RouteSearch(requireContext())
        mRouteSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
            override fun onDriveRouteSearched(
                res: DriveRouteResult, rCode: Int
            ) {

            }

            override fun onBusRouteSearched(
                res: BusRouteResult, rCode: Int
            ) {
            }

            override fun onWalkRouteSearched(
                res: WalkRouteResult, rCode: Int
            ) {
            }

            override fun onRideRouteSearched(
                res: RideRouteResult, rCode: Int
            ) {
                val stationFullList = stationDatabaseHelper.queryAll()
                val stationList = ArrayList<Station>()
                // 遍历路线方案

                val path = res.paths[0]
//                Log.d(tag, "size${path.polyline.size}")


                for (i in 0 until path.polyline.size) {


                    for (station in stationFullList) {
                        // 计算当前路线到站点圆心的距离
                        val distance = utils.calculateDistance(
                            path.polyline[i].longitude, //经度 x
                            path.polyline[i].latitude,  //纬度 y
                            station.longitude,
                            station.latitude
                        )
//                        if (stationList.indexOf(station) == -1) {
                        var currentToLastDistance = Double.MAX_VALUE
                        if (stationList.isNotEmpty()) {
                            currentToLastDistance = utils.calculateDistance(
                                stationList.last().longitude, //经度 x
                                stationList.last().latitude,  //纬度 y
                                station.longitude,
                                station.latitude
                            )
                        }
                        if (distance < 30 && currentToLastDistance > 200) {
                            stationList.add(station)
                        }
//                        }
                    }


                }

                val planLine =
                    if (utils.getUILang() == "zh")
                        Line(name = "${stationList.last().cnName}线", isUpAndDownInvert = false)
                    else
                        Line(name = "Line ${stationList.last().enName}", isUpAndDownInvert = false)
                if (stationList.size >= 2) {
                    for (station in stationList) {
                        planLine.upLineStation += "${station.id} "
                        planLine.downLineStation += "${station.id} "
                    }
                    val length = planLine.upLineStation.length
                    planLine.upLineStation = planLine.upLineStation.substring(0, length - 1)
                    planLine.downLineStation = planLine.downLineStation.substring(0, length - 1)
                    originLine = planLine
                    initLineInterval()
                    currentLineStationState = onNext
                    binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                    loadLine(planLine)
                } else {
                    utils.showMsg(resources.getString(R.string.plan_line_too_short_tip))
                }


            }

        })
    }


    // 当前路线编辑站点列表
    val lineEditorStationList = ArrayList<Station>()
    var lineEditorMode = ""

    // 当前路线编辑路线信息
    var lineEditorLineId = -1
    var lineEditorLineName = ""
    var lineEditorLineDirection = onUp

    var lineEditorUpLineStationListStr = ""
    var lineEditorDownLineStationListStr = ""

    /**
     * 初始化地图
     */
    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun initMap() {
        aMapView = binding.map
        aMapView.onCreate(null)
        aMap = aMapView.map

        aMap.isMyLocationEnabled = false
        aMap.isTrafficEnabled = utils.getIsMapTrafficEnabled()

        binding.mapContainer.setScrollView(binding.main)

        val markerMipmapIds = ArrayList<Int>()
        markerMipmapIds.add(R.mipmap.marker_gray)
        markerMipmapIds.add(R.mipmap.marker_blue)
        markerMipmapIds.add(R.mipmap.marker_green)

        markerMipmapIds.forEach {
            val overlayOptions =
                MultiPointOverlayOptions()
                    .icon(BitmapDescriptorFactory.fromResource(it))
            multiPointOverlayList.add(aMap.addMultiPointOverlay(overlayOptions)!!)
        }

        //标点点击事件
        aMap.setOnMultiPointClickListener {
            if (::aMapStationClickText.isInitialized) aMapStationClickText.remove()
            if (it.customerId == multiPointCustomerId) {
                multiPointCustomerId = ""
                return@setOnMultiPointClickListener true
            }
            multiPointCustomerId = it.customerId
            // 文本颜色
            val fontColor = getFontColor(multiPointCustomerId.toInt())

            val textOptions =
                TextOptions()
                    .text(it.title).fontColor(fontColor).position(it.latLng)
                    .fontSize(48)
                    .typeface(requireContext().resources.getFont(R.font.galano_grotesque_bold))
            aMapStationClickText = aMap.addText(textOptions)!!

            val stationId = currentLineStationList[multiPointCustomerId.toInt()].id ?: -1

            if (utils.getIsMapEditLineMode()) {
                if (currentLine.name == resources.getString(R.string.line_all)) {

                    if (lineEditorStationList.size >= 2)
                        binding.finishEditLine.visibility = VISIBLE

                    var findStationIndex = -1
                    lineEditorStationList.forEachIndexed { i, station ->
                        if (station.id == stationId) {
                            findStationIndex = i
                            return@forEachIndexed
                        }
                    }

                    val dialogBuilder =
                        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                            .setTitle(it.title)

                    // 站点已添加到路线
                    if (findStationIndex >= 0) {
                        dialogBuilder.setNeutralButton("删除", null)

                    }
                    // 站点不在路线中
                    else {
                        dialogBuilder.setNegativeButton("插入", null)
                        dialogBuilder.setPositiveButton("添加到末尾", null)
                    }

                    val dialog = dialogBuilder.show()

                    // 删除
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        var stationDel = Station()
                        val res = lineEditorStationList.removeIf { station ->
                            stationDel = station
                            station.id == stationId
                        }
                        if (res) {
                            utils.showMsg("${stationDel.cnName}[${stationDel.id}] 已删除")
                            refreshMarkerAndTrack()
                        } else {
                            utils.showMsg("该站点尚未添加到路线")
                        }
                        dialog.dismiss()
                    }

                    // 插入
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {

                        val stationNameList = java.util.ArrayList<String>()
                        lineEditorStationList.forEachIndexed { i, station ->
                            stationNameList.add("(${i + 1})${station.cnName}[${station.id}]")
                        }

                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.CustomAlertDialogStyle
                        ).setTitle("插入到站点之前")
                            .setSingleChoiceItems(
                                stationNameList.toTypedArray(), -1
                            ) { dialog, which ->

                                val stationList = stationDatabaseHelper.queryById(stationId)
                                if (stationList.isNotEmpty()) {
                                    val newStation = stationList.first()
                                    lineEditorStationList.add(which, newStation)
                                    utils.showMsg("${newStation.cnName}[${newStation.id}] 添加成功")
                                    refreshMarkerAndTrack()
                                } else {
                                    utils.showMsg("站点不存在")
                                }
                                dialog.dismiss()
                            }
                            .show()
                        dialog.dismiss()
                    }

                    // 添加到末尾
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val stationList = stationDatabaseHelper.queryById(stationId)
                        if (stationList.isNotEmpty()) {
                            val newStation = stationList.first()
                            lineEditorStationList.add(newStation)
                            utils.showMsg("${newStation.cnName}[${newStation.id}] 添加成功")
                            refreshMarkerAndTrack()
                        } else {
                            utils.showMsg("站点不存在")
                        }
                        dialog.dismiss()
                    }

                } else {
                    val dialog = MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.CustomAlertDialogStyle
                    )
                        .setTitle("要编辑路线，须切换到“${resources.getString(R.string.line_all)}”")
                        .setMessage("要现在切换吗？\n或者要退出编辑路线模式，\n请前往“设置”-“定位与地图”")
                        .setPositiveButton(requireContext().getString(android.R.string.ok), null)
                        .setNegativeButton(getString(android.R.string.cancel), null).show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
                        loadLineAll()
                        dialog.dismiss()
                    }
                }
            }

            return@setOnMultiPointClickListener true
        }

        //设置地图缩放比例
        aMap.animateCamera(CameraUpdateFactory.zoomTo(18F))

        //切换地图位置至初始位置
        aMap.animateCamera(CameraUpdateFactory.newLatLng(currentLngLat))

        //设置缩放按钮位于右侧中部
        val uiSettings = aMap.uiSettings
        uiSettings?.zoomPosition = AMapOptions.ZOOM_POSITION_RIGHT_CENTER
        uiSettings?.isScaleControlsEnabled = true
//        aMap.isMyLocationEnabled = true
        uiSettings?.isZoomControlsEnabled = false

        //高德logo设置在地图下方
        aMapUiSettings = aMap.uiSettings!!
        aMapUiSettings.logoPosition = AMapOptions.LOGO_POSITION_BOTTOM_CENTER

        // 触碰地图暂停报站
        binding.map.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            if (utils.getClickMapPauseAn()) {
                pauseAnnounce()
            }
            return@setOnTouchListener true
        }


        aMap.setOnMapClickListener {

            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnMapClickListener
            }

            //单击地图，复制经纬度坐标
            if (utils.getIsClickMapToCopyLngLat()) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("text", "${it.longitude} ${it.latitude}")
                clipboard.setPrimaryClip(clipData)
                utils.showMsg("${it.longitude}\n${it.latitude}")
            }

            //单击地图，添加站点
            if (utils.getIsClickMapToAddStation()) {
                utils.showStationDialog("new", latLng = it, isOrderLatLng = true)
            }

            // 单击地图，设置路线规划起点/终点
            if (utils.getIsLinePlanning()) {
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("")
                    .setNegativeButton("从这出发") { p0, p1 ->
                        planFrom = LatLng(it.latitude, it.longitude)
                        if (this::planFromMarker.isInitialized) planFromMarker.destroy()
                        planFromMarker = aMap.addMarker(
                            MarkerOptions().position(planFrom).title("起点")
                        )
                    }
                    .setPositiveButton("去这里") { p0, p1 ->
                        planTo = LatLng(it.latitude, it.longitude)
                        if (!this::planFrom.isInitialized) {
                            planFrom = currentLngLat
                            planFromMarker = aMap.addMarker(
                                MarkerOptions().position(planFrom).title("起点")
                            )
                        }
                        if (this::planToMarker.isInitialized) planToMarker.destroy()
                        planToMarker = aMap.addMarker(
                            MarkerOptions().position(planTo).title("终点")
                        )
                        // 开始规划路线
                        linePlan(planFrom, planTo)
                    }.setNeutralButton(resources.getString(android.R.string.cancel), null).show()

            }
        }

        aMap.setOnMapTouchListener {
            if (utils.getClickMapPauseAn()) {
                pauseAnnounce()
            }
        }

        // 每隔1s刷新地图Text
        mMapHandler.removeCallbacksAndMessages(null)
        mapRunnable = object : Runnable {
            override fun run() {

                mMapHandler.removeCallbacksAndMessages(null)

                if (!isAdded)
                    return

                // 获取地图缩放级别
                aMapLastZoom = aMapCurrentZoom
                aMapCurrentZoom = aMap.cameraPosition!!.zoom
                // 放大到一定级别
                if (aMapLastZoom < aMapZoomPoint && aMapCurrentZoom >= aMapZoomPoint) {
                    refreshMapStationText()
                }
                // 缩小到一定级别
                else if (aMapLastZoom >= aMapZoomPoint && aMapCurrentZoom < aMapZoomPoint) {
                    refreshMapStationText()
                }

                mMapHandler.postDelayed(this, 1000L)

            }
        }
        mMapHandler.postDelayed(mapRunnable, 1000L)

        setMapMode(utils.getMapType())
    }


    /**
     * 初始化通知
     */
    private fun initNotification() {

        if (hasInitNotice)
            return

        //初始化通知管理
        notificationManager =
            requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel =
            NotificationChannel("0", "路线运行中", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(notificationChannel)

        //初始化通知本体
        val intent = Intent(context, requireActivity()::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("switchToMainFrag", true)
        val pendingIntent = PendingIntent.getActivities(
            requireContext(),
            0,
            arrayOf(intent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notification =
            NotificationCompat.Builder(requireContext(), "0").setSmallIcon(R.mipmap.an)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())


        hasInitNotice = true

    }

    /**
     * 初始化路线
     */
    private fun initLine() {

//        binding.lineStationCard.visibility = ViewGroup.GONE

        //初始化路线站点卡片
        binding.lineStationList.setHasFixedSize(true)
        binding.lineStationList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = StationOfLineAdapter(
            requireContext(),
            ArrayList(),
            0
        )

        binding.lineStationList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

//                Log.d("", "line now show ${firstVisibleItem}-${lastVisibleItem}")
                adapter.firstVisibleItem = firstVisibleItem
                adapter.lastVisibleItem = lastVisibleItem

            }
        })

        binding.lineStationList.adapter = adapter
//        adapter.isScroll = true

        //单击切换区间起点/终点
        adapter.setOnItemClickListener(object : StationOfLineAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return
                }
                val chosenStationCname = currentLineStationList[position].cnName

                var currentLineStartingIndex = when (currentLineDirection) {
                    onUp -> currentUpLineStartingIndex
                    onDown -> currentDownLineStartingIndex
                    else -> 0
                }

                var currentLineTerminalIndex = when (currentLineDirection) {
                    onUp -> currentUpLineTerminalIndex
                    onDown -> currentDownLineTerminalIndex
                    else -> 0
                }

                // 上/下行线路
                val lineList: List<String> = when (currentLineDirection) {
                    onUp -> originLine.upLineStation.split(" ")
                    onDown -> originLine.downLineStation.split(" ")
                    else -> ArrayList()
                }

                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("将 $chosenStationCname 设为")
                    .setPositiveButton("区间起点", object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {

                            val startingId = currentLineStationList[position].id

                            currentLineStartingIndex = lineList.indexOf(startingId.toString())
                            if (currentLineStartingIndex == -1) currentLineStartingIndex = 0

                            if (currentLineTerminalIndex < currentLineStartingIndex || currentLineTerminalIndex == Int.MAX_VALUE) {
                                currentLineTerminalIndex = lineList.size - 1
                            }

                            if (currentLineTerminalIndex - currentLineStartingIndex < 1) {
                                utils.showMsg("不能将原终点站设置为始发站")
                                return
                            }

                            val newLine = Line()
                            newLine.id = originLine.id
                            newLine.name = originLine.name + " 区间"
                            newLine.isUpAndDownInvert = originLine.isUpAndDownInvert
                            when (currentLineDirection) {
                                onUp -> {
                                    newLine.downLineStation = currentLine.downLineStation
                                    newLine.upLineStation =
                                        lineList.slice(currentLineStartingIndex..currentLineTerminalIndex)
                                            .stream().map { n -> java.lang.String.valueOf(n) }
                                            .collect(Collectors.joining(" "))
                                }

                                onDown -> {
                                    newLine.upLineStation = currentLine.upLineStation
                                    newLine.downLineStation =
                                        lineList.slice(currentLineStartingIndex..currentLineTerminalIndex)
                                            .stream().map { n -> java.lang.String.valueOf(n) }
                                            .collect(Collectors.joining(" "))
                                }
                            }

                            loadLine(newLine)
                            utils.haptic(binding.headerMiddleNew)
//                            adapter.isScroll = true
                        }
                    })
                    .setNegativeButton("区间终点", object : DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {

                            val startingId = currentLineStationList[position].id

                            currentLineTerminalIndex = lineList.indexOf(startingId.toString())
                            if (currentLineTerminalIndex == -1) currentLineTerminalIndex =
                                lineList.size - 1

                            if (currentLineTerminalIndex < currentLineStartingIndex || currentLineStartingIndex == -1) {
                                currentLineStartingIndex = 0
                            }

                            if (currentLineTerminalIndex - currentLineStartingIndex < 1) {
                                utils.showMsg("不能将原始发站设置终点站")
                                return
                            }

                            val newLine = Line()
                            newLine.id = originLine.id
                            newLine.name = originLine.name + " 区间"
                            newLine.isUpAndDownInvert = originLine.isUpAndDownInvert
                            when (currentLineDirection) {
                                onUp -> {
                                    newLine.downLineStation = currentLine.downLineStation
                                    newLine.upLineStation =
                                        lineList.slice(currentLineStartingIndex..currentLineTerminalIndex)
                                            .stream().map { n -> java.lang.String.valueOf(n) }
                                            .collect(Collectors.joining(" "))
                                }

                                onDown -> {
                                    newLine.upLineStation = currentLine.upLineStation
                                    newLine.downLineStation =
                                        lineList.slice(currentLineStartingIndex..currentLineTerminalIndex)
                                            .stream().map { n -> java.lang.String.valueOf(n) }
                                            .collect(Collectors.joining(" "))
                                }
                            }

                            loadLine(newLine)
                        }
                    }).setNeutralButton("当前站点") { _, _ ->
                        setStationAndState(position, currentLineStationState)
                        refreshLineStationList()
                        utils.haptic(binding.lineStationList)
                    }.show()
            }
        })

        //切换为 上一次运行的路线 或 默认路线
        val sharedPreferences =
            requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)
        val lastRunningLineName = sharedPreferences.getString("lineName", "") ?: ""
        val onlineLineUpId = sharedPreferences.getString("onlineLineUpId", "") ?: ""
        val onlineLineDownId = sharedPreferences.getString("onlineLineDownId", "") ?: ""

        val localLineList = lineDatabaseHelper.queryByName(lastRunningLineName).toMutableList()
//        Log.d(tag, onlineLineUpId)
//        Log.d(tag, onlineLineDownId)

        // 获取云端路线
        if (localLineList.isEmpty()) {
            if (onlineLineUpId != "" && onlineLineDownId != "") {
                var hasLoad = false
                val onlineLine = Line()
                onlineLine.id = -1
                onlineLine.isUpAndDownInvert = false
                cloudStationList.clear()
                CoroutineScope(Dispatchers.IO).launch {
                    for (i in 0..1) {
                        // 在线搜索路线
                        val lineQuery = BusLineQuery(
                            if (i == 0) onlineLineUpId else onlineLineDownId,
                            BusLineQuery.SearchType.BY_LINE_ID,
                            utils.getCity()
                        )
                        lineQuery.pageNumber = 0
                        lineQuery.extensions = "all"
                        val busLineSearch = BusLineSearch(requireContext(), lineQuery)
                        busLineSearch.setOnBusLineSearchListener { res, rCode ->
//                utils.showMsg("setOnBusLineSearchListener${res.busLines.size}")
                            if (res.busLines.isNotEmpty()) {
//                            Log.d(tag, res.query.queryString)
                                if (i == 0) {
                                    val line = getOnlineLine(res, 0, false)
                                    onlineLine.upLineStation = line.upLineStation
                                    onlineLine.name = line.name
                                } else {
                                    onlineLine.downLineStation =
                                        getOnlineLine(res, 0, false).upLineStation
                                }

                                requireActivity().runOnUiThread {
                                    if (!hasLoad && onlineLine.upLineStation != "" && onlineLine.downLineStation != "") {
                                        originLine = onlineLine
                                        initLineInterval()
                                        currentLineStationState = onNext
                                        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                                        loadLine(onlineLine)
                                        utils.haptic(binding.headerMiddleNew)
                                        hasLoad = true
                                    }
                                }
                            }
                        }
                        busLineSearch.searchBusLineAsyn()
                    }
                }
            }
        } else {
//            utils.showMsg(localLineList.first().name)
            originLine = localLineList.first()
            initLineInterval()
            currentLineStationState = onNext
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
            loadLine(originLine)
            utils.haptic(binding.headerMiddleNew)
        }


    }

    var currentCityName = ""
    fun onMyLocationChange(location: AMapLocation) {

//        utils.showMsg(location.city)

        currentCityName = location.city

        val latStr = String.format(Locale.CHINA, "%.8f", location.latitude)
        val longStr = String.format(Locale.CHINA, "%.8f", location.longitude)

        locationInfoList.add(
            0,
            "[${
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            }] $latStr $longStr"
        )

        binding.locationBtnGroup.check(binding.locationBtn.id)

        //更新位置与时间
        lastTimeMillis = currentTimeMillis
        currentTimeMillis = System.currentTimeMillis()

        lastLngLat = currentLngLat
        currentLngLat = LatLng(location.latitude, location.longitude)

        // 更新地图
        CoroutineScope(Dispatchers.IO).launch {
            if (isAdded) {
                requireActivity().runOnUiThread {
                    aMap.stopAnimation()
                    aMap.animateCamera(CameraUpdateFactory.changeLatLng(lastLngLat))
                }
                Thread.sleep(250L)
                CoroutineScope(Dispatchers.Main).launch {
                    aMap.stopAnimation()
                    aMap.animateCamera(
                        CameraUpdateFactory.changeBearing(
                            sensorHelper.getAzimuth().toFloat()
                        )
                    )
                }
            }
        }

        //更新定位标点
        if (!this::locationMarker.isInitialized) {
            locationMarker = aMap.addMarker(
                MarkerOptions().position(currentLngLat).setFlat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.location_marker))
            )
            locationMarker.setAnchor(0.5F, 0.57F)
        }

        locationMarker.rotateAngle = -sensorHelper.getAzimuth().toFloat()

        val anim = TranslateAnimation(currentLngLat)
        anim.setDuration(100L)
        anim.setInterpolator(DecelerateInterpolator())
        locationMarker.setAnimation(anim)
        locationMarker.startAnimation()

//        Log.d("now latitude", location.latitude.toString())
//        Log.d("last local", (currentTimeMillis - lastTimeMillis).toString())
//        Log.d("bearing", location.bearing.toString())

        lastDistanceToCurrentStation = currentDistanceToCurrentStation
        currentDistanceToCurrentStation = utils.calculateDistance(
            currentLngLat.longitude,
            currentLngLat.latitude,
            currentLineStation.longitude,
            currentLineStation.latitude
        )


        // 距离格式化
        if (currentLine.name == "") {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                getString(R.string.main_distance_value)
        } else if (currentDistanceToCurrentStation >= 100000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation / 1000)
            binding.navStationDistanceValue.text =
                String.format(Locale.CHINA, "%.0f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 10000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.2f", currentDistanceToCurrentStation / 1000)
            binding.navStationDistanceValue.text =
                String.format(Locale.CHINA, "%.0f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 1000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.3f", currentDistanceToCurrentStation / 1000)
            binding.navStationDistanceValue.text =
                String.format(Locale.CHINA, "%.0f", currentDistanceToCurrentStation / 1000)
        } else {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.m)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation)
            binding.navStationDistanceValue.text =
                String.format(Locale.CHINA, "%.0f", currentDistanceToCurrentStation)
        }



        binding.navStationDistanceUnit.text =
            binding.currentDistanceToCurrentStationUnit.text.toString().replace("(", "")
                .replace(")", "")


        //更新速度
        val distance = utils.calculateDistance(
            currentLngLat.longitude,
            currentLngLat.latitude,
            lastLngLat.longitude,
            lastLngLat.latitude
        )

        currentSpeedKmH = if (currentSpeedKmH < 0) 0.0
        else (distance / 1000.0) / ((currentTimeMillis - lastTimeMillis) / 1000.0 / 60.0 / 60.0)

        binding.speedValue.text =
            String.format(Locale.CHINA, "%.1f", currentSpeedKmH)

        binding.navStationSpeedValue.text =
            String.format(Locale.CHINA, "%.0f", currentSpeedKmH)


        // 计算正向距离
        for (i in currentLineStationList.indices) {
            lastDistanceToStationList[i] = currentDistanceToStationList[i]
            currentDistanceToStationList[i] = utils.calculateDistance(
                currentLngLat.longitude,
                currentLngLat.latitude,
                currentLineStationList[i].longitude,
                currentLineStationList[i].latitude
            )
        }

        // 计算反向距离
        for (i in currentReverseLineStationList.indices) {
            reverseLastDistanceToStationList[i] = reverseCurrentDistanceToStationList[i]
            reverseCurrentDistanceToStationList[i] = utils.calculateDistance(
                currentLngLat.longitude,
                currentLngLat.latitude,
                currentReverseLineStationList[i].longitude,
                currentReverseLineStationList[i].latitude
            )
        }

        // todo 遍历当前方向路线所有站点，先遍历正向，如果没有符合的站点，再遍历反向（实验性）
        @Suppress("ControlFlowWithEmptyBody")
        if (!findMatchStation(false)) {
//            findMatchStation(true)
        }

    }


    /**
     * 遍历站点列表，检查是否符合进站、出站、即将到站条件，并切换站点然后报站
     * @return 当前站点是否更改
     */
    private fun findMatchStation(@Suppress("SameParameterValue") isReverseLine: Boolean): Boolean {

        matchCount = (matchCount + 1) % Int.MAX_VALUE
        if (matchCount < 2) return false

        val arriveStationDistance = utils.getStationRangeByLineType(currentLine.type)    // 进站临界距离
        val outStationDistance = arriveStationDistance * 3.0            // 出站临界距离
        val willArriveStationDistance = arriveStationDistance * 2.4     // 即将进站临界距离

        val lineStationList = when (isReverseLine) {
            true -> currentReverseLineStationList
            false -> currentLineStationList
        }

        val lastDistanceToStationList = when (isReverseLine) {
            true -> reverseLastDistanceToStationList
            false -> lastDistanceToStationList
        }

        val currentDistanceToStationList = when (isReverseLine) {
            true -> reverseCurrentDistanceToStationList
            false -> currentDistanceToStationList
        }

        for (i in lineStationList.indices) {
            //进站条件：现在定位在这个站点内
            if (currentDistanceToStationList[i] <= arriveStationDistance) {

                //当前站点及状态相同，直接返回
                if ((lineStationList[i].id == currentLineStation.id && currentLineStationState == onArrive)) {
                    return true
                }

                Log.d(
                    tag,
                    "自动到达站：${lineStationList[i].cnName} for ${currentDistanceToStationList[i]} <= $arriveStationDistance"
                )

                if (isReverseLine) {
                    reverseLineDirection()
                } else {
                    setStationAndState(i, onArrive)
                }

                // 自动切换线路方向
                if (utils.getIsAutoSwitchLineDirection() && currentLine.name != resources.getString(
                        R.string.line_all
                    )
                ) {
                    lineArriveStationIdList.add(i)
                    // 如果切换到了之前（已经过）的站点
                    if (lineArriveStationIdList.size > 1
                        && lineArriveStationIdList.last() < lineArriveStationIdList[lineArriveStationIdList.size - 2]
                    ) {
                        lineArriveStationIdList.clear()
                        reverseLineDirection()
                    }
                }

                announce()
                utils.longHaptic()
                return true
            }
            //即将进站条件：现在位于即将进站范围内，且现在不位于进站进站内
            else if (currentDistanceToStationList[i] <= willArriveStationDistance
                && currentDistanceToStationList[i] > arriveStationDistance
            ) {

                //当前站点及状态相同，直接返回
                if (((currentLineStationState == onWillArrive || currentLineStationState == onArrive))
                    && lineStationList[i].id == currentLineStation.id
                ) {
                    return true
                }

                Log.d(
                    tag,
                    "即将自动进站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )


                if (isReverseLine) {
                    reverseLineDirection()
                }
//                else if (currentLineStationState != onNext || lineStationList[i].id != currentLineStation.id) {
//                    setStationAndState(i, onNext)
//                }
                else {
                    setStationAndState(i, onWillArrive)
                }

                announce()
                utils.longHaptic()
                return true
            }
            //出站条件：上次位于某站点内，现在位于这个站点外（进站范围）
            else if (lastDistanceToStationList[i] < outStationDistance &&
                currentDistanceToStationList[i] > outStationDistance &&
                currentLine.name != resources.getString(
                    R.string.line_all
                )
            ) {

                Log.d(
                    tag,
                    "${lineStationList[i].cnName} 自动出站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )

                if (isReverseLine) {
                    reverseLineDirection()
                }

                // 上行终点站出站
                else if (currentLineDirection == onUp && i >= lineStationList.size - 1 && utils.getSwitchDirectionWhenOutFromTerminalWithOnUp()) {
                    reverseLineDirection()
                    setStationAndState(1, onNext)
                    announce()
                    utils.longHaptic()
                } else if (i < lineStationList.size - 1) {
                    setStationAndState(i + 1, onNext)
                    announce()
                    utils.longHaptic()
                }

                return true
            }
        }
        return false
    }


    val circleWithStationMap = HashMap<Int, Int>()   //<circleIndex, stationIndex>

    /**
     * 刷新路线标记和轨迹
     */
    private fun refreshMarkerAndTrack() {

        aMapView.onPause()

        //移除标点文字
        for (text in aMapStationTextList) {
            text.remove()
        }
        aMapStationTextList.clear()

        //移除标点点击文字
        if (::aMapStationClickText.isInitialized)
            aMapStationClickText.remove()

        showMarkerAndTrack()

        if (binding.mapBtn.isChecked)
            aMapView.onResume()

    }

    val mPolylineLatLngLists = ArrayList<ArrayList<LatLng>>()
    val multiPointLists = ArrayList<ArrayList<MultiPointItem>>()
    var traceResJsonStr = ""

    /**
     * 显示当前路线站点标记点和轨迹线（已通过站点标为绿色，不会移除原有标记）
     */
    private fun showMarkerAndTrack() {

//        Log.d(tag, "showCurrentLineStationMarker")

        val latLngList = ArrayList<LatLng>()

        multiPointLists.clear()
        repeat(3) {
            multiPointLists.add(ArrayList())
        }

        mPolylineLatLngLists.clear()
        repeat(3) {
            mPolylineLatLngLists.add(ArrayList())
        }

        for (i in currentLineStationList.indices) {

            val latLng = LatLng(
                currentLineStationList[i].latitude, currentLineStationList[i].longitude
            )
            latLngList.add(latLng)

            if (!circleWithStationMap.containsKey(i)) {
                val fillColor = if (aMap.mapType == MAP_TYPE_NIGHT)
                    Color.argb(8, 255, 255, 255)
                else
                    Color.argb(8, 0, 0, 0)
                //绘制站点范围圆
                val circle = aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(utils.getStationRangeByLineType(currentLine.type).toDouble())
                        .fillColor(fillColor)
                        .strokeWidth(0F)
                        .zIndex(-100F)
                )
                circleList.add(circle)
                circleWithStationMap[i] = i
            }

            //绘制站点标点
            val multiPointItem = MultiPointItem(latLng)

            // 文本内容
            var indexText: String
            val textContext =
                if (currentLine.name != resources.getString(R.string.line_all)) {
                    indexText = if (i < 9) "0${i + 1}"
                    else "${i + 1}"
                    "$indexText ${currentLineStationList[i].cnName}"
                } else {
                    indexText = if (utils.getIsMapEditLineMode()) {
                        var findStationIndex = -1
                        lineEditorStationList.forEachIndexed { findIndex, station ->
                            if (station.id == currentLineStationList[i].id) {
                                findStationIndex = findIndex
                                return@forEachIndexed
                            }
                        }
                        if (findStationIndex >= 0)
                            "(${findStationIndex + 1})"
                        else
                            ""
                    } else {
                        ""
                    }
                    "${indexText}${currentLineStationList[i].cnName}[${currentLineStationList[i].id!!}]"
                }


            multiPointItem.customerId = i.toString()
            multiPointItem.title = textContext

            if (currentLine.name != resources.getString(R.string.line_all)) {
                when (i) {
                    in 0 until currentLineStationCount ->
                        multiPointLists[0].add(multiPointItem)

                    currentLineStationCount ->
                        multiPointLists[1].add(multiPointItem)

                    in currentLineStationCount + 1 until currentLineStationList.size ->
                        multiPointLists[2].add(multiPointItem)
                }
            } else {

                if (utils.getIsMapEditLineMode()) {
                    val matchStation =
                        lineEditorStationList.find { station -> station.id == currentLineStationList[i].id }
                    if (matchStation == null)
                        multiPointLists[1].add(multiPointItem)
                    else
                        multiPointLists[2].add(multiPointItem)
                } else {
                    when (i) {

                        currentLineStationCount ->
                            multiPointLists[1].add(multiPointItem)

                        else ->
                            multiPointLists[2].add(multiPointItem)
                    }
                }

            }

            // 添加绘制线（不纠偏轨迹，不是全站路线）
            if (!utils.getIsLineTrajectoryCorrection() && currentLine.name != resources.getString(
                    R.string.line_all
                )
            ) {

                when (i) {
                    in 0 until currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(latLngList[i])
                    }

                    currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(latLngList[i])
                        if (currentLineStationState == onNext)
                            mPolylineLatLngLists[1].add(latLngList[i])
                        else
                            mPolylineLatLngLists[0].add(latLngList[i])
                    }

                    currentLineStationCount -> {
                        if (currentLineStationState == onNext)
                            mPolylineLatLngLists[1].add(latLngList[i])
                        else
                            mPolylineLatLngLists[0].add(latLngList[i])
                        mPolylineLatLngLists[2].add(latLngList[i])
                    }

                    in currentLineStationCount + 1 until currentLineStationList.size -> {
                        mPolylineLatLngLists[2].add(latLngList[i])
                    }
                }

            }

        }

        //提交标点
        if (multiPointOverlayList.size >= 3 && multiPointLists.size >= 3) {
            multiPointOverlayList[0].items = multiPointLists[0]
            multiPointOverlayList[1].items = multiPointLists[1]
            multiPointOverlayList[2].items = multiPointLists[2]
        }


        // 非全站路线
        if (currentLine.name != resources.getString(R.string.line_all)) {
            // 纠偏
            if (utils.getIsLineTrajectoryCorrection())
                drawLineTrace(traceResJsonStr)
            // 不纠偏
            else
                addMapLine()
        }
        // 全站路线
        else {
            // 编辑模式
            if (utils.getIsMapEditLineMode()) {
                for (station in lineEditorStationList) {
                    val latLng = LatLng(
                        station.latitude, station.longitude
                    )
                    mPolylineLatLngLists[2].add(latLng)
                }
                addMapLine()
            }
            // 非编辑模式
            else {
                // 不绘制轨迹
            }
        }


//        if (utils.getIsMapEditLineMode() && currentLine.name == resources.getString(R.string.line_all)) {
//            for (station in lineEditorStationList) {
//                val latLng = LatLng(
//                    station.latitude, station.longitude
//                )
//                mPolylineLatLngLists[2].add(latLng)
//            }
//            addMapLine()
//        }
//
//        if (utils.getIsLineTrajectoryCorrection() && currentLine.name != resources.getString(
//                R.string.line_all
//            )
//        ) {
//            drawLineTrace(traceResJsonStr)
//        }
//
//        if (!utils.getIsLineTrajectoryCorrection() &&
//            (currentLine.name != resources.getString(R.string.line_all) || utils.getIsMapEditLineMode())
//        ) {
//            addMapLine()
//        }

        // 绘制站点序号与名称
        for (i in currentLineStationList.indices) {

            val textOptions = TextOptions().text("")
                .position(
                    LatLng(
                        currentLineStationList[i].latitude, currentLineStationList[i].longitude
                    )
                )
                .fontSize(32)
                .typeface(requireContext().resources.getFont(R.font.galano_grotesque_bold))


            aMapStationTextList.add(aMap.addText(textOptions)!!)

        }

        refreshMapStationText()
    }

    /**
     * 上一站
     */
    private fun lastStation() {
        if (currentLineStation.id == null) return

        if (currentLineStationState == onNext || currentLineStationState == onWillArrive) {
            if (currentLineStationCount <= 0) return
            currentLineStationCount--
            currentLineStation = currentLineStationList[currentLineStationCount]
            currentLineStationState = onArrive
        } else if (currentLineStationState == onArrive) {
            currentLineStationState = onNext
        }

        refreshUI()
    }

    /**
     * 下一站
     */
    private fun nextStation(): Boolean {


        if (currentLineStation.id == null) return false
        if (currentLineStationState == onNext || currentLineStationState == onWillArrive) {
            currentLineStationState = onArrive
            refreshUI()
            return true
        } else if (currentLineStationState == onArrive) {
            if (currentLineStationCount >= currentLineStationList.size - 1) return false
            currentLineStationCount++
            currentLineStation = currentLineStationList[currentLineStationCount]
            currentLineStationState = onNext
            refreshUI()
            return true
        } else {
            return false
        }

    }

    /**
     * 切换站点及站点状态
     * @param stationCount 要切换到的站点在路线中的序号
     * @param stationState 要切换的站点状态
     */
    private fun setStationAndState(stationCount: Int, stationState: Int) {

        if (stationCount < 0 || stationCount >= currentLineStationList.size) return

        currentLineStationState = stationState

        currentLineStation = currentLineStationList[stationCount]
        currentLineStationCount = stationCount

        refreshUI()

        //设置高亮度15s屏幕唤醒锁
        wakeLock.acquire(15 * 1000L)
    }

    /**
     * 切换为最近站点（不报站）
     */
    private fun switchToNearestStation() {
        if (currentLineStationList.isNotEmpty()) {
            var minDistance = Double.MAX_VALUE
            var distance: Double
            for (i in currentLineStationList.indices) {
                distance = utils.calculateDistance(
                    currentLngLat.longitude,
                    currentLngLat.latitude,
                    currentLineStationList[i].longitude,
                    currentLineStationList[i].latitude
                )
                if (distance < minDistance) {
                    minDistance = distance
                    currentLineStationCount = i
                    currentLineStation = currentLineStationList[i]
                }
            }
        } else {
            currentLineStationCount = 0
            currentLineStation = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
        }


    }

    /**
     * 更新路线站点显示、小卡片和通知
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun refreshLineStationList() {

        val currentStationStateText = when (currentLineStationState) {
            onNext -> requireContext().resources.getString(R.string.next)
            onWillArrive -> requireContext().resources.getString(R.string.will_arrive)
            onArrive -> requireContext().resources.getString(R.string.arrive)
            else -> ""
        }

        binding.currentStationState.text = currentStationStateText
        val stationName = if (utils.getUILang() == "zh")
            currentLineStation.cnName
        else
            currentLineStation.enName

        binding.currentStationName.text = stationName
        binding.navStationName.showText(stationName)
        binding.navStationName.requestLayout()

        binding.navStationSign.text = when (currentLineStationState) {
            onNext -> "→"
            onWillArrive -> "↘"
            onArrive -> "↓"
            else -> ""
        }


        //路线卡片滚动到当前站点
        binding.lineStationList.post {

            try {
                val manager = binding.lineStationList.layoutManager as LinearLayoutManager
                val adapter = binding.lineStationList.adapter as StationOfLineAdapter
                adapter.stationCount = currentLineStationCount
                adapter.stationState = currentLineStationState
                adapter.notifyDataSetChanged()

                val layout = LayoutInflater.from(requireContext())
                    .inflate(
                        R.layout.item_station_of_line, null
                    )
                val stationIndexView = layout.findViewById<TextView>(R.id.station_index)
                val lineHeight = stationIndexView.lineHeight

                manager.scrollToPositionWithOffset(
                    currentLineStationCount,
                    binding.lineStationList.width / 2 - (lineHeight - TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        2F, Resources.getSystem().displayMetrics
                    ).toInt() * 2)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        //更新通知
        if (utils.getNotice()) {

            initNotification()

            if (!permissionManager.hasNoticePermission()) {
                permissionManager.requestNoticePermission()
                utils.showMsg("要接收运行信息，请授予通知权限")
                utils.showMsg("不希望接收运行信息，请前往“设置”-“系统与电显”关闭")
            }

            if (currentLine.name == getString(R.string.line_all)) {
                notification.setContentTitle(currentLine.name)
                    .setContentText(
                        if (utils.getUILang() == "zh")
                            "${binding.currentStationState.text} ${currentLineStation.cnName}"
                        else
                            "${binding.currentStationState.text} ${currentLineStation.enName}"
                    )
            } else if (currentLineStationList.isNotEmpty()) {
                notification.setContentTitle(
                    if (utils.getUILang() == "zh")
                        "${currentLine.name} ${currentLineStationList.first().cnName} - ${currentLineStationList.last().cnName}"
                    else
                        "${currentLine.name} ${currentLineStationList.last().enName}"
                )
                    .setContentText(
                        if (utils.getUILang() == "zh")
                            "${binding.currentStationState.text} ${currentLineStation.cnName}"
                        else
                            "${binding.currentStationState.text} ${currentLineStation.enName}"
                    )

            } else {
                notification.setContentTitle("临时路线")
                    .setContentText(currentLine.name)
            }
            notification.setWhen(System.currentTimeMillis())
            notificationManager.notify(0, notification.build())
        }

    }

    /**
     * 更新路线站点更新信息
     */
    private fun refreshLineStationChangeInfo() {

        // De
//
//        var newInfo = ""
//
//        val dateFormat = SimpleDateFormat("[HH:mm:ss] ", Locale.getDefault())
//        newInfo += dateFormat.format(Date(System.currentTimeMillis()))
//
//        when (currentLineStationState) {
//            onArrive -> newInfo += "${resources.getString(R.string.arrive)} "
//            onWillArrive -> newInfo += "${resources.getString(R.string.will_arrive)} "
//            onNext -> newInfo += "${resources.getString(R.string.next)} "
//        }
//        newInfo += if (utils.getUILang() == "zh")
//            currentLineStation.cnName
//        else
//            currentLineStation.enName
//        newInfo += "\n"
//
//        binding.lineStationChangeInfo.text =
//            binding.lineStationChangeInfo.text as String + newInfo


        val stationName = if (utils.getUILang() == "zh")
            currentLineStation.cnName
        else
            currentLineStation.enName

        val terminalName = if (currentLineStationList.isNotEmpty()) {
            if (utils.getUILang() == "zh")
                currentLineStationList.last().cnName
            else
                currentLineStationList.last().enName
        } else {
            ""
        }

        runningInfoList.add(
            RunningInfo(
                LocalTime.now(),
                currentLine.id ?: -1,
                currentLineStation.id ?: -1,
                currentLine.name,
                terminalName,
                stationName,
                currentLineStationState
            )
        )

    }

    /**
     * 立即刷新电显，并切换到站点状态和位置（如果有）
     */
    private fun refreshEsToStation() {
        refreshEs(toStation = true)
    }

    /**
     * 立即刷新电显，并切换到首末站显示（如果有）
     */
    private fun refreshEsToStaringAndTerminal() {
        refreshEs(toStaringAndTerminal = true)
    }

    class PcmWithInfo(
        var data: ByteArray?,
        var pcmEncoding: Int,
        var sampleRate: Int,
        /**
         * AudioFormat.CHANNEL_OUT_STEREO（双声道）
         * or
         * AudioFormat.CHANNEL_OUT_MONO（单声道） */
        var channelMask: Int,
        var durationUs: Long,
        var fileIndex: Int
    )


    /**
     * 语音播报
     * @param format 播报格式
     */
    fun announce(format: String = "") {

        if (!isAdded)
            return

        //如果没有管理外部存储的权限，请求授予
        if (!utils.isGrantManageFilesAccessPermission()) {
            utils.requestManageFilesAccessPermission(requireActivity())
            return
        }

        val filePathList = ArrayList<String>()

        val pcmQueue = ArrayBlockingQueue<PcmWithInfo>(1024 * 1024)

        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
        isAnnouncing = false

        if (::audioPlayScope.isInitialized)
            audioPlayScope.cancel()

        // 音频读取与解码
        audioStreamScope = CoroutineScope(Dispatchers.IO).launch {

            if (currentLineStationList.isEmpty()) {
                pauseAnnounce()
                return@launch
            }

            val mediaList = ArrayList<String>()
            val stationType = when (currentLineStationCount) {
                0 -> "Starting"
                1 -> "Second"
                currentLineStationList.size - 1 -> "Terminal"
                else -> "Default"
            }
            val stationState = when (currentLineStationState) {
                onArrive -> "Arrive"
                onNext -> "Next"
                onWillArrive -> "WillArrive"
                else -> ""
            }

            val anExps =
                if (format == "") utils.getAnnouncementFormat(stationState, stationType)
                else format

            if (anExps == "") {
                pauseAnnounce()
                return@launch
            }

            val anExpList = anExps.split("\n")
            val chooseIndex = Random.nextInt(0, anExpList.size)
            val anExp = anExpList[chooseIndex]
//            Log.d("anExp", anExp)

            val anList = utils.getAnnouncements(anExp)
            for (item in anList) {
                if (item == "") {
//                utils.showMsg("请到\"设置\"-\"语音播报库\"设置报站内容")
                    pauseAnnounce()
                    return@launch
                } else if (item[0] == '<') {
                    when (item) {
                        in listOf(
                            "<line>",
                            "<year>",
                            "<years>",
                            "<month>",
                            "<date>",
                            "<hour>",
                            "<minute>",
                            "<second>"
                        ) -> {
                            val str = when (item) {
                                "<line>" -> currentLine.name
                                "<year>" -> LocalDate.now().year.toString()
                                "<years>" -> (LocalDate.now().year % 100).toString()
                                "<month>" -> LocalDate.now().monthValue.toString()
                                "<date>" -> LocalDate.now().dayOfMonth.toString()
                                "<hour>" -> LocalTime.now().hour.toString()
                                "<minute>" -> LocalTime.now().minute.toString()
                                "<second>" -> LocalTime.now().second.toString()
                                else -> ""
                            }
                            mediaList.addAll(utils.getNumOrLetterVoiceList(str))
                        }

                        "<time>" -> {
                            mediaList.addAll(utils.getTimeVoiceList())
                        }

                        "<speed>" -> {
                            mediaList.addAll(
                                utils.intOrLetterToCnReading(
                                    currentSpeedKmH.toInt().toString(),
                                    "cn/number/"
                                )
                            )
                        }

                        else -> {
                            val station = when (item.substring(1, 3)) {
                                "ns" -> currentLineStation
                                "ss" -> currentLineStationList.first()
                                "ts" -> currentLineStationList.last()
                                "ms" -> {
                                    val stationList =
                                        stationDatabaseHelper.queryById(
                                            (item.substring(
                                                5,
                                                item.length - 1
                                            )).toInt()
                                        )
                                    if (stationList.isNotEmpty())
                                        stationList.first()
                                    else Station(
                                        id = Int.MAX_VALUE,
                                        cnName = "未知站点",
                                        enName = "unknown"
                                    )
                                }

                                else -> Station()
                            }
                            val lang = if (item.substring(1, 3) == "ms") {
                                item.substring(3, 5)
                            } else
                                item.drop(3).dropLast(1)
                            when (lang) {
                                "cn" ->
                                    mediaList.add("/${lang}/station/" + station.cnName)

                                "en" ->
                                    mediaList.add("/${lang}/station/" + station.enName)

                                else ->
                                    mediaList.add(
                                        "/${lang}/station/" + utils.getStationNameFromCn(
                                            station.cnName,
                                            lang
                                        )
                                    )
                            }
                        }
                    }
                } else {
                    var hasLocalVoice = false
                    for (lang in announcementLangList) {
                        val file =
                            File("$appRootPath/Media/${utils.getAnnouncementLibrary()}/${lang}/common")
                        val fileList = file.walk()
                            .filter { it.isFile && it.nameWithoutExtension == item }
                            .toList()
                        if (fileList.isNotEmpty()) {
                            mediaList.add("/${lang}/common/" + item)
                            hasLocalVoice = true
                            break
                        }
                    }
                    if (!hasLocalVoice) {
                        mediaList.add("common/$item")
                    }
                }
            }

            //新建缓存文件目录
            val tempFilePath = requireContext().getExternalFilesDir("")?.path

            File(tempFilePath!!).mkdirs()

            val utteranceIdDoneList = ArrayList<String>()

            if (utils.getIsUseTTS()) {

                File("$tempFilePath/tts").walkTopDown().forEach {
                    it.delete()
                }
                File("$tempFilePath/tts").mkdirs()

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String?) {
                    }

                    override fun onDone(utteranceId: String) {
                        utteranceIdDoneList.add(utteranceId)
                    }

                    override fun onError(utteranceId: String?) {
                        utils.showMsg("TTS合成异常，请检查系统设置")
                    }

                })

            }


            val ttsTextList = ArrayList<String>()
            // 查找本地音频/合成TTS音频
            val supportMediaFormatList = listOf("mp3", "wav", "ogg", "aac", "flac")
            for (voice in mediaList) {

                var localFile = File("")
                for (format in supportMediaFormatList) {
                    val file =
                        File("$appRootPath/Media/${utils.getAnnouncementLibrary()}/${voice}.${format}")
                    if (file.exists()) {
                        localFile = file
                        break
                    }
                }

                // 不存在本地音频
                if (localFile.path == "") {
                    // 启用TTS，合成TTS音频
                    if (utils.getIsUseTTS()) {
                        val text = voice.split('/').last()
//                        Log.d(tag, text)

                        val ttsFileName = "${text}.wav"
                        val ttsFile = File("$tempFilePath/tts/", ttsFileName)
                        if (!ttsTextList.contains(text)) {
                            ttsFile.getParentFile()?.mkdirs()
                            ttsFile.createNewFile()
                            val params = Bundle().apply {
                                putString(
                                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                    ttsFileName
                                )
                            }
                            tts.synthesizeToFile(
                                text,
                                params,
                                ttsFile,
                                ttsFile.path
                            )
                            ttsTextList.add(text)
                        }
                        filePathList.add(ttsFile.path)
                    }
                    // 存在本地音频
                } else {
                    filePathList.add(localFile.path)
                }

            }

//            for (file in filePathList)
//                Log.d(tag, file)


            audioReleaseHandler.removeCallbacksAndMessages(null)

//            if (::audioTrack.isInitialized) {
//                audioTrack.release()
//            }


            // 音频解码
            filePathList.forEachIndexed { i, filePath ->

                if (!isActive) {
                    pauseAnnounce()
                    return@launch
                }

                var pcmBytes: ByteArray? = null

                // 等待TTS合成完成
                if (filePath.split("/").reversed()[1] == "tts") {

                    while (true) {
                        if (!isActive) {
                            pauseAnnounce()
                            return@launch
                        }
//                        Thread.sleep(50)
                        if (utteranceIdDoneList.contains(filePath)) {
//                            Log.d(tag, "filePath ok $filePath")
                            break
                        }
                    }
                }

                var sampleRate = 0
                var channelCount = 0
                var pcmEncoding = 0
                var durationUs = 0L

                var decoder = MediaCodec.createDecoderByType("audio/mpeg")
                val extractor = MediaExtractor().apply {
//                    Log.d(tag, "filePath: $filePath")
                    try {
                        setDataSource(filePath)
                    } catch (e: Exception) {
                        Log.d(tag, "setDataSource Error")
                        e.printStackTrace()
                        pauseAnnounce()
                        return@launch
                    }

                    for (i in 0 until trackCount) {
                        val format = getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME)
                        if (mime?.startsWith("audio/") == true) {
                            selectTrack(i)
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            // 非pcm音频流无法获取KEY_PCM_ENCODING，默认16BIT
                            pcmEncoding = try {
                                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            } catch (_: Exception) {
                                AudioFormat.ENCODING_PCM_16BIT
                            }
                            durationUs = format.getLong(MediaFormat.KEY_DURATION)

                            decoder = MediaCodec.createDecoderByType(mime).apply {
                                configure(format, null, null, 0)
                                start()
                            }
                            break
                        }
                    }
                }


                val channelMask = if (channelCount == 2) {
                    AudioFormat.CHANNEL_OUT_STEREO  // 双声道
                } else {
                    AudioFormat.CHANNEL_OUT_MONO  // 单声道
                }

                @Suppress("DEPRECATION")
                val inputBuffers = decoder.inputBuffers

                @Suppress("DEPRECATION")
                val outputBuffers = decoder.outputBuffers

                val info = MediaCodec.BufferInfo()
                var eosReceived = false


                while (!eosReceived) {

                    if (!isActive) {
                        pauseAnnounce()
                        return@launch
                    }

                    // 输入
                    val inputBufferId = decoder.dequeueInputBuffer(100000)
                    if (inputBufferId >= 0) {
                        val buffer = inputBuffers[inputBufferId]
                        val sampleSize = extractor.readSampleData(buffer, 0)

                        if (sampleSize < 0) {
                            // 文件结束
                            decoder.queueInputBuffer(
                                inputBufferId,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            eosReceived = true
                        } else {
                            decoder.queueInputBuffer(
                                inputBufferId,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }

                    // 输出
                    val outIndex = decoder.dequeueOutputBuffer(info, 100000)
                    when (outIndex) {

                        // 输出缓冲区已更改
                        @Suppress("DEPRECATION")
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
//                            Log.d(tag, "INFO_OUTPUT_BUFFERS_CHANGED")
                        }

                        // 输出格式已更改
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
//                            Log.d(tag, "INFO_OUTPUT_FORMAT_CHANGED")
                        }

                        // 暂时没有可用输出
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
//                            Log.d(tag, "INFO_TRY_AGAIN_LATER")
                        }

                        else -> {
                            if (outIndex >= 0) {
//                                Log.d(tag, "outIndex $filePath $i")
                                val outputBuffer: ByteBuffer
                                try {
                                    outputBuffer = outputBuffers[outIndex]
                                    val pcmData = ByteArray(info.size)
                                    outputBuffer.get(pcmData)

                                    if (pcmBytes == null) {
                                        pcmBytes = pcmData
                                    } else {
                                        pcmBytes += pcmData
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                if (pcmBytes != null && pcmBytes.size > 64) {
                                    pcmQueue.add(
                                        PcmWithInfo(
                                            pcmBytes,
                                            pcmEncoding,
                                            sampleRate,
                                            channelMask,
                                            durationUs,
                                            i
                                        )
                                    )
                                    pcmBytes = null
                                }

                                decoder.releaseOutputBuffer(outIndex, false)

                            }
                        }
                    }

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }

                }

//                Log.d(tag, "pcm ok ${filePath}")

//                Log.d(tag, "finish $filePath $i")

                if (pcmBytes != null) {
                    pcmQueue.add(
                        PcmWithInfo(
                            pcmBytes,
                            pcmEncoding,
                            sampleRate,
                            channelMask,
                            durationUs,
                            i
                        )
                    )
                }

            }

        }

        if (::audioTrack.isInitialized)
            audioTrack.release()

        // 音频推流
        audioPlayScope = CoroutineScope(Dispatchers.IO).launch {

            var hasInitAudioTrack = false
            var hasPost = false
            val hasShowSubtitleMap = HashMap<Int, Boolean>() // <fileIndex, hasShow>

//            audioManager?.requestAudioFocus(audioFocusRequest!!)
            isAnnouncing = true

            while (isActive) {

//                Log.d(tag, "pcmQueue ${pcmQueue.size}")

                val pcm = pcmQueue.poll()
                if (pcm != null) {
//                    Log.d(tag, "play ${filePathList[pcm.fileIndex]} ${pcm.sampleRate}")


                    // 初始化audioTrack，或当音频格式变化时重建audioTrack
                    if (!hasInitAudioTrack ||
                        audioTrack.audioFormat != pcm.pcmEncoding ||
                        audioTrack.sampleRate != pcm.sampleRate ||
                        audioTrack.channelConfiguration != pcm.channelMask
                    ) {

                        hasInitAudioTrack = true

                        audioFormat = AudioFormat.Builder().setEncoding(pcm.pcmEncoding)
                            .setSampleRate(pcm.sampleRate).setChannelMask(pcm.channelMask)
                            .build()

                        bufferSizeInBytes = AudioTrack.getMinBufferSize(
                            pcm.sampleRate, pcm.channelMask, pcm.pcmEncoding
                        )


                        if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                            audioTrack.release()
                        }

                        audioTrack = AudioTrack(
                            audioAttributes,
                            audioFormat,
                            bufferSizeInBytes,
                            AudioTrack.MODE_STREAM,
                            1
                        )

                        audioTrack.play()

                    }

                    if (utils.getAnSubtitle()) {
                        if (!hasShowSubtitleMap.containsKey(pcm.fileIndex) || hasShowSubtitleMap[pcm.fileIndex] == false) {

                            requireActivity().runOnUiThread {
                                val fileName = filePathList[pcm.fileIndex].split('/').last()
                                val lastDotIndex = fileName.lastIndexOf(".")
                                utils.showMsg(
                                    fileName.substring(0, lastDotIndex), true
                                )
                            }
                            hasShowSubtitleMap[pcm.fileIndex] = true

                        }
                    }

                    if (pcm.fileIndex == filePathList.size - 1 && !hasPost) {
//                        Log.d(tag, "play finish")
                        audioReleaseHandler.removeCallbacksAndMessages(null)
                        audioReleaseHandler.postDelayed({
                            requireActivity().runOnUiThread {
                                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                                binding.stopAnnouncement.visibility = GONE
                                audioPlayScope.cancel()
                            }
                            isAnnouncing = false
                        }, pcm.durationUs / 1000 + 500) //附加500ms延迟
                        hasPost = true
                    } else {
                        requireActivity().runOnUiThread {
                            audioManager?.requestAudioFocus(audioFocusRequest!!)
                            binding.stopAnnouncement.visibility = VISIBLE
                        }

                    }

                    synchronized(audioTrack) {
                        if (pcm.data != null && audioTrack.state == AudioTrack.STATE_INITIALIZED
                            && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING
                        ) {
                            audioTrack.write(pcm.data!!, 0, pcm.data!!.size)
//                        Log.d(tag, "play ${wl}/${pcm.data!!.size}")
                        }
                    }


                }
            }

        }
    }


    /**
     * 刷新地图站点标记文本
     */
    private fun refreshMapStationText() {
        aMapView.onPause()
        for (i in aMapStationTextList.indices) {
            // 文本颜色
            val fontColor = getFontColor(i)

            // 文本序号/ID
            val indexOrId = if (currentLine.name != resources.getString(R.string.line_all)) {
                if (i < 9) "0${i + 1}"
                else "${i + 1}"
            } else {
                currentLineStationList[i].id!!.toString()
            }

            // 文本内容
            var indexText: String
            val textContext = if (aMap.cameraPosition!!.zoom > aMapZoomPoint) {
                if (currentLine.name != resources.getString(R.string.line_all)) {
                    indexText = if (i < 9) "0${i + 1}"
                    else "${i + 1}"
                    "$indexText ${currentLineStationList[i].cnName}"
                } else {
                    indexText = if (utils.getIsMapEditLineMode()) {
                        var findStationIndex = -1
                        lineEditorStationList.forEachIndexed { findIndex, station ->
                            if (station.id == currentLineStationList[i].id) {
                                findStationIndex = findIndex
                                return@forEachIndexed
                            }
                        }
                        if (findStationIndex >= 0)
                            "(${findStationIndex + 1})"
                        else
                            ""
                    } else {
                        ""
                    }
                    "${indexText}${currentLineStationList[i].cnName}[${currentLineStationList[i].id!!}]"
                }
            } else {
                indexOrId
            }

            // 文本框背景
            val backgroundColor = if (aMap.cameraPosition!!.zoom > aMapZoomPoint) {
                Color.WHITE
            } else {
                Color.TRANSPARENT
            }

            // 应用Text
            aMapStationTextList[i].fontColor = fontColor
            aMapStationTextList[i].text = textContext
            aMapStationTextList[i].backgroundColor = backgroundColor
        }
        if (binding.mapBtn.isChecked)
            aMapView.onResume()
    }

    fun initLineInterval() {
        currentUpLineStartingIndex = -1
        currentUpLineTerminalIndex = Int.MAX_VALUE
        currentDownLineStartingIndex = -1
        currentDownLineTerminalIndex = Int.MAX_VALUE
    }

    /**
     * 开始规划路线
     * @param form 地点经纬度
     * @param to 终点经纬度
     */
    private fun linePlan(form: LatLng, to: LatLng) {
//        Log.d(tag, "${form.latitude} -> ${to.latitude}")
//        Log.d(tag, "${form.longitude} -> ${to.longitude}")
        val fromPoint = LatLonPoint(form.latitude, form.longitude)
        val toPoint = LatLonPoint(to.latitude, to.longitude)
        val query = RouteSearch.RideRouteQuery(
            RouteSearch.FromAndTo(fromPoint, toPoint),
        )
        CoroutineScope(Dispatchers.IO).launch {
            @Suppress("DEPRECATION")
            mRouteSearch.calculateRideRouteAsyn(query)
        }
    }

    /**
     * 按正则表达式匹配线路列表，并弹出路线选择Dialog
     */
    private fun getMatchedLines(
        lineList: List<Line>,
        reg: Regex,
    ): ArrayList<Line> {

        val matchLineList = ArrayList<Line>()

        lineList.forEach { line ->
            if (reg.matches(line.name)) {
                matchLineList.add(line)
            }
        }

        val numStartReg = "^(\\d+.*)$".toRegex()
        val numReg = "\\d+".toRegex()
        val comparator = Comparator { line1: Line, line2: Line ->
            if (numStartReg.matches(line1.name) && numStartReg.matches(line2.name))
                numReg.find(line1.name)!!.value.toInt() - numReg.find(line2.name)!!.value.toInt()
            else
                Int.MAX_VALUE
        }

        val sortedMatchLineList = ArrayList<Line>()
        sortedMatchLineList.addAll(matchLineList.sortedWith(comparator))
        return sortedMatchLineList

    }

    private fun showLinesChoosesDialog(
        sortedMatchLineList: ArrayList<Line>,
        type: Int
    ) {

        val title = when (type) {
            0 -> resources.getString(R.string.line_normal_bus)
            1 -> resources.getString(R.string.line_comm_bus)
            2 -> resources.getString(R.string.line_metro)
            3 -> resources.getString(R.string.line_train)
            4 -> resources.getString(R.string.line_other)
            5 -> resources.getString(R.string.line_all)
            else -> ""
        }

        val lineInfoList =
            arrayOfNulls<String>(sortedMatchLineList.size)

        for (i in sortedMatchLineList.indices) {
            val lineStationIndexListStr =
                sortedMatchLineList[i].upLineStation.split(' ')

            val lineStartingStation =
                stationDatabaseHelper.queryById(
                    lineStationIndexListStr.first().toInt()
                )
            val lineTerminal =
                stationDatabaseHelper.queryById(
                    lineStationIndexListStr.last().toInt()
                )

            val lineStartingStationCnName =
                if (lineStartingStation.isNotEmpty()) lineStartingStation.first().cnName
                else "-"

            val lineTerminalCnName =
                if (lineTerminal.isNotEmpty()) lineTerminal.first().cnName
                else "-"

            lineInfoList[i] =
                "${sortedMatchLineList[i].name}  $lineStartingStationCnName - $lineTerminalCnName"
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
            .setTitle(title)
            .setItems(lineInfoList) { _, which ->
                if (lineInfoList[which] != "") {
                    originLine = sortedMatchLineList[which]
                    initLineInterval()
                    currentLineStationState = onNext
                    binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                    loadLine(sortedMatchLineList[which])
                    utils.haptic(binding.headerMiddleNew)
                }
            }.create()
            .show()
    }

    private fun searchLine(
        key: String,
        dialogBinding: DialogLineSwitchBinding,
        alertDialog: AlertDialog
    ) {


        val comparator = utils.getDefaultLineComparator()

        val res = ArrayList(lineDatabaseHelper.queryByKey(key).sortedWith(comparator))

//        val lineNameList = res.map { it.name }
        val lineInfoList = ArrayList(res.map {
            val lineStationIndexListStr =
                it.upLineStation.split(' ')

            val lineStartingStation =
                stationDatabaseHelper.queryById(
                    lineStationIndexListStr.first().toInt()
                )
            val lineTerminal =
                stationDatabaseHelper.queryById(
                    lineStationIndexListStr.last().toInt()
                )

            val lineStartingStationCnName =
                if (lineStartingStation.isNotEmpty()) lineStartingStation.first().cnName
                else "-"

            val lineTerminalCnName =
                if (lineTerminal.isNotEmpty()) lineTerminal.first().cnName
                else "-"

            return@map "${it.name}  $lineStartingStationCnName - $lineTerminalCnName"

        })

        lineInfoList.add("在线搜索${utils.getCity()} ${dialogBinding.lineNameInput.text} 路/线")
        lineInfoList.add("设为临时路线 ${dialogBinding.lineNameInput.text} 路/线")


//        val adapter =
//            ArrayAdapter(
//                requireActivity(),
//                android.R.layout.simple_list_item_1,
//                lineInfoList
//            )

        //new
        val newAdapter = LineOfSearchAdapter(requireContext(), res)
        newAdapter.setOnItemClickListener(object : LineOfSearchAdapter.OnItemClickListener {
            override fun onItemClick(line: Line) {
                originLine = line
                initLineInterval()
                currentLineStationState = onNext
                binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                loadLine(line)
                utils.haptic(dialogBinding.root)
                alertDialog.cancel()
            }
        })
        dialogBinding.lineListNew.adapter = newAdapter
    }


    /**
     * 更改地图模式
     * @param mode 地图模式标识
     * 根据系统UI选择普通或黑夜普通地图，值为0；
     * MAP_TYPE_NORMAL：普通地图，值为1；
     * MAP_TYPE_SATELLITE：卫星地图，值为2；
     * MAP_TYPE_NIGHT 黑夜地图，夜间模式，值为3；
     * MAP_TYPE_NAVI 导航模式，值为4;
     * MAP_TYPE_BUS 公交模式，值为5。
     */
    fun setMapMode(mode: Int) {
        if (!this::aMap.isInitialized)
            return
        if (mode > 0) {
            aMap.mapType = mode
        } else {
            val uiModeManager =
                requireContext().getSystemService(UI_MODE_SERVICE) as UiModeManager
            if (uiModeManager.nightMode == MODE_NIGHT_YES)
                aMap.mapType = MAP_TYPE_NIGHT
            else
                aMap.mapType = MAP_TYPE_NORMAL
        }
    }

    fun pauseAnnounce() {

        binding.stopAnnouncement.visibility = GONE

        simRunningHandler.removeCallbacksAndMessages(null)

        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
        isAnnouncing = false

        if (::audioPlayScope.isInitialized)
            audioPlayScope.cancel()

        if (::audioStreamScope.isInitialized)
            audioStreamScope.cancel()

        if (::audioTrack.isInitialized)
            audioTrack.release()

    }

    fun refreshEs(toStation: Boolean = false, toStaringAndTerminal: Boolean = false) {

        if (!isAdded)
            return

//        speedRefreshHandler.removeCallbacksAndMessages(null)


//        if (esPlayIndex >= 0 && esPlayIndex < esList.size)
//            Log.d(tag, "refreshEs: $esPlayIndex / ${esList.size} ${esList[esPlayIndex].leftText}")
//        else
//            Log.d(tag, "refreshEs: $esPlayIndex / ${esList.size}")


        if (esPlayIndex == -1 && esList.isNotEmpty()) {
            esPlayIndex = 0
        }


        if (esList.isNotEmpty()) {
            // 切换到首末站显示
            if (toStaringAndTerminal) {
                var frontDefaultItemIndex = -1
                var hasB = false
                for (i in 0 until esList.size) {
                    // 寻找非特定状态显示内容
                    if (utils.extractNWA(
                            Regex("[NWASCT]"),
                            esList[i].type
                        ) == "" && frontDefaultItemIndex == -1
                    ) {
                        frontDefaultItemIndex = i
                    }
                    // 寻找首末站内容
                    if (esList[i].type.contains("B")) {
                        esPlayIndex = i
                        hasB = true
                        break
                    }
                }
                if (!hasB) {
                    esPlayIndex = if (frontDefaultItemIndex >= 0) {
                        frontDefaultItemIndex
                    } else {
                        -1
                    }
                }
            }
            // 仅某状态显示，或切换到当前状态显示
            else if (esList[esPlayIndex].type.contains(Regex("[NWASCT]")) || toStation) {

                var hasMatchCurrentState = false
                var hasMatchCurrentPos = false
                var frontDefaultItemIndex = -1

                val start = if (toStation)
                    0
                else
                    esPlayIndex

                for (i in start until esList.size) {

                    // 寻找非特定状态显示内容（从之后的内容）
                    if (utils.extractNWA(
                            Regex("[NWASCT]"),
                            esList[i].type
                        ) == "" && frontDefaultItemIndex == -1
                    ) {
                        frontDefaultItemIndex = i
                    }

                    // 寻找当前运行站点状态及位置对应内容
                    val currentMatchType = utils.extractNWA(Regex("[NWA]"), esList[i].type)
                    val currentPosType = utils.extractNWA(Regex("[SCT]"), esList[i].type)

                    // 状态及位置类型都有
                    if (currentMatchType != "" && currentPosType != "") {
                        if (getStationStateTypeMap()[currentMatchType] == currentLineStationState &&
                            getStationPositionTypeMap()[currentPosType] == currentLineStationCount
                        ) {
                            // C：即不是`起点站`也不是`终点站`
                            if (currentPosType == "C" &&
                                (currentLineStationCount == 0 || currentLineStationCount == currentLineStationList.size - 1)
                            ) {
                                continue
                            }
                            esPlayIndex = i
                            hasMatchCurrentState = true
                            hasMatchCurrentPos = true
                            break
                        }
                        // 只有状态类型
                    } else if (currentMatchType != "") {
                        if (getStationStateTypeMap()[currentMatchType] == currentLineStationState) {
                            esPlayIndex = i
                            hasMatchCurrentState = true
                            break
                        }
                        // 只有位置类型
                    } else if (currentPosType != "") {
                        // C：即不是`起点站`也不是`终点站`
                        if (currentPosType == "C" &&
                            (currentLineStationCount == 0 || currentLineStationCount == currentLineStationList.size - 1)
                        ) {
                            continue
                        }
                        if (getStationPositionTypeMap()[currentPosType] == currentLineStationCount) {
                            esPlayIndex = i
                            hasMatchCurrentPos = true
                            break
                        }
                    }
                }
                if (!hasMatchCurrentState && !hasMatchCurrentPos) {
                    if (frontDefaultItemIndex >= 0) {
                        esPlayIndex = frontDefaultItemIndex
                    } else {
                        var resIndex = -1
                        for (i in 0 until esList.size) {
                            // 寻找非特定状态显示内容（从所有的内容）
                            if (utils.extractNWA(Regex("[NWASCT]"), esList[i].type) == ""
                            ) {
                                resIndex = i
                                break
                            }
                        }
                        esPlayIndex = resIndex
                    }
                }
            }


        }

        val minTimeS =
            if (esPlayIndex >= 0 && esPlayIndex < esList.size) esList[esPlayIndex].minTimeS else 5
        binding.headerLeftNew.minShowTimeMs = minTimeS * 1000
        binding.headerRightNew.minShowTimeMs = minTimeS * 1000

        refreshEsOnlyText()

    }

    fun refreshEsOnlyText(isUseSet: Boolean = false) {

        Log.d(tag, "refreshEsOnlyText S")

        var leftText: String
        var rightText: String

        if (esPlayIndex >= 0 && esPlayIndex < esList.size) {
            leftText = esList[esPlayIndex].leftText
            rightText = esList[esPlayIndex].rightText
        } else {
            leftText = getString(R.string.main_staring_station_name)
            rightText = getString(R.string.main_terminal_name)
        }

        if (binding.headerMiddleNew.isShowFinish) {
            binding.headerMiddleNew.showText(currentLine.name)
        }

        for (keyword in utils.getDefaultKeywordList()) {
            leftText = leftText.replace(keyword, getValueMapValue(keyword), true)
            rightText = rightText.replace(keyword, getValueMapValue(keyword), true)
        }

        if (!utils.getIsOpenLeftEs()) {
            rightText = "$leftText $rightText"
            leftText = ""
        }

        if (isUseSet) {
            if (binding.headerLeftNew.getText() != leftText)
                binding.headerLeftNew.setText(leftText)
            if (binding.headerRightNew.getText() != rightText)
                binding.headerRightNew.setText(rightText)
        } else {
            binding.headerLeftNew.showText(leftText)
            binding.headerRightNew.showText(rightText)
        }

        Log.d(tag, "refreshEsOnlyText E")


    }

    fun loadLineAll() {
        utils.showMsg("站点数量较多时，加载较慢，请耐心等待")
        val stationList = stationDatabaseHelper.queryAll()
        if (stationList.size >= 2) {

            val allStationLine =
                Line(
                    name = resources.getString(R.string.line_all),
                    isUpAndDownInvert = false
                )
            val allStationLineStationList = ArrayList<Station>()

            for (station in stationList) {

//                val similarStation =
//                    allStationLineStationList.find { s ->
//                        val distance = utils.calculateDistance(
//                            s.longitude,
//                            s.latitude,
//                            station.longitude,
//                            station.latitude,
//                        )
//                        val isContainSameNameStation =
//                            (s.cnName == station.cnName)
//                        distance < 200 && isContainSameNameStation
//                    }
//
//                if (similarStation == null) {
                allStationLineStationList.add(station)
                allStationLine.upLineStation += "${station.id} "
//                }
            }

            val length = allStationLine.upLineStation.length
            allStationLine.upLineStation =
                allStationLine.upLineStation.substring(
                    0,
                    length - 1
                )
            allStationLine.downLineStation =
                allStationLine.upLineStation

            originLine = allStationLine
            initLineInterval()
            currentLineStationState = onArrive
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
            loadLine(allStationLine)

        } else {
            utils.showMsg(getString(R.string.station_not_enough_tip))
        }
    }

    fun refreshUI(isRefreshEs: Boolean = true) {

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //更新路线站点更新信息和系统通知
        refreshLineStationChangeInfo()

        //刷新电显
        if (isRefreshEs)
            refreshEsToStation()

        //刷新地图标点和轨迹
        refreshMarkerAndTrack()
    }

    fun findOnlineLine(
        res: BusLineResult,
        alertDialog: AlertDialog? = null
    ) {
        if (res.busLines.isNotEmpty()) {

            // 搜索结果Dialog
            val lineNameList = Array(
                size = res.busLines.size / 2,
                init = { "" }
            )
            for (i in res.busLines.indices) {
                if (i % 2 == 1)
                    lineNameList[i / 2] = res.busLines[i].busLineName
            }


            if (res.busLines.size == 2) {
                setOnlineLine(res, alertDialog, 0)
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("选择要应用的路线")
                    .setItems(lineNameList) { _, which ->
                        setOnlineLine(res, alertDialog, which * 2)
                    }
                    .show()
            }


        }
    }

    fun setOnlineLine(
        res: BusLineResult,
        alertDialog: AlertDialog? = null,
        chosenIndex: Int
    ) {

        cloudStationList.clear()
        val line = getOnlineLine(res, chosenIndex)

        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putString("lineName", currentLine.name)
            putString("onlineLineUpId", res.busLines[chosenIndex].busLineId)
            putString("onlineLineDownId", res.busLines[chosenIndex + 1].busLineId)
        }

        CoroutineScope(Dispatchers.Main).launch {
            originLine = line
            initLineInterval()
            currentLineStationState = onNext
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
            loadLine(line)
            utils.haptic(binding.headerMiddleNew)
            alertDialog?.cancel()
        }
    }

    fun getOnlineLine(res: BusLineResult, chosenIndex: Int, isGet2Direction: Boolean = true): Line {
        var upLineStationStr = ""
        var downLineStationStr = ""
        val end = if (isGet2Direction)
            chosenIndex + 1
        else
            chosenIndex
        for (x in chosenIndex..end) {
            Log.d(
                tag,
                res.busLines[x].toString()
            )
            for (i in res.busLines[x].busStations.indices) {
                Log.d(
                    tag, "${res.busLines[x].busStations[i].busStationName}" +
                            "\t\t${res.busLines[x].busStations[i].latLonPoint.longitude}" +
                            "\t${res.busLines[x].busStations[i].latLonPoint.latitude}"
                )
                val busStation = res.busLines[x].busStations[i]
                var enName = busStation.busStationName
                val localStation =
                    stationDatabaseHelper.queryByCnName(busStation.busStationName)
                if (localStation.isNotEmpty()) {
                    enName = localStation.first().enName
                }

                val id = -cloudStationList.size - 1
                cloudStationList.add(
                    Station(
                        id,
                        busStation.busStationName,
                        enName,
                        busStation.latLonPoint.longitude,
                        busStation.latLonPoint.latitude,
                        "B"
                    )
                )
                when (x) {
                    chosenIndex ->
                        upLineStationStr += "$id "

                    chosenIndex + 1 ->
                        downLineStationStr += "$id "
                }
            }
        }
//        utils.showMsg(upLineStationStr)
        return Line(
            -1,
            res.busLines[chosenIndex].busLineName.split("路").first(),
            upLineStationStr,
            downLineStationStr,
            false
        )
    }

    fun reverseLineDirection() {
        val checkedId = if (binding.lineDirectionBtnUp.isChecked)
            binding.lineDirectionBtnDown.id
        else
            binding.lineDirectionBtnUp.id

        binding.lineDirectionBtnGroup.check(checkedId)
    }

    fun esPlayNext() {
        if (esPlayIndex < esList.size - 1) {
            esPlayIndex++
        } else {
            esList = utils.getEsList(utils.getEsText())
            esPlayIndex = if (esList.isNotEmpty())
                0
            else
                -1
        }
    }


    fun getStationStateTypeMap(): HashMap<String, Int> {
        val typeMap = HashMap<String, Int>()
        typeMap["N"] = onNext
        typeMap["W"] = onWillArrive
        typeMap["A"] = onArrive
        return typeMap
    }


    fun getStationPositionTypeMap(): HashMap<String, Int> {
        val typeMap = HashMap<String, Int>()
        typeMap["S"] = 0
        typeMap["C"] = currentLineStationCount
        typeMap["T"] = currentLineStationList.size - 1
        return typeMap
    }

    fun initLocalBroadcast() {

        val mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (isAdded) {
                    when (intent.action) {
                        utils.tryListeningAnActionName -> {
                            if (currentLine.name != "") {
                                val stateStr = intent.getStringExtra("stateStr")
                                val typeStr = intent.getStringExtra("typeStr")
                                val format = intent.getStringExtra("format")
                                if (!stateStr.isNullOrBlank() && !typeStr.isNullOrBlank())
                                    utils.showMsg("正在试听${stateStr}${typeStr}播报")
                                announce(format = format ?: "")
                            } else {
                                utils.showMsg("还没有选择路线，请前往“主控”选择")
                            }
                        }

                        utils.switchLineActionName -> {
                            switchLine(id = intent.getIntExtra("id", -1))
                        }

                        utils.editLineOnMapActionName -> {
                            startEditLineOnMap(
                                id = intent.getIntExtra("id", -1),
                                name = intent.getStringExtra("name") ?: "",
                                direction = intent.getIntExtra("direction", 0),
                                type = intent.getStringExtra("type") ?: "update"
                            )
                        }

                        utils.requestCityFromLocationActionName -> {

                            if (!permissionManager.hasLocationPermission()) {
                                utils.showRequestLocationPermissionDialog(permissionManager)
                            }

                            val intent = Intent()
                                .setAction(utils.sendCityFromLocationActionName)
                                .putExtra("cityName", getCityFromLocation())
                            LocalBroadcastManager.getInstance(requireContext())
                                .sendBroadcast(intent)
                        }

                        utils.openLocationActionName -> {
                            binding.locationBtnGroup.check(binding.locationBtn.id)
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(utils.tryListeningAnActionName)
        intentFilter.addAction(utils.switchLineActionName)
        intentFilter.addAction(utils.editLineOnMapActionName)
        intentFilter.addAction(utils.requestCityFromLocationActionName)
        intentFilter.addAction(utils.openLocationActionName)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, intentFilter)

    }

    fun switchLine(id: Int) {

        if (!isAdded)
            return

        val activity = requireActivity() as MainActivity
        activity.binding.viewPager.currentItem = 0

        val line = lineDatabaseHelper.queryById(id).first()
        originLine = line
        initLineInterval()
        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
        loadLine(line)

        utils.showMsg("已切换至 ${line.name} 运行")

    }


    fun startEditLineOnMap(id: Int, name: String = "", direction: Int, type: String = "update") {

        if (!isAdded)
            return

        val activity = requireActivity() as MainActivity
        activity.binding.viewPager.currentItem = 0

        prefs.edit {
            putBoolean("mapEditLineMode", true)
        }

        loadLineAll()

        lineEditorMode = type

        var line = Line()
        if (type == "update") {
            line = lineDatabaseHelper.queryById(id).first()
            lineEditorLineId = line.id ?: -1
            lineEditorLineName = line.name
        } else if (type == "new") {
            line = Line(name = "")
            lineEditorLineId = -1
            lineEditorLineName = name
        }

        lineEditorLineDirection = direction

        //获取当前方向路线站点下标（String形式）序列
        val currentLineStationIndexStrList = when (lineEditorLineDirection) {
            onUp -> line.upLineStation.split(' ')
            onDown -> line.downLineStation.split(' ')
            else -> List(0) { "" }
        }

        lineEditorStationList.clear()
        for (idStr in currentLineStationIndexStrList) {
            if (idStr.toIntOrNull() != null && idStr.toInt() > 0) {
                val lineStationList = stationDatabaseHelper.queryById(idStr.toInt())
                if (lineStationList.isNotEmpty())
                    lineEditorStationList.add(lineStationList.first())
            }
        }

        refreshMarkerAndTrack()

        // 延迟 1 秒关闭定位
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                requireActivity().runOnUiThread {
                    Log.d(tag, "close loc")
                    binding.locationBtnGroup.uncheck(binding.locationBtn.id)
                }
            }
        }, 1000)
    }

    fun getValueMapValue(key: String): String {
        return when (key) {

            // 站点占位符
            "<next>" -> utils.getEsNextWord()
            "<will>" -> utils.getEsWillArriveWord()
            "<arrive>" -> utils.getEsArriveWord()

            // 其他占位符
            "<line>" -> currentLine.name

            "<year>" -> LocalDate.now().year.toString()
            "<years>" -> (LocalDate.now().year % 100).toString()
            "<month>" -> LocalDate.now().monthValue.toString()
            "<date>" -> LocalDate.now().dayOfMonth.toString()

            "<hour>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().hour)
            "<minute>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().minute)
            "<second>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().second)

            "<time>" ->
                String.format(Locale.CHINA, "%02d", LocalTime.now().hour) + ":" +
                        String.format(Locale.CHINA, "%02d", LocalTime.now().minute)

            "<speed>" ->
                if (currentSpeedKmH >= 0) String.format(
                    Locale.CHINA,
                    "%.1f",
                    currentSpeedKmH
                ) else "-"

            else -> {
                val station = when (key.substring(1, 3)) {
                    "ns" -> currentLineStation

                    "ss" -> if (currentLineStationList.isEmpty())
                        Station(
                            cnName = getString(R.string.terminal),
                            enName = getString(R.string.terminal)
                        )
                    else currentLineStationList.first()

                    "ts" -> if (currentLineStationList.isEmpty())
                        Station(
                            cnName = getString(R.string.terminal),
                            enName = getString(R.string.terminal)
                        )
                    else currentLineStationList.last()

                    else -> currentLineStation
                }
                val lang = if (key.substring(1, 3) == "ms") {
                    key.substring(3, 5)
                } else
                    key.drop(3).dropLast(1)
                when (lang) {
                    "cn" -> station.cnName
                    "en" -> station.enName
                    else -> utils.getStationNameFromCn(station.cnName, lang)
                }
            }
        }
    }

    fun getCityFromLocation(): String {
        return currentCityName
    }

    var pointWithStationIndexMap = HashMap<Int, Int>()  //<pointIndex, stationIndex>
    fun drawLineTrace(jsonStr: String) {

        if (jsonStr == "") {
            return
        }

        try {
            Log.d(tag, jsonStr)

            val rootElement = JsonParser.parseString(jsonStr).asJsonObject
            val points =
                rootElement.get("data").asJsonObject.get("points").asJsonArray

            pointWithStationIndexMap = HashMap()

            // 匹配距离每个站点所属的轨迹点
            var lastPointIndex = 0
            currentLineStationList.forEachIndexed { sIndex, station ->

                var minDistance = Double.MAX_VALUE
                var matchPointIndex = 0

                points.forEachIndexed { pIndex, point ->
                    val longitude = point.asJsonObject.get("x").asDouble
                    val latitude = point.asJsonObject.get("y").asDouble

                    val distance = utils.calculateDistance(
                        station.longitude,
                        station.latitude,
                        longitude,
                        latitude
                    )
                    if (distance < minDistance) {
                        minDistance = distance
                        matchPointIndex = pIndex
                    }
                }

//                pointWithStationIndexMap[matchPointIndex] = sIndex

                for (i in lastPointIndex..matchPointIndex) {
                    pointWithStationIndexMap[i] = sIndex - 1
                }
                lastPointIndex = matchPointIndex

            }

//            Log.d("pointWithStationIndexMap", "all ${points.size()}")
//
//            pointWithStationIndexMap.forEach { (key, value) ->
//                Log.d("pointWithStationIndexMap", "$key\t$value")
//            }

            points.forEachIndexed { index, element ->
                val longitude = element.asJsonObject.get("x").asDouble
                val latitude = element.asJsonObject.get("y").asDouble
                val latLng = LatLng(latitude, longitude)
                mPolylineLatLngLists[0].add(latLng)
            }

            addMapLine()


        } catch (e: Exception) {
            utils.showMsg("在线轨迹纠偏获取异常")
            e.printStackTrace()
        }

    }

    val lineWithTypeMap = HashMap<Int, Int>()   //<polyLineIndex, Type(0, 1, 2)>
    fun addMapLine() {
        if (utils.getIsLineTrajectoryCorrection() && currentLine.name != resources.getString(R.string.line_all)) {
            val pointList = ArrayList<LatLng>()
            var stationIndex = 0

            mPolylineLatLngLists[0].forEachIndexed { pIndex, point ->

                pointList.add(point)

                // 绘制该站点轨迹
                if ((pIndex != 0 && pointWithStationIndexMap[pIndex] != pointWithStationIndexMap[pIndex - 1]) ||
                    pIndex == mPolylineLatLngLists[0].size - 1
                ) {

                    val lineType = when (pointWithStationIndexMap[pIndex]) {
                        in 0 until currentLineStationCount ->
                            0

                        currentLineStationCount ->
                            if (currentLineStationState == onNext)
                                1
                            else
                                0

                        else ->
                            2
                    }

                    val lineColorId = when (lineType) {
                        0 -> R.mipmap.line_gray
                        1 -> R.mipmap.line_blue
                        2 -> R.mipmap.line_green
                        else -> R.mipmap.line_gray
                    }

                    // 本站轨迹没有绘制过
                    if (!lineWithTypeMap.containsKey(stationIndex)) {
                        val mPolyline = aMap.addPolyline(
                            PolylineOptions().width(16f)
                                .setCustomTexture((BitmapDescriptorFactory.fromResource(lineColorId)))
                                .addAll(pointList)
                        )
                        polylineList.add(mPolyline)
                        lineWithTypeMap[stationIndex] = lineType
                    }
                    // 本站轨迹绘制过，但颜色不对应
                    else if (lineWithTypeMap[stationIndex] != lineType) {

//                        Log.d(tag, "station $stationIndex redraw $lineType")

                        polylineList[stationIndex].remove()
                        val mPolyline = aMap.addPolyline(
                            PolylineOptions().width(16f)
                                .setCustomTexture((BitmapDescriptorFactory.fromResource(lineColorId)))
                                .addAll(pointList)
                                .zIndex(-90F)
                        )
                        polylineList[stationIndex] = mPolyline
                        lineWithTypeMap[stationIndex] = lineType
                    }

                    pointList.clear()
                    stationIndex++

//                    Log.d(tag, "polylineList ${polylineList.size}")
                }


            }
        } else {
            mPolylineLatLngLists.forEachIndexed { index, points ->
                val lineColorId = when (index) {
                    0 -> R.mipmap.line_gray     //已经过的路径（灰）
                    1 -> R.mipmap.line_blue     //当前处在的路径（蓝）
                    2 -> R.mipmap.line_green    //还未经过的路径（绿）
                    else -> R.mipmap.line_gray
                }
                val mPolyline = aMap.addPolyline(
                    PolylineOptions().addAll(mPolylineLatLngLists[index])
                        .width(16f)
                        .setCustomTexture((BitmapDescriptorFactory.fromResource(lineColorId)))
                        .zIndex(-90F)
                )!!
                polylineList.add(mPolyline)
            }
        }

    }

    fun getFontColor(i: Int): Int {
        return if (currentLine.name == resources.getString(R.string.line_all)) {

            if (utils.getIsMapEditLineMode()) {
                val matchStation =
                    lineEditorStationList.find { station -> station.id == currentLineStationList[i].id }
                if (matchStation == null)
                    Color.rgb(25, 150, 216)
                else
                    Color.rgb(55, 178, 103)
            } else {
                when (i) {
                    currentLineStationCount ->
                        Color.rgb(25, 150, 216)

                    else ->
                        Color.rgb(55, 178, 103)
                }
            }
        } else {
            when (i) {
                in 0 until currentLineStationCount ->
                    Color.rgb(182, 182, 182)

                currentLineStationCount ->
                    Color.rgb(25, 150, 216)

                else ->
                    Color.rgb(55, 178, 103)
            }
        }
    }

}
