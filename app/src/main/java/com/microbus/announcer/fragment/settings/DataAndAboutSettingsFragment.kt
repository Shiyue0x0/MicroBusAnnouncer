package com.microbus.announcer.fragment.settings

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.compose.BaseSettingItem
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


class DataAndAboutSettingsFragment : Fragment() {

    lateinit var utils: Utils
    private lateinit var prefs: SharedPreferences
    private lateinit var permissionManager: PermissionManager

    private val requestRestoreStation = 0

    private val requestRestoreLine = 1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        utils = Utils(requireContext())
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        permissionManager = PermissionManager(requireContext(), requireActivity())

        val composeView = ComposeView(requireContext())

        composeView.apply {
            setContent { MainView() }
        }

        composeView.post {
            val layoutParams = composeView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            composeView.setLayoutParams(layoutParams)
        }


        return composeView
    }

    @Composable
    @Preview
    fun MainView() {

        Surface(
            contentColor = colorResource(R.color.md_theme_onSurface),
            color = colorResource(R.color.an_window_bg)
        ) {
            MaterialTheme {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "数据",
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                        )
                        BackupItem()
                        RestoreItem("station", painterResource(id = R.drawable.station1))
                        RestoreItem("line", painterResource(id = R.drawable.line1))
                        RestorePresetItem("station", painterResource(id = R.drawable.station1))
                        RestorePresetItem("line", painterResource(id = R.drawable.line1))
                        Text(
                            "关于",
                            fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                        )
                        AboutItem()
                        ProjectUrlItem()
                        DeveloperItem()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }


    @Composable
    fun BackupItem() {
        BaseSettingItem(
            "备份站点与路线",
            "将数据备份到Announcer/Backups",
            painterResource(id = R.drawable.backup),
            {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                //获取当前时间
                val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
                val dataTime = dateFormat.format(Date(System.currentTimeMillis()))

                backupFile("station.db", dataTime)
                backupFile("line.db", dataTime)
                utils.showMsg("站点和路线已备份至\nAnnouncer/Backups")


            }) {

        }
    }

    private fun backupFile(
        fileName: String,
        dataTime: String,
        inputStream: FileInputStream? = null
    ) {

        val outputPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Announcer/Backups/" + dataTime + "/database"
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

    @Composable
    fun RestoreItem(type: String, painter: Painter) {
        val name = when (type) {
            "station" -> "站点"
            "line" -> "路线"
            else -> -1
        }
        BaseSettingItem(
            "还原${name}数据",
            "将${name}数据还原到应用内",
            painter,
            {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                val uri =
                    "content://com.android.externalstorage.documents/document/primary:Documents%2fAnnouncer%2fBackups".toUri()
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                intent.setType("application/*")
                val code = when (type) {
                    "station" -> requestRestoreStation
                    "line" -> requestRestoreLine
                    else -> -1
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, code)

            }) {

        }
    }

    @Composable
    fun RestorePresetItem(type: String, painter: Painter) {
        val name = when (type) {
            "station" -> "站点"
            "line" -> "路线"
            else -> -1
        }
        BaseSettingItem(
            "加载预设${name}数据",
            "将${name}内置的预设数据加载到应用",
            painter,
            {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                //todo 询问
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("加载预设${name}数据")
                    .setMessage("该操纵会覆盖您现有的${name}数据，建议您先备份后再操作，要继续吗")
                    .setPositiveButton(requireContext().getString(android.R.string.ok), null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()


                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val preset = when (type) {
                        "station" -> R.raw.station
                        "line" -> R.raw.line
                        else -> R.raw.station
                    }
                    loadPresetData(preset)
                    dialog.dismiss()
                }

            })
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

        utils.showMsg("现有的站点和路线已备份至\nAnnouncer/Backups")
        utils.showMsg("已加载预设数据，再次打开应用生效")
        requireActivity().finish()
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
            val dateFormat =
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
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

            utils.showMsg("原${outputFileCnName}已备份至\nAnnouncer/Backups")
            utils.showMsg("${outputFileCnName}还原成功，再次打开应用生效")

            requireActivity().finish()
        }
    }

    @Composable
    fun AboutItem() {
        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        BaseSettingItem(
            getString(R.string.app_name),
            info.versionName ?: "",
            painterResource(id = R.mipmap.an_round),
            isIcon = false,
            clickFun = {
                utils.showMsg("MicroBus 欢迎您")
                utils.showMsg("鸣谢 yukonga Updater")
            })
    }

    @Composable
    fun ProjectUrlItem() {
        BaseSettingItem(
            "项目地址",
            "Github",
            painterResource(id = R.drawable.github),
            {
                val uri = "https://github.com/Shiyue0x0/MicroBusAnnouncer".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    utils.showMsg("打开失败")
                }
            })
    }

    @Composable
    fun DeveloperItem() {
        BaseSettingItem(
            "开发者",
            "Bilibili@Shiyue0x0",
            painterResource(id = R.drawable.github),
            {
                val uri = "https://space.bilibili.com/34943744".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    utils.showMsg("打开失败")
                }
            })
    }

}

