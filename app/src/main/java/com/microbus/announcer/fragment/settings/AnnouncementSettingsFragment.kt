package com.microbus.announcer.fragment.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.microbus.announcer.MainActivity
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem
import com.microbus.announcer.compose.SwitchSettingItem
import com.microbus.announcer.databinding.AlertDialogInputBinding


class AnnouncementSettingsFragment : Fragment() {

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
        val (cnVoiceCount, setCnVoiceCount) = remember { mutableIntStateOf(0) }
        val (enVoiceCount, setEnVoiceCount) = remember { mutableIntStateOf(0) }


        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "announcementLibrary" -> setAnnouncementLibrary(prefs.getString(key, "") ?: "")
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

                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

}

