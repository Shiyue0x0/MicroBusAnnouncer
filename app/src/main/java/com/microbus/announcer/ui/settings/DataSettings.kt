package com.microbus.announcer.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.databinding.DialogLoadingBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
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


class DataSettings : Fragment() {

    lateinit var utils: Utils

    private val requestRestoreStation = 0

    private val requestRestoreLine = 1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        utils = Utils(requireContext())

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
            color = colorResource(R.color.md_theme_surface)
        ) {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(
                controller = controller
            ) {
                Scaffold(
                    topBar = {
                        AnSmallTopAppBar(requireActivity(), getString(R.string.stationAndLineDate))
                    },
                    content = { innerPadding ->
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "备份与还原",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                                )
                                BackupItem()
                                RestoreItem(
                                    "station",
                                    painterResource(id = R.drawable.station1)
                                )
                                RestoreItem("line", painterResource(id = R.drawable.line1))
                                Text(
                                    "预设数据",
                                    fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
                                )
                                RestorePresetMixItem()
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                        }

                    }
                )

            }
        }
    }


    @Composable
    fun BackupItem() {
        BaseSettingItem(
            "备份站点与路线",
            "将数据备份到 Announcer/Backups",
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


            },
            rightContain = {

            },
        )
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

            },
            rightContain = {

            },
        )
    }

    @Composable
    fun RestorePresetMixItem() {
        BaseSettingItem(
            "加载预设数据",
            "将内置的预设数据加载到应用",
            painterResource(id = R.drawable.database),
            {

                if (!utils.isGrantManageFilesAccessPermission()) {
                    utils.requestManageFilesAccessPermission(requireActivity())
                    return@BaseSettingItem
                }

                val chooseDialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("要加载哪些预设数据？")
                    .setNeutralButton("站点和路线", null)
                    .setNegativeButton("仅站点", null)
                    .setPositiveButton("仅路线", null)
                    .show()

                // 站点和路线
                chooseDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    restorePreset(station = true, line = true, chooseDialog = chooseDialog)
                }

                // 仅站点
                chooseDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    restorePreset(station = true, line = false, chooseDialog = chooseDialog)
                }

                // 仅路线
                chooseDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    restorePreset(station = false, line = true, chooseDialog = chooseDialog)
                }

            },
        )
    }

    /**
     * 加载预设数据
     * @param station 是否加载站点
     * @param line 是否加载路线
     * */
    fun restorePreset(station: Boolean, line: Boolean, chooseDialog: AlertDialog) {
        val typeStr = if (station && line)
            "站点和路线"
        else if (station)
            "站点"
        else if (line)
            "路线"
        else
            ""

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.CustomAlertDialogStyle
        ).setTitle("加载$typeStr")
            .setMessage("该操作会先备份您当前${typeStr}的数据，然后用预设数据覆盖，要继续吗？")
            .setPositiveButton("加载并重启", null)
            .setNegativeButton(getString(android.R.string.cancel), null).show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            dialog.dismiss()
            chooseDialog.dismiss()

            val loadingDialogBinding =
                DialogLoadingBinding.inflate(LayoutInflater.from(context))
            loadingDialogBinding.title.text = "正在加载${typeStr}，请稍后"

            val loadingDialog = MaterialAlertDialogBuilder(
                requireContext(),
                R.style.CustomAlertDialogStyle
            )
                .setView(loadingDialogBinding.root)
                .show()

            if (station)
                loadPresetData(R.raw.station)
            if (line)
                loadPresetData(R.raw.line)
//            utils.showMsg("已加载预设数据，再次打开应用生效")
//            utils.showMsg("Announcer重启中，请稍后")

            utils.showMsg("现有的站点和路线已备份至\nAnnouncer/Backups")
            utils.showMsg("加载完成，请前往站点或路线查看")


//            requireActivity().finish()


//            requireActivity().recreate()
            restartActivityStack(requireContext())
        }
    }

    fun restartActivityStack(context: Context) {
        val packageName = context.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let { intent ->
            // 关键Flag：清空任务栈并创建新任务
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
         (activity as? Activity)?.overridePendingTransition(OVERRIDE_TRANSITION_CLOSE,0, 0)
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
            val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
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
    }

    //    选取文件回调
    @SuppressLint("Recycle")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode in setOf(
                requestRestoreStation,
                requestRestoreLine
            ) && resultCode == RESULT_OK
        ) {

            val tableName = when (requestCode) {
                requestRestoreStation -> "station"
                requestRestoreLine -> "line"
                else -> ""
            }

            val outputFileName = "$tableName.db"

            val outputPath = context?.getExternalFilesDir("")?.path + "/database/" + outputFileName

            val outputFileCnName = when (requestCode) {
                requestRestoreStation -> "站点"
                requestRestoreLine -> "路线"
                else -> ""
            }

            //读入文件
            var fileInputStream =
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
            // todo
            var tempFile: File?
            var database: SQLiteDatabase? = null
            try {
                // 打开数据库（只读模式）
                tempFile = File.createTempFile("temp_db_", ".db")
                tempFile.outputStream().use { output ->
                    fileInputStream.use { input ->
                        input?.copyTo(output)
                    }
                }

                database = SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )

                // 查询 sqlite_master 表检查是否存在要还原的表
                val cursor = database.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    arrayOf(tableName)
                )

                val tableExists = cursor.moveToFirst()
                cursor.close()

                if (!tableExists) {
                    utils.showMsg("数据库中不存在 '${tableName}' 表，请重试")
                    return
                }

            } catch (e: Exception) {
                e.printStackTrace()
                utils.showMsg("读取数据库失败: ${e.message}")
                return
            } finally {
                database?.close()
//                tempFile?.outputStream()?.close()
            }

            //备份当前文件
            val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
            val dataTime = dateFormat.format(Date(System.currentTimeMillis()))

            val inputStream = FileInputStream(outputPath)
            backupFile(outputFileName, "$dataTime-auto", inputStream)
            inputStream.close()

            //清空原文件
            val oldFileOutputStream = FileOutputStream(outputPath)
            File(outputPath).delete()
            oldFileOutputStream.close()

            //写入文件
            val fileOutputStream = FileOutputStream(outputPath)
            val buffer = ByteArray(1024)
            var length: Int

            fileInputStream = requireContext().contentResolver.openInputStream(data.data!!)
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

}

