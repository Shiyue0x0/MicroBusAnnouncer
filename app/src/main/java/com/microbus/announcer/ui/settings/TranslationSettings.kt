package com.microbus.announcer.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.SwitchSettingItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

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
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->

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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.translation),)
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
                                    "中文 - 英文",
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
        BaseSettingItem(
            "翻译请求代码", value, painterResource(id = R.drawable.lang),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置翻译请求代码").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = false
                binding.editText.setText(value)
                binding.textInputLayout.hint = "请输入文本"

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()

                    if (newValue == "") {
                        utils.showMsg("请输入翻译请求代码")
                        return@setOnClickListener
                    }

                    if (!newValue.contains("{{text}}")) {
                        utils.showMsg("请将代码中要翻译的文本替换为{{text}}")
                        return@setOnClickListener
                    }

                    prefs.edit {
                        putString("onlineTranslationRequestCode", newValue)
                    }
                    utils.showMsg("翻译请求代码设置成功")
                    dialog.dismiss()

                }

                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }

    @Composable
    fun OnlineTranslationResponseJSONPathItem(value: String) {
        BaseSettingItem(
            "翻译结果JSONPath", value, painterResource(id = R.drawable.lang),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置翻译结果JSONPath").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = false
                binding.editText.setText(value)
                binding.textInputLayout.hint = "请输入文本"

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()

                    if (newValue == "") {
                        utils.showMsg("请输入翻译结果JSONPath")
                        return@setOnClickListener
                    }

                    prefs.edit {
                        putString("onlineTranslationResponseJSONPath", newValue)
                    }
                    utils.showMsg("翻译结果JSONPath设置成功")
                    dialog.dismiss()

                }

                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }


}

