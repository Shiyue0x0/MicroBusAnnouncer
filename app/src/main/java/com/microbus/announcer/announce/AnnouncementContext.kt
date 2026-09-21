package com.microbus.announcer.announce

import com.microbus.announcer.bean.Station

data class AnnouncementContext(
    val lineName: String,
    val stationList: List<Station>,
    val stationCount: Int,
    val stationState: Int,
    val speedKmh: Double,
    val announcementLangList: ArrayList<String>
)