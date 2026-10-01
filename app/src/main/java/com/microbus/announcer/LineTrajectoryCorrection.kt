package com.microbus.announcer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.microbus.announcer.bean.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import androidx.core.content.edit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale

private val okHttpClient = OkHttpClient()

/**
 *  高德猎鹰轨迹服务：轨迹纠偏
 */
object LineTrajectoryCorrection {

    // 猎鹰轨迹服务ID
    private val sid = "1078247"

    // 终端ID（空表示未知）
    private var tid = ""
    private val LineTrajectoryCorrectionTidKey = "LineTrajectoryCorrectionTid"

    private lateinit var prefs: SharedPreferences
    private lateinit var utils: Utils
    private lateinit var appContext: Context

    // 路线平均速度（km/h）
    val SPEED_KMH = 15

    suspend fun init(context: Context) {

        appContext = context
        prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        utils = Utils(appContext)

        getOrAddTid()
        Log.d("L81", tid)

    }

    private suspend fun getOrAddTid() {
        tid = prefs.getString(LineTrajectoryCorrectionTidKey, "") ?: ""
        if (tid == "") {
            tid = addTerminal(tName = UUID.randomUUID().toString())
            if (tid != "") {
                prefs.edit {
                    putString(LineTrajectoryCorrectionTidKey, tid)
                }
            } else {
                withContext(Dispatchers.Main) {
                    utils.showMsg("添加终端失败")
                }
            }
        }
    }

    suspend fun getTrack(lineStationList: ArrayList<Station>): MutableList<MutableList<Triple<Double, Double, Int?>>> {

        val trid = addTrace()
        Log.d("L90", "${trid}")

        val points = getStationPoints(lineStationList)
        Log.d("L76", points.toString())

        val uploadPointRes = uploadPoint(trid, points.toString())
        Log.d("L92", "${uploadPointRes}")

        // TODO 考虑多页
        val terminalTrsearchRes = terminalTrsearch(trid, 1, 999)
        Log.d("L97", "${terminalTrsearchRes?.toString(2)}")

        val locationsAndIndex: ArrayList<Triple<Double, Double, Int?>> =
            getLocationsFromTracks(tracks = terminalTrsearchRes?.optJSONArray("tracks"))
        Log.d("L101", locationsAndIndex.chunked(10).joinToString("\n"))

        val grouped = groupLocations(locationsAndIndex)

//        Log.d("L90", "${grouped.size} ${lineStationList.size}")

        // 站点上补点
        grouped.forEachIndexed { i, _ ->
            grouped[i].add(
                0,
                Triple(
                    lineStationList[i].longitude,
                    lineStationList[i].latitude,
                    i
                )
            )
            grouped[i].add(
                Triple(
                    lineStationList[i + 1].longitude,
                    lineStationList[i + 1].latitude,
                    i
                )
            )
        }

        return grouped

    }

    private fun groupLocations(locationsAndIndex: ArrayList<Triple<Double, Double, Int?>>): MutableList<MutableList<Triple<Double, Double, Int?>>> {

        val maxIndex = locationsAndIndex.mapNotNull { it.third }.maxOrNull()
            ?: return MutableList(0) { mutableListOf() }
        val grouped: MutableList<MutableList<Triple<Double, Double, Int?>>> =
            MutableList(maxIndex) { mutableListOf() }

        var currentIndex = -1

        for (item in locationsAndIndex) {
            if (item.third != null) {
                currentIndex = item.third!!
                if (currentIndex >= maxIndex) break  // 最后一个站点之后的不需要（或者你也可以保留）
                grouped[currentIndex].add(item)
            } else {
                if (currentIndex in 0 until maxIndex) {
                    grouped[currentIndex].add(item)
                }
            }
        }

        return grouped
    }

    private fun getStationPoints(stationList: ArrayList<Station>): JSONArray {
        var lastStationLocatetime = 0L

        val points = JSONArray()

        for (i in stationList.indices) {

            val station = stationList[i]
            var prevStation: Station? = null
            if (i > 0) {
                prevStation = stationList[i - 1]
            }

            val point = JSONObject()
            point.put("location", getPointLocation(station))
            lastStationLocatetime = getLocatetime(station, prevStation, lastStationLocatetime)
            point.put("locatetime", lastStationLocatetime)
//            point.put("speed", SPEED_KMH)
            point.put("props", JSONObject().apply {
                put("stationIndex", i)
            })
            points.put(point)
        }

        return points

    }

    fun getPointLocation(station: Station): String {
        val longitude = String.format(Locale.US, "%.6f", station.longitude)
        val latitude = String.format(Locale.US, "%.6f", station.latitude)
        return "$longitude,$latitude"
    }

    fun getLocatetime(station: Station, prevStation: Station?, lastStationLocatetime: Long): Long {

        // 第一站：昨天 12:00
        if (prevStation == null) {
            return yesterdayNoonMillis()
        }

        // 上一站到本站的距离（km）
        val distanceKm = utils.calculateDistance(
            prevStation.longitude, prevStation.latitude,
            station.longitude, station.latitude
        ) / 1000

        // 耗时（ms）= 距离 / 速度 * 3600_000
        val durationMs = if (SPEED_KMH > 0) {
            (distanceKm / SPEED_KMH * 3600_000).toLong()
        } else {
            0L
        }

        return lastStationLocatetime + durationMs
    }

    /**
     * 昨天 12:00 的 Unix 毫秒时间戳
     */
    private fun yesterdayNoonMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     *  添加指定的终端，首次使用时创建
     *  @return 终端ID（tid）
     */
    suspend fun addTerminal(tName: String): String = postForDataField(
        url = "https://tsapi.amap.com/v1/track/terminal/add",
        params = mapOf(
            "key" to appContext.getString(R.string.amapKey_web),
            "sid" to sid,
            "name" to tName
        ),
        dataField = "tid",
        tag = "addTerminal"
    )

    /**
     *  创建轨迹
     *  @return 轨迹ID（trid）
     */
    suspend fun addTrace(): String = postForDataField(
        url = "https://tsapi.amap.com/v1/track/trace/add",
        params = mapOf(
            "key" to appContext.getString(R.string.amapKey_web),
            "sid" to sid,
            "tid" to tid
        ),
        dataField = "trid",
        tag = "addTrace"
    )

    /**
     *  轨迹点上传（单点、批量）
     *  @return 错误点的数据errorpoints
     */
    suspend fun uploadPoint(trid: String, pointsJSONStr: String) = postForDataField(
        url = "https://tsapi.amap.com/v1/track/point/upload",
        params = mapOf(
            "key" to appContext.getString(R.string.amapKey_web),
            "sid" to sid,
            "tid" to tid,
            "trid" to trid,
            "points" to pointsJSONStr
        ),
        dataField = "errorpoints",
        tag = "uploadPoint"
    )

    /**
     *  查询轨迹信息（经纬度点、里程、时间等）
     *
     *  查询策略二选一：
     *  1. 查询指定轨迹：传 trid（单次最多查 24 小时内轨迹）；
     *  2. 查询指定终端某时间段内所有轨迹：传 starttime + endtime（跨度不超过 24 小时）。
     *
     *  @param trid       轨迹唯一编号，查询指定轨迹时必填
     *  @param page       查询页数，最大 100
     *  @param pagesize   每页点数，最大 999
     *  @return 成功返回 data 的 JSONObject（含 trid、tracks 等），失败返回 null
     */
    suspend fun terminalTrsearch(
        trid: String,
        page: Int? = null,
        pagesize: Int? = null
    ): JSONObject? {

        val params = mutableMapOf(
            "key" to appContext.getString(R.string.amapKey_web),
            "sid" to sid,
            "tid" to tid,
            "trid" to trid,
            "correction" to "denoise=0,mapmatch=1,attribute=1,threshold=0,mode=driving",
            "recoup" to "1",
            "gap" to "700",
        )

        page?.let { params["page"] = it.toString() }
        pagesize?.let { params["pagesize"] = it.toString() }

        return getForData(
            url = "https://tsapi.amap.com/v1/track/terminal/trsearch",
            params = params,
            tag = "terminalTrsearch"
        )
    }

    fun getLocationsFromTracks(tracks: JSONArray?): ArrayList<Triple<Double, Double, Int?>> {
        val locations = ArrayList<Triple<Double, Double, Int?>>() // (经度, 纬度, stationIndex)

        if (tracks != null) {
            for (i in 0 until tracks.length()) {
                val track = tracks.optJSONObject(i) ?: continue
                val points = track.optJSONArray("points") ?: continue

                for (j in 0 until points.length()) {
                    val point = points.optJSONObject(j) ?: continue

                    val loc = point.optString("location")
                    if (loc.isEmpty()) continue

                    val parts = loc.split(",")
                    if (parts.size != 2) continue

                    val lng = parts[0].toDoubleOrNull() ?: continue
                    val lat = parts[1].toDoubleOrNull() ?: continue

                    // 提取 stationIndex，可能不存在
                    val stationIndex = point
                        .optJSONObject("props")
                        ?.optInt("stationIndex", -1)
                        ?.takeIf { it != -1 }

                    locations.add(Triple(lng, lat, stationIndex))
                }
            }
        }

        return locations
    }

    /**
     *  公共请求方法：向高德猎鹰接口 POST 表单，校验 errcode 并取出 data 中的指定字段
     *  @param url       接口地址
     *  @param params    表单参数（key-value）
     *  @param dataField 成功时从 data 对象中取出的字段名，如 "tid"、"trid"
     *  @param tag       日志标签，用于区分调用来源
     *  @return 成功返回字段值，失败返回 ""
     */
    private suspend fun postForDataField(
        url: String,
        params: Map<String, String>,
        dataField: String,
        tag: String
    ): String = withContext(Dispatchers.IO) {

        val formBodyBuilder = FormBody.Builder()
        params.forEach { (k, v) -> formBodyBuilder.add(k, v) }
        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val bodyStr = response.body.string()
            val json = JSONObject(bodyStr)
            try {
                if (json.optInt("errcode", -1) != 10000) {
                    throw IOException("$tag failed: ${json.optString("errmsg")} ($bodyStr)")
                }
                return@withContext json.getJSONObject("data").getString(dataField)
            } catch (e: Exception) {
                Log.e("L81", "$tag 解析失败: ${e.message}, body=$bodyStr", e)
            }
        }
        return@withContext ""
    }

    /**
     *  公共 GET 请求方法：向高德猎鹰接口发起 GET，校验 errcode 并返回 data 对象
     *  @param url    接口地址
     *  @param params 查询参数（key-value）
     *  @param tag    日志标签，用于区分调用来源
     *  @return 成功返回 data 的 JSONObject，失败返回 null
     */
    private suspend fun getForData(
        url: String,
        params: Map<String, String>,
        tag: String
    ): JSONObject? = withContext(Dispatchers.IO) {

        val httpUrl = url.toHttpUrlOrNull()?.newBuilder()?.apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }?.build() ?: run {
            Log.e("L81", "$tag 非法 URL: $url")
            return@withContext null
        }

        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val bodyStr = response.body.string()
            val json = JSONObject(bodyStr)
            try {
                if (json.optInt("errcode", -1) != 10000) {
                    throw IOException("$tag failed: ${json.optString("errmsg")} ($bodyStr)")
                }
                return@withContext json.optJSONObject("data")
            } catch (e: Exception) {
                Log.e("L81", "$tag 解析失败: ${e.message}, body=$bodyStr", e)
            }
        }
        return@withContext null
    }
}
