package com.microbus.announcer.ui.compose

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.preference.PreferenceManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.databinding.ViewEsHeaderBinding
import com.microbus.announcer.model.StationStatus
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun EsHeaderCompose(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,           // 新增：动画开关
    onHeaderClick: (() -> Unit)? = null,
    lineName: String,
    currentSpeedKmH: Double,
    currentLineStation: Station,
    currentLineStationList: ArrayList<Station>,
    currentLineStationCount: Int,
    currentLineStationState: Int
) {

    val context = LocalContext.current

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val utils = Utils(context)

    @SuppressLint("MutableCollectionMutableState")
    val (esList, setEsList) = remember { mutableStateOf(utils.getEsList(utils.getEsText())) }

    val (esPlayIndex, setEsPlayIndex) = remember { mutableIntStateOf(-1) }

    val (esSpeed, setEsSpeed) = remember {
        mutableIntStateOf(utils.getEsSpeed())
    }

    val (minShowTimeMs, setMinShowTimeMs) = remember {mutableIntStateOf(Int.MAX_VALUE)}

    val (esFinishPositionOfLastWord, setEsFinishPositionOfLastWord) = remember {
        mutableFloatStateOf(utils.getEsFinishPositionOfLastWord())
    }

    val (isOpenLeftEs, setIsOpenLeftEs) = remember {
        mutableStateOf(utils.getIsOpenLeftEs())
    }

    val (isMidLeftEs, setIsMidLeftEs) = remember {
        mutableStateOf(utils.getIsOpenMidEs())
    }

    val (leftText, setLeftText) = remember { mutableStateOf("") }
    val (middleText, setMiddleText) = remember { mutableStateOf("") }
    val (rightText, setRightText) = remember { mutableStateOf("") }

    val (isLeftShowFinish, setIsLeftShowFinish) = remember { mutableStateOf(false) }
    val (isMiddleShowFinish, setIsMiddleShowFinish) = remember { mutableStateOf(false) }
    val (isRightShowFinish, setIsRightShowFinish) = remember { mutableStateOf(false) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "esSpeed" -> setEsSpeed(prefs.getInt(key, 100))
                "esFinishPositionOfLastWord" -> setEsFinishPositionOfLastWord(
                    prefs.getFloat(
                        key,
                        0.5F
                    )
                )

                "isOpenLeftEs" -> setIsOpenLeftEs(prefs.getBoolean(key, true))
                "isMidLeftEs" -> setIsMidLeftEs(prefs.getBoolean(key, true))
                "esList" -> setEsList(utils.getEsList(utils.getEsText()))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }



    @SuppressLint("LocalContextGetResourceValueCall")
    fun getValueMapValue(key: String): String {
        return when (key) {

            // 站点占位符
            "<next>" -> utils.getEsNextWord()
            "<will>" -> utils.getEsWillArriveWord()
            "<arrive>" -> utils.getEsArriveWord()

            // 其他占位符
            "<line>" -> lineName

            "<year>" -> LocalDate.now().year.toString()
            "<years>" -> (LocalDate.now().year % 100).toString()
            "<month>" -> LocalDate.now().monthValue.toString()
            "<date>" -> LocalDate.now().dayOfMonth.toString()

            "<hour>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().hour)
            "<minute>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().minute)
            "<second>" -> String.format(Locale.CHINA, "%02d", LocalTime.now().second)

            "<time>" ->
                String.format(Locale.CHINA, "%02d", LocalTime.now().hour) + ":" +
                        String.format(Locale.CHINA, "%02d", LocalTime.now().minute)

            "<speed>" ->
                if (currentSpeedKmH >= 0) String.format(
                    Locale.CHINA,
                    "%.1f",
                    currentSpeedKmH
                ) else "-"

            else -> {
                if (key.startsWith("<blank") && key.endsWith(">")) {
                    ""
                } else {
                    val station = when (key.substring(1, 3)) {
                        "ns" -> currentLineStation

                        "ss" -> if (currentLineStationList.isEmpty())
                            Station(
                                cnName = context.getString(R.string.starting_station),
                                enName = context.getString(R.string.starting_station)
                            )
                        else currentLineStationList.first()

                        "ts" -> if (currentLineStationList.isEmpty())
                            Station(
                                cnName = context.getString(R.string.terminal),
                                enName = context.getString(R.string.terminal)
                            )
                        else currentLineStationList.last()

                        else -> currentLineStation
                    }
                    val lang = if (key.substring(1, 3) == "ms") {
                        key.substring(3, 5)
                    } else
                        key.drop(3).dropLast(1)
                    when (lang) {
                        "cn" -> station.cnName
                        "en" -> station.enName
                        else -> utils.getStationNameFromCn(station.cnName, lang)
                    }
                }

            }
        }
    }

    fun refreshEsOnlyText(isUseSet: Boolean = false) {

//        Log.d(tag, "refreshEsOnlyText S")

        var leftText: String
        var rightText: String

        @SuppressLint("LocalContextGetResourceValueCall")
        if (esPlayIndex >= 0 && esPlayIndex < esList.size) {
            leftText = esList[esPlayIndex].leftText
            rightText = esList[esPlayIndex].rightText
        } else {
            leftText = context.getString(R.string.main_staring_station_name)
            rightText = context.getString(R.string.main_terminal_name)
        }

        if (isMiddleShowFinish) {
//            binding.headerMiddleNew.showText(lineName)
            setMiddleText(lineName)
        }

        for (keyword in utils.getDefaultKeywordList()) {
            leftText = leftText.replace(keyword, getValueMapValue(keyword), true)
            rightText = rightText.replace(keyword, getValueMapValue(keyword), true)
        }

        if (!utils.getIsOpenLeftEs()) {
            rightText = "$leftText $rightText"
            leftText = ""
        }

//        if (isUseSet) {
//            if (binding.headerLeftNew.getText() != leftText)
//                binding.headerLeftNew.setText(leftText)
//            if (binding.headerRightNew.getText() != rightText)
//                binding.headerRightNew.setText(rightText)
//        } else {
//            binding.headerLeftNew.showText(leftText)
//            binding.headerRightNew.showText(rightText)
//        }


//        Log.d(tag, "refreshEsOnlyText E")

        // TODO
        setLeftText(leftText)
        setRightText(rightText)


    }

    fun esPlayNext() {
        if (esPlayIndex < esList.size - 1) {
            setEsPlayIndex(esPlayIndex + 1)
        } else {
            setEsList(utils.getEsList(utils.getEsText()))
            setEsPlayIndex(
                if (esList.isNotEmpty())
                    0
                else
                    -1
            )
        }
    }

    fun getStationStateTypeMap(): HashMap<String, Int> {
        val typeMap = HashMap<String, Int>()
        typeMap["N"] = StationStatus.ON_NEXT
        typeMap["W"] = StationStatus.ON_WILL_ARRIVE
        typeMap["A"] = StationStatus.ON_ARRIVE
        return typeMap
    }

    fun getStationPositionTypeMap(): HashMap<String, Int> {
        val typeMap = HashMap<String, Int>()
        typeMap["S"] = 0
        typeMap["C"] = currentLineStationCount
        typeMap["T"] = currentLineStationList.size - 1
        return typeMap
    }

    fun refreshEs(toStation: Boolean = false, toStaringAndTerminal: Boolean = false) {


        if (esPlayIndex == -1 && esList.isNotEmpty()) {
            setEsPlayIndex(0)
        }


        if (esList.isNotEmpty()) {
            // 切换到首末站显示
            if (toStaringAndTerminal) {
                var frontDefaultItemIndex = -1
                var hasB = false
                for ((i, element) in esList.withIndex()) {
                    // 寻找非特定状态显示内容
                    if (utils.extractNWA(
                            Regex("[NWASCT]"),
                            element.type
                        ) == "" && frontDefaultItemIndex == -1
                    ) {
                        frontDefaultItemIndex = i
                    }
                    // 寻找首末站内容
                    if (element.type.contains("B")) {
                        setEsPlayIndex(i)
                        hasB = true
                        break
                    }
                }
                if (!hasB) {
                    setEsPlayIndex(
                        if (frontDefaultItemIndex >= 0) {
                            frontDefaultItemIndex
                        } else {
                            -1
                        }
                    )
                }
            }
            // 仅某状态显示，或切换到当前状态显示
            else if (esList[esPlayIndex].type.contains(Regex("[NWASCT]")) || toStation) {

                var hasMatchCurrentState = false
                var hasMatchCurrentPos = false
                var frontDefaultItemIndex = -1

                val start = if (toStation)
                    0
                else
                    esPlayIndex

                for (i in start until esList.size) {

                    // 寻找非特定状态显示内容（从之后的内容）
                    if (utils.extractNWA(
                            Regex("[NWASCT]"),
                            esList[i].type
                        ) == "" && frontDefaultItemIndex == -1
                    ) {
                        frontDefaultItemIndex = i
                    }

                    // 寻找当前运行站点状态及位置对应内容
                    val currentMatchType = utils.extractNWA(Regex("[NWA]"), esList[i].type)
                    val currentPosType = utils.extractNWA(Regex("[SCT]"), esList[i].type)

                    // 状态及位置类型都有
                    if (currentMatchType != "" && currentPosType != "") {
                        if (getStationStateTypeMap()[currentMatchType] == currentLineStationState &&
                            getStationPositionTypeMap()[currentPosType] == currentLineStationCount
                        ) {
                            // C：即不是`起点站`也不是`终点站`
                            if (currentPosType == "C" &&
                                (currentLineStationCount == 0 || currentLineStationCount == currentLineStationList.size - 1)
                            ) {
                                continue
                            }
                            setEsPlayIndex(i)
                            hasMatchCurrentState = true
                            hasMatchCurrentPos = true
                            break
                        }
                        // 只有状态类型
                    } else if (currentMatchType != "") {
                        if (getStationStateTypeMap()[currentMatchType] == currentLineStationState) {
                            setEsPlayIndex(i)
                            hasMatchCurrentState = true
                            break
                        }
                        // 只有位置类型
                    } else if (currentPosType != "") {
                        // C：即不是`起点站`也不是`终点站`
                        if (currentPosType == "C" &&
                            (currentLineStationCount == 0 || currentLineStationCount == currentLineStationList.size - 1)
                        ) {
                            continue
                        }
                        if (getStationPositionTypeMap()[currentPosType] == currentLineStationCount) {
                            setEsPlayIndex(i)
                            hasMatchCurrentPos = true
                            break
                        }
                    }
                }
                if (!hasMatchCurrentState && !hasMatchCurrentPos) {
                    if (frontDefaultItemIndex >= 0) {
                        setEsPlayIndex(frontDefaultItemIndex)
                    } else {
                        var resIndex = -1
                        for ((i, element) in esList.withIndex()) {
                            // 寻找非特定状态显示内容（从所有的内容）
                            if (utils.extractNWA(Regex("[NWASCT]"), element.type) == ""
                            ) {
                                resIndex = i
                                break
                            }
                        }
                        setEsPlayIndex(resIndex)
                    }
                }
            }


        }

        val minTimeS =
            if (esPlayIndex >= 0 && esPlayIndex < esList.size) esList[esPlayIndex].minTimeS else 5
//        binding.headerLeftNew.minShowTimeMs = minTimeS * 1000
//        binding.headerRightNew.minShowTimeMs = minTimeS * 1000
        setMinShowTimeMs(minTimeS * 1000)
        refreshEsOnlyText()

    }

    var isRunning by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        Log.d("L80", "onResume")
        isRunning = true


        onPauseOrDispose {
            Log.d("L80", "onPause")
            isRunning = false

        }
    }

    var isRefreshing = false
    var esRefreshCount = 0

    LaunchedEffect(isRunning) {

        while (isRunning) {

            Log.d("L80", "repeat")
            if (esPlayIndex >= 0 && esPlayIndex < esList.size &&
                esList[esPlayIndex].type.contains("R") && esRefreshCount % 10 == 0
            ) {
                refreshEsOnlyText(true)
            }

            val isLeftFinish =
                isLeftShowFinish || !utils.getIsOpenLeftEs()

            val isRightFinish = isRightShowFinish

            Log.d("L431", "${isRefreshing} ${isLeftFinish} ${isRightFinish}")

            if (!isRefreshing && isLeftFinish && isRightFinish) {
                isRefreshing = true
                esPlayNext()
                refreshEs()
                isRefreshing = false
            }
            if (isMiddleShowFinish) {
//            binding.headerMiddleNew.showText(lineName)
                setMiddleText(lineName)
            }

            esRefreshCount++


            delay(100L.milliseconds)
            // 每 100ms 执行一次的任务
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val binding = ViewEsHeaderBinding.inflate(
                LayoutInflater.from(context), null, false
            )
            binding.root.tag = binding

            binding.headerLeftNew.minShowTimeMs = 500
            binding.headerRightNew.minShowTimeMs = 500

            // 监听IsShowFinish
            binding.headerLeftNew.setOnShowFinishChangedListener { finish ->
                setIsLeftShowFinish(finish)
                Log.d("L466", "${leftText} ${finish}")
            }
//            binding.headerMiddleNew.setOnShowFinishChangedListener { finish ->
//                setIsMiddleShowFinish(finish)
//                Log.d("L466", "${middleText} ${finish}")
//            }
            binding.headerRightNew.setOnShowFinishChangedListener { finish ->
                setIsRightShowFinish(finish)
                Log.d("L466", "${rightText} ${finish}")
            }

            binding.root
        },
        update = { view ->
            val binding = view.tag as? ViewEsHeaderBinding ?: return@AndroidView

            Log.d("L32", "update")

            binding.headerLeftNew.minShowTimeMs = minShowTimeMs
            binding.headerRightNew.minShowTimeMs = minShowTimeMs

            // 更新Text
            binding.headerLeftNew.showText(leftText)
            binding.headerMiddleNew.showText(middleText)
            binding.headerRightNew.showText(rightText)

            listOf(
                binding.headerLeftNew,
                binding.headerMiddleNew,
                binding.headerRightNew
            ).forEach {

                if (isAnimating)
                    it.startAnimation()
                else
                    it.stopAnimation()

                it.pixelMovePerSecond = esSpeed.toFloat()
                it.finishPositionOfLastWord = esFinishPositionOfLastWord

                binding.headerLeftNew.visibility =
                    if (utils.getIsOpenLeftEs())
                        VISIBLE
                    else
                        GONE

                binding.headerMiddleNew.visibility =
                    if (utils.getIsOpenMidEs())
                        VISIBLE
                    else
                        GONE


            }

//            refreshEsOnlyText(true)

            view.setOnClickListener { onHeaderClick?.invoke() }
        }
    )


}

