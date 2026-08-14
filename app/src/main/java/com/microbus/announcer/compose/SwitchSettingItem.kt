package com.microbus.announcer.compose


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun SwitchSettingItem(checked: Boolean, onCheckedChange: ((Boolean) -> Unit) = {}) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(
        controller = controller
    ) {
        Switch(
            checked, onCheckedChange
        )
    }
}