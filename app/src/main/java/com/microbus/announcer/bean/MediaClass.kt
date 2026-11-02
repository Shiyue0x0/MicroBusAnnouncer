package com.microbus.announcer.bean

enum class AudioSourceType {
    /** 空白间隔（例如100ms） */
    BLANK,

    /** 本地文件 */
    LOCAL,

    /** 网络请求获取 */
    NETWORK,

    /** 文字转语音 */
    TTS
}

enum class MediaItemStatus {
    /** 加载中 */
    LOADING,

    /** 加载完成 */
    COMPLETED,

    /** 加载失败 */
    FAILED
}

data class AnMediaItem(
    var source: AudioSourceType,
    var filePath: String,
    val text: String?,
    var status: MediaItemStatus = MediaItemStatus.LOADING,
    var networkInfo: NetworkInfo = NetworkInfo(
        voiceText = "",
        downloadBeginTime = 0,
        downloadDoneTime = 0,
    )
)

data class NetworkInfo(
    var voiceText: String,
    var downloadBeginTime: Long?,
    var downloadDoneTime: Long?,
)