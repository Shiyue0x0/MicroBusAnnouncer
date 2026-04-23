package com.microbus.announcer.fragment.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem
import com.microbus.announcer.compose.SwitchSettingItem
import com.microbus.announcer.databinding.DialogSliderBinding


class LocationAndMapSettingsFragment : Fragment() {

    lateinit var utils: Utils
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        utils = Utils(requireContext(), requireActivity())
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val composeView = ComposeView(requireContext())

        composeView.apply {
            setContent { MainView() }
        }

        composeView.post {
            val layoutParams = composeView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            composeView.setLayoutParams(layoutParams)
        }


        return composeView
    }

    @Composable
    @Preview
    fun MainView() {

        // LineStationRange Begin
//        val (cLineStationRange, setCLineStationRange) = remember {
//            mutableFloatStateOf(utils.getStationRangeByLineType("C", "WillIn"))
//        }
//
//        val (bLineStationRange, setBLineStationRange) = remember {
//            mutableFloatStateOf(utils.getStationRangeByLineType("B", "WillIn"))
//        }
//
//        val (uLineStationRange, setULineStationRange) = remember {
//            mutableFloatStateOf(utils.getStationRangeByLineType("U", "WillIn"))
//        }
//
//        val (tLineStationRange, setTLineStationRange) = remember {
//            mutableFloatStateOf(utils.getStationRangeByLineType("T", "WillIn"))
//        }

        val lineStationRangeMap = mutableMapOf<Pair<String, String>, MutableFloatState>()
        val lineTypes = listOf("C", "B", "U", "T")
        val actions = listOf("WillIn", "In", "Out")
        for (lineType in lineTypes) {
            for (action in actions) {
                lineStationRangeMap[Pair(lineType, action)] = remember {
                    mutableFloatStateOf(utils.getStationRangeByLineType(lineType, action))
                }
            }
        }

        // LineStationRange End

        val autoSwitchStationStateMap = mutableMapOf<String, MutableState<Boolean>>()
        for (action in actions) {
            autoSwitchStationStateMap[action] = remember {
                mutableStateOf(utils.getAutoSwitchStationState(action))
            }
        }


        val (locationInterval, setLocationInterval) = remember {
            mutableIntStateOf(utils.getLocationInterval())
        }

        val (switchDirectionWhenOutFromTerminalWithOnUp, setSwitchDirectionWhenOutFromTerminalWithOnUp) = remember {
            mutableStateOf(utils.getSwitchDirectionWhenOutFromTerminalWithOnUp())
        }

        val (mapEditLineMode, setMapEditLineMode) = remember {
            mutableStateOf(utils.getIsMapEditLineMode())
        }

        val (autoSwitchLineDirection, setAutoSwitchLineDirection) = remember {
            mutableStateOf(utils.getIsAutoSwitchLineDirection())
        }

        val (clickMapToCopyLngLat, setClickMapToCopyLngLat) = remember {
            mutableStateOf(utils.getIsClickMapToCopyLngLat())
        }

        val (clickMapToAddStation, setClickMapToAddStation) = remember {
            mutableStateOf(utils.getIsClickMapToAddStation())
        }

        val (clickLocationButtonToCopyLngLat, setClickLocationButtonToCopyLngLat) = remember {
            mutableStateOf(utils.getIsClickLocationButtonToCopyLngLat())
        }

        val (linePlanning, setLinePlanning) = remember {
            mutableStateOf(utils.getIsLinePlanning())
        }

        val (lineTrajectoryCorrection, setLineTrajectoryCorrection) = remember {
            mutableStateOf(utils.getIsLineTrajectoryCorrection())
        }


        val (isMapTrafficEnabled, setIsMapTrafficEnabled) = remember {
            mutableStateOf(utils.getIsMapTrafficEnabled())
        }



        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {

//                    "CLineStationRange" -> setCLineStationRange(
//                        utils.getStationRangeByLineType(
//                            "C",
//                            "WillIn"
//                        )
//                    )
//
//                    "BLineStationRange" -> setBLineStationRange(
//                        utils.getStationRangeByLineType(
//                            "B",
//                            "WillIn"
//                        )
//                    )
//
//                    "ULineStationRange" -> setULineStationRange(
//                        utils.getStationRangeByLineType(
//                            "U",
//                            "WillIn"
//                        )
//                    )
//
//                    "TLineStationRange" -> setTLineStationRange(
//                        utils.getStationRangeByLineType(
//                            "T",
//                            "WillIn"
//                        )
//                    )

                    "locationInterval" -> setLocationInterval(utils.getLocationInterval())
                    "autoSwitchLineDirection" -> setAutoSwitchLineDirection(utils.getIsAutoSwitchLineDirection())
                    "switchDirectionWhenOutFromTerminalWithOnUp" -> setSwitchDirectionWhenOutFromTerminalWithOnUp(
                        utils.getSwitchDirectionWhenOutFromTerminalWithOnUp()
                    )

                    "mapEditLineMode" -> setMapEditLineMode(utils.getIsMapEditLineMode())
                    "clickMapToCopyLngLat" -> setClickMapToCopyLngLat(utils.getIsClickMapToCopyLngLat())
                    "clickMapToAddStation" -> setClickMapToAddStation(utils.getIsClickMapToAddStation())
                    "clickLocationButtonToCopyLngLat" -> setClickLocationButtonToCopyLngLat(utils.getIsClickLocationButtonToCopyLngLat())
                    "linePlanning" -> setLinePlanning(utils.getIsLinePlanning())
                    "lineTrajectoryCorrection" -> setLineTrajectoryCorrection(utils.getIsLineTrajectoryCorrection())

                    "isMapTrafficEnabled" -> setIsMapTrafficEnabled(utils.getIsMapTrafficEnabled())

                }

                if (key != null) {

                    if (key.contains("StationRange")) {

                        val start = key.indexOf("Line") + "Line".length
                        val end = key.indexOf("StationRange")
                        val action = key.substring(start, end)

                        lineStationRangeMap[Pair(key.first().toString(), action)]!!.value =
                            utils.getStationRangeByLineType(key.first().toString(), action)
                    }

                    if (key.contains("autoSwitchStationStateWhen")) {
                        val action = key.substringAfter("autoSwitchStationStateWhen")
                        autoSwitchStationStateMap[action]!!.value =
                            utils.getAutoSwitchStationState(action)
                    }

                }


            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        Surface(
            contentColor = colorResource(R.color.md_theme_onSurface),
            color = colorResource(R.color.an_window_bg)
        ) {
            MaterialTheme {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "自动切换站点",
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 4.dp)
                        )
                        EsKeywordItemGroup(
                            lineStationRangeMap[Pair("C", "WillIn")]!!.value,
                            lineStationRangeMap[Pair("B", "WillIn")]!!.value,
                            lineStationRangeMap[Pair("U", "WillIn")]!!.value,
                            lineStationRangeMap[Pair("T", "WillIn")]!!.value,
                            autoSwitchStationStateMap["WillIn"]!!,
                            "WillIn"
                        )
                        EsKeywordItemGroup(
                            lineStationRangeMap[Pair("C", "In")]!!.value,
                            lineStationRangeMap[Pair("B", "In")]!!.value,
                            lineStationRangeMap[Pair("U", "In")]!!.value,
                            lineStationRangeMap[Pair("T", "In")]!!.value,
                            autoSwitchStationStateMap["In"]!!,
                            "In"
                        )
                        EsKeywordItemGroup(
                            lineStationRangeMap[Pair("C", "Out")]!!.value,
                            lineStationRangeMap[Pair("B", "Out")]!!.value,
                            lineStationRangeMap[Pair("U", "Out")]!!.value,
                            lineStationRangeMap[Pair("T", "Out")]!!.value,
                            autoSwitchStationStateMap["Out"]!!,
                            "Out"
                        )
                        Text(
                            "定位基础",
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 4.dp)
                        )
                        LocationIntervalMsItem(locationInterval)
                        SwitchItem(
                            autoSwitchLineDirection,
                            setAutoSwitchLineDirection,
                            title = "自动切换上下行",
                            text = "检测到您折回站点时，切换上/下行",
                            icon = painterResource(id = R.drawable.switch2),
                            key = "autoSwitchLineDirection",
                        )
                        SwitchItem(
                            switchDirectionWhenOutFromTerminalWithOnUp,
                            setSwitchDirectionWhenOutFromTerminalWithOnUp,
                            title = "从上行终点站出站时切换下行",
                            text = "检测到您从上行终点站出站时，\n自动切换到下行",
                            icon = painterResource(id = R.drawable.switch2),
                            key = "switchDirectionWhenOutFromTerminalWithOnUp",
                        )

                        Text(
                            "地图",
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 4.dp)
                        )
                        SwitchItem(
                            mapEditLineMode,
                            setMapEditLineMode,
                            title = "地图编辑路线模式",
                            text = "开启该模式并且运行全站路线，\n可以在地图上便捷地编辑路线",
                            icon = painterResource(id = R.drawable.line),
                            key = "mapEditLineMode",
                        )
                        SwitchItem(
                            clickMapToCopyLngLat,
                            setClickMapToCopyLngLat,
                            title = "点击地图复制经纬度",
                            text = "将点击位置的经纬度复制到剪切板",
                            icon = painterResource(id = R.drawable.location__),
                            key = "clickMapToCopyLngLat",
                        )
                        SwitchItem(
                            clickMapToAddStation,
                            setClickMapToAddStation,
                            title = "点击地图添加站点",
                            text = "添加位于点击位置的站点",
                            icon = painterResource(id = R.drawable.add),
                            key = "clickMapToAddStation",
                        )
                        SwitchItem(
                            clickLocationButtonToCopyLngLat,
                            setClickLocationButtonToCopyLngLat,
                            title = "点击定位按钮复制经纬度",
                            text = "将当前位置的经纬度复制到剪切板",
                            icon = painterResource(id = R.drawable.location__),
                            key = "clickLocationButtonToCopyLngLat",
                        )
                        SwitchItem(
                            linePlanning,
                            setLinePlanning,
                            title = "路线规划",
                            text = "根据本地站点规划路线",
                            icon = painterResource(id = R.drawable.line),
                            key = "linePlanning",
                        )
                        SwitchItem(
                            isMapTrafficEnabled,
                            setIsMapTrafficEnabled,
                            title = "显示路况",
                            text = "地图实时显示交通情况",
                            icon = painterResource(id = R.drawable.traffic),
                            key = "isMapTrafficEnabled",
                        )
                        SwitchItem(
                            lineTrajectoryCorrection,
                            setLineTrajectoryCorrection,
                            title = "路线贴合道路",
                            text = "线路轨迹将贴合道路",
                            icon = painterResource(id = R.drawable.road),
                            key = "lineTrajectoryCorrection",
                        )

                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    fun EsKeywordItemGroup(
        cLineStationRange: Float,
        bLineStationRange: Float,
        uLineStationRange: Float,
        tLineStationRange: Float,
        switch: MutableState<Boolean>,
        action: String
    ) {
        val actionName = getActionName(action)



        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colorResource(R.color.an_contain_bg))
        ) {
//            Text(
//                "${actionName}",
//                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
//                modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
//            )
//            Text(
//                "当您位于该站点半径范围内，自动切换到${actionName}",
//                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
//                fontSize = 14.sp,
//                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 4.dp)
//            )

            val (switchValue, switchSet) = switch

            SwitchItem(
                switchValue,
                switchSet,
                title = actionName,
                text = "当您位于该站点半径范围内，\n自动切换到${actionName}",
                icon = painterResource(id = R.drawable.arrow_up),
                key = "autoSwitchStationStateWhen${action}",
                iconRotate = when (action) {
                    "WillIn" -> 90F + 45F
                    "In" -> 90F + 90F
                    "Out" -> 90F
                    else -> 90F
                }
            )

            // todo 分割线
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.padding(start = (24 + 16).dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("社区", "C", cLineStationRange, action)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("公交", "B", bLineStationRange, action)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("地铁", "U", uLineStationRange, action)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("火车", "T", tLineStationRange, action)
                }
            }
        }

    }

    @Composable
    fun EsKeywordItem(title: String, key: String, value: Float, action: String) {
        val actionName = getActionName(action)
        BaseSettingItem(
            title,
            "${value.toInt()}米",
            painterResource(id = R.drawable.city),
            clickFun = {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置${title}${actionName}范围半径").setView(binding.root)
                    .setPositiveButton(requireContext().getString(android.R.string.ok), null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整${title}${actionName}范围半径"
                binding.slider.stepSize = 1F
//                binding.slider.valueFrom = when (key) {
//                    "C" -> 20F - 15F
//                    "B" -> 30F - 25F
//                    "U" -> 300F - 250F
//                    "T" -> 500F - 400F
//                    else -> 30F - 25F
//                }
//                binding.slider.valueTo = when (key) {
//                    "C" -> 20F + 15F
//                    "B" -> 30F + 25F
//                    "U" -> 300F + 250F
//                    "T" -> 500F + 400F
//                    else -> 30F + 25F
//                }
                binding.slider.valueFrom = 0F
                binding.slider.valueTo =
                    maxOf(utils.getStationRangeByLineType(key, action, true) * 3, value)
                binding.slider.value = value

                binding.es.visibility = ViewGroup.GONE

//                binding.text.visibility = ViewGroup.VISIBLE
//                binding.text.text = getString(R.string.currentRange, value.toInt())

                binding.text1.text = getString(R.string.currentRange)
                binding.text2.text = "m"
                binding.editValue.inputType = InputType.TYPE_CLASS_NUMBER
                binding.editValue.setText(value.toInt().toString())

                binding.editableLayout.visibility = ViewGroup.VISIBLE


                binding.slider.addOnChangeListener { slider, value, fromUser ->
//                    binding.text.text = getString(R.string.currentRange, value.toInt())
                    binding.editValue.setText(value.toInt().toString())
                }

                binding.editValue.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().toFloatOrNull() == null || s.toString().toFloatOrNull()?.let { it > 0 } == false) {
                            utils.showMsg("请输入正整数半径")
                            return
                        }

                        val value = s.toString().toFloatOrNull()!!
                        if (value > binding.slider.valueTo) {
                            binding.slider.valueTo = value
                        }

                        binding.slider.value = value
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }
                })

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    utils.setStationRangeByLineType(key, binding.slider.value, action)
                    utils.showMsg("${title}${actionName}范围半径设置成功")
                    dialog.dismiss()
                }
            },
            isShowIcon = false,
        )
    }

    @Composable
    fun LocationIntervalMsItem(interval: Int) {
        BaseSettingItem(
            "定位间隔", "每 $interval 毫秒定位一次", painterResource(id = R.drawable.time),
            {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置定位间隔").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整定位间隔"
                binding.slider.stepSize = 1F
                binding.slider.valueFrom = 1000F
                binding.slider.valueTo = 10000F
                binding.slider.value = interval.toFloat()

                binding.es.visibility = ViewGroup.GONE

                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = getString(R.string.locationPerSecond, interval)

                binding.slider.addOnChangeListener { slider, value, fromUser ->
                    binding.text.text = getString(R.string.locationPerSecond, value.toInt())
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    prefs.edit {
                        putInt("locationInterval", binding.slider.value.toInt())
                    }
                    utils.showMsg("定位间隔设置成功，重启生效")
                    dialog.dismiss()
                }

            },
        )
    }


    @Composable
    fun SwitchItem(
        value: Boolean,
        setValue: (Boolean) -> Unit,
        title: String = "",
        text: String = "",
        icon: Painter? = null,
        key: String = "",
        iconRotate: Float = 0F
    ) {
        BaseSettingItem(
            title,
            text,
            icon,
            {
                toggleSwitch(value, setValue, !value, key)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleSwitch(value, setValue, it, key)
                }
            },
            iconRotate = iconRotate
        )
    }

    fun toggleSwitch(
        value: Boolean,
        setValue: (Boolean) -> Unit,
        it: Boolean,
        key: String
    ) {
        setValue(it)
        prefs.edit {
            putBoolean(key, it)
        }
    }


    fun getActionName(action: String): String {
        return when (action) {
            "WillIn" -> "即将进站"
            "In" -> "进站"
            "Out" -> "出站"
            else -> ""
        }
    }


}

