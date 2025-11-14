package com.microbus.announcer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.amap.api.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.microbus.announcer.bean.EsItem
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogStationInfoBinding
import com.microbus.announcer.fragment.StationFragment
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.time.LocalTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Utils(private val context: Context) {

    var tag: String = javaClass.simpleName
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appRootPath =
        Environment.getExternalStorageDirectory().absolutePath + "/Announcer"

    val tryListeningAnActionName = "com.microbus.announcer.try_listening_an"
    val switchLineActionName = "com.microbus.announcer.switch_line"

    val editLineOnMapActionName = "com.microbus.announcer.edit_line_on_map"

    val requestCityFromLocationActionName = "com.microbus.announcer.request_city_from_location"

    val sendCityFromLocationActionName = "com.microbus.announcer.send_city_from_location"

    val openLocationActionName = "com.microbus.announcer.open_location"

    val lineListScrollToTopActionName = "com.microbus.announcer.line_list_scroll_to_top"
    val stationListScrollToTopActionName = "com.microbus.announcer.station_list_scroll_to_top"

    lateinit var toast: Toast

    /**
     * Toast提示
     * @param msg 提示内容
     */
    fun showMsg(msg: String, isCannel: Boolean = false) {

        if (isCannel && ::toast.isInitialized)
            toast.cancel()

        toast = Toast.makeText(context, msg, Toast.LENGTH_LONG)
        toast.show()

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


//    /**
//     * 从设置中获取默认路线名称
//     */
//    fun getDefaultLineName(): String {
//        return prefs.getString("defaultLineName", "")!!
//    }

    /**
     * 从设置中获取是否显示底部导航栏
     */
    fun getIsShowBottomBar(): Boolean {
        return prefs.getBoolean("showBottomBar", true)
    }

    /**
     * 从设置中获取是否退出后保留后台
     */
    fun getIsSaveBackAfterExit(): Boolean {
        return prefs.getBoolean("saveBackAfterExit", false)
    }


    /**
     * 从设置中获取是否发送运行通知
     */
    fun getNotice(): Boolean {
        return prefs.getBoolean("notice", true)
    }

    /**
     * 从设置中获取是否启用左侧电显
     */
    fun getIsOpenLeftEs(): Boolean {
        return prefs.getBoolean("isOpenLeftEs", true)
    }

    /**
     * 从设置中获取是否启用中部电显
     */
    fun getIsOpenMidEs(): Boolean {
        return prefs.getBoolean("isOpenMidEs", true)
    }

    /**
     * 从设置中获取是否启用巡航模式
     */
    fun getIsNavMode(): Boolean {
        return prefs.getBoolean("isNavMode", false)
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
        return prefs.getString("city", "桂林市") ?: "桂林市"
    }

    fun setUILang(lang: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
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
     * 从设置中获取是否启用路线规划
     */
    fun getIsLinePlanning(): Boolean {
        return prefs.getBoolean("linePlanning", true)
    }


    /**
     * 从设置中获取是否启用地图规划路线模式
     */
    fun getIsMapEditLineMode(): Boolean {
        return prefs.getBoolean("mapEditLineMode", false)
    }


    /**
     * 从设置中获取是否自动切换路线方向
     */
    fun getIsAutoSwitchLineDirection(): Boolean {
        return prefs.getBoolean("autoSwitchLineDirection", true)
    }

    /**
     * 从设置中获取站点判定距离
     */
    @Deprecated("请使用getStationRangeByLineType()替代")
    fun getArriveStationDistance(): Double {
        return prefs.getString("arriveStationDistance", "30.0")!!.toDouble()
    }

    /**
     * 根据路线类型获取进站范围半径
     * @param type 路线类型：C社区|B公交|U地铁|T火车
     */
    fun getStationRangeByLineType(type: String): Float {
        val default = when (type) {
            "C" -> 20F
            "B" -> 30F
            "U" -> 300F
            "T" -> 500F
            else -> 30F
        }
        return prefs.getFloat("${type}LineStationRange", default)
    }

    /**
     * 根据路线类型设置进站范围半径
     * @param type 路线类型：C社区|B公交|U地铁|T火车
     * @param range 半径
     */
    fun setStationRangeByLineType(type: String, range: Float) {
        prefs.edit {
            putFloat("${type}LineStationRange", range)
        }
    }

    /**
     * 从设置中获取定位间隔（毫秒）
     */
    fun getLocationInterval(): Int {
        return prefs.getInt("locationInterval", 2500)
    }

    /**
     * 从设置中获取是否当上行从终点站出站时切换反向
     */
    fun getSwitchDirectionWhenOutFromTerminalWithOnUp(): Boolean {
        return prefs.getBoolean("switchDirectionWhenOutFromTerminalWithOnUp", true)
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


//    /**
//     * 从设置中获取超过秒数自动跟随定位
//     */
//    fun getAutoFollowNavigationWhenAboveSecond(): Long {
//        return prefs.getString("autoFollowNavigationWhenAboveSecond", "10")?.toLong() ?: 10L
//    }

    /**
     * 从设置中获取地图模式
     */
    fun getMapType(): Int {
        return prefs.getString("mapType", "0")?.toInt() ?: 0
    }

    /**
     * 从设置中获取是否启用线路轨迹纠偏
     */
    fun getIsLineTrajectoryCorrection(): Boolean {
        return prefs.getBoolean("lineTrajectoryCorrection", true)
    }

    /**
     * 从设置中获取地图是否显示路况
     */
    fun getIsMapTrafficEnabled(): Boolean {
        return prefs.getBoolean("isMapTrafficEnabled", false)
    }

    /**
     * 从设置中获取是否使用TTS
     */
    fun getIsUseTTS(): Boolean {
        return prefs.getBoolean("useTTS", true)
    }

    fun getIsStationChangeVibrator(): Boolean {
        return prefs.getBoolean("stationChangeVibrator", true)
    }

    fun getAnSubtitle(): Boolean {
        return prefs.getBoolean("anSubtitle", true)
    }

    fun getClickMapPauseAn(): Boolean {
        return prefs.getBoolean("clickMapPauseAn", false)
    }

    fun getServiceLanguageStr(): String {
        val default = "请问还有乘客要下车吗？关门了，请注意。关门了。"
        return prefs.getString("serviceLanguageStr", default) ?: default
    }


    fun getAutoAnInterval(): Int {
        return prefs.getInt("autoAnInterval", 0)
    }

    /**
     * 从设置中获取播报语音库
     */
    fun getAnnouncementLibrary(): String {

        val default = "Default"
        if (!isGrantManageFilesAccessPermission()) {
            return default
        }

        return prefs.getString("announcementLibrary", default) ?: default
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
        onAddDone: () -> Unit = {},
        onDelDone: () -> Unit = {}
        ) {

        val stationDatabaseHelper = StationDatabaseHelper(context)

        val binding =
            DialogStationInfoBinding.inflate(LayoutInflater.from(context))

        val alertDialog =
            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                .setView(binding.root)
                .setNeutralButton("删除", null)
                .setNegativeButton(
                    context.resources.getString(android.R.string.cancel), null
                )
                .setPositiveButton("提交", null)
                .setTitle(if (type == "new") "新增站点" else "更新站点")
                .create()



        if (type == "new") {
            alertDialog.show()
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
                    alertDialog.dismiss()
                    onDelDone()
                }
        }

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                val cnName = binding.editTextCnName.text.toString()
                val enName = binding.editTextEnName.text.toString()
                val stationType = binding.editTextType.text.toString()

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

                if (stationType == "") {
                    this.showMsg("请填写站点类型")
                    return@setOnClickListener
                }

                if (!listOf("C", "B", "U", "T").contains(stationType)) {
                    showMsg("站点类型应为CBUT之一")
                    return@setOnClickListener
                }

                val longitude: Double = binding.editTextLongitude.text.toString().toDouble()
                val latitude: Double = binding.editTextLatitude.text.toString().toDouble()

                val stationNew = Station(null, cnName, enName, longitude, latitude, stationType)

                if (type == "new") {
                    val res = stationDatabaseHelper.insert(stationNew)
                    if (res > 0) {
                        showMsg("已添加站点 ${stationNew.cnName}")
                    } else {
                        showMsg("添加失败，返回码 $res")
                    }
                } else if (type == "update") {
                    stationDatabaseHelper.updateById(oldStation.id!!, stationNew)
                }

                onAddDone()
                alertDialog.cancel()


            }


    }

    /**
     * 从设置中获取报站表达式
     * @param stationState （Next：下一站）（WillArrive：即将到站）（Arrive：进站
     * @param stationType （Default：常规站）（Second：第二站）（Starting：起点站）（Terminal：终点站）
     * @return 相应的报站表达式。如果stationType不为Default，且没有查询到填写值，则返回Default的值
     */
    fun getAnnouncementFormat(stationState: String, stationType: String): String {

        loadAnnouncementFormatFromConfig()

        val exp = prefs.getString("${stationType}${stationState}AnnouncementExpression", "")!!
        return if (exp == "" && stationType != "Default")
            prefs.getString("Default${stationState}AnnouncementExpression", "")!!
        else
            exp

    }

    /**
     * 从表达式中提取报站内容（分词器）
     * @param exp 报站表达式
     * @return 提取后的列表，如果表达式不合法，返回["ERROR", "不合法的内容"]
     */
    fun getAnnouncements(exp: String): ArrayList<String> {

        val lineList = exp.split("\n")
        val anList = ArrayList<String>()

        for (line in lineList) {
            val itemList = line.split("|")
            val itemRegex = Regex("^(?!(.*[|<>])).*$")

            val anKeywordList = getDefaultKeywordList()

            for (item in itemList) {
                if (anKeywordList.contains(item)) {
                    anList.add(item)
                } else {
                    if ((itemRegex.matches(item) && item != "") ||
                        (item.length >= 3 && item.substring(1, 3) == "ms")
                    ) {
                        anList.add(item)
                    } else {
                        return ArrayList(listOf("ERROR", item))
                    }
                }
            }
        }

        return anList
    }

    var hasGetLangList = false
    var myLangList = ArrayList<String>()

    /**
     * 获取语种列表
     * */
    fun getLangList(): ArrayList<String> {

        val defaultLangList = listOf("cn", "en")
        for (defaultLang in defaultLangList) {
            if (!myLangList.contains(defaultLang))
                myLangList.add(defaultLang)
        }

        if (hasGetLangList)
            return myLangList

        if (!isGrantManageFilesAccessPermission()) {
            return myLangList
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

        for (langFolder in langFolderList) {
            if (!myLangList.contains(langFolder.name))
                myLangList.add(langFolder.name)
        }

        hasGetLangList = true

        return myLangList
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
            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                .setTitle(context.getString(R.string.request_manage_files_access_permission_title))
                .setMessage(context.getString(R.string.request_manage_files_access_permission_text))
                .setPositiveButton(context.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                    val permissionManager = PermissionManager(context, activity)
                    permissionManager.requestManageFilesAccessPermission()
                }.setNegativeButton(context.getString(android.R.string.cancel), null).create()
                .show()
        }
    }

    fun isGrantManageFilesAccessPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            val perms = listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            for (p in perms) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        p
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    return false
            }
            return true
        }
    }

    fun getStationStateList(): List<String> {
        return listOf("Next", "WillArrive", "Arrive")
    }

    fun getStationTypeList(): List<String> {
        return listOf("Default", "Starting", "Second", "Terminal")
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

    fun copyDefaultConfig() {

        if (!isGrantManageFilesAccessPermission()) {
            return
        }

        val dir = File("$appRootPath/Media/${getAnnouncementLibrary()}")
        val outputFile = File("${dir.path}/config.json")

        Log.d(tag, "$appRootPath/Media/${getAnnouncementLibrary()}")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        if (!outputFile.exists()) {
            outputFile.createNewFile()
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
        try {
            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                .setTitle("要启用定位服务吗？")
                .setMessage("需要位置权限用于自动报站")
                .setPositiveButton(context.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                    permissionManager.requestLocationPermission()
                }
                .setNegativeButton(context.getString(android.R.string.cancel), null)
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun getEsText(): String {
        val default =
            "<sscn>|<tscn>|5|B\n" +
                    "<ssen>|<tsen>|5|B\n" +
                    "<nscn>|到了|5|A\n" +
                    "<nscn>|就要到了|5|W\n" +
                    "下一站|<nscn>|5|N\n" +
                    "全国文明城市 桂林欢迎您|攻坚十四五 奋进新征程 建设壮美广西|5|D\n" +
                    "速度|<speed>|5|R"
        return prefs.getString("esText", default) ?: default
    }

    /**
     * 获取电显显示内容列表
     * @return
     * */
    fun getEsList(text: String): ArrayList<EsItem> {

        val esList = ArrayList<EsItem>()

        if (text == "") {
            return esList
        }

        val lines = text.split('\n')

        val keywordList = getDefaultKeywordList()
        val numReg = Regex("^[1-9]\\d*$")

        for (i in lines.indices) {
            val items = lines[i].split('|')

            if (items.size != 4) {
                esList.clear()
                esList.add(EsItem("第${i + 1}行参数不正确，请检查格式", "", -1))
                return esList
            } else {
                for (j in items.indices) {
                    // 检查左右文本 todo 关键词判断
                    if (j == 0 || j == 1) {

                    }
                    // 检查最短显示时间和内容类型
                    else if (j == 2) {
                        if (!numReg.matches(items[j])) {
                            esList.clear()
                            esList.add(
                                EsItem(
                                    "第${i + 1}行最短显示时间必须为正整数",
                                    "请前往设置修改",
                                    -1
                                )
                            )
                            return esList
                        }
                    }
                    // 检查内容类型
                    else if (j == 3) {
                        if (!items[j].contains(Regex("[DNWASCTBR]"))) {
                            esList.clear()
                            esList.add(
                                EsItem(
                                    "第${i + 1}行内容类型需选填DNWASCTBR其一",
                                    "请前往设置修改",
                                    -1
                                )
                            )
                            return esList
                        }
                    }
                }
            }
            esList.add(EsItem(items[0], items[1], items[2].toInt(), items[3]))
        }
        return esList
    }

    fun getDefaultKeywordList(): ArrayList<String> {
        val keywordList =
            ArrayList<String>(
                listOf(
                    "<line>",
                    "<time>",
                    "<year>",
                    "<years>",
                    "<month>",
                    "<date>",
                    "<hour>",
                    "<minute>",
                    "<second>",
                    "<speed>"
                )
            )

        for (lang in getLangList()) {
            keywordList.add("<ns$lang>")
            keywordList.add("<ss$lang>")
            keywordList.add("<ts$lang>")
            keywordList.add("<ms$lang>")
        }
        return keywordList
    }

    fun getEsNextWord(): String {
        return prefs.getString("esNextWord", "下一站") ?: "下一站"
    }

    fun getEsWillArriveWord(): String {
        return prefs.getString("esWillArriveWord", "即将到达") ?: "即将到达"
    }

    fun getEsArriveWord(): String {
        return prefs.getString("esArriveWord", "到达") ?: "到达"
    }

    fun getEsSpeed(): Int {
        return prefs.getInt("esSpeed", 150)
    }

    fun getEsFinishPositionOfLastWord(): Float {
        return prefs.getFloat("esFinishPositionOfLastWord", 0F)
    }

    fun getLibVoiceCount(lang: String): Int {

        if (!isGrantManageFilesAccessPermission()) {
            return 0
        }

        val dir = File("$appRootPath/Media/${getAnnouncementLibrary()}/${lang}")
        if (!dir.exists()) {
            return 0
        }

        val commonDir = File("$appRootPath/Media/${getAnnouncementLibrary()}/${lang}/common")
        val stationDir = File("$appRootPath/Media/${getAnnouncementLibrary()}/${lang}/station")

        val commonVoiceList = commonDir.walk()
            .filter { it.isFile }
            .toList()

        val stationVoiceList = stationDir.walk()
            .filter { it.isFile }
            .toList()

        return commonVoiceList.size + stationVoiceList.size
    }

    fun simplifyFraction(numerator: Int, denominator: Int): Pair<Int, Int> {
        require(denominator != 0) { "分母不能为零" }

        val gcd = greatestCommonDivisor(abs(numerator), abs(denominator))
        val simplifiedNum = numerator / gcd
        val simplifiedDen = denominator / gcd

        // 确保分母始终为正数
        return if (simplifiedDen < 0) {
            Pair(-simplifiedNum, -simplifiedDen)
        } else {
            Pair(simplifiedNum, simplifiedDen)
        }
    }

    // 计算最大公约数的辅助函数（使用欧几里得算法）
    fun greatestCommonDivisor(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }


    fun getIfDarkMode(): Boolean {
        val nightModeFlags: Int =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun extractNWA(reg: Regex, input: String): String {
        return reg.findAll(input)
            .joinToString("") { it.value }
    }

    fun openHelperDialog(title: String, url: String) {
        val urlList = listOf("GitHub", "Gitee")
        MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
            .setTitle(title)
            .setSingleChoiceItems(urlList.toTypedArray(), -1) { dialog, which ->
                dialog.dismiss()
                val uriStr = when (which) {
                    0 -> "https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/$url"
                    1 -> "https://gitee.com/shiyue0x0/micro-bus-announcer/blob/master/$url"
                    else -> "https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/$url"
                }
                openUri(uriStr)
            }
            .show()
    }

    /**
     * 根据站点中文名称获取对应语言名称
     * @param cnName 要查找的中文名
     * @param lang 要获取的语言
     * @return cn返回中文名本身，en返回站点在本地数据库的英文名称，其他的从stationLangTable.json查找
     * 查找失败，则返回中文名本身
     * */
    fun getStationNameFromCn(cnName: String, lang: String): String {

        if (lang == "cn") {
            return cnName
        }

        if (lang == "en") {
            val stationDatabaseHelper = StationDatabaseHelper(context)
            val queryStationList = stationDatabaseHelper.queryByCnName(cnName)
            return if (queryStationList.isNotEmpty())
                queryStationList.first().enName
            else
                cnName
        }

        if (!isGrantManageFilesAccessPermission()) {
            return cnName
        }

        val langTableFile = File("$appRootPath/stationLangTable.json")

        if (!langTableFile.exists()) {
//            showMsg("stationLangTable.json文件不存在，请先创建")
            return cnName
        }

        val element = JsonParser.parseString(langTableFile.readText())

        if (!element.isJsonArray)
            return cnName

        val stationList = element.asJsonArray

        for (station in stationList) {

            val obj = station.asJsonObject

            if (obj.get("cn") == null || obj.get("cn").asString != cnName)
                continue

            return if (obj.get(lang) != null)
                obj.get(lang).asString
            else
                cnName
        }

        return cnName

    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        val dLon = lon2Rad - lon1Rad

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360  // 规范化到0-360度

        return bearing
    }

    fun getDefaultLineComparator(): Comparator<Line> {

        val numReg = "\\d+".toRegex()
        return Comparator { line1: Line, line2: Line ->
//            val diff = line1.name.length - line2.name.length
//            if (diff != 0) {
//                diff
//            } else {
            val line1NumRes = numReg.find(line1.name)
            val line2NumRes = numReg.find(line2.name)

            if (line1NumRes == null)
                Integer.MAX_VALUE
            else if (line2NumRes == null)
                Integer.MIN_VALUE
            else line1NumRes.value.toInt() - line2NumRes.value.toInt()
//            }
        }
    }

    fun getRandomNumByTime(min: Int, max: Int): Int {
        val randomNum = System.currentTimeMillis()
        return (randomNum % (max - min) + min).toInt()
    }

    fun openUri(uriStr: String){
        val intent = Intent(Intent.ACTION_VIEW, uriStr.toUri())
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val webviewIntent = Intent(context, WebviewActivity::class.java)
            webviewIntent.putExtra("uriStr", uriStr)
            context.startActivity(webviewIntent)
        }


    }
}

