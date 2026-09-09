package com.microbus.announcer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

object ScrollEventBus {
    // 使用 SharedFlow 发送事件，replay = 0 表示新订阅者不会收到历史事件
    private val _scrollEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val scrollEvents = _scrollEvents.asSharedFlow()

    val lineListScrollToTopActionName = "line_list_scroll_to_top"
    val stationListScrollToTopActionName = "station_list_scroll_to_top"

    // 发送滚动事件
    suspend fun postScrollEvent(action: String) {
        _scrollEvents.emit(action)
    }

    // 订阅特定事件的便捷方法
    fun subscribeToAction(action: String): Flow<String> {
        return scrollEvents.filter { it == action }
    }
}