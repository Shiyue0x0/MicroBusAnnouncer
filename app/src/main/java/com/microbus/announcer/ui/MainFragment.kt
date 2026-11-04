package com.microbus.announcer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
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
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
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
import com.amap.api.maps.LocationSource
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.UiSettings
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MultiPointOverlay
import com.amap.api.maps.model.MultiPointOverlayOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.animation.AnimationSet
import com.amap.api.maps.model.animation.RotateAnimation
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.SensorHelper
import com.microbus.announcer.TabSwitchListener
import com.microbus.announcer.Utils
import com.microbus.announcer.activity.LineSwitcherActivity
import com.microbus.announcer.adapter.StationOfLineAdapter
import com.microbus.announcer.adapter.StationOfRunningInfoAdapter
import com.microbus.announcer.announce.AnnouncementContext
import com.microbus.announcer.announce.AnnouncementPlayer
import com.microbus.announcer.announce.AnnouncementProvider
import com.microbus.announcer.bean.AnMediaItem
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.RunningInfo
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogRunningInfoBinding
import com.microbus.announcer.databinding.FragmentMainBinding
import com.microbus.announcer.model.LineDirection
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.model.TabPage
import com.microbus.announcer.service.DrivingPlanning
import com.microbus.announcer.service.LinePathAndStationPointService
import com.microbus.announcer.service.NavigationService
import com.microbus.announcer.ui.compose.EsHeaderCompose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("MutableCollectionMutableState")
class MainFragment : Fragment() {

    private var tag = javaClass.simpleName

    private lateinit var utils: Utils
    private lateinit var binding: FragmentMainBinding
    private lateinit var prefs: SharedPreferences

    val lastDistanceToStationList = ArrayList<Double>()
    val currentDistanceToStationList = ArrayList<Double>()


    private var lastTimeMillis = System.currentTimeMillis()
    private var currentTimeMillis = System.currentTimeMillis()

    private val mainLooper = Looper.getMainLooper()

    private lateinit var aMapView: TextureMapView
    private lateinit var aMap: AMap

    /**0 灰色：已通过；1 蓝色：当前站点；2 绿色：前方站点*/
    private val multiPointOverlayList = ArrayList<MultiPointOverlay>()

    private lateinit var aMapUiSettings: UiSettings
    private var aMapLastZoom = 0F
    private var aMapCurrentZoom = 0F
    private val aMapZoomPoint = 16F


    private lateinit var lineDatabaseHelper: LineDatabaseHelper
    private lateinit var stationDatabaseHelper: StationDatabaseHelper


    /**初始位置：广西桂林市秀峰区十字街*/
    private var lastLngLat = LatLng(25.278617, 110.295833)
    var currentLngLat = lastLngLat

    private var currentBearing = 0.0

    private var currentDistanceToCurrentStation = 100.0
    private var lastDistanceToCurrentStation = 100.0

    var originLine = Line()

    private var currentLine by mutableStateOf(Line())

    /**当前路线站点运行方向（上下行）*/
    var currentLineDirection = LineDirection.ON_UP

    /**当前路线运行方向站点列表*/
    @get:SuppressLint("MutableCollectionMutableState")
    private var currentLineStationList by mutableStateOf(ArrayList<Station>())

    /**当前路线站点*/
    private var currentLineStation by mutableStateOf(
        Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
    )

    /** 当前路线运行站点计数（内部状态） */
    private val _currentLineStationCount = MutableStateFlow(0)

    /** 当前路线运行站点计数（外部订阅） */
    val currentLineStationCount: StateFlow<Int> = _currentLineStationCount

    /** 当前路线运行站点状态（内部状态） */
    private val _currentLineStationState = MutableStateFlow(StationStatus.ON_NEXT)

    /** 当前路线运行站点状态（外部订阅） */
    val currentLineStationState: StateFlow<Int> = _currentLineStationState


    /**当前速度*/
    private var currentSpeedKmH by androidx.compose.runtime.mutableDoubleStateOf(-1.0)

    /**路线到站序列*/
    private var lineArriveStationIdList = ArrayList<Int>()


    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: Notification.Builder

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


    private lateinit var audioStreamScope: Job

    lateinit var locationClient: AMapLocationClient

    lateinit var locationMarker: Marker

    lateinit var sensorHelper: SensorHelper

    private var cloudStationList = ArrayList<Station>()

    val lastStationHandler = Handler(mainLooper)
    val nextStationHandler = Handler(mainLooper)

    private lateinit var announcementLangList: ArrayList<String>

    private lateinit var permissionManager: PermissionManager
    private var hasInitNotice = false

    var matchCount = 0

    var userLocationOpen = false
    var userMapOpen = false

    val runningInfoList = ArrayList<RunningInfo>()

    val locationInfoList = ArrayList<String>()


    val simRunningHandler = Handler(mainLooper)

    var lastTouchMapTime = 0L

    private var tabSwitchListener: TabSwitchListener? = null

    private var enableLowPowerMode = false

    private var runningSimRunning = false

    private var esIsAnimating = mutableStateOf(false)

    private var startLineSwitcherForResult: ActivityResultLauncher<Intent>

    // 横屏状态下floatingView的宽度（dp）
    private val floatingViewWidthDpWhenLand = 400

    val lineName = mutableStateOf("")

    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    init {
        startLineSwitcherForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {

                    val data: Intent = result.data ?: return@registerForActivityResult

                    val action = data.getIntExtra("action", -1)
                    when (action) {

                        utils.LOAD_LOCAL_LINE -> {
                            val lineId = data.getIntExtra("lineId", -1)
                            if (lineId == -1) {
                                return@registerForActivityResult
                            }
                            setLine(lineDatabaseHelper.queryById(lineId).first())

                            val sharedPreferences =
                                requireContext().getSharedPreferences(
                                    "lastRunningInfo",
                                    MODE_PRIVATE
                                )
                            sharedPreferences.edit {
                                remove("onlineLineUpId")
                                remove("onlineLineDownId")
                            }

                            utils.haptic(binding.root)
                        }

                        utils.LOAD_LINE_ALL -> {
                            loadLineAll(true)

                            val sharedPreferences =
                                requireContext().getSharedPreferences(
                                    "lastRunningInfo",
                                    MODE_PRIVATE
                                )
                            sharedPreferences.edit {
                                remove("onlineLineUpId")
                                remove("onlineLineDownId")
                            }
                        }

                        utils.LOAD_CLOUD_LINE -> {
                            // todo
                            @Suppress("DEPRECATION") val line =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    data.getSerializableExtra("line", Line::class.java)
                                } else {
                                    data.getSerializableExtra("line") as? Line
                                }

                            if (line == null) {
                                return@registerForActivityResult
                            }

                            @Suppress("UNCHECKED_CAST", "DEPRECATION")
                            val cloudStationList =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    data.getSerializableExtra(
                                        "cloudStationList",
                                        ArrayList::class.java
                                    ) as? ArrayList<Station>
                                } else {
                                    data.getSerializableExtra("line") as? ArrayList<Station>
                                }

                            if (cloudStationList == null) {
                                return@registerForActivityResult
                            }

                            this.cloudStationList = cloudStationList

                            setLine(line)
                            utils.haptic(binding.root)

                        }

                        utils.SET_TEMPORARY_LINE_NAME -> {
                            val lineName =
                                data.getStringExtra("lineName") ?: return@registerForActivityResult
                            currentLine.name = lineName
                            // TODO
//                        binding.headerMiddleNew.showText(currentLine.name)
//                        binding.headerMiddleNew.requestLayout()
                        }


                    }


                } else {
                    // 用户取消或其他情况
                }
            }

        prefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                Log.d(tag, "onSharedPreferenceChanged: $key")
                when (key) {
                    "isNavMode" -> {
//                        if (utils.getIsNavMode()) {
//                            binding.navCard.visibility = VISIBLE
//                        } else {
//                            binding.navCard.visibility = GONE
//                        }
                    }

                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {


        binding = FragmentMainBinding.inflate(inflater, container, false)
//        return binding.root

        return ComposeView(requireContext()).apply {
            setContent { MainUI() }
        }

    }

    @Composable
    fun MainUI() {

        val controller = remember { ThemeController(ColorSchemeMode.System) }

        var hideBottomBar = false

        val currentLineStationCount by currentLineStationCount.collectAsState()
        val currentLineStationState by currentLineStationState.collectAsState()

        MiuixTheme(
            controller = controller
        ) {
            val backdrop = rememberLayerBackdrop()

            Scaffold(content = { paddingValues ->

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                    val isLandscape = maxWidth > maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        AndroidView(
                            factory = { binding.root }, modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (utils.getIsLiquidGlass()) {
                                        Modifier.layerBackdrop(backdrop)
                                    } else {
                                        Modifier
                                    }
                                )
                        )

                        Box(
                            modifier = Modifier
                                .then(
                                    // 竖屏
                                    if (!isLandscape) {
                                        Modifier.fillMaxWidth()
                                    } else {
                                        Modifier.width(floatingViewWidthDpWhenLand.dp)
                                    }
                                )
                                .padding(
                                    top = paddingValues.calculateTopPadding(),
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 0.dp
                                )
                                .align(Alignment.TopEnd)
                                .onSizeChanged { size ->
                                    binding.fill2.post {
                                        val lp = binding.fill2.layoutParams
                                        lp.height = size.height
                                        binding.fill2.layoutParams = lp
                                    }
                                }
                        ) {

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 顶部电显
                                EsHeaderCompose(

                                    line = currentLine,
                                    currentSpeedKmH = currentSpeedKmH,
                                    currentLineStation = currentLineStation,
                                    currentLineStationList = currentLineStationList,
                                    currentLineStationCount = currentLineStationCount,
                                    currentLineStationState = currentLineStationState,

                                    isAnimating = esIsAnimating.value,
                                    onHeaderClick = {
                                        if (isOperationLock) {
                                            utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                                            return@EsHeaderCompose
                                        }

                                        val intent =
                                            Intent(
                                                requireContext(),
                                                LineSwitcherActivity::class.java
                                            )
                                        intent.putExtra("currentLineId", currentLine.id)
                                        startLineSwitcherForResult.launch(intent)

                                        return@EsHeaderCompose
                                    },
                                    onHeaderLongClick = {

                                        with(binding) {
                                            val visibility = if (hideBottomBar) VISIBLE else GONE
                                            leftButtonGroup.visibility = visibility
                                            rightButtonGroup.visibility = visibility
                                            mainSwitchGroup.visibility = visibility
                                            lineStationController.visibility = visibility
                                        }

                                        val actionText = if (hideBottomBar)
                                            "展开"
                                        else
                                            "收起"

                                        utils.showMsg("已${actionText}控制栏", true)

                                        hideBottomBar = !hideBottomBar

                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (utils.getIsLiquidGlass()) {
                                                Modifier.drawBackdrop(
                                                    backdrop = backdrop,
                                                    shape = { CircleShape },
                                                    effects = {
                                                        vibrancy()
                                                        blur(2f.dp.toPx())
                                                        lens(
                                                            refractionHeight = 2f.dp.toPx(),
                                                            refractionAmount = 4f.dp.toPx()
                                                        )
                                                    }
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                )
                                // 播放文本
                                AnnouncementPlayingText(backdrop)
                            }

                        }

                    }

                }


            })


        }

    }

    @Composable
    fun AnnouncementPlayingText(backdrop: LayerBackdrop) {

        val text by anPlayer.playingText.collectAsStateWithLifecycle()
        val playbackDuration by anPlayer.playbackDuration.collectAsStateWithLifecycle()
        val playbackStartTime by anPlayer.playbackStartTime.collectAsStateWithLifecycle()

        val playbackProgress by produceState(0f, text, playbackDuration, playbackStartTime) {
            if (text.isEmpty() || playbackDuration <= 0L || playbackStartTime <= 0L) {
                value = 0f
                return@produceState
            }
            while (true) {
                withFrameNanos {
                    val elapsed =
                        (SystemClock.elapsedRealtime() - playbackStartTime).coerceAtLeast(0L)
                    value = (elapsed.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f)
                }
                if (value >= 1f) break
            }
        }

        val progressBarHeight = 4

        AnimatedVisibility(
            visible = text.isNotEmpty(),
            enter = expandVertically(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(2f.dp.toPx())
                            lens(
                                refractionHeight = 2f.dp.toPx(),
                                refractionAmount = 4f.dp.toPx()
                            )
                        }
                    ),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.an_contain_bg_tran)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null,
                onClick = {
                    anPlayer.stopAnnounce()
                    utils.showMsg("已停止播放", true)
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = text,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(
                                    top = 12.dp,
                                    bottom = 12.dp,
                                    start = 16.dp,
                                    end = (16 - progressBarHeight).dp
                                )
                                .basicMarquee(initialDelayMillis = 500)
                                .fillMaxWidth(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(
                                Font(R.font.galano_grotesque_bold, FontWeight.Bold)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(progressBarHeight.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(playbackProgress)
                                    .background(androidx.compose.ui.graphics.Color(0xFF1996D8))
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
    }

    fun initView(savedInstanceState: Bundle?) {


        utils = Utils(requireContext())

        sensorHelper = SensorHelper(requireContext())

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        lineDatabaseHelper = LineDatabaseHelper.getInstance(requireContext())
        stationDatabaseHelper = StationDatabaseHelper.getInstance(requireContext())

        currentDistanceToCurrentStation =
            utils.getStationRangeByLineType(currentLine.type, "In").toDouble() + 1
        lastDistanceToCurrentStation =
            utils.getStationRangeByLineType(currentLine.type, "In").toDouble() + 1

        /**设置屏幕唤醒锁*/
        powerManager =
            requireActivity().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, tag)

        //设置状态栏填充高度
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        permissionManager = PermissionManager(requireContext(), requireActivity())

        // 让内容延伸到状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用新的API
            binding.root.setOnApplyWindowInsetsListener { view, insets ->
                // 保留系统手势区域的内边距，但忽略状态栏
                val systemGestureInsets = insets.getInsets(
                    WindowInsets.Type.systemGestures()
                )
                // 设置内边距，只保留系统手势区域，让内容延伸到状态栏后面
                view.setPadding(
                    systemGestureInsets.left,
                    0, // 顶部不保留内边距，让内容延伸到状态栏
                    systemGestureInsets.right,
                    systemGestureInsets.bottom
                )
                insets
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
            binding.root.fitsSystemWindows = false
        }

        binding.lineName = getString(R.string.main_line_0)

        // 横屏/竖屏适配
        updFloatingViewWidth()

        // 初始化定位
        initLocation()

        // 初始化地图
        initMap(savedInstanceState)

        //初始化通知
        if (utils.getNotice()) {
            initNotification()
        }

        // 初始化报站
        initAnnouncement()

        // 初始化导航
        initNavigation()

        // 初始化路线路径服务
        initLinePathService()

        //初始化路线
        initLine()

        // 初始化按钮回调
        initButton()

        // 初始化路线规划
        initLinePlan()

        // 初始化本地广播
        initLocalBroadcast()

        announcementLangList = utils.getLangList()

        if (prefs.getBoolean("enableLowPowerMode", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000.milliseconds)
                withContext(Dispatchers.Main) {
                    switchPowerSavingMode(true)
                }
            }
        }

        utils.applyOrientation(resources.configuration.orientation, binding.floatingView)
    }

    private var isVisible = false


    /* 与用户交互时 */
    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")

        isVisible = true

        if (userMapOpen) {
            // 取消之前的延迟任务
            mapPauseRunnable?.let {
                binding.root.removeCallbacks(it)
            }
            binding.mapBtnGroup.check(binding.mapBtn.id)
        }

        if (userLocationOpen) {
            binding.locationBtnGroup.check(binding.locationBtn.id)
        }

        (binding.lineStationList.adapter as StationOfLineAdapter).isShown = true

//        binding.headerLeftNew.startAnimation()
//        binding.headerMiddleNew.startAnimation()
//        binding.headerRightNew.startAnimation()
        binding.navStationName.startAnimation()

        esIsAnimating.value = true

        aMapView.onResume()

        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        if (utils.getIsNavMode()) {
            binding.navCard.visibility = VISIBLE
        } else {
            binding.navCard.visibility = GONE
        }

    }

    private var mapPauseRunnable: Runnable? = null


    /* 不再与用户交互时 */
    override fun onPause() {
        Log.d(tag, "onPause")

        userMapOpen = binding.mapBtn.isChecked
        userLocationOpen = binding.locationBtn.isChecked

        // 取消之前的延迟任务
        mapPauseRunnable?.let {
            binding.root.removeCallbacks(it)
        }

        // 创建新的延迟任务
        mapPauseRunnable = Runnable {
            if (isAdded && !isDetached) {
                Log.d(tag, "L549")
                binding.mapBtnGroup.uncheck(binding.mapBtn.id)
            }
        }

        binding.root.postDelayed(mapPauseRunnable, 1000)
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)

        (binding.lineStationList.adapter as StationOfLineAdapter).isShown = false

        // 保存历史位置
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("location", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putFloat("latitude", currentLngLat.latitude.toFloat())
            putFloat("longitude", currentLngLat.longitude.toFloat())
        }

        isVisible = false

//        binding.headerLeftNew.stopAnimation()
//        binding.headerMiddleNew.stopAnimation()
//        binding.headerRightNew.stopAnimation()
        binding.navStationName.stopAnimation()

        esIsAnimating.value = false

        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)

        super.onPause()

    }

    override fun onStop() {
        Log.d(tag, "onStop")
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        aMapView.onSaveInstanceState(outState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabSwitchListener) {
            tabSwitchListener = context
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lastStationHandler.removeCallbacksAndMessages(null)
        nextStationHandler.removeCallbacksAndMessages(null)
        aMapView.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updFloatingViewWidth()
    }

    fun Station.toLngLatString(): String {
        val lng = String.format(Locale.US, "%.6f", longitude)
        val lat = String.format(Locale.US, "%.6f", latitude)
        return "$lng,$lat"
    }

    /**
     * 加载路线
     */
    fun loadLine(line: Line) {

        //切换当前路线
        currentLine = line

        // 写入历史
        val sharedPreferences =
            requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)
        sharedPreferences.edit(commit = true) {
            putString("lineName", currentLine.name)
        }

        //加载路线名称
//        binding.headerMiddle.text = currentLine.name
//        binding.headerMiddleNew.showText(currentLine.name)
        binding.lineName = currentLine.name
//        binding.headerMiddleNew.showText(binding.lineName!!)


        //获取当前方向路线站点下标（String形式）序列
        val currentLineStationIndexStrList = when (currentLineDirection) {
            LineDirection.ON_UP -> currentLine.upLineStation.split(' ')
            LineDirection.ON_DOWN -> currentLine.downLineStation.split(' ')
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
//                Log.d(
//                    "L855",
//                    "${currentLineStationList.last().cnName} ${currentLineStationList.last().bearing}"
//                )
            }
            // 云端路线
            else if (strIndex.toIntOrNull() != null) {
                val cloudStation =
                    cloudStationList.find { station -> station.id == strIndex.toInt() }
                if (cloudStation != null) {
                    currentLineStationList.add(cloudStation.copy())
                } else {
                    currentLineStationList.add(
                        Station(
                            id = Int.MAX_VALUE,
                            cnName = "未知站点",
                            enName = "unknown"
                        )
                    )
                }
            }

            if (currentLineStationList.isNotEmpty()) {
                val curStation = currentLineStationList.last()
                val sourceCnName = curStation.cnName
                if (!curStation.hasPraseName) {
                    curStation.cnName =
                        utils.getStationNameFromCn(sourceCnName, "cn", curStation)
                    curStation.enName =
                        utils.getStationNameFromCn(sourceCnName, "en", curStation)
                    curStation.hasPraseName = true
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


        // 删除网络加载列表
//        anResolver.clearPreloadingNetworkMediaFileList()

//        preloadingCommonText()

        //切换当前站点为最近站点
        switchToNearestStation()

//        preloadingStation()

        val viewList = ArrayList<View>()

        viewList.add(binding.lineDirectionBtnGroup)
        viewList.add(binding.terminalCard)

        viewList.add(binding.lastStation)
        viewList.add(binding.nextStation)

//        viewList.add(binding.startingStation)
//        viewList.add(binding.terminal)

        if (!enableLowPowerMode && utils.getIsNavMode()) {
            viewList.add(binding.navCard)
        }
        //显示|隐藏路线站点框和全站点路线按钮（渐出动画）
//        if (currentLine.name != resources.getString(R.string.line_all) && currentLine.name != resources.getString(
//                R.string.main_line_0
//            )
        if (currentLine.name != resources.getString(
                R.string.main_line_0
            )
        ) {
            if (!enableLowPowerMode) {

//            Log.d(tag, "出现")
                val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                fadeIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        binding.lineStationCard.visibility = INVISIBLE
//                        binding.liquidGlassViewOfLineStationCard.visibility = INVISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        binding.lineStationCard.visibility = VISIBLE
//                        binding.liquidGlassViewOfLineStationCard.visibility = VISIBLE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                binding.lineStationCard.startAnimation(fadeIn)
//                binding.liquidGlassViewOfLineStationCard.startAnimation(fadeIn)
                viewList.forEach { view ->
                    view.visibility = VISIBLE
                }

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
//                    if (binding.liquidGlassViewOfLineStationCard.isVisible)
//                        binding.liquidGlassViewOfLineStationCard.visibility = INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            binding.lineStationCard.startAnimation(fadeOut)
//            binding.liquidGlassViewOfLineStationCard.startAnimation(fadeOut)
            viewList.forEach { view ->
                view.visibility = GONE
            }
        }

        //更新路线站点卡片
        val adapter = binding.lineStationList.adapter as StationOfLineAdapter
        adapter.stationList = currentLineStationList
        adapter.stationCount = _currentLineStationCount.value
        adapter.stationState = _currentLineStationState.value
        adapter.mHandler.removeCallbacksAndMessages(null)
        adapter.tag = "Main"

        @SuppressLint("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()

        refreshUIAndPreloadingStationVoice()

        // TODO 驾车规划（获取 路线轨迹 和 导航信息）
        if (utils.getIsEnableOnlineNavigation() || utils.getIsLineTrajectoryCorrection()) {
            lifecycleScope.launch {
                val planner = DrivingPlanning(requireContext())

                val firstStation = currentLineStationList.first()
                val lastStation = currentLineStationList.last()

                val origin = firstStation.toLngLatString()
                val dest = lastStation.toLngLatString()

                // 途径站点
                val wayStationList =
                    currentLineStationList.subList(1, currentLineStationList.size - 1)
                val waypoints = wayStationList.joinToString(separator = ";") { it.toLngLatString() }

                val steps: List<DrivingPlanning.Step> = planner.planDrivingSteps(
                    origin = origin,
                    destination = dest,
                    waypoints = waypoints
                )

                steps.forEachIndexed { index, step ->
                    val stationCnName = currentLineStationList
                        .getOrNull(step.waypoint_index)
                        ?.cnName

                    Log.d(
                        "L1258",
                        "i=${index + 1}, for_station=${step.waypoint_index + 1}, station=${stationCnName}, step=${step}" +
                                "distance=${step.step_distance}, polylinePoints=${step.polyline.size}"
                    )
                }

                // 全站路线
                if (currentLine.name != resources.getString(R.string.line_all)){
                    navigationService.startNavigation(steps)
                }

                drawLinePathAndStationPoint(steps)

            }
        } else {
            drawLinePathAndStationPoint(null)
        }
    }


    private fun drawLinePathAndStationPoint(onlineSteps: List<DrivingPlanning.Step>?) {
        linePathAndStationPointService.draw(aMap, currentLineStationList, onlineSteps, currentLine.name)
        observeStationState(viewLifecycleOwner.lifecycleScope)
    }

    private val mockLocation = false
    lateinit var mockLocationList: List<Pair<Date, Location>>

    var baseMillis: Long = 0


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

        // todo MOCK 定位
        if (mockLocation) {
            mockLocationList = readLocationLog()
            Log.d("L1116", "${mockLocationList.size}")
            mockLocationList = normalizeTimes(mockLocationList)
            Log.d("L1117", "${mockLocationList.size}")

            mockLocationList.forEachIndexed { _, item ->


                // 变速处理，全局x10
                item.first.time /= 10

            }

            val mockLocationListGet40pers =
                mockLocationList.filterIndexed { index, _ -> index % 4 == 0 }


            val replayer = LocationReplayer(mockLocationListGet40pers) { location ->
                // 在这里处理，比如更新地图
                val aMapLocation = AMapLocation(location)
                onMyLocationChange(aMapLocation)
            }


            replayer.start()

        }

        if (!mockLocation) {
            locationClient.setLocationListener { location ->
                if (location.errorCode == 0)
                    if (utils.getIsEnableOnlineNavigation()) {
                        navigationService.onLocationChanged(location)
                    }
                onMyLocationChange(location)
            }
            locationClient.startLocation()
        }


    }

//    data class LogEntry(
//        val time: Date,
//        val location: Location
//    )


    fun readLocationLog(): List<Pair<Date, Location>> {
        val path = Environment.getExternalStorageDirectory().absolutePath + "/Announcer"
        val file = File(path, "location_log.txt")

        if (!file.exists()) return emptyList()

        val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val result = mutableListOf<Pair<Date, Location>>()

        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachLine

            // 按空白字符（一个或多个空格/制表符）分割
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size < 3) return@forEachLine

            // parts[0] = "[17:30:58]"  -> 去掉方括号
            val timeStr = parts[0].removePrefix("[").removeSuffix("]")
            val latStr = parts[1]
            val lonStr = parts[2]

            try {
                val time = fullFormat.parse("$today $timeStr") ?: return@forEachLine
                val latitude = latStr.toDouble()
                val longitude = lonStr.toDouble()

                val location = Location("").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.time = time.time
                }

                result.add(time to location)
            } catch (_: Exception) {
                // 忽略无法解析的行
            }
        }

        return result
    }

    fun normalizeTimes(entries: List<Pair<Date, Location>>): List<Pair<Date, Location>> {
        if (entries.isEmpty()) return emptyList()

        baseMillis = entries.first().first.time

        return entries.map { (time, location) ->
            val shifted = Date(time.time - baseMillis)
            // 可选：同步更新 Location.time
            location.time = shifted.time

            Log.d("L1201", "normalizeTimes: $shifted")

            shifted to location

        }
    }


    class LocationReplayer(
        private val entries: List<Pair<Date, Location>>,   // normalizeTimes 后的列表
        private val onMyLocationChange: (Location) -> Unit
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var index = 0
        private var running = false

        fun start() {
            if (running || entries.isEmpty()) return
            running = true
            index = 0
            scheduleNext()
        }

//        fun stop() {
//            running = false
//            handler.removeCallbacksAndMessages(null)
//        }

        private fun scheduleNext() {
            if (!running || index >= entries.size) {
                running = false
                return
            }

            val (time, location) = entries[index]
            var delay = time.time
//            Log.d("L1230", "scheduleNext: $delay")
            if (index > 0) {
                delay -= entries[index - 1].first.time
            }


            handler.postDelayed({
                if (!running) return@postDelayed
                onMyLocationChange(location)
                index++
                scheduleNext()
                Log.d("L1243", "$index $delay")
            }, if (index == 0) 10000 else delay)
        }
    }

    /**
     * 初始化按钮回调
     */
    fun initButton() {

        binding.mapBtnGroup.check(binding.mapBtn.id)
        binding.locationBtnGroup.uncheck(binding.locationBtn.id)
//        binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)


        //单击地图定位按钮，地图移动到当前位置
        binding.mapLocation.setOnClickListener {

            if (!permissionManager.hasLocationPermission()) {
                utils.showRequestLocationPermissionDialog(permissionManager)
                return@setOnClickListener
            }

            // 开启定位和地图
            binding.locationBtnGroup.check(binding.locationBtn.id)
            binding.mapBtnGroup.check(binding.mapBtn.id)

            // 地图移动到当前位置
            mapToCenter(true)
            lastTouchMapTime = System.currentTimeMillis() - 10_000

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

        // 进入/退出 省电模式
        binding.switchPowerSavingMode.setOnClickListener {
            switchPowerSavingMode(!enableLowPowerMode)
        }

        // 单击显示路线运行记录
        binding.runningInfo.setOnClickListener {
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                return@setOnClickListener
            }

            val dialogBinding = DialogRunningInfoBinding.inflate(LayoutInflater.from(context))

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

//                val intent = Intent(requireContext(), LocationService::class.java)
//                requireContext().startForegroundService(intent)

                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f
                binding.navStationCard.visibility = VISIBLE
                binding.navSpeedCard.visibility = VISIBLE

//                val myLocationStyle = MyLocationStyle()
//                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE)
//                aMap.myLocationStyle = myLocationStyle
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
                binding.currentDistanceToCurrentStationValue.text =
                    getString(R.string.main_distance_value)
                binding.navStationCard.visibility = GONE
                binding.navSpeedCard.visibility = GONE
//                val myLocationStyle = MyLocationStyle()
//                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW)
//                aMap.myLocationStyle = myLocationStyle
            }


        }

        // 启用/关闭地图按钮
        binding.mapBtn.addOnCheckedChangeListener { button, isChecked ->
            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                button.isChecked = !button.isChecked
                return@addOnCheckedChangeListener
            }
            if (isChecked) {
                aMapView.visibility = VISIBLE
                aMapView.onResume()
//                initMap()
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 1f

            } else {
                if (this::locationMarker.isInitialized)
                    locationMarker.alpha = 0f
                aMapView.onPause()
//                aMapView.onDestroy()
                aMapView.visibility = INVISIBLE
            }
        }

        //切换上下行开关
        binding.lineDirectionBtnGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->

            if (!isChecked)
                return@addOnButtonCheckedListener

            if (isOperationLock) {
                utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                group.post {
                    group.uncheck(checkedId)
                }
                return@addOnButtonCheckedListener
            }

            if (currentLine.isRingRoute) {
                utils.showMsg("该路线为环线")
                group.post {
                    group.uncheck(checkedId)
                }
                return@addOnButtonCheckedListener
            }

            currentLineDirection = if (checkedId == binding.lineDirectionBtnUp.id) {
                LineDirection.ON_UP
            } else {
                LineDirection.ON_DOWN
            }

            val sharedPreferences =
                requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)
            sharedPreferences.edit(commit = true) {
                putInt("lastLineDirection", currentLineDirection)
            }
            Log.d("L1292", "${sharedPreferences.getInt("lastLineDirection", LineDirection.ON_UP)}")

            if (currentLine.id == null) return@addOnButtonCheckedListener

            val lastLineStation = currentLineStation

            loadLine(currentLine)

            //若切换后站点仍在列表中，切换为该站
            for (stationIndex in currentLineStationList.indices) {
                if (currentLineStationList[stationIndex].cnName == lastLineStation.cnName) {
                    _currentLineStationCount.value = stationIndex
                    currentLineStation = lastLineStation
                    break
                }
            }

            if (currentLine.upLineStation == currentLine.downLineStation) {
                utils.showMsg("注意：该线是单向路线，上下行相同")
            }

            refreshUIAndPreloadingStationVoice()
        }

        if (false) {
            //切换到起点站按钮
            binding.startingStation.scaleX = -1f
            binding.startingStation.setOnClickListener {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setOnClickListener
                }
                if (_currentLineStationCount.value != 0) {
                    setStationAndState(0, StationStatus.ON_NEXT)
                    utils.haptic(binding.startingStation)
                }

            }

            //切换到终点站按钮
            binding.terminal.scaleX = 1f
            binding.terminal.setOnClickListener {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setOnClickListener
                }
                if (_currentLineStationCount.value != currentLineStationList.size - 1) {
                    setStationAndState(currentLineStationList.size - 1, StationStatus.ON_ARRIVE)
                    utils.haptic(binding.terminal)
                }

            }
        }

        // 上站：短按=上站，长按=跳起点站
        binding.lastStation.setupTapLongPress(
            onClick = {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setupTapLongPress
                }
                val before = _currentLineStationState.value
                lastStation()
                if (_currentLineStationState.value != before) utils.haptic(binding.lastStation)
            },
            onLongClick = {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setupTapLongPress
                }
                if (_currentLineStationCount.value != 0) {
                    setStationAndState(0, StationStatus.ON_NEXT)
                    utils.haptic(binding.lastStation)
                }
            }
        )

        // 下站：短按=下站，长按=跳终点站
        binding.nextStation.setupTapLongPress(
            onClick = {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setupTapLongPress
                }
                val before = _currentLineStationState.value
                nextStation()
                if (_currentLineStationState.value != before) utils.haptic(binding.nextStation)
            },
            onLongClick = {
                if (isOperationLock) {
                    utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
                    return@setupTapLongPress
                }
                if (_currentLineStationCount.value != currentLineStationList.size - 1) {
                    setStationAndState(currentLineStationList.size - 1, StationStatus.ON_ARRIVE)
                    utils.haptic(binding.nextStation)
                }
            }
        )

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

            utils.showMsg("开始模拟运行报站")
            // 关闭定位
            binding.locationBtnGroup.uncheck(binding.locationBtn.id)

            //关闭报站
            anPlayer.stopAnnounce()

            // 切换到起点站
            if (_currentLineStationCount.value != 0) {
                setStationAndState(0, StationStatus.ON_NEXT)
                utils.haptic(binding.startingStation)
            }

            runningSimRunning = true
            announce()

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

            val infoUpdateHandler = Handler(mainLooper)
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


        // 停止播报
        binding.stopAnnouncement.setOnClickListener {
            runningSimRunning = false
            anPlayer.stopAnnounce()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun View.setupTapLongPress(
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        // 用长按监听器处理长按
        setOnLongClickListener {
            onLongClick()
            true
        }

        // 短按通过触摸抬起且未触发长按来判断
        var longPressTriggered = false

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressTriggered = false
                    false
                }

                MotionEvent.ACTION_UP -> {
                    if (!longPressTriggered) {
                        onClick()
                    }
                    false
                }

                MotionEvent.ACTION_CANCEL -> false
                else -> false
            }
        }
    }

    private lateinit var anResolver: AnnouncementProvider
    private lateinit var anPlayer: AnnouncementPlayer

    @OptIn(UnstableApi::class)
    private fun initAnnouncement() {

        // anResolver
        anResolver = AnnouncementProvider.getInstance(requireContext())

        // anPlayer
        anPlayer = AnnouncementPlayer.getInstance(requireContext())

    }


    private lateinit var navigationService: NavigationService
    private fun initNavigation() {
        navigationService = NavigationService.getInstance(requireContext())
    }

    private lateinit var linePathAndStationPointService: LinePathAndStationPointService
    private fun initLinePathService() {
        linePathAndStationPointService =
            LinePathAndStationPointService.getInstance(requireContext())

    }

    private fun observeStationState(scope: CoroutineScope) {
        scope.launch {
            // 用 combine 同时监听两个 Flow，任一变化都触发
            combine(
                currentLineStationCount,
                currentLineStationState
            ) { count, state -> count to state }
                .distinctUntilChanged()
                .collect { (count, state) ->
                    linePathAndStationPointService.onStationIndexChange(count)
                    linePathAndStationPointService.onStationStateChange(state)
                }
        }
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


                for (i in path.polyline.indices) {


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
                    setLine(planLine)
                } else {
                    utils.showMsg(resources.getString(R.string.plan_line_too_short_tip))
                }


            }

        })
    }


    lateinit var mLocationChangedListener: LocationSource.OnLocationChangedListener


    /**
     * 初始化地图
     */
    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    private fun initMap(savedInstanceState: Bundle?) {
        aMapView = binding.map
        aMapView.onCreate(savedInstanceState)
        aMap = aMapView.map

        utils.setMapMode(aMap, utils.getMapType())

//        aMap.isMyLocationEnabled = false
        aMap.isTrafficEnabled = utils.getIsMapTrafficEnabled()
        aMap.setLocationSource(object : LocationSource {
            override fun activate(listener: LocationSource.OnLocationChangedListener) {
                mLocationChangedListener = listener
            }

            override fun deactivate() {
            }
        })

        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.location_marker))
//        myLocationStyle.anchor(0.5F, 0.57F)
//        myLocationStyle.showMyLocation(true)
//        myLocationStyle.interval(1000);
        aMap.myLocationStyle = myLocationStyle

        binding.mapContainer.setScrollView(binding.main)

        mapToCenter(false)

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


        locationMarker = aMap.addMarker(
            MarkerOptions().position(currentLngLat).setFlat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.location_marker))
        )
        locationMarker.setAnchor(0.5F, 0.57F)


        // 标点点击事件
        aMap.setOnMarkerClickListener { marker ->
            Log.d("MarkerClick", "${marker.`object`}")

            val station = (marker.`object` as Station)
            val stationIndex = currentLineStationList.indexOf(station)

            onClickStation(stationIndex)
            return@setOnMarkerClickListener true

        }

        //设置地图缩放比例
        aMap.animateCamera(CameraUpdateFactory.zoomTo(14F))

        //切换地图位置至初始位置
        mapToCenter(false)

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
                runningSimRunning = false
                anPlayer.stopAnnounce()
            }

            lastTouchMapTime = System.currentTimeMillis()

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
                utils.showStationDialog(requireActivity(), "new", latLng = it, isOrderLatLng = true)
            }

            // 单击地图，设置路线规划起点/终点
            if (utils.getIsLinePlanning()) {
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("")
                    .setNegativeButton("从这出发") { _, _ ->
                        planFrom = LatLng(it.latitude, it.longitude)
                        if (this::planFromMarker.isInitialized) planFromMarker.destroy()
                        planFromMarker = aMap.addMarker(
                            MarkerOptions().position(planFrom).title("起点")
                        )
                    }
                    .setPositiveButton("去这里") { _, _ ->
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
                runningSimRunning = false
                anPlayer.stopAnnounce()
            }
            lastTouchMapTime = System.currentTimeMillis()
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val windowManager = requireActivity().windowManager

                var lastRotateAngle: Float
                var curRotateAngle = 0f

                while (isActive) {
//                    Log.d(tag, "L2115")

//                    Log.d("L2017", "${currentLineStationCount} ${currentLineStation.cnName}")

                    withContext(Dispatchers.Main) {
                        if (binding.locationBtn.isChecked && binding.mapBtn.isChecked) {

                            @Suppress("DEPRECATION") val rotation =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
                                } else {
                                    @Suppress("DEPRECATION")
                                    windowManager.defaultDisplay.rotation
                                }
//                            Log.d(tag, "L2112 ${rotation}")


//                            var rotateAngle = (sensorHelper.getAzimuth().toFloat() + 360f) % 360f
                            var rotateAngle = currentBearing.toFloat()

//                            Log.d("L2108", "${rotateAngle}")
                            lastRotateAngle = curRotateAngle
                            curRotateAngle = rotateAngle

                            val rotateAngleAdd = when (rotation) {
                                Surface.ROTATION_0 -> 0f
                                Surface.ROTATION_90 -> 90f
                                Surface.ROTATION_180 -> 180f
                                Surface.ROTATION_270 -> 270f
                                else -> 0f
                            }

                            rotateAngle = (rotateAngle + rotateAngleAdd) % 360f

                            val updRotate = abs(lastRotateAngle - curRotateAngle) > 10f

                            if (this@MainFragment::locationMarker.isInitialized) {


                                val set = AnimationSet(false).apply {

                                    //更新蓝点位置
                                    addAnimation(TranslateAnimation(currentLngLat))

                                    //更新蓝点方位
                                    if (updRotate) {
                                        addAnimation(
                                            RotateAnimation(
                                                locationMarker.rotateAngle,
                                                -rotateAngle
                                            )
                                        )
                                    }

                                    setDuration(100L)
                                }
                                locationMarker.setAnimation(set)
                                locationMarker.startAnimation()
                            }

                            // 更新地图
                            if (System.currentTimeMillis() - lastTouchMapTime >= 10_000) {
                                val cameraPosition = CameraPosition.builder()
                                    .target(getMapFinalLngLat(currentLngLat))
//                                    .bearing(if (updRotate) rotateAngle else aMap.cameraPosition.bearing)
                                    .bearing(if (updRotate) rotateAngle else currentBearing.toFloat())
                                    .zoom(aMap.cameraPosition.zoom)
                                    .tilt(aMap.cameraPosition.tilt)
                                    .build()

                                aMap.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                                    100,
                                    null
                                )
                            }


                        }

                        // 每隔1s刷新地图Text
                        if (binding.mapBtn.isChecked) {
                            // 获取地图缩放级别
                            aMapLastZoom = aMapCurrentZoom
                            aMapCurrentZoom = aMap.cameraPosition!!.zoom
                            // 放大到一定级别
                            if (aMapLastZoom < aMapZoomPoint && aMapCurrentZoom >= aMapZoomPoint) {
                                // TODO: 刷新地图Text
//                                refreshMapStationText()
                            }
                            // 缩小到一定级别
                            else if (aMapLastZoom >= aMapZoomPoint && aMapCurrentZoom < aMapZoomPoint) {
                                // TODO: 刷新地图Text
//                                refreshMapStationText()
                            }
                        }

                        if (::aMap.isInitialized && aMap.isTrafficEnabled != utils.getIsMapTrafficEnabled()) {
                            aMap.isTrafficEnabled = utils.getIsMapTrafficEnabled()
                        }

                    }
                    delay(1000.milliseconds)
                }
            }
        }


//0

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
            NotificationChannel("0", "路线运行实况", NotificationManager.IMPORTANCE_HIGH)
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
        notificationBuilder =
            Notification.Builder(requireContext(), "0")
                .setSmallIcon(R.mipmap.an)
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
//        binding.lineStationList.setHasFixedSize(true)
        binding.lineStationList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapter = StationOfLineAdapter(
            requireContext(),
            ArrayList(),
            0
        )
        binding.lineStationList.adapter = adapter

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

//        adapter.isScroll = true

        //单击设为 区间线起点/终点 或 当前站点
        setOnStationItemClickListener()

        //切换为 上一次运行的路线 或 默认路线
        val sharedPreferences =
            requireContext().getSharedPreferences("lastRunningInfo", MODE_PRIVATE)

        val lastRunningLineName = sharedPreferences.getString("lineName", "") ?: ""


        val lastLineDirection =
            sharedPreferences.getInt("lastLineDirection", LineDirection.ON_UP)
//        Log.d("L2568", "${lastLineDirection}")
        currentLineDirection = lastLineDirection

        if (currentLineDirection == LineDirection.ON_UP) {
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
        }

        if (currentLineDirection == LineDirection.ON_DOWN) {
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnDown.id)
        }


        // 全站路线
        if (lastRunningLineName == resources.getString(R.string.line_all)) {
            currentLineDirection = LineDirection.ON_UP
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
            loadLineAll(true)
            return
        }


        // 云端路线
        val onlineLineUpId = sharedPreferences.getString("onlineLineUpId", "") ?: ""
        val onlineLineDownId = sharedPreferences.getString("onlineLineDownId", "") ?: ""
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
                    busLineSearch.setOnBusLineSearchListener { res, _ ->
//                utils.showMsg("setOnBusLineSearchListener${res.busLines.size}")
                        if (res.busLines.isNotEmpty()) {
//                            Log.d(tag, res.query.queryString)
                            // 上行
                            if (i == 0) {
                                val line = getOnlineLine(res, 0, 0)
                                onlineLine.upLineStation = line.upLineStation
                                onlineLine.name = line.name
                            }
                            // 下行（如果有）
                            else {
                                // 下行
                                if (onlineLineUpId != onlineLineDownId) {
                                    onlineLine.downLineStation =
                                        getOnlineLine(res, 0, 0).upLineStation
                                }
                                // 单向路线
                                else {
                                    onlineLine.downLineStation = onlineLine.upLineStation
                                }

                            }

                            requireActivity().runOnUiThread {
                                if (!hasLoad && onlineLine.upLineStation != "" && onlineLine.downLineStation != "") {
                                    setLine(onlineLine, false)
                                    utils.haptic(binding.root)
                                    hasLoad = true
                                }
                            }
                        }
                    }
                    busLineSearch.searchBusLineAsyn()
                }
            }
            return
        }


        // 本地路线
        val localLineList = lineDatabaseHelper.queryByName(lastRunningLineName).toMutableList()
        if (localLineList.isNotEmpty()) {
            setLine(localLineList.first(), false)
            utils.haptic(binding.root)
        }

    }

    var currentCityName = ""
    fun onMyLocationChange(location: AMapLocation) {

//        utils.showMsg(location.city)
//        Log.d("L2546", "")

        currentCityName = location.city

        val latStr = String.format(Locale.CHINA, "%.8f", location.latitude)
        val longStr = String.format(Locale.CHINA, "%.8f", location.longitude)

        val locationInfoStr = "[${
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        }] $latStr $longStr"

        locationInfoList.add(0, locationInfoStr)
//        appendLocationToFile(requireContext(), locationInfoStr)


        binding.locationBtnGroup.check(binding.locationBtn.id)

        //更新时间
        lastTimeMillis = currentTimeMillis
        currentTimeMillis = System.currentTimeMillis()

        //更新位置
        lastLngLat = currentLngLat
        currentLngLat = LatLng(location.latitude, location.longitude)

        updCurrentDistance()

        //更新速度
        val distance = utils.calculateDistance(
            currentLngLat.longitude,
            currentLngLat.latitude,
            lastLngLat.longitude,
            lastLngLat.latitude
        )

        currentSpeedKmH = if (currentSpeedKmH < 0) 0.0
        else (distance / 1000.0) / ((currentTimeMillis - lastTimeMillis) / 1000.0 / 60.0 / 60.0)

        if (mockLocation) {
            currentSpeedKmH /= 10
        }

        binding.speedValue.text =
            String.format(Locale.CHINA, "%.1f", currentSpeedKmH)

        binding.navStationSpeedValue.text =
            String.format(Locale.CHINA, "%.0f", currentSpeedKmH)

        // 计算方位角[0-360)
        if (currentSpeedKmH >= 5.0) {
            currentBearing = utils.calculateBearing(
                lastLngLat.latitude, lastLngLat.longitude,
                currentLngLat.latitude, currentLngLat.longitude
            )
            Log.d("L2662", "${currentBearing}")
        }


        // 计算当前定位距离所有路线站点的距离
        for (i in currentLineStationList.indices) {
            lastDistanceToStationList[i] = currentDistanceToStationList[i]
            currentDistanceToStationList[i] = utils.calculateDistance(
                currentLngLat.longitude,
                currentLngLat.latitude,
                currentLineStationList[i].longitude,
                currentLineStationList[i].latitude
            )
        }

        if (::mLocationChangedListener.isInitialized) {
            mLocationChangedListener.onLocationChanged(location)
        }

        findMatchStation()

    }

    fun updCurrentDistance() {
        lastDistanceToCurrentStation = currentDistanceToCurrentStation
        currentDistanceToCurrentStation = utils.calculateDistance(
            currentLngLat.longitude,
            currentLngLat.latitude,
            currentLineStation.longitude,
            currentLineStation.latitude
        )

        val distance = currentDistanceToCurrentStation

        when {
//            currentLine.name == "" -> {
//                binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
//                binding.currentDistanceToCurrentStationValue.text = getString(R.string.main_distance_value)
//            }

            distance >= 1000 -> {
                val km = distance / 1000
                binding.currentDistanceToCurrentStationUnit.text = getString(R.string.km)
                binding.currentDistanceToCurrentStationValue.text = String.format(
                    Locale.CHINA,
                    when {
                        distance >= 100000 -> "%.1f"
                        distance >= 10000 -> "%.2f"
                        else -> "%.3f"
                    },
                    km
                )
                binding.navStationDistanceValue.text = String.format(Locale.CHINA, "%.0f", km)
            }

            else -> {
                binding.currentDistanceToCurrentStationUnit.text = getString(R.string.m)
                binding.currentDistanceToCurrentStationValue.text =
                    String.format(Locale.CHINA, "%.1f", distance)
                binding.navStationDistanceValue.text =
                    String.format(Locale.CHINA, "%.0f", distance)
            }
        }

        binding.navStationDistanceUnit.text =
            binding.currentDistanceToCurrentStationUnit.text.toString()
                .replace("(", "")
                .replace(")", "")
    }

    /**
     * 遍历站点列表，检查是否符合进站、出站、即将到站条件，并切换站点然后报站
     * @return 当前站点是否更改
     */
    private fun findMatchStation() {

        matchCount = (matchCount + 1) % Int.MAX_VALUE
        if (matchCount < 2) return

        val willInStationDistance =
            utils.getStationRangeByLineType(currentLine.type, "WillIn")     // 即将进站临界距离
        val inStationDistance =
            utils.getStationRangeByLineType(currentLine.type, "In")    // 进站临界距离
        val outStationDistance =
            utils.getStationRangeByLineType(currentLine.type, "Out")         // 出站临界距离


        val lineStationList = currentLineStationList
        val lastDistanceToStationList = lastDistanceToStationList
        val currentDistanceToStationList = currentDistanceToStationList

        val rangeAfter = _currentLineStationCount.value until lineStationList.size
        val rangeBefore = _currentLineStationCount.value - 1 downTo 0
        for (i in rangeAfter + rangeBefore) {
//            Log.d(tag, "find station $i")
            //进站条件：上次定位在这个站点外，现在定位在这个站点内
            if (lastDistanceToStationList[i] > inStationDistance &&
                currentDistanceToStationList[i] <= inStationDistance &&
                utils.getAutoSwitchStationState("In")
            ) {


                //当前站点及状态相同，直接返回
                if ((lineStationList[i].id == currentLineStation.id && _currentLineStationState.value == StationStatus.ON_ARRIVE)) {
                    return
                }

                // 判断方位角
                if (!isStationBearingOK(lineStationList[i])) {
                    return
                }

                Log.d(
                    tag,
                    "自动到达站：${lineStationList[i].cnName} for ${currentDistanceToStationList[i]} <= $inStationDistance"
                )

                setStationAndState(i, StationStatus.ON_ARRIVE)

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
                return
            }
            //即将进站条件：现在位于即将进站范围内，且现在不位于进站进站内
            else if (currentDistanceToStationList[i] <= willInStationDistance &&
                currentDistanceToStationList[i] > inStationDistance &&
                utils.getAutoSwitchStationState("WillIn")
            ) {

                //当前站点及状态相同，直接返回
                if (((_currentLineStationState.value == StationStatus.ON_WILL_ARRIVE || _currentLineStationState.value == StationStatus.ON_ARRIVE))
                    && lineStationList[i].id == currentLineStation.id
                ) {
                    return
                }

                // 判断方位角
                if (!isStationBearingOK(lineStationList[i])) {
                    return
                }

                Log.d(
                    tag,
                    "即将自动进站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )


                setStationAndState(i, StationStatus.ON_WILL_ARRIVE)

                announce()
                utils.longHaptic()
                return
            }
            //出站条件：上次位于某站点内，现在位于这个站点外（出站临界距离）
            else if (lastDistanceToStationList[i] < outStationDistance &&
                currentDistanceToStationList[i] > outStationDistance &&
                currentLine.name != resources.getString(
                    R.string.line_all
                ) &&
                utils.getAutoSwitchStationState("Out")
            ) {


                // 判断方位角
                if (!isStationBearingOK(lineStationList[i])) {
                    return
                }

                Log.d(
                    tag,
                    "${lineStationList[i].cnName} 自动出站：${lineStationList[i].cnName} for ${lastDistanceToStationList[i]} to ${currentDistanceToStationList[i]}"
                )


                // 上行终点站出站
                if (currentLineDirection == LineDirection.ON_UP && i >= lineStationList.size - 1 && utils.getSwitchDirectionWhenOutFromTerminalWithOnUp()) {
                    if (!currentLine.isRingRoute) {
                        reverseLineDirection()
                    }
                    setStationAndState(1, StationStatus.ON_NEXT)
                    announce()
                    utils.longHaptic()
                    return
                }

                if (i < lineStationList.size - 1) {
                    setStationAndState(i + 1, StationStatus.ON_NEXT)
                    announce()
                    utils.longHaptic()
                }

                return
            }
        }
    }


    /**
     * 刷新路线标记和轨迹
     */
    private fun refreshMarkerAndTrack() {

        // TODO 由[LinePathAndStationPointService]W
    }

    /**
     * 上一站
     */
    private fun lastStation(): Boolean {
        if (currentLineStation.id == null) return false

        when (_currentLineStationState.value) {

            StationStatus.ON_NEXT -> {
                if (_currentLineStationCount.value <= 0) return false
                _currentLineStationCount.value--
                currentLineStation = currentLineStationList[_currentLineStationCount.value]
                _currentLineStationState.value = StationStatus.ON_ARRIVE
                refreshUIAndPreloadingStationVoice()
                return true
            }

            StationStatus.ON_WILL_ARRIVE -> {
                _currentLineStationState.value = StationStatus.ON_NEXT
                refreshUIAndPreloadingStationVoice()
                return true
            }

            StationStatus.ON_ARRIVE -> {
                _currentLineStationState.value = StationStatus.ON_WILL_ARRIVE
                refreshUIAndPreloadingStationVoice()
                return true
            }

            else -> {
                return false
            }
        }
    }

    /**
     * 下一站
     */
    private fun nextStation(): Boolean {
        if (currentLineStation.id == null) return false

        when (_currentLineStationState.value) {

            StationStatus.ON_NEXT -> {
//                currentLineStationState = StationStatus.ON_WILL_ARRIVE
                _currentLineStationState.value = StationStatus.ON_ARRIVE
                refreshUIAndPreloadingStationVoice()
                return true
            }

            StationStatus.ON_WILL_ARRIVE -> {
                _currentLineStationState.value = StationStatus.ON_ARRIVE
                refreshUIAndPreloadingStationVoice()
                return true
            }

            StationStatus.ON_ARRIVE -> {
                if (_currentLineStationCount.value >= currentLineStationList.size - 1) return false
                _currentLineStationCount.value++
                currentLineStation = currentLineStationList[_currentLineStationCount.value]
                _currentLineStationState.value = StationStatus.ON_NEXT
                refreshUIAndPreloadingStationVoice()
                return true
            }

            else -> {
                return false
            }
        }

    }

    /**
     * 下一站
     */
    private fun hasNextStation(): Boolean {
        if (currentLineStation.id == null) return false

        when (_currentLineStationState.value) {
            StationStatus.ON_WILL_ARRIVE -> {
                return true
            }

            StationStatus.ON_NEXT -> {
                return true
            }

            StationStatus.ON_ARRIVE -> {
                return _currentLineStationCount.value < currentLineStationList.size - 1
            }

            else -> {
                return false
            }
        }

    }

    /**
     * 切换站点及站点状态
     * @param stationCount 要切换到的站点在路线中的序号
     * @param stationState 要切换的站点状态
     */
    private fun setStationAndState(stationCount: Int, stationState: Int) {

        if (stationCount < 0 || stationCount >= currentLineStationList.size) return

        _currentLineStationState.value = stationState

        currentLineStation = currentLineStationList[stationCount]
        _currentLineStationCount.value = stationCount

        refreshUIAndPreloadingStationVoice()

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
                    _currentLineStationCount.value = i
                    currentLineStation = currentLineStationList[i]
                }
            }
        } else {
            _currentLineStationCount.value = 0
            currentLineStation = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
        }


    }

    /**
     * 更新路线站点显示、小卡片和通知
     */
    private fun refreshLineStationListAndNotice() {

        val currentStationStateText = when (_currentLineStationState.value) {
            StationStatus.ON_NEXT -> requireContext().resources.getString(R.string.next)
            StationStatus.ON_WILL_ARRIVE -> requireContext().resources.getString(R.string.will_arrive)
            StationStatus.ON_ARRIVE -> requireContext().resources.getString(R.string.arrive)
            else -> ""
        }

        binding.currentStationState.text = currentStationStateText
        val stationName = if (utils.getUILang() == "zh")
            currentLineStation.cnName
        else
            currentLineStation.enName
        binding.currentStationName.text = stationName

        binding.navStationName.nextText(stationName)
        binding.navStationName.requestLayout()

        binding.navStationSign.text = when (_currentLineStationState.value) {
            StationStatus.ON_NEXT -> "→"
            StationStatus.ON_WILL_ARRIVE -> "↘"
            StationStatus.ON_ARRIVE -> "↓"
            else -> ""
        }

        //路线卡片滚动到当前站点
        binding.lineStationList.post {

            try {
                val manager = binding.lineStationList.layoutManager as LinearLayoutManager
                val adapter = binding.lineStationList.adapter as StationOfLineAdapter

                val lastLineStationCount = adapter.stationCount
                adapter.stationCount = _currentLineStationCount.value
                adapter.stationState = _currentLineStationState.value

                val positionStart =
                    lastLineStationCount.coerceAtMost(_currentLineStationCount.value)
                val itemCount = abs(lastLineStationCount - _currentLineStationCount.value) + 1

                adapter.notifyItemRangeChanged(positionStart, itemCount, false)

                @SuppressLint("InflateParams")
                val layout = LayoutInflater.from(requireContext())
                    .inflate(
                        R.layout.item_station_of_line, null, false
                    )
                val stationIndexView = layout.findViewById<TextView>(R.id.station_index)
                val lineHeight = stationIndexView.lineHeight

                manager.scrollToPositionWithOffset(
                    _currentLineStationCount.value,
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
        // TODO 实时动态开关
        if (utils.getNotice() && true) {

            initNotification()

            if (!permissionManager.hasNoticePermission()) {
                permissionManager.requestNoticePermission()
                utils.showMsg("要接收运行信息，请授予通知权限")
                utils.showMsg("不希望接收运行信息，请前往“设置”-“系统与电显”关闭")
            }


            // title
            var title = currentLine.name
            if (currentLine.name != getString(R.string.line_all) && currentLineStationList.isNotEmpty()) {
                title += if (utils.getUILang() == "zh")
                    " ${
                        currentLineStationList.first().cnName
                    } - ${currentLineStationList.last().cnName}"
                else
                    " To ${currentLineStationList.last().enName}"
            }
            notificationBuilder.setContentTitle(title)

            // text
            var text = "${binding.currentStationState.text} "
            text += if (utils.getUILang() == "zh")
                currentLineStation.cnName
            else
                currentLineStation.enName
            notificationBuilder.setContentText(text)

            notificationBuilder.setWhen(System.currentTimeMillis())

            notificationBuilder.setSmallIcon(R.mipmap.an_full_round)
//            notificationBuilder.setLargeIcon(
//                BitmapFactory.decodeResource(
//                    resources,
//                    R.mipmap.an_round
//                )
//            )

            // Android 16.1+
            if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {


                val progressStyle = Notification.ProgressStyle().apply {
                    setStyledByProgress(false)
                    setProgress(currentLineStationList.size) // 设置总进度
                    // 添加分段 (Segment)
                    setProgressSegments(
                        listOf(
                            Notification.ProgressStyle.Segment(_currentLineStationCount.value)
                                .setColor("#B6B6B6".toColorInt()),  // 已过
                            Notification.ProgressStyle
                                .Segment(currentLineStationList.size - _currentLineStationCount.value - 1)
                                .setColor("#37B267".toColorInt())     // 前方站
                        )
                    )

                    // 设置进度
                    val originalIcon =
                        Icon.createWithResource(requireContext(), R.drawable.to_right)

                    setProgressTrackerIcon(
                        when (_currentLineStationState.value) {
                            StationStatus.ON_NEXT -> utils.rotateIcon(
                                requireContext(),
                                originalIcon,
                                0f
                            )

                            StationStatus.ON_WILL_ARRIVE -> utils.rotateIcon(
                                requireContext(),
                                originalIcon,
                                0f
                            )

                            StationStatus.ON_ARRIVE -> utils.rotateIcon(
                                requireContext(),
                                originalIcon,
                                90f
                            )

                            else -> utils.rotateIcon(requireContext(), originalIcon, 0f)
                        }
                    )
                    setProgress(_currentLineStationCount.value)
                }

                var shortCriticalText = ""
                if (currentLineStationList.isNotEmpty()) {
                    shortCriticalText = currentLineStationList.last().cnName
                }

                notificationBuilder
                    .setRequestPromotedOngoing(true)
                    .setSmallIcon(utils.createTextIcon(currentLine.name, 96))
                    .setShortCriticalText(shortCriticalText)
                    .setStyle(progressStyle)

            }

            notificationManager.notify(0, notificationBuilder.build())


        }

    }

    /**
     * 更新路线站点更新信息
     */
    private fun refreshLineStationChangeInfo() {

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
                _currentLineStationState.value
            )
        )

    }


    var filePathList = mutableListOf<AnMediaItem>()

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

        audioStreamScope = lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val announcementContext = AnnouncementContext(
                    binding.lineName!!,
                    currentLineStationList,
                    _currentLineStationCount.value,
                    _currentLineStationState.value,
                    currentSpeedKmH,
                    announcementLangList
                )

                filePathList = anResolver.resolveAndBuild(format, announcementContext)

                Log.d("L60", "announce: $filePathList")
                anPlayer.play(filePathList)
            }
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




    fun pauseAnnounce() {

        binding.stopAnnouncement.visibility = GONE

        simRunningHandler.removeCallbacksAndMessages(null)

        if (::audioStreamScope.isInitialized)
            audioStreamScope.cancel()
    }

    fun loadLineAll(acceptStationTypeEnableSetting: Boolean = false) {

        utils.showMsg("全站路线加载中")

        val stationList = if (acceptStationTypeEnableSetting)
            stationDatabaseHelper.queryByTypes(
                booleanArrayOf(
                    utils.getLineAllStationTypeEnable("C"),
                    utils.getLineAllStationTypeEnable("B"),
                    utils.getLineAllStationTypeEnable("U"),
                    utils.getLineAllStationTypeEnable("T")
                )
            )
        else
            stationDatabaseHelper.queryAll()

        if (stationList.size < 2) {
            utils.showMsg(getString(R.string.station_not_enough_tip))
            return
        }

        val allStationLine =
            Line(
                name = resources.getString(R.string.line_all),
                isUpAndDownInvert = false
            )
        val allStationLineStationList = ArrayList<Station>()

        for (station in stationList) {
            allStationLineStationList.add(station)
            allStationLine.upLineStation += "${station.id} "
        }

        val length = allStationLine.upLineStation.length
        allStationLine.upLineStation =
            allStationLine.upLineStation.substring(
                0,
                length - 1
            )
        allStationLine.downLineStation =
            allStationLine.upLineStation

        setLine(allStationLine)

    }

    fun refreshUIAndPreloadingStationVoice() {

        //更新路线站点显示、小卡片和通知
        refreshLineStationListAndNotice()

        //更新路线站点更新信息和系统通知
        refreshLineStationChangeInfo()

        //刷新地图标点和轨迹
        refreshMarkerAndTrack()

        //更新当前站点距离
        updCurrentDistance()

    }

    fun getOnlineLine(
        res: BusLineResult, beginIndex: Int, endIndex: Int
    ): Line {
        var upLineStationStr = ""
        var downLineStationStr = ""

        for (x in beginIndex..endIndex) {
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
                    beginIndex ->
                        upLineStationStr += "$id "

                    endIndex ->
                        downLineStationStr += "$id "
                }
            }
        }
//        utils.showMsg(upLineStationStr)

        val line = Line(
            -1,
            res.busLines[beginIndex].busLineName.substringBefore("(").split("路", "号", "线")
                .first(),
            upLineStationStr,
            downLineStationStr,
            false
        )

        // 单向路线
        if (beginIndex == endIndex) {
            line.downLineStation = line.upLineStation
        }

        return line
    }

    fun reverseLineDirection() {
        val checkedId = if (binding.lineDirectionBtnUp.isChecked)
            binding.lineDirectionBtnDown.id
        else
            binding.lineDirectionBtnUp.id

        binding.lineDirectionBtnGroup.check(checkedId)
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
                            val format = intent.getStringExtra("format")

                            if (currentLine.name == "") {
                                utils.showMsg("还没有选择路线，请前往“主控”选择")
                                return
                            }

                            if (format == null || format == "") {
                                return
                            }

                            announce(format = format)
                        }

                        utils.switchLineActionName -> {
                            val sharedPreferences =
                                requireContext().getSharedPreferences(
                                    "lastRunningInfo",
                                    MODE_PRIVATE
                                )
                            sharedPreferences.edit {
                                remove("onlineLineUpId")
                                remove("onlineLineDownId")
                            }
                            switchLine(id = intent.getIntExtra("id", -1))
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

                        utils.setLoudnessBoostAmountName -> {
                            anPlayer.setTargetGain()
                        }

                        utils.backHomeName -> {
                            tabSwitchListener?.switchToTab(TabPage.MAIN)
                        }

                        utils.pauseAnnounceName -> {
                            anPlayer.stopAnnounce()
                        }

                        // 播放准备就绪
                        utils.ON_PLAYER_STATE_READY -> {
                            binding.stopAnnouncement.visibility = VISIBLE
                        }

                        // 播放完成
                        utils.ON_PLAYER_STATE_ENDED -> {
                            if (runningSimRunning) {
                                if (hasNextStation()) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay((utils.getAutoAnInterval() * 1000L).milliseconds)
                                        withContext(Dispatchers.Main) {
                                            if (nextStation()) {
                                                announce()
                                            } else {
                                                stopSimRunning()
                                            }
                                        }
                                    }
                                } else {
                                    stopSimRunning()
                                }
                            }

                            if (!runningSimRunning) {
                                pauseAnnounce()
                            }
                        }

                        utils.ON_PLAYER_STOP -> {
                            pauseAnnounce()
                        }

                        utils.preloadingCommonTextName -> {
                            preloadCommonAndNumberAndLetterText()
                        }

                        utils.preloadingStationName -> {
                            preloadingStation()
                        }

                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            listOf(
                utils.tryListeningAnActionName,
                utils.switchLineActionName,
                utils.editLineOnMapActionName,
                utils.requestCityFromLocationActionName,
                utils.openLocationActionName,
                utils.setLoudnessBoostAmountName,
                utils.backHomeName,
                utils.pauseAnnounceName,
                utils.ON_PLAYER_STATE_READY,
                utils.ON_PLAYER_STATE_ENDED,
                utils.ON_PLAYER_STOP,
                utils.preloadingCommonTextName,
                utils.preloadingStationName,
            ).forEach { addAction(it) }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, intentFilter)

    }


    fun switchLine(id: Int) {

        if (!isAdded)
            return

        tabSwitchListener?.switchToTab(TabPage.MAIN)

        val line = lineDatabaseHelper.queryById(id).first()
        setLine(line)

        utils.showMsg("正在运行路线：${line.name}", true)

    }


    fun getCityFromLocation(): String {
        return currentCityName
    }

    fun stopSimRunning() {
        runningSimRunning = false
        simRunningHandler.removeCallbacksAndMessages(null)
        utils.showMsg("模拟运行报站结束")
    }


    // 设置区间线
    fun setShuttleLine(
        lineList: List<String>,
        currentLineStartingIndex: Int,
        currentLineTerminalIndex: Int
    ) {
        val newLine = Line(
            id = originLine.id,
            name = originLine.name,
            isUpAndDownInvert = originLine.isUpAndDownInvert,
            upLineStation = originLine.upLineStation,
            downLineStation = originLine.downLineStation
        )

        val stationRange = lineList.slice(currentLineStartingIndex..currentLineTerminalIndex)
        val stationStr = stationRange.joinToString(" ")

        when (currentLineDirection) {
            LineDirection.ON_UP -> {
                newLine.upLineStation = stationStr
            }

            LineDirection.ON_DOWN -> {
                newLine.downLineStation = stationStr
            }
        }

        loadLine(newLine)

        utils.haptic(binding.root)
    }

    fun switchPowerSavingMode(enable: Boolean) {
        enableLowPowerMode = enable

        if (enableLowPowerMode) {
            binding.mapBtnGroup.uncheck(binding.mapBtn.id)
            binding.lineStationCard.visibility = GONE
            (binding.lineStationList.adapter as StationOfLineAdapter).isShown = false
//                enableEs = false

//                binding.headerLeftNew.stopAnimation()
//                binding.headerRightNew.stopAnimation()
            binding.navStationName.stopAnimation()

//                binding.headerLeftNew.visibility = GONE
//                binding.headerRightNew.visibility = GONE
            binding.navCard.visibility = GONE
        }

        if (!enableLowPowerMode) {
            binding.mapBtnGroup.check(binding.mapBtn.id)
            binding.lineStationCard.visibility = VISIBLE
            (binding.lineStationList.adapter as StationOfLineAdapter).isShown = true
//                enableEs = true

//                binding.headerLeftNew.startAnimation()
//                binding.headerRightNew.startAnimation()
            binding.navStationName.startAnimation()

//                if (utils.getIsOpenLeftEs()) {
//                    binding.headerLeftNew.visibility = VISIBLE
//                }
//                binding.headerRightNew.visibility = VISIBLE
            if (utils.getIsNavMode()) {
                binding.navCard.visibility = VISIBLE
            }
        }

        prefs.edit {
            putBoolean("enableLowPowerMode", enableLowPowerMode)
        }

        val actionName = if (enableLowPowerMode)
            "进入"
        else
            "退出"
        utils.showMsg("已${actionName}省电模式", true)

    }

    fun setLine(line: Line, lineDirectionToUp: Boolean = true) {
        originLine = line
        initLineInterval()
        _currentLineStationState.value = StationStatus.ON_NEXT
        if (lineDirectionToUp) {
            binding.lineDirectionBtnGroup.check(binding.lineDirectionBtnUp.id)
        }
        loadLine(line)
    }


    fun setOnStationItemClickListener() {
        val adapter = binding.lineStationList.adapter as StationOfLineAdapter
        adapter.setOnItemClickListener(object : StationOfLineAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                onClickStation(position)
            }
        })
    }

    fun onClickStation(position: Int) {
        if (isOperationLock) {
            utils.showMsg(resources.getString(R.string.operation_lock_on_tip))
            return
        }

        val chosenStationCnName = currentLineStationList[position].cnName
        val chosenStationEnName = currentLineStationList[position].enName

        var currentLineStartingIndex = when (currentLineDirection) {
            LineDirection.ON_UP -> currentUpLineStartingIndex
            LineDirection.ON_DOWN -> currentDownLineStartingIndex
            else -> 0
        }

        var currentLineTerminalIndex = when (currentLineDirection) {
            LineDirection.ON_UP -> currentUpLineTerminalIndex
            LineDirection.ON_DOWN -> currentDownLineTerminalIndex
            else -> 0
        }

        // 上/下行线路
        val lineList = when (currentLineDirection) {
            LineDirection.ON_UP -> currentLine.upLineStation.split(" ")
            LineDirection.ON_DOWN -> currentLine.downLineStation.split(" ")
            else -> emptyList()
        }

        val startingId = currentLineStationList[position].id

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
            .setTitle(
                "${
                    String.format(
                        Locale.CHINA,
                        "%02d",
                        position + 1
                    )
                } $chosenStationCnName"
            )
            .setMessage(chosenStationEnName)
            .setPositiveButton(
                "设为区间起点",
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {

                        currentLineStartingIndex =
                            lineList.indexOf(startingId.toString())

                        if (currentLineStartingIndex == -1) currentLineStartingIndex = 0

                        if (currentLineTerminalIndex < currentLineStartingIndex || currentLineTerminalIndex == Int.MAX_VALUE) {
                            currentLineTerminalIndex = lineList.size - 1
                        }

                        if (currentLineTerminalIndex - currentLineStartingIndex < 1) {
                            utils.showMsg("不能将原终点站设置为起点站")
                            return
                        }

                        setShuttleLine(
                            lineList,
                            currentLineStartingIndex,
                            currentLineTerminalIndex
                        )

                    }
                })
            .setNegativeButton(
                "设为区间终点",
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {

                        currentLineTerminalIndex =
                            lineList.indexOf(startingId.toString())

                        if (currentLineTerminalIndex == -1) currentLineTerminalIndex =
                            lineList.size - 1

                        if (currentLineTerminalIndex < currentLineStartingIndex || currentLineStartingIndex == -1) {
                            currentLineStartingIndex = 0
                        }

                        if (currentLineTerminalIndex - currentLineStartingIndex < 1) {
                            utils.showMsg("不能将原起点站设置终点站")
                            return
                        }

                        setShuttleLine(
                            lineList,
                            currentLineStartingIndex,
                            currentLineTerminalIndex
                        )

                    }
                })
            .setNeutralButton("设为当前站点") { _, _ ->
                setStationAndState(position, _currentLineStationState.value)
                refreshLineStationListAndNotice()
                utils.haptic(binding.lineStationList)
            }.show()
    }

    fun mapToCenter(enableAnimate: Boolean) {
        if (enableAnimate) {
            aMap.stopAnimation()
        }

        val finalLngLat = getMapFinalLngLat(currentLngLat)

        if (enableAnimate)
            aMap.animateCamera(CameraUpdateFactory.changeLatLng(finalLngLat))
        else
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(finalLngLat))
    }


    fun getMapFinalLngLat(currentLngLat: LatLng): LatLng {
        var finalLngLat = currentLngLat
        // 横屏
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val density = resources.displayMetrics.density
            val offsetPx = (floatingViewWidthDpWhenLand / 2 * density).toInt()
            val projection = aMap.projection
            val baseScreen = projection.toScreenLocation(currentLngLat)
//            Log.d("L4886", "${offsetPx}")
            baseScreen.x += offsetPx
            finalLngLat = projection.fromScreenLocation(baseScreen)
        }
        return finalLngLat
    }

    fun updFloatingViewWidth() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val width = if (isLandscape) {
            (floatingViewWidthDpWhenLand * resources.displayMetrics.density).toInt()
        } else {
            ViewGroup.LayoutParams.MATCH_PARENT
        }

        listOf(binding.floatingView, binding.floatingViewBottom).forEach { view ->
            view.layoutParams = view.layoutParams.apply { this.width = width }
            utils.applyOrientation(resources.configuration.orientation, view)
        }
    }

    fun isStationBearingOK(currentLineStation: Station): Boolean {

        // bearing < 0表示未设置方位角，始终判断为有效
        if (currentLineStation.bearing < 0) return true

        // 已设置方位角，判断夹角22.5度内即为有效
        val angle = (utils.angleBetween(
            currentBearing,
            currentLineStation.bearing
        ))
        Log.d(
            "L4643",
            "match with ${currentLineStation.cnName}, ${currentLineStation.bearing} to ${currentBearing}, angle: ${angle}"
        )

        return angle <= 22.5
    }

    fun <T> List<T>.forEachByDistance(i: Int, action: (index: Int, value: T) -> Unit) {
        if (isEmpty()) return
        require(i in indices) { "index $i out of bounds for size $size" }

        action(i, this[i])
        var distance = 1
        while (true) {
            var hasItem = false
            val left = i - distance
            val right = i + distance

            if (left >= 0) {
                action(left, this[left])
                hasItem = true
            }
            if (right < size) {
                action(right, this[right])
                hasItem = true
            }
            if (!hasItem) break
            distance++
        }
    }

    fun preloadCommonAndNumberAndLetterText() {
        // network 预加载 common 和 number 语句
        val stationStateList = listOf("Next", "WillArrive", "Arrive")
        val stationTypeList = listOf("Default", "Second", "Starting", "Terminal")

        val commonTextSet = mutableSetOf<String>()

        stationStateList.forEach { stationState ->
            stationTypeList.forEach { stationType ->
                val format = utils.getAnnouncementFormat(stationState, stationType)
                val anList = utils.getAnnouncements(format)
                val commonTextList = anList
                    .filter { !(it.first() == '<' && it.last() == '>') }
                    .map { "/common/${it}" }
                commonTextSet.addAll(commonTextList)
            }
        }

        val numberTextList = utils.chineseNumberMap.values.map { "/cn/number/${it}" }
        val letterTextList = ('A'..'Z').map { "/en/letter/${it}" }

        val totalTextList = (commonTextSet + numberTextList + letterTextList).distinct()

        utils.showMsg("正在预加载……")
        Log.d("L4177", "preloadingCommonText: $totalTextList")
        totalTextList.forEach { item ->
            anResolver.preloadingNetworkMedia(item)
        }
    }

    fun preloadingStation() {
        // network 预加载站点名称

        val stationTextSet = mutableSetOf<String>()

        currentLineStationList.forEachByDistance(_currentLineStationCount.value) { _, station ->
            stationTextSet.add("/station/cn/${station.cnName}")
            stationTextSet.add("/station/en/${station.enName}")
        }


        utils.showMsg("正在预加载……")
        val allStationTextList = stationTextSet.toList()
        allStationTextList.forEach { item ->
            anResolver.preloadingNetworkMedia(item)
        }
    }

}