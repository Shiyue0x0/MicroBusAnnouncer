package com.microbus.announcer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.microbus.announcer.data.Left
import com.microbus.announcer.data.Right
import com.microbus.announcer.data.StringToastBean
import com.microbus.announcer.data.TextParams
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Utils(private val context: Context) {

    var tag: String = javaClass.simpleName

    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        initHaptic()
    }

    fun getSystemProperty(propName: String): String {
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
    fun isRunHyperOS(): Boolean {
        // 米系品牌
        if (listOf("Xiaomi", "Redmi", "POCO").contains(Build.MANUFACTURER)) {
            // HyperOS
            if (getSystemProperty("ro.miui.ui.version.code").toInt() >= 816) {
                return true
            }
        }
        return false
    }

    /**
     * Toast提示
     * @param msg 提示内容
     */
    fun showMsg(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("WrongConstant")
    fun showMIUITosat(context: Context, msg: String) {
        if (isRunHyperOS()) {
            Log.d(tag, "javaClass" + javaClass.name)
            val textParams = TextParams(msg, Color.parseColor("#4CAF50"))
            val left = Left(textParams = textParams)
            val right = Right()
            val stringToastBean = StringToastBean(left, right)
            val jsonStr = Json.encodeToString(StringToastBean.serializer(), stringToastBean)

            // 创建TosatBundle
            val mBundle: Bundle = Bundle()
            mBundle.putString("package_name", javaClass.name)
//            mBundle.putString("strong_toast_category", )
            mBundle.putParcelable("target", null)
            mBundle.putString("param", null)
            mBundle.putLong("duration", 2500L)
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


    /**
     * 用Haversine公式计算两点间距离（米）
     * @param lon1 起点经度
     * @param lat1 起点纬度
     * @param lon2 终点经度
     * @param lat2 终点纬度
     */
    fun calculateDistance(
        lon1: Double,
        lat1: Double,
        lon2: Double,
        lat2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(
            Math.toRadians(
                lat2
            )
        ) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6371 * 1000 * c
    }

    /**
     * 拼接文件
     * @param addPath 需要拼接到原文件后的文件
     * @param basePath 原文件
     */
    fun jointFile(addPath: String, basePath: String) {
        val audioFile = File(addPath)
        val toFile = File(basePath)
        val `in` = FileInputStream(audioFile)
        val out = FileOutputStream(toFile, true)
        val bs = ByteArray(1024 * 4)
        var len: Int
        //先读第一个
        while (`in`.read(bs).also { len = it } != -1) {
            out.write(bs, 0, len)
        }
        `in`.close()
        out.close()
    }


    /**
     * 从设置中获取默认路线名称
     */
    fun getDefaultLineName(): String {
        return prefs.getString("defaultLineName", "")!!.toString()
    }

    /**
     * 从设置中获取是否显示底部导航栏
     */
    fun getShowBottomBar(): Boolean {
        return prefs.getBoolean("showBottomBar", true)
    }

    /**
     * 从设置中获取路线头牌刷新间隔（秒）
     */
    fun getLineHeadCardChangeTime(): Int {
        return prefs.getString("lineHeadCardChangeTime", "5")!!.toInt()
    }

    /**
     * 从设置中获取点击地图是否复制经纬度
     */
    fun getClickMapToCopyLngLat(): Boolean {
        return prefs.getBoolean("clickMapToCopyLngLat", true)
    }

    /**
     * 从设置中获取点击地图是否复制经纬度
     */
    fun getClickMapToAddStation(): Boolean {
        return prefs.getBoolean("clickMapToAddStation", false)
    }

    /**
     * 从设置中获取点击定位按钮是否复制经纬度
     */
    fun getClickLocationButtonToCopyLngLat(): Boolean {
        return prefs.getBoolean("clickLocationButtonToCopyLngLat", false)
    }


    /**
     * 从设置中获取是否开启高精度定位
     */
    fun getHighPrecisionLocation(): Boolean {
        return prefs.getBoolean("highPrecisionLocation", true)
    }

    /**
     * 从设置中获取是否寻访路线中所有站点
     */
    fun getFindAllLineStation(): Boolean {
        return prefs.getBoolean("findAllLineStation", true)
    }

    /**
     * 从设置中获取是否自动切换路线方向
     */
    fun getAutoSwitchLineDirection(): Boolean {
        return prefs.getBoolean("AutoSwitchLineDirection", true)
    }

    /**
     * 从设置中获取站点判定距离
     */
    fun getArriveStationDistance(): Double {
        return prefs.getString("arriveStationDistance", "15.0")!!.toDouble()
    }

    /**
     * 从设置中获取定位间隔（毫秒）
     */
    fun getLocationInterval(): Long {
        return prefs.getString("locationInterval", "1000")!!.toLong()
    }

    /**
     * 从设置中获取欢迎信息
     * @param index 0:左侧信息 1：右侧信息
     */
    fun getWelInfo(index: Int): String {
        return when (index) {
            0 -> prefs.getString("welInfoLeft", "MicroBus")!!.toString()
            1 -> prefs.getString("welInfoRight", "Announcer")!!.toString()
            else -> ""
        }
    }

    /**
     * 从设置中获取头屏显示信息
     */
    fun getHeadSignShowInfo(): MutableSet<String>? {
        return prefs.getStringSet(
            "headSignShowInfo",
            setOf("0", "1", "2", "3")
        )
    }


    /**
     * 从设置中获取地图站点显示方式
     */
    fun getMapStationShowType(): Int {
        return prefs.getString("mapStationShowType", "1")!!.toInt()
    }

    /**
     * 从设置中获取站点变更是否震动
     */
    private fun getStationChangeHaptic(): Boolean {
        return prefs.getBoolean("stationChangeVibrator", true)
    }

    /**
     * 从设置中获取是否普通话播报
     */
    fun getVoiceAnnouncements(): Boolean {
        return prefs.getBoolean("voiceAnnouncements", true)
    }

    /**
     * 从设置中获取是否英语播报
     */
    fun getEnVoiceAnnouncements(): Boolean {
        return prefs.getBoolean("enVoiceAnnouncements", true)
    }

    /**
     * 从设置中获取是否启用线路轨迹纠偏
     */
    fun getLineTrajectoryCorrection(): Boolean {
        return prefs.getBoolean("lineTrajectoryCorrection", true)
    }

    /**
     * 从设置中获取是否使用TTS
     */
    fun getUseTTS(): Boolean {
        return prefs.getBoolean("useTTS", true)
    }

    /**
     * 初始化振动
     * @return 设备是否支持振动
     */
    private fun initHaptic() {

    }

    /**
     * 振动
     */
    fun haptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * 长振动
     */
    fun longHaptic() {
        val vibrator = context.getSystemService(Vibrator::class.java)

        val timings: LongArray = longArrayOf(100, 100, 100, 100, 100)
        val amplitudes: IntArray = intArrayOf(255, 192, 129, 66, 0)
        val repeatIndex = -1

        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeatIndex))
    }

}