package com.microbus.announcer.fragment.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem


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

        return composeView
    }

    @Composable
    @Preview
    fun MainView() {

        val (announcementLibrary, setAnnouncementLibrary) = remember {
            mutableStateOf(utils.getAnnouncementLibrary())
        }

        val (libraryPath, setLibraryPath) = remember { mutableStateOf("") }
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

        val customLangList = utils.getAnnouncementLangList()
        customLangList.remove("cn")
        customLangList.remove("en")
        val (customLangListStr, setCustomLangListStr) = remember {
            mutableStateOf(
                customLangList.joinToString(" ")
            )
        }


        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "announcementLibrary" -> {
                        setAnnouncementLibrary(prefs.getString(key, "") ?: "")
                        setCnVoiceCount(utils.getLibVoiceCount("cn"))
                        setEnVoiceCount(utils.getLibVoiceCount("en"))
                        val customLangList = utils.getAnnouncementLangList()
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
        }
        else{
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
                                .background(colorResource(R.color.an_window_bg))
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
                                .background(colorResource(R.color.an_window_bg))
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
                            .background(colorResource(R.color.an_window_bg))
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

}

