package com.microbus.announcer.bean

/**
 * 高德轨迹点上传参数
 */
data class TrackPoint(
    val location: String,
    val locatetime: Long? = null,
    val speed: Double? = null,
    val direction: Double? = null,
    val height: Double? = null,
    val accuracy: Double? = null,
    val props: Map<String, Any>? = null
)