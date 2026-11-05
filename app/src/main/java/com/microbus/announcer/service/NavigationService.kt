package com.microbus.announcer.service

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocation
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.utils.SpatialRelationUtil
import com.microbus.announcer.Utils
import com.microbus.announcer.announce.AnnouncementContext
import com.microbus.announcer.announce.AnnouncementPlayer
import com.microbus.announcer.announce.AnnouncementProvider
import com.microbus.announcer.bean.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class NavigationService private constructor(context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: NavigationService? = null

        fun getInstance(context: Context): NavigationService {
            return instance ?: synchronized(this) {
                instance ?: NavigationService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val utils = Utils(context)
    private var anResolver = AnnouncementProvider.getInstance(context)
    private val anPlayer = AnnouncementPlayer.getInstance(context)

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

        if (!utils.getIsEnableOnlineNavigation()) {
            return
        }

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

            // 下一路段预报
            val nextStep = currentSteps.getOrNull(currentStepIndex + 1)

            if (nextStep != null) {
                val targetStepIndex = currentStepIndex + 1
                listOf(200.0, 100.0, 30.0).forEach { threshold ->
                    checkEnterStart(
                        lastStepStartDistanceList,
                        curStepStartDistanceList,
                        targetStepIndex,
                        threshold
                    )
                }
            }

        }


    }

    private fun checkEnterStart(
        lastList: List<Double>,
        curList: List<Double>,
        index: Int,
        threshold: Double
    ) {
        val lastOutside = lastList[index] >= threshold
        val curInside = curList[index] <= threshold
        if (lastOutside && curInside) {
            onEnterStart(index, threshold)
        }
    }

    fun onEnterStart(index: Int, threshold: Double) {
        val step = stepList?.getOrNull(index) ?: return
        Log.d("L59", "驶入路段起点: $step")

        if (step.navi["assistant_action"] != null && step.navi["assistant_action"] == "到达途经地") {
            return
        }

        val navVoiceList = mutableListOf<String>()

        if (threshold > 30) {
            navVoiceList.add("${threshold.toInt()}米后")
        }

        if (step.navi["action"] != null && step.navi["action"] != "") {
            navVoiceList.add(step.navi["action"]!!)
        }

        if (step.navi["assistant_action"] != null && step.navi["assistant_action"] != "") {
            navVoiceList.add(step.navi["assistant_action"]!!)
        }

        if (step.road_name.isNotBlank()) {
            navVoiceList.add("进入")
            navVoiceList.add(step.road_name)
        }

        navVoiceList.removeAll { it.isEmpty() }

        Log.d("L59", "驶入路段起点 导航播报: ${navVoiceList.joinToString("|")}")
        playNaviVoice(navVoiceList)

    }

    fun updDistance(currentSteps: List<DrivingPlanning.Step>, currentLocation: LatLng) {
        // 上一次定位距离
        lastStepStartDistanceList.clear()
        lastStepStartDistanceList.addAll(curStepStartDistanceList)

        // 计算路段起点距离当前位置距离
        curStepStartDistanceList.clear()
        for ((_, _, _, _, _, _, polyline) in currentSteps) {
            curStepStartDistanceList.add(
                utils.calculateDistance(
                    polyline.first().longitude,
                    polyline.first().latitude,
                    currentLocation.longitude,
                    currentLocation.latitude
                )
            )
        }

        // 上一次定位距离路段终点列表
        lastStepEndDistanceList.clear()
        lastStepEndDistanceList.addAll(curStepEndDistanceList)

        curStepEndDistanceList.clear()
        for ((_, _, _, _, _, _, polyline) in currentSteps) {
            curStepEndDistanceList.add(
                utils.calculateDistance(
                    polyline.last().longitude,
                    polyline.last().latitude,
                    currentLocation.longitude,
                    currentLocation.latitude
                )
            )
        }
    }

    private fun playNaviVoice(navVoiceList: MutableList<String>) {
        val mockStationList = mutableListOf<Station>()

        mockStationList.add(
            Station(
                id = 1,
                latitude = 39.9042,
                longitude = 116.4074
            )
        )

        mockStationList.add(
            Station(
                id = 2,
                latitude = 39.9042,
                longitude = 116.4074
            )
        )

        val announcementContext = AnnouncementContext(
            "",
            mockStationList,
            2,
            0,
            0.0,
            ArrayList()
        )

        // TODO navVoiceList与上次播报相同时不播报

        val navVoiceListStr = navVoiceList.joinToString("|")

        CoroutineScope(Dispatchers.IO).launch {
            val filePathList = anResolver.resolveAndBuild(
                navVoiceListStr,
                announcementContext
            )
            anPlayer.play(filePathList, AnnouncementPlayer.PlayMode.APPEND, 20)
        }

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

        return (distance < 20)

    }


}