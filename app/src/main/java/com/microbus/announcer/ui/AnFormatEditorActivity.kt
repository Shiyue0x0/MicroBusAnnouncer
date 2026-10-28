package com.microbus.announcer.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.preference.PreferenceManager
import com.microbus.announcer.BaseActivity
import com.microbus.announcer.Utils
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utils = Utils(this)
        lineDatabaseHelper = LineDatabaseHelper.getInstance(this)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        enableEdgeToEdge()
        setContent {
            MainView()
        }
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
                    AnSmallTopAppBar(this, "编辑播报格式")
                },
                content = { innerPadding ->
                    MainContent(innerPadding)
                },
                floatingActionButtonPosition = FabPosition.End
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
        val (stationStatus, setStationStatus) = remember { mutableIntStateOf(StationStatus.ON_NEXT) }

        /** 站点的位置类型 */
        val (stationPosition, setStationPosition) = remember { mutableIntStateOf(StationPositionType.DEFAULT.value) }

        /** 格式列表 */
        val (formatList, setFormatList) = remember {
            mutableStateOf(
                listOf(
                    FormatItem(
                        editingSequenceList = listOf(
                            SentenceItem(
                                id = UUID.randomUUID().toString(),
                                type = SentenceItemType.Normal,
                                sourceText = "当前到站",
                                displayText = "当前到站"
                            ),
                            SentenceItem(
                                id = UUID.randomUUID().toString(),
                                type = SentenceItemType.Normal,
                                sourceText = "下一站",
                                displayText = "下一站"
                            ),
                            SentenceItem(
                                id = UUID.randomUUID().toString(),
                                type = SentenceItemType.Placeholder,
                                sourceText = "<nscn>",
                                displayText = "下一站中文名"
                            )
                        )
                    )
                )
            )
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
            setFormatList(formatList.replacedSequencesAt(formatIndex, newSequences))
        }



        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(bottom = 16.dp)
            ) {
                FormatStationStatusAndPositionSelector(
                    stationStatus,
                    setStationStatus,
                    stationPosition,
                    setStationPosition
                )
                FormatIndexSelector(
                    formatIndex,
                    setFormatIndex,
                    totalFormatCount,
                    { newTotal ->
                        when {
                            newTotal > formatList.size -> {
                                // 增加播报项
                                setFormatList(formatList.appendedFormat())
                                setFormatIndex(formatList.size) // 选中新增的
                            }

                            newTotal < formatList.size -> {
                                // 删除当前播报项（原逻辑是删当前项）
                                if (formatList.size <= 1) {
                                    utils.showMsg("至少保留一条播报项")
                                } else {
                                    setFormatList(formatList.removedFormatAt(formatIndex))
                                    setFormatIndex((formatIndex - 1).coerceAtLeast(0))
                                }
                            }

                            else -> Unit
                        }
                    }
                )

                val sequenceGridState = rememberLazyGridState()


                EditingSequenceListCard(
                    list = editingSequenceList,
                    lazyGridState = sequenceGridState,
                    setList = { updateCurrentSequences(it) }
                )

                Text("添加站点名称语句")
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
        }

    }

    fun getStationStatusDisplayName(status: Int): String {
        return when (status) {
            StationStatus.ON_NEXT -> "下一站"
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

            TabRow(
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

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(text = "暂无播报语句", modifier = Modifier.padding(16.dp))
            }
            return
        }

        val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
            // 当拖拽移动时，同步更新数据源顺序
            setList(list.moved(from.index, to.index))
        }

        val columnsCount = if (list.size > 3) 2 else 1

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MiuixTheme.colorScheme.primary,
                    shape = RoundedCornerShape(CardDefaults.CornerRadius)
                )
                .heightIn(max = 300.dp),
        ) {
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

                        // 拖拽状态下的缩放动画
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
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .longPressDraggableHandle(
                                    onDragStopped = { /* 可添加触感反馈 */ }
                                ),
                            onClick = {
//                                utils.showMsg("点击播报项 $index")
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(text = "${index + 1}", modifier = Modifier, fontSize = 14.sp)
//                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
//                                        utils.showMsg("试听播报项 $index")
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

    // 添加站点名称语句
    @Composable
    fun StationNameAppendSentenceCompose(onAppend: (SentenceItem) -> Unit) {

        var showPopup by remember { mutableStateOf(false) }
        var chooseStationPositionType by remember { mutableStateOf(StationPositionType.DEFAULT) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    chooseStationPositionType = StationPositionType.START
                    showPopup = true
                }
            ) {
                Text("起点站")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    chooseStationPositionType = StationPositionType.DEFAULT
                    showPopup = true
                }
            ) {
                // 下一站或到达站
                Text("下一站")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    chooseStationPositionType = StationPositionType.END
                    showPopup = true
                }
            ) {
                Text("终点站")
            }
        }
        val items = listOf("中文", "英文", "其他")

        WindowListPopup(
            show = showPopup,
            alignment = PopupPositionProvider.Align.Start,
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
                            val langKey = getLangKey(index)
                            val sentenceItem =
                                getStationNameSentence(chooseStationPositionType, langKey)
                            onAppend(sentenceItem)
                            dismiss?.invoke()
                        }
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
                "下一站"
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
            "cn" -> {
                "中文"
            }

            "en" -> {
                "英文"
            }

            else -> {
                langKey
            }
        }

        val sentenceItem = SentenceItem(
            id = UUID.randomUUID().toString(),
            type = SentenceItemType.Placeholder,
            sourceText = "${positionTypeKey}${langKey}",
            displayText = "${positionTypeLabel}${langLabel}名"
        )

        return sentenceItem
    }

}