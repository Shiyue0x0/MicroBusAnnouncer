package com.microbus.announcer

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.microbus.announcer.bean.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class TranslationService private constructor(context: Context) {

    var tag: String = javaClass.simpleName

    private val appContext: Context = context.applicationContext
    private val utils = Utils(appContext)

    // ---- 本地缓存 ----
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val taskQueue = CopyOnWriteArrayList<TranslationTask>()
    private var workerJob: Job? = null

    private data class TranslationTask(
        val station: Station,
        val sourceText: String,
        val onSuccess: ((String) -> Unit)? = null,
        val onFailure: ((Throwable?) -> Unit)? = null,
    )

    companion object {
        private const val PREFS_NAME = "translation_cache"
        private const val KEY_PREFIX = "tr_"

        @Volatile
        private var INSTANCE: TranslationService? = null

        fun getInstance(context: Context): TranslationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TranslationService(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 缓存读写
    // ---------------------------------------------------------------------

    /** 读缓存；未命中返回 null */
    private fun getCachedTranslation(sourceText: String): String? {
        return prefs.getString(KEY_PREFIX + sourceText, null)
    }

    /** 写缓存 */
    private fun putCachedTranslation(sourceText: String, translated: String) {
        if (sourceText.isBlank() || translated.isBlank()) return
        prefs.edit().putString(KEY_PREFIX + sourceText, translated).apply()
    }

    /** 清空翻译缓存 */
    fun clearCache() {
        prefs.edit().clear().apply()
    }

    /** 移除单条缓存 */
    fun removeCache(sourceText: String) {
        prefs.edit().remove(KEY_PREFIX + sourceText).apply()
    }

    // ---------------------------------------------------------------------
    // 对外接口
    // ---------------------------------------------------------------------

    fun requestTranslation(
        station: Station,
        onSuccess: ((String) -> Unit)? = null,
        onFailure: ((Throwable?) -> Unit)? = null,
    ) {
        val sourceText = station.cnName
        if (sourceText.isBlank()) {
            onFailure?.invoke(IllegalArgumentException("站点中文名为空"))
            return
        }

        // 若已有结果（enName 非空且不等于 cnName），认为已翻译
        if (station.enName.isNotBlank() && station.enName != sourceText) {
            onSuccess?.invoke(station.enName)
            return
        }

        // 1) 优先查本地缓存
        val cached = getCachedTranslation(sourceText)
        if (!cached.isNullOrBlank()) {
            station.enName = cached
            Log.d(tag, "命中缓存：$sourceText -> $cached")
            onSuccess?.invoke(cached)
            return
        }

        // 2) 去重入队
        val exists = taskQueue.any { it.station === station || it.station.id == station.id }
        if (exists) {
            Log.d(tag, "任务已存在，跳过：$sourceText")
            return
        }

        taskQueue.add(TranslationTask(station, sourceText, onSuccess, onFailure))
        Log.d(tag, "入队：$sourceText，当前队列 ${taskQueue.size}")
        startWorkerIfNeeded()
    }

    fun requestTranslationBatch(
        stations: List<Station>,
        onAllDone: (() -> Unit)? = null,
    ) {
        if (stations.isEmpty()) {
            onAllDone?.invoke()
            return
        }

        var remaining = stations.size
        val lock = Any()

        stations.forEach { station ->
            requestTranslation(station, onSuccess = {
                synchronized(lock) {
                    remaining--
                    if (remaining <= 0) onAllDone?.invoke()
                }
            }, onFailure = {
                synchronized(lock) {
                    remaining--
                    if (remaining <= 0) onAllDone?.invoke()
                }
            })
        }
    }

    fun cancelAll() {
        taskQueue.clear()
        workerJob?.cancel()
        workerJob = null
    }

    // ---------------------------------------------------------------------
    // 队列 worker
    // ---------------------------------------------------------------------

    private fun startWorkerIfNeeded() {
        if (workerJob?.isActive == true) return

        Log.d(tag, "启动翻译 worker")
        workerJob = CoroutineScope(Dispatchers.IO.limitedParallelism(1)).launch {
            while (isActive) {
                val task = taskQueue.removeFirstOrNull()
                if (task == null) {
                    Log.d(tag, "队列为空，worker 退出")
                    return@launch
                }

                try {
                    val result = translateOne(task.sourceText)
                    if (result.isNotEmpty()) {
                        task.station.enName = result
                        // 写入缓存
                        putCachedTranslation(task.sourceText, result)
                        Log.d(tag, "翻译成功：${task.sourceText} -> $result")
                        withContext(Dispatchers.Main) {
                            task.onSuccess?.invoke(result)
                        }
                    } else {
                        Log.w(tag, "翻译结果为空：${task.sourceText}")
                        withContext(Dispatchers.Main) {
                            task.onFailure?.invoke(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "翻译失败：${task.sourceText}", e)
                    withContext(Dispatchers.Main) {
                        task.onFailure?.invoke(e)
                    }
                }

                delay(100L.milliseconds)
            }
        }
    }

    // ---------------------------------------------------------------------
    // 单个翻译请求
    // ---------------------------------------------------------------------

    private suspend fun translateOne(sourceText: String): String {
        val requestCode = utils.getOnlineTranslationRequestCode()
        if (requestCode.isBlank()) {
            Log.w(tag, "翻译请求模板为空")
            return ""
        }

        val parseRequest = try {
            HTTPRequest().parseRequest(requestCode)
        } catch (e: Exception) {
            Log.e(tag, "解析翻译请求模板失败", e)
            return ""
        }

        val url = parseRequest.url.replace("{{text}}", sourceText)
        val body = parseRequest.body?.replace("{{text}}", sourceText)

        val builder = Request.Builder().url(url)
        parseRequest.headers.forEach { (k, v) -> builder.addHeader(k, v) }

        when (parseRequest.method.uppercase()) {
            "GET" -> builder.get()

            "POST" -> {
                val contentType = parseRequest.headers["Content-Type"]
                    ?.toMediaTypeOrNull()
                    ?: "application/json".toMediaTypeOrNull()
                builder.post((body ?: "").toRequestBody(contentType))
            }

            else -> {
                Log.e(tag, "不支持的请求方法：${parseRequest.method}")
                return ""
            }
        }

        return try {
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body.string()
                    Log.e(tag, "HTTP ${resp.code}：$err")
                    return ""
                }
                val respBody = resp.body.string()
                parseTranslationResult(respBody)
            }
        } catch (e: IOException) {
            Log.e(tag, "网络请求失败", e)
            ""
        }
    }

    private fun parseTranslationResult(jsonStr: String): String {
        if (jsonStr.isBlank()) return ""

        val jsonPath = utils.getOnlineTranslationResponseJSONPath()
        if (jsonPath.isBlank()) {
            Log.w(tag, "JSONPath 为空")
            return ""
        }

        return try {
            val root = JsonParser.parseString(jsonStr)
            var current = root

            for (segment in jsonPath.split('.')) {
                if (current == null) return ""
                current = when {
                    current.isJsonObject -> current.asJsonObject.get(segment)
                    current.isJsonArray -> {
                        val idx = segment.toIntOrNull() ?: return ""
                        current.asJsonArray.get(idx)
                    }

                    else -> return ""
                }
            }

            if (current == null || current.isJsonNull) {
                ""
            } else if (current.isJsonPrimitive) {
                current.asJsonPrimitive.asString
            } else {
                current.toString()
            }
        } catch (e: Exception) {
            Log.e(tag, "解析翻译结果失败：$jsonStr", e)
            ""
        }
    }
}