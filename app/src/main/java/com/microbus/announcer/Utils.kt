package com.microbus.announcer

import android.content.Context
import android.content.SharedPreferences
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Utils(private val context: Context) {

    var tag: String = javaClass.simpleName

    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)


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
     * Toast提示
     * @param msg 提示内容
     */
    fun showMsg(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
     * 从设置中获取默认路线名称
     */
    fun getDefaultLineName(): String {
        return prefs.getString("defaultLineName", "")!!
    }

    /**
     * 从设置中获取是否显示底部导航栏
     */
    fun getIsShowBottomBar(): Boolean {
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
    fun getIsClickMapToCopyLngLat(): Boolean {
        return prefs.getBoolean("clickMapToCopyLngLat", true)
    }

    /**
     * 从设置中获取点击地图是否复制经纬度
     */
    fun getIsClickMapToAddStation(): Boolean {
        return prefs.getBoolean("clickMapToAddStation", false)
    }

    /**
     * 从设置中获取点击定位按钮是否复制经纬度
     */
    fun getIsClickLocationButtonToCopyLngLat(): Boolean {
        return prefs.getBoolean("clickLocationButtonToCopyLngLat", false)
    }


    /**
     * 从设置中获取是否寻访路线中所有站点
     */
    fun getIsFindAllLineStation(): Boolean {
        return prefs.getBoolean("findAllLineStation", true)
    }

    /**
     * 从设置中获取是否自动切换路线方向
     */
    fun getIsAutoSwitchLineDirection(): Boolean {
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
    fun getLocationInterval(): Int {
        return prefs.getString("locationInterval", "1000")!!.toInt()
    }

    /**
     * 从设置中获取欢迎信息
     * @param index 0:左侧信息 1：右侧信息
     */
    fun getWelInfo(index: Int): String {
        return when (index) {
            0 -> prefs.getString("welInfoLeft", "MicroBus")!!
            1 -> prefs.getString("welInfoRight", "Announcer")!!
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


    fun getAutoFollowNavigationWhenAboveSecond(): Long {
        return prefs.getString("autoFollowNavigationWhenAboveSecond", "10")!!.toLong()
    }

    /**
     * 从设置中获取是否普通话播报
     */
    fun getIsVoiceAnnouncements(): Boolean {
        return prefs.getBoolean("voiceAnnouncements", true)
    }

    /**
     * 从设置中获取是否英语播报
     */
    fun getIsEnVoiceAnnouncements(): Boolean {
        return prefs.getBoolean("enVoiceAnnouncements", true)
    }


    /**
     * 从设置中获取是否进站时间播报
     */
    fun getIsArriveTimeAnnouncements(): Boolean {
        return prefs.getBoolean("arriveTimeAnnouncements", true)
    }

    /**
     * 从设置中获取是否启用线路轨迹纠偏
     */
    fun getIsLineTrajectoryCorrection(): Boolean {
        return prefs.getBoolean("lineTrajectoryCorrection", true)
    }

    /**
     * 从设置中获取是否使用TTS
     */
    fun getIsUseTTS(): Boolean {
        return prefs.getBoolean("useTTS", true)
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


    /**
     * 从InputStream中读取Byte[]
     */
    @Throws(IOException::class)
    fun convertToByteArray(inputStream: InputStream): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024) // 定义缓冲区
        var length: Int

        // 持续读取直到流结束
        while ((inputStream.read(buffer).also { length = it }) != -1) {
            byteArrayOutputStream.write(buffer, 0, length)
        }

        return byteArrayOutputStream.toByteArray() // 转换为字节数组
    }

    /**
     * 将2位及以下整数转换为中文读法字符串列表
     * 输入 0,1,...,10,为[0],[1],...,[10]；20,30...,90为[20],[30],...,[90]；其他例如21为[20,1]
     * @param num 要转换的2位及以下整数
     * @param before 列表中每项需要添加的后缀
     * @param isAddZeros 个位数是否补零。如输入1为[0, 1]
     */
    fun intToCnReading(
        num: Int,
        before: String = "",
        isAddZeros: Boolean = false
    ): ArrayList<String> {
        val list = ArrayList<String>()
        if (num !in 0..99) {
            return list
        }
        if (num in 0..10) {    // 0 - 10
            if (isAddZeros && num != 10)
                list.add(list.size, "${before}0")
            list.add(list.size, "$before$num")
        } else {// 11 - 99
            // 十位
            if (num >= 20) {
                list.add(list.size, "$before${num / 10}")
            }
//            十
            list.add(list.size, "${before}10")
            // 个位
            if (num % 10 != 0) {
                list.add(list.size, "$before${num % 10}")
            }
        }
        return list
    }

    fun getTimeVoiceList(): ArrayList<String> {
        val currentTime = LocalTime.now()
        val voiceList = ArrayList<String>()

        voiceList.addAll(intToCnReading(currentTime.hour, "/cn/time/"))
        voiceList.add(voiceList.size, "/cn/time/hour")
        voiceList.addAll(intToCnReading(currentTime.minute, "/cn/time/", true))
        voiceList.add(voiceList.size, "/cn/time/minute")

        voiceList.forEach {
            Log.d("TIME11", it)
        }

        return voiceList
    }

}