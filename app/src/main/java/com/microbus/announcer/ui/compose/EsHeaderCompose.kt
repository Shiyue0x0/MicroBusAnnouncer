package com.microbus.announcer.ui.compose

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.preference.PreferenceManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.databinding.ViewEsHeaderBinding
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.ui.view.ESView
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds


@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun EsHeaderCompose(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false,
    onHeaderClick: (() -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,

    line: Line,
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

    val esPlayIndex = remember { mutableIntStateOf(-1) }

    val (esSpeed, setEsSpeed) = remember {
        mutableIntStateOf(utils.getEsSpeed())
    }

    val minShowTimeMs = remember { mutableIntStateOf(0) }

    val (esFinishPositionOfLastWord, setEsFinishPositionOfLastWord) = remember {
        mutableFloatStateOf(utils.getEsFinishPositionOfLastWord())
    }

    val (isOpenLeftEs, setIsOpenLeftEs) = remember {
        mutableStateOf(utils.getIsOpenLeftEs())
    }

    val (isMidLeftEs, setIsMidLeftEs) = remember {
        mutableStateOf(utils.getIsOpenMidEs())
    }


    val (isLeftShowFinish, setIsLeftShowFinish) = remember { mutableStateOf(false) }
    val (isMiddleShowFinish, setIsMiddleShowFinish) = remember { mutableStateOf(false) }
    val (isRightShowFinish, setIsRightShowFinish) = remember { mutableStateOf(false) }

    val currentIsLeftShowFinish by rememberUpdatedState(isLeftShowFinish)
    val currentIsMiddleShowFinish by rememberUpdatedState(isMiddleShowFinish)
    val currentIsRightShowFinish by rememberUpdatedState(isRightShowFinish)

    var esHeaderBinding by remember { mutableStateOf<ViewEsHeaderBinding?>(null) }

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
            "<line>" -> line.name

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

//        Log.d("L187", "${currentLineStationCount} ${currentLineStation.cnName}")

//        Log.d(tag, "refreshEsOnlyText S")

        var leftText: String
        var rightText: String

        @SuppressLint("LocalContextGetResourceValueCall")
        if (esPlayIndex.intValue >= 0 && esPlayIndex.intValue < esList.size) {
            leftText = esList[esPlayIndex.intValue].leftText
            rightText = esList[esPlayIndex.intValue].rightText
        } else {
            leftText = context.getString(R.string.main_staring_station_name)
            rightText = context.getString(R.string.main_terminal_name)
        }

        for (keyword in utils.getDefaultKeywordList()) {
            leftText = leftText.replace(keyword, getValueMapValue(keyword), true)
            rightText = rightText.replace(keyword, getValueMapValue(keyword), true)
        }

        if (!utils.getIsOpenLeftEs()) {
            rightText = "$leftText $rightText"
            leftText = ""
        }

//        Log.d("L218", "${esHeaderBinding == null}")

        esHeaderBinding?.let { binding ->

            fun setText(view: ESView, text: String) {
                if (isUseSet) {
                    if (view.getText() != text) {
                        view.setText(text)
                    }
                } else {
                    view.nextText(text)
                }
            }

            setText(binding.headerLeftNew, leftText)
            setText(binding.headerMiddleNew, line.name)
            setText(binding.headerRightNew, rightText)
        }


//        Log.d(tag, "refreshEsOnlyText E")

        // TODO
//        Log.d("L221", "${leftText} ${rightText}")
//        setLeftText(leftText)
//        setRightText(rightText)

    }

    fun esPlayNext() {
//        Log.d("L237", "${esPlayIndex}")
        if (esPlayIndex.intValue < esList.size - 1) {
            esPlayIndex.intValue++
        } else {
            setEsList(utils.getEsList(utils.getEsText()))
            esPlayIndex.intValue = (
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


        if (esPlayIndex.intValue == -1 && esList.isNotEmpty()) {
            esPlayIndex.intValue = 0
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
                        esPlayIndex.intValue = i
                        hasB = true
                        break
                    }
                }
                if (!hasB) {
                    esPlayIndex.intValue = (
                            if (frontDefaultItemIndex >= 0) {
                                frontDefaultItemIndex
                            } else {
                                -1
                            }
                            )
                }
            }
            // 仅某状态显示，或切换到当前状态显示
            else if (esList[esPlayIndex.intValue].type.contains(Regex("[NWASCT]")) || toStation) {

                var hasMatchCurrentState = false
                var hasMatchCurrentPos = false
                var frontDefaultItemIndex = -1

                val start = if (toStation)
                    0
                else
                    esPlayIndex.intValue

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
                            esPlayIndex.intValue = i
                            hasMatchCurrentState = true
                            hasMatchCurrentPos = true
                            break
                        }
                        // 只有状态类型
                    } else if (currentMatchType != "") {
                        if (getStationStateTypeMap()[currentMatchType] == currentLineStationState) {
                            esPlayIndex.intValue = i
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
                            esPlayIndex.intValue = i
                            hasMatchCurrentPos = true
                            break
                        }
                    }
                }
                if (!hasMatchCurrentState && !hasMatchCurrentPos) {
                    if (frontDefaultItemIndex >= 0) {
                        esPlayIndex.intValue = frontDefaultItemIndex
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
                        esPlayIndex.intValue = resIndex
                    }
                }
            }


        }

        val minTimeS =
            if (esPlayIndex.intValue >= 0 && esPlayIndex.intValue < esList.size) esList[esPlayIndex.intValue].minTimeS else 5
//        binding.headerLeftNew.minShowTimeMs = minTimeS * 1000
//        binding.headerRightNew.minShowTimeMs = minTimeS * 1000
        minShowTimeMs.intValue = (minTimeS * 1000)
        refreshEsOnlyText()

    }

    var isRunning by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
//        Log.d("L80", "onResume")
        isRunning = true


        onPauseOrDispose {
//            Log.d("L80", "onPause")
            isRunning = false

        }
    }

    LaunchedEffect(
        isRunning,
        esPlayIndex.intValue,
        esList
    ) {
        if (!isRunning) return@LaunchedEffect

        while (isRunning) {
            val idx = esPlayIndex.intValue
            if (idx >= 0 && idx < esList.size &&
                esList[idx].type.contains("R")
            ) {
                refreshEsOnlyText(true)
            }
            delay(100L.milliseconds)
        }
    }

    LaunchedEffect(isLeftShowFinish, isRightShowFinish, isRunning) {
        if (!isRunning) return@LaunchedEffect
        val isLeftFinish = isLeftShowFinish || !utils.getIsOpenLeftEs()
        val isRightFinish = isRightShowFinish
        if (isLeftFinish && isRightFinish) {
            esPlayNext()
            refreshEs()
        }
    }

    /**
     * 立即刷新电显，并切换到站点状态和位置（如果有）
     */
    fun refreshEsToStation() {
        refreshEs(toStation = true)
    }

    /**
     * 立即刷新电显，并切换到首末站显示（如果有）
     */
    fun refreshEsToStaringAndTerminal() {
        refreshEs(toStaringAndTerminal = true)
    }

    LaunchedEffect(currentLineStation, currentLineStationCount, currentLineStationState) {
        refreshEsToStation()
    }

    LaunchedEffect(line, currentLineStationList) {
        refreshEsToStaringAndTerminal()
    }


    AndroidView(
        modifier = modifier,
        factory = { context ->

            val binding = ViewEsHeaderBinding.inflate(
                LayoutInflater.from(context), null, false
            )

            binding.root.tag = binding
            esHeaderBinding = binding

            binding.headerLeftNew.minShowTimeMs = 500
            binding.headerRightNew.minShowTimeMs = 500

            // 监听IsShowFinish
            binding.headerLeftNew.setOnShowFinishChangedListener { finish ->
                setIsLeftShowFinish(finish)
            }
            binding.headerMiddleNew.setOnShowFinishChangedListener { finish ->
                setIsMiddleShowFinish(finish)
            }
            binding.headerRightNew.setOnShowFinishChangedListener { finish ->
                setIsRightShowFinish(finish)
            }

            binding.root
        },
        update = { view ->
            val binding = view.tag as? ViewEsHeaderBinding ?: return@AndroidView

//            Log.d("L32", "update ${leftText} ${rightText}")


            listOf(
                binding.headerLeftNew,
                binding.headerMiddleNew,
                binding.headerRightNew
            ).forEach {

                it.minShowTimeMs = minShowTimeMs.intValue

                if (isAnimating)
                    it.startAnimation()
                else
                    it.stopAnimation()

                it.pixelMovePerSecond = esSpeed.toFloat()
                it.finishPositionOfLastWord = esFinishPositionOfLastWord

            }

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

            view.setOnClickListener { onHeaderClick?.invoke() }
            view.setOnLongClickListener {
                onHeaderLongClick?.invoke()
                true}
        }
    )


}

