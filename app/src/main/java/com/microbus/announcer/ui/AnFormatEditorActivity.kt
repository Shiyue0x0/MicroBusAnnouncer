package com.microbus.announcer.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.microbus.announcer.BaseActivity
import com.microbus.announcer.Utils
import com.microbus.announcer.announce.AnnouncementPlayer
import com.microbus.announcer.announce.AnnouncementProvider
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.util.UUID

class AnFormatEditorActivity : BaseActivity() {

    private var tag = javaClass.simpleName
    private lateinit var utils: Utils
    private lateinit var lineDatabaseHelper: LineDatabaseHelper
    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var prefs: SharedPreferences
    private lateinit var announcementProvider: AnnouncementProvider
    private lateinit var anPlayer: AnnouncementPlayer

    var stationStatus = -1
    var stationPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        utils = Utils(this)
        initExtra()


        lineDatabaseHelper = LineDatabaseHelper.getInstance(this)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        announcementProvider = AnnouncementProvider.getInstance(this)
        anPlayer = AnnouncementPlayer.getInstance(this)

        enableEdgeToEdge()
        setContent {
            MainView()
        }
    }

    override fun onStop() {
        super.onStop()
        anPlayer.stopAnnounce()
    }

    private var stateKey: String? = ""
    private var positionKey: String? = ""

    private var initialFormatList: List<FormatItem> = emptyList()

    fun initExtra() {

        stateKey = intent.getStringExtra("state")
        positionKey = intent.getStringExtra("position")

        if (positionKey == null || stateKey == null) {
            utils.showMsg("参数错误")
            finish()
            return
        }

        stationStatus = when (stateKey) {
            "Next" -> StationStatus.ON_NEXT
            "WillArrive" -> StationStatus.ON_WILL_ARRIVE
            "Arrive" -> StationStatus.ON_ARRIVE
            else -> -1
        }

        stationPosition = when (positionKey) {
            "Default" -> StationPositionType.DEFAULT.value
            "Starting" -> StationPositionType.START.value
            "Second" -> StationPositionType.SECOND.value
            "Terminal" -> StationPositionType.END.value
            else -> -1
        }

        val sourceFormat = intent.getStringExtra("sourceFormat") ?: ""
        initialFormatList = parseSourceFormat(sourceFormat)
    }


    /**
     * 把 sourceFormat 解析成播报项列表。
     * - 按行分割，每行是一个播报项
     * - 行内按 '|' 分割，每段是一条语句
     */
    fun parseSourceFormat(sourceFormat: String): List<FormatItem> {
        if (sourceFormat.isEmpty()) return emptyList()

        return sourceFormat
            .split("\n")
            .filter { it.isNotEmpty() }   // 过滤空行
            .map { line ->
                FormatItem(editingSequenceList = parseLine(line))
            }
    }

    /**
     * 解析单行（一个播报项）内的语句列表。
     * - 以 '<' 开头且以 '>' 结尾的是占位符语句，其余为普通语句
     * - 占位符去掉尖括号后：ns/ss/ts 开头为站点名称语句，其余为其他占位符语句
     */
    fun parseLine(line: String): List<SentenceItem> {
        return line.split("|")
            .filter { it.isNotEmpty() }   // 过滤分割产生的空串
            .map { raw ->
                if (raw.startsWith("<") && raw.endsWith(">")) {
                    parsePlaceholderSentence(raw.removePrefix("<").removeSuffix(">"))
                } else {
                    // 普通语句：sourceText 与 displayText 均为原文
                    SentenceItem(
                        id = UUID.randomUUID().toString(),
                        type = SentenceItemType.Normal,
                        sourceText = raw,
                        displayText = raw
                    )
                }
            }
    }

    /**
     * 根据占位符（不含尖括号）构建 SentenceItem。
     */
    private fun parsePlaceholderSentence(rawKey: String): SentenceItem {
        // 站点名称语句：ns/ss/ts 开头，后面为语言代码
        val stationPositionType = when {
            rawKey.startsWith("ns") -> StationPositionType.DEFAULT
            rawKey.startsWith("ss") -> StationPositionType.START
            rawKey.startsWith("ts") -> StationPositionType.END
            else -> null
        }

        if (stationPositionType != null) {
            val langKey = rawKey.removePrefix(
                when (stationPositionType) {
                    StationPositionType.DEFAULT -> "ns"
                    StationPositionType.START -> "ss"
                    StationPositionType.END -> "ts"
                    else -> ""
                }
            )
            return buildStationNameSentence(stationPositionType, langKey)
        }

        // 其他占位符语句
        return buildOtherPlaceholderSentence(rawKey)
    }

    /**
     * 站点名称语句：sourceText 为 位置Key+langKey（如 nscn），displayText 为 “下一站中文名”。
     * 与 [getStationNameSentence] 的输出保持一致。
     */
    private fun buildStationNameSentence(
        stationPositionType: StationPositionType,
        langKey: String
    ): SentenceItem {
        val positionTypeKey = when (stationPositionType) {
            StationPositionType.DEFAULT -> "ns"
            StationPositionType.START -> "ss"
            StationPositionType.END -> "ts"
            else -> ""
        }

        val positionTypeLabel = when (stationPositionType) {
            StationPositionType.START -> "起点站"
            StationPositionType.DEFAULT -> getStationNextOrCurLabel()
            StationPositionType.END -> "终点站"
            else -> ""
        }

        val langLabel = when (langKey) {
            "cn" -> "中文"
            "en" -> "英文"
            "yue" -> "粤语"
            "ja" -> "日文"
            "ko" -> "韩文"
            else -> langKey   // 自定义输入直接展示代码
        }

        return SentenceItem(
            id = UUID.randomUUID().toString(),
            type = SentenceItemType.Placeholder,
            sourceText = "${positionTypeKey}${langKey}",
            displayText = "${positionTypeLabel}${langLabel}名"
        )
    }

    /**
     * 其他占位符语句：
     * - blankXX（如 blank500）→ displayText 为 “播报间隔500毫秒”
     * - 其余 → 从 [placeholderMap] 中查含义，查不到时回退为 rawKey 本身
     */
    private fun buildOtherPlaceholderSentence(rawKey: String): SentenceItem {
        val displayText: String = if (rawKey.startsWith("blank")) {
            val ms = rawKey.removePrefix("blank").toIntOrNull()
            if (ms != null) "播报间隔${ms}毫秒" else rawKey
        } else {
            // placeholderMap 的 key 带尖括号，这里补上再查
            placeholderMap["<$rawKey>"] ?: rawKey
        }

        return SentenceItem(
            id = UUID.randomUUID().toString(),
            type = SentenceItemType.Placeholder,
            sourceText = rawKey,
            displayText = displayText
        )
    }


    @Composable
    @Preview
    private fun MainView() {
        val controller = remember { ThemeController(ColorSchemeMode.System) }
        MiuixTheme(
            controller = controller,
        ) {
            Scaffold(
                topBar = {
                    AnSmallTopAppBar(
                        activity = this,
                        title = "编辑播报格式",
                        leftActions = {
                            IconButton(onClick = {
                                finish()
                            }) {
                                Icon(MiuixIcons.Ok, contentDescription = "完成")
                            }
                        })
                },
                content = { innerPadding ->
                    MainContent(innerPadding)
                },

                )
        }
    }

    enum class SentenceItemType {

        /** 普通语句 */
        Normal,

        /** 占位符语句 */
        Placeholder
    }

    val placeholderMap: Map<String, String> = mapOf(
        "<line>" to "当前路线名称",
        "<time>" to "当前时间",
        "<year>" to "当前年份",
        "<years>" to "当前短年份",
        "<month>" to "当前月份",
        "<date>" to "当前日期",
        "<hour>" to "当前小时数",
        "<minute>" to "当前分钟数",
        "<second>" to "当前秒数",
        "<speed>" to "当前速度",
        // TODO 动态值
        "<blankXX>" to "播报间隔"
    )

    data class SentenceItem(
        /** uuid */
        val id: String = UUID.randomUUID().toString(),
        /** 语句类型 */
        val type: SentenceItemType,
        /** 语句原始文本
         * 普通语句：如当前到站、下一站
         * 占位符语句：如nscn, line（不包括尖括号<>），参考[placeholderMap]
         */
        val sourceText: String,
        /** 语句显示文本
         * 普通语句：与[sourceText]相同
         * 占位符语句：当前站点中文, 线路名称，参考[placeholderMap] */
        val displayText: String
    )

    /**
     * 单个播报项：包含该播报项下所有语句
     */
    data class FormatItem(
        /** uuid */
        val id: String = UUID.randomUUID().toString(),
        /** 该播报项下的语句列表（即 editingSequenceList） */
        val editingSequenceList: List<SentenceItem> = emptyList()
    )


    // —— 播报项级别的操作（操作外层的 formatList） ——

    /** 新增一个播报项（默认空列表） */
    fun List<FormatItem>.appendedFormat(): List<FormatItem> =
        this + FormatItem()

    /** 删除指定索引的播报项 */
    fun List<FormatItem>.removedFormatAt(index: Int): List<FormatItem> =
        filterIndexed { i, _ -> i != index }

    /** 替换指定播报项的语句列表 */
    fun List<FormatItem>.replacedSequencesAt(
        formatIndex: Int,
        sequences: List<SentenceItem>
    ): List<FormatItem> =
        mapIndexed { i, old ->
            if (i == formatIndex) old.copy(editingSequenceList = sequences) else old
        }

    // —— 语句级别的操作（操作外层的 editingSequenceList） ——
    fun List<SentenceItem>.appended(item: SentenceItem): List<SentenceItem> = this + item

    fun List<SentenceItem>.removedAt(index: Int): List<SentenceItem> =
        filterIndexed { i, _ -> i != index }

    fun List<SentenceItem>.replacedAt(index: Int, item: SentenceItem): List<SentenceItem> =
        mapIndexed { i, old -> if (i == index) item else old }

    fun List<SentenceItem>.moved(from: Int, to: Int): List<SentenceItem> =
        toMutableList().apply { add(to, removeAt(from)) }

    @Composable
    private fun MainContent(
        innerPadding: PaddingValues
    ) {

        val coroutineScope = rememberCoroutineScope()

        /** 格式的站点状态 */
        val (stationStatus, setStationStatus) = remember { mutableIntStateOf(stationStatus) }

        /** 站点的位置类型 */
        val (stationPosition, setStationPosition) = remember { mutableIntStateOf(stationPosition) }

        /** 格式列表 */
        val (formatList, setFormatList) = remember {
            mutableStateOf(
                initialFormatList.ifEmpty {
                    // 兜底：至少保留一个空播报项
                    listOf(FormatItem())
                }
            )
        }

        fun updateFormatList(transform: (List<FormatItem>) -> List<FormatItem>) {

            val newList = transform(formatList)
//            Log.d("L440", "updateFormatList: $newList -> ${newList.size} 条")
            setFormatList(newList)

            val newFormatString = formatList2String(newList)
//            Log.d("L431", "updateFormatList: $newFormatString")
            savePrefsAndConfig(newFormatString)
        }

        val (formatIndex, setFormatIndex) = remember { mutableIntStateOf(0) }

        // 派生值：总条数
        val totalFormatCount = formatList.size

        // 当前正在编辑的播报项
        val currentFormatItem = formatList.getOrNull(formatIndex)

        // 当前播报项的语句列表
        val editingSequenceList: List<SentenceItem> =
            currentFormatItem?.editingSequenceList ?: emptyList()

        // —— 语句级别的操作（操作内层的 editingSequenceList） ——
        /** 把新的语句列表写回当前播报项 */
        fun updateCurrentSequences(newSequences: List<SentenceItem>) {
            updateFormatList { it.replacedSequencesAt(formatIndex, newSequences) }
        }



        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            val scrollState = rememberScrollState()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
            ) {
//                FormatStationStatusAndPositionSelector(
//                    stationStatus,
//                    setStationStatus,
//                    stationPosition,
//                    setStationPosition
//                )

                val stationStatusDisplayName = getStationStatusDisplayName(stationStatus)
                val stationPositionDisplayName = StationPositionType.getDisplayName(stationPosition)
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    text = "$stationStatusDisplayName $stationPositionDisplayName"
                )

                FormatIndexSelector(
                    formatIndex,
                    setFormatIndex,
                    totalFormatCount
                ) { newTotal ->
                    when {
                        newTotal > formatList.size -> {
                            // 增加播报项
                            updateFormatList { it.appendedFormat() }
                            setFormatIndex(formatList.size) // 选中新增的
                        }

                        newTotal < formatList.size -> {
                            // 删除当前播报项（原逻辑是删当前项）
                            if (formatList.size <= 1) {
                                utils.showMsg("至少保留一条播报项")
                            } else {
                                updateFormatList { it.removedFormatAt(formatIndex) }
                                setFormatIndex((formatIndex - 1).coerceAtLeast(0))
                            }
                        }

                        else -> Unit
                    }
                }

                val sequenceGridState = rememberLazyGridState()


                EditingSequenceListCard(
                    list = editingSequenceList,
                    lazyGridState = sequenceGridState,
                    setList = { updateCurrentSequences(it) }
                )

                TestAn(currentFormatItem)

                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 12.sp,
                    text = "* 轻触编辑/删除 长按拖动调整顺序"
                )

                Card(
                ) {
                    Column {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Text(
                                modifier = Modifier.padding(bottom = 4.dp),
                                text = "添加普通语句",
                                fontSize = 14.sp
                            )
                            NormalAppendSentenceCompose(
                                scrollState = scrollState,
                                onAppend = { newItem ->
                                    val newList = editingSequenceList.appended(newItem)
                                    updateCurrentSequences(newList)
                                    // 滚动到新加入的最后一项
                                    coroutineScope.launch {
                                        sequenceGridState.animateScrollToItem(newList.lastIndex)
                                    }
                                }
                            )

                        }
                        HorizontalDivider()

                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Text(
                                modifier = Modifier.padding(bottom = 4.dp),
                                text = "添加站点名称占位符语句",
                                fontSize = 14.sp
                            )
                            StationNameAppendSentenceCompose(
                                onAppend = { newItem ->
                                    val newList = editingSequenceList.appended(newItem)
                                    updateCurrentSequences(newList)
                                    // 滚动到新加入的最后一项
                                    coroutineScope.launch {
                                        sequenceGridState.animateScrollToItem(newList.lastIndex)
                                    }
                                }
                            )
                        }
                        HorizontalDivider()

                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Text(
                                modifier = Modifier.padding(bottom = 4.dp),
                                text = "添加其他占位符语句（路线名、时间、速度等）",
                                fontSize = 14.sp
                            )
                            OtherPlaceholderAppendSentenceCompose(
                                onAppend = { newItem ->
                                    val newList = editingSequenceList.appended(newItem)
                                    updateCurrentSequences(newList)
                                    coroutineScope.launch {
                                        sequenceGridState.animateScrollToItem(newList.lastIndex)
                                    }
                                }
                            )
                            // TODO
                            Text(
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                fontSize = 12.sp,
                                text = "* 下滑可选择更多"
                            )
                        }
                    }
                }

                EditSourceFormat(formatList, setFormatList)

            }
        }

    }


    fun getStationStatusDisplayName(status: Int): String {
        return when (status) {
            StationStatus.ON_NEXT -> getStationNextOrCurLabel()
            StationStatus.ON_WILL_ARRIVE -> "即将到站"
            StationStatus.ON_ARRIVE -> "到站"
            else -> "未知状态"
        }
    }

    /**
     * 当前站点位置类型
     */
    enum class StationPositionType(val value: Int, val displayName: String) {
        DEFAULT(0, "默认"),
        START(1, "起点站"),
        SECOND(2, "第二站"),
        END(3, "终点站");

        companion object {
            fun fromValue(value: Int): StationPositionType =
                entries.firstOrNull { it.value == value } ?: DEFAULT

            fun getDisplayName(value: Int): String =
                fromValue(value).displayName
        }
    }

    @Composable
    private fun FormatStationStatusAndPositionSelector(
        stationStatus: Int,
        setStationStatus: (Int) -> Unit,
        stationPosition: Int,
        setStationPosition: (Int) -> Unit
    ) {
        var stationStatusShowPopup by remember { mutableStateOf(false) }
        val stationStatusItems = listOf(
            getStationStatusDisplayName(StationStatus.ON_NEXT),
            getStationStatusDisplayName(StationStatus.ON_WILL_ARRIVE),
            getStationStatusDisplayName(StationStatus.ON_ARRIVE)
        )

        var stationPositionShowPopup by remember { mutableStateOf(false) }
        val stationPositionItems = listOf(
            StationPositionType.getDisplayName(StationPositionType.DEFAULT.value),
            StationPositionType.getDisplayName(StationPositionType.START.value),
            StationPositionType.getDisplayName(StationPositionType.SECOND.value),
            StationPositionType.getDisplayName(StationPositionType.END.value)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = getStationStatusDisplayName(stationStatus),
                    onClick = { stationStatusShowPopup = true }
                )
                WindowListPopup(
                    show = stationStatusShowPopup,
                    alignment = PopupPositionProvider.Align.Start,
                    onDismissRequest = { stationStatusShowPopup = false }
                ) {
                    val dismiss = LocalDismissState.current
                    ListPopupColumn {
                        stationStatusItems.forEachIndexed { index, string ->
                            DropdownImpl(
                                text = string,
                                optionSize = stationStatusItems.size,
                                isSelected = stationStatus == index,
                                index = index,
                                onSelectedIndexChange = {
                                    setStationStatus(index)
                                    dismiss?.invoke()
                                }
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = StationPositionType.getDisplayName(stationPosition),
                    onClick = { stationPositionShowPopup = true }
                )
                WindowListPopup(
                    show = stationPositionShowPopup,
                    alignment = PopupPositionProvider.Align.Start,
                    onDismissRequest = { stationPositionShowPopup = false }
                ) {
                    val dismiss = LocalDismissState.current
                    ListPopupColumn {
                        stationPositionItems.forEachIndexed { index, string ->
                            DropdownImpl(
                                text = string,
                                optionSize = stationPositionItems.size,
                                isSelected = stationPosition == index,
                                index = index,
                                onSelectedIndexChange = {
                                    setStationPosition(index)
                                    dismiss?.invoke()
                                }
                            )
                        }
                    }
                }
            }
        }

    }

    @Composable
    private fun FormatIndexSelector(
        index: Int,
        setIndex: (Int) -> Unit,
        total: Int,
        setTotal: (Int) -> Unit
    ) {


        Column() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 确认删除对话框
                var showConfirmDeleteDialog by remember { mutableStateOf(false) }

                IconButton(onClick = {
                    if (total <= 1) {
                        utils.showMsg("至少保留一条播报项")
                        return@IconButton
                    }
                    showConfirmDeleteDialog = true
                }) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(0.dp),
                        contentDescription = "删除播报项"
                    )
                }

                OverlayDialog(
                    title = "确认删除播报项",
                    summary = "确认删除 播报${index + 1} 吗？",
                    show = showConfirmDeleteDialog,
                    onDismissRequest = { showConfirmDeleteDialog = false } // 关闭对话框
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            text = "确认删除",
                            colors = ButtonDefaults.textButtonColors(
                                color = MiuixTheme.colorScheme.error,
                                textColor = Color.White
                            ),
                            onClick = {
                                setIndex(index - 1)
                                setTotal(total - 1)
                                showConfirmDeleteDialog = false
                            }, // 关闭对话框
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            text = "取消",
                            onClick = {
                                showConfirmDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                }

                Text(
                    text = "共 $total 条播报"
                )


                IconButton(onClick = {
                    setTotal(total + 1)
                    setIndex(total)
                }) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(0.dp),
                        contentDescription = "添加播报项"
                    )
                }


            }

            val indexTabs = (0 until total).toList()
            val tabListState = rememberLazyListState()

            TabRowWithContour(
                modifier = Modifier.fillMaxWidth(),
                tabs = indexTabs.map { "播报${it + 1}" },
                selectedTabIndex = index,
                onTabSelected = { setIndex(it) },
                listState = tabListState,
            )
        }

    }

    @Composable
    fun EditingSequenceListCard(
        list: List<SentenceItem>,
        lazyGridState: LazyGridState,
        setList: (List<SentenceItem>) -> Unit
    ) {

        // 是否显示操作对话框
        var showItemActionDialog by remember { mutableStateOf(false) }
        // 当前操作的语句索引（-1 表示无）
        var actionTargetIndex by remember { mutableIntStateOf(-1) }

        // 编辑中的文本（仅普通语句使用）
        var editingText by remember { mutableStateOf("") }

        val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
            setList(list.moved(from.index, to.index))
        }

        val columnsCount = if (list.size > 6) 2 else 1

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MiuixTheme.colorScheme.primary,
                    shape = RoundedCornerShape(CardDefaults.CornerRadius)
                )
                .height(300.dp),
        ) {
            if (list.isEmpty()) {
                // 空状态：仍保留在带边框的 Card 内
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = MiuixIcons.ListView,
                        modifier = Modifier.size(48.dp),
                        contentDescription = "暂无播报语句",
                        tint = MiuixTheme.colorScheme.onSecondaryVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "暂无播报语句\n请在下方添加",
                        color = MiuixTheme.colorScheme.onSecondaryVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    state = lazyGridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = list,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        ReorderableItem(reorderableLazyGridState, key = item.id) { isDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 0.9f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "dragScale"
                            )
                            val contentColor =
                                if (item.type == SentenceItemType.Placeholder)
                                    MiuixTheme.colorScheme.primary
                                else
                                    MiuixTheme.colorScheme.onSecondaryVariant

                            Card(
                                pressFeedbackType = PressFeedbackType.Sink,
                                colors = CardColors(
                                    color = MiuixTheme.colorScheme.secondaryVariant,
                                    contentColor = contentColor,
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .animateItem()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .longPressDraggableHandle(
                                        onDragStopped = { /* 可添加触感反馈 */ }
                                    ),
                                onClick = {
                                    actionTargetIndex = index
                                    // 初始化编辑文本（仅普通语句有实际内容）
                                    editingText = item.sourceText
                                    showItemActionDialog = true
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        modifier = Modifier,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val format = sentenceItem2String(item)

                                            val intent = Intent()
                                                .setAction(utils.tryListeningAnActionName)
                                                .putExtra("format", format)
                                            LocalBroadcastManager.getInstance(this@AnFormatEditorActivity)
                                                .sendBroadcast(intent)

                                        }
                                    ) {
                                        Icon(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(0.dp),
                                            imageVector = MiuixIcons.Play,
                                            contentDescription = "试听播报项"
                                        )
                                    }
                                    Text(text = item.displayText, modifier = Modifier)
                                }
                            }
                        }
                    }
                }
            }
        }

        // —— 合并后的语句操作/编辑对话框 ——
        val targetItem = list.getOrNull(actionTargetIndex)
        val isNormal = targetItem?.type == SentenceItemType.Normal

        OverlayDialog(
            title = "编辑播报语句",
            show = showItemActionDialog,
            onDismissRequest = { showItemActionDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isNormal) {
                    // 普通语句：直接内嵌输入框
                    TextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "编辑语句",
                        singleLine = true
                    )
                    TextButton(
                        text = "确认编辑",
                        colors = ButtonDefaults.textButtonColors(
                            color = MiuixTheme.colorScheme.primary,
                            textColor = Color.White
                        ),
                        onClick = {
                            val newText = editingText.trim()
                            val current = list.getOrNull(actionTargetIndex)
                            if (newText.isNotEmpty() && current != null) {
                                // 普通语句：sourceText 与 displayText 一致
                                setList(
                                    list.replacedAt(
                                        actionTargetIndex,
                                        current.copy(
                                            sourceText = newText,
                                            displayText = newText
                                        )
                                    )
                                )
                            }
                            showItemActionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    text = "确认删除",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = Color.White
                    ),
                    onClick = {
                        if (actionTargetIndex in list.indices) {
                            setList(list.removedAt(actionTargetIndex))
                        }
                        showItemActionDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "取消",
                    onClick = {
                        showItemActionDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 试听
    @Composable
    fun TestAn(currentFormatItem: FormatItem?) {
        Button(
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (currentFormatItem == null) return@Button
                val formatStr = formatItem2String(currentFormatItem)

                val intent = Intent()
                    .setAction(utils.tryListeningAnActionName)
                    .putExtra("format", formatStr)
                LocalBroadcastManager.getInstance(this@AnFormatEditorActivity)
                    .sendBroadcast(intent)

            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(0.dp),
                    imageVector = MiuixIcons.Play,
                    contentDescription = "试听播报"
                )
                Text("试听播报")
            }
        }
    }

    // 添加站点名称语句
    @Composable
    fun StationNameAppendSentenceCompose(onAppend: (SentenceItem) -> Unit) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StationNameButton(
                modifier = Modifier.weight(1f),
                text = "起点站",
                stationPositionType = StationPositionType.START,
                alignment = PopupPositionProvider.Align.Start,
                onAppend = onAppend
            )
            StationNameButton(
                modifier = Modifier.weight(1f),
                text = getStationNextOrCurLabel(),
                stationPositionType = StationPositionType.DEFAULT,
                alignment = PopupPositionProvider.Align.Start,
                onAppend = onAppend
            )
            StationNameButton(
                modifier = Modifier.weight(1f),
                text = "终点站",
                stationPositionType = StationPositionType.END,
                alignment = PopupPositionProvider.Align.End,
                onAppend = onAppend
            )
        }
    }

    @Composable
    private fun StationNameButton(
        modifier: Modifier = Modifier,
        text: String,
        stationPositionType: StationPositionType,
        onAppend: (SentenceItem) -> Unit,
        alignment: PopupPositionProvider.Align
    ) {
        var showPopup by remember { mutableStateOf(false) }

        // 控制“其他”语言输入框的显示
        var showCustomLangDialog by remember { mutableStateOf(false) }
        // 用户输入的 langKey
        var customLangKey by remember { mutableStateOf("") }

        val items = listOf("中文", "英文", "其他")

        Box(modifier = modifier) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showPopup = true }
            ) {
                Text(text)
            }

            WindowListPopup(
                show = showPopup,
                alignment = alignment,
                onDismissRequest = { showPopup = false }
            ) {
                val dismiss = LocalDismissState.current
                ListPopupColumn {
                    items.forEachIndexed { index, string ->
                        DropdownImpl(
                            text = string,
                            optionSize = items.size,
                            isSelected = false,
                            index = index,
                            onSelectedIndexChange = {
                                dismiss?.invoke()
                                if (index == 2) {
                                    // “其他”：弹出输入框
                                    customLangKey = ""
                                    showCustomLangDialog = true
                                } else {
                                    val langKey = getLangKey(index)
                                    val sentenceItem =
                                        getStationNameSentence(stationPositionType, langKey)
                                    onAppend(sentenceItem)
                                }
                            }
                        )
                    }
                }
            }

            // “其他”语言输入框
            OverlayDialog(
                title = "输入语言代码",
                summary = "请填写 $text 名称的语言代码，例如 ja、ko、fr 等",
                show = showCustomLangDialog,
                onDismissRequest = { showCustomLangDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = customLangKey,
                        onValueChange = { customLangKey = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = "语言代码",
                        singleLine = true
                    )
                    TextButton(
                        text = "确认",
                        colors = ButtonDefaults.textButtonColors(
                            color = MiuixTheme.colorScheme.primary,
                            textColor = Color.White
                        ),
                        onClick = {
                            val langKey = customLangKey.trim()
                            if (langKey.isEmpty()) {
                                // 可选：提示用户
                                return@TextButton
                            }
                            val sentenceItem =
                                getStationNameSentence(stationPositionType, langKey)
                            onAppend(sentenceItem)
                            showCustomLangDialog = false
                            customLangKey = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "取消",
                        onClick = {
                            showCustomLangDialog = false
                            customLangKey = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    fun getLangKey(index: Int): String {
        when (index) {
            0 -> {
                return "cn"
            }

            1 -> {
                return "en"
            }

            else -> {
                // TODO 手动输入其他语言
                return ""
            }
        }
    }

    fun getStationNameSentence(
        stationPositionType: StationPositionType,
        langKey: String
    ): SentenceItem {

        val positionTypeKey = when (stationPositionType) {
            StationPositionType.DEFAULT -> {
                "ns"
            }

            StationPositionType.START -> {
                "ss"
            }

            StationPositionType.END -> {
                "ts"
            }

            else -> {
                ""
            }
        }

        val positionTypeLabel = when (stationPositionType) {
            StationPositionType.DEFAULT -> {
                getStationNextOrCurLabel()
            }

            StationPositionType.START -> {
                "起点站"
            }

            StationPositionType.END -> {
                "终点站"
            }

            else -> {
                ""
            }
        }

        val langLabel = when (langKey) {
            "cn" -> "中文"
            "en" -> "英文"
            "yue" -> "粤语"
            "ja" -> "日文"
            "ko" -> "韩文"
            else -> langKey   // 自定义输入直接展示代码
        }

        val sentenceItem = SentenceItem(
            id = UUID.randomUUID().toString(),
            type = SentenceItemType.Placeholder,
            sourceText = "${positionTypeKey}${langKey}",
            displayText = "${positionTypeLabel}${langLabel}名"
        )

        return sentenceItem
    }


    // 添加其他占位符语句（如路线名、时间）
    @Composable
    fun OtherPlaceholderAppendSentenceCompose(
        onAppend: (SentenceItem) -> Unit = {}
    ) {
        // 占位符定义：key 为带尖括号的占位符，value 为含义
        // 复用已有的 placeholderMap，但需要过滤掉站点名称类（ns/ss/ts 系列由另一个组件处理）
        // 这里 placeholderMap 本身就是非站点名称的占位符，直接使用
        val placeholderItems = placeholderMap.entries.toList()

        var showPopup by remember { mutableStateOf(false) }

        // “播报间隔”输入框状态
        var showBlankDialog by remember { mutableStateOf(false) }
        var blankMsText by remember { mutableStateOf("") }

        Box {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showPopup = true }
            ) {
                Text("添加其他占位符语句")
            }

            WindowListPopup(
                show = showPopup,
                alignment = PopupPositionProvider.Align.Start,
                onDismissRequest = { showPopup = false }
            ) {
                val dismiss = LocalDismissState.current
                ListPopupColumn {
                    placeholderItems.forEachIndexed { index, (placeholder, meaning) ->
                        DropdownImpl(
                            text = meaning,
                            optionSize = placeholderItems.size,
                            isSelected = false,
                            index = index,
                            onSelectedIndexChange = {
                                dismiss?.invoke()
                                if (placeholder == "<blankXX>") {
                                    // 播报间隔：需要用户输入毫秒数
                                    blankMsText = ""
                                    showBlankDialog = true
                                } else {
                                    // 普通占位符：直接创建语句
                                    onAppend(buildPlaceholderSentence(placeholder, meaning))
                                }
                            }
                        )
                    }
                }
            }

            // 播报间隔毫秒数输入框
            OverlayDialog(
                title = "输入播报间隔",
                summary = "请填写播报中断的毫秒数，例如 500、1000",
                show = showBlankDialog,
                onDismissRequest = { showBlankDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = blankMsText,
                        onValueChange = { blankMsText = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = "间隔毫秒数",
                        singleLine = true
                    )
                    TextButton(
                        text = "确认",
                        colors = ButtonDefaults.textButtonColors(
                            color = MiuixTheme.colorScheme.primary,
                            textColor = Color.White
                        ),
                        onClick = {
                            val ms = blankMsText.trim().toIntOrNull()
                            if (ms == null || ms < 0) {
                                utils.showMsg("请输入有效的毫秒数")
                                return@TextButton
                            }
                            // sourceText 为 blankXX（XX 替换为具体毫秒数），不带尖括号
                            val sentenceItem = SentenceItem(
                                id = UUID.randomUUID().toString(),
                                type = SentenceItemType.Placeholder,
                                sourceText = "blank$ms",
                                displayText = "播报间隔${ms}毫秒"
                            )
                            onAppend(sentenceItem)
                            showBlankDialog = false
                            blankMsText = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "取消",
                        onClick = {
                            showBlankDialog = false
                            blankMsText = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    /**
     * 根据占位符及其含义构建一条占位符语句。
     * sourceText 不带尖括号，displayText 为含义。
     */
    private fun buildPlaceholderSentence(placeholder: String, meaning: String): SentenceItem {
        // 去掉首尾尖括号：<line> -> line
        val rawKey = placeholder.removePrefix("<").removeSuffix(">")
        return SentenceItem(
            id = UUID.randomUUID().toString(),
            type = SentenceItemType.Placeholder,
            sourceText = rawKey,
            displayText = meaning
        )
    }

    /**
     * 添加普通语句：用户输入一段文本，确认后追加到列表中。
     * 输入内容不能包含尖括号 '<' '>' 和竖线 '|'。
     */
    @Composable
    fun NormalAppendSentenceCompose(
        onAppend: (SentenceItem) -> Unit = {},
        scrollState: ScrollState
    ) {
        var inputText by remember { mutableStateOf("") }

        // 关键词提示（来自 common 目录所有音频文件名）
        val allKeywords = remember { announcementProvider.getCommonMediaKeywords() }

        // like 匹配：包含输入内容（忽略大小写），最多展示 10 条
        val suggestions = remember(inputText, allKeywords) {
            if (inputText.isBlank()) emptyList()
            else allKeywords
                .filter { it.contains(inputText, ignoreCase = true) }
                .take(10)
        }

        // 校验：不能含尖括号和竖线
        val isInputValid = inputText.isNotEmpty() &&
                !inputText.contains('<') &&
                !inputText.contains('>') &&
                !inputText.contains('|')


        // 记录本组件在父滚动容器中的位置，用于 suggestions 出现时滚动到它
        var anchorY by remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        // 关键：suggestions 从空变为非空时，把输入框滚动到可视区顶部附近
        LaunchedEffect(suggestions.isNotEmpty()) {
            if (suggestions.isNotEmpty()) {
                // 留出顶部一点空间，让下方提示列表完整露出
                val target = (anchorY - 100).coerceAtLeast(0)
                scrollState.animateScrollTo(target)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.onGloballyPositioned {
                // 相对父可滚动内容的 y 坐标
                anchorY = it.positionInParent().y.toInt()
            }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = "搜索普通语句",
                    singleLine = true
                )
                TextButton(
                    modifier = Modifier.fillMaxHeight(),
                    text = "确认添加",
                    enabled = isInputValid,
                    onClick = {
                        val text = inputText.trim()
                        if (text.isEmpty()) {
                            utils.showMsg("请输入语句内容")
                            return@TextButton
                        }
                        if (text.contains('<') || text.contains('>') || text.contains('|')) {
                            utils.showMsg("语句不能包含尖括号 < > 和竖线 |")
                            return@TextButton
                        }
                        onAppend(
                            SentenceItem(
                                id = UUID.randomUUID().toString(),
                                type = SentenceItemType.Normal,
                                sourceText = text,
                                displayText = text
                            )
                        )
                        inputText = ""
                    }
                )
            }

            // 搜索提示词列表
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        suggestions.forEach { keyword ->
                            BasicComponent(
                                title = keyword,
                                onClick = {
                                    inputText = keyword
                                }
                            )
                        }
                    }
                }
            }

            // 非法字符提示
            if (inputText.isNotEmpty() && !isInputValid) {
                Text(
                    text = "语句不能包含尖括号 < > 和竖线 |",
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    fun getStationNextOrCurLabel(): String {
        return if (stationStatus == StationStatus.ON_NEXT) {
            "下一站"
        } else {
            "当前站"
        }
    }

    fun formatList2String(formatList: List<FormatItem>): String {
        return formatList
            .map { formatItem2String(it) }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    fun formatItem2String(formatItem: FormatItem): String {
        return formatItem.editingSequenceList.joinToString(separator = "|") {
            sentenceItem2String(it)
        }
    }

    fun sentenceItem2String(sentenceItem: SentenceItem): String {
        return when (sentenceItem.type) {
            SentenceItemType.Placeholder -> "<${sentenceItem.sourceText}>"
            SentenceItemType.Normal -> sentenceItem.sourceText
        }
    }

    @Composable
    fun EditSourceFormat(formatList: List<FormatItem>, setFormatList: (List<FormatItem>) -> Unit) {

        val sourceFormat = formatList2String(formatList)

        var showDialog by remember { mutableStateOf(false) }
        var sourceText by remember { mutableStateOf("") }




        Button(
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // 打开时同步当前源文本
                sourceText = sourceFormat
                showDialog = true
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(0.dp),
                    imageVector = MiuixIcons.Edit,
                    contentDescription = "试听播报"
                )
                Text("编辑播报格式源文本")
            }
        }

        OverlayDialog(
            title = "编辑播报格式源文本",
            show = showDialog,
            onDismissRequest = {
                showDialog = false
                // 中断播报
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent().setAction(utils.pauseAnnounceName))
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // 多行输入框，限定高度可滚动（避免长文本撑爆对话框）
                TextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 280.dp),
                    label = "播报格式源文本",
                    singleLine = false,
                    // 多行文本需要在 TextField 内滚动，这里保持默认即可
                )

                // 试听 / 帮助 一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        text = "试听",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (sourceText.isEmpty()) {
                                utils.showMsg("请输入播报格式")
                                return@TextButton
                            }
                            val ans = utils.getAnnouncements(sourceText)
                            if (ans[0] == "ERROR") {
                                utils.showMsg("${ans[1]}不正确，请修改")
                            } else {
                                LocalBroadcastManager.getInstance(this@AnFormatEditorActivity)
                                    .sendBroadcast(
                                        Intent()
                                            .setAction(utils.tryListeningAnActionName)
                                            .putExtra("format", sourceText)
                                    )
                            }
                        }
                    )
                    TextButton(
                        text = "帮助",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            utils.openHelperDialog("查看语音播报文档", "readme/语音播报.md")
                        }
                    )
                }

                // 保存
                TextButton(
                    text = "保存",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        textColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // 允许填空（空字符串表示清空格式），但非空时需要校验
                        if (sourceText.isNotEmpty()) {
                            val ans = utils.getAnnouncements(sourceText)
                            if (ans[0] == "ERROR") {
                                utils.showMsg("${ans[1]}不正确，请修改")
                                return@TextButton
                            }
                        }

                        val newList = parseSourceFormat(sourceText)
                        setFormatList(newList)

                        utils.showMsg("播报格式设置成功")
                        showDialog = false
                    }
                )

                // 取消
                TextButton(
                    text = "取消",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showDialog = false
                    }
                )
            }
        }
    }

    fun savePrefsAndConfig(newFormatString: String) {
        prefs.edit {
            putString(
                "${positionKey}${stateKey}AnnouncementExpression",
                newFormatString
            )
        }

        // 同步修改config.json
        utils.updateAnnouncementFormatConfig(
            positionKey!!,
            stateKey!!,
            newFormatString
        )
    }

}