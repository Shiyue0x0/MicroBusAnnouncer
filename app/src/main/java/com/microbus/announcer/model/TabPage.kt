package com.microbus.announcer.model

import com.microbus.announcer.R

// 定义页面枚举
enum class TabPage(val position: Int, val titleResId: Int, val iconResId: Int) {
    MAIN(0, R.string.nav_main, R.drawable.keyboard_command_key),
    LINE(1, R.string.nav_line, R.drawable.line),
    STATION(2, R.string.nav_station, R.drawable.station),
    SETTING(3, R.string.nav_setting, R.drawable.settings)
}