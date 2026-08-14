package com.microbus.announcer

interface TabSwitchListener {
    /**
     * 切换到指定Tab
     * @param position Tab位置
     * @param smoothScroll 是否平滑滚动
     */
    fun switchToTab(position: Int, smoothScroll: Boolean = true)

    /**
     * 切换到指定Tab
     * @param tab TabPage枚举
     * @param smoothScroll 是否平滑滚动
     */
    fun switchToTab(tab: TabPage, smoothScroll: Boolean = true)
}