package com.microbus.announcer.service

import android.app.UiModeManager
import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import com.microbus.announcer.R
import androidx.core.graphics.createBitmap
import com.microbus.announcer.bean.Station

class StationMarkerFactory(
    private val appContext: Context,
    private val fontResId: Int = R.font.galano_grotesque_bold
) {

    companion object {
        private const val MARKER_SIZE = 72
        private const val STROKE_RATIO = 12f
        private const val MARKER_TEXT_SIZE_RATIO_SHORT = 0.5f
        private const val MARKER_TEXT_SIZE_RATIO_LONG = 0.4f
        private const val CACHE_SIZE = 64

        // SMALL 模式（pill 形状）参数
        private const val SMALL_MARKER_HEIGHT = 72
        private const val SMALL_MARKER_H_PADDING = 24f   // pill 左右内边距
        private const val SMALL_MARKER_TEXT_SIZE = 40f
        private const val SMALL_MARKER_CORNER_RATIO = 0.5f

        private val COLOR_UNKNOWN = "#B6B6B6".toColorInt()
        private val COLOR_CURRENT = "#1996D8".toColorInt()
        private val COLOR_FORWARD = "#37B267".toColorInt()
    }


    // ==================== Paint 复用 ====================
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val uiModeManager =
            appContext.getSystemService(UI_MODE_SERVICE) as UiModeManager
        val backgroundColor = if (uiModeManager.nightMode == MODE_NIGHT_YES) {
            // 灰色
            "#36425A".toColorInt()
        } else {
            Color.WHITE
        }

        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = MARKER_SIZE / STROKE_RATIO
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // ==================== Typeface 惰性加载 + 缓存 ====================
    @Volatile
    private var cachedTypeface: Typeface? = null

    private fun mGetTypeface(): Typeface? {
        return cachedTypeface ?: ResourcesCompat.getFont(appContext, fontResId)
            ?.also { cachedTypeface = it }
    }

    // ==================== Bitmap 缓存 ====================
    private val markerCache = LruCache<String, Bitmap>(CACHE_SIZE)

    /**
     * 创建站点 Marker Bitmap。
     *
     * @param stationNumber 站点索引（0 基）
     * @param newStationNumber 当前站点索引或id，null 表示未知
     */
    fun createStationMarkerBitmap(
        stationNumber: Int,
        newStationNumber: Int?,
        zoomLevel: LinePathAndStationPointService.MapZoomLevel,
        station: Station,
        markerNumberType: LinePathAndStationPointService.MarkerNumberType
    ): Bitmap {
        var mainColor = when {
            newStationNumber == null -> COLOR_UNKNOWN
            stationNumber == newStationNumber -> COLOR_CURRENT
            stationNumber > newStationNumber -> COLOR_FORWARD
            else -> COLOR_UNKNOWN
        }

        if (markerNumberType == LinePathAndStationPointService.MarkerNumberType.ID) {
            mainColor = COLOR_UNKNOWN
        }

        // 缓存 key：颜色 + 站点序号 + 缩放级别（SMALL 还依赖 cnName 决定宽度）
        val cacheKey = buildString {
            append(mainColor)
                .append('-')
                .append(stationNumber)
                .append('-')
                .append(zoomLevel)
            if (zoomLevel == LinePathAndStationPointService.MapZoomLevel.SMALL) {
                append('-')
                    .append(station.cnName)
            }
        }
        markerCache.get(cacheKey)?.let { return it }

        val bitmap = when (zoomLevel) {
            LinePathAndStationPointService.MapZoomLevel.LAGER ->
                drawCircleMarker(stationNumber, mainColor, markerNumberType)

            LinePathAndStationPointService.MapZoomLevel.SMALL ->
                drawPillMarker(stationNumber, station.cnName, mainColor, markerNumberType)
        }
        markerCache.put(cacheKey, bitmap)
        return bitmap
    }

    // ==================== LAGER：圆形 marker（原逻辑） ====================
    private fun drawCircleMarker(
        stationNumber: Int,
        mainColor: Int,
        markerNumberType: LinePathAndStationPointService.MarkerNumberType
    ): Bitmap {
        val bitmap = createBitmap(MARKER_SIZE, MARKER_SIZE)
        val canvas = Canvas(bitmap)

        val radius = MARKER_SIZE / 2f
        val drawRadius = radius - strokePaint.strokeWidth / 2f

        // 背景颜色填充圆
        canvas.drawCircle(radius, radius, drawRadius, fillPaint)

        // 彩色描边圆
        strokePaint.color = mainColor
        canvas.drawCircle(radius, radius, drawRadius, strokePaint)

        // 绘制序号
        val text = if (markerNumberType == LinePathAndStationPointService.MarkerNumberType.INDEX) {
            (stationNumber + 1).toString()
        } else {
            stationNumber.toString()
        }
        textPaint.apply {
            color = mainColor
            textSize = if (text.length <= 2) {
                MARKER_SIZE * MARKER_TEXT_SIZE_RATIO_SHORT
            } else {
                MARKER_SIZE * MARKER_TEXT_SIZE_RATIO_LONG
            }
            typeface = mGetTypeface()
        }

        // 文字垂直居中
        val fm = textPaint.fontMetrics
        val baseline = radius - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, radius, baseline, textPaint)

        return bitmap
    }

    // ==================== SMALL：pill marker（序号 + cnName） ====================
    private fun drawPillMarker(
        stationNumber: Int,
        cnName: String,
        mainColor: Int,
        markerNumberType: LinePathAndStationPointService.MarkerNumberType
    ): Bitmap {

        // 绘制序号与站点名称
        val number = if (markerNumberType == LinePathAndStationPointService.MarkerNumberType.INDEX) {
            (stationNumber + 1).toString()
        } else {
            stationNumber.toString()
        }

        val text = "$number $cnName"

        textPaint.apply {
            color = mainColor
            textSize = SMALL_MARKER_TEXT_SIZE
            typeface = mGetTypeface()
        }

        val textWidth = textPaint.measureText(text)
        val width =
            (textWidth + SMALL_MARKER_H_PADDING * 2f).toInt().coerceAtLeast(SMALL_MARKER_HEIGHT)
        val height = SMALL_MARKER_HEIGHT

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val strokeWidth = strokePaint.strokeWidth
        val rect = RectF(
            strokeWidth / 2f,
            strokeWidth / 2f,
            width - strokeWidth / 2f,
            height - strokeWidth / 2f
        )
        val corner = height * SMALL_MARKER_CORNER_RATIO

        // 白色填充 pill
        canvas.drawRoundRect(rect, corner, corner, fillPaint)

        // 彩色描边 pill
        strokePaint.color = mainColor
        canvas.drawRoundRect(rect, corner, corner, strokePaint)

        // 绘制文字，垂直居中
        val fm = textPaint.fontMetrics
        val baseline = height / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, width / 2f, baseline, textPaint)

        return bitmap
    }

    /**
     * 当 newStationIndex 变化导致大量 marker 需要重建时调用，
     * 清空缓存即可（比按 key 精细失效更简单）。
     */
    fun clearCache() {
        markerCache.evictAll()
    }

    /**
     * 释放缓存（Activity/Fragment 销毁时调用，避免内存泄漏）。
     */
    fun release() {
        markerCache.evictAll()
        cachedTypeface = null
    }
}