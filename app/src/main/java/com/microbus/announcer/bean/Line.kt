package com.microbus.announcer.bean

import java.io.Serializable

data class Line(
    var id: Int? = null,
    var name: String = "",
    var upLineStation: String = "",
    var downLineStation: String = "",
    @Deprecated("use isRingRoute instead")
    var isUpAndDownInvert: Boolean = true,
    var type: String = "B",
    var isRingRoute: Boolean = false, //自 dbVersion 2 起
) : Serializable