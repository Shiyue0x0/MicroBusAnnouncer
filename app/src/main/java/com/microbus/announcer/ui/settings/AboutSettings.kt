package com.microbus.announcer.fragment.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonParser
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.databinding.DialogLoadingBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class AboutSettings : Fragment() {

    lateinit var utils: Utils

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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.about))
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AboutItem()
                                ProjectUrlItem()
                                DeveloperItem()
                                HelperItem()
                                CheckForUpdatesItem()
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                        }

                    }
                )

            }
        }
    }


    @Composable
    @Preview
    fun AboutItem() {

        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    utils.showMsg("MicroBus 欢迎您")
                    utils.showMsg("鸣谢 yukonga Updater")
                },
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Image(
                painter = painterResource(id = R.mipmap.an_round),
                modifier = Modifier
                    .padding(top = 64.dp, bottom = 64.dp)
                    .size(100.dp),
                contentDescription = getString(R.string.app_name)
            )
            Text(
                text = getString(R.string.app_name),
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = info.versionName ?: "",
                fontSize = 10.sp,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )

        }
    }

    @Composable
    fun CheckForUpdatesItem() {
        val wayList = listOf("GitHub", "Gitee")
        BaseSettingItem(
            "检查更新",
            painter = painterResource(id = R.drawable.update),
            clickFun = {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择更新渠道").setSingleChoiceItems(
                    wayList.toTypedArray(), -1
                ) { dialog, which ->

                    val loadingDialogBinding =
                        DialogLoadingBinding.inflate(LayoutInflater.from(context))
                    loadingDialogBinding.title.text = "正在检查更新"

                    val loadingDialog = MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.CustomAlertDialogStyle
                    )
                        .setView(loadingDialogBinding.root)
                        .show()

                    val url =
                        when (which) {
                            0 -> "https://api.github.com/repos/Shiyue0x0/MicroBusAnnouncer/releases"
                            1 -> "https://gitee.com/api/v5/repos/shiyue0x0/micro-bus-announcer/releases"
                            else -> ""
                        }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url(url)
                                .build()
                            val res = client.newCall(request).execute()
                            val body = res.body.string()
                            val releaseList = JsonParser.parseString(body).asJsonArray
                            // ID越大，版本越新
                            var maxId = Int.MIN_VALUE
                            var lastVersionName = ""
                            var lastVersionBody = ""
                            var lastVersionApkUrl = ""
                            for (release in releaseList) {
                                val obj = release.asJsonObject
                                val id = obj.get("id").asString.toInt()
                                if (id > maxId) {
                                    maxId = id
                                    lastVersionName = obj.get("tag_name").asString
                                    lastVersionBody = obj.get("body").asString
                                    for (asset in obj.get("assets").asJsonArray) {
                                        val url =
                                            asset.asJsonObject.get("browser_download_url").asString
                                        if (url.split(".").last() == "apk") {
                                            // todo apk 下载
                                            lastVersionApkUrl = url
                                            break
                                        }
                                    }
                                }
                            }

                            val currentVerName = requireContext().packageManager
                                .getPackageInfo(requireContext().packageName, 0).versionName

                            //1.2.3-250901-1200
                            val currentVerNameList = currentVerName?.split("-")[0]!!.split(
                                "v",
                                "."
                            )

                            //v1.2.3
                            val lastVerNameList =
                                lastVersionName.drop(1).split(".")

                            var isLast = true
                            currentVerNameList.forEachIndexed { i, string ->
                                Log.d(tag, "${lastVerNameList[i]} > ${currentVerNameList[i]}")
                                if (lastVerNameList[i].toInt() > currentVerNameList[i].toInt()) {
                                    isLast = false
                                }
                            }

                            requireActivity().runOnUiThread {
                                loadingDialog.dismiss()
                                if (isLast) {
                                    MaterialAlertDialogBuilder(
                                        requireContext(),
                                        R.style.CustomAlertDialogStyle
                                    ).setTitle("已是最新版本")
                                        .setMessage(
                                            "${getString(R.string.app_name)} ${
                                                lastVersionName.drop(
                                                    1
                                                )
                                            }"
                                        )
                                        .setPositiveButton(getString(android.R.string.ok), null)
                                        .show()
                                } else {

                                    val newVerDialog = MaterialAlertDialogBuilder(
                                        requireContext(),
                                        R.style.CustomAlertDialogStyle
                                    ).setTitle("有最新版本 $lastVersionName")
                                        .setMessage(lastVersionBody)
                                        .setNegativeButton(getString(android.R.string.cancel), null)
                                        .setPositiveButton("现在更新", null)
                                        .show()

                                    newVerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                        .setOnClickListener {
                                            val uriStr = when (which) {
                                                0 -> "https://github.com/Shiyue0x0/MicroBusAnnouncer/releases"
                                                1 -> "https://gitee.com/shiyue0x0/micro-bus-announcer/releases"
                                                else -> ""
                                            }
                                            utils.openUri(uriStr)


                                        }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                    dialog.cancel()
                }.show()
            },
        )
    }

    @Composable
    fun ProjectUrlItem() {
        BaseSettingItem(
            "项目地址",
            "GitHub/Gitee",
            painterResource(id = R.drawable.github),
            {
                val urlList = listOf("GitHub", "Gitee").toTypedArray()
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择仓库").setSingleChoiceItems(
                    urlList, -1
                ) { dialog, which ->
                    val uriStr = when (which) {
                        0 -> "https://github.com/Shiyue0x0/MicroBusAnnouncer"
                        1 -> "https://gitee.com/shiyue0x0/micro-bus-announcer"
                        else -> "https://github.com/Shiyue0x0/MicroBusAnnouncer"
                    }
                    utils.openUri(uriStr)
                    dialog.cancel()
                }.show()

            },
        )
    }

    @Composable
    fun DeveloperItem() {
        BaseSettingItem(
            "开发者",
            "Bilibili@Shiyue0x0",
            painterResource(id = R.drawable.github),
            {
                val uriStr = "https://space.bilibili.com/34943744"
                utils.openUri(uriStr)
            },
        )
    }

    @Composable
    fun HelperItem() {
        BaseSettingItem(
            "使用文档",
            "了解 ${resources.getString(R.string.app_name)}",
            painterResource(id = R.drawable.doc),
            {
                utils.openHelperDialog("要在哪里阅读？", "README.md")
            },
        )
    }

}

