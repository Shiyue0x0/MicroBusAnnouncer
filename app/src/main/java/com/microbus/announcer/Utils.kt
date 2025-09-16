package com.microbus.announcer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.CountDownTimer
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.amap.api.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.kiylx.libx.pref_component.preference_util.delegate.boolean
import com.kiylx.libx.pref_component.preference_util.delegate.int
import com.kiylx.libx.pref_component.preference_util.delegate.string
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogStationInfoBinding
import com.microbus.announcer.fragment.StationFragment
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalTime
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.microbus.announcer.PrefsHelper


class Utils(private val context: Context) {

    var tag: String = javaClass.simpleName

    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val appRootPath =
        Environment.getExternalStorageDirectory().absolutePath + "/Announcer"


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
     * 从设置中获取是否发送运行通知
     */
    fun getIsSeedNotice(): Boolean {
        return prefs.getBoolean("notice", true)
    }

    /**
     * 从设置中获取界面语言
     * @return zh中文，en英文
     */
    fun getUILang(): String {
        val prefStr = prefs.getString("lang", "auto")
        return if (prefStr == "auto")
            Locale.getDefault().language
        else
            prefStr!!
    }

    /**
     * 从设置中获取所在城市
     */
    fun getCity(): String {
        return prefs.getString("city", "桂林")!!
    }

    fun setUILang(lang: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
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
     * 从设置中获取是否自动切换路线方向
     */
    fun getIsAutoSwitchLineDirection(): Boolean {
        return prefs.getBoolean("AutoSwitchLineDirection", true)
    }

    /**
     * 从设置中获取站点判定距离
     */
    fun getArriveStationDistance(): Double {
        return prefs.getString("arriveStationDistance", "30.0")!!.toDouble()
    }

    /**
     * 从设置中获取定位间隔（毫秒）
     */
    fun getLocationInterval(): Int {
        return prefs.getString("locationInterval", "2500")!!.toInt()
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
        return prefs.getString("mapStationShowType", "0")!!.toInt()
    }

    /**
     * 从设置中获取超过秒数自动跟随定位
     */
    fun getAutoFollowNavigationWhenAboveSecond(): Long {
        return prefs.getString("autoFollowNavigationWhenAboveSecond", "10")!!.toLong()
    }


    /**
     * 从设置中获取地图模式
     */
    fun getMapType(): Int {
        return prefs.getString("mapType", "1")!!.toInt()
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
     * 从设置中获取播报语音库
     */
    fun getAnnouncementLibrary(): String {

        if (!isGrantManageFilesAccessPermission()) {
            return "Default"
        }

        return prefs.getString("announcementLibrary", "Default")!!
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
     * 将3位及以下整数转换为中文读法字符串列表
     * 输入 0,1,...,10,为[0],[1],...,[10]；20,30...,90为[20],[30],...,[90]；其他例如21为[20,1]
     * @param str 要转换的2位及以下整数
     * @param before 列表中每项需要添加的后缀
     * @param isAddZeros 个位数是否补零。如输入1为[0, 1]
     */
    fun intOrLetterToCnReading(
        str: String,
        before: String = "",
        isAddZeros: Boolean = false
    ): ArrayList<String> {
        val list = ArrayList<String>()
        // 英文
        if ("[a-zA-z]+".toRegex().matches(str)) {
            list.add(list.size, "$before$str")
        }
        // 数字
        else if ("[0-9]+".toRegex().matches(str)) {
            Log.d(tag, str)
            val num = str.toInt()

            if (num !in 0..999) {
                return list
            }

            if (num in 100..999) {
                //百位
                list.add(list.size, "$before${num / 100}")
                // 百
                list.add(list.size, "${before}100")
                //一
                if (num % 100 in 10..19) {
                    list.add(list.size, "${before}1")
                }
                // 十位及个位
                if (num != 100)
                    list.addAll(intOrLetterToCnReading((num % 100).toString(), "/cn/number/", true))
            } else if (num in 11..99) {
                // 十位
                if (num >= 20) {
                    list.add(list.size, "$before${num / 10}")
                }
                // 十
                list.add(list.size, "${before}10")
                // 个位
                if (num % 10 != 0) {
                    list.addAll(intOrLetterToCnReading((num % 10).toString(), "/cn/number/", false))
                }
            } else {
                // 零(0-9)
                if (isAddZeros && num != 10)
                    list.add(list.size, "${before}0")
                list.add(list.size, "$before$num")
            }
        }
        return list
    }

    fun getTimeVoiceList(): ArrayList<String> {
        val currentTime = LocalTime.now()
        val voiceList = ArrayList<String>()

        voiceList.addAll(intOrLetterToCnReading(currentTime.hour.toString(), "/cn/number/"))
        voiceList.add("/cn/common/点")
        voiceList.addAll(intOrLetterToCnReading(currentTime.minute.toString(), "/cn/number/", true))
        voiceList.add("/cn/common/分")

        return voiceList
    }

    fun getNumOrLetterVoiceList(str: String): ArrayList<String> {

        //拆分数字和字母
        val strList = "([a-zA-Z]+|\\d+)".toRegex().findAll(str).toList()
        val voiceList = ArrayList<String>()

        strList.forEach { result ->
            if ("\\d+".toRegex().findAll(result.value).toList().isNotEmpty())
                voiceList.addAll(intOrLetterToCnReading(result.value, "/cn/number/"))
            else
                voiceList.addAll(intOrLetterToCnReading(result.value, "/en/letter/"))

        }

        return voiceList
    }

    fun showStationDialog(
        type: String,
        oldStation: Station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0),
        latLng: LatLng = LatLng(0.0, 0.0),
        isOrderLatLng: Boolean = false,
        stationFragment: StationFragment = StationFragment(),
        isOrderGetCurLatLng: Boolean = false,

        ) {

        val stationDatabaseHelper = StationDatabaseHelper(context)

        val binding =
            AlertDialogStationInfoBinding.inflate(LayoutInflater.from(context))

        val alertDialog =
            MaterialAlertDialogBuilder(context)
                .setView(binding.root)
                .setNegativeButton(
                    context.resources.getString(android.R.string.cancel), null
                )
                .setPositiveButton("提交", null)
                .setTitle(if (type == "new") "新增站点" else "更新站点")
                .create()


        if (type == "new") {
            alertDialog.show()
            if (isOrderGetCurLatLng) {
                alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).text =
                    "获取当前位置"
                alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener {
                        stationFragment.initLocation()
                        stationFragment.mLocationClient.startLocation()
                        object : CountDownTimer(4 * 1000, 10000) {
                            override fun onTick(millisUntilFinished: Long) {
                            }

                            override fun onFinish() {
                                stationFragment.mLocationClient.stopLocation()
                            }
                        }.start()
                    }
            }

            if (isOrderLatLng) {
                binding.editTextLongitude.setText(latLng.longitude.toString())
                binding.editTextLatitude.setText(latLng.latitude.toString())
            }
        } else if (type == "update") {

            binding.editTextCnName.setText(oldStation.cnName)
            binding.editTextEnName.setText(oldStation.enName)
            binding.editTextType.setText(oldStation.type)
            binding.editTextLongitude.setText(oldStation.longitude.toString())
            binding.editTextLatitude.setText(oldStation.latitude.toString())

            alertDialog.show()

            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).text = "删除"
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {
                    stationDatabaseHelper.delById(oldStation.id!!)
                }


        }

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                val cnName = binding.editTextCnName.text.toString()
                val enName = binding.editTextEnName.text.toString()
                val type = binding.editTextType.text.toString()

                if (cnName == "") {
                    this.showMsg("请填写中文名称")
                    return@setOnClickListener
                }

                if (enName == "") {
                    this.showMsg("请填写英文名称")
                    return@setOnClickListener
                }

                if (binding.editTextLongitude.text.toString() == "") {
                    this.showMsg("请填写经度")
                    return@setOnClickListener
                }

                val longitudeRegex = Regex("(\\d+(\\.\\d+)?)( \\d+(\\.\\d+)?)?")
                if (!binding.editTextLongitude.text.matches(longitudeRegex)) {
                    this.showMsg("经度格式错误")
                    return@setOnClickListener
                }

                if (binding.editTextLatitude.text.toString() == "") {
                    if (!binding.editTextLongitude.text.matches(longitudeRegex)) {
                        this.showMsg("请填写经度")
                        return@setOnClickListener
                    }
                    val latLng = binding.editTextLongitude.text.toString().split(' ')
                    binding.editTextLongitude.setText(latLng[0])
                    binding.editTextLatitude.setText(latLng[1])
                }

                val latitudeRegex = Regex("\\d+(\\.\\d+)?")
                if (!binding.editTextLatitude.text.matches(latitudeRegex)) {
                    this.showMsg("纬度格式错误")
                    return@setOnClickListener
                }

                if (binding.editTextLatitude.text.toString() == "") {
                    val latLng = binding.editTextLongitude.text.toString().split(' ')
                    binding.editTextLongitude.setText(latLng[0])
                    binding.editTextLatitude.setText(latLng[1])
                }

                val longitude: Double = binding.editTextLongitude.text.toString().toDouble()
                val latitude: Double = binding.editTextLatitude.text.toString().toDouble()

                val stationNew = Station(null, cnName, enName, longitude, latitude, type)

                if (type == "new") {
                    stationDatabaseHelper.insert(stationNew)
                } else if (type == "update") {
                    stationDatabaseHelper.updateById(oldStation.id!!, stationNew)
                }

                alertDialog.cancel()


            }


    }

    /**
     * 从设置中获取报站表达式
     * @param stationType （Default：常规站）（Second：第二站）（Starting：起点站）（Terminal：终点站）
     * @param stationState （Next：下一站）（WillArrive：即将到站）（Arrive：进站）
     * @return 相应的报站表达式。如果stationType不为Default，且没有查询到填写值，则返回Default的值
     */
    fun getAnnouncementFormat(stationType: String, stationState: String): String {

        loadAnnouncementFormatFromConfig()

        val exp = prefs.getString("${stationType}${stationState}AnnouncementExpression", "")!!
        return if (exp == "" && stationType != "Default")
            prefs.getString("Default${stationState}AnnouncementExpression", "")!!
        else
            exp

    }

    /**
     * 从表达式中提取报站内容
     * @param exp 报站表达式
     * @return 提取后的列表，如果表达式不合法，返回["ERROR", "不合法的内容"]
     */
    fun getAnnouncements(exp: String): ArrayList<String> {

        val itemList = exp.split("|")
        val anList = ArrayList<String>()
        val itemRegex = Regex("^(?!(.*[|<>])).*$")

        val keywordList = ArrayList<String>(listOf("<time>", "<speed>", "<line>"))

        for (lang in getAnnouncementLangList()) {
            keywordList.add("<ns$lang>")
            keywordList.add("<ss$lang>")
            keywordList.add("<ts$lang>")
        }

        for (item in itemList) {
            if (keywordList.contains(item)) {
                anList.add(item)
            } else {
                if (itemRegex.matches(item) && item != "") {
                    anList.add(item)
                } else {
                    return ArrayList(listOf("ERROR", item))
                }
            }
        }

//        for (an in anList) {
//            Log.d(tag, an)
//        }

        return anList
    }

    /**
     * 获取报站语种列表
     * */
    fun getAnnouncementLangList(): ArrayList<String> {

        if (!isGrantManageFilesAccessPermission()) {
            return ArrayList()
        }


        val dir = File("$appRootPath/Media/${getAnnouncementLibrary()}")
        val cnDir = File("$appRootPath/Media/${getAnnouncementLibrary()}/cn")
        val enDir = File("$appRootPath/Media/${getAnnouncementLibrary()}/en")

        if (!cnDir.exists()) {
            cnDir.mkdirs()
        }

        if (!enDir.exists()) {
            enDir.mkdirs()
        }

        val langFolderList = dir.walk()
            .filter { it.isDirectory && it.path.split("/").size == dir.path.split("/").size + 1 }
            .toList()

        val langList = ArrayList<String>(listOf("cn", "en"))
        for (langFolder in langFolderList) {
            if (!langList.contains(langFolder.name))
                langList.add(langFolder.name)
        }

        return langList
    }

    /**
     * 获取报站语音库列表
     * */
    fun getAnnouncementLibraryList(): List<String> {

        if (!isGrantManageFilesAccessPermission()) {
            return ArrayList()
        }

        val file = File("$appRootPath/Media/")
        val fileList = file.walk()
            .filter { it.isDirectory && it.path.split("/").size == file.path.split("/").size + 1 }
            .toList()

        val list = ArrayList<String>()
        for (file in fileList) {
            list.add(file.name)
        }
        return list
    }

    fun requestManageFilesAccessPermission(activity: Activity) {
        if (!isGrantManageFilesAccessPermission()) {
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.request_manage_files_access_permission_title))
                .setMessage(context.getString(R.string.request_manage_files_access_permission_text))
                .setPositiveButton(context.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                    val permissionManager = PermissionManager(context, activity)
                    permissionManager.requestManageFilesAccessPermission()
                }.setNegativeButton(context.getString(android.R.string.cancel), null).create()
                .show()
        }
    }

    fun requestNormalPermission(activity: Activity): Boolean {
        val permissionManager = PermissionManager(context, activity)
        return permissionManager.requestNormalPermission()
    }

    fun isGrantManageFilesAccessPermission(): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager())
    }

    fun getStationTypeList(): List<String> {
        return listOf("Default", "Starting", "Second", "Terminal")
    }

    fun getStationStateList(): List<String> {
        return listOf("Next", "WillArrive", "Arrive")
    }

    fun loadAnnouncementFormatFromConfig() {

        if (!isGrantManageFilesAccessPermission()) {
            return
        }

        // 读取config.json
        val configFile = File("$appRootPath/Media/${getAnnouncementLibrary()}/config.json")
        if (!configFile.exists()) {
            copyDefaultConfig()
        }

        val configElem = JsonParser.parseString(configFile.readText())
        val configObj = configElem.asJsonObject
        val formatObj = configObj.get("announcementFormat").asJsonObject

        prefs.edit {
            for (stationState in getStationStateList()) {
                val stateObj = formatObj.get(stationState).asJsonObject
                for (stationType in getStationTypeList()) {
                    val format = stateObj.get(stationType).asString
                    putString("${stationType}${stationState}AnnouncementExpression", format)
                }
            }
        }

    }

    fun updateAnnouncementFormatConfig(
        stationType: String,
        stationState: String,
        newValue: String
    ) {

        if (!isGrantManageFilesAccessPermission()) {
            return
        }

        // 读取config.json
        val configFile = File("$appRootPath/Media/${getAnnouncementLibrary()}/config.json")
        if (!configFile.exists()) {
            copyDefaultConfig()
        }

        val configElem = JsonParser.parseString(configFile.readText())
        val configObj = configElem.asJsonObject
        val formatObj = configObj.get("announcementFormat").asJsonObject
        val stateObj = formatObj.get(stationState).asJsonObject
        stateObj.addProperty(stationType, newValue)

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val configJsonStr = gson.toJson(configElem)

        val writer = FileWriter(configFile)
        writer.write(configJsonStr)
        writer.flush()


    }

    //todo 添加默认文件
    fun copyDefaultConfig() {

        if (!isGrantManageFilesAccessPermission()) {
            return
        }

        val dir = File("$appRootPath/Media/${getAnnouncementLibrary()}")
        val outputFile = File("${dir.path}/config.json")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (outputFile.exists()) {
            return
        }

        val defaultJsonIS = context.resources.openRawResource(R.raw.announcement_default_config)
        val bufferedReader = BufferedReader(InputStreamReader(defaultJsonIS))

        val fileWriter = FileWriter(outputFile)
        var line = bufferedReader.readLine()
        var jsonStr = ""
        while (line != null) {
            jsonStr += line + "\n"
            line = bufferedReader.readLine()
        }

        Log.d(tag, "" + jsonStr)
        fileWriter.write(jsonStr)

        fileWriter.flush()
        bufferedReader.close()
        defaultJsonIS.close()
    }

    fun showRequestLocationPermissionDialog(permissionManager: PermissionManager) {
        MaterialAlertDialogBuilder(context)
            .setTitle("要启用定位服务吗？")
            .setMessage("位置权限：用于地图定位和自动报站")
            .setPositiveButton(context.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                permissionManager.requestLocationPermission()
            }
            .setNegativeButton(context.getString(android.R.string.cancel), null)
            .create()
            .show()
    }

    fun getSettings(){
         PrefsHelper(prefs).defaultLineName
    }


}

