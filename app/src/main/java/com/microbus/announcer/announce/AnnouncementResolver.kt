package com.microbus.announcer.announce

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.util.WavSilenceGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.collections.addAll
import kotlin.random.Random


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
        initTTS()
    }

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

            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String) {
                ttsFileDoneList.add(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
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

    }

    fun resolveAndBuild(
        format: String = "",
        anContext: AnnouncementContext
    ): ArrayList<String> {

        this.format = format
        this.anContext = anContext

        val mediaList = resolve()
        val filePathList = build(mediaList)

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
                            anContext.speedKmh.toInt().toString(),
                            "cn/number/"
                        )
                        mediaList.addAll(lineMediaList)
                        continue
                    }

                    "<line>" -> {
                        // 寻找lineName音频
                        val lineMediaList = resolveLineNameKey(item)
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
                val stationList =
                    stationDatabaseHelper.queryById(
                        (item.substring(
                            5,
                            item.length - 1
                        )).toInt()
                    )
                if (stationList.isNotEmpty())
                    stationList.first()
                else Station(
                    id = Int.MAX_VALUE,
                    cnName = "未知站点",
                    enName = "unknown"
                )
            }

            else -> Station()
        }

        val lang = if (item.substring(1, 3) == "ms") {
            item.substring(3, 5)
        } else
            item.drop(3).dropLast(1)

        val stationName = when (lang) {
            "cn" -> station.cnName
            "en" -> station.enName
            else -> utils.getStationNameFromCn(
                station.cnName,
                lang
            )
        }

        return "/${lang}/station/${stationName}"

    }

    fun resolveCommonMedia(item: String): String {

        for (lang in anContext.announcementLangList) {
            val file =
                File("${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${lang}/common")

            val fileList = file.walk()
                .filter { it.isFile && it.nameWithoutExtension == item }
                .toList()

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
            if ("\\d+".toRegex().findAll(result.value).toList().isNotEmpty())
                voiceList.addAll(utils.intOrLetterToCnReading(result.value, "/cn/number/"))
            else
                voiceList.addAll(utils.intOrLetterToCnReading(result.value, "/en/letter/"))

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
                currentTime.minute.toString(),
                "/cn/number/",
                true
            )
        )
        voiceList.add("/cn/common/分")

        return voiceList
    }

    fun resolveLineNameKey(item: String): ArrayList<String> {

        for (lang in anContext.announcementLangList) {
            val file =
                File("${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${lang}/line")
            val fileList = file.walk()
                .filter { it.isFile && it.nameWithoutExtension == anContext.lineName }
                .toList()
            if (fileList.isNotEmpty()) {
                return arrayListOf("/${lang}/line/" + anContext.lineName)
            }
        }

        return getNumOrLetterVoiceList(anContext.lineName)
    }

    fun resolveBlankKey(item: String): String {
        val matchResult = Regex("<blank(\\d+)>").find(item)
        val blankDurationMs =
            matchResult?.groupValues?.get(1)?.toIntOrNull() ?: return ""
        return "/blank/${blankDurationMs}.pcm"
    }

    fun build(mediaList: ArrayList<String>): ArrayList<String> {

        if (mediaList.isEmpty()) {
            return arrayListOf()
        }

        // 新建缓存文件目录
        File(tempFilePath).mkdirs()

        ttsFileDoneList = arrayListOf()
        ttsTextList = arrayListOf()

        // 清空TTS生成目录
        if (utils.getIsUseTTS() && ttsReady) {
            // 清空TTS
            File("$tempFilePath/tts").walkTopDown().forEach {
                it.delete()
            }
            File("$tempFilePath/tts").mkdirs()
        }

        // 查找本地音频/合成TTS音频
        val filePathList = ArrayList<String>()

        for (media in mediaList) {

            Log.d("L3770", media)

            // 1. 空白占位音频
            if (media.startsWith("/blank")) {
                val blankFilePath = buildBlankMedia(media)
                filePathList.add(blankFilePath)
                continue
            }

            // 2. 本地音频
            val localFilePath = buildLocalMedia(media)
            if (localFilePath != "") {
                filePathList.add(localFilePath)
                continue
            }

            // 3. 不存在本地音频
            // 启用TTS，合成TTS音频
            val ttsFilePath = buildTTSMedia(media)
            if (ttsFilePath != "") {
                filePathList.add(ttsFilePath)
                continue
            }

        }

        return filePathList
    }

    fun buildBlankMedia(media: String): String {

        val matchResult = Regex("/blank/(\\d+).pcm").find(media)
        val blankDurationMs =
            matchResult?.groupValues?.get(1)?.toIntOrNull() ?: return ""

        val outputPath = "$tempFilePath/blank/${blankDurationMs}.pcm"

        if (!File(outputPath).exists()) {
            val wavSilenceGeneratorRes =
                WavSilenceGenerator.generateSilenceWav(
                    blankDurationMs.toLong(),
                    outputPath
                )
            if (!wavSilenceGeneratorRes) return ""
        }

        return outputPath
    }

    fun buildLocalMedia(media: String): String {

        val supportMediaFormatList =
            listOf("mp3", "wav", "ogg", "aac", "flac", "m4a", "pcm")

        for (format in supportMediaFormatList) {
            val filePathName =
                "${utils.appRootPath}/Media/${utils.getAnnouncementLibrary()}/${media}.${format}"

            if (File(filePathName).exists()) {
                return filePathName
            }

        }

        return ""
    }

    fun buildTTSMedia(media: String): String {

        if (!(utils.getIsUseTTS() && ttsReady)) {
            return ""
        }

        val text = media.split('/').last()
        val ttsFileName = "${text}.wav"
        val ttsFile = File("$tempFilePath/tts/", ttsFileName)

        if (!ttsTextList.contains(text)) {
            ttsFile.getParentFile()?.mkdirs()
            ttsFile.createNewFile()
            val params = Bundle().apply {
                putString(
                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    ttsFileName
                )
            }
            tts.synthesizeToFile(
                text,
                params,
                ttsFile,
                ttsFile.path
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
        Log.d("L481", "${ttsFileDoneList.contains(filePath)}")
        return ttsFileDoneList.contains(filePath)
    }

}