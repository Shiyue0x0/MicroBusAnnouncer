package com.microbus.announcer.bean


class EsItem(
    var leftText: String = "",
    var rightText: String = "",
    var minTimeS: Int = 5,
    var type: String = ""       /* C:普通文本，N下一站文本，W即将到站文本，A到站文本，B首末站文本，S速度文本，T时间文本 */
)