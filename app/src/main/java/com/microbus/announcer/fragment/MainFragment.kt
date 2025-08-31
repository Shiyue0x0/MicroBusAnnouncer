package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
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
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.util.Log
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import com.amap.api.services.busline.BusLineSearch
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import com.arthenica.mobileffmpeg.FFmpeg
import com.microbus.announcer.HeaderTextView
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.SensorHelper
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.StationOfLineAdapter
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogLineSwitchBinding
import com.microbus.announcer.databinding.FragmentMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors


class MainFragment : Fragment() {

    private var tag = javaClass.simpleName

    private val appRootPath =
        Environment.getExternalStorageDirectory().absolutePath + "/Documents/Announcer"

    /**正前往下一站标志*/
    private val onNext = 0

    /**正前往下一站标志*/
    private val onWillArrive = 1

    /**已到达站点标志*/
    private val onArrive = 2

    /**路线上行标志*/
    private val onUp = 0

    /**路线下行标志*/
    private val onDown = 1

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
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

    private var lastTimeMillis = System.currentTimeMillis()
    private var currentTimeMillis = System.currentTimeMillis()

    private val mLooper: Looper = Looper.getMainLooper()

    private val mMapHandler: Handler = Handler(mLooper)
    private lateinit var mapRunnable: Runnable

    private lateinit var permissionManager: PermissionManager

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
    private var currentLngLat = lastLngLat

    private var currentDistanceToCurrentStation = 100.0
    private var lastDistanceToCurrentStation = 100.0

    var originLine = Line()

    private lateinit var currentLine: Line

    /**当前方向路线站点下标序列*/
//    private var currentLineStationIdList = ArrayList<Int>()

    /**当前路线站点运行方向*/
    private var currentLineDirection = onUp

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

    //路线头牌显示序列
    private var lineHeadCardShowList: MutableSet<String>? = null

    //路线头牌当前显示下标
    private var lineHeadCardCurrentShowIndex = 0

    //路线头牌当前显示
    private var lineHeadCardCurrentShow = onNextOrArrive

    private val autoFollowNavigationHandler = Handler(mLooper)
    private var autoFollowNavigationRunnable: Runnable? = null

    //路线头牌刷新计时
//    private var lineHeadCardRefreshTime = 0

    //路线头牌立即刷新标识
    private var lineHeadCardImmediatelyRefresh = false

    private lateinit var lineStationCardLayoutManager: LinearLayoutManager

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var notificationManager: NotificationManager? = null
    private var notification: NotificationCompat.Builder? = null

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


    var matchCount = 0

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        if (_binding != null) return binding.root

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

        sensorHelper = SensorHelper(requireContext())

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        //请求权限
        permissionManager = PermissionManager(requireContext(), this)

        locationManager = (requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager)

        lineDatabaseHelper = LineDatabaseHelper(context)
        stationDatabaseHelper = StationDatabaseHelper(context)

        currentDistanceToCurrentStation = utils.getArriveStationDistance() + 1
        lastDistanceToCurrentStation = utils.getArriveStationDistance() + 1

        //初始化通知管理
        notificationManager =
            requireContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        /**设置屏幕唤醒锁*/
        powerManager =
            requireActivity().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, tag)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        //初始化定位
        initLocation()

        //初始化地图
        initMap()

        // 初始化按钮回调
        initButtonClickListener()

        // 初始化头牌
        initHeadSign()

        // 初始化报站
        initAnnouncement()

        // 初始化路线规划
        initLinePlan()

        // 初始化路线运行服务
//        initLineRunningService()

        currentLine = Line(name = getString(R.string.main_line_0))

        //切换为默认路线
        val defaultLineName = utils.getDefaultLineName()
        val defaultLineList = lineDatabaseHelper.quertByName(defaultLineName)
        if (defaultLineList.isNotEmpty()) {
            currentLine = defaultLineList.first()
            initLineInterval()
            originLine = currentLine
            loadLine(currentLine)
            currentLineStationState = onNext
        }

        requestManageFilesAccessPermission()

        return binding.root
    }

    // 与用户交互时
    override fun onResume() {
        super.onResume()
        binding.switchMap.isChecked = true
        aMapView.onResume()
    }

    // 不再与用户交互时
    override fun onPause() {
        super.onPause()
        binding.switchMap.isChecked = false
        aMapView.onPause()

        // 保存历史位置
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("location", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putFloat("latitude", currentLngLat.latitude.toFloat())
            putFloat("longitude", currentLngLat.longitude.toFloat())
        }
    }

    override fun onStart() {
        super.onStart()
        binding.locationSwitch.isChecked = true
    }

    override fun onStop() {
        super.onStop()
        binding.locationSwitch.isChecked = false
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        aMapView.onSaveInstanceState(outState)
    }

    /**
     * 加载路线
     */
    fun loadLine(line: Line) {

        //切换当前路线
        currentLine = line

        //加载路线名称
//        binding.headerMiddle.text = currentLine.name
        binding.headerMiddleNew.setText(currentLine.name)
        binding.headerMiddleNew.requestLayout()
        binding.lineStationChangeInfo.text =
            binding.lineStationChangeInfo.text.toString() + "\n--" + currentLine.name

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

            binding.headerLeftNew.setText(
                if (utils.getUILang() == "zh") currentLineStationList.first().cnName
                else currentLineStationList.first().enName
            )

            binding.headerRightNew.setText(
                if (utils.getUILang() == "zh") currentLineStationList.last().cnName
                else
                    currentLineStationList.last().enName
            )

            //加载路线站点变更卡片

            if (currentLine.name == resources.getString(R.string.line_all))
                binding.lineStationChangeInfo.text =
                    binding.lineStationChangeInfo.text as String + "--"
            else
                binding.lineStationChangeInfo.text =
                    binding.lineStationChangeInfo.text as String +
                            if (utils.getUILang() == "zh")
                                " 开往 " + currentLineStationList.last().cnName + "--"
                            else
                                " To " + currentLineStationList.last().enName + "--"
            //更新通知
            if (binding.locationSwitch.isChecked && notification != null && notificationManager != null) {
                notification!!.setContentText(
                    if (utils.getUILang() == "zh")
                        "${currentLine.name} 开往 ${currentLineStationList.last().cnName}"
                    else
                        "${currentLine.name} To ${currentLineStationList.last().enName}"
                )
                notification!!.setWhen(System.currentTimeMillis())
                notificationManager!!.notify(0, notification!!.build())
            }
        } else {
            binding.terminalName.text = getString(R.string.main_to_station_name)
            binding.headerLeftNew.setText(getString(R.string.main_staring_station_name))
            binding.headerRightNew.setText(getString(R.string.main_terminal_name))
            binding.lineStationChangeInfo.text = binding.lineStationChangeInfo.text as String + "--"
//            binding.currentStationState.text = getString(R.string.next)
//            binding.currentStationName.text = getString(R.string.main_next_station_name)
//            binding.currentDistanceToCurrentStationUnit.text = getString(R.string.main_distance_unit)
//            binding.currentDistanceToCurrentStationValue.text = getString(R.string.main_distance_value)


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
        viewList.add(binding.lineDirectionSwitch)
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
                    if (binding.lineStationCard.visibility == VISIBLE)
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

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新站点标点
        refreshStationMarker()

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

//        utils.showMsg(latitude.toString())

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

        //单击头牌切换路线
        binding.headerNew.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            val dialogBinding = AlertDialogLineSwitchBinding.inflate(LayoutInflater.from(context))


            val alertDialog =
                AlertDialog.Builder(requireContext()).setView(dialogBinding.root)
                    .setTitle(resources.getString(R.string.switch_line))
                    .setNeutralButton(resources.getString(R.string.line_all)) { dialog, which ->
                        loadLineAll()
                    }
                    .setPositiveButton(
                        resources.getString(R.string.out_line_running)
                    ) { dialog, which ->
                        val line = Line(name = getString(R.string.main_line_0))
                        originLine = line
                        initLineInterval()
                        loadLine(line)
                        currentLineStationState = onNext
                        utils.haptic(binding.headerMiddleNew)
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

                        AlertDialog.Builder(context)
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



                                        AlertDialog.Builder(context)
                                            .setTitle(resources.getString(R.string.line_other))
                                            .setItems(lineInfoList) { _, which ->
                                                if (lineInfoList[which] != "") {
                                                    originLine = otherLineList[which]
                                                    initLineInterval()
                                                    loadLine(otherLineList[which])
                                                    currentLineStationState = onNext
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
                .show(
                    WindowInsetsCompat.Type.ime()
                )

            searchLine("", dialogBinding, alertDialog)

            dialogBinding.lineNameInput.addTextChangedListener { text ->
                searchLine(text.toString(), dialogBinding, alertDialog)
            }

            return@setOnClickListener

        }

        //单击地图定位按钮，地图移动到当前位置
        binding.mapLocation.setOnClickListener {

            binding.switchFollowLocation.isChecked = true
            binding.locationSwitch.isChecked = true

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
            AlertDialog.Builder(requireContext()).setTitle(getString(R.string.running_info))
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
        binding.locationSwitch.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                locationClient.startLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f
            } else {
                locationClient.stopLocation()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 0f
                matchCount = 0
            }
            binding.switchFollowLocation.isChecked = isChecked
        }

        //跟随定位开关
        binding.switchFollowLocation.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }
        }

        //启用地图开关
        binding.switchMap.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                aMapView.onResume()
            } else {
                aMapView.onPause()

            }
        }

        //切换上下行开关
        binding.lineDirectionSwitch.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                currentLineDirection = onUp
                binding.lineDirectionSwitch.text = resources.getString(R.string.up_line)
            } else {
                currentLineDirection = onDown
                binding.lineDirectionSwitch.text = resources.getString(R.string.down_line)
            }

            if (currentLine.id == null) return@setOnCheckedChangeListener

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
            refreshLineHeadDisplay()

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

        //报本站按钮
        binding.voiceAnnouncement.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }
            //语音播报当前站点
            if (currentLineStationState == onWillArrive)
                announce(1)
            else
                announce()

            utils.haptic(requireView())
        }

    }

    /**
     * 初始化头牌
     */
    private fun initHeadSign() {

        //路线头牌显示序列
        lineHeadCardShowList = utils.getHeadSignShowInfo()
        //路线头牌当前显示下标
        lineHeadCardCurrentShowIndex = 0
        //路线头牌当前显示
        lineHeadCardCurrentShow =
            lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

        binding.headerMiddleNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000
        binding.headerLeftNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000
        binding.headerRightNew.showTimeMs = utils.getLineHeadCardChangeTime() * 1000

        var isLeftScrollFinish = false
        var isRightScrollFinish = false


        binding.headerMiddleNew.scrollFinishCallback =
            object : HeaderTextView.ScrollFinishCallback {
                override fun onScrollFinish() {
                    binding.headerMiddleNew.setText(currentLine.name)
                }
            }

        binding.headerLeftNew.scrollFinishCallback = object : HeaderTextView.ScrollFinishCallback {
            override fun onScrollFinish() {
                isLeftScrollFinish = true
                if (isRightScrollFinish) {
                    refreshHeader()
                    isLeftScrollFinish = false
                    isRightScrollFinish = false
                }
            }
        }

        binding.headerRightNew.scrollFinishCallback = object : HeaderTextView.ScrollFinishCallback {
            override fun onScrollFinish() {
                isRightScrollFinish = true
                if (isLeftScrollFinish) {
                    refreshHeader()
                    isLeftScrollFinish = false
                    isRightScrollFinish = false
                }
            }
        }

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
            .setSampleRate(32000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        bufferSizeInBytes = AudioTrack.getMinBufferSize(
            32000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
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
                    loadLine(planLine)
                    currentLineStationState = onNext
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
                AlertDialog.Builder(requireContext()).setTitle("").setNegativeButton(
                    "从这出发"
                ) { p0, p1 ->
                    planFrom = LatLng(it.latitude, it.longitude)
                    if (this::planFromMarker.isInitialized) planFromMarker.destroy()
                    planFromMarker = aMap.addMarker(
                        MarkerOptions().position(planFrom).title("起点")
                    )
                }.setPositiveButton("去这里") { p0, p1 ->
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
            binding.currentDistanceToCurrentStationValue.text = "-"
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

        binding.speedValue.text = String.format(Locale.CHINA, "%.1f", currentSpeedKmH)

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

        val arriveStationDistance = utils.getArriveStationDistance()    // 进站临界点
        val willArriveStationDistance = arriveStationDistance + 50    // 进站临界点

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
                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
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
                        binding.lineDirectionSwitch.isChecked =
                            !binding.lineDirectionSwitch.isChecked
                    }
                }

                announce()
                utils.longHaptic()
                return true
            }
            //即将进站条件：上次位于即将进站范围外，现在位于即将进站范围内，且现在不位于进站进站内
            else if (lastDistanceToStationList[i] > willArriveStationDistance
                && currentDistanceToStationList[i] <= willArriveStationDistance
                && currentDistanceToStationList[i] > arriveStationDistance
            ) {

                Log.d(
                    tag,
                    "即将进站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )

                if (isReverseLine) {
                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
                } else if (currentLineStationState != onNext || lineStationList[i].id != currentLineStation.id) {
                    setStationAndState(i, onNext)
                } else {
                    setStationAndState(i, onWillArrive)
                }

                announce(1)
                utils.longHaptic()
                return true
            }
            //出站条件：上次位于某站点内，现在位于这个站点外（进站范围）
            else if (lastDistanceToStationList[i] < arriveStationDistance && currentDistanceToStationList[i] > arriveStationDistance && currentLine.name != resources.getString(
                    R.string.line_all
                )
            ) {

                Log.d(
                    tag,
                    "${lineStationList[i].cnName} 出站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )

                if (isReverseLine) {
                    binding.lineDirectionSwitch.isChecked = !binding.lineDirectionSwitch.isChecked
                }
                // 上行终点站出站
                else if (i >= lineStationList.size - 1 && currentLineDirection == onUp) {
                    binding.lineDirectionSwitch.isChecked =
                        !binding.lineDirectionSwitch.isChecked
                    setStationAndState(1, onNext)
                } else if (i < lineStationList.size - 1) {
                    setStationAndState(i + 1, onNext)
                }

                announce()
                utils.longHaptic()

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
        multiPointOverlayList[0].items = multiPointLists[0]
        multiPointOverlayList[1].items = multiPointLists[1]
        multiPointOverlayList[2].items = multiPointLists[2]

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

        lineStationCardLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        //更新路线站点卡片
        binding.lineStationList.setHasFixedSize(true)
        binding.lineStationList.layoutManager = lineStationCardLayoutManager
        val adapter = StationOfLineAdapter(
            requireContext(),
            currentLineStationList,
            currentLineStationCount,
            currentLine.name
        )
        binding.lineStationList.adapter = adapter

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

                AlertDialog.Builder(requireContext()).setTitle(chosenStationCname)
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
        // todo
        var midIndex = 7
//        if (binding.lineStationList.adapter!!.itemCount > 0){
//            val itemWidth = (binding.lineStationList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
//            val maxShowItems = binding.lineStationList.width / itemWidth
//            midIndex = maxShowItems/ 2
//        }
        if (currentLineStationCount >= midIndex) lineStationCardLayoutManager.scrollToPosition(
            currentLineStationCount - midIndex + 1
        )
    }

    /**
     * 更新路线站点更新信息
     */
    private fun refreshLineStationChangeInfo() {

        val oldInfo = binding.lineStationChangeInfo.text.toString()
        var newInfo = ""

        val dateFormat = SimpleDateFormat("[HH:mm:ss] ", Locale.getDefault())
        val dataTime = dateFormat.format(Date(System.currentTimeMillis()))

        binding.lineStationChangeInfo.text = oldInfo + "\n" + dataTime
        when (currentLineStationState) {
            onArrive -> newInfo += "${resources.getString(R.string.arrive)} "
            onWillArrive -> newInfo += "${resources.getString(R.string.will_arrive)} "
            onNext -> newInfo += "${resources.getString(R.string.next)} "
        }
        newInfo += if (utils.getUILang() == "zh")
            currentLineStation.cnName
        else
            currentLineStation.enName

        binding.lineStationChangeInfo.text =
            binding.lineStationChangeInfo.text as String + newInfo

    }

    /**
     * 刷新路线头屏
     */
    private fun refreshLineHeadDisplay() {
        lineHeadCardImmediatelyRefresh = true
        lineHeadCardCurrentShowIndex = 0
        lineHeadCardCurrentShow = lineHeadCardShowList!!.elementAt(0).toInt()
//        mHandler.removeCallbacksAndMessages(null)
//        mHandler.postDelayed(lineHeadCardRefreshRunnable as Runnable, 0)
        refreshHeader()
    }

    /**
     * 语音播报
     * 音频格式标准
     * wav 单声道 32000HZ 192kbps
     * @param type 0：报进站或出站，1：报即将到站，2：单独报站名
     * @param station type为2时要播报站点
     * @param lang type为2时要播报的语种，cn，en，或者all（先报中文，再报英文）
     */
    fun announce(
        type: Int = 0, station: Station = Station(
            null,
            "MicroBus 欢迎您",
            "MicroBus",
            0.0,
            0.0
        ), lang: String = ""
    ) {

        if (currentLineStationList.isEmpty())
            return

        Log.d(tag, "开始报站")

        //如果没有管理外部存储的权限，请求授予
        if (!requestManageFilesAccessPermission())
            return

        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)

        val voiceList = ArrayList<String>()
        val filePathList = ArrayList<String>()
        val tempFilePath = requireContext().getExternalFilesDir("")?.path
        val tempFile = File("$tempFilePath/tempAudio.wav")

        // 列车进站/出站播报
        if (type == 0) {

            if (utils.getIsVoiceAnnouncements()) {


                //普通话报站
                if (currentLineStationState == onNext) {
                    voiceList.add(voiceList.size, "/cn/thisTrain")
                    voiceList.addAll(utils.getNumOrLetterVoiceList(currentLine.name))
                    voiceList.add(voiceList.size, "/cn/isBoundFor")

                    //终点站名称
                    voiceList.add(
                        voiceList.size,
                        "/cn/station/${currentLineStationList.last().cnName}"
                    )
                } else if (currentLineStationState == onArrive && utils.getIsArriveTimeAnnouncements()) {
                    voiceList.addAll(utils.getTimeVoiceList())
                }


                //当前到站 或 下一站
                when (currentLineStationState) {
                    onArrive -> voiceList.add(voiceList.size, "/cn/arriveStation")
                    onNext -> voiceList.add(voiceList.size, "/cn/nextStation")
                }

                //起点站 或 终点站
                if (currentLineStationCount == 0) voiceList.add(
                    voiceList.size,
                    "/cn/startingStation"
                )
                if (currentLineStationCount == currentLineStationList.size - 1) voiceList.add(
                    voiceList.size, "/cn/terminal"
                )

                //站点名称
                voiceList.add(voiceList.size, "/cn/station/" + currentLineStation.cnName)

//        when (currentLineStationState) {
//            onNext -> {
//                //欢迎乘坐
//                voiceList.add(voiceList.size, "welcomeToTake")
//                //路线名称
//                voiceList.add(voiceList.size, "/line/${currentLine.name}")
//
//            }
//        }
            }

            //英语报站
            if (utils.getIsEnVoiceAnnouncements()) {
                if (currentLineStationState == onNext) {
                    //本次列车开往
                    voiceList.add(voiceList.size, "/en/thisTrainIsBoundFor")

                    //终点站名称
                    if (File("$appRootPath/Media/en/station/${currentLineStationList.last().enName}.wav").exists()) voiceList.add(
                        voiceList.size, "/en/station/${currentLineStationList.last().enName}"
                    )
                    else {
                        if (utils.getIsUseTTS()) {
                            voiceList.add(
                                voiceList.size,
                                "/cn/station/${currentLineStationList.last().cnName}"
                            )
                        } else {
//                        utils.showMsg("报站音频缺失\nen/station/$currentLineTerminalEnName.wav")
                            voiceList.add(
                                voiceList.size,
                                "/cn/station/${currentLineStationList.last().cnName}"
                            )
                        }

                    }
                }

                //列车到达 或 下一站
                when (currentLineStationState) {
                    onArrive -> voiceList.add(voiceList.size, "/en/arriveStation")
                    onNext -> voiceList.add(voiceList.size, "/en/nextStation")
                }

                //起点站 或 终点站
                if (currentLineStationCount == 0) voiceList.add(
                    voiceList.size, "/en/startingStation"
                )
                if (currentLineStationCount == currentLineStationList.size - 1) voiceList.add(
                    voiceList.size, "/en/terminal"
                )

                //站点名称
                if (File("$appRootPath/Media/en/station/${currentLineStation.enName}.wav").exists()) voiceList.add(
                    voiceList.size, "/en/station/${currentLineStation.enName}"
                )
                else {
                    if (utils.getIsUseTTS()) {
                        voiceList.add(
                            voiceList.size,
                            "/cn/station/${currentLineStation.cnName}"
                        )
                    } else {
//                    utils.showMsg("报站音频缺失\nen/station/${currentLineStation.enName}.wav")
                        voiceList.add(
                            voiceList.size,
                            "/cn/station/${currentLineStation.cnName}"
                        )
                    }
                }
            }


            if (utils.getIsSpeedAnnouncements() && currentLineStationState == onNext
                && currentSpeedKmH >= 10 && currentSpeedKmH < 100
            ) {
                voiceList.add(voiceList.size, "/cn/当前时速")
                voiceList.addAll(
                    utils.intOrLetterToCnReading(
                        currentSpeedKmH.toInt().toString(),
                        "/cn/time/"
                    )
                )
            }
        }
//        报即将到站
        else if (type == 1) {
            voiceList.add(voiceList.size, "/cn/距离前方到站还有")
//            voiceList.add(voiceList.size, currentDistanceToCurrentStation.toInt().toString() + "米")
            voiceList.add(voiceList.size, "/cn/station/" + currentLineStation.cnName)
//        报站名
        } else if (type == 2) {
            if (lang == "cn" || lang == "all") voiceList.add(
                voiceList.size,
                "/cn/station/${station.cnName}"
            )
            if (lang == "en" || lang == "all") voiceList.add(
                voiceList.size,
                "/en/station/${station.enName}"
            )
        }

        //合成报站音频
        if (this::audioScope.isInitialized) {
            audioScope.cancel()
        }

        audioScope = CoroutineScope(Dispatchers.IO).launch {

//            val id = UUID.randomUUID().toString().substring(4)

            if (!this.isActive) {
                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                return@launch
            }

            //新建缓存文件目录
            File(tempFilePath!!).mkdirs()
            //创建缓存文件
            tempFile.createNewFile()
            //清除缓存文件内容
            val writer = FileWriter(tempFile)
            writer.write("")
            writer.close()

            val doneUtteranceIdList = ArrayList<String>()

            if (utils.getIsUseTTS()) {

                File("$tempFilePath/tts").walkTopDown().forEach {
                    it.delete()
                }
                File("$tempFilePath/tts").mkdirs()

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                    }

                    override fun onDone(utteranceId: String) {
                        doneUtteranceIdList.add(utteranceId)
                    }

                    override fun onError(utteranceId: String?) {
                    }

                })

            }

            // 合成音频
            var isHasVoice = false
            for (i in voiceList.indices) {
                //寻找报站音频资源
                if (!File("$appRootPath/Media/${voiceList[i]}.wav").exists()) {
                    // 启用TTS，使用TTS音频
                    if (utils.getIsUseTTS()) {
                        filePathList.add("$tempFilePath/tts/" + voiceList[i].split('/').last())
                    }
                    // 不启用TTS，忽略该文本报站
                    else {
                        continue
                    }
                } else {
                    isHasVoice = true
                    filePathList.add("$appRootPath/Media/${voiceList[i]}.wav")
                }
            }

            if (!isHasVoice) {
                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                return@launch
            }

            //启动报站
            if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack.stop()
                audioTrack.release()
            }

            audioTrack = AudioTrack(
                audioAttributes, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, 1
            )

            audioTrack.play()


            for (file in filePathList)
                Log.d(tag, file)

            // TTS文件的<原文件名，合成音频文件名>
            val ttsMap = HashMap<String, String>()

            //预合成tts
            for (file in filePathList) {
                if (file.split("/").reversed()[1] == "tts") {
                    // 合成TTS
                    val ttsFileName = "ttsFile" + UUID.randomUUID().toString() + ".wav"
                    ttsMap[file] = ttsFileName
                    val ttsFile = File("$tempFilePath/tts/", ttsFileName)
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsFileName)
                    tts.synthesizeToFile(
                        file.split('/').last(),
                        params,
                        ttsFile,
                        ttsFileName
                    )
                }
            }

            //读取pcm，tts转码，加入播放流
            for (file in filePathList) {
                if (!isActive) {
                    audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                    return@launch
                }
                val fis: FileInputStream
                val fisPath: String
                // TTS合成音频
                if (file.split("/").reversed()[1] == "tts") {

                    val ttsFileName = ttsMap[file]
                    val ttsFile = File("$tempFilePath/tts/", ttsFileName!!)

                    while (true) {
                        if (!isActive) {
                            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                            return@launch
                        }
                        Thread.sleep(100)
//                    Log.d(tag, "$id $ttsDoneNum/$ttsTotalNum")
                        if (doneUtteranceIdList.contains(ttsFileName)) break
                    }

                    //44100Hz -> 32000Hz
                    val ttsFilePath =
                        tempFilePath + "/tts/ttsFile" + UUID.randomUUID().toString() + ".wav"
                    val command = "-i $ttsFile -ar 32000 -ac 1 $ttsFilePath"
                    FFmpeg.cancel()
                    FFmpeg.execute(command)
                    if (!File(ttsFilePath).exists()) {
                        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                        return@launch
                    }
                    fisPath = ttsFilePath
                }
                // 本地音频
                else {
                    fisPath = file
                }
                fis = FileInputStream(fisPath)

                audioManager!!.requestAudioFocus(audioFocusRequest!!)

                val audioStream = FileInputStream(fisPath)
                audioStream.skip(44 * 2)
                val audioData = utils.convertToByteArray(audioStream)
                audioTrack.write(audioData!!, 0, audioData.size)


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

                fis.close()
            }

            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)

            // 合成WAV音频 结束
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
        lineList: MutableList<Line>,
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

        AlertDialog.Builder(context).setTitle(title)
            .setItems(lineInfoList) { _, which ->
                if (lineInfoList[which] != "") {
                    originLine = sortedMatchLineList[which]
                    initLineInterval()
                    loadLine(sortedMatchLineList[which])
                    currentLineStationState = onNext
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
            val diff = line1.name.length - line2.name.length
            if (diff != 0) {
                diff
            } else {
                numReg.find(line1.name)!!.value.toInt() - numReg.find(line2.name)!!.value.toInt()
            }
        }

        val res = ArrayList(lineDatabaseHelper.quertByKey(key).sortedWith(comparator))

        val lineNameList = res.map { it.name }
        val linInfoList = ArrayList(res.map {
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

        linInfoList.add("在线搜索路线")

        val adapter =
            ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_list_item_1,
                linInfoList
            )
        dialogBinding.lineList.adapter = adapter
        dialogBinding.lineList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                // 本地路线
                if (position < lineNameList.size && lineNameList[position] != "") {
                    originLine = lineDatabaseHelper.quertByName(lineNameList[position]).first()
                    initLineInterval()
                    loadLine(lineDatabaseHelper.quertByName(lineNameList[position]).first())
                    currentLineStationState = onNext
                    utils.haptic(dialogBinding.root)
                    alertDialog.cancel()
                }
                // 云端路线
                else {
                    val busLineQuery = BusLineQuery(
                        dialogBinding.lineNameInput.text.toString(),
                        BusLineQuery.SearchType.BY_LINE_NAME,
                        "桂林"
                    )
                    busLineQuery.pageNumber = 0
                    busLineQuery.extensions = "all"
                    val busLineSearch = BusLineSearch(requireContext(), busLineQuery)
                    busLineSearch.setOnBusLineSearchListener { res, rCode ->

                        if (res.busLines.isNotEmpty()) {

                            cloudStationList.clear()
                            var upLineStationStr = ""
                            var downLineStationStr = ""
                            for (x in 0..1) {
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
                                        0 ->
                                            upLineStationStr += "$id "

                                        1 ->
                                            downLineStationStr += "$id "
                                    }
                                }
                            }

                            val line =
                                Line(
                                    -1,
                                    res.busLines[0].busLineName.split("路").first(),
                                    upLineStationStr,
                                    downLineStationStr,
                                    false
                                )

                            CoroutineScope(Dispatchers.Main).launch {
                                originLine = line
                                initLineInterval()
                                loadLine(line)
                                currentLineStationState = onNext
                                utils.haptic(binding.headerMiddleNew)
                                alertDialog.cancel()
                            }


                        } else {
                            utils.showMsg("还没有这条路线，请重试")
                        }


                    }
                    busLineSearch.searchBusLineAsyn()
                }
            }
    }

    private fun requestManageFilesAccessPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.request_manage_files_access_permission_title))
                .setMessage(resources.getString(R.string.request_manage_files_access_permission_text))
                .setPositiveButton(resources.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                    permissionManager.requestManageFilesAccessPermission()
                }.setNegativeButton(resources.getString(android.R.string.cancel), null).create()
                .show()
            return false
        } else {
            return true
        }
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
            val uiModeManager = requireContext().getSystemService(UI_MODE_SERVICE) as UiModeManager
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

    fun refreshHeader() {
        if (!isAdded)
            return


        if (binding.headerMiddleNew.isShowFinish) {
            binding.headerMiddleNew.setText(currentLine.name)
        }

        lineHeadCardImmediatelyRefresh = false

        var leftInfo = ""
        var rightInfo = ""

        when (lineHeadCardCurrentShow) {

            onWel -> {
                leftInfo = utils.getWelInfo(0)
                rightInfo = utils.getWelInfo(1)
            }

            onStartAndTerminal -> {
                if (currentLineStationList.isNotEmpty() && currentLine.name != resources.getString(
                        R.string.line_all
                    )
                ) {
                    if (utils.getUILang() == "zh") {
                        leftInfo = currentLineStationList.first().cnName
                        rightInfo = currentLineStationList.last().cnName
                    } else {
                        leftInfo = currentLineStationList.first().enName
                        rightInfo = currentLineStationList.last().enName
                    }
                } else {
                    leftInfo = resources.getString(R.string.header_wel_left)
                    rightInfo = resources.getString(R.string.header_wel_right)
                }

            }

            onNextOrArrive -> {
                leftInfo = when (currentLineStationState) {
                    onNext -> resources.getString(R.string.next)
                    onWillArrive -> resources.getString(R.string.will_arrive)
                    onArrive -> resources.getString(R.string.arrive)
                    else -> ""
                }
                //判断当前是否为起点站
                var info = ""
                if (currentLineStationCount == 0) info += "${resources.getString(R.string.starting_station)} "
                //判断当前是否为终点站
                else if (currentLineStationCount == currentLineStationList.size - 1) info += "${
                    resources.getString(
                        R.string.terminal
                    )
                } "
                info += if (utils.getUILang() == "zh")
                    currentLineStation.cnName
                else
                    currentLineStation.enName
                rightInfo = info
            }

            onSpeed -> {
                leftInfo = resources.getString(R.string.header_speed)
                rightInfo = String.format(Locale.CHINA, "%.1fkm/h", currentSpeedKmH)
            }
        }

        binding.headerLeftNew.setText(leftInfo)
        binding.headerRightNew.setText(rightInfo)

        lineHeadCardCurrentShowIndex =
            (lineHeadCardCurrentShowIndex + 1) % (lineHeadCardShowList!!.size)
        lineHeadCardCurrentShow =
            lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

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
            loadLine(allStationLine)
            currentLineStationState = onNext

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
        refreshLineHeadDisplay()

        //更新路线站点更新信息和系统通知
        refreshLineStationChangeInfo()

        //刷新站点标点
        refreshStationMarker()
    }


}
