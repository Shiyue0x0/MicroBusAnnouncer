package com.microbus.announcer.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


open class FragFragAdapter(
    ff: Fragment,
    private val list: MutableList<Fragment>
) : FragmentStateAdapter(ff) {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }


}
