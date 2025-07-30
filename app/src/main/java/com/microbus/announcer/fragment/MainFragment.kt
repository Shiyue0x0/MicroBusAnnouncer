package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
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
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import com.arthenica.mobileffmpeg.FFmpeg
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.StationOfLineAdapter
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogStationInfoBinding
import com.microbus.announcer.databinding.FragmentMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


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

    private lateinit var utils: Utils

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

    private var lastTimeMillis = System.currentTimeMillis()
    private var currentTimeMillis = System.currentTimeMillis()

    private val mLooper: Looper = Looper.getMainLooper()

    private val mHandler: Handler = Handler(mLooper)

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
    private var currentLine = Line()

    /**当前路线站点下标（String形式）序列*/
    private var currentLineStationIndexListStr = listOf<String>()

    /**当前路线站点下标序列*/
    private var currentLineStationIdList = ArrayList<Int>()

    /**当前路线站点运行方向*/
    private var currentLineDirection = onUp

    /**当前路线站点起点站中文名称*/
    private var currentLineStartingStationCnName = "起点未知"

    /**当前路线站点起点站英文名称*/
    private var currentLineStartingStationEnName = "unknown"

    /**当前路线站点终点站中文名称*/
    private var currentLineTerminalCnName = "终点未知"

    /**当前路线站点终点站英文名称*/
    private var currentLineTerminalEnName = "unknown"

    /**当前路线站点列表*/
    private var currentLineStationList = ArrayList<Station>()

    /**当前路线站点*/
    private var currentLineStation =
        Station(null, "未知站点", "未知站点", currentLngLat.longitude, currentLngLat.latitude)

    /**当前路线运行站点计数，对应currentLineStation的下标*/
    private var currentLineStationCount = 0
    private var currentLineStationState: Int = onNext

    //    private var markerList = ArrayList<Marker>()
    private var circleList = ArrayList<Circle>()
    private val polylineList = ArrayList<Polyline>()
    private val lineLatLngList = ArrayList<LatLng>()
    private val lineLatLngForStationList = ArrayList<Int>()

    /**上次定位路线站点距离列表*/
    private val lastDistanceToStationList = ArrayList<Double>()

    /**本次定位路线站点距离列表*/
    private val currentDistanceToStationList = ArrayList<Double>()

    /**当前速度*/
    private var currentSpeedKmH = -1.0

    /**路线到站序列*/
    private var lineArriveStationIdList = ArrayList<Int>()

    /**即将到站标识列表*/
    private var willArriveStationList = ArrayList<Boolean>()

    //路线头牌显示序列
    private var lineHeadCardShowList: MutableSet<String>? = null

    //路线头牌当前显示下标
    private var lineHeadCardCurrentShowIndex = 0

    //路线头牌当前显示
    private var lineHeadCardCurrentShow = onNextOrArrive

    //路线头牌刷新Runnable
    private var lineHeadCardRefreshRunnable: Runnable? = null

    private val autoFollowNavigationHandler = Handler(mLooper)
    private var autoFollowNavigationRunnable: Runnable? = null

    //路线头牌刷新计时
    private var lineHeadCardRefreshTime = 0

    //路线头牌立即刷新标识
    private var lineHeadCardImmediatelyRefresh = false

    private lateinit var lineStationCardLayoutManager: LinearLayoutManager

    private var mediaPlayer: MediaPlayer? = null
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

    var locationCount = 0

    @SuppressLint("SetTextI18n", "InternalInsetResource", "DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        if (_binding != null) return binding.root

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

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
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /**设置屏幕唤醒锁*/
        powerManager =
            requireActivity().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION") wakeLock =
            powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, tag)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

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

//        val locale = Locale("en")
//        Locale.setDefault(locale)
//        val config: Configuration = requireContext().resources.configuration
//        config.setLocale(locale)
//        requireContext().resources
//            .updateConfiguration(config, requireContext().resources.displayMetrics)
//        requireActivity().recreate()

        return binding.root
    }

    // 与用户交互时
    override fun onResume() {
        super.onResume()
        aMapView.onResume()
    }

    // 不再与用户交互时
    override fun onPause() {
        super.onPause()
        aMapView.onPause()

        // 保存历史位置
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("location", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putFloat("latitude", currentLngLat.latitude.toFloat())
            putFloat("longitude", currentLngLat.longitude.toFloat())
        }

        Log.d(tag, "onPause ${currentLngLat.longitude}")

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
    @SuppressLint("SetTextI18n")
    fun loadLine(line: Line) {

        //切换当前路线
        currentLine = line

        //加载路线名称
        binding.headerMiddle.text = currentLine.name
        binding.lineStationChangeInfo.text = currentLine.name

        //解析线路站点
        when (currentLineDirection) {
            onUp -> currentLineStationIndexListStr = currentLine.upLineStation.split(' ')
            onDown -> currentLineStationIndexListStr = currentLine.downLineStation.split(' ')
        }

        // val lineStationCount = currentLineStationIndexListStr.size

        //获取当前路线站点下标列表
        val stationStrIndexList = currentLineStationIndexListStr.toMutableList()
        currentLineStationIdList.clear()
        for (i in stationStrIndexList.indices) currentLineStationIdList.add(stationStrIndexList[i].toInt())

        //获取当前路线站点列表
        currentLineStationList.clear()
        var lineStationList: List<Station>
        for (stationIndex in currentLineStationIdList) {
            lineStationList = stationDatabaseHelper.queryById(stationIndex)
            if (lineStationList.isNotEmpty()) currentLineStationList.add(lineStationList.first())
        }


        currentLineStartingStationCnName = currentLineStationList.first().cnName
        currentLineStartingStationEnName = currentLineStationList.first().enName

        currentLineTerminalCnName = currentLineStationList.last().cnName
        currentLineTerminalEnName = currentLineStationList.last().enName

        //加载终点站卡片
        binding.terminalName.text = currentLineTerminalCnName

        binding.headerLeft.text = currentLineStartingStationCnName
        binding.headerRight.text = currentLineTerminalCnName

        //加载路线站点变更卡片
        binding.lineStationChangeInfo.text =
            binding.lineStationChangeInfo.text as String + " 开往 " + currentLineTerminalCnName

        //更新通知
        if (binding.locationSwitch.isChecked && notification != null && notificationManager != null) {
            notification!!.setContentText("${currentLine.name} 开往 $currentLineTerminalCnName")
            notification!!.setWhen(System.currentTimeMillis())
            notificationManager!!.notify(0, notification!!.build())
        }

        lineArriveStationIdList.clear()

        //更新当前站点为最近站点
        switchToNearestStation()

        //初始化距离站点距离
        val arriveStationDistance = utils.getArriveStationDistance()
        lastDistanceToStationList.clear()
        currentDistanceToStationList.clear()
        willArriveStationList.clear()
        currentLineStationIdList.forEach { _ ->
            lastDistanceToStationList.add(arriveStationDistance + 1)
            currentDistanceToStationList.add(arriveStationDistance + 1)
            willArriveStationList.add(false)
        }

        //显示路线站点框
        if (binding.lineStationCard.isInvisible) {
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            fadeIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    binding.lineStationCard.visibility = VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            binding.lineStationCard.startAnimation(fadeIn)
        }

        // 自动切换站点判定距离
        if (line.name.length >= 2 && ArrayList<String>(
                listOf(
                    "U1", "U2"
                )
            ).contains(line.name.substring(0, 2))
        ) {
            prefs.edit { putString("arriveStationDistance", "20") }
        }
        // 500m
        else if (line.name.isNotEmpty() && ArrayList<String>(
                listOf(
                    "C", "D", "G", "S", "T", "Y", "Z"
                )
            ).contains(line.name.substring(0, 1))
        ) {
            prefs.edit { putString("arriveStationDistance", "500") }
            // Under
        } else if (line.name.length >= 3 && line.name.isNotEmpty() && ArrayList<String>(
                listOf(
                    "NNU",
                )
            ).contains(line.name.substring(0, 3))
        ) {
            prefs.edit { putString("arriveStationDistance", "50") }
        } else {
            prefs.edit { putString("arriveStationDistance", "30") }
        }

        //获取轨迹（开启纠偏时）
        //每8个站点为1组
        if (utils.getIsLineTrajectoryCorrection()) {

            CoroutineScope(Dispatchers.IO).launch {
                val routeSearch = RouteSearch(requireContext())
                val currentLineStationGroup = ArrayList<Station>()

                lineLatLngList.clear()

                for (i in currentLineStationList.indices) {
                    currentLineStationGroup.add(currentLineStationList[i])
                    if (currentLineStationGroup.size >= 8 || i == currentLineStationList.size - 1) {
                        val fromAndTo = RouteSearch.FromAndTo(
                            LatLonPoint(
                                currentLineStationGroup.first().latitude,
                                currentLineStationGroup.first().longitude
                            ), LatLonPoint(
                                currentLineStationGroup.last().latitude,
                                currentLineStationGroup.last().longitude
                            )
                        )
                        var query: RouteSearch.DriveRouteQuery
                        if (currentLineStationGroup.size > 2) {
                            // 途径点（去掉第一个和最后一个站点的列表）
                            val passedByPoints = ArrayList<LatLonPoint>()
                            for (j in currentLineStationGroup.indices) {
                                if (j != 0 && j != currentLineStationGroup.size - 1) {
                                    passedByPoints.add(
                                        LatLonPoint(
                                            currentLineStationGroup[j].latitude,
                                            currentLineStationGroup[j].longitude
                                        )
                                    )
                                }
                            }
                            query = RouteSearch.DriveRouteQuery(
                                fromAndTo,
                                RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST_AVOID_CONGESTION,
                                passedByPoints,
                                null,
                                ""
                            )
                        } else {
                            query = RouteSearch.DriveRouteQuery(
                                fromAndTo,
                                RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST_AVOID_CONGESTION,
                                null,
                                null,
                                ""
                            )
                        }
                        @Suppress("DEPRECATION") val result = routeSearch.calculateDriveRoute(query)
                        for (path in result.paths) {
                            for (step in path.steps) {
                                for (polyline in step.polyline) {
                                    lineLatLngList.add(
                                        LatLng(
                                            polyline.latitude, polyline.longitude
                                        )
                                    )
//                                    lineLatLngForStationList.add(i - 1)
                                }
                            }
                        }
                        currentLineStationGroup.clear()
                        currentLineStationGroup.add(currentLineStationList[i])
                    }
                }

                // 查找路线所属站点
                lineLatLngForStationList.clear()
                var i = 0
                for (lineLatLng in lineLatLngList) {

                    MultiPointItem(lineLatLng)
//                    Log.d(tag, "lineLatLng")
//                    Log.d(
//                        tag,
//                        lineLatLng.longitude.toString() + " " + lineLatLng.latitude.toString()
//                    )
//                    Log.d(
//                        tag,
//                        currentLineStationList[i].longitude.toString() + " " + currentLineStationList[i].latitude.toString()
//                    )

                    val distance = utils.calculateDistance(
                        lineLatLng.longitude,
                        lineLatLng.latitude,
                        currentLineStationList[i].longitude,
                        currentLineStationList[i].latitude
                    )

                    // 如果当前路径的坐标接近站点的坐标
                    if (distance < 10) {
                        i += 1
                    }
                    lineLatLngForStationList.add(i - 1)
                }

                //刷新站点标点
                refreshStationMarker()
            }

        }
        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新站点标点
        if (!utils.getIsLineTrajectoryCorrection()) refreshStationMarker()

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
            onLocationChange(location)
        }

        locationClient.startLocation()
    }

    /**
     * 初始化按钮回调
     */
    fun initButtonClickListener() {

        //单击路线名称切换路线
        binding.headerMiddle.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            val lineList = lineDatabaseHelper.quertAll()
            val lineInfoList = arrayOfNulls<String>(lineList.size + 1)

            for (i in 0 until lineList.size) {
                val lineStationIndexListStr = lineList[i].upLineStation.split(' ')

                val lineStartingStation =
                    stationDatabaseHelper.queryById(lineStationIndexListStr.first().toInt())
                val lineTerminal =
                    stationDatabaseHelper.queryById(lineStationIndexListStr.last().toInt())

                val lineStartingStationCnName =
                    if (lineStartingStation.isNotEmpty()) lineStartingStation.first().cnName
                    else "未知"

                val lineTerminalCnName = if (lineTerminal.isNotEmpty()) lineTerminal.first().cnName
                else "未知"

                lineInfoList[i] =
                    "${lineList[i].name}  $lineStartingStationCnName - $lineTerminalCnName"
            }

            //添加全站点路线
            val allStationLine = Line(name = "全站点路线", isUpAndDownInvert = false)
            val stationList = stationDatabaseHelper.quertAll()
            if (stationList.size >= 2) {
                for (station in stationList) {
                    allStationLine.upLineStation += "${station.id} "
                    allStationLine.downLineStation += "${station.id} "
                }
                val length = allStationLine.upLineStation.length
                allStationLine.upLineStation = allStationLine.upLineStation.substring(0, length - 1)
                allStationLine.downLineStation =
                    allStationLine.downLineStation.substring(0, length - 1)
                lineList.add(allStationLine)
                lineInfoList[lineList.size - 1] = "全站点路线"
            } else {
                lineList.add(Line())
                lineInfoList[lineList.size - 1] = "请先添加至少2个站点"
            }


            AlertDialog.Builder(context).setTitle("切换路线").setItems(lineInfoList) { _, which ->
                if (lineInfoList[which] != "") {
                    originLine = lineList[which]
                    initLineInterval()
                    loadLine(lineList[which])
                    currentLineStationState = onNext
                }
            }.create().show()

            utils.haptic(binding.headerMiddle)
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
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            val textView = TextView(requireContext())
            textView.text = binding.lineStationChangeInfo.text
            textView.setLineSpacing(100f, 0f)
            textView.setPadding(100, 50, 100, 50)
            val scrollView = ScrollView(requireContext())
            scrollView.addView(textView)
            AlertDialog.Builder(requireContext()).setTitle("运行信息").setView(scrollView).create()
                .show()
        }

        //长按地图定位按钮，开启/关闭操作锁定
        binding.mapLocation.setOnLongClickListener {
            run {
                isOperationLock = !isOperationLock
                if (isOperationLock) utils.showMsg("已开启操作锁定")
                else utils.showMsg("已解除操作锁定")
                utils.haptic(requireView())
            }
            true
        }

        //启用定位按钮
        binding.locationSwitch.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
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
                locationCount = 0
            }
            binding.switchFollowLocation.isChecked = isChecked

        }

        //跟随定位开关
        binding.switchFollowLocation.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }
        }

        //切换上下行开关
        binding.lineDirectionSwitch.setOnCheckedChangeListener { switchCompat, isChecked ->
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                switchCompat.isChecked = !switchCompat.isChecked
                return@setOnCheckedChangeListener
            }
            refreshLineDirection(isChecked)
        }

        //起点站按钮
        binding.startingStation.scaleX = -1f
        binding.startingStation.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            val lastLineStationCount = currentLineStationCount
            changeStation(0, currentLineStationState)
            if (currentLineStationCount != lastLineStationCount) utils.haptic(binding.startingStation)
        }

        //终点站按钮
        binding.terminal.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            val lastLineStationCount = currentLineStationCount
            changeStation(currentLineStationList.size - 1, currentLineStationState)
            if (currentLineStationCount != lastLineStationCount) utils.haptic(binding.terminal)
        }

        //上站按钮
        binding.lastStation.setOnClickListener {
            val lastLineStationState = currentLineStationState
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            lastStation()
            if (currentLineStationState != lastLineStationState) utils.haptic(binding.lastStation)
        }

        //下站按钮
        binding.nextStation.setOnClickListener {
            val lastLineStationState = currentLineStationState

            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            nextStation()
            if (currentLineStationState != lastLineStationState) utils.haptic(binding.nextStation)

        }

        //报本站按钮
        binding.voiceAnnouncement.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                return@setOnClickListener
            }
            //语音播报当前站点
            announce()

            utils.haptic(requireView())
        }

    }

    /**
     * 初始化头牌
     */
    private fun initHeadSign() {

        /*
        * 自动刷新路线头牌
        */
        //路线头牌显示序列
        lineHeadCardShowList = utils.getHeadSignShowInfo()
        //路线头牌当前显示下标
        lineHeadCardCurrentShowIndex = 0
        //路线头牌当前显示
        lineHeadCardCurrentShow =
            lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

        //文字移动速度（像素每秒）
        var textMoveSpeed: Int
        //如果文字超出显示范围，在该文本前添加的填充文本，确保超出范围的文字依次显示
        var fillSpaceStr: String
        var leftInfo = ""
        var rightInfo = ""

        val paint = Paint()

        var fillSpaceCount: Int

        var lineCardStartingStationTextWidth: Float
        var lineCardTerminalTextWidth: Float

        lineHeadCardRefreshRunnable = object : Runnable {
            override fun run() {

                lineHeadCardRefreshTime++

                paint.textSize = binding.headerRight.textSize

                lineCardStartingStationTextWidth =
                    paint.measureText(binding.headerRight.text.toString())

                lineCardTerminalTextWidth = paint.measureText(binding.headerRight.text.toString())

                fillSpaceCount = (binding.headerLeft.width / paint.measureText(" ")).toInt()

                fillSpaceStr = " ".repeat(fillSpaceCount)

                textMoveSpeed = paint.measureText("车 ").toInt()

                if (lineHeadCardImmediatelyRefresh || (lineHeadCardRefreshTime >= utils.getLineHeadCardChangeTime() && lineHeadCardRefreshTime >= (lineCardStartingStationTextWidth - binding.headerLeft.width) / textMoveSpeed && lineHeadCardRefreshTime >= (lineCardTerminalTextWidth - binding.headerRight.width) / textMoveSpeed)) {

                    lineHeadCardImmediatelyRefresh = false

                    lineHeadCardRefreshTime = 0

                    when (lineHeadCardCurrentShow) {
                        onWel -> {
                            leftInfo = utils.getWelInfo(0)
                            rightInfo = utils.getWelInfo(1)
                        }

                        onStartAndTerminal -> {
                            leftInfo = currentLineStartingStationCnName
                            rightInfo = currentLineTerminalCnName
                        }

                        onNextOrArrive -> {
                            leftInfo = when (currentLineStationState) {
                                onNext -> "下一站"
                                onWillArrive -> "即将到站"
                                onArrive -> "到达"
                                else -> ""
                            }
                            //判断当前是否为起点站
                            var info = ""
                            if (currentLineStationCount == 0) info += "起点站 "
                            //判断当前是否为终点站
                            else if (currentLineStationCount == currentLineStationIdList.size - 1) info += "终点站 "
                            info += currentLineStation.cnName
                            rightInfo = info
                        }

                        onSpeed -> {
                            leftInfo = "速度 Speed"
                            rightInfo = String.format(Locale.CHINA, "%.1fkm/h", currentSpeedKmH)
                        }
                    }

                    // 文本过长时，往文本开头添加刚好占满一行的空格，实现文字滚动
                    if (paint.measureText(leftInfo) + textMoveSpeed > binding.headerLeft.width) leftInfo =
                        "$fillSpaceStr$leftInfo"
                    if (paint.measureText(rightInfo) + textMoveSpeed > binding.headerRight.width) rightInfo =
                        "$fillSpaceStr$rightInfo"

                    binding.headerLeft.text = leftInfo
                    binding.headerRight.text = rightInfo

                    lineHeadCardCurrentShowIndex =
                        (lineHeadCardCurrentShowIndex + 1) % (lineHeadCardShowList!!.size)
                    lineHeadCardCurrentShow =
                        lineHeadCardShowList!!.elementAt(lineHeadCardCurrentShowIndex).toInt()

                }

                mHandler.postDelayed(this, 1000L)
            }
        }
        mHandler.postDelayed(lineHeadCardRefreshRunnable as Runnable, 1000L)
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

        //播放完毕后，释放音频焦点
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

                    if (i == 0)
                        continue

                    val lastPolyline = path.polyline[i - 1]
                    val currentPolyline = path.polyline[i]

                    // 求路线一般式方程

//                    A = Y2 - Y1
//                    B = X1 - X2
//                    C = X2*Y1 - X1*Y2

                    val x1 = lastPolyline.longitude / 111110
                    val x2 = currentPolyline.longitude / 111110
                    val y1 = lastPolyline.latitude / 111110
                    val y2 = currentPolyline.latitude / 111110

                    val lineA = y2 - y1
                    val lineB = x1 - x2
                    val lineC = (x2 * y1) * (x1 * y2)

                    for (station in stationFullList) {
                        // 计算当前路线到站点圆心的距离
                        val x = station.longitude / 111110
                        val y = station.latitude / 111110
                        val distance =
                            abs(lineA * x + lineB * y + lineC) /
                                    sqrt(lineA.pow(2) + lineB.pow(2))
                        Log.d(tag, distance.toString())
//                            val distance = utils.calculateDistance(
//                                polyline.longitude, //经度 x
//                                polyline.latitude,  //纬度 y
//                                station.longitude,
//                                station.latitude
//                            )
                        if (stationList.indexOf(station) == -1) {
                            if (distance < 30) {
                                stationList.add(station)
                            }
                        }
                    }


                }

                val planLine =
                    Line(name = "${stationList.last().cnName}线", isUpAndDownInvert = false)
                if (stationList.size >= 2) {
                    for (station in stationList) {
                        planLine.upLineStation += "${station.id} "
                        planLine.downLineStation += "${station.id} "
                    }
                    val length = planLine.upLineStation.length
                    planLine.upLineStation = planLine.upLineStation.substring(0, length - 1)
                    planLine.downLineStation = planLine.downLineStation.substring(0, length - 1)
                } else {
                    utils.showMsg("路径过短，建议重新规划")
                }

                originLine = planLine
                initLineInterval()
                loadLine(planLine)
                currentLineStationState = onNext
            }

        })
    }

    /**
     * 初始化地图
     */
    @SuppressLint("ResourceType")
    private fun initMap() {
        aMapView = binding.map
        aMapView.onCreate(null)
        aMap = aMapView.map

        aMap.isMyLocationEnabled = false

        binding.mapContainer.setScrollView(binding.main)

        val markerDrawableIds = ArrayList<Int>()
        markerDrawableIds.add(R.drawable.marker_gray)
        markerDrawableIds.add(R.drawable.marker_blue)
        markerDrawableIds.add(R.drawable.marker_green)

        markerDrawableIds.forEach {
            val overlayOptions =
                MultiPointOverlayOptions().icon(BitmapDescriptorFactory.fromResource(it))
            multiPointOverlayList.add(aMap.addMultiPointOverlay(overlayOptions)!!)
        }

//        val style = MyLocationStyle()
////        style.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE)
//        style.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
//        aMap.myLocationStyle = style

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

        aMap.setOnMapClickListener {

            if (isOperationLock) {
                utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
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
                val alertBinding =
                    AlertDialogStationInfoBinding.inflate(LayoutInflater.from(context))

                val alertDialog: AlertDialog? =
                    AlertDialog.Builder(requireContext()).setView(alertBinding.root)
                        ?.setTitle("添加站点")?.setNegativeButton("取消") { _, _ -> }
                        ?.setPositiveButton("提交", null)?.show()

                alertBinding.editTextLongitude.setText(it.longitude.toString())
                alertBinding.editTextLatitude.setText(it.latitude.toString())

                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                    val cnName = alertBinding.editTextCnName.text.toString()
                    var enName = alertBinding.editTextEnName.text.toString()

                    if (cnName == "") {
                        utils.showMsg("请填写中文名称")
                        return@setOnClickListener
                    }

                    if (enName == "") {
                        enName = cnName
                    }

                    if (alertBinding.editTextLongitude.text.toString() == "") {
                        utils.showMsg("请填写经度")
                        return@setOnClickListener
                    }


                    val longitudeRegex = Regex("(\\d+(\\.\\d+)?)( \\d+(\\.\\d+)?)?")
                    if (!alertBinding.editTextLongitude.text.matches(longitudeRegex)) {
                        utils.showMsg("经度格式错误")
                        return@setOnClickListener
                    }

                    if (alertBinding.editTextLatitude.text.toString() == "") {
                        if (!alertBinding.editTextLongitude.text.matches(longitudeRegex)) {
                            utils.showMsg("请填写经度")
                            return@setOnClickListener
                        }
                        val latLng = alertBinding.editTextLongitude.text.toString().split(' ')
                        alertBinding.editTextLongitude.setText(latLng[0])
                        alertBinding.editTextLatitude.setText(latLng[1])
                    }

                    val latitudeRegex = Regex("\\d+(\\.\\d+)?")
                    if (!alertBinding.editTextLatitude.text.matches(latitudeRegex)) {
                        utils.showMsg("纬度格式错误")
                        return@setOnClickListener
                    }

                    val longitude: Double =
                        alertBinding.editTextLongitude.text.toString().toDouble()
                    val latitude: Double =
                        alertBinding.editTextLatitude.text.toString().toDouble()

                    val station = Station(null, cnName, enName, longitude, latitude)
                    stationDatabaseHelper.insert(station)

                    alertDialog.cancel()
                }
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
                }.setNeutralButton("取消", null).show()

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

    @SuppressLint("SetTextI18n")
    fun onLocationChange(location: Location) {

        locationCount = (locationCount + 1) % Int.MAX_VALUE

        //更新位置与时间
        lastTimeMillis = currentTimeMillis
        currentTimeMillis = System.currentTimeMillis()

        lastLngLat = currentLngLat
        currentLngLat = LatLng(location.latitude, location.longitude)

        // 更新地图
        if (binding.switchFollowLocation.isChecked) {
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(lastLngLat))
            aMap.moveCamera(CameraUpdateFactory.changeBearing(location.bearing))
        }

        //更新定位标点
        if (!this::locationMarker.isInitialized) {
            locationMarker = aMap.addMarker(
                MarkerOptions().position(currentLngLat).setFlat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
            )
            locationMarker.setAnchor(0.5F, 0.57F)
        }

        locationMarker.rotateAngle = -location.bearing

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


//        距离格式化
        if (currentDistanceToCurrentStation >= 100000) {
            binding.currentDistanceToCurrentStationUnit.text = "(km)"
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.1f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 10000) {
            binding.currentDistanceToCurrentStationUnit.text = "(km)"
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.2f", currentDistanceToCurrentStation / 1000)
        } else if (currentDistanceToCurrentStation >= 1000) {
            binding.currentDistanceToCurrentStationUnit.text = "(km)"
            binding.currentDistanceToCurrentStationValue.text =
                String.format(Locale.CHINA, "%.3f", currentDistanceToCurrentStation / 1000)
        } else {
            binding.currentDistanceToCurrentStationUnit.text = "(m)"
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

        val arriveStationDistance = utils.getArriveStationDistance()    // 进站临界点
        val willArriveStationDistance = arriveStationDistance + 50    // 进站临界点

        //计算距离当前遍历站点距离
        for (stationIndex in currentLineStationList.indices) {
            lastDistanceToStationList[stationIndex] = currentDistanceToStationList[stationIndex]

            currentDistanceToStationList[stationIndex] = utils.calculateDistance(
                currentLngLat.longitude,
                currentLngLat.latitude,
                currentLineStationList[stationIndex].longitude,
                currentLineStationList[stationIndex].latitude
            )
        }

        if (locationCount < 2) return

        //遍历当前路线所有站点
        for (stationIndex in currentLineStationList.indices) {
            //进站条件：现在定位在这个站点内，且当前站点及状态不相同
            if (currentDistanceToStationList[stationIndex] <= arriveStationDistance && !(stationIndex == currentLineStationCount && currentLineStationState == onArrive)) {
                //切换到这个站点
                Log.d(
                    tag,
                    "到达站：${currentLineStationList[stationIndex].cnName} ${lastDistanceToStationList[stationIndex]} -> ${currentDistanceToStationList[stationIndex]}"
                )
                changeStation(stationIndex, onArrive)

                // 自动切换线路方向
                if (utils.getIsAutoSwitchLineDirection()) {
                    lineArriveStationIdList.add(stationIndex)
                    if (lineArriveStationIdList.size > 1 && lineArriveStationIdList.last() < lineArriveStationIdList[lineArriveStationIdList.size - 2]) {
//                                Log.d(tag, "站点：切换方向$lineArriveStationIdList")
                        lineArriveStationIdList.clear()
                        binding.lineDirectionSwitch.isChecked =
                            !binding.lineDirectionSwitch.isChecked
                        refreshLineDirection(binding.lineDirectionSwitch.isChecked)
                    }
                }

                announce()
                utils.longHaptic()
                break
            }
            //即将进站条件：上次位于即将进站范围外，现在位于即将进站范围内，且现在不位于进站进站内
            else if (lastDistanceToStationList[stationIndex] > willArriveStationDistance && currentDistanceToStationList[stationIndex] <= willArriveStationDistance && currentDistanceToStationList[stationIndex] > arriveStationDistance) {
                if (currentLineStationState != onNext || currentLineStationList[stationIndex].id != currentLineStation.id) {
                    changeStation(stationIndex, onNext)
                }
                changeStation(stationIndex, onWillArrive)
                announce(1)
                utils.longHaptic()
                break
            }
            //出站条件：上次位于某站点内，现在位于这个站点外（进站范围）
            else if (lastDistanceToStationList[stationIndex] < arriveStationDistance && currentDistanceToStationList[stationIndex] > arriveStationDistance) {
                if (stationIndex >= currentLineStationList.size - 1) break
                Log.d(
                    tag,
                    "${currentLineStationList[stationIndex].cnName} 下一站：${currentLineStationList[stationIndex + 1].cnName} ${lastDistanceToStationList[stationIndex]} -> ${currentDistanceToStationList[stationIndex]}"
                )
                changeStation(stationIndex + 1, onNext)
                announce()
                utils.longHaptic()
                break
            }

        }

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
            multiPointItem.title = "${station.id} ${station.cnName}"
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
            val indexText = if (i < 9) "0${i + 1}"
            else "${i + 1}"
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
            if (!utils.getIsLineTrajectoryCorrection()) {
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
        val lineWidth = 16f
        var mPolyline = aMap.addPolyline(
            PolylineOptions().addAll(mPolylineLatLngLists[0]).width(lineWidth)
//                .color(Color.argb(200, 182, 182, 182)
                .setCustomTexture((BitmapDescriptorFactory.fromResource(R.drawable.line_gray)))
        )!!
        polylineList.add(mPolyline)
        //当前处在的路径（蓝）
        mPolyline = aMap.addPolyline(
            PolylineOptions().addAll(mPolylineLatLngLists[1]).width(lineWidth)
//                .color(Color.argb(200, 25, 150, 216))
                .setCustomTexture((BitmapDescriptorFactory.fromResource(R.drawable.line_blue)))

        )!!
        polylineList.add(mPolyline)
        //还未经过的路径（绿）
        mPolyline = aMap.addPolyline(
            PolylineOptions().addAll(mPolylineLatLngLists[2]).width(lineWidth)
//                .color(Color.argb(200, 55, 178, 103))
                .setCustomTexture((BitmapDescriptorFactory.fromResource(R.drawable.line_green)))

        )!!
        polylineList.add(mPolyline)

        // 绘制站点序号与名称
        for (i in currentLineStationList.indices) {

            val textOptions = TextOptions().text("").position(
                LatLng(
                    currentLineStationList[i].latitude, currentLineStationList[i].longitude
                )
            ).fontSize(32)
            aMapStationTextList.add(aMap.addText(textOptions)!!)

            //名称
//            textOptions = TextOptions()
//                .text(currentLineStationList[i].cnName)
//                .position(
//                    LatLng(
//                        currentLineStationList[i].latitude + 0.0002,
//                        currentLineStationList[i].longitude
//                    )
//                )
//                .fontColor(textColor)
//                .fontSize(32)
//                .backgroundColor(Color.TRANSPARENT)
//                .visible(false)
//            aMapStationNameTextList.add(aMap.addText(textOptions)!!)
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
     * 更新上下行
     * @param isChecked 更新前的状态（上行true，下行false）
     */
    private fun refreshLineDirection(isChecked: Boolean) {

        if (isChecked) {
            currentLineDirection = onUp
            binding.lineDirectionSwitch.text = "路线上行"
        } else {
            currentLineDirection = onDown
            binding.lineDirectionSwitch.text = "路线下行"
        }

        if (currentLine.id == null) return

        //反转当前站点
        val lastLineStation = currentLineStation

        loadLine(currentLine)

//        currentLineStationCount = 0
        currentLineStation =
            stationDatabaseHelper.queryById(currentLineStationIdList.first()).first()

        //若切换后站点仍在列表中，切换为该站
        for (stationIndex in currentLineStationIdList.indices) if (currentLineStationIdList[stationIndex] == lastLineStation.id) {
            currentLineStationCount = stationIndex
            currentLineStation = lastLineStation
            break
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

        //仅显示当前路线站点时，刷新地图标记
        if (utils.getMapStationShowType() == 2) {
            showCurrentStation()
        }

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新路线头屏
        refreshLineHeadDisplay()

        //更新路线站点更新信息
        refreshLineStationChangeInfo()

        //刷新站点标点
        refreshStationMarker()
    }

    /**
     * 下一站
     */
    private fun nextStation() {
        if (currentLineStation.id == null) return
        if (currentLineStationState == onNext || currentLineStationState == onWillArrive) {
            currentLineStationState = onArrive
        } else if (currentLineStationState == onArrive) {
            if (currentLineStationCount >= currentLineStationIdList.size - 1) return
            currentLineStationCount++
            currentLineStation = currentLineStationList[currentLineStationCount]
            currentLineStationState = onNext
        }

        //仅显示当前路线站点时，刷新地图标记
        if (utils.getMapStationShowType() == 2) {
            showCurrentStation()
        }

        //更新路线站点显示、小卡片和通知
        refreshLineStationList()

        //刷新路线头屏
        refreshLineHeadDisplay()

        //更新路线站点更新信息
        refreshLineStationChangeInfo()

        //刷新站点标点
        refreshStationMarker()
    }

    /**
     * 切换站点及站点状态
     * @param stationCount 要切换到的站点在路线中的序号
     * @param stationState 要切换的站点状态
     */
    private fun changeStation(stationCount: Int, stationState: Int) {

        if (stationCount < 0 || stationCount >= currentLineStationList.size) return

        currentLineStationState = stationState

        currentLineStation = currentLineStationList[stationCount]
        currentLineStationCount = stationCount

        //仅显示当前路线站点时，刷新地图标记
        if (utils.getMapStationShowType() == 2) {

//            //移除所有地图标点，清空标点列表
//            for (marker in markerList) marker.destroy()
//            markerList.clear()

            //刷新地图标记
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

        //设置高亮度15s屏幕唤醒锁
        wakeLock.acquire(15 * 1000L)
    }

    /**
     * 切换为最近站点
     */
    private fun switchToNearestStation() {
//        if (aMapLocationClient!!.isStarted) {
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

    }

    /**
     * 更新路线站点显示、小卡片和通知
     */
    private fun refreshLineStationList() {

        lineStationCardLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        //更新路线站点卡片
        binding.lineStationList.layoutManager = lineStationCardLayoutManager
        val adapter = StationOfLineAdapter(
            requireContext(),
            currentLineStationIdList,
            stationDatabaseHelper,
            currentLineStationCount
        )
        binding.lineStationList.adapter = adapter

        //单击切换区间起点/终点
        adapter.setOnItemClickListener(object : StationOfLineAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                if (isOperationLock) {
                    utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
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
                        changeStation(position, currentLineStationState)
                        refreshLineStationList()
                        utils.haptic(binding.lineStationList)
                    }.show()
            }


        })

        val currentStationStateText = when (currentLineStationState) {
            onNext -> "下一站"
            onWillArrive -> "即将到站"
            onArrive -> "到达"
            else -> ""
        }
        binding.currentStationState.text = currentStationStateText
        binding.currentStationName.text = currentLineStation.cnName

        //路线卡片滚动到当前站点
        if (currentLineStationCount >= 7) lineStationCardLayoutManager.scrollToPosition(
            currentLineStationCount - 7 + 1
        )
    }

    /**
     * 更新路线站点更新信息
     */
    @SuppressLint("SetTextI18n")
    private fun refreshLineStationChangeInfo() {

        //更新主控下方信息
        val oldInfo = binding.lineStationChangeInfo.text.toString()
        var newInfo = ""

        val dateFormat = SimpleDateFormat("[HH:mm:ss] ", Locale.getDefault())
        val dataTime = dateFormat.format(Date(System.currentTimeMillis()))

        binding.lineStationChangeInfo.text = oldInfo + "\n" + dataTime
        when (currentLineStationState) {
            onArrive -> newInfo += "到达 "
            onWillArrive -> newInfo += "即将到达 "
            onNext -> newInfo += "下一站 "
        }
        newInfo += currentLineStation.cnName
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
        mHandler.removeCallbacksAndMessages(null)
        mHandler.postDelayed(lineHeadCardRefreshRunnable as Runnable, 0)
    }

    /**
     * 语音播报
     * 音频格式标准
     * wav 单声道 32000HZ 192kbps
     * @param type 0：报进站或出站，1：报即将到站，2：单独报站名
     * @param station type为2时要播报站点
     * @param lang type为2时要播报的语种，cn，en，或者all（先报中文，再报英文）
     */
    fun announce(type: Int = 0, station: Station = Station(), lang: String = "") {

        if (!utils.getIsVoiceAnnouncements()) return

        //如果没有管理外部存储的权限，请求授予
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(context)
                .setTitle("允许 ${requireContext().resources.getString(R.string.app_name)} 访问设备上的文件吗？")
                .setMessage("应用需要该权限来读取报站语音资源")
                .setPositiveButton("前往授予") { _, _ ->
                    permissionManager.requestManageFilesAccessPermission()
                }.setNegativeButton("取消", null).create().show()
            return
        }

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
        }
        audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)

        val voiceList = ArrayList<String>()
        val filePathList = ArrayList<String>()
        val tempFilePath = requireContext().getExternalFilesDir("")?.path
        val tempFile = File("$tempFilePath/tempAudio.wav")

//        列车进站/出站播报
        if (type == 0) {

            //普通话报站
            if (currentLineStationState == onNext) {
                //本次列车开往
                voiceList.add(voiceList.size, "/cn/thisTrainIsBoundFor")
                //终点站名称
                voiceList.add(voiceList.size, "/cn/station/$currentLineTerminalCnName")
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

            //英语报站
            if (utils.getIsEnVoiceAnnouncements()) {
                if (currentLineStationState == onNext) {
                    //本次列车开往
                    voiceList.add(voiceList.size, "/en/thisTrainIsBoundFor")
                    //终点站名称
                    if (File("$appRootPath/Media/en/station/$currentLineTerminalEnName.wav").exists()) voiceList.add(
                        voiceList.size, "/en/station/$currentLineTerminalEnName"
                    )
                    else {
                        if (utils.getIsUseTTS()) {
                            voiceList.add(
                                voiceList.size,
                                "/cn/station/$currentLineTerminalCnName"
                            )
                        } else {
//                        utils.showMsg("报站音频缺失\nen/station/$currentLineTerminalEnName.wav")
                            voiceList.add(
                                voiceList.size,
                                "/cn/station/$currentLineTerminalCnName"
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

            if (currentLineStationState == onNext && currentSpeedKmH >= 10 && currentSpeedKmH < 100) {
                voiceList.add(voiceList.size, "/cn/当前时速")
                voiceList.addAll(utils.intToCnReading(currentSpeedKmH.toInt(), "/cn/time/"))
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

            if (!this.isActive) return@launch

            //新建缓存文件目录
            File(tempFilePath!!).mkdirs()
            //创建缓存文件
            tempFile.createNewFile()
            //清除缓存文件内容
            val writer = FileWriter(tempFile)
            writer.write("")
            writer.close()

            val ttsNameList = ArrayList<String>()

//            Log.d(tag, "timeSS" + Date().time)

            if (utils.getIsUseTTS()) {

                File("$tempFilePath/tts").walkTopDown().forEach {
                    it.delete()
                }
                File("$tempFilePath/tts").mkdirs()

                var ttsTotalNum = 0
                var ttsDoneNum = 0
                val ttsIndexList = ArrayList<Int>()

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        // 开始合成
                    }

                    override fun onDone(utteranceId: String) {
                        ttsDoneNum++
                    }

                    override fun onError(utteranceId: String?) {
                    }

                })

                // 计算需要实时合成和语音数
                for (i in voiceList.indices) {
                    if (!File("$appRootPath/Media/${voiceList[i]}.wav").exists() && utils.getIsUseTTS()) {
                        ttsIndexList.add(i)
                        ttsTotalNum++
                    }
                }

                // 合成TTS
                for (i in 0 until ttsTotalNum) {
                    val ttsFileName = "ttsFile" + UUID.randomUUID().toString() + ".wav"
                    ttsNameList.add(ttsFileName)
                    val ttsFile = File("$tempFilePath/tts/", ttsFileName)
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsFileName)
                    tts.synthesizeToFile(
                        voiceList[ttsIndexList[i]].split('/').last(),
                        params,
                        ttsFile,
                        ttsFileName
                    )
                }

                while (true) {
                    if (!isActive) return@launch
                    Thread.sleep(100)
//                    Log.d(tag, "$id $ttsDoneNum/$ttsTotalNum")
                    if (ttsDoneNum >= ttsTotalNum) break
                }

            }

            // 合成音频
            var isHasVoice = false
            var ttsCount = 0
            for (i in voiceList.indices) {
//                Log.d(tag, voiceList[i])
                //寻找报站音频资源
                if (!File("$appRootPath/Media/${voiceList[i]}.wav").exists()) {
                    // 启用，使用TTS音频
                    if (utils.getIsUseTTS()) {
                        filePathList.add("$tempFilePath/tts/" + ttsNameList[ttsCount])
                        ttsCount++
                    }
                    // 不启用，跳过
                    else {
                        continue
                    }
                } else {
                    isHasVoice = true
                    filePathList.add("$appRootPath/Media/${voiceList[i]}.wav")
                }
            }

            if (!isHasVoice) return@launch

            // 合成WAV音频 开始
            val fos = FileOutputStream(tempFile)
            val dos = DataOutputStream(fos)

            // 写RIFF头部
            dos.writeBytes("RIFF")
            dos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())

            // 写WAVE头部
            dos.writeBytes("WAVE")
            dos.writeBytes("fmt ")
            dos.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x10).array()
            )
            dos.writeByte(1)    //表示pcm编码
            dos.writeByte(0)    //表示pcm编码
            dos.writeByte(1)    //表示单声道
            dos.writeByte(0)    //表示单声道
            dos.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32000).array()
            )   //采样率
            dos.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32000 * 1 * 16 / 8)
                    .array()
            )   //采样率*通道数*采样深度/8
            dos.writeByte(1 * 16.toByte()) //通道数*采样位数
            dos.writeByte(0) //通道数*采样位数
            dos.writeByte(1 * 16.toByte())   //每个样本的数据位数
            dos.writeByte(0) //通道数*采样位数
            dos.writeBytes("data")
            dos.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
            )   //音频数据长度
            dos.flush()

            //读取pcm写入
            for (file in filePathList) {
                if (!File(file).exists() || !isActive) {
                    return@launch
                }
                val fis: FileInputStream
                // 从TTS合成
                if (file.split("/").reversed()[1] == "tts") {
                    //44100 -> 32000
                    val ttsFilePath =
                        tempFilePath + "/tts/ttsFile" + UUID.randomUUID().toString() + ".wav"
                    val command = "-i $file -ar 32000 -ac 1 $ttsFilePath"
                    FFmpeg.cancel()
                    FFmpeg.execute(command)
                    if (!File(ttsFilePath).exists()) {
                        return@launch
                    }
                    fis = FileInputStream(ttsFilePath)
                }
                // 本地音频
                else {
                    fis = FileInputStream(file)
                }
                fis.channel.position(44)
                dos.write(fis.readBytes())
                fis.close()
            }
            dos.flush()
            dos.close()
            fos.close()


//            Log.d(tag, "size" + tempFile.length())
            //写入文件大小
            val raf = RandomAccessFile(tempFile, "rw")
            raf.seek(4)
            raf.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((tempFile.length() - 8).toInt()).array()
            )
            raf.seek(40)
            raf.write(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((tempFile.length() - 44).toInt()).array()
            )

            raf.close()

            // 合成WAV音频 结束


            //申请音频焦点
            audioManager!!.requestAudioFocus(audioFocusRequest!!)

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.fromFile(tempFile))
                val time =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.d(tag, "time $time")
                Handler(Looper.getMainLooper()).postDelayed({
                    audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
                }, time!!.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!isActive) return@launch

            //播放报站
            val audioStream = FileInputStream(tempFile)
            audioStream.skip(44)
            val audioData = utils.convertToByteArray(audioStream)
            if (::audioTrack.isInitialized && audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack.stop()
                audioTrack.release()
            }

            audioTrack = AudioTrack(
                audioAttributes, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, 1
            )

            audioTrack.play()
            audioTrack.write(audioData!!, 0, audioData.size)
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
}
