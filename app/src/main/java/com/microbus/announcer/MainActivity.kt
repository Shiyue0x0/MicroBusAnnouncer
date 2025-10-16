package com.microbus.announcer

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import com.microbus.announcer.adapter.FragActivityAdapter
import com.microbus.announcer.databinding.ActivityMainBinding
import com.microbus.announcer.fragment.LineFragment
import com.microbus.announcer.fragment.MainFragment
import com.microbus.announcer.fragment.SettingFragment
import com.microbus.announcer.fragment.StationFragment


class MainActivity : AppCompatActivity() {

    var tag: String = javaClass.simpleName
    val fragmentList: MutableList<Fragment> = ArrayList()


    private lateinit var utils: Utils

    /**屏幕唤醒锁*/
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: WakeLock

    lateinit var binding: ActivityMainBinding

    private var backPressedTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        utils = Utils(this)

        utils.loadAnnouncementFormatFromConfig()

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // 设置语言
        utils.setUILang(utils.getUILang())

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //设置低亮度1h屏幕唤醒锁
        powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, tag)
        wakeLock.acquire(60 * 60 * 1000L)

        fragmentList.add(MainFragment())
        fragmentList.add(LineFragment())
        fragmentList.add(StationFragment())
        fragmentList.add(SettingFragment())

        binding.viewPager.adapter = FragActivityAdapter(this, fragmentList)
        binding.viewPager.offscreenPageLimit = binding.viewPager.adapter!!.itemCount

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val mainFragment = fragmentList[0] as MainFragment
            if (mainFragment.isOperationLock)
                return@setOnItemSelectedListener false
            val lastItem = binding.viewPager.currentItem
            when (item.itemId) {
                // 主控
                R.id.item0 -> binding.viewPager.currentItem = 0
                // 路线
                R.id.item1 -> {
                    if (binding.viewPager.currentItem != 1) {
                        binding.viewPager.currentItem = 1
                    } else {
                        val intent = Intent()
                            .setAction(utils.lineListScrollToTopActionName)
                        LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(intent)
                    }
                }
                // 站点
                R.id.item2 -> {
                    if (binding.viewPager.currentItem != 2) {
                        binding.viewPager.currentItem = 2
                    } else {
                        val intent = Intent()
                            .setAction(utils.stationListScrollToTopActionName)
                        LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(intent)
                    }
                }                // 设置
                R.id.item3 -> binding.viewPager.currentItem = 3
            }
            if (lastItem != binding.viewPager.currentItem)
                utils.haptic(binding.bottomNavigationView)
            return@setOnItemSelectedListener true
        }

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                if (position == 0) {
                    // 黑夜地图 或 （跟随系统且主题色黑色）
                    if (utils.getMapType() == 3 || (utils.getMapType() == 0 && utils.getIfDarkMode())) {
                        insetsController.isAppearanceLightStatusBars = false
                    } else {
                        insetsController.isAppearanceLightStatusBars = true
                    }
                } else {
                    insetsController.isAppearanceLightStatusBars = !utils.getIfDarkMode()
                }


                // LineFragment
                val lineFragment = fragmentList[1] as LineFragment
                if (position == 1) {
                    lineFragment.updateAllItemShown(true)
                }
                // Not LineFragment
                else {
                    lineFragment.updateAllItemShown(false)
                }

                super.onPageSelected(position)
                val mainFragment = fragmentList[0] as MainFragment
                if (mainFragment.isOperationLock) {
                    utils.showMsg(getString(R.string.operation_lock_on_tip))
                    binding.viewPager.currentItem = 0
                    return
                }
                binding.bottomNavigationView.menu[position].isChecked = true


            }
        })

        if (utils.getIsShowBottomBar())
            binding.bottomNavigationView.visibility = View.VISIBLE
        else
            binding.bottomNavigationView.visibility = View.GONE

        onBackPressedDispatcher.addCallback(this) {
            onBack()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            onBack()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onRestart() {
        super.onRestart()
        wakeLock.release()
        wakeLock.acquire(60 * 60 * 1000L)
//        Log.d(tag, "onRestart" + intent.getBooleanExtra("switchToMainFrag", false))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("switchToMainFrag", false)) {
            binding.viewPager.currentItem = 0
        }
    }

    override fun onResume() {
        super.onResume()
        val lineFragment = fragmentList[1] as LineFragment
        lineFragment.updateAllItemShown(true)
    }

    override fun onPause() {
        val lineFragment = fragmentList[1] as LineFragment
        lineFragment.updateAllItemShown(false)
        super.onPause()
    }


    fun onBack() {
        // 主控
        if (binding.viewPager.currentItem == 0) {
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

        }
        // 其他页
        else {
            binding.viewPager.currentItem = 0
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

}
