package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.FragFragAdapter
import com.microbus.announcer.databinding.FragmentSettingBinding
import com.microbus.announcer.fragment.settings.AnSettingsFragment
import com.microbus.announcer.fragment.settings.SysAndEsSettingsFragment

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


        fragmentList.add(SysAndEsSettingsFragment())
        fragmentList.add(AnSettingsFragment())
        fragmentList.add(SettingPreferenceFragment())
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

}