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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class AnnouncementLibrarySettings : Fragment() {

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

        val (announcementLibrary, setAnnouncementLibrary) = remember {
            mutableStateOf(utils.getAnnouncementLibrary())
        }

        val (useTTS, setUseTTS) = remember {
            mutableStateOf(utils.getIsUseTTS())
        }

        val (enableHTTPGetVoiceItem, setEnableHTTPGetVoiceItem) = remember {
            mutableStateOf(utils.getEnableHTTPGetVoiceItem())
        }

        val (httpGetVoiceURL, setHTTPGetVoiceURL) = remember {
            mutableStateOf(utils.getHTTPGetVoiceURL())
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

                    "httpGetVoiceURL" -> {
                        setHTTPGetVoiceURL(utils.getHTTPGetVoiceURL())
                    }

                    "announcementLibrary" -> {
                        setAnnouncementLibrary(prefs.getString(key, "") ?: "")
                        setCnVoiceCount(utils.getLibVoiceCount("cn"))
                        setEnVoiceCount(utils.getLibVoiceCount("en"))
                        val customLangList = utils.getLangList()
                        customLangList.remove("cn")
                        customLangList.remove("en")
                        setCustomLangListStr(customLangList.joinToString(" "))
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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.announcement_source))
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
                                Text(
                                    "本地语音",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 4.dp)
                                )
                                LibraryItem(
                                    announcementLibrary,
                                    cnVoiceCount,
                                    enVoiceCount,
                                    customLangListStr
                                )

                                Text(
                                    "系统文字转语音（TTS）",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 4.dp)
                                )
                                TTSItem(useTTS, setUseTTS)
                                if (useTTS) {
                                    TTSSetting()
                                }

                                Text(
                                    "网络下载",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 4.dp)
                                )
                                EnableHTTPGetVoiceItem(
                                    enableHTTPGetVoiceItem,
                                    setEnableHTTPGetVoiceItem
                                )
                                if (enableHTTPGetVoiceItem) {
                                    HTTPGetVoiceURLItem(httpGetVoiceURL)
                                }
                            }
                        }
                    })

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
            "当前语音库：${announcementLibrary}"
        }

        BaseSettingItem(
            painter = painterResource(id = R.drawable.library),
            clickFun = {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                //                val loadingDialogBinding =
                //                    DialogLoadingBinding.inflate(LayoutInflater.from(requireContext()))
                //                loadingDialogBinding.title.text = "正在加载语音库"
                //
                //                val loadingDialog = MaterialAlertDialogBuilder(
                //                    requireContext(),
                //                    R.style.CustomAlertDialogStyle
                //                )
                //                    .setView(loadingDialogBinding.root)
                //                    .show()

                val libraryList = utils.getAnnouncementLibraryList()
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("选择语音库")
                    .setSingleChoiceItems(
                        libraryList.toTypedArray(),
                        libraryList.indexOf(utils.getAnnouncementLibrary())
                    ) { dialog, which ->
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        sharedPreferences.edit {
                            putString(
                                "announcementLibrary",
                                libraryList[which]
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
                    Text(
                        "*语音库文件夹：根目录/Announcer/Media",
                        fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                        fontSize = 12.sp
                    )
                }
            },
        )
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
    fun EnableHTTPGetVoiceItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "通过GET/POST请求获取音频",
            "",
            painterResource(id = R.drawable.tts),
            {
                toggleEnableHTTPGetVoiceItem(setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleEnableHTTPGetVoiceItem(setValue, it)
                }
            },
        )
    }

    fun toggleEnableHTTPGetVoiceItem(setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("enableHTTPGetVoiceItem", it)
        }
    }

    @Composable
    fun HTTPGetVoiceURLItem(value: String) {
        BaseSettingItem(
            "通过GET/POST请求获取音频",
            """例如：
流萤
POST https://www.getFireFlyVoice.com
Authorization: key=xxx

{input: "{{text}}"}""",
            painterResource(id = R.drawable.tts),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置获取音频请求").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = true
                binding.editText.setText(value)

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()

                    if (newValue == "") {
                        utils.showMsg("请输入请求获取音频URL")
                        return@setOnClickListener
                    }

                    if (utils.getVoiceUrlKey(newValue) == "default") {
                        utils.showMsg("请在第1行输入请求名称")
                        return@setOnClickListener
                    }

                    if (!newValue.contains("{{text}}")) {
                        utils.showMsg("请将url中要合成音频的文本替换为{{text}}")
                        return@setOnClickListener
                    }

                    prefs.edit {
                        putString("httpGetVoiceURL", newValue)
                    }
                    utils.showMsg("请求获取音频URL设置成功")
                    dialog.dismiss()


                }

                binding.editText.isSingleLine = false
                binding.textInputLayout.hint = "请输入URL"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }
}

