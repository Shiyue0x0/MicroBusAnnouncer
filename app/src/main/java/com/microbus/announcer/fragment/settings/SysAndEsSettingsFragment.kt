package com.microbus.announcer.fragment.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.MainActivity
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem
import com.microbus.announcer.compose.SwitchSettingItem
import com.microbus.announcer.databinding.AlertDialogInputBinding


class SysAndEsSettingsFragment : Fragment() {

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

        val (lang, setLang) = remember {
            mutableStateOf(prefs.getString("lang", "auto") ?: "auto")
        }

        val (city, setCity) = remember {
            mutableStateOf(utils.getCity())
        }

        val (nav, setNav) = remember {
            mutableStateOf(utils.getIsShowBottomBar())
        }

        val (esText, setEsText) = remember {
            mutableStateOf(utils.getEsText())
        }

        val (esNextWord, setEsNextWord) = remember {
            mutableStateOf(utils.getEsNextWord())
        }

        val (esWillArriveWord, setEsWillArriveWord) = remember {
            mutableStateOf(utils.getEsWillArriveWord())
        }

        val (esArriveWord, setEsArriveWord) = remember {
            mutableStateOf(utils.getEsArriveWord())
        }

        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "lang" -> setLang(prefs.getString(key, "") ?: "")
                    "city" -> setCity(prefs.getString(key, "") ?: "")
                    "showBottomBar" -> setNav(prefs.getBoolean(key, true))
                    "esText" -> setEsText(prefs.getString(key, "") ?: "")
                    "esNextWord" -> setEsNextWord(prefs.getString(key, "") ?: "")
                    "esWillArriveWord" -> setEsWillArriveWord(prefs.getString(key, "") ?: "")
                    "esArriveWord" -> setEsArriveWord(prefs.getString(key, "") ?: "")
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
                        UiLangItem(lang)
                        CityNameItem(city)
                        NavItem(nav, setNav)
                        ESTextItem(esText)
                        EsKeywordItemGroup(esNextWord, esWillArriveWord, esArriveWord)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }





    @Composable
    fun UiLangItem(lang: String) {
        val nameList = listOf("跟随系统", "简体中文", "English")
        val valueList = listOf("auto", "zh", "en")
        val currentChooseIndex = valueList.indexOf(prefs.getString("lang", "auto"))

        BaseSettingItem(
            "界面语言", nameList[valueList.indexOf(lang)], painterResource(id = R.drawable.lang), {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择界面语言").setSingleChoiceItems(
                    nameList.toTypedArray(), currentChooseIndex
                ) { dialog, which ->
                    prefs.edit {
                        putString("lang", valueList[which])
                    }
                    utils.setUILang(valueList[which])
                    dialog.cancel()
                }.show()
            }) {

        }
    }

    @Composable
    fun CityNameItem(city: String) {
        BaseSettingItem(
            "搜索城市", city, painterResource(id = R.drawable.city), {
                val binding = AlertDialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置城市").setView(binding.root).setPositiveButton("确定", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newValue = binding.editText.text.toString()
                    if (newValue == "") {
                        utils.showMsg("请输入城市名称")
                        return@setOnClickListener
                    }
                    prefs.edit {
                        putString("city", newValue)
                    }
                    utils.showMsg("已将城市设置为${newValue}")
                    dialog.dismiss()
                }

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            })
    }

    @Composable
    fun NavItem(nav: Boolean, setNav: (Boolean) -> Unit) {
        BaseSettingItem(
            "导航栏", "底部导航栏", painterResource(id = R.drawable.bottom_nav), {
                toggleNav(nav, setNav, !nav)
            }) {
            SwitchSettingItem(nav) {
                toggleNav(nav, setNav, it)
            }
        }
    }

    fun toggleNav(nav: Boolean, setNav: (Boolean) -> Unit, it: Boolean) {
        setNav(it)
        prefs.edit {
            putBoolean("showBottomBar", it)
        }
        val activity = requireActivity() as MainActivity
        if (it) {
            activity.binding.bottomNavigationView.visibility = View.VISIBLE
        } else {
            activity.binding.bottomNavigationView.visibility = View.GONE
            utils.showMsg("现在请尝试左右滑动来切换界面")
        }
    }

    @Composable
    fun ESTextItem(eSText: String) {
        BaseSettingItem(
            "电显内容", eSText, painterResource(id = R.drawable.text), {
                val binding = AlertDialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("编辑电显内容").setView(binding.root).setPositiveButton("确定", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = false
                binding.editText.setText(eSText)
                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()
                    val esList = utils.getEsList(newValue)

                    if (esList.isNotEmpty() && esList.first().minTimeS < 0) {
                        utils.showMsg(esList.first().leftText)
                        return@setOnClickListener
                    } else {
                        prefs.edit {
                            putString("esText", newValue)
                        }
                        for (es in esList) Log.d(
                            "es",
                            "${es.leftText} + ${es.rightText}(${es.minTimeS})"
                        )
                        utils.showMsg("设置成功，本次轮播完毕后切换")
                    }
                    dialog.dismiss()
                }

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            })
    }

    @Composable
    fun EsKeywordItemGroup(esNextWord: String, esWillArriveWord: String, esArriveWord: String) {

        Column {
            Text(
                "电显提示词",
                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("下一站", "esNextWord", esNextWord)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("即将到站", "esWillArriveWord", esWillArriveWord)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("到站", "esArriveWord", esArriveWord)
                }
            }
        }

    }

    @Composable
    fun EsKeywordItem(title: String, key: String, value: String) {
        val keyName = when (key) {
            "esNextWord" -> "<next>"
            "esWillArriveWord" -> "<will>"
            "esArriveWord" -> "<arrive>"
            else -> ""
        }
        BaseSettingItem(
            title,
            value + "\n${keyName}",
            painterResource(id = R.drawable.city),
            isShowIcon = false,
            clickFun = {
                val binding = AlertDialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置${title}提示词")
                    .setView(binding.root)
                    .setPositiveButton("确定", null)
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .show()

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newValue = binding.editText.text.toString()
                    if (newValue == "") {
                        utils.showMsg("请输入${title}提示词")
                        return@setOnClickListener
                    }
                    prefs.edit {
                        putString(key, newValue)
                    }
                    utils.showMsg("${title}提示词：${newValue}")
                    dialog.dismiss()
                }

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            })
    }


}

