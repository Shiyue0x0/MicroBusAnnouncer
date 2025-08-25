package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.microbus.announcer.Utils
import com.microbus.announcer.databinding.FragmentSettingBinding

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val tag = javaClass.simpleName
    private lateinit var utils: Utils

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        utils = Utils(requireContext())

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

//        if(savedInstanceState == null){
//            init()
//        }

        return binding.root

    }

    override fun onResume() {
        super.onResume()
        init()
    }

    private fun init(){
        val fragmentManager: FragmentManager = getChildFragmentManager()
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        val preferenceFragment = SettingPreferenceFragment()
        fragmentTransaction.replace(binding.fragmentContainer.id, preferenceFragment)
        fragmentTransaction.commitAllowingStateLoss()

        Log.d(tag,"fragmentTransaction.add")
    }
}