package com.microbus.announcer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.microbus.announcer.R

@Composable
fun BaseSettingItem(
    title: String = "",
    text: String = "",
    icon: Painter? = null,
    clickFun: () -> Unit = {},
    isShowIcon: Boolean = true,
    function: @Composable () -> Unit = {}
) {
//        Surface(
//            contentColor = colorResource(R.color.md_theme_onSurface),
//            color = colorResource(R.color.an_window_bg)
//        ) {
    Box(
        modifier = Modifier
//                        .padding(16.dp, 0.dp, 16.dp, 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable() {
                clickFun()
            }) {
        Column(
            modifier = Modifier
                .background(colorResource(R.color.an_contain_bg))
                .padding(16.dp)

        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isShowIcon) Column {
                    if (icon != null) {
                        Icon(
                            painter = icon,
                            contentDescription = title,
                            modifier = Modifier.height(24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (title != "") Text(
                        title,
                        fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                        fontSize = 16.sp
                    )
                    if (text != "") {
                        val lines = text.split('\n')
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {  // 段间距
                            for (line in lines) {
                                Text(
                                    text = line,
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    color = colorResource(R.color.an_text_1),
                                    fontSize = 14.sp,
                                    style = LocalTextStyle.current.merge(
                                        TextStyle(
                                            lineHeight = 1.2.em,
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            ),
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    function()
                }
            }
        }
    }
//        }
}