package com.microbus.announcer.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.microbus.announcer.R
import com.microbus.announcer.bean.Station
import com.microbus.announcer.model.StationStatus
import androidx.core.graphics.createBitmap
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLngBounds
import com.microbus.announcer.Utils

class LinePathAndStationPointService private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    private var stationList: ArrayList<Station> = ArrayList()

    private val linePathList = ArrayList<Polyline>()
    private val stationPointList = ArrayList<Marker>()
    private val stationMarkerFactory = StationMarkerFactory(appContext)

    // ====== debug 模式 ======
    /** 是否开启 debug 模式（默认取 BuildConfig.DEBUG） */
    private var debug: Boolean = false

    /** debug 下标标记 */
    private val stepIndexMarkerList = ArrayList<Marker>()


    /** debug Step 起点圆圈（半径 200m） */
    private val stepCircleList = ArrayList<Circle>()

    /** 半径（米） */
    private val STEP_CIRCLE_RADIUS_METERS = 100.0

    private val utils = Utils(appContext)

    enum class MapZoomLevel {
        SMALL,
        LAGER
    }

    private var mapZoomLevel = MapZoomLevel.LAGER

    companion object {
        @Volatile
        private var INSTANCE: LinePathAndStationPointService? = null

        fun getInstance(context: Context): LinePathAndStationPointService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LinePathAndStationPointService(context).also { INSTANCE = it }
            }
        }
    }

    /** 手动开启/关闭 debug 模式 */
    fun setDebug(enable: Boolean) {
        if (debug == enable) return
        debug = enable
        // 关闭时需要清除已绘制的下标标记
        if (!debug) {
            clearStepIndexMarkers()
        }
    }

    /**
     * 绘制线路和站点
     */
    fun draw(
        aMap: AMap,
        stationList: ArrayList<Station>,
        steps: List<DrivingPlanning.Step>?,
        lineName: String
    ) {

        oldStationIndex = null
        newStationIndex = null
        oldStationState = null
        newStationState = null

        this.stationList = stationList

        // 路线
        clearLinePath()
        if (lineName != appContext.resources.getString(R.string.line_all)) {
            val mSteps = getLinePath(stationList, steps)
            drawLinePath(aMap, mSteps)
            // todo 切换地图相机，完整显示路线
            fitLinePath(aMap)
        }

        // 站点标点
        clearStationPoint()
        drawStationPoint(aMap, stationList)

        Log.d("draw", "draw finish: ${linePathList.size}, ${stationPointList.size}")

    }


    /**
     * 获取路径
     */
    private fun getLinePath(
        stationList: ArrayList<Station>,
        steps: List<DrivingPlanning.Step>?
    ): List<DrivingPlanning.Step> {

        // 有在线路径，补点
        if (steps != null) {
            fillPathPoints(steps)
            return steps
        }

        // 没有在线路径，手动获取
        val mSteps = emptyList<DrivingPlanning.Step>().toMutableList()

        // 遍历除最后一个站点外的所有站点
        for (index in 0 until stationList.size - 1) {

            val curStation = stationList[index]
            val nextStation = stationList[index + 1]

            val polyline = ArrayList<LatLng>()
            polyline.add(LatLng(curStation.latitude, curStation.longitude))
            polyline.add(LatLng(nextStation.latitude, nextStation.longitude))


            val step = DrivingPlanning.Step(
                waypoint_index = index,
                instruction = "",
                orientation = "",
                road_name = "",
                step_distance = "",
                navi = emptyMap(),
                polyline = polyline
            )

            mSteps.add(step)
        }


        return mSteps

    }


    /**
     * 路径补点
     */
    private fun fillPathPoints(steps: List<DrivingPlanning.Step>) {
        // 遍历除最后一个路径外的所有路径
        for (index in 0 until steps.size - 1) {

            // 后续路径起点
            val nextStepStart = steps[index + 1].polyline.first()

            // 在本路径终点和后续路径起点之间补点
            val curStep = steps[index]
            curStep.polyline += nextStepStart

        }
    }

    /**
     * 绘制路径（全灰）
     */
    private fun drawLinePath(
        aMap: AMap,
        steps: List<DrivingPlanning.Step>
    ) {

        // steps按waypoint_index分类
        val stepListMap = steps.groupBy { it.waypoint_index }

        // 绘制路径
        stepListMap.forEach { (_, stepList) ->

            val latLngList = stepList.flatMap { it.polyline }
            val customTexture = (BitmapDescriptorFactory.fromResource(R.mipmap.line_gray))

            val polyline = aMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngList)
                    .setCustomTexture(customTexture)
                    .width(16f)
                    .zIndex(0f)
            )

            linePathList.add(polyline)
        }

        Log.d("drawLinePath", "drawLinePath: ${linePathList.size}")

        // debug 模式：绘制每个 Step 的下标
        if (debug) {
            drawStepIndexMarkers(aMap, steps)
        }
    }

    /**
     * 移动相机，使已绘制的整条路线完整显示在屏幕内
     */
    private fun fitLinePath(aMap: AMap) {
        val builder = LatLngBounds.Builder()
        var hasPoint = false
        linePathList.forEach { polyline ->
            polyline.points.forEach { builder.include(it); hasPoint = true }
        }
        if (!hasPoint) return

        val bounds = builder.build()
        val dm = appContext.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        // 左右各不留，上下各留 120px
        val paddingLeftRight = 0
        val paddingTopBottom = utils.dp2px(300f)

        // newLatLngBounds(bounds, width, height, padding) 的 width/height
        // 是用于计算缩放的"有效视口"，padding 是四边统一留白。
        // 这里用"缩小有效视口"来间接实现"上下多留"：
        val effectiveW = screenW - 0
        val effectiveH = screenH - paddingTopBottom * 2

        try {
            aMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, effectiveW, effectiveH, 0)
            )
        } catch (e: Exception) {
            Log.w("fitLinePath", "fit bounds failed: ${e.message}")
            linePathList.firstOrNull()?.points?.firstOrNull()?.let {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
            }
        }
    }

    // ====== debug 相关 ======

    private fun drawStepIndexMarkers(aMap: AMap, steps: List<DrivingPlanning.Step>) {
        clearStepIndexMarkers()

        steps.forEachIndexed { index, step ->
            val polyline = step.polyline
            if (polyline.isEmpty()) return@forEachIndexed

            val startPoint = polyline.first()

            // ===== 新增：绘制半径 200m 的半透明圆 =====
            val circle = aMap.addCircle(
                CircleOptions()
                    .center(startPoint)
                    .radius(STEP_CIRCLE_RADIUS_METERS)
                    .fillColor(Color.argb(60, 0, 122, 255))   // 半透明蓝色填充
                    .strokeColor(Color.argb(140, 0, 122, 255)) // 边框稍深
                    .strokeWidth(2f)
                    .zIndex(9f)                                // 在路径之上、标记之下
            )
            stepCircleList.add(circle)

            // ===== 原有的下标标记 =====
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(startPoint)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .anchor(0.5f, 0.5f)
                    .title("Step ${index + 1}")
                    .snippet("waypoint_index=${step.waypoint_index}, size=${polyline.size}")
                    .zIndex(10f)
            )
            marker.setIcon(
                BitmapDescriptorFactory.fromBitmap(createStepIndexBitmap("${index + 1}"))
            )

            stepIndexMarkerList.add(marker)
        }

        Log.d(
            "drawStepIndexMarkers",
            "markers=${stepIndexMarkerList.size}, circles=${stepCircleList.size}"
        )
    }

    /**
     * 创建下标小圆点 Bitmap
     */
    @SuppressLint("UseKtx")
    private fun createStepIndexBitmap(text: String): Bitmap {
        val size = 64
        val bitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // 红色圆底
        paint.color = Color.RED
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

        // 白色文字
        paint.color = Color.WHITE
        paint.textSize = if (text.length > 2) size * 0.28f else size * 0.4f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, size / 2f, y, paint)

        return bitmap
    }

    private fun clearStepIndexMarkers() {
        stepIndexMarkerList.forEach { it.remove() }
        stepIndexMarkerList.clear()

        // 同步清理圆
        stepCircleList.forEach { it.remove() }
        stepCircleList.clear()
    }
    // ====== end debug ======

    private fun clearLinePath() {
        linePathList.forEach { it.remove() }
        linePathList.clear()

        // 同步清除 debug 下标标记
        clearStepIndexMarkers()
    }

    /**
     * 绘制站点标点
     */
    private fun drawStationPoint(aMap: AMap, stationList: ArrayList<Station>) {

        stationList.forEachIndexed { index, station ->

            val icon = BitmapDescriptorFactory.fromBitmap(
                createStationMarkerBitmap(
                    index,
                    station,
                    mapZoomLevel
                )
            )

            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
            )
            marker.`object` = station
            stationPointList.add(marker)
        }

        Log.d("drawStationPoint", "drawStationPoint: ${stationPointList.size}")
    }

    private fun clearStationPoint() {
        stationPointList.forEach { it.remove() }
        stationPointList.clear()
    }

    private fun createStationMarkerBitmap(
        stationIndex: Int,
        station: Station,
        zoomLevel: MapZoomLevel
    ): Bitmap {
        val bitmap = stationMarkerFactory.createStationMarkerBitmap(
            stationIndex = stationIndex,
            station = station,
            newStationIndex = newStationIndex,
            zoomLevel = zoomLevel
        )
        return bitmap
    }

    private var oldStationIndex: Int? = null
    private var newStationIndex: Int? = null

    private var oldStationState: Int? = null
    private var newStationState: Int? = null

    fun onStationIndexChange(index: Int) {

        if (index == newStationIndex) {
            return
        }

        Log.d("onStationIndexChange", "onStationIndexChange: $index")
        oldStationIndex = newStationIndex
        newStationIndex = index
        updView()
    }

    fun onStationStateChange(state: Int) {

        if (state == newStationState) {
            return
        }

        Log.d("onStationStateChange", "onStationStateChange: $state")
        oldStationState = newStationState
        newStationState = state
        updView()
    }

    private fun updView() {

        if (newStationState == null || newStationIndex == null) {
            return
        }

        Log.d("updView", "StationIndex: $oldStationIndex -> $newStationIndex")


        var startIndex = oldStationIndex
        var endIndex = newStationIndex

        if (startIndex == null || endIndex == null) {
            startIndex = 0
            endIndex = stationList.size - 1
        }

        if (startIndex > endIndex) {
            val temp = startIndex
            startIndex = endIndex
            endIndex = temp
        }

        // 更新范围
        val updRange = startIndex..endIndex
        Log.d("updView", "updRange: $updRange")
        for (i in updRange) {
            updLinePath(i)
            updStationPoint(i, mapZoomLevel)
        }

    }


    private fun updLinePath(stationIndex: Int) {
        val polyline = linePathList.getOrNull(stationIndex) ?: return

        var lineBitmap: Int

        if (newStationIndex == null) {
            lineBitmap = R.mipmap.line_gray
        } else {
            // 前方路径
            lineBitmap = if (stationIndex >= newStationIndex!!) {
                R.mipmap.line_green
            }
            // 当前所处路径
            else if (stationIndex == newStationIndex!! - 1 &&
                (newStationState == StationStatus.ON_WILL_ARRIVE || newStationState == StationStatus.ON_NEXT)
            ) {
                R.mipmap.line_blue
            } else {
                R.mipmap.line_gray
            }
        }

        val customTexture = (BitmapDescriptorFactory.fromResource(lineBitmap))
        polyline.setCustomTexture(customTexture)
    }

    private fun updStationPoint(stationIndex: Int, zoomLevel: MapZoomLevel) {
        val marker = stationPointList.getOrNull(stationIndex) ?: return
        val station = stationList.getOrNull(stationIndex) ?: return

        val icon =
            BitmapDescriptorFactory.fromBitmap(
                createStationMarkerBitmap(
                    stationIndex,
                    station,
                    zoomLevel
                )
            )

        marker.setIcon(icon)

    }

    fun updAllStationPoint(zoomLevel: MapZoomLevel) {
        this.mapZoomLevel = zoomLevel
        stationPointList.forEachIndexed { index, marker ->
            updStationPoint(index, zoomLevel)
        }
    }
}