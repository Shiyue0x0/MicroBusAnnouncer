package com.microbus.announcer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


open class FragAdapter(
    fa: FragmentActivity,
    private val list: MutableList<Fragment>
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }

}
