package com.microbus.announcer.ui.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun AnSmallTopAppBar(activity: Activity, title: String = "") {
    SmallTopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = {
                activity.finish()
            }) {
                Icon(MiuixIcons.Back, contentDescription = "返回")
            }
        }
    )
}