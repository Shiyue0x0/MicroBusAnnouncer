package com.microbus.announcer.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.microbus.announcer.BaseActivity
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.model.StationStatus
import com.microbus.announcer.service.LinePathAndStationPointService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowListPopup
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * 站点序列的一项 UI 条目。
 *
 * [uid] 仅在本次编辑会话内唯一，作为 Compose 的稳定 key，
 * 用于在存在重复 [stationId] 时区分不同出现位置（如环线/折返）。
 * [stationId] 才是写入数据库的真正站点 id。
 */
private data class StationEntry(
    val uid: Long,
    val stationId: Int
)

class LineEditorActivity : BaseActivity() {

    private var tag = javaClass.simpleName
    private lateinit var utils: Utils
    private lateinit var lineDatabaseHelper: LineDatabaseHelper
    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var linePathAndStationPointService: LinePathAndStationPointService
    private lateinit var totalStationPointService: LinePathAndStationPointService

    /** 是否编辑模式（intent 带 id） */
    private var isEditing = false

    /** 传入的路线 id（编辑模式） */
    private var lineId: Int? = null

    /** 当前路线数据 */
    private var lineState by mutableStateOf(Line())

    // 保留 line 的便捷读写（可选）
    private var line: Line
        get() = lineState
        set(value) {
            lineState = value
        }

    /** 上行站点条目序列（含 uid，允许 stationId 重复） */
    private var upEntries by mutableStateOf<List<StationEntry>>(emptyList())

    /** 下行站点条目序列（含 uid，允许 stationId 重复） */
    private var downEntries by mutableStateOf<List<StationEntry>>(emptyList())

    /** 新建模式下是否有未保存的变更 */
    private var isDirty = false

    /** 站点 id -> Station 的缓存（用于展示站名） */
    private val stationCache = mutableMapOf<Int, Station>()

    /** uid 生成器（会话内唯一） */
    private var uidCounter by mutableLongStateOf(0L)
    private fun nextUid(): Long = uidCounter++

    // ★ 新增：当前编辑方向（0=上行，1=下行），提升到 Activity 供地图回调共享
    private var currentDirState by mutableIntStateOf(0)

    // ★ 新增：地图站点删除确认相关 state（提升到 Activity，供地图回调与 Compose 共享）
    /** 待删除的站点条目 uid（null 表示无） */
    private var deleteTargetUid by mutableStateOf<Long?>(null)

    /** 删除确认弹窗是否显示 */
    private var showDeleteDialog by mutableStateOf(false)


    /** 地图上点击的 total 站点 id（null 表示无） */
    private var totalPickStationId by mutableStateOf<Int?>(null)

    /** 添加 total 站点对话框是否显示 */
    private var showTotalPickDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utils = Utils(this)
        lineDatabaseHelper = LineDatabaseHelper.getInstance(this)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(this)
        linePathAndStationPointService = LinePathAndStationPointService(this)
        totalStationPointService = LinePathAndStationPointService(this)

        initExtra()

        enableEdgeToEdge()
        setContent {
            MainView()
        }
    }

    private fun initExtra() {
        lineId = intent.getIntExtra("lineId", -1).takeIf { it > 0 }
        isEditing = lineId != null

        if (isEditing) {
            val loaded = lineDatabaseHelper.queryById(lineId!!).firstOrNull()
            Log.d(tag, "initExtra: $loaded")
            if (loaded == null) {
                utils.showMsg("路线不存在")
                finishWithResult()
                return
            }
            line = loaded
        } else {
            line = Line(type = "B")
        }

        // 加载并解析站点序列 -> StationEntry
        upEntries = parseStationEntries(line.upLineStation)
        downEntries = parseStationEntries(line.downLineStation)

        // 预热站点缓存
        preloadStationCache()
    }

    /**
     * 把空格分隔的站点 id 字符串解析为 List<StationEntry>。
     * 非法 token（非数字）会被排除并提示。重复 id 会保留（各自分配独立 uid）。
     */
    private fun parseStationEntries(raw: String): List<StationEntry> {
        if (raw.isBlank()) return emptyList()
        val result = mutableListOf<StationEntry>()
        val invalid = mutableListOf<String>()
        raw.split(" ").forEach { token ->
            if (token.isBlank()) return@forEach
            val id = token.toIntOrNull()
            if (id == null) invalid += token else result += StationEntry(nextUid(), id)
        }
        if (invalid.isNotEmpty()) {
            utils.showMsg("存在非法站点 id：${invalid.joinToString(", ")}，已忽略")
        }
        return result
    }

    private fun preloadStationCache() {
        stationCache.clear()
        (upEntries + downEntries).map { it.stationId }.distinct().forEach { id ->
            val station = stationDatabaseHelper.queryById(id).firstOrNull()
            if (station != null) {
                station.cnName = utils.getStationNameFromCn(station.cnName, "cn", station)
                stationCache[id] = station
            }
        }
    }

    private fun getStationDisplayName(id: Int): String {
        stationCache[id]?.let { return it.cnName }
        val station = stationDatabaseHelper.queryById(id).firstOrNull()
        return if (station != null) {
            stationCache[id] = station
            station.cnName = utils.getStationNameFromCn(station.cnName, "cn", station)
            station.cnName
        } else {
            "未知站点(#$id)"
        }
    }

    // —— 写入逻辑 ——

    /** 编辑模式：立即写库；新建模式：仅标记 dirty */
    private fun saveLineNow() {
        if (isEditing) {
            lineDatabaseHelper.updateById(lineId!!, line)
        } else {
            isDirty = true
        }
    }

    private fun updateLine(transform: (Line) -> Line) {
        line = transform(line)
        saveLineNow()
    }

    /** 把 StationEntry 列表序列化为空格分隔的站点 id 字符串 */
    private fun serializeEntries(entries: List<StationEntry>): String =
        entries.joinToString(" ") { it.stationId.toString() }

    private fun updateUpEntries(transform: (List<StationEntry>) -> List<StationEntry>) {
        upEntries = transform(upEntries)
        updateLine { it.copy(upLineStation = serializeEntries(upEntries)) }
    }

    private fun updateDownEntries(transform: (List<StationEntry>) -> List<StationEntry>) {
        downEntries = transform(downEntries)
        updateLine { it.copy(downLineStation = serializeEntries(downEntries)) }
    }

    // ★ 新增：当前是否处于上行方向
    private fun isUpDirActive(): Boolean = currentDirState == 0

    // —— UI ——

    @Composable
    @Preview
    private fun MainView() {
        val controller = remember { ThemeController(ColorSchemeMode.System) }
        MiuixTheme(controller = controller) {
            Scaffold(
                content = { innerPadding ->
                    MainContent(innerPadding)
                }
            )
        }
    }

    @Composable
    private fun MainContent(innerPadding: PaddingValues) {

        // ★ 修改：currentDir 改为引用 Activity 成员 state，供地图回调共享
        val currentDir = currentDirState
        val setCurrentDir: (Int) -> Unit = { currentDirState = it }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 底层地图
            AMapCompose(modifier = Modifier.fillMaxSize(), currentDir)

            // 上层可滚动表单
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                BasicInfoButton()
                StationCardSection(currentDir, setCurrentDir)
            }

            // ★ 新增：地图站点删除确认弹窗（在地图回调中被触发）
            StationDeleteDialog(
                show = showDeleteDialog,
                targetUid = deleteTargetUid,
                onDismiss = {
                    showDeleteDialog = false
                    deleteTargetUid = null
                }
            )

            // 地图 total 站点点击：添加到末尾 / 删除
            TotalPickDialog(
                show = showTotalPickDialog,
                stationId = totalPickStationId,
                onDismiss = {
                    showTotalPickDialog = false
                    totalPickStationId = null
                }
            )
        }
    }

    // —— 基本信息：按钮 + 弹窗 ——
    @Composable
    private fun BasicInfoButton() {
        var showDialog by remember { mutableStateOf(false) }

        val typeLabel = when (line.type) {
            "C" -> "C 社区"
            "B" -> "B 公交"
            "U" -> "U 地铁"
            "T" -> "T 火车"
            else -> "B 公交"
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            BasicComponent(
                title = "编辑基本信息",
                summary = "${line.name.ifBlank { "未命名" }} · $typeLabel${if (line.isRingRoute) " · 环线" else ""}",
                onClick = { showDialog = true }
            )
        }

        BasicInfoDialog(
            show = showDialog,
            onDismiss = { showDialog = false }
        )
    }

    @Composable
    private fun BasicInfoDialog(
        show: Boolean,
        onDismiss: () -> Unit
    ) {
        OverlayDialog(
            title = "编辑路线基本信息",
            show = show,
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = line.name,
                    onValueChange = { newName ->
                        updateLine { it.copy(name = newName) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "路线名称",
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LineTypeSelector(
                            type = line.type,
                            onTypeChange = { newType ->
                                updateLine { it.copy(type = newType) }
                            }
                        )
                    }
                    Text(text = "环线")
                    Switch(
                        checked = line.isRingRoute,
                        onCheckedChange = { checked ->
                            updateLine { it.copy(isRingRoute = checked) }
                        }
                    )
                }

                TextButton(
                    text = "完成",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        textColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }

    /** 路线类型下拉选择：C社区 | B公交 | U地铁 | T火车 */
    @Composable
    private fun LineTypeSelector(type: String, onTypeChange: (String) -> Unit) {
        var showPopup by remember { mutableStateOf(false) }
        val typeEntries = listOf(
            "C" to "C 社区",
            "B" to "B 公交",
            "U" to "U 地铁",
            "T" to "T 火车"
        )
        val displayName = typeEntries.firstOrNull { it.first == type }?.second ?: "B 公交"

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            text = displayName,
            onClick = { showPopup = true }
        )
        WindowListPopup(
            show = showPopup,
            alignment = PopupPositionProvider.Align.Start,
            onDismissRequest = { showPopup = false }
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                typeEntries.forEachIndexed { index, (key, label) ->
                    DropdownImpl(
                        text = label,
                        optionSize = typeEntries.size,
                        isSelected = type == key,
                        index = index,
                        onSelectedIndexChange = {
                            dismiss?.invoke()
                            onTypeChange(key)
                        }
                    )
                }
            }
        }
    }

    // —— 站点序列区：tab + 卡片 + 源文本 + 添加 ——

    @Composable
    private fun StationCardSection(
        currentDir: Int,
        setCurrentDir: (Int) -> Unit
    ) {
        // 环线切换后，若当前是下行则回到上行
        LaunchedEffect(line.isRingRoute) {
            if (line.isRingRoute && currentDir == 1) setCurrentDir(0)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!line.isRingRoute) {
                val tabs = listOf("上行", "下行")
                val tabListState = rememberLazyListState()
                TabRowWithContour(
                    modifier = Modifier.fillMaxWidth(),
                    tabs = tabs,
                    selectedTabIndex = currentDir,
                    onTabSelected = { setCurrentDir(it) },
                    listState = tabListState,
                )
            } else {
                Card(
                    insideMargin = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        text = "环线（仅上行）"
                    )
                }
            }

            val currentEntries = if (currentDir == 0) upEntries else downEntries
            val setCurrentEntries: (List<StationEntry>) -> Unit = { newList ->
                if (currentDir == 0) updateUpEntries { newList } else updateDownEntries { newList }
            }

            StationCard(
                entries = currentEntries,
                setEntries = setCurrentEntries,
                dirKey = currentDir
            )
        }
    }

    @Composable
    private fun StationCard(
        entries: List<StationEntry>,
        setEntries: (List<StationEntry>) -> Unit,
        dirKey: Int
    ) {
        // 编辑源文本对话框
        var showSourceDialog by remember { mutableStateOf(false) }
        var sourceText by remember { mutableStateOf("") }

        // 添加站点对话框
        var showAddDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .border(
                    width = 1.5.dp,
                    color = MiuixTheme.colorScheme.primary,
                    shape = RoundedCornerShape(CardDefaults.CornerRadius)
                )
        ) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = MiuixIcons.ListView,
                        modifier = Modifier.size(48.dp),
                        contentDescription = "暂无站点",
                        tint = MiuixTheme.colorScheme.onSecondaryVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "暂无站点\n请在下方添加",
                        color = MiuixTheme.colorScheme.onSecondaryVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    // 按 uid 重排，重复 stationId 不影响
                    val mutable = entries.toMutableList()
                    val item = mutable.removeAt(from.index)
                    mutable.add(to.index, item)
                    setEntries(mutable)
                }

                // 每个方向独立记录上一次 size；dirKey 变化时重新初始化
                var lastSize by remember(dirKey) { mutableIntStateOf(entries.size) }
                LaunchedEffect(dirKey, entries.size) {
                    if (entries.size > lastSize) {
                        lazyListState.animateScrollToItem(entries.size - 1)
                    }
                    lastSize = entries.size
                }

                LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = entries.size,
                        // key 使用 uid，保证重复 stationId 时唯一且重排稳定
                        key = { index -> entries[index].uid }
                    ) { index ->
                        val entry = entries[index]
                        val stationId = entry.stationId
                        ReorderableItem(reorderableState, key = entry.uid) { isDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 0.9f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "dragScale"
                            )
                            Card(
                                pressFeedbackType = PressFeedbackType.Sink,
                                colors = CardColors(
                                    color = MiuixTheme.colorScheme.secondaryVariant,
                                    contentColor = MiuixTheme.colorScheme.onSecondaryVariant,
                                ),
                                modifier = Modifier
                                    .animateItem()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .longPressDraggableHandle(
                                        onDragStopped = { }
                                    ),
                                onClick = {
                                    // 记录 uid，交给统一的删除确认弹窗处理
                                    deleteTargetUid = entry.uid
                                    showDeleteDialog = true
                                    // 地图移到站点位置
                                    val station =
                                        stationDatabaseHelper.queryById(stationId).firstOrNull()
                                    if (station != null) {
                                        focusStationOnMap(stationId)
                                    }
                                }
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                ) {

                                    // 站点序号
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "${index + 1}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                    )

                                    // 站点名称
                                    val name = getStationDisplayName(stationId)
                                    val lineHeight = 16.sp
                                    val fixedHeight =
                                        with(LocalDensity.current) { (lineHeight * 4).toDp() }
                                    val slotHeight =
                                        with(LocalDensity.current) { lineHeight.toDp() }

                                    Box(
                                        modifier = Modifier
                                            .height(fixedHeight)
                                            .clipToBounds(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {

                                            // 超过 4 字：逐字竖排 + 匀速垂直滚动
                                            name.length > 4 -> {
                                                val scrollState = rememberScrollState()

                                                LaunchedEffect(name) {
                                                    // 等布局完成拿到 maxValue
                                                    snapshotFlow { scrollState.maxValue }.first { it > 0 }

                                                    val speedPxPerMs =
                                                        0.05f   // ← 恒定速度：每毫秒滚多少 px，调它

                                                    while (true) {
                                                        // 顶部停留
                                                        delay(1200.milliseconds)

                                                        // 匀速滚到底
                                                        val distDown =
                                                            scrollState.maxValue - scrollState.value
                                                        if (distDown > 0) {
                                                            val durDown =
                                                                (abs(distDown) / speedPxPerMs)
                                                                    .toInt().coerceAtLeast(100)
                                                            scrollState.animateScrollTo(
                                                                scrollState.maxValue,
                                                                animationSpec = tween(
                                                                    durDown,
                                                                    easing = LinearEasing
                                                                )
                                                            )
                                                        }

                                                        // 底部停留
                                                        delay(1200.milliseconds)

                                                        // 回顶
                                                        scrollState.scrollTo(0)
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .verticalScroll(scrollState),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    name.forEach { ch ->
                                                        Box(
                                                            modifier = Modifier.height(slotHeight),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = ch.toString(),
                                                                fontSize = 14.sp,
                                                                lineHeight = lineHeight,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 恰好 4 字：逐字换行，4 行填满
                                            name.length == 4 -> {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = name.toCharArray().joinToString("\n"),
                                                    fontSize = 14.sp,
                                                    lineHeight = lineHeight,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 4,
                                                    minLines = 4,
                                                    overflow = TextOverflow.Clip
                                                )
                                            }

                                            // 不足 4 字：4 槽位 SpaceBetween
                                            else -> {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillMaxHeight(),
                                                    verticalArrangement = Arrangement.SpaceBetween,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    name.forEach { ch ->
                                                        Box(
                                                            modifier = Modifier.height(slotHeight),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = ch.toString(),
                                                                fontSize = 14.sp,
                                                                lineHeight = lineHeight,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 包装
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp
        ) {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                text = "* 轻触删除 长按拖动调整顺序"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { showAddDialog = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        modifier = Modifier.size(18.dp),
                        contentDescription = "添加站点"
                    )
                    Text("添加站点")
                }
            }
            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.secondaryVariant
                ),
                onClick = {
                    sourceText = entries.joinToString(" ") { it.stationId.toString() }
                    showSourceDialog = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Edit,
                        modifier = Modifier.size(18.dp),
                        contentDescription = "编辑源文本"
                    )
                    Text("源文本")
                }
            }
        }

        // —— 编辑源文本对话框（单行） ——
        OverlayDialog(
            title = "编辑站点序列源文本",
            summary = "空格分隔的站点 id，例如 1 2 3 4（允许重复）",
            show = showSourceDialog,
            onDismissRequest = { showSourceDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "站点 id 序列",
                    singleLine = true
                )
                TextButton(
                    text = "保存",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        textColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val tokens = sourceText.trim().split(" ").filter { it.isNotBlank() }
                        val ids = mutableListOf<Int>()
                        var hasError = false
                        tokens.forEach { token ->
                            val id = token.toIntOrNull()
                            if (id == null) {
                                utils.showMsg("非法站点 id：$token")
                                hasError = true
                                return@forEach
                            }
                            val exists = stationDatabaseHelper.queryById(id).isNotEmpty()
                            if (!exists) {
                                utils.showMsg("站点 id 不存在：$id")
                                hasError = true
                                return@forEach
                            }
                            ids += id
                        }
                        if (hasError) return@TextButton
                        // 为每个 id 分配新的 uid（重复 id 各自独立）
                        setEntries(ids.map { StationEntry(nextUid(), it) })
                        showSourceDialog = false
                    }
                )
                TextButton(
                    text = "取消",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSourceDialog = false }
                )
            }
        }

        // —— 添加站点对话框 ——
        AddStationDialog(
            show = showAddDialog,
            onDismiss = { showAddDialog = false },
            onPick = { station ->
                val id = station.id ?: return@AddStationDialog
                // 允许添加重复站点（环线/折返），仅在重复时给出轻提示
//                if (entries.any { it.stationId == id }) {
//                    utils.showMsg("该站点已存在，仍已追加")
//                }
                utils.showMsg("已添加 ${station.cnName}", true)
                setEntries(entries + StationEntry(nextUid(), id))
            }
        )

    }


    // —— 添加站点弹窗：搜索 + 列表点选（保持打开） ——

    @Composable
    private fun AddStationDialog(
        show: Boolean,
        onDismiss: () -> Unit,
        onPick: (Station) -> Unit
    ) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Station>>(emptyList()) }

        // 打开时初始化；关键词变化时查询
        LaunchedEffect(show, query) {
            if (show) {
                results = if (query.isBlank()) {
                    stationDatabaseHelper.queryAll()
                } else {
                    stationDatabaseHelper.queryByKey(query)
                }
            }
        }

        OverlayDialog(
            title = "添加站点",
            show = show,
            onDismissRequest = onDismiss
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "搜索站点（id/中文/英文）",
                    singleLine = true
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (results.isEmpty()) {
                        Text(
                            text = "无匹配站点",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(
                                count = results.size,
                                key = { index -> results[index].id ?: index }
                            ) { index ->
                                val station = results[index]
                                BasicComponent(
                                    title = "${station.id}${station.type} ${station.cnName}",
                                    onClick = { onPick(station) }
                                )
                            }
                        }
                    }
                }

                TextButton(
                    text = "关闭",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }

    // ★ 新增：地图站点删除确认弹窗
    // 因为 initMap 是普通函数，没有 Compose 作用域，所以弹窗 state 提升到 Activity，
    // 弹窗 UI 在 MainContent 的 Compose 树里渲染。
    @Composable
    private fun StationDeleteDialog(
        show: Boolean,
        targetUid: Long?,
        onDismiss: () -> Unit
    ) {
        val currentEntries = if (isUpDirActive()) upEntries else downEntries
        val targetIndex = currentEntries.indexOfFirst { it.uid == targetUid }
        val targetEntry = currentEntries.getOrNull(targetIndex) ?: return
        val stationName = getStationDisplayName(targetEntry.stationId)


        OverlayDialog(
            title = stationName,
            summary = "第 ${targetIndex + 1} 站",
            show = show,
            onDismissRequest = onDismiss
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = "确认删除",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = Color.White
                    ),
                    onClick = {
                        if (targetUid != null) {
                            // 按 uid 精确删除（兼容重复 stationId）
                            if (isUpDirActive()) {
                                updateUpEntries { list -> list.filterNot { it.uid == targetUid } }
                            } else {
                                updateDownEntries { list -> list.filterNot { it.uid == targetUid } }
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun TotalPickDialog(
        show: Boolean,
        stationId: Int?,
        onDismiss: () -> Unit
    ) {
        if (!show || stationId == null) return

        val stationName = getStationDisplayName(stationId)

        OverlayDialog(
            title = stationName,
            summary = "id: $stationId",
            show = true,
            onDismissRequest = onDismiss
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = "添加到末尾",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        textColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // 追加到当前方向末尾（允许重复站点）
                        val newEntry = StationEntry(nextUid(), stationId)
                        if (isUpDirActive()) {
                            updateUpEntries { it + newEntry }
                        } else {
                            updateDownEntries { it + newEntry }
                        }
                        onDismiss()
                    }
                )
                TextButton(
                    text = "取消",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }

    // —— 地图（底层） ——

    private var mapView: MapView? = null
    private var aMap: AMap? = null

    @Composable
    private fun AMapCompose(modifier: Modifier = Modifier, currentDir: Int) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle

        val mapView = remember {
            MapView(context).apply {
                onCreate(Bundle())
            }
        }
        this.mapView = mapView

        DisposableEffect(lifecycle, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                mapView.onDestroy()
            }
        }

        AndroidView(
            factory = {
                mapView.also {
                    initMap(it.map)
                    addTotalStationPoint()
                }
            },
            modifier = modifier
        )

        // ★ 首次绘制 或 切换方向时才移动相机；增删/重排不移动
        var shouldMoveToLine by remember { mutableStateOf(true) }
        var lastDir by remember { mutableIntStateOf(currentDir) }
        LaunchedEffect(currentDir) {
            if (currentDir != lastDir) {
                lastDir = currentDir
                shouldMoveToLine = true   // 方向切换 → 重新定位到该方向的路线
            }
        }

        LaunchedEffect(upEntries, downEntries, currentDir) {
            val map = aMap ?: return@LaunchedEffect

            Log.d("AMapCompose", "currentDir: $currentDir, moveMapToLine: $shouldMoveToLine")

            val currentEntries = if (currentDir == 0) upEntries else downEntries
            val stationList = currentEntries.map {
                stationDatabaseHelper.queryById(it.stationId).firstOrNull()
                    ?: utils.getUnknownStation()
            }
            stationList.forEach { it.cnName = utils.getStationNameFromCn(it.cnName, "cn", it) }

            val moveNow = shouldMoveToLine
            linePathAndStationPointService.apply {
                draw(
                    map,
                    ArrayList(stationList),
                    null,
                    line.name,
                    "line",
                    moveMapToLine = moveNow,
                    showInUpperHalf = true
                )
                onStationIndexChange(0)
                onStationStateChange(StationStatus.ON_NEXT)
            }
            // 消费掉标志，后续增删/重排不再移动
            shouldMoveToLine = false
        }
    }

    private val aMapZoomPoint = 14F

    private fun initMap(aMap: AMap) {
        utils.setMapMode(aMap, utils.getMapType())
        aMap.isTrafficEnabled = utils.getIsMapTrafficEnabled()
        this.aMap = aMap

        var curZoomLevel = LinePathAndStationPointService.MapZoomLevel.LAGER

        aMap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(cameraPosition: CameraPosition?) {
                // 视角变化过程中回调，频率高，谨慎做耗时操作
            }

            override fun onCameraChangeFinish(cameraPosition: CameraPosition?) {
                val currentZoom = cameraPosition?.zoom ?: return
                curZoomLevel = utils.onAMapMarkerZoomChange(
                    listOf(linePathAndStationPointService, totalStationPointService),
                    currentZoom,
                    aMapZoomPoint,
                    curZoomLevel
                )
            }
        })

        aMap.setOnMarkerClickListener { marker ->
            val prefix =
                marker.title.split("-").firstOrNull() ?: return@setOnMarkerClickListener true
            val stationIndex =
                marker.title.split("-").lastOrNull() ?: return@setOnMarkerClickListener true
            Log.d("AMapCompose", "prefix: $prefix, stationIndex: $stationIndex")


            if (prefix == "line") {
                val index = stationIndex.toIntOrNull()
                if (index != null) {
                    // 按当前方向 + index 反查 uid（index 会随重排变化，uid 才稳定）
                    val currentEntries = if (isUpDirActive()) upEntries else downEntries
                    val entry = currentEntries.getOrNull(index)
                    if (entry == null) {
                        utils.showMsg("站点不存在或已被移除")
                    } else {
                        // 相机移到该站点
                        focusStationOnMap(entry.stationId)
                        linePathAndStationPointService.onStationIndexChange(index)
                        // 记录 uid 并弹出删除确认
                        deleteTargetUid = entry.uid
                        showDeleteDialog = true

                    }
                }
            } else if (prefix == "total") {
                // 从 marker.title 解析站点 id（格式 "total-<index>"，index 对应 queryAll 的下标）
                val index = stationIndex.toIntOrNull()
                if (index != null) {
                    val allStations = stationDatabaseHelper.queryAll()
                    val station = allStations.getOrNull(index)


                    if (station?.id == null) {
                        utils.showMsg("站点不存在或已被移除")
                        return@setOnMarkerClickListener true
                    }

                    // 相机移到该站点
                    focusStationOnMap(station.id!!)
                    // 记录站点 id 并弹出添加对话框
                    totalPickStationId = station.id
                    showTotalPickDialog = true
                }
            }


            true
        }
    }

    private fun focusStationOnMap(stationId: Int) {
        val station = stationDatabaseHelper.queryById(stationId).firstOrNull() ?: return
        val map = aMap ?: return
        val view = mapView ?: return
        val target = LatLng(station.latitude, station.longitude)

        val projection = map.projection
        // 目标点在"当前相机"下的屏幕位置
        val screenPoint = projection.toScreenLocation(target)
        val desiredX = view.width / 2f
        val desiredY = view.height / 3f
        val dx = screenPoint.x - desiredX
        val dy = screenPoint.y - desiredY

        val centerScreen = projection.toScreenLocation(map.cameraPosition.target)
        val newCenter = projection.fromScreenLocation(
            android.graphics.Point(
                (centerScreen.x + dx).toInt(),
                (centerScreen.y + dy).toInt()
            )
        )
        map.animateCamera(CameraUpdateFactory.newLatLng(newCenter))
    }

    /** 通知 LineFragment 数据已变更后关闭 */
    private fun finishWithResult() {
        setResult(RESULT_OK)
        finish()
    }

    private fun addTotalStationPoint() {
        val totalStationList = stationDatabaseHelper.queryAll()
        totalStationList.forEach {
            it.cnName = utils.getStationNameFromCn(it.cnName, "cn", it)
        }
        totalStationPointService.apply {
            draw(
                aMap!!,
                ArrayList(totalStationList),
                null,
                resources.getString(R.string.line_all),
                "total",
                LinePathAndStationPointService.MarkerNumberType.ID,
                moveMapToLine = false
            )
            onStationIndexChange(totalStationList.size - 1)
        }
    }
}