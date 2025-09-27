package com.microbus.announcer.fragment.settings

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem
import com.microbus.announcer.compose.SwitchSettingItem
import com.microbus.announcer.databinding.DialogInputBinding


class AnSettingsFragment : Fragment() {

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

        val (announcementLibrary, setAnnouncementLibrary) = remember {
            mutableStateOf(utils.getAnnouncementLibrary())
        }

        val (cnVoiceCount, setCnVoiceCount) = remember {
            mutableIntStateOf(
                utils.getLibVoiceCount(
                    "cn"
                )
            )
        }

        val (enVoiceCount, setEnVoiceCount) = remember {
            mutableIntStateOf(
                utils.getLibVoiceCount(
                    "en"
                )
            )
        }

        val customLangList = utils.getLangList()
        customLangList.remove("cn")
        customLangList.remove("en")
        val (customLangListStr, setCustomLangListStr) = remember {
            mutableStateOf(
                customLangList.joinToString(" ")
            )
        }

        val (useTTS, setUseTTS) = remember {
            mutableStateOf(utils.getIsUseTTS())
        }

        val (stationChangeVibrator, setStationChangeVibrator) = remember {
            mutableStateOf(utils.getIsStationChangeVibrator())
        }

        val (anSubtitle, setAnSubtitle) = remember {
            mutableStateOf(utils.getAnSubtitle())
        }

        val (serviceLanguageStr, setServiceLanguageStr) = remember {
            mutableStateOf(utils.getServiceLanguageStr())
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

        val (anFormatArray, setAnFormatArray) = remember {
            mutableStateOf(anFormatArrayOri)
        }


        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->

//                utils.showMsg(key ?: "")
                when (key) {
                    "announcementLibrary" -> {
                        setAnnouncementLibrary(prefs.getString(key, "") ?: "")
                        setCnVoiceCount(utils.getLibVoiceCount("cn"))
                        setEnVoiceCount(utils.getLibVoiceCount("en"))
                        val customLangList = utils.getLangList()
                        customLangList.remove("cn")
                        customLangList.remove("en")
                        setCustomLangListStr(customLangList.joinToString(" "))
                    }

                    "useTTS" -> {
                        setUseTTS(utils.getIsUseTTS())
                    }

                    "stationChangeVibrator" -> {
                        setStationChangeVibrator(utils.getIsStationChangeVibrator())
                    }

                    "anSubtitle" -> {
                        setAnSubtitle(utils.getAnSubtitle())
                    }

                    "serviceLanguageStr" -> {
                        setServiceLanguageStr(utils.getServiceLanguageStr())
                    }

                    else -> {
                        val anFormatArrayOri = Array(3) { Array(4) { "" } }
                        anFormatArrayOri.forEachIndexed { rowIndex, row ->
                            row.forEachIndexed { colIndex, value ->
                                anFormatArrayOri[rowIndex][colIndex] = utils.getAnnouncementFormat(
                                    stationStateList[rowIndex],
                                    stationTypeList[colIndex],
                                )
                            }
                        }
                        setAnFormatArray(anFormatArrayOri)
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
                        LibraryItem(
                            announcementLibrary,
                            cnVoiceCount,
                            enVoiceCount,
                            customLangListStr
                        )
                        TTSItem(useTTS, setUseTTS)
                        StationChangeVibratorItem(stationChangeVibrator, setStationChangeVibrator)
                        AnSubtitleItem(anSubtitle, setAnSubtitle)
                        ServiceLanguageItem(serviceLanguageStr)
                        AnFormatGroup(anFormatArray)
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun LibraryItem(
        announcementLibrary: String,
        cnVoiceCount: Int,
        enVoiceCount: Int,
        langListStr: String
    ) {

        val title = if (!utils.isGrantManageFilesAccessPermission()) {
            "暂无语音库读取权限，轻触以授予"
        } else {
            "${announcementLibrary}（当前语音库）"
        }

        BaseSettingItem(
            "",
            "",
            painterResource(id = R.drawable.library),
            {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("选择语音播报库")
                    .setSingleChoiceItems(
                        utils.getAnnouncementLibraryList().toTypedArray(),
                        utils.getAnnouncementLibraryList().indexOf(utils.getAnnouncementLibrary())
                    ) { dialog, which ->
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        sharedPreferences.edit {
                            putString(
                                "announcementLibrary",
                                utils.getAnnouncementLibraryList()[which]
                            )
                        }
                        dialog.cancel()
                    }
                    .show()
            },
            isCustomLeft = true,
            leftContain = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title,
                        fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorResource(R.color.md_theme_surface))
                                .padding(8.dp)
                                .weight(1f)
                        ) {
                            Text(
                                "中文语音 ${cnVoiceCount}条",
                                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                fontSize = 14.sp,
                                color = colorResource(R.color.an_text_1)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorResource(R.color.md_theme_surface))
                                .padding(8.dp)
                                .weight(1f)
                        ) {
                            Text(
                                "英文语音 ${enVoiceCount}条",
                                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                fontSize = 14.sp,
                                color = colorResource(R.color.an_text_1)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorResource(R.color.md_theme_surface))
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        val langCount = langListStr.split(" ").size
                        val text = if (langCount == 0 || langListStr == "")
                            "暂无自定义语种"
                        else
                            "${langCount}个自定义语种 $langListStr"
                        Text(
                            text,
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            fontSize = 14.sp,
                            color = colorResource(R.color.an_text_1)
                        )
                    }
                }
            })
    }

    @Composable
    fun TTSItem(tts: Boolean, setTTS: (Boolean) -> Unit) {
        BaseSettingItem(
            "使用TTS播报",
            "找不到对应音频文件时使用TTS播报",
            painterResource(id = R.drawable.tts),
            {
                toggleTTS(setTTS, !tts)
            },
            rightContain = {
                SwitchSettingItem(tts) {
                    toggleTTS(setTTS, it)
                }
            })
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
            })
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
            })
    }

    fun toggleAnSubtitle(setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("anSubtitle", it)
        }
    }

    @Composable
    fun ServiceLanguageItem(value: String) {
        BaseSettingItem(
            "服务语", value, painterResource(id = R.drawable.service), {
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
            })
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
                        })
                }
            }
        }
    }


}

