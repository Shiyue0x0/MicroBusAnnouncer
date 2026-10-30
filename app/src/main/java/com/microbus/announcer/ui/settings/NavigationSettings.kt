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

class NavigationSettings : Fragment() {

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


        //启用导航
        val (isEnableOnlineNavigation, setIsEnableOnlineNavigation) = remember {
            mutableStateOf(utils.getIsEnableOnlineNavigation())
        }

        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->

                when (key) {
                    "enableOnlineNavigation" -> {
                        setIsEnableOnlineNavigation(utils.getIsEnableOnlineNavigation())
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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.navigation))
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
                                EnableOnlineNavigationItem(
                                    isEnableOnlineNavigation,
                                    setIsEnableOnlineNavigation
                                )
                                if (isEnableOnlineNavigation) {

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
    fun EnableOnlineNavigationItem(
        isEnableOnlineNavigation: Boolean,
        setIsEnableOnlineNavigation: (Boolean) -> Unit
    ) {
        BaseSettingItem(
            "启用在线导航",
            "",
            painterResource(id = R.drawable.text),
            {
                toggleEnableOnlineNavigation(
                    setIsEnableOnlineNavigation,
                    !isEnableOnlineNavigation
                )
            },
            rightContain = {
                SwitchSettingItem(isEnableOnlineNavigation) {
                    toggleEnableOnlineNavigation(setIsEnableOnlineNavigation, it)
                }
            },
        )
    }

    fun toggleEnableOnlineNavigation(
        setIsEnableOnlineNavigation: (Boolean) -> Unit,
        it: Boolean
    ) {
        setIsEnableOnlineNavigation(it)
        utils.setIsEnableOnlineNavigation(it)
    }

}

