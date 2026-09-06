package com.microbus.announcer.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RichTooltip
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Help

@Composable
fun NavHelpBox(textList: List<String>) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                modifier = Modifier.padding(16.dp),
                title = {
                    textList.forEach { text ->
                        Text(text)
                    }
                },
                action = {
                    TextButton(
                        text = "关闭",
                        onClick = { tooltipState.dismiss() }
                    )
                },
            ) {}
        },
        state = tooltipState,
        focusable = true,
    ) {
        IconButton(onClick = { scope.launch { tooltipState.show() } }) {
            Icon(MiuixIcons.Help, contentDescription = "帮助")
        }
    }
}