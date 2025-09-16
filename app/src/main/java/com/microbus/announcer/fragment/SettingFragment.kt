package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.kiylx.compose.preference.component.auto.PreferenceCollapseItem
import com.kiylx.compose.preference.component.auto.PreferenceItem
import com.kiylx.compose.preference.component.auto.SetTheme
import com.kiylx.compose.preference.component.cross.PreferenceItem
import com.kiylx.compose.preference.theme.PreferenceIconStyle
import com.kiylx.compose.preference.theme.Preferences
import com.kiylx.libx.pref_component.preference_util.OldPreferenceHolder
import com.microbus.announcer.PrefsHelper
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.FragActivityAdapter
import com.microbus.announcer.adapter.FragFragAdapter
import com.microbus.announcer.databinding.FragmentSettingBinding
import java.time.LocalDateTime

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val tag = javaClass.simpleName
    private lateinit var utils: Utils

    private lateinit var prefs: SharedPreferences

    val fragmentList: MutableList<Fragment> = ArrayList()

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())


        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

//        return ComposeView(requireContext()).apply {
//            setContent {
//                Preferences(requireContext())
//            }
//        }


//        init()
//        if(savedInstanceState == null){
//            init()
//        }


        fragmentList.add(LineFragment())
        fragmentList.add(StationFragment())
        binding.viewPager.adapter = FragFragAdapter(this, fragmentList)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })


        return binding.root

    }

    override fun onResume() {
        super.onResume()
//        init()
    }

//    private fun init() {
//        val fragmentManager: FragmentManager = getChildFragmentManager()
//        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
//        val preferenceFragment = SettingPreferenceFragment()
//        fragmentTransaction.replace(binding.fragmentContainer.id, preferenceFragment)
//        fragmentTransaction.commitAllowingStateLoss()
//    }

    @Composable
    fun Preferences(ctx: Context) {

        val holder = remember {
            OldPreferenceHolder.instance(
                ctx.getSharedPreferences(
                    "ddd",
                    Context.MODE_PRIVATE
                )
            )
        }

        val customNodeName = "customNode"
        //创建一个自定义节点
        val node = holder.registerDependence(customNodeName, true)
        val scope = rememberCoroutineScope()

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = resources.getDimensionPixelSize(resourceId)

        val themeTint =
            if (isSystemInDarkTheme())
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onPrimary

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Preferences
                .SetTheme(
                    holder = holder,
                    iconStyle = PreferenceIconStyle(
                        paddingValues = PaddingValues(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                    )
                )
                {
                    Column {
                        Text(
                            text = "",
                            modifier = Modifier
                                .height(statusBarHeight.dp)
                        )
                        Text(
                            text = stringResource(R.string.nav_setting),
                        )

                        var expand by remember { mutableStateOf(false) }
                        PreferenceCollapseItem(
                            expand = expand,
                            title = "基本",
                            dependenceKey = customNodeName,
                            stateChanged = { expand = !expand })
                        {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                PreferenceItem(
                                    title = "默认路线",
                                    dependenceKey = customNodeName,
                                    desc = "",
                                    onClick = {
                                        PrefsHelper(prefs).defaultLineName =
                                            LocalDateTime.now().toString()
                                    }
                                )
                                PreferenceItem(
                                    title = "界面语言",
                                    desc = "简体中文",
                                )
                            }
                        }


//                    PreferenceSwitch(
//                        defaultValue = false,
//                        title = "使用新特性",
//                        desc = "实验功能，可能不稳定",
//                        dependenceKey = DependenceNode.rootName,
//                        keyName = "s1"
//                    ) { state ->
//                        //这里获取并修改了当前的enable状态，
//                        //依赖这个节点的会改变显示状态，
//                        //如果当前没有指定依赖，自身也会受到影响
//                        scope.launch {
//                            holder.getDependence("s1")?.setEnabled(state)
//                        }
//                    }
//                    PreferenceItem(
//                        dependenceKey = "s1",
//                        title = "关联组件",
//                        icon = Icons.Outlined.AccountCircle
//                    )
//
//                    PreferenceSwitchWithContainer(
//                        title = "调整您的设置信息",
//                        desc = "账户、翻译、帮助信息等",
//                        defaultValue = false,
//                        keyName = "b2",
//                        dependenceKey = DependenceNode.rootName,
//                        icon = Icons.Outlined.AccountCircle,
//                    ) {
//                        scope.launch {
//                            node.setEnabled(it)
//                        }
//                    }
//                    PreferenceItem(
//                        modifier = Modifier,
//                        title = "账户",
//                        icon = Icons.Outlined.AccountCircle,
//                        dependenceKey = customNodeName,
//                        desc = "本地、谷歌",
//                    )
//                    var expand by remember { mutableStateOf(false) }
//                    PreferenceCollapseItem(
//                        expand = expand,
//                        title = "附加内容",
//                        dependenceKey = customNodeName,
//                        stateChanged = { expand = !expand })
//                    {
//                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
//                            PreferenceItem(
//                                title = "动画",
//                                desc = "动画反馈、触感反馈",
//                            )
//                            PreferenceItem(
//                                title = "语言",
//                                desc = "中文(zh)",
//                            )
//                        }
//                    }
//                    PreferencesCautionCard(
//                        title = "调整您的设置信息",
//                        desc = "账户、翻译、帮助信息等",
//                        dependenceKey = customNodeName,
//                        icon = Icons.Outlined.AccountCircle,
//                    )

                    }
                }
        }
    }
}