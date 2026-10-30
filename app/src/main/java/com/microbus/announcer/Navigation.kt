package com.microbus.announcer

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocation
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.utils.SpatialRelationUtil
import com.microbus.announcer.announce.AnnouncementProvider
import kotlin.properties.Delegates

class Navigation private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Navigation? = null

        fun getInstance(context: Context): Navigation {
            return instance ?: synchronized(this) {
                instance ?: Navigation(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val utils = Utils(context)
    private var anResolver = AnnouncementProvider.getInstance(context)

    // 导航路段列表
    var stepList: List<DrivingPlanning.Step>? = null

    // 上一次定位距离路段起点列表
    private var lastStepStartDistanceList = mutableListOf<Double>()

    // 当前定位距离路段起点列表
    private val curStepStartDistanceList = mutableListOf<Double>()

    //  上一次定位距离路段终点列表
    private var lastStepEndDistanceList = mutableListOf<Double>()

    // 当前定位距离路段终点列表
    private val curStepEndDistanceList = mutableListOf<Double>()


    // 当前所在路段下标，-1表示不在任何路段上或未开始导航
    var currentStepIndex: Int by Delegates.observable(-1) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onStepIndexChanged()
        }
    }

    fun startNavigation(steps: List<DrivingPlanning.Step>) {
        this.stepList = steps
    }

    fun onLocationChanged(location: AMapLocation) {
        val currentSteps = stepList ?: return

        // 获取当前位置所在路段
        val currentLocation = LatLng(location.latitude, location.longitude)
        for (index in currentSteps.indices) {
            if (isOnStep(currentLocation, currentSteps[index].polyline)) {
                currentStepIndex = index
                break
            }
        }

        updDistance(currentSteps, currentLocation)

        if (currentStepIndex != -1) {
            // [currentStepIndex - 1, currentStepIndex + 1]
            for (i in (currentStepIndex - 1).coerceAtLeast(0)..(currentStepIndex + 1).coerceAtMost(
                currentSteps.size - 1
            )) {

                // 上次定位是否位于点内
                val lastInside = lastStepStartDistanceList[i] <= 20
                // 当前定位是否位于点外
                val curOutside = curStepStartDistanceList[i] >= 20

                if (lastInside && curOutside) {
                    // 驶出路段起点
                    onExitStart(i)
                }

                // 上次定位是否位于点外
                val lastOutside = lastStepEndDistanceList[i] >= 80
                // 当前定位是否位于点内
                val curInside = curStepEndDistanceList[i] <= 80

                if (lastOutside && curInside) {
                    // 驶入路段终点
                    onEnterEnd(i)
                }
            }


        }


    }

    fun updDistance(currentSteps: List<DrivingPlanning.Step>, currentLocation: LatLng) {
        // 上一次定位距离
        lastStepStartDistanceList.clear()
        lastStepStartDistanceList.addAll(curStepStartDistanceList)

        // 计算路段起点距离当前位置距离
        curStepStartDistanceList.clear()
        for (step in currentSteps) {
            curStepStartDistanceList.add(
                utils.calculateDistance(
                    step.polyline.first().longitude,
                    step.polyline.first().latitude,
                    currentLocation.longitude,
                    currentLocation.latitude
                )
            )
        }

        // 上一次定位距离路段终点列表
        lastStepEndDistanceList.clear()
        lastStepEndDistanceList.addAll(curStepEndDistanceList)

        curStepEndDistanceList.clear()
        for (step in currentSteps) {
            curStepEndDistanceList.add(
                utils.calculateDistance(
                    step.polyline.last().longitude,
                    step.polyline.last().latitude,
                    currentLocation.longitude,
                    currentLocation.latitude
                )
            )
        }
    }

    /** 驶出路段起点 */
    private fun onExitStart(index: Int) {
        val step = stepList?.getOrNull(index) ?: return
        Log.d("L59", "驶出路段起点: ${step}")

        val navVoiceStartList = mutableListOf<String>()
        navVoiceStartList.add("向${step.orientation}方向")
        if (step.road_name.isNotBlank()) {
            navVoiceStartList.add("沿")
            navVoiceStartList.add(step.road_name)
        }
        navVoiceStartList.add("行驶")

        val stepDistanceInt = step.step_distance.toIntOrNull()
        if (stepDistanceInt != null) {
            navVoiceStartList.add(step.step_distance)

        }

        navVoiceStartList.add("米")

        Log.d("L59", "驶出路段起点 导航播报: ${navVoiceStartList.joinToString("|")}")
        playNaviVoice(navVoiceStartList)

    }

    /** 驶入路段终点 */
    private fun onEnterEnd(index: Int) {
        val step = stepList?.getOrNull(index) ?: return
        Log.d("L59", "驶入路段终点: ${step}")

        if (step.navi["assistant_action"] != null && step.navi["assistant_action"] == "到达途经地") {
            return
        }

        val navVoiceStartList = mutableListOf<String>()
        if (step.navi["action"] != null) {
            navVoiceStartList.add(step.navi["action"]!!)
        }

        if (step.navi["assistant_action"] != null) {
            navVoiceStartList.add(step.navi["assistant_action"]!!)
        }

        val nextStep = stepList?.getOrNull(index + 1)
        if (nextStep != null) {
            navVoiceStartList.add("进入")
            navVoiceStartList.add(nextStep.road_name)
        }

        Log.d("L59", "驶入路段终点 导航播报: ${navVoiceStartList.joinToString("|")}")
        playNaviVoice(navVoiceStartList)
    }

    private fun playNaviVoice(navVoiceList: MutableList<String>) {

    }

    // 当前路段变化
    private fun onStepIndexChanged() {
        val currentStep = stepList?.get(currentStepIndex) ?: return
        Log.d("L59", "当前路段: $currentStep")
    }


    /**
     * 判断点是否在路段上
     * @param currentPos 当前位置
     * @param roadPoints 路段的顶点列表
     */
    fun isOnStep(currentPos: LatLng, roadPoints: List<LatLng>): Boolean {
        if (roadPoints.size < 2) return false


        val result = SpatialRelationUtil.calShortestDistancePoint(
            roadPoints,
            currentPos
        )

        val distance = utils.calculateDistance(
            result.second.longitude,
            result.second.latitude,
            currentPos.longitude,
            currentPos.latitude
        )

        return (distance < 10)

    }


}