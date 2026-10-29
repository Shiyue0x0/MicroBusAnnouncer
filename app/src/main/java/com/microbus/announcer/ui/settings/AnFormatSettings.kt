package com.microbus.announcer.ui.settings

import android.content.Intent
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
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.ui.AnFormatEditorActivity
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.compose.BaseSettingItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class AnFormatSettings : Fragment() {

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

        return composeView
    }


    @Composable
    @Preview
    fun MainView() {

        val customLangList = utils.getLangList()
        customLangList.remove("cn")
        customLangList.remove("en")

        val (serviceLanguageStr, setServiceLanguageStr) = remember {
            mutableStateOf(utils.getServiceLanguageStr())
        }

        val anFormatArrayOri = Array(3) { Array(4) { "" } }

        val stationStateList = utils.getStationStateList()
        val stationTypeList = utils.getStationPosotionList()

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
                        val customLangList = utils.getLangList()
                        customLangList.remove("cn")
                        customLangList.remove("en")
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
            color = colorResource(R.color.md_theme_surface)
        ) {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(
                controller = controller
            ) {
                Scaffold(
                    topBar = {
                        AnSmallTopAppBar(requireActivity(), getString(R.string.anFormat))
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
                                ServiceLanguageItem(serviceLanguageStr)
                                AnFormatGroup(anFormatArray)
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    })

            }
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
    fun AnFormatGroup(anFormatArray: Array<Array<String>>) {
        val stationStateList = utils.getStationStateList()
        val stationPositionList = utils.getStationPosotionList()

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
                for (position in stationPositionList) {
                    val positionStr = when (position) {
                        "Default" -> "默认"
                        "Starting" -> "起点站"
                        "Second" -> "第二站"
                        "Terminal" -> "终点站"
                        else -> ""
                    }

                    val sourceFormat =
                        anFormatArray[stationStateList.indexOf(state)][stationPositionList.indexOf(
                            position
                        )]

                    val displayFormat = addLineNumbers(sourceFormat)

                    BaseSettingItem(
                        positionStr,
                        displayFormat,
                        painterResource(id = R.drawable.format),
                        {

                            if (!utils.isGrantManageFilesAccessPermission()) {
                                utils.requestManageFilesAccessPermission(requireActivity())
                                return@BaseSettingItem
                            }

                            val intent =
                                Intent(
                                    requireContext(),
                                    AnFormatEditorActivity::class.java
                                )
                            // stationState
                            intent.putExtra("state", state)
                            // stationType
                            intent.putExtra("position", position)
                            // sourceFormat
                            intent.putExtra("sourceFormat", sourceFormat)
                            startActivity(intent)
                        },
                    )
                }
            }
        }
    }

    fun addLineNumbers(text: String): String {
        val lines = text.lines()
        if (lines.size <= 1) return text
        return lines.mapIndexed { index, line -> "${index + 1}. $line" }
            .joinToString("\n")
    }

}

