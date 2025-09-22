package com.microbus.announcer.bean

class Line(
    var id: Int? = null,
    var name: String = "",
    var upLineStation: String = "",
    var downLineStation: String = "",
    var isUpAndDownInvert: Boolean = true,
    var type: String = "B",
)