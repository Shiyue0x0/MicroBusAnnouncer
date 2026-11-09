package com.microbus.announcer.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
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
import top.yukonga.miuix.kmp.basic.IconButton
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
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowListPopup

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

    /** 上行站点 id 序列 */
    private var upList by mutableStateOf<List<Int>>(emptyList())

    /** 下行站点 id 序列 */
    private var downList by mutableStateOf<List<Int>>(emptyList())

    /** 新建模式下是否有未保存的变更 */
    private var isDirty = false

    /** 站点 id -> Station 的缓存（用于展示站名） */
    private val stationCache = mutableMapOf<Int, Station>()

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

        // 加载并解析站点序列
        upList = parseStationIds(line.upLineStation)
        downList = parseStationIds(line.downLineStation)

        // 预热站点缓存
        preloadStationCache()
    }

    /**
     * 把空格分隔的站点 id 字符串解析为 List<Int>。
     * 非法 token（非数字）会被排除并提示。
     */
    private fun parseStationIds(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        val result = mutableListOf<Int>()
        val invalid = mutableListOf<String>()
        raw.split(" ").forEach { token ->
            if (token.isBlank()) return@forEach
            val id = token.toIntOrNull()
            if (id == null) invalid += token else result += id
        }
        if (invalid.isNotEmpty()) {
            utils.showMsg("存在非法站点 id：${invalid.joinToString(", ")}，已忽略")
        }
        return result
    }

    private fun preloadStationCache() {
        stationCache.clear()
        (upList + downList).distinct().forEach { id ->
            val st = stationDatabaseHelper.queryById(id).firstOrNull()
            if (st != null) stationCache[id] = st
        }
    }

    private fun getStationDisplayName(id: Int): String {
        stationCache[id]?.let { return it.cnName }
        val st = stationDatabaseHelper.queryById(id).firstOrNull()
        return if (st != null) {
            stationCache[id] = st
            st.cnName
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

    private fun updateUpList(transform: (List<Int>) -> List<Int>) {
        upList = transform(upList)
        updateLine { it.copy(upLineStation = upList.joinToString(" ")) }
    }

    private fun updateDownList(transform: (List<Int>) -> List<Int>) {
        downList = transform(downList)
        updateLine { it.copy(downLineStation = downList.joinToString(" ")) }
    }

    /** 新建模式下保存并退出 */
    private fun commitAndFinish() {
        if (line.name.isBlank()) {
            utils.showMsg("请输入路线名称")
            return
        }
        val result = lineDatabaseHelper.insert(line)
        if (result > 0) {
            utils.showMsg("已保存")
            finishWithResult()
        } else {
            utils.showMsg("保存失败")
        }
    }

    // —— UI ——

    @Composable
    @Preview
    private fun MainView() {
        val controller = remember { ThemeController(ColorSchemeMode.System) }
        MiuixTheme(controller = controller) {
            Scaffold(
                topBar = {
                    AnSmallTopAppBar(
                        activity = this,
                        title = "编辑路线",
                        leftActions = {
                            IconButton(onClick = {
                                if (isEditing) {
                                    finishWithResult()
                                } else {
                                    commitAndFinish()
                                }
                            }) {
                                Icon(MiuixIcons.Ok, contentDescription = "完成")
                            }
                        })
                },
                content = { innerPadding ->
                    MainContent(innerPadding)
                }
            )
        }
    }

    @Composable
    private fun MainContent(innerPadding: PaddingValues) {

        val (currentDir, setCurrentDir) = remember { mutableIntStateOf(0) }


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
                StationSequenceSection(scrollState, currentDir, setCurrentDir)
            }
        }
    }

    // —— 基本信息：按钮 + 弹窗 ——
    @Composable
    private fun BasicInfoButton() {
        var showDialog by remember { mutableStateOf(false) }

        // 展示当前信息的摘要，方便用户不打开弹窗也能看到
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
            title = "编辑基本信息",
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
    private fun StationSequenceSection(
        scrollState: ScrollState,
        currentDir: Int,
        setCurrentDir: (Int) -> Unit
    ) {
        // 当前方向：0=上行，1=下行
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
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    text = "环线（仅上行）"
                )
            }

            val currentList = if (currentDir == 0) upList else downList
            val setCurrentList: (List<Int>) -> Unit = { newList ->
                if (currentDir == 0) updateUpList { newList } else updateDownList { newList }
            }

            StationSequenceCard(
                list = currentList,
                setList = setCurrentList,
                scrollState = scrollState
            )
        }
    }

    @Composable
    private fun StationSequenceCard(
        list: List<Int>,
        setList: (List<Int>) -> Unit,
        scrollState: androidx.compose.foundation.ScrollState
    ) {
        // 编辑源文本对话框
        var showSourceDialog by remember { mutableStateOf(false) }
        var sourceText by remember { mutableStateOf("") }

        // 添加站点对话框
        var showAddDialog by remember { mutableStateOf(false) }

        // 删除确认
        var deleteTargetIndex by remember { mutableIntStateOf(-1) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .border(
                    width = 1.5.dp,
                    color = MiuixTheme.colorScheme.primary,
                    shape = RoundedCornerShape(CardDefaults.CornerRadius)
                )
                .height(120.dp)
        ) {
            if (list.isEmpty()) {
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
                    setList(list.toMutableList().apply { add(to.index, removeAt(from.index)) })
                }
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = list.size,
                        key = { index -> index }
                    ) { index ->
                        val stationId = list[index]
                        ReorderableItem(reorderableState, key = index) { isDragging ->
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
                                    deleteTargetIndex = index
                                    showDeleteDialog = true
                                    // 地图移到站点位置
                                    val station = stationDatabaseHelper.queryById(stationId).firstOrNull()
                                    if (station != null) {
                                        val latLng = LatLng(station.latitude, station.longitude)
                                        aMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                                    }

                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = getStationDisplayName(stationId).toCharArray()
                                            .joinToString("\n"),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
            text = "* 轻触删除 长按拖动调整顺序"
        )

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
                    color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.3f)
                ),
                onClick = {
                    sourceText = list.joinToString(" ")
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
            summary = "空格分隔的站点 id，例如 1 2 3 4",
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
                        setList(ids)
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
                setList(list + station.id!!)
            }
        )

        // —— 删除确认对话框 ——
        OverlayDialog(
            title = "删除站点",
            summary = "确认删除第 ${deleteTargetIndex + 1} 个站点吗？",
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = "确认删除",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = Color.White
                    ),
                    onClick = {
                        if (deleteTargetIndex in list.indices) {
                            setList(list.filterIndexed { i, _ -> i != deleteTargetIndex })
                        }
                        showDeleteDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "取消",
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
                                    title = station.cnName,
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

    // —— 地图（底层） ——

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
// upList


        // 关键：监听上下行序列，变化时刷新地图
        LaunchedEffect(upList, downList, currentDir) {

            if (aMap == null) {
                return@LaunchedEffect
            }

            // 获取line序列
            Log.d("AMapCompose", "currentDir: $currentDir")
            val lineStationStr = if (currentDir == 0) upList else downList
            // 获取站点列表
            val stationList = lineStationStr.map {
                stationDatabaseHelper.queryById(it).firstOrNull() ?: utils.getUnknownStation()
            }
            linePathAndStationPointService.apply {
                draw(aMap!!, ArrayList(stationList), null, line.name)
                onStationIndexChange(0)
                onStationStateChange(StationStatus.ON_NEXT)
//                updAllStationPoint(
//                    LinePathAndStationPointService.MapZoomLevel.SMALL
//                )
            }

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
    }

    /** 通知 LineFragment 数据已变更后关闭 */
    private fun finishWithResult() {
        setResult(RESULT_OK)
        finish()
    }

    private fun addTotalStationPoint() {
        val totalStationList = stationDatabaseHelper.queryAll()
        totalStationPointService.apply {
            draw(
                aMap!!,
                ArrayList(totalStationList),
                null,
                resources.getString(R.string.line_all)
            )
            onStationIndexChange(totalStationList.size - 1)
        }

    }
}