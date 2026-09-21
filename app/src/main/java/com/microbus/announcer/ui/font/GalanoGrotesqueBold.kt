package com.microbus.announcer.ui.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.microbus.announcer.R

object GalanoGrotesqueBold {
    fun build(): FontFamily {
        return FontFamily(
            Font(R.font.galano_grotesque_bold, FontWeight.Bold)
        )
    }
}