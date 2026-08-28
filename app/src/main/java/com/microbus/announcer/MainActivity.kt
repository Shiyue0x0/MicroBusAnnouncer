package com.microbus.announcer

import kotlinx.coroutines.flow.MutableStateFlow
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.microbus.announcer.fragment.LineFragment
import com.microbus.announcer.fragment.MainFragment
import com.microbus.announcer.fragment.SettingFragment
import com.microbus.announcer.fragment.StationFragment
import com.microbus.announcer.ui.theme.AnnouncerTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.icon.MiuixIcons

// 定义页面枚举
enum class TabPage(val position: Int, val titleResId: Int, val iconResId: Int) {
    MAIN(0, R.string.nav_main, R.drawable.keyboard_command_key),
    LINE(1, R.string.nav_line, R.drawable.line),
    STATION(2, R.string.nav_station, R.drawable.station),
    SETTING(3, R.string.nav_setting, R.drawable.settings)
}

class MainActivity : AppCompatActivity(), TabSwitchListener {

    var tag: String = javaClass.simpleName
    private lateinit var utils: Utils
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: WakeLock
    private var backPressedTime: Long = 0

    // ViewPager2 和 Adapter 引用
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: MainPagerAdapter

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
            AnnouncerTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {

        val view = LocalView.current

        // 当前选中的Tab位置
        var currentTabPosition by remember { mutableIntStateOf(0) }

        // 使用 collectAsState 替代 collectAsStateWithLifecycle
        val switchRequest by _switchTabFlow.collectAsState()

        LaunchedEffect(switchRequest) {
            switchRequest?.let { (position, smoothScroll) ->
                if (::viewPager.isInitialized) {
                    Log.d(tag, "Switching to tab: $position, smooth: $smoothScroll")
                    viewPager.setCurrentItem(position, smoothScroll)
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
                    viewPager.currentItem = TabPage.MAIN.position
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

        // 处理返回键
        BackHandler {
            onBack()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (isShowBottomBar && utils.getIsShowBottomBar()) {
                    // 使用Compose的NavigationBar作为底部导航
                    NavigationBar(
                        containerColor = colorResource(id = R.color.an_contain_bg)
                    ) {
                        TabPage.entries.forEach { tab ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painterResource(id = tab.iconResId),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(id = tab.titleResId)) },
                                selected = currentTabPosition == tab.position,
                                onClick = {
                                    // 主控锁定检查
                                    if (utils.isOperationLock()) {
                                        utils.showMsg(getString(R.string.operation_lock_on_tip))
                                        // 切换到主页
                                        viewPager.currentItem = TabPage.MAIN.position
                                        return@NavigationBarItem
                                    }

                                    if (currentTabPosition != tab.position) {
                                        // 切换到对应的Tab
                                        viewPager.currentItem = tab.position
                                    } else {
                                        // 点击当前项滚动到顶部
                                        val action = when (tab) {
                                            TabPage.LINE -> utils.lineListScrollToTopActionName
                                            TabPage.STATION -> utils.stationListScrollToTopActionName
                                            else -> null
                                        }
                                        action?.let {
                                            LocalBroadcastManager.getInstance(this@MainActivity)
                                                .sendBroadcast(Intent().setAction(it))
                                        }
                                    }
                                    utils.haptic(view)
                                }
                            )
                        }
                    }
                }
            }

        ) { innerPadding ->
            // 使用AndroidView包装ViewPager2
            AndroidView(
                factory = { context ->
                    // 创建ViewPager2
                    viewPager = ViewPager2(context).apply {
                        id = View.generateViewId()
                        // 滑动切换
                        isUserInputEnabled = true
                        // 设置离屏页面数量为页面总数，确保所有Fragment都保持存活
                        offscreenPageLimit = TabPage.entries.size - 1
                    }

                    // 初始化Adapter
                    pagerAdapter = MainPagerAdapter(this@MainActivity)
                    viewPager.adapter = pagerAdapter

                    // 设置页面切换监听
                    viewPager.registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            currentTabPosition = position
                        }
                    })

                    viewPager
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                update = { view ->
                    // 当主题变化或其他更新时，可以在这里处理
                    // 例如更新Fragment的配置等
                }
            )
        }
    }

    // 页面适配器
    class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        // 使用lazy初始化，确保Fragment只创建一次
        private val fragments by lazy {
            listOf(
                MainFragment(),
                LineFragment(),
                StationFragment(),
                SettingFragment()
            )
        }

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        // 可选：为每个Fragment提供稳定的ID
        override fun getItemId(position: Int): Long {
            return when (position) {
                TabPage.MAIN.position -> TabPage.MAIN.position.toLong()
                TabPage.LINE.position -> TabPage.LINE.position.toLong()
                TabPage.STATION.position -> TabPage.STATION.position.toLong()
                TabPage.SETTING.position -> TabPage.SETTING.position.toLong()
                else -> super.getItemId(position)
            }
        }

        // 可选：确保Fragment不会被重新创建
        override fun containsItem(itemId: Long): Boolean {
            return itemId in 0 until itemCount
        }
    }

    // 处理返回键
    private fun onBack() {
        val currentPosition = viewPager.currentItem

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
            viewPager.currentItem = TabPage.MAIN.position
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
        if (::viewPager.isInitialized) {
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
        viewPager.adapter = null
    }
}