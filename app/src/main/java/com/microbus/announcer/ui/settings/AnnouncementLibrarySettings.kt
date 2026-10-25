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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.microbus.announcer.announce.AnnouncementProvider
import com.microbus.announcer.announce.AnnouncementProvider.NetworkMediaLoadingType
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.time.Duration.Companion.milliseconds


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

    enum class httpGetVoiceURLAction {
        Add,
        Edit
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
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 0.dp)
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
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 0.dp)
                                )
                                TTSItem(useTTS, setUseTTS)
                                if (useTTS) {
                                    TTSSetting()
                                }

                                Text(
                                    "网络下载",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 0.dp)
                                )
                                EnableHTTPGetVoiceItem(
                                    enableHTTPGetVoiceItem,
                                    setEnableHTTPGetVoiceItem
                                )
                                if (enableHTTPGetVoiceItem) {
                                    PreloadItem()
                                    HTTPGetVoiceURLItemSwitch(httpGetVoiceURL)
                                    if (httpGetVoiceURL != "") {
                                        AddOrEditHTTPGetVoiceURLItem(httpGetVoiceURLAction.Edit)
                                    }
                                    AddOrEditHTTPGetVoiceURLItem(httpGetVoiceURLAction.Add)
                                    NetworkDownloadManage()
                                }

                                Spacer(modifier = Modifier.height(8.dp))
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
            "找不到音频或下载失败时",
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
            "通过网络下载音频",
            "",
            painterResource(id = R.drawable.network),
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
    fun HTTPGetVoiceURLItemSwitch(value: String) {

        val voiceUrlKey = utils.getVoiceUrlKey(value)

        BaseSettingItem(
            "选择网络请求",
            if (value != "") "已选择：${voiceUrlKey}" else "还未选择或添加",
            painterResource(id = R.drawable.network),
            clickFun = {

                val httpGetVoiceURLArray = utils.getHTTPGetVoiceURLSet().toHashSet().toTypedArray()
                val httpGetVoiceURLKeyArray =
                    httpGetVoiceURLArray.map { it.lineSequence().firstOrNull() }.toTypedArray()

                if (httpGetVoiceURLArray.isEmpty()) {
                    utils.showMsg("请先添加网络请求")
                    return@BaseSettingItem
                }

                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择网络请求").setSingleChoiceItems(
                    httpGetVoiceURLKeyArray, -1
                ) { dialog, which ->
                    prefs.edit {
                        putString("httpGetVoiceURL", httpGetVoiceURLArray[which])
                    }
                    dialog.cancel()
                }.show()
            }
        )

    }

    @Composable
    fun AddOrEditHTTPGetVoiceURLItem(action: httpGetVoiceURLAction) {

        val actionName = if (action == httpGetVoiceURLAction.Add) "添加" else "编辑"
        val painterId = if (action == httpGetVoiceURLAction.Add) R.drawable.add else R.drawable.edit

        BaseSettingItem(
            text = "${actionName}网络请求",
            painter = painterResource(id = painterId),
            clickFun = {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val voiceUrl = utils.getHTTPGetVoiceURL()
                var voiceUrlKey = ""
                if (action == httpGetVoiceURLAction.Edit) {
                    voiceUrlKey = utils.getVoiceUrlKey(voiceUrl)
                }

                val title =
                    if (action == httpGetVoiceURLAction.Add) "添加" else "编辑：${voiceUrlKey}"

                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle(title).setView(binding.root)
                    .setPositiveButton(actionName, null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = false
                binding.editText.hint = """例如：
流萤
POST https://www.getFireFlyVoice.com
Authorization: key=xxx

{input: "{{text}}"}"""

                if (action == httpGetVoiceURLAction.Edit) {
                    binding.editText.setText(voiceUrl)
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newVoiceUrl = binding.editText.text.toString()

                    if (newVoiceUrl == "") {
                        utils.showMsg("请输入请求获取音频网络请求")
                        return@setOnClickListener
                    }

                    val newVoiceUrlKey = utils.getVoiceUrlKey(newVoiceUrl)

                    if (newVoiceUrlKey == "default") {
                        utils.showMsg("请在第1行输入请求名称")
                        return@setOnClickListener
                    }

                    if (!newVoiceUrl.contains("{{text}}")) {
                        utils.showMsg("请将请求中要合成音频的文本替换为{{text}}")
                        return@setOnClickListener
                    }


                    val httpGetVoiceURLSet = utils.getHTTPGetVoiceURLSet().toHashSet()

                    if (action == httpGetVoiceURLAction.Add) {
                        if (httpGetVoiceURLSet.contains(newVoiceUrlKey)) {
                            utils.showMsg("请求名称已存在")
                            return@setOnClickListener
                        }

                        httpGetVoiceURLSet.add(newVoiceUrl)

                        prefs.edit {
                            putStringSet("httpGetVoiceURLSet", httpGetVoiceURLSet)
                        }
                    }

                    if (action == httpGetVoiceURLAction.Edit) {

                        val oldVoiceUrl = httpGetVoiceURLSet.find {
                            it.lineSequence().firstOrNull() == voiceUrlKey
                        }

                        if (oldVoiceUrl == null) {
                            utils.showMsg("请求名称不存在")
                            return@setOnClickListener
                        }

                        httpGetVoiceURLSet.remove(oldVoiceUrl)
                        httpGetVoiceURLSet.add(newVoiceUrl)

                        prefs.edit {
                            putStringSet("httpGetVoiceURLSet", httpGetVoiceURLSet)
                            putString("httpGetVoiceURL", newVoiceUrl)
                        }
                    }

                    utils.showMsg("获取音频网络请求${actionName}成功")
                    dialog.dismiss()

                }

                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )

    }

    @Composable
    fun PreloadItem() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent()
                        .setAction(utils.preloadingCommonTextName)
                    LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(intent)
                }
            ) {
                top.yukonga.miuix.kmp.basic.Text("预加载常规文本")
            }
            Button(
                onClick = {
                    val intent = Intent()
                        .setAction(utils.preloadingStationName)
                    LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(intent)
                }
            ) {
                top.yukonga.miuix.kmp.basic.Text("预加载站点文本")
            }
        }
    }

    @Composable
    fun NetworkDownloadManage() {

        var playingDownloadTotal by remember { mutableIntStateOf(0) }
        var preloadDownloadTotal by remember { mutableIntStateOf(0) }

        var playingDownloadingText by remember { mutableStateOf("") }
        var preloadDownloadingText by remember { mutableStateOf("") }


        val anProvider = AnnouncementProvider.getInstance(requireContext())

        LaunchedEffect(Unit) {
            while (true) {
                delay(500L.milliseconds)
                playingDownloadTotal =
                    anProvider.getNetworkLoadingListTotal(NetworkMediaLoadingType.PLAYING)
                preloadDownloadTotal =
                    anProvider.getNetworkLoadingListTotal(NetworkMediaLoadingType.PRELOAD)

                playingDownloadingText =
                    anProvider.getNetworkLoadingInfo(NetworkMediaLoadingType.PLAYING)
                preloadDownloadingText =
                    anProvider.getNetworkLoadingInfo(NetworkMediaLoadingType.PRELOAD)
            }
        }

        BaseSettingItem(
            painter = painterResource(id = R.drawable.network),
            isCustomLeft = true,
            leftContain = {

                if (playingDownloadTotal + preloadDownloadTotal == 0) {
                    Text(text = "暂无语音正在下载")
                    return@BaseSettingItem
                }

                if (playingDownloadTotal > 0) {
                    Text(text = "播放中：${playingDownloadTotal} 项")
                    Text(text = playingDownloadingText)
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (preloadDownloadTotal > 0) {
                    Text(text = "预载中：${preloadDownloadTotal} 项")
                    Text(text = preloadDownloadingText)
                }
            }
        )
    }
}

