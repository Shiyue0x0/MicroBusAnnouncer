package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.microbus.announcer.adapter.FragFragAdapter
import com.microbus.announcer.databinding.FragmentSettingBinding
import com.microbus.announcer.fragment.settings.AnSettingsFragment
import com.microbus.announcer.fragment.settings.DataAndAboutSettingsFragment
import com.microbus.announcer.fragment.settings.LocationAndMapSettingsFragment
import com.microbus.announcer.fragment.settings.SysAndEsSettingsFragment

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val tag = javaClass.simpleName

    val fragmentList: MutableList<Fragment> = ArrayList()

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        fragmentList.add(SysAndEsSettingsFragment())
        fragmentList.add(AnSettingsFragment())
        fragmentList.add(LocationAndMapSettingsFragment())
        fragmentList.add(DataAndAboutSettingsFragment())
//        fragmentList.add(SettingPreferenceFragment())

        binding.viewPager.adapter = FragFragAdapter(this, fragmentList)
        binding.viewPager.offscreenPageLimit = binding.viewPager.adapter!!.itemCount

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


}