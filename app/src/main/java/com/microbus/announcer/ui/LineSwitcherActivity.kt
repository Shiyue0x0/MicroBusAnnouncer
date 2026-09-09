package com.microbus.announcer.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.amap.api.services.busline.BusLineQuery
import com.amap.api.services.busline.BusLineResult
import com.amap.api.services.busline.BusLineSearch
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.activity.FragmentContainerActivity
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.ui.compose.AnSmallTopAppBar
import com.microbus.announcer.ui.settings.SystemSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import kotlin.time.Duration.Companion.milliseconds

class LineSwitcherActivity : ComponentActivity() {

    private var tag = javaClass.simpleName
    private lateinit var utils: Utils
    private lateinit var lineDatabaseHelper: LineDatabaseHelper
    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var prefs: SharedPreferences

    private var totalCloudStationList = arrayListOf<Station>()

    private lateinit var localLineTypeList: List<LocalLineTypeItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        utils = Utils(this)
        lineDatabaseHelper = LineDatabaseHelper.getInstance(this)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        localLineTypeList = listOf(
            LocalLineTypeItem(0, getString(R.string.total_line), ""),
            LocalLineTypeItem(1, getString(R.string.line_comm_bus), "C"),
            LocalLineTypeItem(2, getString(R.string.line_normal_bus), "B"),
            LocalLineTypeItem(3, getString(R.string.line_metro), "U"),
            LocalLineTypeItem(4, getString(R.string.line_train), "T")
        )

        enableEdgeToEdge()
        setContent {
            MainView()
        }


    }


    @Composable
    @Preview
    fun MainView() {
        val controller = remember { ThemeController(ColorSchemeMode.System) }
        MiuixTheme(
            controller = controller,
        ) {

            val (searchText, setSearchText) = remember { mutableStateOf("") }

            Scaffold(

                topBar = {
                    AnSmallTopAppBar(this, getString(R.string.switch_line))
                },
                content = { innerPadding ->
                    MainContent(searchText, setSearchText, innerPadding)
                },
                floatingActionButton = {
                    MyFloatingToolbar(searchText)
                },
                floatingActionButtonPosition = FabPosition.End
            )
        }
    }

    @Composable
    fun MainContent(
        searchText: String,
        setSearchText: (String) -> Unit,
        innerPadding: PaddingValues
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
//                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(bottom = 16.dp)
            ) {

                val coroutineScope = rememberCoroutineScope()

                var localLineResults by remember { mutableStateOf<List<Line>>(emptyList()) }
                var cloudLineResults by remember { mutableStateOf<List<Line>>(emptyList()) }

                var searchLineType by remember { mutableIntStateOf(SearchLineType.LOCAL) }

                val pagerState = rememberPagerState(pageCount = { 2 })

                var isSearching by remember { mutableStateOf(false) }

                // 节流用的 Job
                var searchJob by remember { mutableStateOf<Job?>(null) }
                // 记录上次执行时间
                var lastSearchTime by remember { mutableLongStateOf(0L) }

                val (localLineTypeSelectedIndex, setLocalLineTypeSelectedIndex) = remember {
                    mutableIntStateOf(
                        0
                    )
                }

                val (city, setCity) = remember {
                    mutableStateOf(utils.getCity())
                }

                fun performSearch(key: String, localLineTypeSelectedIndex: Int) {
                    isSearching = true
                    when (searchLineType) {
                        SearchLineType.LOCAL -> {
                            localLineResults = emptyList()
                            localLineResults = searchLocalLine(key, localLineTypeSelectedIndex)
                            isSearching = false
                        }

                        SearchLineType.CLOUD -> {
                            cloudLineResults = emptyList()
                            // 取消之前的搜索任务（如果有）
                            searchJob?.cancel()

                            val currentTime = System.currentTimeMillis()
                            // 检查距离上次执行是否超过节流间隔（1000ms）
                            if (currentTime - lastSearchTime >= 1000) {
                                // 立即执行搜索
                                searchJob = coroutineScope.launch {
                                    lastSearchTime = currentTime
                                    cloudLineResults = searchCloudLine(city, key)
                                    isSearching = false
                                }
                            } else {
                                // 还没到执行时间，但为了确保最终会执行，可以延迟到剩余时间后执行
                                searchJob = coroutineScope.launch {
                                    val remainingTime = 1000 - (currentTime - lastSearchTime)
                                    delay(remainingTime.milliseconds)
                                    lastSearchTime = System.currentTimeMillis()
                                    cloudLineResults = searchCloudLine(city, key)
                                    isSearching = false
                                }
                            }
                        }
                    }
                }

                DisposableEffect(prefs) {
                    val listener =
                        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                            when (key) {
                                "city" -> {
                                    setCity(prefs.getString(key, "") ?: "")
                                    if (pagerState.currentPage == SearchLineType.CLOUD) {
                                        performSearch(searchText, localLineTypeSelectedIndex)
                                    }
                                }
                            }
                        }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
                }

                LaunchedEffect(pagerState.currentPage) {
                    searchLineType = pagerState.currentPage
                }

                // 初始加载、关键字变化、本地站点类型、本地/在线路线页面、变化时搜索
                LaunchedEffect(searchText, localLineTypeSelectedIndex, pagerState.currentPage) {
//                    Log.d(tag, "L258 $searchText")
                    performSearch(searchText, localLineTypeSelectedIndex)
                }

                MySearchBar(
                    searchText = searchText,
                    onSearchTextChange = { newText ->
                        setSearchText(newText)
                    }
                )

                Column(Modifier.padding(horizontal = 16.dp)) {

                    SearchLineTypeTab(
                        searchLineType = searchLineType,
                        onTabSelected = { newSearchLineType ->
                            searchLineType = newSearchLineType
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(newSearchLineType)
                            }
                        }
                    )

                    if (pagerState.currentPage == SearchLineType.LOCAL) {
                        LocalLineTypeListPopup(
                            localLineTypeSelectedIndex,
                            setLocalLineTypeSelectedIndex
                        )
                    }

                    if (pagerState.currentPage == SearchLineType.CLOUD) {
                        CloudLineCityEditor(city)
                    }


                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    LineList(
                        searchResults = if (page == SearchLineType.LOCAL) localLineResults else cloudLineResults,
                        lineType = page,
                        searchText = searchText,
                        isSearching = isSearching
                    )

                }
            }
        }

    }

    @Composable
    fun MySearchBar(
        searchText: String,
        onSearchTextChange: (String) -> Unit
    ) {

        val focusRequester = remember { FocusRequester() }

        // 在组件首次组合时请求焦点
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SearchBar(
            inputField = {
                InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = { onSearchTextChange(searchText) },
                    expanded = false,
                    onExpandedChange = {},
                    label = "通过路线名称搜索",
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            expanded = false,
            onExpandedChange = {}
        ) {
        }

    }

    object SearchLineType {
        /**本地路线*/
        const val LOCAL = 0

        /**在线路线*/
        const val CLOUD = 1
    }

    data class LocalLineTypeItem(
        val index: Int,
        val name: String,
        val value: String
    )


    @Composable
    fun LocalLineTypeListPopup(selectedIndex: Int, setLocalLineTypeSelectedIndex: (Int) -> Unit) {

        var showPopup by remember { mutableStateOf(false) }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {
            TextButton(
                text = localLineTypeList[selectedIndex].name,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showPopup = true }
            )
            OverlayListPopup(
                show = showPopup,
                alignment = PopupPositionProvider.Align.Start,
                onDismissRequest = { showPopup = false }) {
                ListPopupColumn {
                    localLineTypeList.forEachIndexed { index, localLineType ->
                        DropdownImpl(
                            text = localLineType.name,
                            optionSize = localLineTypeList.size,
                            isSelected = selectedIndex == index,
                            index = index,
                            onSelectedIndexChange = {
                                setLocalLineTypeSelectedIndex(index)
                                showPopup = false
                            }
                        )
                    }
                }
            }
        }

    }


    @Composable
    fun CloudLineCityEditor(city: String) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 8.dp)
                .fillMaxWidth()

        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    FragmentContainerActivity.start(
                        this@LineSwitcherActivity,
                        SystemSettings::class.java
                    )
                }
            ) {
                Text(
                    text = "搜索城市：${city}",
                )
                Icon(
                    imageVector = MiuixIcons.Edit,
                    contentDescription = "修改城市",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }

    @Composable
    fun SearchLineTypeTab(searchLineType: Int, onTabSelected: (Int) -> Unit) {

        val tabs = listOf("本地路线", "在线路线")

        TabRow(
            tabs = tabs,
            selectedTabIndex = searchLineType,
            onTabSelected = { onTabSelected(it) },
            modifier = Modifier.padding(
//                horizontal = 16.dp,
                vertical = 8.dp
            )
        )
    }

    @Composable
    fun LineList(
        searchResults: List<Line>,
        searchText: String,
        lineType: Int,
        isSearching: Boolean
    ) {

        @Composable
        fun SearchIcon() {
            Icon(
                imageVector = MiuixIcons.Search,
                contentDescription = getString(R.string.search),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(32.dp)
            )
        }

        Card(
            cornerRadius = 24.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = rememberLazyListState()
            ) {
                items(
                    items = searchResults,
                    key = { it.id ?: Int.MIN_VALUE }
                ) { line ->
                    LineItem(
                        lineType = lineType,
                        line = line,
                        onClick = {
                            if (lineType == SearchLineType.LOCAL) {
                                finishAndLoadLocalLine(line.id ?: Int.MIN_VALUE)
                            }
                            if (lineType == SearchLineType.CLOUD) {
                                finishAndLoadCloudLine(line)
                            }
                        }
                    )
                }
            }


            if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (searchText == "") {
                        SearchIcon()
                        Text("请输入要搜索的路线名称")
                    } else if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                        Text("正在搜索路线：$searchText")
                    } else {
                        SearchIcon()
                        Text("暂时搜索不到路线 $searchText")
                    }
                }
            }
        }


    }

    @Composable
    fun LineItem(line: Line, onClick: () -> Unit, lineType: Int) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = onClick,
            pressFeedbackType = PressFeedbackType.Sink
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                var lineName = ""
                var firstStationName = ""
                var endStationName = ""

                if (lineType == SearchLineType.LOCAL) {
                    val firstStation =
                        stationDatabaseHelper.queryById(
                            line.upLineStation.split(" ").first().toInt()
                        )
                            .first()
                    val endStation =
                        stationDatabaseHelper.queryById(
                            line.upLineStation.split(" ").last().toInt()
                        )
                            .first()
                    lineName = line.name
                    firstStationName = firstStation.cnName
                    endStationName = endStation.cnName

                }

//                Log.d(tag, "L438 ${line.name}")

                if (lineType == SearchLineType.CLOUD) {
                    val pattern = Regex("^(.+?)\\((.+?)--(.+?)\\)$")
                    val matchResult = pattern.matchEntire(line.name)
                    if (matchResult != null) {
                        lineName = matchResult.groupValues[1]
                        firstStationName = matchResult.groupValues[2]
                        endStationName = matchResult.groupValues[3]
                    }
                }

                ArrowPreference(
                    title = "$firstStationName - $endStationName",
                    startAction = {
                        Card(
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = lineName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun MyFloatingToolbar(searchText: String) {
        FloatingToolbar(
            modifier = Modifier
                .imePadding(),
            color = MiuixTheme.colorScheme.secondary
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingToolBtnItem(
                    text = getString(R.string.set_temporary_line_name),
                    icon = MiuixIcons.Edit,
                    onClick = {
                        finishAndSetTemporaryLineName(searchText)
                    })
                FloatingToolBtnItem(
                    text = getString(R.string.line_all),
                    icon = MiuixIcons.All,
                    onClick = {
                        finishAndLoadLineAll()
                    })
            }
        }
    }

    fun searchLocalLine(key: String = "", localLineTypeSelectedIndex: Int): ArrayList<Line> {
        val comparator = utils.getDefaultLineComparator()
        val typeValue = localLineTypeList[localLineTypeSelectedIndex].value

        val lineList =
            // 全部路线
            if (typeValue == "")
                lineDatabaseHelper.queryByKey(key)
            // 按类型查找
            else
                lineDatabaseHelper.queryByKeyAndType(key, typeValue)

        return ArrayList(lineList.sortedWith(comparator))
    }

    suspend fun searchCloudLine(city: String = "", key: String = ""): List<Line> {


        if (key == "") {
            return emptyList()
        }

        val busLineQuery = BusLineQuery(
            key,
            BusLineQuery.SearchType.BY_LINE_NAME,
            city
        )
        busLineQuery.pageNumber = 0
        busLineQuery.extensions = "all"
        busLineQuery.pageSize = 999999

        return withContext(Dispatchers.IO) {
            try {
                val busLineSearch = BusLineSearch(this@LineSwitcherActivity, busLineQuery)
                val res = busLineSearch.searchBusLine()
                getCloudLineFromRes(res)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    utils.showMsg(e.message ?: "路线搜索失败")
                }
                return@withContext emptyList()
            }
        }

    }

    fun getCloudLineFromRes(res: BusLineResult): List<Line> {

        if (res.busLines.isEmpty()) {
            return emptyList()
        }

        // 编号相同的路线合并
        val lineNameList = ArrayList<String>()
        for (busLine in res.busLines) {
            val lineNumberName = busLine.busLineName.substringBefore("(")
            val sameLineName =
                lineNameList.find { it.substringBefore("(") == lineNumberName }
            if (sameLineName == null) {
                lineNameList.add(busLine.busLineName)
            }
        }

        // 获取云端站点
        val lineList = lineNameList.mapIndexed { index, name ->
            val busLines =
                res.busLines.filter { it.busLineName.substringBefore("(") == name.substringBefore("(") }
            val beginIndex = res.busLines.indexOf(busLines.first())
            val endIndex = res.busLines.indexOf(busLines.last())
            getOnlineLine(res, beginIndex, endIndex, index * -1)
        }

        return lineList

    }

    fun getOnlineLine(
        res: BusLineResult, beginIndex: Int, endIndex: Int, lineId: Int
    ): Line {
        var upLineStationStr = ""
        var downLineStationStr = ""

        for (x in beginIndex..endIndex) {
            Log.d(
                tag,
                res.busLines[x].toString()
            )
            for (i in res.busLines[x].busStations.indices) {
                Log.d(
                    tag, "${res.busLines[x].busStations[i].busStationName}" +
                            "\t\t${res.busLines[x].busStations[i].latLonPoint.longitude}" +
                            "\t${res.busLines[x].busStations[i].latLonPoint.latitude}"
                )
                val busStation = res.busLines[x].busStations[i]
                val id = -totalCloudStationList.size - 1
                totalCloudStationList.add(
                    Station(
                        id,
                        busStation.busStationName,
                        "",
                        busStation.latLonPoint.longitude,
                        busStation.latLonPoint.latitude,
                        "B"
                    )
                )
                when (x) {
                    beginIndex ->
                        upLineStationStr += "$id "

                    endIndex ->
                        downLineStationStr += "$id "
                }
            }
        }
//        utils.showMsg(upLineStationStr)

        val line = Line(
            lineId,
            res.busLines[beginIndex].busLineName,
            upLineStationStr,
            downLineStationStr,
            false
        )

        // 单向路线
        if (beginIndex == endIndex) {
            line.downLineStation = line.upLineStation
        }

        return line
    }

    fun finishAndLoadLocalLine(lineId: Int) {
        val resultIntent = Intent().apply {
            putExtra("action", utils.LOAD_LOCAL_LINE)
            putExtra("lineId", lineId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    fun finishAndLoadLineAll() {
        val resultIntent = Intent().apply {
            putExtra("action", utils.LOAD_LINE_ALL)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    fun finishAndLoadCloudLine(line: Line) {

        line.name = line.name
            .substringBefore("(")
            .split("路", "号", "线")
            .first()

        // 获取云端站点
        val lineStationIdList =
            (line.upLineStation.trim() + " " + line.downLineStation.trim())
                .trim()
                .split(" ")
                .map { it.toIntOrNull() }

//        println("L520 $lineStationIdList")

        val cloudStationList = ArrayList<Station>()

        lineStationIdList.forEach { stationId ->

            val station = totalCloudStationList.find { station -> station.id == stationId }
            if (station == null) {
                utils.showMsg("在线路线加载异常")
                return
            }

            station.enName = station.cnName
            val localStation = stationDatabaseHelper.queryByCnName(station.enName)
            if (localStation.isNotEmpty()) {
                station.enName = localStation.first().enName
            }

            cloudStationList.add(station)

        }

        val resultIntent = Intent().apply {
            putExtra("action", utils.LOAD_CLOUD_LINE)
            putExtra("line", line)
            putExtra("cloudStationList", cloudStationList)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    fun finishAndSetTemporaryLineName(name: String) {
        val resultIntent = Intent().apply {
            putExtra("action", utils.SET_TEMPORARY_LINE_NAME)
            putExtra("lineName", name)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    @Composable
    fun FloatingToolBtnItem(text: String, icon: ImageVector, onClick: () -> Unit) {
        TooltipBox(text = text) {
            IconButton(onClick = onClick) {
                Icon(
                    icon,
                    contentDescription = text
                )
            }
        }
    }

}
