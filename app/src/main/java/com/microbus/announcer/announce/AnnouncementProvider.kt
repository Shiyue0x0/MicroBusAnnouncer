package com.microbus.announcer.announce

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.microbus.announcer.HTTPRequest
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.AudioSourceType
import com.microbus.announcer.bean.AnMediaItem
import com.microbus.announcer.bean.MediaItemStatus
import com.microbus.announcer.bean.NetworkInfo
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


class AnnouncementProvider private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private var utils = Utils(appContext)
    private var stationDatabaseHelper = StationDatabaseHelper.getInstance(appContext)
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

    companion object {
        @Volatile
        private var INSTANCE: AnnouncementProvider? = null

        fun getInstance(context: Context): AnnouncementProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnnouncementProvider(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }



    private fun initTTS() {
        tts = TextToSpeech(appContext) { status ->
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
                playingAnMediaItemList.findAllTTSByPath(utteranceId).forEach {
                    it.status = MediaItemStatus.COMPLETED
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                playingAnMediaItemList.findAllTTSByPath(utteranceId).forEach {
                    it.status = MediaItemStatus.FAILED
                }
                CoroutineScope(Dispatchers.Main).launch {
                    utils.showMsg("TTS合成异常，请检查系统设置")
                }
            }

        })

        tempFilePath = appContext.getExternalFilesDir("")?.path ?: ""

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
    ): MutableList<AnMediaItem> {

        this.format = format
        this.anContext = anContext

        val textWithPathList = resolve()
        val filePathList = build(textWithPathList)

        Log.d("L94", filePathList.toString())

        return filePathList
    }

    fun resolve(): java.util.ArrayList<String> {

        if (anContext.stationList.isEmpty()) {
            return arrayListOf()
        }

        val textWithPathList = ArrayList<String>()

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
                            textWithPathList.addAll(getNumOrLetterVoiceList(str))
                        }
                        continue
                    }

                    "<time>" -> {
                        textWithPathList.addAll(getTimeVoiceList())
                        continue
                    }

                    "<speed>" -> {
                        val lineMediaList = utils.intOrLetterToCnReading(
                            anContext.speedKmh.toInt().toString(), "cn/number/"
                        )
                        textWithPathList.addAll(lineMediaList)
                        continue
                    }

                    "<line>" -> {
                        // 寻找lineName音频
                        val lineMediaList = resolveLineNameKey()
                        textWithPathList.addAll(lineMediaList)
                        continue
                    }

                    else -> {

                        // blank
                        if (item.startsWith("<blank") && item.endsWith(">")) {
                            val blankMedia = resolveBlankKey(item)
                            if (blankMedia != "") {
                                textWithPathList.add(blankMedia)
                            }
                            continue
                        }

                        // station
                        if (item.substring(1, 3) in listOf("ns", "ss", "ts", "ms")) {
                            textWithPathList.add(resolveStationKey(item))
                            continue
                        }

                    }
                }
            }

            // common（常规）语句
            val commonMedia = resolveCommonMedia(item)
            textWithPathList.add(commonMedia)

        }

        return textWithPathList
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
                station.cnName, lang, station
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
        // 预编译正则，避免重复编译
        val splitRegex = Regex("([a-zA-Z]+|\\d+)")
        val numberRegex = Regex("\\d+")

        val voiceList = ArrayList<String>()

        var lastEnd = 0
        splitRegex.findAll(str).forEach { match ->
            // 1. 匹配片段之前的"非数字/字母"文本 → 走 common 目录原文本
            if (match.range.first > lastEnd) {
                voiceList.add("/common/${str.substring(lastEnd, match.range.first)}")
            }

            // 2. 匹配到的数字/字母片段
            val value = match.value
            if (numberRegex.matches(value)) {
                voiceList.addAll(utils.intOrLetterToCnReading(value, "/cn/number/"))
            } else {
                voiceList.addAll(utils.intOrLetterToCnReading(value, "/en/letter/"))
            }

            lastEnd = match.range.last + 1
        }

        // 3. 末尾剩余的"非数字/字母"文本
        if (lastEnd < str.length) {
            voiceList.add("/common/${str.substring(lastEnd)}")
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

        // 从line目录查找音频
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

    enum class NetworkMediaLoadingType {
        /** 预加载 */
        PRELOAD,

        /** 正在播放（立即下载） */
        PLAYING,

    }

    var playingAnMediaItemList = CopyOnWriteArrayList<AnMediaItem>()
    var preloadAnMediaItemList = CopyOnWriteArrayList<AnMediaItem>()

    private var playingNetworkDownloadJob: Job? = null
    private var preloadNetworkDownloadJob: Job? = null

    fun build(textWithPathList: ArrayList<String>): MutableList<AnMediaItem> {

        if (textWithPathList.isEmpty()) {
            return mutableListOf()
        }

        playingAnMediaItemList = CopyOnWriteArrayList<AnMediaItem>()

        for (textWithPath in textWithPathList) {

            Log.d("L3770", textWithPath)

            // 1. 空白占位音频
            if (textWithPath.startsWith("/blank")) {
                val blankFilePath = buildBlankMedia(textWithPath)
                playingAnMediaItemList.add(
                    AnMediaItem(
                        AudioSourceType.BLANK, blankFilePath, null, MediaItemStatus.COMPLETED
                    )
                )
                continue
            }

            val text = textWithPath.split('/').last()


            // 2. 本地音频
            val localFilePath = buildLocalMedia(textWithPath)
            if (localFilePath != "") {
                playingAnMediaItemList.add(
                    AnMediaItem(
                        AudioSourceType.LOCAL, localFilePath, text, MediaItemStatus.COMPLETED
                    )
                )
                continue
            }

            // 3. 通过网络API获取音频
            val networkMediaBuildResult = buildNetworkMedia(textWithPath)
            if (networkMediaBuildResult.code != NetworkMediaBuildResultCode.FAIL) {
                playingAnMediaItemList.add(
                    networkMediaBuildResult.anMediaItem
                )
                if (networkMediaBuildResult.code != NetworkMediaBuildResultCode.LOCALCACHE) {
                    beginDownloadNetworkMedia(NetworkMediaLoadingType.PLAYING)
                }
                continue
            }

            // 4. TTS合成音频
            val ttsText = utils.getVoiceText(text)
            val ttsFilePath = buildTTSMedia(ttsText)
            if (ttsFilePath != "") {
                val anMediaItem = AnMediaItem(
                    AudioSourceType.TTS, ttsFilePath, ttsText, MediaItemStatus.LOADING
                )
                if (ttsFileDoneList.contains(ttsFilePath)) {
                    anMediaItem.status = MediaItemStatus.COMPLETED
                }
                playingAnMediaItemList.add(anMediaItem)
                continue
            }

        }

        return playingAnMediaItemList
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


    // 正在下载或排队等待的网络音频文件
//    val networkMediaFileList: MutableList<NetworkInfo> = mutableListOf()

    fun MutableList<AnMediaItem>.removeNetworkByPath(path: String) {
        removeAll { it.source == AudioSourceType.NETWORK && it.filePath == path }
    }

    fun List<AnMediaItem>.findAllTTSByPath(path: String): List<AnMediaItem> =
        filter { it.source == AudioSourceType.TTS && it.filePath == path }

    fun List<AnMediaItem>.findNetworkByPath(path: String): AnMediaItem? =
        find { it.source == AudioSourceType.NETWORK && it.filePath == path }

    fun List<AnMediaItem>.findFirstNetworkLoading(): AnMediaItem? =
        find { it.source == AudioSourceType.NETWORK && it.status == MediaItemStatus.LOADING }


    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)   // TTS 合成可能较慢，读超时给长一点
        .build()

    private var call: Job? = null

    enum class NetworkMediaBuildResultCode {

        /** 本地缓存 */
        LOCALCACHE,

        /** 已构建 */
        HASMEDIA,

        /** 新增媒体 */
        NEWMEDIA,

        /** 失败 */
        FAIL

    }

    data class NetworkMediaBuildResult(
        var code: NetworkMediaBuildResultCode,
        var anMediaItem: AnMediaItem?
    )

    fun buildNetworkMedia(
        textWithPath: String
    ): NetworkMediaBuildResult {

        if (textWithPath.isBlank()) {
            return NetworkMediaBuildResult(
                code = NetworkMediaBuildResultCode.FAIL,
                anMediaItem = null
            )
        }

        if (!utils.getEnableHTTPGetVoiceItem()) {
            return NetworkMediaBuildResult(
                code = NetworkMediaBuildResultCode.FAIL,
                anMediaItem = null
            )
        }

        val baseUrl = utils.getHTTPGetVoiceURL()
        if (baseUrl.isEmpty() || !baseUrl.contains("{{text}}")) {
            return NetworkMediaBuildResult(
                code = NetworkMediaBuildResultCode.FAIL,
                anMediaItem = null
            )
        }

        val urlKey = utils.getVoiceUrlKey(baseUrl)
        val basePath = textWithPath.substringBeforeLast("/")
        val text = textWithPath.substringAfterLast("/")
        val voiceText = utils.getVoiceText(text)

        val networkDir = File("${utils.appRootPath}/networkMedia/$urlKey/$basePath")
        val networkFileName = "${voiceText}.wav"

        if (!networkDir.exists()) {
            networkDir.mkdirs()
        }
        val networkFile = File(networkDir, networkFileName)

        val anMediaItem = AnMediaItem(
            AudioSourceType.NETWORK,
            networkFile.path,
            voiceText,
            MediaItemStatus.LOADING,
            NetworkInfo(
                voiceText = voiceText,
                downloadBeginTime = null,
                downloadDoneTime = null
            )
        )

        // 本地缓存：相同urlKey且text相同的请求，直接返回
        if (networkFile.exists() && networkFile.length() > 0L) {
            Log.d("L555", "本地缓存：${networkFile.path}")
            anMediaItem.status = MediaItemStatus.COMPLETED
            return NetworkMediaBuildResult(
                code = NetworkMediaBuildResultCode.LOCALCACHE,
                anMediaItem = anMediaItem
            )
        }

        // 已在队列中加载，直接返回
        val samePathMediaItem = playingAnMediaItemList.findNetworkByPath(networkFile.path)
        if (samePathMediaItem != null) {
            Log.d("L562", "正在下载：${samePathMediaItem.filePath}")
            return NetworkMediaBuildResult(
                code = NetworkMediaBuildResultCode.HASMEDIA,
                anMediaItem = samePathMediaItem
            )
        }

//        Log.d("L516", "${mediaItemList.last()}")
        Log.d("L568", "新增对象：${anMediaItem.filePath}")


        return NetworkMediaBuildResult(
            code = NetworkMediaBuildResultCode.NEWMEDIA,
            anMediaItem = anMediaItem
        )
    }

    private fun beginDownloadNetworkMedia(loadingType: NetworkMediaLoadingType) {

        var job = when (loadingType) {
            NetworkMediaLoadingType.PLAYING -> {
                playingNetworkDownloadJob
            }

            NetworkMediaLoadingType.PRELOAD -> {
                preloadNetworkDownloadJob
            }
        }

        val mediaItemList = getMediaItemList(loadingType)

        if (job == null) {
            Log.d("download", "新建协程(${loadingType})")
            job = CoroutineScope(Dispatchers.IO.limitedParallelism(1)).launch {
                // 依次发起请求
                while (isActive) {

                    // 找到第一个正在加载的立即加载项
                    val loadingMediaItem = mediaItemList.findFirstNetworkLoading()

                    if (loadingMediaItem == null) {
                        Log.d("download", "无下载项，关闭协程(${loadingType})")
                        cancel()
                        updNetworkDownload(loadingType, null)

                        if (loadingType == NetworkMediaLoadingType.PRELOAD) {
                            CoroutineScope(Dispatchers.Main).launch {
                                utils.showMsg("已完成预加载")
                            }
                        }

                        return@launch
                    }

//                    Log.d("L506", "httpGetJob")
                    val loadingMediaFile = File(loadingMediaItem.filePath)
                    if (loadingMediaFile.exists()) {
                        loadingMediaItem.status = MediaItemStatus.COMPLETED
                        continue
                    }

                    val tempFile = File("${loadingMediaFile.path}.tmp")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    try {
                        val builder: Request.Builder
                        try {

                            val baseUrl = utils.getHTTPGetVoiceURL()
                            val parseRequest = HTTPRequest().parseRequest(baseUrl)


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
                            "get start: ${loadingMediaItem.networkInfo.voiceText}"
                        )
                        val startTime = System.currentTimeMillis()
                        loadingMediaItem.networkInfo.downloadBeginTime = startTime
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
                                Log.d("L627", "$loadingMediaItem")
                                loadingMediaItem.status = MediaItemStatus.FAILED
                                loadingMediaItem.networkInfo.downloadDoneTime =
                                    System.currentTimeMillis()
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
                                loadingMediaItem.networkInfo.downloadDoneTime =
                                    System.currentTimeMillis()
//                                cancel()
//                                httpGetJob = null
//                                return@launch
                                continue
                            }

                            // 下载完成，移动到最终文件
                            tempFile.copyTo(loadingMediaFile, overwrite = true)
                            tempFile.delete()

                            mediaItemList.removeNetworkByPath(loadingMediaItem.filePath)

                            loadingMediaItem.status = MediaItemStatus.COMPLETED
                            loadingMediaItem.networkInfo.downloadDoneTime =
                                System.currentTimeMillis()

                            val elapsedSeconds =
                                (System.currentTimeMillis() - startTime) / 1000.0
                            Log.d(
                                "HttpGetMedia",
                                "get done: ${loadingMediaItem.networkInfo.voiceText}"
                            )

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
                        loadingMediaItem.networkInfo.downloadDoneTime = System.currentTimeMillis()

                        CoroutineScope(Dispatchers.Main).launch {
                            utils.showMsg(
                                "下载失败：${loadingMediaItem.networkInfo.voiceText}",
                                true
                            )
                        }

//                        cancel()
//                        httpGetJob = null
//                        return@launch
                        continue
                    }

                    delay(100L.milliseconds)

                }
            }
            updNetworkDownload(loadingType, job)
        }
    }

    private fun updNetworkDownload(
        loadingType: NetworkMediaLoadingType,
        job: Job?
    ) {
        when (loadingType) {
            NetworkMediaLoadingType.PLAYING -> playingNetworkDownloadJob = job
            NetworkMediaLoadingType.PRELOAD -> preloadNetworkDownloadJob = job
        }
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

        val mediaItem = playingAnMediaItemList.findNetworkByPath(filePath) ?: return false

        Log.d(
            "L782", "${mediaItem.networkInfo.voiceText} ${mediaItem.status}"
        )


        return playingAnMediaItemList.find { it.filePath == filePath && it.status != MediaItemStatus.LOADING } != null
    }

    fun preloadingNetworkMedia(textWithPath: String) {
        val networkMediaBuildResult = buildNetworkMedia(textWithPath)
        if (networkMediaBuildResult.code == NetworkMediaBuildResultCode.NEWMEDIA) {
            preloadAnMediaItemList.add(networkMediaBuildResult.anMediaItem)
            beginDownloadNetworkMedia(NetworkMediaLoadingType.PRELOAD)
        }
    }


//    fun clearPreloadingNetworkMediaFileList() {
//        playingMediaItemList.removeAll {
//            it.source == AudioSourceType.NETWORK &&
//                    it.networkInfo.priority == PriorityLevel.PRELOAD
//        }
//    }
//
//    fun clearImmediateNetworkMediaFileList() {
//        playingMediaItemList.removeAll {
//            it.source == AudioSourceType.NETWORK &&
//                    it.networkInfo.priority == PriorityLevel.IMMEDIATE
//        }
//    }

    fun getNetworkLoadingListTotal(loadingType: NetworkMediaLoadingType): Int {

        val mediaItemList = getMediaItemList(loadingType)

        return mediaItemList
            .filter { it.status == MediaItemStatus.LOADING }
            .size
    }

    fun getNetworkLoadingInfo(loadingType: NetworkMediaLoadingType): String {

        val mediaItemList = getMediaItemList(loadingType)
        val loadingMediaItem = mediaItemList.findFirstNetworkLoading() ?: return ""

        var info = loadingMediaItem.networkInfo.voiceText

        if (loadingMediaItem.networkInfo.downloadBeginTime != null) {
            val elapsedSeconds =
                (System.currentTimeMillis() - loadingMediaItem.networkInfo.downloadBeginTime!!) / 1000.0
            info += " ${elapsedSeconds}s"
        }

        return info
    }

    fun getMediaItemList(loadingType: NetworkMediaLoadingType): MutableList<AnMediaItem> {
        return when (loadingType) {
            NetworkMediaLoadingType.PLAYING -> {
                playingAnMediaItemList
            }

            NetworkMediaLoadingType.PRELOAD -> {
                preloadAnMediaItemList
            }
        }
    }

    /**
     * 获取公告库中所有语言 common 目录下的音频文件名（不含后缀），
     * 作为「普通语句」的搜索提示关键词。去重后返回。
     */
    fun getCommonMediaKeywords(): List<String> {
        val keywords = LinkedHashSet<String>()
        val library = utils.getAnnouncementLibrary()
        for (lang in utils.getLangList()) {
            val commonDir = File("${utils.appRootPath}/Media/$library/$lang/common")
            if (!commonDir.exists()) continue
            commonDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val name = file.nameWithoutExtension
                    if (name.isNotEmpty()) keywords.add(name)
                }
            }
        }
        return keywords.toList()
    }

}