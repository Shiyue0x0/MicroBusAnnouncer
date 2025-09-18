package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.UiModeManager
import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.UI_MODE_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
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
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.SensorHelper
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.LineOfSearchAdapter
import com.microbus.announcer.adapter.StationOfLineAdapter
import com.microbus.announcer.bean.EsItem
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogLineSwitchBinding
import com.microbus.announcer.databinding.FragmentMainBinding
import com.microbus.announcer.view.HeaderTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import java.util.stream.Collectors


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

    /**路线卡显示下一站或到站信息标志*/
    private val onNextOrArrive = 0

    /**路线卡显示首末站信息标志*/
    private val onStartAndTerminal = 1

    /**路线卡显示欢迎信息标志*/
    private val onWel = 2

    /**路线卡显示速度标志*/
    private val onSpeed = 3

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
    private val lineLatLngList = ArrayList<LatLng>()
    private val lineLatLngForStationList = ArrayList<Int>()

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

    //路线电显当前显示
    private var lineHeadCardCurrentShow = onNextOrArrive

    private val autoFollowNavigationHandler = Handler(mLooper)
    private var autoFollowNavigationRunnable: Runnable? = null

    //路线电显刷新计时
//    private var lineHeadCardRefreshTime = 0

    //路线电显立即刷新标识
    private var lineHeadCardImmediatelyRefresh = false

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: NotificationCompat.Builder

    private lateinit var locationManager: LocationManager

    //是否跟随定位
    //private var isFollowNavigation = true

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

    private lateinit var audioScope: Job

    lateinit var locationClient: AMapLocationClient

    lateinit var locationMarker: Marker

    lateinit var sensorHelper: SensorHelper

    private val cloudStationList = ArrayList<Station>()

    val lastStationHandler = Handler(mLooper)
    val nextStationHandler = Handler(mLooper)

    private val speedRefreshHandler = Handler(mLooper)

    private val audioReleaseHandler = Handler(mLooper)

    private lateinit var announcementLangList: ArrayList<String>

    private lateinit var permissionManager: PermissionManager
    private var hasInitNotice = false

    var matchCount = 0

    var esList = ArrayList<EsItem>()
    var esPlayIndex = 0

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        if (_binding != null) return binding.root

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

        sensorHelper = SensorHelper(requireContext())

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        locationManager = (requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager)

        lineDatabaseHelper = LineDatabaseHelper(context)
        stationDatabaseHelper = StationDatabaseHelper(context)

        currentDistanceToCurrentStation = utils.getArriveStationDistance() + 1
        lastDistanceToCurrentStation = utils.getArriveStationDistance() + 1

        /**设置屏幕唤醒锁*/
        powerManager =
            requireActivity().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, tag)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

//        //点亮状态栏，控制状态栏字体颜色变黑
//        val controller = WindowInsetsControllerCompat(requireActivity().window, requireActivity().decorView)
//        controller.isAppearanceLightStatusBars = true

        permissionManager = PermissionManager(requireContext(), requireActivity())

        //初始化定位
        initLocation()


        //初始化地图
        initMap()

        //初始化通知
        if (utils.getIsSeedNotice()) {
            initNotification()
        }

        //初始化路线
        initLine()

        // 初始化按钮回调
        initButtonClickListener()

        // 初始化电显
        initEs()

        // 初始化报站
        initAnnouncement()

        // 初始化路线规划
        initLinePlan()

        // 初始化路线运行服务
//        initLineRunningService()

//        binding.lineStationListContainer.setScrollView(binding.lineStationList)


        announcementLangList = utils.getAnnouncementLangList()


        return binding.root
    }

    /* 与用户交互时 */
    // todo
    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")
//        binding.switchMap.isChecked = true
//        binding.locationSwitch.isChecked = true
        binding.mapBtnGroup.check(binding.mapBtnGroup.id)
        binding.locationBtnGroup.check(binding.locationBtn.id)


        (binding.lineStationList.adapter as StationOfLineAdapter).isScroll = true
    }

    /* 不再与用户交互时 */
    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause")
//        binding.switchMap.isChecked = false
//        binding.locationSwitch.isChecked = false
        binding.mapBtnGroup.uncheck(binding.mapBtnGroup.id)
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)


        (binding.lineStationList.adapter as StationOfLineAdapter).isScroll = false

        // 保存历史位置
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("location", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putFloat("latitude", currentLngLat.latitude.toFloat())
            putFloat("longitude", currentLngLat.longitude.toFloat())
        }
    }


    override fun onStop() {
        super.onStop()
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        aMapView.onSaveInstanceState(outState)
    }

    /**
     * 加载路线
     */
    fun loadLine(line: Line) {

        speedRefreshHandler.removeCallbacksAndMessages(null)

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
            }
            // 云端路线
            else if (strIndex.toIntOrNull() != null) {
                Log.d(tag, "strIndex: ${strIndex.toInt()}")
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


        //加载终点站卡片
        if (currentLineStationList.isNotEmpty()) {
            binding.terminalName.text = if (utils.getUILang() == "zh")
                currentLineStationList.last().cnName
            else
                currentLineStationList.last().enName

//            binding.headerLeftNew.showText(
//                if (utils.getUILang() == "zh") currentLineStationList.first().cnName
//                else currentLineStationList.first().enName
//            )
//
//            binding.headerRightNew.showText(
//                if (utils.getUILang() == "zh") currentLineStationList.last().cnName
//                else
//                    currentLineStationList.last().enName
//            )

            //加载路线站点变更卡片

            binding.lineStationChangeInfo.text =
                binding.lineStationChangeInfo.text.toString() + "【"
            if (currentLine.name != resources.getString(R.string.line_all)) {
                binding.lineStationChangeInfo.text =
                    binding.lineStationChangeInfo.text.toString() + currentLine.name
                binding.lineStationChangeInfo.text =
                    binding.lineStationChangeInfo.text as String +
                            if (utils.getUILang() == "zh")
                                " 开往 " + currentLineStationList.last().cnName
                            else
                                " To " + currentLineStationList.last().enName
            }
            binding.lineStationChangeInfo.text =
                binding.lineStationChangeInfo.text.toString() + "】"

        } else {
            binding.terminalName.text = getString(R.string.main_to_station_name)
//            binding.headerLeftNew.showText(getString(R.string.main_staring_station_name))
//            binding.headerRightNew.showText(getString(R.string.main_terminal_name))
        }

//        binding.headerLeft.text =
//            if (utils.getUILang() == "zh") currentLineStationList.first().cnName
//            else currentLineStationList.first().enName
//
//        binding.headerRight.text =
//            if (utils.getUILang() == "zh") currentLineStationList.last().cnName
//            else
//                currentLineStationList.last().enName


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
        //显示|隐藏路线站点框和全站点路线按钮（渐出动画）
        if (currentLine.name != resources.getString(R.string.line_all) && currentLine.name != resources.getString(
                R.string.main_line_0
            )
        ) {
            Log.d(tag, "出现")
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
            Log.d(tag, "隐藏")
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

        // 自动切换站点判定距离
        // 社区线：20m
        if (line.name.length >= 2 && ArrayList<String>(
                listOf(
                    "U1", "U2"
                )
            ).contains(line.name.substring(0, 2))
        ) {
            prefs.edit { putString("arriveStationDistance", "20") }
        }
        // 火车：500m
        else if (line.name.isNotEmpty() && ArrayList<String>(
                listOf(
                    "C", "D", "G", "S", "T", "Y", "Z"
                )
            ).contains(line.name.substring(0, 1))
        ) {
            prefs.edit { putString("arriveStationDistance", "500") }
            // 轨交：50m
        } else if (line.name.length >= 3 && line.name.isNotEmpty() && ArrayList<String>(
                listOf(
                    "NNU",
                )
            ).contains(line.name.substring(0, 3))
        ) {
            prefs.edit { putString("arriveStationDistance", "50") }
            // 其他（公交）：30m
        } else {
            prefs.edit { putString("arriveStationDistance", "30") }
        }

        //更新路线站点卡片
        val adapter = binding.lineStationList.adapter as StationOfLineAdapter
        adapter.stationList = currentLineStationList
        adapter.stationCount = currentLineStationCount
        adapter.stationState = currentLineStationState
        adapter.lineName = currentLine.name
        adapter.mHandler.removeCallbacksAndMessages(null)
        adapter.notifyDataSetChanged()

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新站点标点
        refreshStationMarker()

        refreshEsToStaringAndTerminal()

//        val intent = Intent(LineRunningService().actionLineLoad)
//            .putExtra("line", currentLine.name)
//            .putExtra("lineId", currentLine.id)
//            .putExtra("currentLineStationList", currentLineStationList)
//            .putExtra("currentReverseLineStationList", currentReverseLineStationList)
//            .putExtra("lastDistanceToStationList", lastDistanceToStationList)
//            .putExtra("currentDistanceToStationList", currentDistanceToStationList)
//            .putExtra("lineArriveStationIdList", lineArriveStationIdList)
//        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

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
        locationClient.setLocationOption(option)

        locationClient.setLocationListener { location ->
            onMyLocationChange(location)
        }

        locationClient.startLocation()

    }

    /**
     * 初始化按钮回调
     */
    fun initButtonClickListener() {

        binding.locationBtnGroup.check(binding.locationBtn.id)
        binding.mapBtnGroup.check(binding.mapBtn.id)
        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)


        //单击电显切换路线
        binding.headerNew.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            val dialogBinding = AlertDialogLineSwitchBinding.inflate(LayoutInflater.from(context))

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
                        val stationList = stationDatabaseHelper.quertAll()
                        val lineList = lineDatabaseHelper.quertAll()
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

                                        val allLineSet = lineDatabaseHelper.quertAll().toSet()

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
                //                    Log.d(tag, "在线搜索路线")
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

            binding.mapBtnGroup.check(binding.mapBtnGroup.id)
            binding.locationBtnGroup.check(binding.locationBtn.id)

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
            val textView = TextView(requireContext())
            textView.text = binding.lineStationChangeInfo.text
            textView.setLineSpacing(100f, 0f)
            textView.setPadding(100, 50, 100, 50)
            val scrollView = ScrollView(requireContext())
            scrollView.addView(textView)
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                .setTitle(getString(R.string.running_info))
                .setView(scrollView).create()
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

        //启用定位按钮
//        binding.locationSwitch.setOnCheckedChangeListener { switchCompat, isChecked ->
//
//            if (isOperationLock) {
//                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
//                switchCompat.isChecked = !switchCompat.isChecked
//                return@setOnCheckedChangeListener
//            }
//
//
//            if (isChecked) {
//
//                if (!permissionManager.hasLocationPermission()) {
//                    utils.showRequestLocationPermissionDialog(permissionManager)
//                    return@setOnCheckedChangeListener
//                }
//
//                locationClient.startLocation()
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 1f
//            } else {
//                locationClient.stopLocation()
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 0f
//                matchCount = 0
//            }
//            binding.switchFollowLocation.isChecked = isChecked
//        }

        binding.locationBtn.addOnCheckedChangeListener { button, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                button.isChecked = !button.isChecked
                return@addOnCheckedChangeListener
            }

            if (isChecked) {
                if (!permissionManager.hasLocationPermission()) {
                    utils.showRequestLocationPermissionDialog(permissionManager)
                    return@addOnCheckedChangeListener
                }
                locationClient.startLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f
            } else {
                locationClient.stopLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 0f
                matchCount = 0
            }
        }

//        binding.locationBtnGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
//
//            utils.showMsg(binding.locationBtn.isChecked.toString())
//
//            if (isOperationLock) {
//                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
//                if (isChecked)
//                    group.uncheck(checkedId)
//                else
//                    group.check(checkedId)
//                return@addOnButtonCheckedListener
//            }
//
//            if (isChecked) {
//                if (!permissionManager.hasLocationPermission()) {
//                    utils.showRequestLocationPermissionDialog(permissionManager)
//                    return@addOnButtonCheckedListener
//                }
//                locationClient.startLocation()
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 1f
//            } else {
//                locationClient.stopLocation()
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 0f
//                matchCount = 0
//            }
//        }

//        //跟随定位开关
//        binding.switchFollowLocation.setOnCheckedChangeListener { switchCompat, isChecked ->
//            if (isOperationLock) {
//                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
//                switchCompat.isChecked = !switchCompat.isChecked
//                return@setOnCheckedChangeListener
//            }
//
//            if (isChecked && !binding.locationSwitch.isChecked) {
//                utils.showMsg("请先开启定位")
//                binding.switchFollowLocation.isChecked = false
//            }
//
//        }
//
//        //启用地图开关
//        binding.switchMap.setOnCheckedChangeListener { switchCompat, isChecked ->
//            if (isOperationLock) {
//                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
//                switchCompat.isChecked = !switchCompat.isChecked
//                return@setOnCheckedChangeListener
//            }
//            if (isChecked) {
//                aMapView.onResume()
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 1f
//            } else {
//                if (this::locationMarker.isInitialized)
//                    locationMarker.alpha = 0f
//                aMapView.onPause()
//
//            }
//        }

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
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                if (isChecked)
                    group.uncheck(checkedId)
                else
                    group.check(checkedId)
                return@addOnButtonCheckedListener
            }

            if (isChecked) {
                currentLineDirection = onUp
//                binding.lineDirectionSwitch.text = resources.getString(R.string.up_line)
            } else {
                currentLineDirection = onDown
//                binding.lineDirectionSwitch.text = resources.getString(R.string.down_line)
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

            //更新路线站点显示、小卡片和通知
            refreshLineStationList()

            //刷新路线头屏
            refreshEsToStationState()

            //更新路线站点更新信息
            //updateLineStationChangeInfoAndNotice()

            //刷新站点标点
            refreshStationMarker()
        }


        //起点站按钮
        binding.startingStation.scaleX = -1f
        binding.startingStation.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            val lastLineStationCount = currentLineStationCount
            setStationAndState(0, currentLineStationState)
            if (currentLineStationCount != lastLineStationCount) utils.haptic(binding.startingStation)
        }

        //终点站按钮
        binding.terminal.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            val lastLineStationCount = currentLineStationCount
            setStationAndState(currentLineStationList.size - 1, currentLineStationState)
            if (currentLineStationCount != lastLineStationCount) utils.haptic(binding.terminal)
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
            if (currentLineStationState == onWillArrive)
                announce()
            else
                announce()

            utils.haptic(requireView())
        }

        // 终点站卡片
        binding.terminalCard.setOnClickListener {
            utils.showMsg("${binding.terminalTitle.text} ${binding.terminalName.text}")
        }

        // 当前站点卡片
        binding.currentStationCard.setOnClickListener {
            utils.showMsg("${binding.currentStationState.text} ${binding.currentStationName.text}")
        }

        // 距离卡片
        binding.currentDistanceToCurrentStationCard.setOnClickListener {
            utils.showMsg("${binding.currentDistanceToCurrentStationValue.text}${binding.currentDistanceToCurrentStationUnit.text}")
        }

        // 速度卡片
        binding.speedCard.setOnClickListener {
            utils.showMsg("${binding.speedValue.text}${binding.speedUnit.text}")
        }
    }

    /**
     * 初始化电显
     */
    private fun initEs() {

        esList = utils.getEsList(utils.getEsText())

        //路线电显显示序列
        lineHeadCardShowList = utils.getHeadSignShowInfo()
        //路线电显当前显示下标
        lineHeadCardCurrentShowIndex = 0
        //路线电显当前显示
        lineHeadCardCurrentShow =
            lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

//        binding.headerMiddleNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000
//        binding.headerLeftNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000
//        binding.headerRightNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000

        var isLeftScrollFinish = false
        var isRightScrollFinish = false

        binding.headerMiddleNew.scrollFinishCallback =
            object : HeaderTextView.ScrollFinishCallback {
                override fun onScrollFinish() {
                    binding.headerMiddleNew.showText(currentLine.name)
                }
            }

        binding.headerLeftNew.scrollFinishCallback =
            object : HeaderTextView.ScrollFinishCallback {
                override fun onScrollFinish() {
                    isLeftScrollFinish = true
                    if (isRightScrollFinish) {
                        isLeftScrollFinish = false
                        isRightScrollFinish = false
                        refreshEs()
                    }
                }
            }

        binding.headerRightNew.scrollFinishCallback =
            object : HeaderTextView.ScrollFinishCallback {
                override fun onScrollFinish() {
                    isRightScrollFinish = true
                    if (isLeftScrollFinish) {
                        isLeftScrollFinish = false
                        isRightScrollFinish = false
                        refreshEs()
                    }
                }
            }
        refreshEs()
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
                val stationFullList = stationDatabaseHelper.quertAll()
                val stationList = ArrayList<Station>()
                // 遍历路线方案
//                for (path in res.paths) {
//                }
                val path = res.paths[0]
                Log.d(tag, "size${path.polyline.size}")


                // 路线加密（预期为5m一段）
//                val polylineList = ArrayList<LatLonPoint>()
//                for (i in 0 until path.polyline.size) {
//
//                    if (i == 0) {
//                        polylineList.add(path.polyline[i])
//                        continue
//                    }
//
//                    Log.d(tag, i.toString())
//
//                    val lastPolyline = path.polyline[i - 1]
//                    val currentPolyline = path.polyline[i]
//
//                    val lineSlope =
//                        (lastPolyline.latitude - currentPolyline.latitude) / (lastPolyline.longitude - currentPolyline.longitude)
//
////                    val district = utils.calculateDistance(
////                        lastPolyline.longitude, //经度 x
////                        lastPolyline.latitude,  //纬度 y
////                        currentPolyline.longitude,
////                        currentPolyline.latitude,
////                    )
//
//
//
//                    polylineList.add(lastPolyline)
//
//                }

                for (i in 0 until path.polyline.size) {

//                    aMap.addMarker(MarkerOptions().position(
//                        LatLng(path.polyline[i].latitude, path.polyline[i].longitude)
//                    ))

//                    if (i == 0)
//                        continue

//                    val lastPolyline = path.polyline[i - 1]
//                    val currentPolyline = path.polyline[i]

                    // 求路线一般式方程

//                    A = Y2 - Y1
//                    B = X1 - X2
//                    C = X2*Y1 - X1*Y2

//                    val x1 = lastPolyline.longitude / 111110
//                    val x2 = currentPolyline.longitude / 111110
//                    val y1 = lastPolyline.latitude / 111110
//                    val y2 = currentPolyline.latitude / 111110

//                    val lineA = y2 - y1
//                    val lineB = x1 - x2
//                    val lineC = (x2 * y1) * (x1 * y2)

                    for (station in stationFullList) {
                        // 计算当前路线到站点圆心的距离
//                        val x = station.longitude / 111110
//                        val y = station.latitude / 111110
//                        val distance =
//                            abs(lineA * x + lineB * y + lineC) /
//                                    sqrt(lineA.pow(2) + lineB.pow(2))
//                        Log.d(tag, distance.toString())

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

    /**
     * 初始化地图
     */
    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun initMap() {
        aMapView = binding.map
        aMapView.onCreate(null)
        aMap = aMapView.map

        setMapMode(utils.getMapType())
        aMap.isMyLocationEnabled = false
        aMap.isTrafficEnabled = true

        binding.mapContainer.setScrollView(binding.main)

        val markerMipmapIds = ArrayList<Int>()
        markerMipmapIds.add(R.mipmap.marker_gray)
        markerMipmapIds.add(R.mipmap.marker_blue)
        markerMipmapIds.add(R.mipmap.marker_green)

        markerMipmapIds.forEach {
            val overlayOptions =
                MultiPointOverlayOptions().icon(BitmapDescriptorFactory.fromResource(it))
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
            val fontColor = when (it.customerId.toInt()) {
                in 0 until currentLineStationCount -> Color.rgb(182, 182, 182)

                currentLineStationCount -> Color.rgb(25, 150, 216)

                else -> Color.rgb(55, 178, 103)
            }
            val textOptions =
                TextOptions().text(it.title).fontColor(fontColor).position(it.latLng)
                    .fontSize(48)
            aMapStationClickText = aMap.addText(textOptions)!!
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

        //高德logo设置在地图下方
        aMapUiSettings = aMap.uiSettings!!
        aMapUiSettings.logoPosition = AMapOptions.LOGO_POSITION_BOTTOM_CENTER

        // 触碰地图暂停报站
        binding.map.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            pauseAnnounce()
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

            // 单击地图，设置路线规划起点/终点（关闭“单击地图添加站点”时可用）
            if (!utils.getIsClickMapToAddStation()) {
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

        autoFollowNavigationRunnable = Runnable {
            if (binding.locationBtn.isChecked)
                binding.switchFollowLocation.isChecked = true
        }

        aMap.setOnMapTouchListener {

            if (binding.switchFollowLocation.isChecked) {
                binding.switchFollowLocation.isChecked = false
            }

            //   超过秒数自动跟随定位
            autoFollowNavigationHandler.removeCallbacks(autoFollowNavigationRunnable!!)
            autoFollowNavigationHandler.postDelayed(
                autoFollowNavigationRunnable!!,
                utils.getAutoFollowNavigationWhenAboveSecond() * 1000
            )
            pauseAnnounce()
        }

        // 每隔1s刷新地图Text
        if (::mapRunnable.isInitialized) mMapHandler.removeCallbacks(mapRunnable)
        mapRunnable = object : Runnable {
            override fun run() {
                // 获取地图缩放级别
                mMapHandler.postDelayed(this, 1000L)
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
            }
        }
        mMapHandler.postDelayed(mapRunnable, 1000L)

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
                .setContentIntent(pendingIntent).setFullScreenIntent(pendingIntent, true)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())


        hasInitNotice = true

    }

    /**
     * 初始化路线
     */
    private fun initLine() {

        //初始化路线站点卡片
        binding.lineStationList.setHasFixedSize(true)
        binding.lineStationList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = StationOfLineAdapter(
            requireContext(),
            ArrayList(),
            0,
            ""
        )
        binding.lineStationList.adapter = adapter
        adapter.isScroll = true

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
                    .setTitle(chosenStationCname)
                    .setMessage("将 $chosenStationCname 设置为")
                    .setNegativeButton("区间始发站", object : DialogInterface.OnClickListener {
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
                            adapter.isScroll = true
                        }
                    })
                    .setPositiveButton("区间终点站", object : DialogInterface.OnClickListener {
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
        val lastRunningLineName = sharedPreferences.getString("lineName", "")!!

        val lastLineList = lineDatabaseHelper.quertByName(lastRunningLineName).toMutableList()
        val onlineLineUpId = sharedPreferences.getString("onlineLineUpId", "")!!
        val onlineLineDownId = sharedPreferences.getString("onlineLineDownId", "")!!
        utils.showMsg(lastRunningLineName)
        Log.d(tag, onlineLineUpId)
        Log.d(tag, onlineLineDownId)
        if (lastLineList.isEmpty() && onlineLineUpId != "" && onlineLineDownId != "") {
            // 在线搜索路线
            val busLineQuery = BusLineQuery(
                "",
                BusLineQuery.SearchType.BY_LINE_ID,
                utils.getCity()
            )
            busLineQuery.pageNumber = 0
            busLineQuery.extensions = "all"
            val busLineSearch = BusLineSearch(requireContext(), busLineQuery)
            val onlineLine = Line()
            cloudStationList.clear()
            busLineSearch.setOnBusLineSearchListener { res, rCode ->
                utils.showMsg("setOnBusLineSearchListener${res.busLines.size}")
                if (res.busLines.isNotEmpty()) {
                    Log.d(tag, res.query.queryString)
                    if (res.query.queryString == onlineLineUpId) {
                        val line = getOnlineLine(res, 0, false)
                        onlineLine.upLineStation = line.upLineStation
                        onlineLine.name = line.name
                        if (onlineLine.downLineStation != "") {
                            utils.showMsg("down ok")
                            originLine = onlineLine
                            initLineInterval()
                            currentLineStationState = onNext
                            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                            loadLine(onlineLine)
                            utils.haptic(binding.headerMiddleNew)
                        }
                    } else if (res.query.queryString == onlineLineDownId) {
                        onlineLine.downLineStation = getOnlineLine(res, 0, false).upLineStation
                        if (onlineLine.upLineStation != "") {
                            utils.showMsg("up ok")
                            originLine = onlineLine
                            initLineInterval()
                            currentLineStationState = onNext
                            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
                            loadLine(onlineLine)
                            utils.haptic(binding.headerMiddleNew)
                        }
                    }
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                busLineQuery.queryString = onlineLineUpId
                busLineSearch.searchBusLineAsyn()
                busLineQuery.queryString = onlineLineDownId
                busLineSearch.searchBusLineAsyn()
            }
        }

        val defaultLineName = utils.getDefaultLineName()
        val defaultLineList = lineDatabaseHelper.quertByName(defaultLineName)

        val selectedLine = if (lastLineList.isNotEmpty())
            lastLineList.first()
        else if (defaultLineList.isNotEmpty())
            defaultLineList.first()
        else Line(name = getString(R.string.main_line_0))

        currentLine = selectedLine
        initLineInterval()
        originLine = currentLine
        currentLineStationState = onNext
        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
        loadLine(currentLine)


    }

    fun onMyLocationChange(location: Location) {

        //更新位置与时间
        lastTimeMillis = currentTimeMillis
        currentTimeMillis = System.currentTimeMillis()

        lastLngLat = currentLngLat
        currentLngLat = LatLng(location.latitude, location.longitude)

        // 更新地图
        CoroutineScope(Dispatchers.IO).launch {
            if (binding.switchFollowLocation.isChecked) {
                CoroutineScope(Dispatchers.Main).launch {
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
            binding.currentDistanceToCurrentStationValue.text =
                getString(R.string.main_distance_value)
        } else if (currentDistanceToCurrentStation >= 100000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 10000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.2f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 1000) {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.3f", currentDistanceToCurrentStation / 1000)
        } else {
            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.m)
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation)
        }


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

        //遍历当前方向路线所有站点，先遍历正向，如果没有符合的站点，再遍历反向（Beta）
        if (!findMatchStation(false)) {
            findMatchStation(true)
        }

    }


    /**
     * 遍历站点列表，检查是否符合进站、出站、即将到站条件，并切换站点然后报站
     * @return 当前站点是否更改
     */
    private fun findMatchStation(isReverseLine: Boolean): Boolean {

        matchCount = (matchCount + 1) % Int.MAX_VALUE
        if (matchCount < 2) return false

        val arriveStationDistance = utils.getArriveStationDistance()    // 进站临界距离
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
                    "到达站：${lineStationList[i].cnName} for ${currentDistanceToStationList[i]} <= $arriveStationDistance"
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
                    "即将进站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
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
                    "${lineStationList[i].cnName} 出站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )

                if (isReverseLine) {
                    reverseLineDirection()
                }

                // 上行终点站出站
                else if (currentLineDirection == onUp && i >= lineStationList.size - 1) {
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

    /**
     * 移除所有地图标记
     */
    private fun refreshStationMarker() {

        //移除所有路线标点，清空标点列表
        for (polyline in polylineList) {
            polyline.remove()
        }
        polylineList.clear()

        //移除所有路线圆，清空圆列表
        for (circle in circleList) {
            circle.remove()
        }
        circleList.clear()

        if (::aMapStationClickText.isInitialized) aMapStationClickText.remove()

        //移除标记序号
        for (text in aMapStationTextList) {
            text.remove()
        }
        aMapStationTextList.clear()

        when (utils.getMapStationShowType()) {
            //显示全部站点
            0 -> showAllStation()
            //仅显示当前路线站点
            1 -> showCurrentLineStationMarker()
            //仅显示当前站点
            2 -> showCurrentStation()
        }
    }

    /**
     * 显示全部站点
     */
    private fun showAllStation() {
        val stationList = stationDatabaseHelper.quertAll()
        val multiPointList = ArrayList<MultiPointItem>()
        for (station in stationList) {
            val latLng = LatLng(station.latitude, station.longitude)
            //绘制标点
//            val marker = aMap.addMarker(
//                MarkerOptions().position(latLng).title("${station.id} ${station.cnName}")
//            )
//            if (marker != null) markerList.add(marker)

            val multiPointItem = MultiPointItem(latLng)
            multiPointItem.title =
                if (utils.getUILang() == "zh")
                    "${station.id} ${station.cnName}"
                else
                    "${station.id} ${station.enName}"
            multiPointItem.customerId = station.id.toString()
            multiPointList.add(multiPointItem)
            multiPointOverlayList[1].items = multiPointList

            //绘制面
            val circle = aMap.addCircle(
                CircleOptions().center(latLng).radius(utils.getArriveStationDistance())
                    .fillColor(Color.argb(8, 0, 0, 0)).strokeColor(Color.argb(64, 0, 0, 0))
                    .strokeWidth(0F)
            )
            circleList.add(circle!!)

        }
    }

    /**
     * 显示当前路线站点标记点和轨迹线（已通过站点标为绿色不会移除原有标记）
     */
    private fun showCurrentLineStationMarker() {

        val latLngList = ArrayList<LatLng>()

        val multiPointLists = ArrayList<ArrayList<MultiPointItem>>()
        multiPointLists.add(ArrayList())
        multiPointLists.add(ArrayList())
        multiPointLists.add(ArrayList())

        val mPolylineLatLngLists = ArrayList<ArrayList<LatLng>>()
        mPolylineLatLngLists.add(ArrayList())
        mPolylineLatLngLists.add(ArrayList())
        mPolylineLatLngLists.add(ArrayList())

        for (i in currentLineStationList.indices) {

            val latLng = LatLng(
                currentLineStationList[i].latitude, currentLineStationList[i].longitude
            )
            latLngList.add(latLng)

            //绘制站点面
            val circle = aMap.addCircle(
                CircleOptions().center(latLng).radius(utils.getArriveStationDistance())
                    .fillColor(Color.argb(8, 0, 0, 0)).strokeColor(Color.argb(64, 0, 0, 0))
                    .strokeWidth(0F)
            )
            if (circle != null) circleList.add(circle)

            //绘制站点标点
            val multiPointItem = MultiPointItem(latLng)
            // 文本序号
            var indexText: String
            if (currentLine.name != resources.getString(R.string.line_all)) {
                indexText = if (i < 9) "0${i + 1}"
                else "${i + 1}"
            } else {
                val stationIndex = currentLineStationList[i].id!!
                indexText = if (stationIndex < 9) "0${stationIndex + 1}"
                else "${stationIndex + 1}"
            }

            multiPointItem.title = "$indexText ${currentLineStationList[i].cnName}"
            multiPointItem.customerId = i.toString()
            when (i) {
                in 0 until currentLineStationCount -> multiPointLists[0].add(multiPointItem)

                currentLineStationCount -> multiPointLists[1].add(multiPointItem)

                in currentLineStationCount + 1 until currentLineStationList.size -> multiPointLists[2].add(
                    multiPointItem
                )
            }

            // 绘制线（不线路轨迹纠偏）
            if (!utils.getIsLineTrajectoryCorrection() && currentLine.name != resources.getString(R.string.line_all)) {
                when (i) {
                    in 0 until currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(latLngList[i])
                    }

                    currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(latLngList[i])
                        mPolylineLatLngLists[1].add(latLngList[i])
                    }

                    currentLineStationCount -> {
                        mPolylineLatLngLists[1].add(latLngList[i])
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

        // 绘制线（线路轨迹纠偏）
        if (utils.getIsLineTrajectoryCorrection()) {
            for (i in lineLatLngList.indices) {
//                Log.d(tag, lineLatLngForStationList[i].toString())
                when (lineLatLngForStationList[i]) {
                    in 0 until currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(lineLatLngList[i])
                    }

                    currentLineStationCount - 1 -> {
                        mPolylineLatLngLists[0].add(lineLatLngList[i])
                        mPolylineLatLngLists[1].add(lineLatLngList[i])
                    }

                    currentLineStationCount -> {
                        mPolylineLatLngLists[1].add(lineLatLngList[i])
                        mPolylineLatLngLists[2].add(lineLatLngList[i])
                    }

                    in currentLineStationCount + 1 until currentLineStationList.size -> {
                        mPolylineLatLngLists[2].add(lineLatLngList[i])
                    }
                }
            }
        }

        //提交线
        //已经过的路径（灰）
        if (currentLine.name != resources.getString(R.string.line_all)) {

            val lineWidth = 16f
            var mPolyline = aMap.addPolyline(
                PolylineOptions().addAll(mPolylineLatLngLists[0]).width(lineWidth)
//                .color(Color.argb(200, 182, 182, 182)
                    .setCustomTexture((BitmapDescriptorFactory.fromResource(R.mipmap.line_gray)))
            )!!
            polylineList.add(mPolyline)
            //当前处在的路径（蓝）
            mPolyline = aMap.addPolyline(
                PolylineOptions().addAll(mPolylineLatLngLists[1]).width(lineWidth)
//                .color(Color.argb(200, 25, 150, 216))
                    .setCustomTexture((BitmapDescriptorFactory.fromResource(R.mipmap.line_blue)))

            )!!
            polylineList.add(mPolyline)
            //还未经过的路径（绿）
            mPolyline = aMap.addPolyline(
                PolylineOptions().addAll(mPolylineLatLngLists[2]).width(lineWidth)
//                .color(Color.argb(200, 55, 178, 103))
                    .setCustomTexture((BitmapDescriptorFactory.fromResource(R.mipmap.line_green)))

            )!!
            polylineList.add(mPolyline)
        }

        // 绘制站点序号与名称
        for (i in currentLineStationList.indices) {

            val textOptions = TextOptions().text("").position(
                LatLng(
                    currentLineStationList[i].latitude, currentLineStationList[i].longitude
                )
            ).fontSize(32)
            aMapStationTextList.add(aMap.addText(textOptions)!!)

        }
        refreshMapStationText()
    }

    /**
     * 仅显示当前站点（不会移除原有标记）
     */
    private fun showCurrentStation() {
        val latLng = LatLng(currentLineStation.latitude, currentLineStation.longitude)

        //绘制标记
//        val marker = aMap.addMarker(
//            MarkerOptions().position(latLng)
//                .title("${currentLineStationCount + 1} ${currentLineStation.cnName}")
//        )
//        if (marker != null) markerList.add(marker)
        val multiPointItem = MultiPointItem(latLng)
        val multiPointList = ArrayList<MultiPointItem>()
        multiPointItem.title = "${currentLineStationCount + 1} ${currentLineStation.cnName}"
        multiPointItem.customerId = currentLineStationCount.toString()
        multiPointList.add(multiPointItem)
        multiPointOverlayList[1].items = multiPointList

        //绘制面
        val circle = aMap.addCircle(
            CircleOptions().center(latLng).radius(utils.getArriveStationDistance())
                .fillColor(Color.argb(8, 0, 0, 0)).strokeColor(Color.argb(64, 0, 0, 0))
                .strokeWidth(0F)
        )
        if (circle != null) circleList.add(circle)
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
    private fun nextStation() {
        if (currentLineStation.id == null) return
        if (currentLineStationState == onNext || currentLineStationState == onWillArrive) {
            currentLineStationState = onArrive
        } else if (currentLineStationState == onArrive) {
            if (currentLineStationCount >= currentLineStationList.size - 1) return
            currentLineStationCount++
            currentLineStation = currentLineStationList[currentLineStationCount]
            currentLineStationState = onNext
        }

        refreshUI()
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
    private fun refreshLineStationList() {

        val currentStationStateText = when (currentLineStationState) {
            onNext -> resources.getString(R.string.next)
            onWillArrive -> resources.getString(R.string.will_arrive)
            onArrive -> resources.getString(R.string.arrive)
            else -> ""
        }

        binding.currentStationState.text = currentStationStateText
        if (utils.getUILang() == "zh")
            binding.currentStationName.text = currentLineStation.cnName
        else
            binding.currentStationName.text = currentLineStation.enName

        //路线卡片滚动到当前站点
        binding.lineStationList.post {

            try {
                val manager = binding.lineStationList.layoutManager as LinearLayoutManager
                val adapter = binding.lineStationList.adapter as StationOfLineAdapter
                adapter.stationCount = currentLineStationCount
                adapter.stationState = currentLineStationState
                adapter.notifyDataSetChanged()

                val factory = LayoutInflater.from(requireContext())
                val layout = factory.inflate(R.layout.item_station_of_line, null)
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
        if (utils.getIsSeedNotice()) {

            if (!permissionManager.requestNoticePermission()) {
                utils.showMsg("要接收运行信息，请授予通知权限")
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

        var newInfo = ""

        val dateFormat = SimpleDateFormat("[HH:mm:ss] ", Locale.getDefault())
        newInfo += dateFormat.format(Date(System.currentTimeMillis()))

        when (currentLineStationState) {
            onArrive -> newInfo += "${resources.getString(R.string.arrive)} "
            onWillArrive -> newInfo += "${resources.getString(R.string.will_arrive)} "
            onNext -> newInfo += "${resources.getString(R.string.next)} "
        }
        newInfo += if (utils.getUILang() == "zh")
            currentLineStation.cnName
        else
            currentLineStation.enName
        newInfo += "\n"

        binding.lineStationChangeInfo.text =
            binding.lineStationChangeInfo.text as String + newInfo

    }

    /**
     * 立即刷新电显，并切换到站点状态（如果有）
     */
    private fun refreshEsToStationState() {
        lineHeadCardImmediatelyRefresh = true
        esPlayIndex = 0
        for (i in esList.indices) {
            if (getTypeMap()[esList[i].type] == currentLineStationState) {
                esPlayIndex = i
                break
            }
        }
        refreshEs()
    }

    /**
     * 立即刷新电显，并切换到首末站显示（如果有）
     */
    private fun refreshEsToStaringAndTerminal() {
        lineHeadCardImmediatelyRefresh = true
        esPlayIndex = 0
        for (i in esList.indices) {
            if (esList[i].type == "B") {
                esPlayIndex = i
                break
            }
        }
        refreshEs()
    }

    /**
     * 语音播报
     * @param format 播报格式
     */
    fun announce(format: String = "") {

        //如果没有管理外部存储的权限，请求授予
        if (!utils.isGrantManageFilesAccessPermission()) {
            utils.requestManageFilesAccessPermission(requireActivity())
            return
        }

        if (currentLineStationList.isEmpty())
            return

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

        val anExp = utils.getAnnouncementFormat(stationType, stationState)
        val anList = utils.getAnnouncements(anExp)
        for (item in anList) {
            if (item == "") {
                utils.showMsg("请到\"设置\"设置报站内容")
                return
            } else if (item[0] == '<') {
                when (item) {
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

                    "<line>" -> {
                        mediaList.addAll(utils.getNumOrLetterVoiceList(currentLine.name))
                    }

                    else -> {
                        val station = when (item.substring(1, 3)) {
                            "ns" -> currentLineStation
                            "ss" -> currentLineStationList.first()
                            "ts" -> currentLineStationList.last()
                            else -> Station()
                        }
                        val lang = item.drop(3).dropLast(1)
                        when (lang) {
                            "cn" ->
                                mediaList.add("/${lang}/station/" + station.cnName)

                            "en" ->
                                mediaList.add("/${lang}/station/" + station.enName)

                            else ->
                                mediaList.add("/${lang}/station/" + station.cnName)
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

        //合成报站音频
        if (this::audioScope.isInitialized) {
            audioScope.cancel()
        }

        audioScope = CoroutineScope(Dispatchers.IO).launch {

            if (!this.isActive) {
                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                return@launch
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
                    override fun onStart(utteranceId: String) {
//                        Log.d(tag, "tts onStart: ${utteranceId}")

                    }

                    override fun onDone(utteranceId: String) {
//                        Log.d(tag, "tts finished: ${utteranceId}")
                        utteranceIdDoneList.add(utteranceId)
                    }

                    override fun onError(utteranceId: String?) {
                    }

                })

            }

            val filePathList = ArrayList<String>()

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
                        Log.d(tag, text)

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

            // 音频推流
            filePathList.forEachIndexed { i, filePath ->


                if (!isActive) {
                    audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                    return@launch
                }

                // 等待TTS合成完成
                if (filePath.split("/").reversed()[1] == "tts") {
                    while (true) {
                        if (!isActive) {
                            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                            return@launch
                        }
//                        Thread.sleep(100)
                        Thread.sleep(50)
//                        Log.d(tag, "wait: ${filePath}")
                        if (utteranceIdDoneList.contains(filePath)) break
                    }
                }

                var sampleRate = 0
                var channelCount = 0
                var pcmEncoding = 0
                var durationUs = 0L

                var decoder = MediaCodec.createDecoderByType("audio/mpeg")
                val extractor = MediaExtractor().apply {
                    setDataSource(filePath)

//                    val trackCount = extractor.trackCount
//                    Log.d(tag, "${filePath} track: ${trackCount}")

                    for (i in 0 until trackCount) {
                        val format = getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME)
                        if (mime?.startsWith("audio/") == true) {
                            selectTrack(i)
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            try {
                                pcmEncoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

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
                    AudioFormat.CHANNEL_OUT_STEREO
                } else {
                    AudioFormat.CHANNEL_OUT_MONO
                }

                audioFormat = AudioFormat.Builder().setEncoding(pcmEncoding)
                    .setSampleRate(sampleRate).setChannelMask(channelMask)
                    .build()

                bufferSizeInBytes = AudioTrack.getMinBufferSize(
                    sampleRate, channelMask, pcmEncoding
                )

                //启动报站
                if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.stop()
                    audioTrack.release()
                }

                audioTrack = AudioTrack(
                    audioAttributes, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, 1
                )

                audioTrack.play()

                val inputBuffers = decoder.inputBuffers
                val outputBuffers = decoder.outputBuffers
                val info = MediaCodec.BufferInfo()
                var eosReceived = false

                while (!eosReceived) {

                    if (!isActive) {
                        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                        return@launch
                    }

                    // 输入
                    val inputBufferId = decoder.dequeueInputBuffer(10000)
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

                    // 最后一段音频播放完毕，释放资源
                    if (i == filePathList.size - 1) {
                        audioReleaseHandler.postDelayed({
                            if (!audioScope.isActive)
                                return@postDelayed
                            audioTrack.release()
                            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                        }, durationUs / 1000 + 500) //500ms冗余
                    }

                    // 输出
                    audioManager?.requestAudioFocus(audioFocusRequest!!)
                    val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                    when (outIndex) {
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 输出缓冲区已更改
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // 输出格式已更改
                        }

                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 暂时没有可用输出
                        }

                        else -> {
                            if (outIndex >= 0) {
                                val outputBuffer = outputBuffers[outIndex]
                                val pcmData = ByteArray(info.size)
                                outputBuffer.get(pcmData)

                                synchronized(audioTrack) {
                                    if (audioTrack.state == AudioTrack.STATE_INITIALIZED && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                        audioTrack.write(pcmData, 0, pcmData.size)
//                                        Log.d(
//                                            tag,
//                                            "${audioTrack.playbackHeadPosition * 1000 / audioTrack.sampleRate}/ ${info.presentationTimeUs / 1000}"
//                                        )
                                    }
                                }
                                decoder.releaseOutputBuffer(outIndex, false)
                            }
                        }
                    }

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }

                }

                // 等待播放完毕
                if (i == filePathList.size - 1) {
                    Thread.sleep(durationUs / 1000 + 1000)  //1000ms冗余
                }

            }
        }
    }

    /**
     * 刷新地图站点标记文本
     */
    private fun refreshMapStationText() {
        for (i in aMapStationTextList.indices) {
            // 文本颜色
            val fontColor = when (i) {
                in 0 until currentLineStationCount -> Color.rgb(182, 182, 182)

                currentLineStationCount -> Color.rgb(25, 150, 216)

                else -> Color.rgb(55, 178, 103)
            }
            // 文本序号
            val indexText = if (i < 9) "0${i + 1}"
            else "${i + 1}"
            // 文本内容
            val textContext = if (aMap.cameraPosition!!.zoom > aMapZoomPoint) {
                "$indexText ${currentLineStationList[i].cnName}"
            } else {
                indexText
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
        Log.d(tag, "${form.latitude} -> ${to.latitude}")
        Log.d(tag, "${form.longitude} -> ${to.longitude}")
        val fromPoint = LatLonPoint(form.latitude, form.longitude)
        val toPoint = LatLonPoint(to.latitude, to.longitude)
        val query = RouteSearch.RideRouteQuery(
            RouteSearch.FromAndTo(fromPoint, toPoint),
        )
        CoroutineScope(Dispatchers.IO).launch {
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
        dialogBinding: AlertDialogLineSwitchBinding,
        alertDialog: AlertDialog
    ) {

        "^(\\d+.*)$".toRegex()
        val numReg = "\\d+".toRegex()
        val comparator = Comparator { line1: Line, line2: Line ->
//            val diff = line1.name.length - line2.name.length
//            if (diff != 0) {
//                diff
//            } else {
            val line1NumRes = numReg.find(line1.name)
            val line2NumRes = numReg.find(line2.name)

            if (line1NumRes == null)
                Integer.MAX_VALUE
            else if (line2NumRes == null)
                Integer.MIN_VALUE
            else line1NumRes.value.toInt() - line2NumRes.value.toInt()
//            }
        }


        val res = ArrayList(lineDatabaseHelper.quertByKey(key).sortedWith(comparator))


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

//        dialogBinding.lineList.adapter = adapter
//        dialogBinding.lineList.onItemClickListener =
//            AdapterView.OnItemClickListener { parent, view, position, id ->
//                // 在线搜索路线
//                if (position == lineInfoList.size - 2) {
////                    Log.d(tag, "在线搜索路线")
//                    val busLineQuery = BusLineQuery(
//                        dialogBinding.lineNameInput.text.toString(),
//                        BusLineQuery.SearchType.BY_LINE_NAME,
//                        utils.getCity()
//                    )
//                    busLineQuery.pageNumber = 0
//                    busLineQuery.extensions = "all"
//                    busLineQuery.pageSize = 999999
//                    val busLineSearch = BusLineSearch(requireContext(), busLineQuery)
//                    busLineSearch.setOnBusLineSearchListener { res, rCode ->
//                        findOnlineLine(res, alertDialog)
//                    }
//                    busLineSearch.searchBusLineAsyn()
//                }
//                // 设为临时路线
//                else if (position == lineInfoList.size - 1) {
////                    Log.d(tag, "设为临时路线")
//                    currentLine = Line(name = dialogBinding.lineNameInput.text.toString())
//                    initLineInterval()
//                    originLine = currentLine
//                    currentLineStationState = onNext
//                    loadLine(currentLine)
//                    alertDialog.cancel()
//                }
//                // 本地路线
//                else if (lineNameList[position] != "") {
//                    originLine = lineDatabaseHelper.quertByName(lineNameList[position]).first()
//                    initLineInterval()
//                    currentLineStationState = onNext
//                    loadLine(lineDatabaseHelper.quertByName(lineNameList[position]).first())
//                    utils.haptic(dialogBinding.root)
//                    alertDialog.cancel()
//                }
//
//            }
    }


    /**
     * 更改地图模式
     * @param mode 地图模式标识
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
        if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            audioTrack.stop()
            audioTrack.release()
        }
        if (this::audioScope.isInitialized) {
            audioScope.cancel()
        }
    }

    fun refreshEs() {

        if (!isAdded)
            return

//        if(esPlayIndex >= 0 && esPlayIndex < esList.size)
//            Log.d(tag, "refreshEs: $esPlayIndex / ${esList.size} ${esList[esPlayIndex].leftText}")
//        else
//            Log.d(tag, "refreshEs: $esPlayIndex / ${esList.size}")


        val typeList = getTypeList()
        val typeMap = getTypeMap()

        if (esPlayIndex >= 0 && esPlayIndex < esList.size && typeList.contains(esList[esPlayIndex].type)) {
            if (typeMap[esList[esPlayIndex].type] != currentLineStationState) {
                esPlayNext()
                return
            }
        }


        var leftText: String
        var rightText: String
        if (esPlayIndex >= 0 && esPlayIndex < esList.size) {
            leftText = esList[esPlayIndex].leftText
            rightText = esList[esPlayIndex].rightText
        } else {
            leftText = getString(R.string.main_staring_station_name)
            rightText = getString(R.string.main_terminal_name)
        }

        speedRefreshHandler.removeCallbacksAndMessages(null)

        if (binding.headerMiddleNew.isShowFinish) {
            binding.headerMiddleNew.showText(currentLine.name)
        }

        lineHeadCardImmediatelyRefresh = false

        val valueMap = HashMap<String, String>()

        valueMap["<next>"] = utils.getEsNextWord()
        valueMap["<will>"] = utils.getEsWillArriveWord()
        valueMap["<arrive>"] = utils.getEsArriveWord()

        valueMap["<time>"] = LocalTime.now().toString()
        valueMap["<speed>"] = currentSpeedKmH.toString()
        valueMap["<line>"] = currentLine.name

        valueMap["<nscn>"] = currentLineStation.cnName
        valueMap["<nsen>"] = currentLineStation.enName

        valueMap["<sscn>"] =
            if (currentLineStationList.isEmpty()) "" else currentLineStationList.first().cnName
        valueMap["<ssen>"] =
            if (currentLineStationList.isEmpty()) "" else currentLineStationList.first().enName

        valueMap["<tscn>"] =
            if (currentLineStationList.isEmpty()) "" else currentLineStationList.last().cnName
        valueMap["<tsen>"] =
            if (currentLineStationList.isEmpty()) "" else currentLineStationList.last().enName

        val keywordList = utils.getEsKeywordList()
        for (keyword in keywordList) {
            leftText = leftText.replace(keyword, valueMap[keyword] ?: "", true)
            rightText = rightText.replace(keyword, valueMap[keyword] ?: "", true)
        }

        val minTimeS =
            if (esPlayIndex >= 0 && esPlayIndex < esList.size) esList[esPlayIndex].minTimeS else 5
        binding.headerLeftNew.showTimeMs = minTimeS * 1000
        binding.headerRightNew.showTimeMs = minTimeS * 1000

        binding.headerLeftNew.showText(leftText)
        binding.headerRightNew.showText(rightText)

        // 每隔1s刷新一次速度
        var speedRefreshCount = 0
        if (esPlayIndex >= 0 && esPlayIndex < esList.size && esList[esPlayIndex].type == "S") {
            val speedRefreshRunnable = object : Runnable {
                override fun run() {
//                    binding.headerRightNew.setText(
//                        String.format(
//                            Locale.CHINA,
//                            "%.1fkm/h",
//                            currentSpeedKmH
//                        )
//                    )

                    speedRefreshCount++

                    if (speedRefreshCount < utils.getLineHeadCardChangeTime())
                        speedRefreshHandler.postDelayed(this, 1000L)
                }
            }
            speedRefreshHandler.postDelayed(speedRefreshRunnable, 0L)
        }


//        lineHeadCardCurrentShowIndex =
//            (lineHeadCardCurrentShowIndex + 1) % (lineHeadCardShowList!!.size)
//        lineHeadCardCurrentShow =
//            lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

        esPlayNext()

    }

    fun loadLineAll() {
        val stationList = stationDatabaseHelper.quertAll()
        if (stationList.size >= 2) {

            val allStationLine =
                Line(
                    name = resources.getString(R.string.line_all),
                    isUpAndDownInvert = false
                )
            val allStationLineStationList = ArrayList<Station>()

            for (station in stationList) {

                val similarStation =
                    allStationLineStationList.find { s ->
                        val distance = utils.calculateDistance(
                            s.longitude,
                            s.latitude,
                            station.longitude,
                            station.latitude,
                        )
                        val isContainSameNameStation =
                            (s.cnName == station.cnName)
                        distance < 200 && isContainSameNameStation
                    }

                if (similarStation == null && station.type == "B") {
                    allStationLineStationList.add(station)
                    allStationLine.upLineStation += "${station.id} "
                }
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

//    fun initLineRunningService() {
//
//        val mChannel = NotificationChannel(
//            "default_channel",
//            "Default Channel",
//            NotificationManager.IMPORTANCE_DEFAULT
//        )
//        val notificationManager =
//            requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(mChannel)
//
//        val service = LineRunningService()
//        val intent = Intent(requireContext(), service::class.java)
//            .putExtra("locationInterval", utils.getLocationInterval())
//        requireContext().startForegroundService(intent)
//
//        val receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                when (intent.action) {
//                    LineRunningService().actionNameLocationChange -> {
//                        Log.d(tag, intent.getDoubleExtra("latitude", 0.0).toString())
//                        onReceiveLocation(intent)
//                    }
//
//                    LineRunningService().actionNameLineReverse -> {
//                        binding.lineDirectionSwitch.isChecked =
//                            !binding.lineDirectionSwitch.isChecked
//
//                    }
//                }
//            }
//        }
//        val intentFilter = IntentFilter(LineRunningService().actionNameLocationChange)
//        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, intentFilter)
//
//    }
//
//    fun onReceiveLocation(intent: Intent) {
//        val latitude = intent.getDoubleExtra("latitude", 0.0)
//        val longitude = intent.getDoubleExtra("longitude", 0.0)
//        val azimuth = intent.getDoubleExtra("azimuth", 0.0)
//        val distanceToCurrentStation = intent.getDoubleExtra("distanceToCurrentStation", 0.0)
//        val speedKmH = intent.getDoubleExtra("speedKmH", 0.0)
//
//        val lngLat = LatLng(latitude, longitude)
//
//        // 更新地图中心点和方位
//        CoroutineScope(Dispatchers.IO).launch {
//            if (binding.switchFollowLocation.isChecked) {
//                CoroutineScope(Dispatchers.Main).launch {
//                    aMap.stopAnimation()
//                    aMap.animateCamera(CameraUpdateFactory.changeLatLng(lngLat))
//                }
//                Thread.sleep(250L)
//                CoroutineScope(Dispatchers.Main).launch {
//                    aMap.stopAnimation()
//                    aMap.animateCamera(
//                        CameraUpdateFactory.changeBearing(
//                            azimuth.toFloat()
//                        )
//                    )
//                }
//            }
//        }
//
//        //更新定位标点
//        if (!this::locationMarker.isInitialized) {
//            locationMarker = aMap.addMarker(
//                MarkerOptions().position(lngLat).setFlat(true)
//                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.location_marker))
//            )
//            locationMarker.setAnchor(0.5F, 0.57F)
//        }
//
//        locationMarker.rotateAngle = -azimuth.toFloat()
//
//        val anim = TranslateAnimation(lngLat)
//        anim.setDuration(100L)
//        anim.setInterpolator(DecelerateInterpolator())
//        locationMarker.setAnimation(anim)
//        locationMarker.startAnimation()
//
//        // 更新距离当前到站距离，格式化距离
//        if (currentLine.name == "") {
//            binding.currentDistanceToCurrentStationValue.text = "-"
//        } else if (distanceToCurrentStation >= 100000) {
//            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
//            binding.currentDistanceToCurrentStationValue.text =
//                String.format(Locale.CHINA, "%.1f", distanceToCurrentStation / 1000)
//        } else if (distanceToCurrentStation >= 10000) {
//            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
//            binding.currentDistanceToCurrentStationValue.text =
//                String.format(Locale.CHINA, "%.2f", distanceToCurrentStation / 1000)
//        } else if (distanceToCurrentStation >= 1000) {
//            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
//            binding.currentDistanceToCurrentStationValue.text =
//                String.format(Locale.CHINA, "%.3f", distanceToCurrentStation / 1000)
//        } else {
//            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.m)
//            binding.currentDistanceToCurrentStationValue.text =
//                String.format(Locale.CHINA, "%.1f", distanceToCurrentStation)
//        }
//
//        // 更新速度
//        binding.speedValue.text = String.format(Locale.CHINA, "%.1f", speedKmH)
//    }

    fun refreshUI() {
        //仅显示当前路线站点时，刷新地图标记
        if (utils.getMapStationShowType() == 2) {
            showCurrentStation()
        }

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新路线头屏
        refreshEsToStationState()

        //更新路线站点更新信息和系统通知
        refreshLineStationChangeInfo()

        //刷新站点标点
        refreshStationMarker()
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
                    stationDatabaseHelper.quertByName(busStation.busStationName)
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
        utils.showMsg(upLineStationStr)
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
//        esPlayIndex = (esPlayIndex + 1) % esList.size
        if (esPlayIndex < esList.size - 1) {
            esPlayIndex++
        } else {
            esList = utils.getEsList(utils.getEsText())
            esPlayIndex = 0
        }
    }

    fun getTypeList(): List<String> {
        return listOf("N", "W", "A")
    }

    fun getTypeMap(): HashMap<String, Int> {
        val typeMap = HashMap<String, Int>()
        typeMap["N"] = onNext
        typeMap["W"] = onWillArrive
        typeMap["A"] = onArrive
        return typeMap
    }

}
