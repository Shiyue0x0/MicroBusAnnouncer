package com.microbus.announcer.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.microbus.announcer.MainActivity
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min


open class SettingPreferenceFragment : PreferenceFragmentCompat() {

    private val requestRestoreStation = 0

    private val requestRestoreLine = 1

    private lateinit var permissionManager: PermissionManager

    private lateinit var utils: Utils


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        utils = Utils(requireContext())

        //默认路线名称
        val defaultLinePreference: EditTextPreference? = findPreference("defaultLineName")
        defaultLinePreference?.setOnPreferenceChangeListener { _, newValue ->
            val lineDatabaseHelper = LineDatabaseHelper(requireContext())
            if (lineDatabaseHelper.quertByName(newValue.toString())
                    .isNotEmpty() || newValue == ""
            ) {
                utils.showMsg("设置成功")
                true
            } else {
                utils.showMsg("路线不存在")
                false
            }
        }

        //显示底部导航栏
        val showBottomBarPreference: SwitchPreferenceCompat? = findPreference("showBottomBar")
        showBottomBarPreference?.setOnPreferenceChangeListener { _, newValue ->
            val activity = requireActivity() as MainActivity
            if (newValue.toString() == "true") {
                activity.binding.bottomNavigationView.visibility = View.VISIBLE
            } else {
                activity.binding.bottomNavigationView.visibility = View.GONE
            }
            return@setOnPreferenceChangeListener true
        }

        //站点判定距离
        val arriveStationDistancePreference: EditTextPreference? =
            findPreference("arriveStationDistance")
        arriveStationDistancePreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editText.setSelection(editText.text.length)
        }

        //定位间隔（毫秒）
        val locationIntervalPreference: EditTextPreference? =
            findPreference("locationInterval")
        locationIntervalPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setSelection(editText.text.length)
        }
        locationIntervalPreference!!.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().toInt() < 1000) {
                utils.showMsg("定位间隔不能低于1000毫秒")
                false
            } else
                true
        }


        //路线头牌刷新间隔
        val lineHeadCardChangeTimePreference: EditTextPreference? =
            findPreference("lineHeadCardChangeTime")
        lineHeadCardChangeTimePreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setSelection(editText.text.length)
        }

        //超过秒数自动跟随定位
        val autoFollowNavigationWhenAboveSecondPreference: EditTextPreference? =
            findPreference("autoFollowNavigationWhenAboveSecond")
        autoFollowNavigationWhenAboveSecondPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setSelection(editText.text.length)
        }

        //更改地图模式
        val mapTypePreference: DropDownPreference =
            findPreference("mapType")!!
        mapTypePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val activity = requireActivity() as MainActivity
                val mainFragment = activity.fragmentList[0] as MainFragment
                mainFragment.setMapMode(newValue.toString().toInt())
                return@OnPreferenceChangeListener true
            }

        //备份站点与路线
        findPreference<Preference>("backupStationAndLine")?.setOnPreferenceClickListener {

            permissionManager = PermissionManager(requireContext(), this)
            //判断是否有管理外部存储的权限
            if (Build.VERSION.SDK_INT >= 30) {
                if (!Environment.isExternalStorageManager()) {
                    AlertDialog.Builder(context)
                        .setTitle("允许 ${requireContext().resources.getString(R.string.app_name)} 访问设备上的文件吗？")
                        .setMessage("应用需要该权限来备份数据")
                        .setPositiveButton("前往授予") { _, _ ->
                            permissionManager.requestManageFilesAccessPermission()
                        }
                        .setNegativeButton("取消", null)
                        .create()
                        .show()
                }
            }


            //获取当前时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val dataTime = dateFormat.format(Date(System.currentTimeMillis()))

            backupFile("station.db", dataTime)
            backupFile("line.db", dataTime)
            utils.showMsg("站点和路线已备份至\nDocuments/Announcer/Backups")

            true
        }

        //还原站点
        findPreference<Preference>("restoreStation")?.setOnPreferenceClickListener {
            val uri =
                "content://com.android.externalstorage.documents/document/primary:Documents%2fAnnouncer%2fBackups".toUri()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            intent.setType("application/*")
            @Suppress("DEPRECATION")
            startActivityForResult(intent, requestRestoreStation)

            true
        }

        //还原路线
        findPreference<Preference>("restoreLine")?.setOnPreferenceClickListener {
            val uri =
                "content://com.android.externalstorage.documents/document/primary:Documents%2fAnnouncer%2fBackups".toUri()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            intent.setType("application/*")
            @Suppress("DEPRECATION")
            startActivityForResult(intent, requestRestoreLine)

            true
        }

        //  加载预设站点
        findPreference<Preference>("loadPresetStationData")?.setOnPreferenceClickListener {
            loadPresetData(R.raw.station)
            true
        }

        //  加载预设路线
        findPreference<Preference>("loadPresetLineData")?.setOnPreferenceClickListener {
            loadPresetData(R.raw.line)
            true
        }


        //  搜索暂无报站音频的站点
        val appRootPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Documents/Announcer"
        findPreference<Preference>("searchStationNotHaveVoice")?.setOnPreferenceClickListener {

            //如果没有管理外部存储的权限，请求授予
            if (!requestManageFilesAccessPermission())
                return@setOnPreferenceClickListener true

            val stationDatabaseHelper = StationDatabaseHelper(requireContext())
            val stationList = stationDatabaseHelper.quertAll()

            val stationVoiceCnLostList = ArrayList<String>()
            val stationVoiceEnLostList = ArrayList<String>()

            stationList.forEach { station ->
                // 搜索中文音频
                if (!File("$appRootPath/Media/cn/station/${station.cnName}.wav").exists()) {
                    if (!stationVoiceCnLostList.contains(station.cnName))
                        stationVoiceCnLostList.add(station.cnName)
                }
                // 搜索英文音频
                if (!File("$appRootPath/Media/en/station/${station.enName}.wav").exists()) {
                    if (!stationVoiceEnLostList.contains(station.enName))
                        stationVoiceEnLostList.add(station.enName)
                }
            }

            var text = "中文：\n"
            stationVoiceCnLostList.forEach { name ->
                text += "$name\n"
            }
             text += "English: \n"
            stationVoiceEnLostList.forEach { name ->
                text += "$name\n"
            }

            Log.d(tag, text)

            AlertDialog.Builder(requireContext())
                .setTitle("")
                .setMessage(text)
                .create()
                .show()

            true
        }

        //关于
        val aboutPref = findPreference<Preference>("about")!!
        aboutPref.setOnPreferenceClickListener {
            utils.showMsg("MicroBus 欢迎您")
            utils.showMsg("鸣谢 yukonga Updater")

            true
        }
        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        aboutPref.summary = info.versionName

    }

    //    选取文件回调
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode in setOf(
                requestRestoreStation,
                requestRestoreLine
            ) && resultCode == RESULT_OK
        ) {

            val outputPath = context?.getExternalFilesDir("")?.path + "/database/" +
                    when (requestCode) {
                        requestRestoreStation -> "station.db"
                        requestRestoreLine -> "line.db"
                        else -> ""
                    }

            val outputFileName = when (requestCode) {
                requestRestoreStation -> "station.db"
                requestRestoreLine -> "line.db"
                else -> ""
            }

            val outputFileCnName = when (requestCode) {
                requestRestoreStation -> "站点"
                requestRestoreLine -> "路线"
                else -> ""
            }

            //读入文件
            val fileInputStream =
                requireContext().contentResolver.openInputStream(data!!.data!!)
            Log.d("file", data.data!!.path.toString())

            //检查文件头
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    requireContext().contentResolver.openInputStream(data.data!!)
                )
            )
            val content = StringBuilder()
            var line: String
            var count = 0
            while (bufferedReader.readLine().also { line = it } != null && count < 15) {
                content.append(line)
                count += line.length
            }
            val fileHeader = content.substring(
                0,
                min(content.length.toDouble(), 15.0).toInt()
            )
            if (fileHeader != "SQLite format 3") {
                utils.showMsg("这不是db文件，请重试")
                return
            }
            bufferedReader.close()

            //检测表是否存在

            //备份当前文件
            val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val dataTimeStr = dateFormat.format(Date(System.currentTimeMillis()))
            val inputStream = FileInputStream(outputPath)
            backupFile(outputFileName, "$dataTimeStr-auto", inputStream)
            inputStream.close()

            //清空原文件
            val oldFileOutputStream = FileOutputStream(outputPath)
            File(outputPath).delete()
            oldFileOutputStream.close()

            //写入文件
            val fileOutputStream = FileOutputStream(outputPath)
            val buffer = ByteArray(1024)
            var length: Int
            while (fileInputStream!!.read(buffer).also { length = it } > 0) {
                fileOutputStream.write(buffer, 0, length)
            }

            fileOutputStream.close()
            fileInputStream.close()

            utils.showMsg("原${outputFileCnName}已备份至\nDocuments/Announcer/Backups")
            utils.showMsg("${outputFileCnName}还原成功，重启生效")

            requireActivity().finish()
        }
    }

    /**
     * 备份站点或路线
     */
    private fun backupFile(
        fileName: String,
        dataTime: String,
        inputStream: FileInputStream? = null
    ) {

        val outputPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Documents/Announcer/Backups/" + dataTime + "/database"
        //新建备份文件目录
        File(outputPath).mkdirs()

        //读入文件
        val fileInputStream = inputStream
            ?: FileInputStream(context?.getExternalFilesDir("")?.path + "/database/" + fileName)

        //写入备份文件
        val fileOutputStream =
            FileOutputStream("$outputPath/$fileName")
        val buffer = ByteArray(1024)
        var length: Int
        while (fileInputStream.read(buffer).also { length = it } > 0) {
            fileOutputStream.write(buffer, 0, length)
        }

        fileInputStream.close()
        fileOutputStream.close()
    }


    private fun loadPresetData(resId: Int) {
        val fileList = ArrayList<Int>()

        fileList.add(resId)

        fileList.forEach {

            val fileName = when (it) {
                R.raw.station -> "station.db"
                R.raw.line -> "line.db"
                else -> ""
            }

            //  备份原数据
            val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val dataTime = dateFormat.format(Date(System.currentTimeMillis()))
            backupFile(fileName, "$dataTime-auto")

            //  读取预设数据
            Files.copy(
                resources.openRawResource(it),
                Paths.get(
                    context?.getExternalFilesDir("")?.path + "/database/$fileName",
                ),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        utils.showMsg("站点和路线已备份至\nDocuments/Announcer/Backups")
        utils.showMsg("已加载预设数据，重启生效")
        requireActivity().finish()
    }

    fun requestManageFilesAccessPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(requireContext())
                .setTitle(requireContext().resources.getString(R.string.request_manage_files_access_permission_title))
                .setMessage(requireContext().resources.getString(R.string.request_manage_files_access_permission_text))
                .setPositiveButton(requireContext().resources.getString(R.string.request_manage_files_access_permission_to_grant)) { _, _ ->
                    PermissionManager(requireContext(), this).requestManageFilesAccessPermission()
                }.setNegativeButton(
                    requireContext().resources.getString(android.R.string.cancel),
                    null
                ).create()
                .show()
            return false
        } else {
            return true
        }
    }

}