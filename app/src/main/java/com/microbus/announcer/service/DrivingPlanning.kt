package com.microbus.announcer.service

import android.content.Context
import android.util.Log
import com.amap.api.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import com.microbus.announcer.R
import com.microbus.announcer.Utils

class DrivingPlanning(context: Context) {

    private val appContext = context.applicationContext
    private val client = OkHttpClient()
    private val utils = Utils(context)

    private val apiKey = appContext.getString(R.string.amapKey_web)

    companion object {
        /** 高德驾车接口单次最多 16 个途经点 */
        private const val MAX_WAYPOINTS_PER_REQUEST = 16
        private const val URL_DRIVING = "https://restapi.amap.com/v5/direction/driving"
    }

    /** 分段 */
    data class Step(
        /** 分段所属的[起点 + 途径点 + 终点]下标
         * 接口不会返回，需要根据waypoints和polyline
         * 0表示起点到下一个途径点（没有途径点就是destination）的分段
         * 1表示第1个途径点到下一个途径点（没有途径点就是destination）的分段
         * 2表示第2个途径点到下一个途径点（没有途径点就是destination）的分段
         * ...
         */
        var waypoint_index: Int,
        /** 行驶指示 */
        val instruction: String,
        /** 进入道路方向 */
        val orientation: String,
        /** 分段道路名称 */
        val road_name: String,
        /** 分段距离信息 */
        val step_distance: String,
        /** 详细导航动作指令
         *  action：导航主要动作指令
         *  assistant_action：导航辅助动作指令*/
        val navi: Map<String, String>,
        /**  分路段坐标点列表 */
        var polyline: List<LatLng>
    )

    /**
     * 驾车路径规划（自动处理途经点超过 16 个的情况）
     *
     * @param origin 起点 "经度,纬度"
     * @param destination 终点 "经度,纬度"
     * @param waypoints 途经点 "lon,lat;lon,lat;..."，可为 null
     * @return 合并后的高德风格 JSON（结构与单次返回兼容），失败返回 null
     */
    suspend fun planDrivingRoute(
        origin: String,
        destination: String,
        waypoints: String?
    ): String? = withContext(Dispatchers.IO) {
        // 1. 解析途经点
        val waypointList: List<String> = waypoints
            ?.split(";")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        Log.d("L47", "waypointList: ${waypointList.size}")

        // 2. 不需要拆分，直接单次请求（保持原有行为）
        if (waypointList.size <= MAX_WAYPOINTS_PER_REQUEST) {
            return@withContext requestOnce(origin, destination, waypoints)
        }

        // 3. 拆分成多段请求
        //    每段结构：起点、终点、该段途经点列表
        data class Segment(val origin: String, val dest: String, val wps: List<String>)

        val segments = mutableListOf<Segment>()
        var currentOrigin = origin
        var index = 0
        while (index < waypointList.size) {
            val end = minOf(index + MAX_WAYPOINTS_PER_REQUEST, waypointList.size)
            val chunk = waypointList.subList(index, end)
            // 本段终点：如果还有剩余点，取本段最后一个途经点作为终点；否则用最终目的地
            val isLast = end == waypointList.size
            val segmentDest = if (isLast) destination else chunk.last()
            // 非最后一段时，该段的最后一个途经点已被当作终点，不再作为 waypoints 传入
            val effectiveWps = if (isLast) chunk else chunk.dropLast(1)

            segments.add(Segment(currentOrigin, segmentDest, effectiveWps))
            currentOrigin = segmentDest
            index = end
        }

        // 4. 依次请求，收集每段成功的响应
        val segmentResponses = mutableListOf<Pair<Segment, JSONObject>>()
        for (seg in segments) {
            val wpStr = if (seg.wps.isEmpty()) null else seg.wps.joinToString(";")
            val body = requestOnce(seg.origin, seg.dest, wpStr) ?: return@withContext null
            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
            // 校验 status
            if (json.optString("status") != "1") {
                return@withContext null
            }
            segmentResponses.add(seg to json)
        }

        // 5. 合并：把所有段的 paths[0].steps 拼起来，合并距离/耗时
        mergeSegmentResponses(segmentResponses.map { it.second })
    }

    /**
     * 驾车路径规划，直接返回解析好的 Step 列表
     *
     * @param origin 起点 "经度,纬度"
     * @param destination 终点 "经度,纬度"
     * @param waypoints 途经点 "lon,lat;lon,lat;..."，可为 null
     * @return 解析后的 Step 列表；失败或无步骤返回空列表
     */
    suspend fun planDrivingSteps(
        origin: String,
        destination: String,
        waypoints: String?
    ): List<Step> {
        val jsonStr = planDrivingRoute(origin, destination, waypoints) ?: return emptyList()
        // TODO 获取waypoint_index
        val stepList = parseSteps(jsonStr)
        getStepListWithWaypointIndex(origin, destination, waypoints, stepList)
        return stepList
    }

    /**
     * 将高德风格 JSON 中的 steps 解析为 List<Step>
     */
    private fun parseSteps(jsonStr: String): List<Step> {
        val result = mutableListOf<Step>()
        try {
            val root = JSONObject(jsonStr)
            if (root.optString("status") != "1") return result

            val route = root.optJSONObject("route") ?: return result
            val paths = route.optJSONArray("paths") ?: return result
            if (paths.length() == 0) return result

            val path = paths.getJSONObject(0) // strategy=2 只有一条
            val steps = path.optJSONArray("steps") ?: return result

            for (i in 0 until steps.length()) {
                val stepJson = steps.getJSONObject(i)

                // 1) 解析 polyline（"lon,lat;lon,lat;..."）
                val polyline = mutableListOf<LatLng>()
                val polylineStr = stepJson.optString("polyline", "")
                if (polylineStr.isNotEmpty()) {
                    polylineStr.split(";").forEach { pair ->
                        val parts = pair.split(",")
                        if (parts.size == 2) {
                            val lon = parts[0].trim().toDoubleOrNull()
                            val lat = parts[1].trim().toDoubleOrNull()
                            if (lon != null && lat != null) {
                                polyline.add(LatLng(lat, lon)) // LatLng(latitude, longitude)
                            }
                        }
                    }
                }

                // 2) 解析 navi（键值对，值可能为 String 或 JSONObject，统一转字符串）
                val navi = mutableMapOf<String, String>()
                val naviJson = stepJson.optJSONObject("navi")
                if (naviJson != null) {
                    naviJson.keys().forEach { key ->
                        navi[key] = naviJson.optString(key, "")
                    }
                }

                result.add(
                    Step(
                        waypoint_index = stepJson.optInt("waypoint_index", -1),
                        instruction = stepJson.optString("instruction", ""),
                        orientation = stepJson.optString("orientation", ""),
                        road_name = stepJson.optString("road_name", ""),
                        step_distance = stepJson.optString("step_distance", ""),
                        navi = navi,
                        polyline = polyline
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun getStepListWithWaypointIndex(
        origin: String,
        destination: String,
        waypoints: String?,
        stepList: List<Step>
    ) {
        // 无途径点
        if (waypoints.isNullOrBlank()) {
            stepList.forEach { it.waypoint_index = 0 }
            return
        }

        // 有途径点，转成 LatLng ArrayList
        val totalpointList = waypoints.split(";").toMutableList()

        // 添加起点和终点
        totalpointList.add(0, origin)
        totalpointList.add(destination)


        // 转换成 LatLng 列表
        val totalPointLatLngList = totalpointList.map { pair ->
            val parts = pair.split(",")
            if (parts.size == 2) {
                val lon = parts[0].trim().toDoubleOrNull()
                val lat = parts[1].trim().toDoubleOrNull()
                if (lon != null && lat != null) {
                    LatLng(lat, lon) // LatLng(latitude, longitude)
                } else null
            } else null
        }.filterNotNull()

//        Log.d(
//            "totalPointLatLngList",
//            "size: ${totalPointLatLngList.size} totalPointLatLngList=${totalPointLatLngList}"
//        )

//        var lastMatchStepIndex = -1

        // 从1开始遍历totalPointLatLngList
        for (i in 1 until totalPointLatLngList.size) {
            val point = totalPointLatLngList[i]
            // 匹配 当前点的和step起点
            val step = stepList.find { step ->
                utils.isSamePoint(step.polyline.first(), point)
//                        && stepList.indexOf(step) > lastMatchStepIndex
            }
            if (step != null) {
                val matchStepIndex = stepList.indexOf(step)
//                lastMatchStepIndex = matchStepIndex
//                Log.d("step", "find ${matchStepIndex} ${step}")
                stepList.forEachIndexed { index, step ->
                    if (step.waypoint_index == -1 && index < matchStepIndex) {
                        step.waypoint_index = i - 1
                    }
                }
            }
        }

        // 兜底：仍未赋值的归入最后一段
        val lastIndex = totalPointLatLngList.size - 2
        stepList.forEach { if (it.waypoint_index == -1) it.waypoint_index = lastIndex }

        // 遍历循环打印
//        stepList.forEach { step ->
//            Log.d("step", step.toString())
//        }


    }

    /**
     * 单次请求（原逻辑封装）
     * 返回原始 JSON 字符串；网络异常或非 200 返回 null
     */
    private fun requestOnce(origin: String, destination: String, waypoints: String?): String? {
        val formBodyBuilder = FormBody.Builder()
            .add("key", apiKey)
            .add("origin", origin)
            .add("destination", destination)
            .add("strategy", "2")
            .add("show_fields", "navi,polyline")

        if (!waypoints.isNullOrBlank()) {
            formBodyBuilder.add("waypoints", waypoints)
        }

        val request = Request.Builder()
            .url(URL_DRIVING)
            .post(formBodyBuilder.build())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body.string()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将多段响应合并成一个"高德风格"的 JSON：
     * {
     *   "status":"1", "info":"OK", "count":"1",
     *   "route": {
     *     "origin":"...", "destination":"...",
     *     "paths":[ { "distance":"...", "duration":"...",
     *                 "steps":[ 所有段的 steps 顺序拼接 ] } ]
     *   }
     * }
     */
    private fun mergeSegmentResponses(responses: List<JSONObject>): String {
        if (responses.isEmpty()) return """{"status":"0","info":"EMPTY"}"""

        val mergedRoute = JSONObject()
        // origin / destination 用第一段起点和最后一段终点
        val firstRoute = responses.first().optJSONObject("route") ?: JSONObject()
        val lastRoute = responses.last().optJSONObject("route") ?: JSONObject()
        mergedRoute.put("origin", firstRoute.optString("origin", ""))
        mergedRoute.put("destination", lastRoute.optString("destination", ""))

        val mergedSteps = JSONArray()
        var totalDistance = 0L
        var totalDuration = 0L

        for (resp in responses) {
            val route = resp.optJSONObject("route") ?: continue
            val paths = route.optJSONArray("paths") ?: continue
            if (paths.length() == 0) continue
            val path = paths.getJSONObject(0) // strategy=2 只返回一条
            totalDistance += path.optString("distance").toLongOrNull() ?: 0L
            totalDuration += path.optString("duration").toLongOrNull() ?: 0L
            val steps = path.optJSONArray("steps") ?: continue
            for (i in 0 until steps.length()) {
                mergedSteps.put(steps.getJSONObject(i))
            }
        }

        val mergedPath = JSONObject()
            .put("distance", totalDistance.toString())
            .put("duration", totalDuration.toString())
            .put("steps", mergedSteps)

        mergedRoute.put("paths", JSONArray().put(mergedPath))

        return JSONObject()
            .put("status", "1")
            .put("info", "OK")
            .put("infocode", "10000")
            .put("count", "1")
            .put("route", mergedRoute)
            .toString()
    }
}