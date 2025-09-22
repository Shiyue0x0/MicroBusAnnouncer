package com.microbus.announcer.bean


class EsItem(
    var leftText: String = "",
    var rightText: String = "",
    var minTimeS: Int = 5,
    var type: String = ""       /* D:普通文本 | N下一站文本 W即将到站文本 A到站文本 | S起点站文本 C当前站文本 T终点站文本 | B首末站文本，R实时更新文本  */
)