package com.microbus.announcer.fragment.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        utils = Utils(requireContext())
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


        val (cLineStationRange, setCLineStationRange) = remember {
            mutableFloatStateOf(utils.getStationRangeByLineType("C"))
        }

        val (bLineStationRange, setBLineStationRange) = remember {
            mutableFloatStateOf(utils.getStationRangeByLineType("B"))
        }

        val (uLineStationRange, setULineStationRange) = remember {
            mutableFloatStateOf(utils.getStationRangeByLineType("U"))
        }

        val (tLineStationRange, setTLineStationRange) = remember {
            mutableFloatStateOf(utils.getStationRangeByLineType("T"))
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



        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "CLineStationRange" -> setCLineStationRange(utils.getStationRangeByLineType("C"))
                    "BLineStationRange" -> setBLineStationRange(utils.getStationRangeByLineType("B"))
                    "ULineStationRange" -> setULineStationRange(utils.getStationRangeByLineType("U"))
                    "TLineStationRange" -> setTLineStationRange(utils.getStationRangeByLineType("T"))

                    "locationInterval" -> setLocationInterval(utils.getLocationInterval())
                    "autoSwitchLineDirection" -> setAutoSwitchLineDirection(utils.getIsAutoSwitchLineDirection())
                    "switchDirectionWhenOutFromTerminalWithOnUp" -> setSwitchDirectionWhenOutFromTerminalWithOnUp(utils.getIsAutoSwitchLineDirection())

                    "mapEditLineMode" -> setMapEditLineMode(utils.getIsMapEditLineMode())
                    "clickMapToCopyLngLat" -> setClickMapToCopyLngLat(utils.getIsClickMapToCopyLngLat())
                    "clickMapToAddStation" -> setClickMapToAddStation(utils.getIsClickMapToAddStation())
                    "clickLocationButtonToCopyLngLat" -> setClickLocationButtonToCopyLngLat(utils.getIsClickLocationButtonToCopyLngLat())
                    "linePlanning" -> setLinePlanning(utils.getIsLinePlanning())
                    "lineTrajectoryCorrection" -> setLineTrajectoryCorrection(utils.getIsLineTrajectoryCorrection())

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
                        EsKeywordItemGroup(
                            cLineStationRange,
                            bLineStationRange,
                            uLineStationRange,
                            tLineStationRange
                        )
                        LocationIntervalMsItem(locationInterval)
                        SwitchItem(
                            autoSwitchLineDirection,
                            setAutoSwitchLineDirection,
                            title = "自动切换上下行",
                            text = "检测到您折回站点时，切换上/下行",
                            icon = painterResource(id = R.drawable.switch2),
                            key = "autoSwitchLineDirection"
                        )
                        SwitchItem(
                            switchDirectionWhenOutFromTerminalWithOnUp,
                            setSwitchDirectionWhenOutFromTerminalWithOnUp,
                            title = "从上行终点站出站时切换下行",
                            text = "检测到您从上行终点站出站时，\n自动切换到下行",
                            icon = painterResource(id = R.drawable.switch2),
                            key = "switchDirectionWhenOutFromTerminalWithOnUp"
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
                            key = "mapEditLineMode"
                        )
                        SwitchItem(
                            clickMapToCopyLngLat,
                            setClickMapToCopyLngLat,
                            title = "点击地图复制经纬度",
                            text = "将点击位置的经纬度复制到剪切板",
                            icon = painterResource(id = R.drawable.location__),
                            key = "clickMapToCopyLngLat"
                        )
                        SwitchItem(
                            clickMapToAddStation,
                            setClickMapToAddStation,
                            title = "点击地图添加站点",
                            text = "添加位于点击位置的站点",
                            icon = painterResource(id = R.drawable.add),
                            key = "clickMapToAddStation"
                        )
                        SwitchItem(
                            clickLocationButtonToCopyLngLat,
                            setClickLocationButtonToCopyLngLat,
                            title = "点击定位按钮复制经纬度",
                            text = "将当前位置的经纬度复制到剪切板",
                            icon = painterResource(id = R.drawable.location__),
                            key = "clickLocationButtonToCopyLngLat"
                        )
                        SwitchItem(
                            linePlanning,
                            setLinePlanning,
                            title = "路线规划",
                            text = "根据本地站点规划路线",
                            icon = painterResource(id = R.drawable.line),
                            key = "linePlanning"
                        )
                        SwitchItem(
                            lineTrajectoryCorrection,
                            setLineTrajectoryCorrection,
                            title = "线路轨迹纠偏（实验性）",
                            text = "线路轨迹将贴合道路",
                            icon = painterResource(id = R.drawable.road),
                            key = "lineTrajectoryCorrection"
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
        tLineStationRange: Float
    ) {
        Column {
            Text(
                "进站判定范围半径",
                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)
            )
            Text(
                "当您位于该半径范围内，自动切换进站",
                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("社区", "C", cLineStationRange)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("公交", "B", bLineStationRange)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("地铁", "U", uLineStationRange)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("火车", "T", tLineStationRange)
                }
            }
        }

    }

    @Composable
    fun EsKeywordItem(title: String, key: String, value: Float) {
        BaseSettingItem(
            title,
            "${value.toInt()}米",
            painterResource(id = R.drawable.city),
            isShowIcon = false,
            clickFun = {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置${title}进站范围半径").setView(binding.root)
                    .setPositiveButton(requireContext().getString(android.R.string.ok), null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整${title}进站范围半径"
                binding.slider.stepSize = 1F
                binding.slider.valueFrom = when (key) {
                    "C" -> 20F - 15F
                    "B" -> 30F - 25F
                    "U" -> 300F - 250F
                    "T" -> 500F - 400F
                    else -> 30F - 25F
                }
                binding.slider.valueTo = when (key) {
                    "C" -> 20F + 15F
                    "B" -> 30F + 25F
                    "U" -> 300F + 250F
                    "T" -> 500F + 400F
                    else -> 30F + 25F
                }
                binding.slider.value = value

                binding.es.visibility = ViewGroup.GONE

                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = getString(R.string.currentRange, value.toInt())

                binding.slider.addOnChangeListener { slider, value, fromUser ->
                    binding.text.text = getString(R.string.currentRange, value.toInt())
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    utils.setStationRangeByLineType(key, binding.slider.value)
                    utils.showMsg("${title}进站范围半径设置成功")
                    dialog.dismiss()
                }
            })
    }

    @Composable
    fun LocationIntervalMsItem(interval: Int) {
        BaseSettingItem(
            "定位间隔", "每 $interval 毫秒定位一次", painterResource(id = R.drawable.time), {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置定位间隔").setView(binding.root)
                    .setPositiveButton(requireContext().getString(android.R.string.ok), null)
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

            })
    }


    @Composable
    fun SwitchItem(
        value: Boolean,
        setValue: (Boolean) -> Unit,
        title: String = "",
        text: String = "",
        icon: Painter? = null,
        key: String = ""
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

            })
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


}

