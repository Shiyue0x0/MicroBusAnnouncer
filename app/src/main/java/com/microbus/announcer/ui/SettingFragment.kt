package com.microbus.announcer.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.microbus.announcer.activity.FragmentContainerActivity
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.settings.AboutSettings
import com.microbus.announcer.ui.settings.AnFormatSettings
import com.microbus.announcer.ui.settings.AnnouncementLibrarySettings
import com.microbus.announcer.ui.settings.DataSettings
import com.microbus.announcer.ui.settings.ESSettings
import com.microbus.announcer.ui.settings.LocationSettings
import com.microbus.announcer.ui.settings.MapSettings
import com.microbus.announcer.ui.settings.SystemSettings
import com.microbus.announcer.ui.settings.VoiceBroadcastSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class SettingFragment : Fragment() {

    private lateinit var utils: Utils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {

        //获取Utils
        utils = Utils(requireContext())
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
                        navigationIcon = {
                            IconButton(onClick = {
                                val intent = Intent()
                                    .setAction(utils.backHomeName)
                                LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(intent)
                            }) {
                                Icon(MiuixIcons.Home, contentDescription = "返回主控")
                            }
                        },
                    )
                },
                content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                                bottom = 16.dp,
                                start = 16.dp,
                                end = 16.dp,
                            )
                            .fillMaxSize()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                cornerRadius = 16.dp
                            ) {
                                val announcementPreferences = listOf(
                                    ArrowPreferenceItem(
                                        title = getString(R.string.announcement_library),
                                        fragmentClass = AnnouncementLibrarySettings::class.java
                                    ),
                                    ArrowPreferenceItem(
                                        title = getString(R.string.voice_broadcast),
                                        fragmentClass = VoiceBroadcastSettings::class.java
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
                                cornerRadius = 16.dp
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
                                cornerRadius = 16.dp
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
                                        title = getString(R.string.about_an),
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