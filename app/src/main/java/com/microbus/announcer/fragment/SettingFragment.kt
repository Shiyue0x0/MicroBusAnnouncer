package com.microbus.announcer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import com.microbus.announcer.activity.FragmentContainerActivity
import com.microbus.announcer.R
import com.microbus.announcer.fragment.settings.AboutSettings
import com.microbus.announcer.fragment.settings.AnFormatSettings
import com.microbus.announcer.fragment.settings.AnnouncementLibrarySettings
import com.microbus.announcer.fragment.settings.DataSettings
import com.microbus.announcer.fragment.settings.ESSettings
import com.microbus.announcer.fragment.settings.LocationSettings
import com.microbus.announcer.fragment.settings.MapSettings
import com.microbus.announcer.fragment.settings.SystemSettings
import com.microbus.announcer.fragment.settings.VoiceBroadcastSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        MainUI()
    }


    @Composable
    @Preview
    fun MainUI() {

        data class ArrowPreferenceItem(
            val title: String = "",
            val summary: String = "",
            val fragmentClass: Class<out Fragment>
        )

        val controller = remember { ThemeController(ColorSchemeMode.System) }

        MiuixTheme(
            controller = controller
        ) {
            Scaffold(
                topBar = {
                    SmallTopAppBar(
                        title = getString(R.string.nav_setting),
                    )
                },
                content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(top = paddingValues.calculateTopPadding())
                            .fillMaxSize()
                    ) {
                        Column {
                            val cardModifier = Modifier.padding(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 0.dp
                            )
                            Card(
                                cornerRadius = 16.dp,
                                modifier = cardModifier
                            ) {
                                val mainPreferences = listOf(
                                    ArrowPreferenceItem(
                                        title = getString(R.string.es),
                                        fragmentClass = ESSettings::class.java
                                    ),

                                    ArrowPreferenceItem(
                                        title = getString(R.string.map),
                                        fragmentClass = MapSettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.location),
                                        fragmentClass = LocationSettings::class.java
                                    )
                                )
                                Column {
                                    mainPreferences.forEach { item ->
                                        ArrowPreference(
                                            title = item.title,
                                            onClick = {
                                                FragmentContainerActivity.start(
                                                    requireContext(),
                                                    item.fragmentClass
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            Card(
                                cornerRadius = 16.dp,
                                modifier = cardModifier

                            ) {
                                val announcementPreferences = listOf(
                                    ArrowPreferenceItem(
                                        title = getString(R.string.voice_broadcast),
                                        fragmentClass = VoiceBroadcastSettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.announcement_library),
                                        fragmentClass = AnnouncementLibrarySettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.anFormat),
                                        fragmentClass = AnFormatSettings::class.java
                                    ),
                                )
                                Column {
                                    announcementPreferences.forEach { item ->
                                        ArrowPreference(
                                            title = item.title,
                                            onClick = {
                                                FragmentContainerActivity.start(
                                                    requireContext(),
                                                    item.fragmentClass
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            Card(
                                cornerRadius = 16.dp,
                                modifier = cardModifier

                            ) {
                                val otherPreferences = listOf(
                                    ArrowPreferenceItem(
                                        title = getString(R.string.system),
                                        fragmentClass = SystemSettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.stationAndLineDate),
                                        fragmentClass = DataSettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.about),
                                        fragmentClass = AboutSettings::class.java
                                    )
                                )
                                Column {
                                    otherPreferences.forEach { item ->
                                        ArrowPreference(
                                            title = item.title,
                                            onClick = {
                                                FragmentContainerActivity.start(
                                                    requireContext(),
                                                    item.fragmentClass
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                        }
                    }

                }

            )


        }
    }

}