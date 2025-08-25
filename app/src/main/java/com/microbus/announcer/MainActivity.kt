package com.microbus.announcer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.microbus.announcer.adapter.FragAdapter
import com.microbus.announcer.databinding.ActivityMainBinding
import com.microbus.announcer.fragment.LineFragment
import com.microbus.announcer.fragment.MainFragment
import com.microbus.announcer.fragment.SettingFragment
import com.microbus.announcer.fragment.StationFragment
import java.util.Locale


class MainActivity : AppCompatActivity() {

    var tag: String = javaClass.simpleName
    val fragmentList: MutableList<Fragment> = ArrayList()


    private lateinit var utils: Utils

    /**屏幕唤醒锁*/
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: WakeLock

    lateinit var binding: ActivityMainBinding

    private var exitTime: Long = 0

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        utils = Utils(this)

        // 设置语言
        val config = resources.configuration
        config.locale = Locale(utils.getUILang())
        resources.updateConfiguration(config, null)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //设置低亮度1h屏幕唤醒锁
        powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, tag)
        wakeLock.acquire(60 * 60 * 1000L)

        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        //点亮状态栏，控制状态栏字体颜色变黑
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        fragmentList.add(MainFragment())
        fragmentList.add(LineFragment())
        fragmentList.add(StationFragment())
        fragmentList.add(SettingFragment())
        binding.viewPager.adapter = FragAdapter(this, fragmentList)
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val mainFragment = fragmentList[0] as MainFragment
            if (mainFragment.isOperationLock)
                return@setOnItemSelectedListener false
            val lastItem = binding.viewPager.currentItem
            when (item.itemId) {
                R.id.item0 -> binding.viewPager.currentItem = 0
                R.id.item1 -> binding.viewPager.currentItem = 1
                R.id.item2 -> binding.viewPager.currentItem = 2
                R.id.item3 -> binding.viewPager.currentItem = 3
            }
            if (lastItem != binding.viewPager.currentItem)
                utils.haptic(binding.bottomNavigationView)
            return@setOnItemSelectedListener true
        }

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val mainFragment = fragmentList[0] as MainFragment
                if (mainFragment.isOperationLock) {
                    utils.showMsg("操作锁定已开启\n请长按定位按钮解锁")
                    binding.viewPager.currentItem = 0
                    return
                }
                super.onPageSelected(position)
                binding.bottomNavigationView.menu[position].isChecked = true
            }
        })

        if (utils.getIsShowBottomBar())
            binding.bottomNavigationView.visibility = View.VISIBLE
        else
            binding.bottomNavigationView.visibility = View.GONE


    }

    override fun onRestart() {
        Log.d(tag, "onRestart")
        wakeLock.release()
        wakeLock.acquire(60 * 60 * 1000L)
        super.onRestart()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                //当前不在主控界面，跳转到主控界面
                if (binding.viewPager.currentItem != 0)
                    binding.viewPager.currentItem = 0

                //再按一次退出应用
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    utils.showMsg("再按一次退出应用")
                    exitTime = System.currentTimeMillis()
                } else {
                    finish()
//                    exitProcess(0)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        val messageType = intent?.getIntExtra("message", -1)
//        if (messageType != null) {
//            binding.viewPager.currentItem = messageType
//        }
//    }

//    override fun onPause() {
//        super.onPause()
//        val mainFrag = binding.viewPager.findFragment<MainFragment>()
//        mainFrag.locationClient.stopLocation()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if(binding.viewPager.isActivated){
//            val mainFrag = binding.viewPager.findFragment<MainFragment>()
//            mainFrag.locationClient.startLocation()
//        }
//
//    }

}
