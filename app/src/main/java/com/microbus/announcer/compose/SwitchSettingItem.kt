package com.microbus.announcer.compose

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.microbus.announcer.R

@Composable
fun SwitchSettingItem(checked: Boolean, onCheckedChange: ((Boolean) -> Unit) = {}) {
    Switch(
        checked, onCheckedChange, colors = SwitchDefaults.colors(
            checkedThumbColor = colorResource(R.color.md_theme_primary),
            checkedTrackColor = colorResource(R.color.md_theme_primaryContainer),
            uncheckedThumbColor = colorResource(R.color.md_theme_secondary),
            uncheckedTrackColor = colorResource(R.color.md_theme_secondaryContainer),
        )
    )
}