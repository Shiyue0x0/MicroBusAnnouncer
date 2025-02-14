package com.microbus.announcer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import com.microbus.announcer.data.IconParams
import com.microbus.announcer.data.Left
import com.microbus.announcer.data.Right
import com.microbus.announcer.data.StringToastBean
import com.microbus.announcer.data.TextParams
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object MiuiStringToast {

    var tag: String = javaClass.simpleName

    object Category {
        const val RAW = "raw"
        const val DRAWABLE = "drawable"
        const val FILE = "file"
        const val MIPMAP = "mipmap"
    }

    object FileType {
        const val MP4 = "mp4"
        const val PNG = "png"
        const val SVG = "svg"
    }

    object StrongToastCategory {
        const val VIDEO_TEXT = "video_text"
        const val VIDEO_BITMAP_INTENT = "video_bitmap_intent"
        const val TEXT_BITMAP = "text_bitmap"
        const val TEXT_BITMAP_INTENT = "text_bitmap_intent"
        const val VIDEO_TEXT_TEXT_VIDEO = "video_text_text_video"
    }

    private fun getSystemProperty(propName: String): String {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return ""
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (_: IOException) {
                }
            }
        }
        return line
    }

    /**
     * 设备是否运行HyperOS
     * @return 设备是否运行HyperOS
     */
    private fun isRunHyperOS(): Boolean {
        // 米系品牌
        if (listOf("Xiaomi", "Redmi", "POCO").contains(Build.MANUFACTURER)) {
            // HyperOS
            if (getSystemProperty("ro.miui.ui.version.code").toInt() >= 816) {
                return true
            }
        }
        return false
    }


    @SuppressLint("WrongConstant", "InlinedApi")
    fun show(context: Context, msgLeft: String) {
        if (isRunHyperOS()) {
            val iconParams = IconParams(Category.DRAWABLE, FileType.SVG, "ic_launcher", 1)
            val textParams = TextParams(msgLeft, Color.parseColor("#35EE80"))
            val left = Left(textParams = textParams)
            val right = Right(iconParams = iconParams)
            val stringToastBean = StringToastBean(left, right)
            val jsonString = Json.encodeToString(StringToastBean.serializer(), stringToastBean)

            // 创建TosatBundle
            val mBundle = Bundle()
            mBundle.putString("package_name", javaClass.name)
            mBundle.putString("strong_toast_category", StrongToastCategory.TEXT_BITMAP_INTENT)
            mBundle.putParcelable(
                "target",
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            mBundle.putString("param", jsonString)
            mBundle.putLong("duration", 5000L)
            mBundle.putFloat("level", 0f)
            mBundle.putFloat("rapid_rate", 0f)
            mBundle.putString("charge", null)
            mBundle.putInt("string_toast_charge_flag", 0)
            mBundle.putString("status_bar_strong_toast", "show_custom_strong_toast")

            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus",
                Int::class.javaPrimitiveType,
                String::class.java,
                Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", mBundle)

        }
    }

}