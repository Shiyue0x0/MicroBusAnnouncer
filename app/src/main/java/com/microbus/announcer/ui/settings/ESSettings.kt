package com.microbus.announcer.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import com.microbus.announcer.databinding.DialogSliderBinding
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.math.abs


class ESSettings : Fragment() {

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


        val (esText, setEsText) = remember {
            mutableStateOf(utils.getEsText())
        }

        val (esNextWord, setEsNextWord) = remember {
            mutableStateOf(utils.getEsNextWord())
        }

        val (esWillArriveWord, setEsWillArriveWord) = remember {
            mutableStateOf(utils.getEsWillArriveWord())
        }

        val (esArriveWord, setEsArriveWord) = remember {
            mutableStateOf(utils.getEsArriveWord())
        }

        val (esSpeed, setEsSpeed) = remember {
            mutableIntStateOf(utils.getEsSpeed())
        }

        val (esFinishPositionOfLastWord, setEsFinishPositionOfLastWord) = remember {
            mutableFloatStateOf(utils.getEsFinishPositionOfLastWord())
        }

        val (isOpenLeftEs, setIsOpenLeftEs) = remember {
            mutableStateOf(utils.getIsOpenLeftEs())
        }

        val (isMidLeftEs, setIsMidLeftEs) = remember {
            mutableStateOf(utils.getIsOpenMidEs())
        }


        DisposableEffect(prefs) {
            val listener = OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "esText" -> setEsText(prefs.getString(key, "") ?: "")
                    "esNextWord" -> setEsNextWord(prefs.getString(key, "") ?: "")
                    "esWillArriveWord" -> setEsWillArriveWord(prefs.getString(key, "") ?: "")
                    "esArriveWord" -> setEsArriveWord(prefs.getString(key, "") ?: "")
                    "esSpeed" -> setEsSpeed(prefs.getInt(key, 100))
                    "esFinishPositionOfLastWord" -> setEsFinishPositionOfLastWord(
                        prefs.getFloat(
                            key,
                            0.5F
                        )
                    )

                    "isOpenLeftEs" -> setIsOpenLeftEs(prefs.getBoolean(key, true))
                    "isMidLeftEs" -> setIsMidLeftEs(prefs.getBoolean(key, true))

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
                        AnSmallTopAppBar(requireActivity(), getString(R.string.es))
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
                                ESTextItem(esText)
                                ESSpeedItem(esSpeed)
                                ESFinishPositionOfLastWordItem(esFinishPositionOfLastWord)
                                IsOpenLeftEsItem(isOpenLeftEs, setIsOpenLeftEs)
                                IsOpenMidEsItem(isMidLeftEs, setIsMidLeftEs)
                                EsKeywordItemGroup(esNextWord, esWillArriveWord, esArriveWord)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    })
            }
        }
    }


    lateinit var cityInput: TextInputEditText

    @Composable
    fun IsOpenLeftEsItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "左侧电显",
            painter = painterResource(id = R.drawable.left_align),
            clickFun = {
                toggleIsOpenLeftEs(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleIsOpenLeftEs(value, setValue, it)
                }
            },
        )
    }

    fun toggleIsOpenLeftEs(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("isOpenLeftEs", it)
        }
    }

    @Composable
    fun IsOpenMidEsItem(value: Boolean, setValue: (Boolean) -> Unit) {
        BaseSettingItem(
            "中部电显",
            painter = painterResource(id = R.drawable.mid_align),
            clickFun = {
                toggleIsOpenMidEs(value, setValue, !value)
            },
            rightContain = {
                SwitchSettingItem(value) {
                    toggleIsOpenMidEs(value, setValue, it)
                }
            },
        )
    }

    fun toggleIsOpenMidEs(value: Boolean, setValue: (Boolean) -> Unit, it: Boolean) {
        setValue(it)
        prefs.edit {
            putBoolean("isOpenMidEs", it)
        }
    }

    @Composable
    fun ESTextItem(eSText: String) {
        BaseSettingItem(
            "电显内容", eSText, painterResource(id = R.drawable.text),
            {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("编辑电显内容").setView(binding.root)
                    .setNeutralButton("帮助", null)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.editText.isSingleLine = false
                binding.editText.setText(eSText)
                dialog.setCanceledOnTouchOutside(false)

                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    utils.openHelperDialog("查看模拟电显文档", "readme/模拟电显.md")
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                    val newValue = binding.editText.text.toString()
                    val esList = utils.getEsList(newValue)

                    if (esList.isNotEmpty() && esList.first().minTimeS < 0) {
                        utils.showMsg(esList.first().leftText)
                        return@setOnClickListener
                    } else {
                        prefs.edit {
                            putString("esText", newValue)
                        }
                        for (es in esList) Log.d(
                            "es",
                            "${es.leftText} + ${es.rightText}(${es.minTimeS})"
                        )
                        utils.showMsg("设置成功，本次轮播完毕后切换")
                    }
                    dialog.dismiss()
                }

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
        )
    }

    @Composable
    fun EsKeywordItemGroup(esNextWord: String, esWillArriveWord: String, esArriveWord: String) {
        Column {
            Text(
                "电显提示词",
                fontFamily = FontFamily(Font(R.font.galano_grotesque_bold)),
                modifier = Modifier.padding(16.dp, 8.dp, 0.dp, 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("下一站", "esNextWord", esNextWord)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("即将到站", "esWillArriveWord", esWillArriveWord)
                }
                Column(modifier = Modifier.weight(1f)) {
                    EsKeywordItem("到站", "esArriveWord", esArriveWord)
                }
            }
        }

    }

    @Composable
    fun EsKeywordItem(title: String, key: String, value: String) {
        val keyName = when (key) {
            "esNextWord" -> "<next>"
            "esWillArriveWord" -> "<will>"
            "esArriveWord" -> "<arrive>"
            else -> ""
        }
        BaseSettingItem(
            title,
            value + "\n${keyName}",
            painterResource(id = R.drawable.city),
            clickFun = {
                val binding = DialogInputBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置${title}提示词")
                    .setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .show()

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newValue = binding.editText.text.toString()
                    if (newValue == "") {
                        utils.showMsg("请输入${title}提示词")
                        return@setOnClickListener
                    }
                    prefs.edit {
                        putString(key, newValue)
                    }
                    utils.showMsg("${title}提示词：${newValue}")
                    dialog.dismiss()
                }

                binding.textInputLayout.hint = "请输入文本"
                binding.textInputLayout.requestFocus()
                WindowCompat.getInsetsController(requireActivity().window, binding.editText)
                    .show(WindowInsetsCompat.Type.ime())
            },
            isShowIcon = false,
        )
    }

    val esDemoText = "请有序排队 文明乘车 桂林公交欢迎您 K99 开往 汽车客运南站"

    @Composable
    fun ESSpeedItem(eSSpeed: Int) {
        BaseSettingItem(
            "电显文字滚动速度", "$eSSpeed 像素/秒", painterResource(id = R.drawable.speed),
            {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("设置电显文字滚动速度").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整电显文字滚动速度"
                binding.slider.stepSize = 1F
                binding.slider.valueFrom = 1F
                binding.slider.valueTo = 500F
                binding.slider.value = eSSpeed.toFloat()

                binding.es.pixelMovePerSecond = eSSpeed.toFloat()
                binding.es.finishPositionOfLastWord = utils.getEsFinishPositionOfLastWord()
                binding.es.showText(esDemoText)


                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = getString(R.string.xPixelPerSecond, eSSpeed)

//                utils.showMsg(eSSpeed.toString())
                binding.slider.addOnChangeListener { slider, value, fromUser ->
//                    Log.d(tag, "slider: $value")
                    binding.es.pixelMovePerSecond = value
                    binding.text.text = getString(R.string.xPixelPerSecond, value.toInt())

                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    prefs.edit {
                        putInt("esSpeed", binding.slider.value.toInt())
                    }
                    utils.showMsg("速度设置成功")
                    dialog.dismiss()
                }

                dialog.setOnDismissListener {
                    binding.es.stopAnimation()
                }

            },
        )
    }

    @Composable
    fun ESFinishPositionOfLastWordItem(esFinishPositionOfLastWord: Float) {
        val text = getESFinishPositionOfLastWordItemText(esFinishPositionOfLastWord)
        BaseSettingItem(
            "电显文字滚动结束时机",
            text,
            painterResource(id = R.drawable.switch2),
            {
                val binding = DialogSliderBinding.inflate(LayoutInflater.from(context))
                val dialog = MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialogStyle
                ).setTitle("电显文字滚动结束时机").setView(binding.root)
                    .setPositiveButton("保存", null)
                    .setNegativeButton(getString(android.R.string.cancel), null).show()

                binding.slider.contentDescription = "拖动以调整"
                binding.slider.stepSize = 0.1F
                binding.slider.valueFrom = 0.0F
                binding.slider.valueTo = 1.0F
                binding.slider.value = esFinishPositionOfLastWord

                binding.es.finishPositionOfLastWord = esFinishPositionOfLastWord
                binding.es.pixelMovePerSecond = utils.getEsSpeed().toFloat()
                binding.es.showText(esDemoText)

                binding.text.visibility = ViewGroup.VISIBLE
                binding.text.text = text

//                utils.showMsg(eSSpeed.toString())
                binding.slider.addOnChangeListener { slider, value, fromUser ->
//                    Log.d(tag, "slider: $value")
                    binding.es.finishPositionOfLastWord = value
                    binding.text.text = getESFinishPositionOfLastWordItemText(value)
                }

                dialog.setCanceledOnTouchOutside(false)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    prefs.edit {
                        putFloat("esFinishPositionOfLastWord", binding.slider.value)
                    }
                    utils.showMsg("设置成功")
                    dialog.dismiss()
                }

                dialog.setOnDismissListener {
                    binding.es.stopAnimation()
                }

            },
        )
    }

    fun getESFinishPositionOfLastWordItemText(esFinishPositionOfLastWord: Float): String {
        return if (abs(esFinishPositionOfLastWord - 0.0F) < 0.0000000001)
            "最后一个字滚动离开屏幕时"
        else if (abs(esFinishPositionOfLastWord - 1.0F) < 0.0000000001) {
            "最后一个字滚动进入屏幕时"
        } else {
            val numerator = (esFinishPositionOfLastWord * 10).toInt()
            val denominator = 10
            val pair = utils.simplifyFraction(numerator, denominator)
            "最后一个字滚动到屏幕左边 ${pair.first} / ${pair.second} 处时"
        }

    }


}

