package com.microbus.announcer.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.microbus.announcer.PermissionManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.ui.compose.BaseSettingItem
import com.microbus.announcer.ui.compose.SwitchSettingItem
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class SystemSettings : Fragment() {

    lateinit var utils: Utils
    private lateinit var prefs: SharedPreferences
    private lateinit var permissionManager: PermissionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

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

        val mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (isAdded) {
                    when (intent.action) {
                        utils.sendCityFromLocationActionName -> {
                            val cityName = intent.getStringExtra("cityName")
                            if (cityName != "" && ::cityInput.isInitialized) {
                                utils.showMsg("当前城市：$cityName")
                                cityInput.setText(cityName)
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(utils.sendCityFromLocationActionName)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, intentFilter)

        return composeView
    }

    @Composable
    @Preview
    fun MainView() {

        val (lang, setLang) = remember {
            mutableStateOf(prefs.getString("lang", "auto") ?: "auto")
        }

        val (city, setCity) = remember {
            mutableStateOf(utils.getCity())
        }

        val (showBottomBar, setShowBottomBar) = remember {
            mutableStateOf(utils.getIsShowBottomBar())
        }

        val (saveBackAfterExit, setSaveBackAfterExit) = remember {
            mutableStateOf(utils.getIsSaveBackAfterExit())
        }

        val (notice, setNotice) = remember {
            mutableStateOf(utils.getNotice())
        }

        val (isNavMode, setIsNavMode) = remember {
            mutableStateOf(utils.getIsNavMode())
        }

        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "lang" -> setLang(prefs.getString(key, "") ?: "")
                    "city" -> setCity(prefs.getString(key, "") ?: "")
                    "showBottomBar" -> setShowBottomBar(prefs.getBoolean(key, true))
                    "saveBackAfterExit" -> setSaveBackAfterExit(prefs.getBoolean(key, true))
                    "notice" -> setNotice(prefs.getBoolean(key, true))
                    "isNavMode" -> setIsNavMode(prefs.getBoolean(key, false))

                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.system))
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
                                UiLangItem(lang)
                                CityNameItem(city)
                                BottomBarItem(showBottomBar, setShowBottomBar)
                                SaveBackAfterExitItem(saveBackAfterExit, setSaveBackAfterExit)
                                NoticeItem(notice, setNotice)
                                IsNavModeItem(isNavMode, setIsNavMode)
                                LineAllStationTypeItem()
                            }
                        }
                    })
            }
        }
    }


    @Composable
    fun UiLangItem(lang: String) {
        val nameList = listOf("跟随系统", "简体中文", "English")
        val valueList = listOf("auto", "zh", "en")
        val currentChooseIndex = valueList.indexOf(prefs.getString("lang", "auto"))

        BaseSettingItem(
            "界面语言", nameList[valueList.indexOf(lang)], painterResource(id = R.drawable.lang),
            {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("选择界面语言").setSingleChoiceItems(
                    nameList.toTypedArray(), currentChooseIndex
                ) { dialog, which ->
                    prefs.edit {
                        putString("lang", valueList[which])
                    }
                    utils.setUILang(valueList[which])
                    dialog.cancel()
                }.show()
            },
            rightContain = {

            },
        )
    }


    lateinit var cityInput: TextInputEditText

    @Composable
    fun CityNameItem(city: String) {
        BaseSettingItem(
            "搜索城市", city, painterResource(id = R.drawable.city),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                )
                    .setTitle("设置城市").setView(binding.root)
                    .setNeutralButton("定位", null)
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setPositiveButton("保存", null)
                    .show()

                dialog.setCanceledOnTouchOutside(false)

                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    val intent = Intent()
                        .setAction(utils.requestCityFromLocationActionName)
                    LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(intent)
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newValue = binding.editText.text.toString()
                    if (newValue == "") {
                        utils.showMsg("请输入城市名称")
                        return@setOnClickListener
                    }
                    prefs.edit {
                        putString("city", newValue)
                    }
                    utils.showMsg("已将城市设置为${newValue}")
                    dialog.dismiss()
                }

                cityInput = binding.editText

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }

    @Composable
    fun BottomBarItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "导航栏", "底部导航栏", painterResource(id = R.drawable.bottom_nav),
            {
                toggleBottomBar(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleBottomBar(value, setValue, it)
                }
            },
        )
    }

    fun toggleBottomBar(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("showBottomBar", it)
        }
        // TODO

//        val activity = requireActivity() as MainActivity
        if (it) {
//            activity.binding.bottomNavigationView.visibility = View.VISIBLE
        } else {
//            activity.binding.bottomNavigationView.visibility = View.GONE
            utils.showMsg("现在请尝试左右滑动来切换界面")
        }
    }

    @Composable
    fun SaveBackAfterExitItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "退出后保留后台",
            "暂时保留后台，以便下次返回\n快速加载，但不会继续定位",
            painterResource(id = R.drawable.exit),
            {
                toggleSaveBackAfterExit(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleSaveBackAfterExit(value, setValue, it)
                }
            },
        )
    }

    fun toggleSaveBackAfterExit(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("saveBackAfterExit", it)
        }
    }

    @Composable
    fun NoticeItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "路线运行通知",
            "出站/即将到站/到站时发送通知",
            painterResource(id = R.drawable.notice),
            {
                toggleNotice(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleNotice(value, setValue, it)
                }
            },
        )
    }

    fun toggleNotice(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("notice", it)
        }
        if (it) {
            permissionManager.requestNoticePermission()
        }
    }

    @Composable
    fun IsNavModeItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "巡航信息栏",
            "在主控顶部显示巡航信息",
            painterResource(id = R.drawable.nav),
            {
                toggleIsNavMode(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleIsNavMode(value, setValue, it)
                }
            },
        )
    }

    fun toggleIsNavMode(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("isNavMode", it)
        }
        if (it) {
            permissionManager.requestNoticePermission()
        }
    }

    @Composable
    fun LineAllStationTypeItem() {

        val stationTypeList = listOf("社区站点", "公交站点", "地铁站点", "火车站点")
        val valueList = listOf("C", "B", "U", "T")

        BaseSettingItem(
            "全站路线显示的站点类型", painter = painterResource(id = R.drawable.station),

            clickFun = {
                val checkedItems = valueList
                    .map { utils.getLineAllStationTypeEnable(it) }
                    .toBooleanArray()

                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                )
                    .setMultiChoiceItems(
                        stationTypeList.toTypedArray(),
                        checkedItems
                    ) { _, which, isChecked ->
                        prefs.edit {
                            putBoolean("LineAllStationType${valueList[which]}Enable", isChecked)
                        }
                    }
                    .setTitle("全站路线显示的站点类型")
                    .show()
            },
            rightContain = {

            },
        )
    }


}

