package com.microbus.announcer.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class
MapSettings : Fragment() {

    lateinit var utils: Utils
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

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

        val (mapType, setMapType) = remember {
            mutableIntStateOf(utils.getMapType())
        }
        val (mapEditLineMode, setMapEditLineMode) = remember {
            mutableStateOf(utils.getIsMapEditLineMode())
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

                    "mapType" -> setMapType(
                        utils.getMapType()
                    )

                    "mapEditLineMode" -> setMapEditLineMode(utils.getIsMapEditLineMode())
                    "clickMapToCopyLngLat" -> setClickMapToCopyLngLat(utils.getIsClickMapToCopyLngLat())
                    "clickMapToAddStation" -> setClickMapToAddStation(utils.getIsClickMapToAddStation())
                    "clickLocationButtonToCopyLngLat" -> setClickLocationButtonToCopyLngLat(utils.getIsClickLocationButtonToCopyLngLat())
                    "linePlanning" -> setLinePlanning(utils.getIsLinePlanning())
                    "lineTrajectoryCorrection" -> setLineTrajectoryCorrection(utils.getIsLineTrajectoryCorrection())

                    "isMapTrafficEnabled" -> setIsMapTrafficEnabled(utils.getIsMapTrafficEnabled())

                }

            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        Surface(
            contentColor = colorResource(R.color.md_theme_onSurface),
            color = colorResource(R.color.md_theme_surface)
        ) {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(
                controller = controller
            ) {
                Scaffold(
                    topBar = {
                        AnSmallTopAppBar(requireActivity(), getString(R.string.map))
                    },
                    content = { innerPadding ->
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MapTypeItem(mapType, setMapType)
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
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    })

            }
        }
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

    @Composable
    fun MapTypeItem(mapType: Int, setMapType: (Int) -> Unit) {
        val nameList =
            listOf(
                "跟随系统（普通或黑夜）",
                "普通地图",
                "卫星地图",
                "黑夜地图",
                "导航地图",
                "公交地图"
            )
        val valueList = listOf(0, 1, 2, 3, 4, 5)
        val currentChooseIndex = valueList.indexOf(utils.getMapType())

        BaseSettingItem(
            "地图模式", nameList[valueList.indexOf(mapType)], painterResource(id = R.drawable.map),
            {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择地图模式").setSingleChoiceItems(
                    nameList.toTypedArray(), currentChooseIndex
                ) { dialog, which ->
                    prefs.edit {
                        putString("mapType", valueList[which].toString())
                    }
                    dialog.cancel()
                }.show()
            },
            rightContain = {

            },
        )
    }

}
