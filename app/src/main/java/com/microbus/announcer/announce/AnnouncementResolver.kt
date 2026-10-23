package com.microbus.announcer.announce

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.microbus.announcer.HTTPRequest
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.util.WavSilenceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds


class AnnouncementResolver(private val context: Context) {

    private var utils = Utils(context)
    private var stationDatabaseHelper = StationDatabaseHelper.getInstance(context)
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private lateinit var anContext: AnnouncementContext
    private lateinit var format: String
    private var ttsFileDoneList = ArrayList<String>()
    private var tempFilePath = ""
    private var ttsTextList = ArrayList<String>()


    init {

        // 新建缓存文件目录
        File(tempFilePath).mkdirs()

        initTTS()

    }

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

    class MediaItem(
        var source: AudioSourceType,
        var filePath: String,
        val text: String?,
        var status: MediaItemStatus = MediaItemStatus.LOADING,
        var networkInfo: NetworkInfo = NetworkInfo(
            voiceText = ""
        )
    )

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.CHINA
                ttsReady = true
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    utils.showMsg("TTS加载失败")
                }
            }
//            Log.d("L1895", status.toString())
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(utteranceId: String) {
            }

            override fun onDone(utteranceId: String) {
                Log.d("L102", "onDone: $utteranceId")
                ttsFileDoneList.add(utteranceId)
                mediaItemList.findAllTTSByPath(utteranceId).forEach {
                    it.status = MediaItemStatus.COMPLETED
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                mediaItemList.findAllTTSByPath(utteranceId).forEach {
                    it.status = MediaItemStatus.FAILED
                }
                CoroutineScope(Dispatchers.Main).launch {
                    utils.showMsg("TTS合成异常，请检查系统设置")
                }
            }

        })

        tempFilePath = context.getExternalFilesDir("")?.path ?: ""

        if (tempFilePath == "") {
            CoroutineScope(Dispatchers.Main).launch {
                utils.showMsg("缓存目录异常")
            }
        }

        // 清空TTS生成目录
        if (utils.getIsUseTTS() && ttsReady) {
            // 清空TTS
            File("$tempFilePath/tts").walkTopDown().forEach {
                it.delete()
            }
            File("$tempFilePath/tts").mkdirs()
        }

    }

    fun resolveAndBuild(
        format: String = "", anContext: AnnouncementContext
    ): MutableList<MediaItem> {

        this.format = format
        this.anContext = anContext

        val mediaList = resolve()
        val filePathList = build(mediaList)

        Log.d("L94", filePathList.toString())

        return filePathList
    }

    fun resolve(): java.util.ArrayList<String> {

        if (anContext.stationList.isEmpty()) {
            return arrayListOf()
        }

        val mediaList = ArrayList<String>()

        val anExps = getAnExps()

        if (anExps == "") {
            return arrayListOf()
        }

        val anExpList = anExps.split("\n")
        val chooseIndex = Random.nextInt(0, anExpList.size)
        val anExp = anExpList[chooseIndex]

        val anList = utils.getAnnouncements(anExp)
        for (item in anList) {

            if (item == "") {
                continue
            }

            // 占位符
            if (item.first() == '<' && item.last() == '>') {

                when (item) {
                    in listOf(
                        "<year>", "<years>", "<month>", "<date>", "<hour>", "<minute>", "<second>"
                    ) -> {
                        val str = resolveTimeKey(item)
                        if (str != "") {
                            mediaList.addAll(getNumOrLetterVoiceList(str))
                        }
                        continue
                    }

                    "<time>" -> {
                        mediaList.addAll(getTimeVoiceList())
                        continue
                    }

                    "<speed>" -> {
                        val lineMediaList = utils.intOrLetterToCnReading(
                            anContext.speedKmh.toInt().toString(), "cn/number/"
                        )
                        mediaList.addAll(lineMediaList)
                        continue
                    }

                    "<line>" -> {
                        // 寻找lineName音频
                        val lineMediaList = resolveLineNameKey()
                        mediaList.addAll(lineMediaList)
                        continue
                    }

                    else -> {

                        // blank
                        if (item.startsWith("<blank") && item.endsWith(">")) {
                            val blankMedia = resolveBlankKey(item)
                            if (blankMedia != "") {
                                mediaList.add(blankMedia)
                            }
                            continue
                        }

                        // station
                        if (item.substring(1, 3) in listOf("ns", "ss", "ts", "ms")) {
                            mediaList.add(resolveStationKey(item))
                            continue
                        }

                    }
                }
            }

            // common（常规）语句
            val commonMedia = resolveCommonMedia(item)
            mediaList.add(commonMedia)

        }

        return mediaList
    }

    fun resolveTimeKey(item: String): String {
        return when (item) {
            "<year>" -> LocalDate.now().year.toString()
            "<years>" -> (LocalDate.now().year % 100).toString()
            "<month>" -> LocalDate.now().monthValue.toString()
            "<date>" -> LocalDate.now().dayOfMonth.toString()
            "<hour>" -> LocalTime.now().hour.toString()
            "<minute>" -> LocalTime.now().minute.toString()
            "<second>" -> LocalTime.now().second.toString()
            else -> ""
        }
    }

    fun resolveStationKey(item: String): String {

        val station = when (item.substring(1, 3)) {
            "ns" -> anContext.stationList[anContext.stationCount]
            "ss" -> anContext.stationList.first()
            "ts" -> anContext.stationList.last()
            "ms" -> {
                val stationList = stationDatabaseHelper.queryById(
                    (item.substring(
                        5, item.length - 1
                    )).toInt()
                )
                if (stationList.isNotEmpty()) stationList.first()
                else Station(
                    id = Int.MAX_VALUE, cnName = "未知站点", enName = "unknown"
                )
            }

            else -> Station()
        }

        val lang = if (item.substring(1, 3) == "ms") {
            item.substring(3, 5)
        } else item.drop(3).dropLast(1)

        val stationName = when (lang) {
            "cn" -> station.cnName
            "en" -> station.enName
            else -> utils.getStationNameFromCn(
                station.cnName, lang
            )
        }

        return "/${lang}/station/${stationName}"

    }

    fun resolveCommonMedia(item: String): String {

        for (lang in anContext.announcementLangList) {
            val file =
                File("${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${lang}/common")

            val fileList =
                file.walk().filter { it.isFile && it.nameWithoutExtension == item }.toList()

            if (fileList.isNotEmpty()) {
                return "/${lang}/common/$item"
            }
        }

        return "/common/$item"
    }

    fun getNumOrLetterVoiceList(str: String): ArrayList<String> {

        //拆分数字和字母
        val strList = "([a-zA-Z]+|\\d+)".toRegex().findAll(str).toList()
        val voiceList = ArrayList<String>()

        strList.forEach { result ->
            if ("\\d+".toRegex().findAll(result.value).toList()
                    .isNotEmpty()
            ) voiceList.addAll(utils.intOrLetterToCnReading(result.value, "/cn/number/"))
            else voiceList.addAll(utils.intOrLetterToCnReading(result.value, "/en/letter/"))

        }

        return voiceList
    }

    fun getTimeVoiceList(): ArrayList<String> {
        val currentTime = LocalTime.now()
        val voiceList = ArrayList<String>()

        voiceList.addAll(utils.intOrLetterToCnReading(currentTime.hour.toString(), "/cn/number/"))
        voiceList.add("/cn/common/点")
        voiceList.addAll(
            utils.intOrLetterToCnReading(
                currentTime.minute.toString(), "/cn/number/", true
            )
        )
        voiceList.add("/cn/common/分")

        return voiceList
    }

    fun resolveLineNameKey(): ArrayList<String> {

        for (lang in anContext.announcementLangList) {
            val file =
                File("${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${lang}/line")
            val fileList =
                file.walk().filter { it.isFile && it.nameWithoutExtension == anContext.lineName }
                    .toList()
            if (fileList.isNotEmpty()) {
                return arrayListOf("/${lang}/line/" + anContext.lineName)
            }
        }

        return getNumOrLetterVoiceList(anContext.lineName)
    }

    fun resolveBlankKey(item: String): String {
        val matchResult = Regex("<blank(\\d+)>").find(item)
        val blankDurationMs = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: return ""
        return "/blank/${blankDurationMs}.pcm"
    }

    var mediaItemList = CopyOnWriteArrayList<MediaItem>()

    fun build(mediaList: ArrayList<String>): MutableList<MediaItem> {

        if (mediaList.isEmpty()) {
            return mutableListOf()
        }

        mediaItemList = CopyOnWriteArrayList<MediaItem>()

        for (mediaWithPath in mediaList) {

            Log.d("L3770", mediaWithPath)

            // 1. 空白占位音频
            if (mediaWithPath.startsWith("/blank")) {
                val blankFilePath = buildBlankMedia(mediaWithPath)
                mediaItemList.add(
                    MediaItem(
                        AudioSourceType.BLANK, blankFilePath, null, MediaItemStatus.COMPLETED
                    )
                )
                continue
            }

            val text = mediaWithPath.split('/').last()


            // 2. 本地音频
            val localFilePath = buildLocalMedia(mediaWithPath)
            if (localFilePath != "") {
                mediaItemList.add(
                    MediaItem(
                        AudioSourceType.LOCAL, localFilePath, text, MediaItemStatus.COMPLETED
                    )
                )
                continue
            }

            // 3. 通过GET/POST请求获取音频
            if (toBuildHttpGetMedia(text, PriorityLevel.IMMEDIATE)) {
                continue
            }

            // 4. TTS合成音频
            val ttsText = utils.getVoiceText(text)
            val ttsFilePath = buildTTSMedia(ttsText)
            if (ttsFilePath != "") {
                val mediaItem = MediaItem(
                    AudioSourceType.TTS, ttsFilePath, ttsText, MediaItemStatus.LOADING
                )
                if (ttsFileDoneList.contains(ttsFilePath)) {
                    mediaItem.status = MediaItemStatus.COMPLETED
                }
                mediaItemList.add(mediaItem)
                continue
            }

        }

        return mediaItemList
    }

    fun buildBlankMedia(media: String): String {

        Log.d("L426", media)

        val matchResult = Regex("/blank/(\\d+).pcm").find(media)
        val blankDurationMs = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: return ""

        val outputPath = "$tempFilePath/blank/${blankDurationMs}.pcm"

        if (!File(outputPath).exists()) {
            val wavSilenceGeneratorRes = WavSilenceGenerator.generateSilenceWav(
                blankDurationMs.toLong(), outputPath
            )
            if (!wavSilenceGeneratorRes) return ""
        }

        return outputPath
    }


    enum class PriorityLevel(val description: String) {
        IMMEDIATE("立即加载"), PRELOAD("预加载"),
    }


    data class NetworkInfo(
        var voiceText: String,
//        val mediaItem: MediaItem,
        var priority: PriorityLevel = PriorityLevel.IMMEDIATE
    )

    // 正在下载或排队等待的网络音频文件
//    val networkMediaFileList: MutableList<NetworkInfo> = mutableListOf()

    fun MutableList<MediaItem>.removeNetworkByPath(path: String) {
        removeAll { it.source == AudioSourceType.NETWORK && it.filePath == path }
    }

    fun List<MediaItem>.findTTSByPath(path: String): MediaItem? =
        find { it.source == AudioSourceType.TTS && it.filePath == path }

    fun List<MediaItem>.findAllTTSByPath(path: String): List<MediaItem> =
        filter { it.source == AudioSourceType.TTS && it.filePath == path }

    fun List<MediaItem>.findNetworkByPath(path: String): MediaItem? =
        find { it.source == AudioSourceType.NETWORK && it.filePath == path }

    fun List<MediaItem>.findFirstImmediateNetworkLoading(): MediaItem? =
        find { it.source == AudioSourceType.NETWORK && it.status == MediaItemStatus.LOADING && it.networkInfo.priority == PriorityLevel.IMMEDIATE }

    fun List<MediaItem>.findFirstPreloadNetworkLoading(): MediaItem? =
        find { it.source == AudioSourceType.NETWORK && it.status == MediaItemStatus.LOADING && it.networkInfo.priority == PriorityLevel.PRELOAD }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)   // TTS 合成可能较慢，读超时给长一点
        .build()

    private var call: Job? = null

    private var httpGetJob: Job? = null

    fun buildHttpGetMedia(mediaItem: MediaItem): String {

        if (mediaItem.text == null) return ""

        val baseUrl = utils.getHTTPGetVoiceURL()
        val urlKey = utils.getVoiceUrlKey(baseUrl)
        // 移除url前的名称部分
//        val url = baseUrl.replaceFirst(Regex("\\[.*?]"), "")
        val parseRequest = HTTPRequest().parseRequest(baseUrl)

        val voiceText = utils.getVoiceText(mediaItem.text)
        mediaItem.networkInfo.voiceText = voiceText
        val networkFileName = "${voiceText}.wav"
        val networkDir = File("${utils.appRootPath}/networkMedia/$urlKey/")
        if (!networkDir.exists()) {
            networkDir.mkdirs()
        }
        val networkFile = File(networkDir, networkFileName)


        // 本地缓存：相同urlKey且text相同的请求，直接返回
        if (networkFile.exists() && networkFile.length() > 0L) {
            mediaItem.status = MediaItemStatus.COMPLETED
            Log.d("L546", "本地缓存：$networkFile.path")
            return networkFile.path
        }

        val theMediaItem = mediaItemList.findNetworkByPath(networkFile.path)
        // 已在队列中加载，直接返回
        if (theMediaItem != null) {
//            theMediaItem.networkInfo.priority = priority
            return networkFile.path
        }

        // 发起请求
        mediaItem.filePath = networkFile.path

//        Log.d("L516", "${mediaItemList.last()}")
        Log.d("L517", "${mediaItemList}")
        // 依次发起请求
        if (httpGetJob == null) {
            httpGetJob = CoroutineScope(Dispatchers.IO.limitedParallelism(1)).launch {
                while (isActive) {

                    // 找到第一个正在加载的立即加载项
                    val loadingMediaItem = mediaItemList.findFirstImmediateNetworkLoading()
                        ?: mediaItemList.findFirstPreloadNetworkLoading()

                    if (loadingMediaItem == null) {
                        cancel()
                        httpGetJob = null
                        return@launch
                    }

//                    Log.d("L506", "httpGetJob")
                    val loadingMediaFile = File(loadingMediaItem.filePath)

                    val tempFile = File("${loadingMediaFile.path}.tmp")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    try {
                        val builder: Request.Builder
                        try {

                            // 构建URL
                            val url = parseRequest.url.replace(
                                "{{text}}",
                                loadingMediaItem.networkInfo.voiceText
                            )

                            val body = parseRequest.body?.replace(
                                "{{text}}",
                                loadingMediaItem.networkInfo.voiceText
                            )

                            builder = Request.Builder().url(url)

                            // 添加 headers
                            parseRequest.headers.forEach { (k, v) -> builder.addHeader(k, v) }

                            when (parseRequest.method) {
                                "GET" -> builder.get()
                                "POST" -> {
                                    val contentType =
                                        parseRequest.headers["Content-Type"]?.toMediaTypeOrNull()
                                            ?: "application/json".toMediaTypeOrNull()
                                    val body = (body ?: "").toRequestBody(contentType)
                                    builder.post(body)
                                }

                                else -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        utils.showMsg("不支持的方法")
                                    }
                                    continue
                                }
                            }

                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                utils.showMsg("GET URL格式错误")
                            }
                            loadingMediaItem.status = MediaItemStatus.FAILED
//                            cancel()
//                            httpGetJob = null
//                            return@launch
                            continue
                        }

                        // execute() 是同步阻塞的，必须放在 IO 线程（这里已经在 IO 协程里）
                        Log.e(
                            "HttpGetMedia",
                            "get start: ${loadingMediaItem.networkInfo.voiceText} ${loadingMediaItem.networkInfo.priority}"
                        )
                        val startTime = System.currentTimeMillis()
                        call = client.newCall(builder.build()).execute().use { resp ->
                            if (!resp.isSuccessful) {
                                val errorBody = resp.body.string()
                                Log.e(
                                    "HttpGetMedia",
                                    "HTTP ${resp.code} for ${loadingMediaItem.networkInfo.voiceText}"
                                )
                                Log.e("HttpGetMedia 请求失败：", errorBody)
                                withContext(Dispatchers.Main) {
                                    utils.showMsg("请求失败：$errorBody")
                                }
                                Log.d("L627", "${loadingMediaItem}")
                                loadingMediaItem.status = MediaItemStatus.FAILED
//                                cancel()
//                                httpGetJob = null
//                                return@launch
                                continue
                            }

                            val body = resp.body

                            body.byteStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                    output.flush()
                                    output.fd?.sync()   // ① 强制落盘
                                }
                            }

                            if (tempFile.length() == 0L) {
                                Log.e(
                                    "HttpGetMedia",
                                    "empty body for ${loadingMediaItem.networkInfo.voiceText}"
                                )
                                loadingMediaItem.status = MediaItemStatus.FAILED
//                                cancel()
//                                httpGetJob = null
//                                return@launch
                                continue
                            }

                            // 下载完成，移动到最终文件
                            tempFile.copyTo(loadingMediaFile, overwrite = true)
                            tempFile.delete()

                            mediaItemList.removeNetworkByPath(loadingMediaItem.filePath)

                            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                            Log.d(
                                "HttpGetMedia",
                                "get done: ${loadingMediaItem.networkInfo.voiceText}"
                            )
                            loadingMediaItem.status = MediaItemStatus.COMPLETED

                            Log.d(
                                "HttpGetMedia",
                                "耗时 $elapsedSeconds 秒，剩余 ${mediaItemList.size} 个"
                            )
                            CoroutineScope(Dispatchers.Main).launch {
                                utils.showMsg(
                                    "下载完成：${loadingMediaItem.networkInfo.voiceText}",
                                    true
                                )
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(
                            "HttpGetMedia",
                            "get failed ${loadingMediaItem.networkInfo.voiceText}",
                            e
                        )
                        loadingMediaItem.status = MediaItemStatus.FAILED
//                        cancel()
//                        httpGetJob = null
//                        return@launch
                        continue
                    }

                    delay(100L.milliseconds)

                }
            }
        }

        return networkFile.path
    }

    fun buildLocalMedia(media: String): String {

        val supportMediaFormatList = listOf("mp3", "wav", "ogg", "aac", "flac", "m4a", "pcm")

        for (format in supportMediaFormatList) {
            val filePathName =
                "${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${media}.${format}"

            if (File(filePathName).exists()) {
                return filePathName
            }

        }

        return ""
    }

    fun buildTTSMedia(text: String): String {

        if (!(utils.getIsUseTTS() && ttsReady)) {
            return ""
        }

        val ttsFileName = "${text}.wav"
        val ttsFile = File("$tempFilePath/tts/", ttsFileName)

        if (!ttsTextList.contains(text)) {
            Log.d("L716", "buildTTSMedia: $text")
            ttsFile.getParentFile()?.mkdirs()
            ttsFile.createNewFile()
            val params = Bundle().apply {
                putString(
                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsFileName
                )
            }
            tts.synthesizeToFile(
                text, params, ttsFile, ttsFile.path
            )
            ttsTextList.add(text)
        }

        return ttsFile.path


    }

    private fun getAnExps(): String {

        val stationType = when (anContext.stationCount) {
            0 -> "Starting"
            1 -> "Second"
            anContext.stationList.size - 1 -> "Terminal"
            else -> "Default"
        }
        val stationState = when (anContext.stationState) {
            StationStatus.ON_ARRIVE -> "Arrive"
            StationStatus.ON_NEXT -> "Next"
            StationStatus.ON_WILL_ARRIVE -> "WillArrive"
            else -> ""
        }

        return if (format == "") utils.getAnnouncementFormat(stationState, stationType)
        else format
    }

    fun hasTTSDone(filePath: String): Boolean {
//        Log.d("L481", "${ttsFileDoneList}, $filePath")
        return ttsFileDoneList.contains(filePath)
    }

    /**
     * 完成或失败返回true，加载中返回false
     */
    fun hasNetworkVoiceDone(filePath: String): Boolean {
        val file = File(filePath)
        if (file.exists() && file.length() > 0L) {
            return true
        }

//        mediaItemList.forEach { item ->
//            Log.d("L770", "${item.networkInfo.voiceText}, ${item.status}")
//        }

        val mediaItem = mediaItemList.findNetworkByPath(filePath)
        if (mediaItem == null) {
            return false
        }

        Log.d(
            "L782", "${mediaItem.networkInfo.voiceText} ${mediaItem.status}"
        )


        return mediaItemList.find { it.filePath == filePath && it.status != MediaItemStatus.LOADING } != null
    }

    fun preloadingNetworkMedia(sourceText: String) {
        toBuildHttpGetMedia(sourceText, PriorityLevel.PRELOAD)
    }

    fun clearPreloadingNetworkMediaFileList() {
        mediaItemList.removeAll {
            it.source == AudioSourceType.NETWORK &&
                    it.networkInfo.priority == PriorityLevel.PRELOAD
        }
    }

    fun clearImmediateNetworkMediaFileList() {
        mediaItemList.removeAll {
            it.source == AudioSourceType.NETWORK &&
                    it.networkInfo.priority == PriorityLevel.IMMEDIATE
        }
    }


    fun toBuildHttpGetMedia(
        text: String,
        priority: PriorityLevel = PriorityLevel.IMMEDIATE
    ): Boolean {
        if (!utils.getEnableHTTPGetVoiceItem()) {
            return false
        }

        val url = utils.getHTTPGetVoiceURL()

        if (url.isEmpty() || !url.contains("{{text}}")) {
            return false
        }

//        Log.d("L813", "toBuildHttpGetMedia: $url")

        val mediaItem = MediaItem(
            AudioSourceType.NETWORK, "", text, MediaItemStatus.LOADING
        )
        mediaItem.networkInfo.priority = priority
        val httpGetMediaPath = buildHttpGetMedia(mediaItem)
        mediaItem.filePath = httpGetMediaPath
        mediaItemList.add(
            mediaItem
        )
        return true
    }


}