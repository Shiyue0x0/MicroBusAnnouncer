package com.microbus.announcer.ui.settings

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.microbus.announcer.TranslationService
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.time.Duration.Companion.milliseconds

class TranslationSettings : Fragment() {

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

        val (isEnableOnlineTranslation, setIsEnableOnlineTranslation) = remember {
            mutableStateOf(utils.getIsEnableOnlineTranslation())
        }

        val (onlineTranslationRequestCode, setOnlineTranslationRequestCode) = remember {
            mutableStateOf(utils.getOnlineTranslationRequestCode())
        }

        val (onlineTranslationResponseJSONPath, setOnlineTranslationResponseJSONPath) = remember {
            mutableStateOf(utils.getOnlineTranslationResponseJSONPath())
        }

        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->

                when (key) {
                    "enableOnlineTranslation" -> {
                        setIsEnableOnlineTranslation(utils.getIsEnableOnlineTranslation())
                    }

                    "onlineTranslationRequestCode" -> {
                        setOnlineTranslationRequestCode(utils.getOnlineTranslationRequestCode())
                    }

                    "onlineTranslationResponseJSONPath" -> {
                        setOnlineTranslationResponseJSONPath(utils.getOnlineTranslationResponseJSONPath())
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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.translation))
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                //TODO
                                androidx.compose.material3.Text(
                                    "中文 -> 英文",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 4.dp, 0.dp, 0.dp)
                                )
                                EnableOnlineTranslationItem(
                                    isEnableOnlineTranslation,
                                    setIsEnableOnlineTranslation
                                )
                                if (isEnableOnlineTranslation) {
                                    OnlineTranslationRequestCodeItem(onlineTranslationRequestCode)
                                    OnlineTranslationResponseJSONPathItem(
                                        onlineTranslationResponseJSONPath
                                    )
                                    ViewTranslationCacheItem()
                                    ClearTranslationCacheItem()
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                        }

                    }
                )

            }
        }
    }

    @Composable
    fun EnableOnlineTranslationItem(
        isEnableOnlineTranslation: Boolean,
        setIsEnableOnlineTranslation: (Boolean) -> Unit
    ) {
        BaseSettingItem(
            "启用在线翻译",
            "未设置站点英文名称时",
            painterResource(id = R.drawable.text),
            {
                toggleEnableOnlineTranslation(
                    setIsEnableOnlineTranslation,
                    !isEnableOnlineTranslation
                )
            },
            rightContain = {
                SwitchSettingItem(isEnableOnlineTranslation) {
                    toggleEnableOnlineTranslation(setIsEnableOnlineTranslation, it)
                }
            },
        )
    }

    fun toggleEnableOnlineTranslation(
        setIsEnableOnlineTranslation: (Boolean) -> Unit,
        it: Boolean
    ) {
        setIsEnableOnlineTranslation(it)
        prefs.edit {
            putBoolean("enableOnlineTranslation", it)
        }
    }


    @Composable
    fun OnlineTranslationRequestCodeItem(value: String) {
        var showDialog by remember { mutableStateOf(false) }

        BaseSettingItem(
            "翻译API", value, painterResource(id = R.drawable.lang),
            { showDialog = true }
        )

        if (showDialog) {
            var input by remember(value) { mutableStateOf(value) }

            OverlayDialog(
                title = "设置翻译API",
                show = true,
                onDismissRequest = { showDialog = false },
                defaultWindowInsetsPadding = false
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "保存",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val newValue = input.trim()

                            if (newValue.isEmpty()) {
                                utils.showMsg("请输入翻译API")
                                return@TextButton
                            }

                            if (!newValue.contains("{{text}}")) {
                                utils.showMsg("请将API中要翻译的文本替换为{{text}}")
                                return@TextButton
                            }

                            prefs.edit {
                                putString("onlineTranslationRequestCode", newValue)
                            }
                            utils.showMsg("翻译API设置成功")
                            showDialog = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun OnlineTranslationResponseJSONPathItem(value: String) {
        var showDialog by remember { mutableStateOf(false) }

        BaseSettingItem(
            "翻译结果JSONPath", value, painterResource(id = R.drawable.lang),
            { showDialog = true }
        )

        if (showDialog) {
            var input by remember(value) { mutableStateOf(value) }

            OverlayDialog(
                title = "设置翻译结果JSONPath",
                show = true,
                onDismissRequest = { showDialog = false },
                defaultWindowInsetsPadding = false
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "保存",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val newValue = input.trim()

                            if (newValue.isEmpty()) {
                                utils.showMsg("请输入翻译结果JSONPath")
                                return@TextButton
                            }

                            prefs.edit {
                                putString("onlineTranslationResponseJSONPath", newValue)
                            }
                            utils.showMsg("翻译结果JSONPath设置成功")
                            showDialog = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ViewTranslationCacheItem() {
        var showDialog by remember { mutableStateOf(false) }
        var editingSourceText by remember { mutableStateOf<String?>(null) }
        var refreshKey by remember { mutableIntStateOf(0) }
        val translationService = TranslationService.getInstance(requireContext())

        BaseSettingItem(
            "查看翻译缓存", "", painterResource(id = R.drawable.lang),
            clickFun = {
                if (translationService.getAllCache().isEmpty()) {
                    utils.showMsg("暂无翻译缓存")
                } else {
                    showDialog = true
                }
            }
        )

        val cache = remember(refreshKey, showDialog) {
            translationService.getAllCache()
        }

        if (showDialog) {
            TranslationCacheDialog(
                entries = cache.entries.toList(),
                onDismiss = { showDialog = false },
                onEdit = { sourceText ->
                    editingSourceText = sourceText
                }
            )
        }

        editingSourceText?.let { sourceText ->
            EditTranslationDialog(
                translationService = translationService,
                sourceText = sourceText,
                onDismiss = { editingSourceText = null },
                onSaved = {
                    editingSourceText = null
                    refreshKey++
                }
            )
        }
    }

    @Composable
    private fun TranslationCacheDialog(
        entries: List<Map.Entry<String, String>>,
        onDismiss: () -> Unit,
        onEdit: (String) -> Unit
    ) {
        // 输入框中的即时值
        var searchInput by remember { mutableStateOf("") }
        // 实际用于过滤的关键字
        var searchKeyword by remember { mutableStateOf("") }
        // 上次执行搜索的时间戳
        var lastSearchTime by remember { mutableLongStateOf(0L) }

        // 节流搜索：距上次搜索 >= 1 秒则立即搜索，否则等剩余时间后再搜索
        LaunchedEffect(searchInput) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastSearchTime
            if (elapsed < 1000L) {
                delay((1000L - elapsed).milliseconds)
            }
            lastSearchTime = System.currentTimeMillis()
            searchKeyword = searchInput.trim()
        }

        // 按原文/译文模糊过滤；关键字为空时返回全部
        val filteredEntries = remember(entries, searchKeyword) {
            if (searchKeyword.isBlank()) {
                entries
            } else {
                entries.filter { (sourceText, translated) ->
                    sourceText.contains(searchKeyword, ignoreCase = true) ||
                            translated.contains(searchKeyword, ignoreCase = true)
                }
            }
        }

        OverlayDialog(
            title = "翻译缓存（共 ${filteredEntries.size} 条）",
            show = true,
            onDismissRequest = onDismiss
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "搜索原文或译文"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    if (filteredEntries.isEmpty()) {
                        Text(
                            text = if (searchKeyword.isBlank()) "暂无翻译缓存" else "未找到匹配的缓存",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredEntries, key = { it.key }) { (sourceText, translated) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable { onEdit(sourceText) }
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = sourceText,
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = translated,
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    text = "关闭",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }


    @Composable
    private fun EditTranslationDialog(
        translationService: TranslationService,
        sourceText: String,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        val currentTranslation = translationService.getAllCache()[sourceText] ?: ""
        var input by remember(sourceText) { mutableStateOf(currentTranslation) }

        OverlayDialog(
            title = "编辑译文：$sourceText",
            show = true,
            onDismissRequest = onDismiss,
            // 关键修改：关闭默认的窗口插入处理
            defaultWindowInsetsPadding = false
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // 可选：给内容底部增加一点间距，视觉上更舒适
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "保存",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val newValue = input.trim()

                        if (newValue.isEmpty()) {
                            utils.showMsg("译文不能为空")
                            return@TextButton
                        }

                        if (translationService.updateCacheEntry(sourceText, newValue)) {
                            utils.showMsg("译文已更新")
                            onSaved()
                        } else {
                            utils.showMsg("更新失败：该缓存可能已被移除")
                            onDismiss()
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ClearTranslationCacheItem() {
        BaseSettingItem(
            "清除翻译缓存", "", painterResource(id = R.drawable.lang),
            {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                )
                    .setTitle("清除翻译缓存")
                    .setMessage("确定要清除所有翻译缓存吗？此操作不可撤销。")
                    .setPositiveButton("清除") { _, _ ->
                        val translationService = TranslationService.getInstance(requireContext())
                        translationService.clearCache()
                        utils.showMsg("翻译缓存已清除")
                    }
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .show()
            },
        )
    }


}