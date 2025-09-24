package com.microbus.announcer.bean

import java.time.LocalTime

class RunningInfo(
    var time: LocalTime,
    var lineId: Int,
    var stationId: Int,
    var lineName: String,
    var terminalName: String,
    var stationName: String,
    var stationState: Int
)