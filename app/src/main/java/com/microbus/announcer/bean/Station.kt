package com.microbus.announcer.bean

import java.io.Serializable

class Station(
    var id: Int? = null,
    var cnName: String = "",
    var enName: String = "",
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var type: String = "B"
) : Serializable