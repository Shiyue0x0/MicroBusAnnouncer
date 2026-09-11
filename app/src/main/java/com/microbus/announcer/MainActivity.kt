package com.microbus.announcer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.microbus.announcer.model.TabPage
import com.microbus.announcer.ui.LineFragment
import com.microbus.announcer.ui.LineSwitcherActivity
import com.microbus.announcer.ui.MainFragment
import com.microbus.announcer.ui.SettingFragment
import com.microbus.announcer.ui.StationFragment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : BaseActivity(), TabSwitchListener {

    var tag: String = javaClass.simpleName
    private lateinit var utils: Utils
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var backPressedTime: Long = 0

    // ViewPager2 和 Adapter 引用
    private lateinit var mainViewPager: ViewPager2
    private lateinit var sideViewPager: ViewPager2

    // 用于在Compose中接收新Intent的StateFlow
    private val _newIntentFlow = MutableSharedFlow<Intent>()

    // 改用 MutableStateFlow，每次发射新值都会触发
    private val _switchTabFlow = MutableStateFlow<Pair<Int, Boolean>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utils = Utils(this)
        utils.loadAnnouncementFormatFromConfig()

        // 使用更现代的窗口设置
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 设置状态栏为全屏布局
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        utils.setUILang(utils.getUILang())

        powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, tag)
        wakeLock.acquire(60 * 60 * 1000L)

        setContent {
            MainScreen()
        }
    }

    @Composable
    @Preview
    fun MainScreen() {

        val view = LocalView.current

        // 当前选中的Tab位置
        val (currentTabPosition, setCurrentTabPosition) = remember { mutableIntStateOf(0) }

        //  检测屏幕方向
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // 使用 collectAsState 替代 collectAsStateWithLifecycle
        val switchRequest by _switchTabFlow.collectAsState()

        LaunchedEffect(switchRequest) {
            switchRequest?.let { (position, smoothScroll) ->
                if (::mainViewPager.isInitialized) {
                    Log.d(tag, "Switching to tab: $position, smooth: $smoothScroll")
                    mainViewPager.setCurrentItem(position, smoothScroll)
                    // 🔥 清除请求，允许再次触发相同的切换
                    _switchTabFlow.value = null
                }
            }
        }

        // 观察新Intent
        val newIntent by _newIntentFlow.collectAsStateWithLifecycle(initialValue = null)
        LaunchedEffect(newIntent) {
            newIntent?.let { intent ->
                if (intent.getBooleanExtra("switchToMainFrag", false)) {
                    // 切换到主页面
                    mainViewPager.currentItem = TabPage.MAIN.position
                }
            }
        }

        // 获取是否显示底部栏的状态
        val isShowBottomBar by remember { mutableStateOf(utils.getIsShowBottomBar()) }

        // 控制状态栏图标颜色
        LaunchedEffect(currentTabPosition) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            val isDarkMode = utils.getIfDarkMode()
            val isMainPage = currentTabPosition == TabPage.MAIN.position

            if (isMainPage) {
                if (utils.getMapType() == 3 || (utils.getMapType() == 0 && isDarkMode)) {
                    insetsController.isAppearanceLightStatusBars = false
                } else {
                    insetsController.isAppearanceLightStatusBars = true
                }
            } else {
                insetsController.isAppearanceLightStatusBars = !isDarkMode
            }
        }

        LaunchedEffect(isLandscape) {

            Log.d("L163", "${isLandscape}")

            if (isLandscape) {
                mainViewPager.currentItem = TabPage.MAIN.position
                mainViewPager.isUserInputEnabled = false
            }

            if (!isLandscape) {
                mainViewPager.isUserInputEnabled = true
            }

            setCurrentTabPosition(0)
        }

        // 处理返回键
        BackHandler {
            onBack()
        }
        val controller = remember { ThemeController(ColorSchemeMode.System) }

        val context = LocalContext.current

        // 主viewPager
        val mainViewPager = remember {
            this@MainActivity.mainViewPager =
                getViewPager2(context, setCurrentTabPosition, false)
            this@MainActivity.mainViewPager
        }

        // 副viewPager
        val sideViewPager = remember {
            this@MainActivity.sideViewPager =
                getViewPager2(context, setCurrentTabPosition, true)
            this@MainActivity.sideViewPager
        }

        val (showSideViewPager, setShowSideViewPager) = remember { mutableStateOf(false) }

        MiuixTheme(
            controller = controller
        ) {

            // 竖屏
            if (!isLandscape) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        MyNavigationBar(
                            isShowBottomBar,
                            currentTabPosition,
                            view,
                            setCurrentTabPosition
                        )
                    },
                    content = { innerPadding ->
                        AndroidView(
                            factory = { mainViewPager },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        )
                    }
                )
            }

            // 横屏
            if (isLandscape) {

                val sidePagerProgress by animateFloatAsState(
                    targetValue = if (showSideViewPager) 1f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "sidePager"
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    MyNavigationRail(
                        isShowBottomBar,
                        currentTabPosition,
                        view,
                        true,
                        showSideViewPager,
                        setShowSideViewPager,
                        setCurrentTabPosition
                    )
                    // 主屏
                    AndroidView(
                        factory = { mainViewPager },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(2f)
                    )
                    // 副屏
                    if (sidePagerProgress > 0f) {
                        AndroidView(
                            factory = { sideViewPager },
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(sidePagerProgress)
                        )
                    }
                    // TODO 切换路线页
                }
            }
        }

    }


    @Composable
    fun MyNavigationBar(
        isShowBottomBar: Boolean,
        currentTabPosition: Int,
        view: View,
        setCurrentTabPosition: (Int) -> Unit,
    ) {
        if (isShowBottomBar && utils.getIsShowBottomBar()) {
            NavigationBar {
                TabPage.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = ImageVector.vectorResource(tab.iconResId),
                        label = stringResource(id = tab.titleResId),
                        selected = currentTabPosition == tab.position,
                        onClick = {
                            handleTabClick(
                                tab, currentTabPosition, view,
                                isLandscape = false,
                                setShowSideViewPager = {},
                                setCurrentTabPosition
                            )
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun MyNavigationRail(
        isShowBottomBar: Boolean,
        currentTabPosition: Int,
        view: View,
        isLandscape: Boolean,
        showSideViewPager: Boolean,
        setShowSideViewPager: (Boolean) -> Unit,
        setCurrentTabPosition: (Int) -> Unit
    ) {
        if (isShowBottomBar && utils.getIsShowBottomBar()) {
            val railState = rememberNavigationRailState()
            NavigationRail(state = railState) {
                TabPage.entries.forEach { tab ->
                    NavigationRailItem(
                        icon = ImageVector.vectorResource(tab.iconResId),
                        label = stringResource(id = tab.titleResId),
                        selected = currentTabPosition == tab.position,
                        onClick = {
                            handleTabClick(
                                tab,
                                currentTabPosition,
                                view,
                                isLandscape,
                                setShowSideViewPager,
                                setCurrentTabPosition
                            )
                        }
                    )
                }
            }
        }
    }

    private fun handleTabClick(
        tab: TabPage,
        currentTabPosition: Int,
        view: View,
        isLandscape: Boolean,
        setShowSideViewPager: (Boolean) -> Unit,
        setCurrentTabPosition: (Int) -> Unit
    ) {

//        Log.d("L321", "${isLandscape}")

        val viewPager = if (isLandscape)
            sideViewPager
        else
            mainViewPager

        if (isLandscape) {
            if (tab.position == 0) {
                setShowSideViewPager(false)
            } else {
                setShowSideViewPager(true)
            }
        }

        setCurrentTabPosition(tab.position)

        // 主控锁定检查
        if (utils.isOperationLock()) {
            utils.showMsg(getString(R.string.operation_lock_on_tip))
            // 切换到主页
            viewPager.currentItem = TabPage.MAIN.position
            return
        }

        // 横屏下只允许在主控
//        if (isLandscape && tab.position != TabPage.MAIN.position) {
//            return
//        }

//        Log.d("L349", "${currentTabPosition}, ${tab.position}")

        if (currentTabPosition != tab.position) {
            // 切换到对应的Tab
            if (isLandscape) {
                if (tab.position != 0) {
                    viewPager.currentItem = tab.position - 1
                }
            } else {
                viewPager.currentItem = tab.position
            }
        } else {
            // 点击当前项滚动到顶部
            val action = when (tab) {
                TabPage.LINE -> ScrollEventBus.lineListScrollToTopActionName
                TabPage.STATION -> ScrollEventBus.stationListScrollToTopActionName
                else -> null
            }
            action?.let {
                lifecycleScope.launch {
                    ScrollEventBus.postScrollEvent(it)
                }
            }
        }
        utils.haptic(view)
    }


    // 处理返回键
    private fun onBack() {
        val currentPosition = mainViewPager.currentItem

        if (currentPosition == TabPage.MAIN.position) {
            // 在主页面
            if (utils.getIsSaveBackAfterExit()) {
                moveTaskToBack(true)
            } else {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancelAll()
                    finish()
                } else {
                    utils.showMsg(getString(R.string.press_again_exit_app))
                }
                backPressedTime = System.currentTimeMillis()
            }
        } else {
            // 在其他页面，切换到主页
            mainViewPager.currentItem = TabPage.MAIN.position
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 返回键已经在Compose中通过BackHandler处理
        return super.onKeyDown(keyCode, event)
    }

    override fun onRestart() {
        super.onRestart()
        wakeLock.release()
        wakeLock.acquire(60 * 60 * 1000L)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lifecycleScope.launch {
            _newIntentFlow.emit(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        Log.d(tag, "requestCode: $requestCode")

        var allGranted = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        if (allGranted) {
            when (requestCode) {
                PermissionManager.REQUEST_LOCATION -> {
                    val intent = Intent()
                        .setAction(utils.openLocationActionName)
                    LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(intent)
                }
            }
        }
    }

    /**
     * 切换到指定的Tab页面
     * @param position TabPage的位置
     * @param smoothScroll 是否平滑滚动
     */
    override fun switchToTab(position: Int, smoothScroll: Boolean) {
        if (::mainViewPager.isInitialized) {
            val targetPosition = position.coerceIn(0, TabPage.entries.size - 1)
            Log.d(tag, "switchToTab called: $targetPosition")
            lifecycleScope.launch {
                // 🔥 每次生成不同的值，确保 Flow 能触发
                _switchTabFlow.emit(Pair(targetPosition, smoothScroll))
            }
        } else {
            Log.w(tag, "viewPager not initialized yet")
        }
    }

    /**
     * 切换到指定的Tab页面
     * @param tab TabPage枚举
     * @param smoothScroll 是否平滑滚动
     */
    override fun switchToTab(tab: TabPage, smoothScroll: Boolean) {
        Log.d("switchToTab", "switchToTab")
        switchToTab(tab.position, smoothScroll)
    }


    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        mainViewPager.adapter = null
    }

    // 页面适配器
    class MyPagerAdapter(activity: FragmentActivity, private var fragRange: IntRange = (0..3)) :
        FragmentStateAdapter(activity) {

        // 使用lazy初始化，确保Fragment只创建一次
        private val allFragments by lazy {
            listOf(
                MainFragment(),
                LineFragment(),
                StationFragment(),
                SettingFragment()
            )
        }

        // 根据 fragRange 获取对应的 fragments
        private val fragments: List<Fragment>
            get() = fragRange.map { allFragments[it] }

        // 内部用可变列表保存当前页面索引（顺序即显示顺序）
        private val pageIndexes: MutableList<Int> = fragRange.toMutableList()

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        // 可选：为每个Fragment提供稳定的ID
        override fun getItemId(position: Int): Long {
            return pageIndexes[position].toLong()
        }

        // 可选：确保Fragment不会被重新创建
        override fun containsItem(itemId: Long): Boolean {
            return pageIndexes.contains(itemId.toInt())
        }


    }

    fun getViewPager2(
        context: Context,
        setCurrentTabPosition: (Int) -> Unit,
        isSidePage: Boolean
    ): ViewPager2 {

        val fragRange = if (isSidePage)
            (1..3)
        else
            (0..3)

        Log.d("L526", "${isSidePage} ${fragRange}")
        return ViewPager2(context).apply {
            id = View.generateViewId()
            // 滑动切换
            isUserInputEnabled = true
            // 设置离屏页面数量为页面总数，确保所有Fragment都保持存活
            offscreenPageLimit = TabPage.entries.size

            // 初始化Adapter
            adapter = MyPagerAdapter(this@MainActivity, fragRange)

            // 设置页面切换监听
            registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    Log.d("L541", "${position}")
                    var truePosition = position
                    if (isSidePage) {
                        truePosition++
                    }
                    super.onPageSelected(truePosition)
                    setCurrentTabPosition(truePosition)
                }
            })
        }
    }
}