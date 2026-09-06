package com.microbus.announcer.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.databinding.DialogSliderBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class VoiceBroadcastSettings : Fragment() {

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
            composeView.layoutParams = layoutParams
        }

        initLocalBroadcast()

        return composeView
    }

    fun initLocalBroadcast() {

        val mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(utils.tryListeningAnActionName)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, intentFilter)

    }

    @Composable
    @Preview
    fun MainView() {

        val (useTTS, setUseTTS) = remember {
            mutableStateOf(utils.getIsUseTTS())
        }

        val (stationChangeVibrator, setStationChangeVibrator) = remember {
            mutableStateOf(utils.getIsStationChangeVibrator())
        }

        val (anSubtitle, setAnSubtitle) = remember {
            mutableStateOf(utils.getAnSubtitle())
        }

        val (clickMapPauseAn, setClickMapPauseAn) = remember {
            mutableStateOf(utils.getClickMapPauseAn())
        }

        val (autoAnInterval, setAutoAnInterval) = remember {
            mutableIntStateOf(utils.getAutoAnInterval())
        }

        val (loudnessBoostAmount, setLoudnessBoostAmount) = remember {
            mutableIntStateOf(utils.getLoudnessBoostAmount())
        }

        val anFormatArrayOri = Array(3) { Array(4) { "" } }

        val stationStateList = utils.getStationStateList()
        val stationTypeList = utils.getStationTypeList()

        anFormatArrayOri.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, value ->
                anFormatArrayOri[rowIndex][colIndex] = utils.getAnnouncementFormat(
                    stationStateList[rowIndex],
                    stationTypeList[colIndex],
                )
            }
        }


        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->

//                utils.showMsg(key ?: "")
                when (key) {

                    "useTTS" -> {
                        setUseTTS(utils.getIsUseTTS())
                    }

                    "stationChangeVibrator" -> {
                        setStationChangeVibrator(utils.getIsStationChangeVibrator())
                    }

                    "anSubtitle" -> {
                        setAnSubtitle(utils.getAnSubtitle())
                    }

                    "clickMapPauseAn" -> {
                        setClickMapPauseAn(utils.getClickMapPauseAn())
                    }


                    "autoAnInterval" -> {
                        setAutoAnInterval(utils.getAutoAnInterval())
                    }

                    "loudnessBoostAmount" -> {
                        setLoudnessBoostAmount(utils.getLoudnessBoostAmount())
                    }

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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.voice_broadcast))
                    },
                    content = { innerPadding ->
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TTSItem(useTTS, setUseTTS)
                                TTSSetting()
                                StationChangeVibratorItem(
                                    stationChangeVibrator,
                                    setStationChangeVibrator
                                )
                                AnSubtitleItem(anSubtitle, setAnSubtitle)
                                ClickMapPauseAnItem(clickMapPauseAn, setClickMapPauseAn)
                                AutoAnIntervalItem(autoAnInterval)
                                LoudnessBoostAmountItem(loudnessBoostAmount)
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    })

            }
        }
    }

    @Composable
    fun TTSItem(tts: Boolean, setTTS: (Boolean) -> Unit) {
        BaseSettingItem(
            "使用系统文字转语音",
            "找不到对应音频文件时使用TTS播报",
            painterResource(id = R.drawable.tts),
            {
                toggleTTS(setTTS, !tts)
            },
            rightContain = {
                SwitchSettingItem(tts) {
                    toggleTTS(setTTS, it)
                }
            },
        )
    }

    @Composable
    fun TTSSetting() {
        BaseSettingItem(
            "系统文字转语音设置",
            "TTS设置",
            painterResource(id = R.drawable.tts),
            {
                val intent = Intent()
                intent.setAction("com.android.settings.TTS_SETTINGS")
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(intent);
            },
        )
    }

    fun toggleTTS(setTTS: (Boolean) -> Unit, it: Boolean) {
        setTTS(it)
        prefs.edit {
            putBoolean("useTTS", it)
        }
    }

    @Composable
    fun StationChangeVibratorItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "站点状态变更振动提醒",
            "出站/即将进站/进站时振动提醒",
            painterResource(id = R.drawable.vibrator),
            {
                toggleStationChangeVibrator(setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleStationChangeVibrator(setValue, it)
                }
            },
        )
    }

    fun toggleStationChangeVibrator(setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("stationChangeVibrator", it)
        }
    }

    @Composable
    fun AnSubtitleItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "播报字幕",
            "播报时显示字幕",
            painterResource(id = R.drawable.subtitle),
            {
                toggleAnSubtitle(setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleAnSubtitle(setValue, it)
                }
            },
        )
    }

    fun toggleAnSubtitle(setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("anSubtitle", it)
        }
    }

    @Composable
    fun ClickMapPauseAnItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "点击地图中断播报",
            "点击地图后，自动中断播报",
            painterResource(id = R.drawable.pause),
            {
                toggleClickMapPauseAn(setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleClickMapPauseAn(setValue, it)
                }
            },
        )
    }

    fun toggleClickMapPauseAn(setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("clickMapPauseAn", it)
        }
    }

    @Composable
    fun ServiceLanguageItem(value: String) {
        BaseSettingItem(
            "服务语", value, painterResource(id = R.drawable.service),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置服务语").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.setText(value)

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()

                    if (newValue == "") {
                        utils.showMsg("请输入服务语")
                        return@setOnClickListener
                    }

                    val serviceLanguageList = newValue.split("\n")

                    var hasError = false
                    serviceLanguageList.forEachIndexed { index, langStr ->

                        val ans = utils.getAnnouncements(langStr)
                        if (ans[0] == "ERROR") {
                            utils.showMsg("${ans[1]}不正确，请修改")
                            hasError = true
                            return@forEachIndexed
                        }
                    }

                    if (!hasError) {
                        prefs.edit {
                            putString("serviceLanguageStr", newValue)
                        }
                        utils.showMsg("服务语设置成功")
                        dialog.dismiss()
                    }

                }

                binding.editText.isSingleLine = false
                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }

    @Composable
    fun AutoAnIntervalItem(interval: Int) {
        BaseSettingItem(
            "自动播报间隔", "每次报站间隔 $interval 秒", painterResource(id = R.drawable.time),
            {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置自动播报间隔").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整自动播报间隔"
                binding.slider.stepSize = 1F
                binding.slider.valueFrom = 0F
                binding.slider.valueTo = 180F
                binding.slider.value = interval.toFloat()

                binding.es.visibility = ViewGroup.GONE

                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = getString(R.string.autoAnPerSecond, interval)

                binding.slider.addOnChangeListener { slider, value, fromUser ->
                    binding.text.text = getString(R.string.autoAnPerSecond, value.toInt())
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    prefs.edit {
                        putInt("autoAnInterval", binding.slider.value.toInt())
                    }
                    utils.showMsg("自动播报间隔设置成功")
                    dialog.dismiss()
                }

            },
        )
    }

    @Composable
    fun LoudnessBoostAmountItem(amount: Int) {
        val itemText = "音量增强幅度"
        BaseSettingItem(
            itemText,
            getString(R.string.volumeBoostAmountMdB, amount),
            painterResource(id = R.drawable.sound),
            {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置${itemText}").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整${itemText}"
                binding.slider.stepSize = 1F
                binding.slider.valueFrom = 0F
                binding.slider.valueTo = 6000F
                binding.slider.value = amount.toFloat()

                binding.es.visibility = ViewGroup.GONE

                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = getString(R.string.volumeBoostAmountMdB, amount)

                binding.slider.addOnChangeListener { slider, value, fromUser ->
                    binding.text.text = getString(R.string.volumeBoostAmountMdB, value.toInt())
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    prefs.edit {
                        putInt("loudnessBoostAmount", binding.slider.value.toInt())
                    }

                    val intent = Intent()
                        .setAction(utils.setLoudnessBoostAmountName)
                    LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(intent)

                    utils.showMsg("${itemText}设置成功")
                    dialog.dismiss()
                }

            },
        )
    }

    @Composable
    fun AnFormatGroup(anFormatArray: Array<Array<String>>) {
        val stationStateList = utils.getStationStateList()
        val stationTypeList = utils.getStationTypeList()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (state in stationStateList) {
                val stateStr = when (state) {
                    "Next" -> "下一站"
                    "WillArrive" -> "即将到站"
                    "Arrive" -> "到站"
                    else -> ""
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${stateStr}格式",
                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 4.dp)
                )
                for (type in stationTypeList) {
                    val typeStr = when (type) {
                        "Default" -> "默认"
                        "Starting" -> "起点站"
                        "Second" -> "第二站"
                        "Terminal" -> "终点站"
                        else -> ""
                    }
                    BaseSettingItem(
                        typeStr,
                        anFormatArray[stationStateList.indexOf(state)][stationTypeList.indexOf(type)],
                        painterResource(id = R.drawable.format),
                        {

                            if (!utils.isGrantManageFilesAccessPermission()) {
                                utils.requestManageFilesAccessPermission(requireActivity())
                                return@BaseSettingItem
                            }

                            val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                            val dialog = MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.CustomAlertDialogStyle
                            ).setTitle("设置${stateStr}${typeStr}播报")
                                .setView(binding.root)
                                .setPositiveButton("保存", null)
                                .setNeutralButton("帮助", null)
                                .setNegativeButton("试听", null)
                                .show()

                            binding.editText.isSingleLine = false
                            binding.editText.setText(
                                anFormatArray[stationStateList.indexOf(state)][stationTypeList.indexOf(
                                    type
                                )]
                            )

                            dialog.setCanceledOnTouchOutside(false)

                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                                utils.openHelperDialog("查看语音播报文档", "readme/语音播报.md")
                            }

                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {

                                val newValue = binding.editText.text.toString()

                                if (newValue == "") {
                                    utils.showMsg("请输入播报格式")
                                    return@setOnClickListener
                                }

                                val ans = utils.getAnnouncements(newValue)
                                if (ans[0] == "ERROR") {
                                    utils.showMsg("${ans[1]}不正确，请修改")
                                    return@setOnClickListener
                                } else {
                                    val intent = Intent()
                                        .setAction(utils.tryListeningAnActionName)
                                        .putExtra("stateStr", stateStr)
                                        .putExtra("typeStr", typeStr)
                                        .putExtra("format", newValue)
                                    LocalBroadcastManager.getInstance(requireContext())
                                        .sendBroadcast(intent)
                                }

                            }

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                val newValue = binding.editText.text.toString()

//                                if (newValue == "") {
//                                    utils.showMsg("请输入播报格式")
//                                    return@setOnClickListener
//                                }

                                // 允许填空

                                val ans = utils.getAnnouncements(newValue)
                                if (ans[0] == "ERROR" && newValue != "") {
                                    utils.showMsg("${ans[1]}不正确，请修改")
                                } else {

                                    prefs.edit {
                                        putString(
                                            "${type}${state}AnnouncementExpression",
                                            newValue
                                        )
                                    }

                                    // 同步修改config.json
                                    utils.updateAnnouncementFormatConfig(
                                        type,
                                        state,
                                        newValue
                                    )
                                    utils.showMsg("播报格式设置成功")
                                    dialog.dismiss()
                                }
                            }

                            // 中断播报
                            dialog.setOnDismissListener {
                                val intent = Intent()
                                    .setAction(utils.tryListeningAnActionName)
                                    .putExtra("stateStr", "")
                                    .putExtra("typeStr", "")
                                    .putExtra("format", " ")
                                LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(intent)
                            }

                            binding.textInputLayout.hint = "请输入文本"
                            binding.textInputLayout.requestFocus()
                            WindowCompat.getInsetsController(
                                requireActivity().window,
                                binding.editText
                            )
                                .show(WindowInsetsCompat.Type.ime())
                        },
                    )
                }
            }
        }
    }

}

