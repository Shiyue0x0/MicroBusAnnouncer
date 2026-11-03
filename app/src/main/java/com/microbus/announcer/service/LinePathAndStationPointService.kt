package com.microbus.announcer.service

import android.content.Context
import android.graphics.Bitmap
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

class LinePathAndStationPointService private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    private var stationList: ArrayList<Station> = ArrayList()

    private val linePathList = ArrayList<Polyline>()
    private val stationPointList = ArrayList<Marker>()
    private val stationMarkerFactory = StationMarkerFactory(appContext)

    companion object {
        @Volatile
        private var INSTANCE: LinePathAndStationPointService? = null

        fun getInstance(context: Context): LinePathAndStationPointService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LinePathAndStationPointService(context).also { INSTANCE = it }
            }
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

    }


    private fun clearLinePath() {
        linePathList.forEach { it.remove() }
        linePathList.clear()
    }

    /**
     * 绘制站点标点
     */
    /**
     * 绘制站点标点（普通 Marker 方案）
     */
    private fun drawStationPoint(aMap: AMap, stationList: ArrayList<Station>) {

        stationList.forEachIndexed { index, station ->

            val icon = BitmapDescriptorFactory.fromBitmap(createStationMarkerBitmap(index))

            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
            )
            stationPointList.add(marker)
        }

        Log.d("drawStationPoint", "drawStationPoint: ${stationPointList.size}")
    }

    private fun clearStationPoint() {
        stationPointList.forEach { it.remove() }
        stationPointList.clear()
    }

    private fun createStationMarkerBitmap(stationIndex: Int): Bitmap {
        val bitmap = stationMarkerFactory.createStationMarkerBitmap(
            stationIndex = stationIndex,
            newStationIndex = newStationIndex
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

//        Log.d("updView", "StationState: $oldStationState -> $newStationState")
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
            updStationPoint(i)
            updLinePath(i)
        }

    }

    private fun updStationPoint(stationIndex: Int) {
        val marker = stationPointList.getOrNull(stationIndex) ?: return

        val icon = BitmapDescriptorFactory.fromBitmap(createStationMarkerBitmap(stationIndex))

        marker.setIcon(icon)

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

}

