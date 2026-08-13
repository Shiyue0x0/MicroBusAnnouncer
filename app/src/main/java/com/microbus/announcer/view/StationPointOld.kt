package com.microbus.announcer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class StationPointOld @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        // 不需要在init中设置高度，在onMeasure中处理
    }

    /**
     * 设置填充颜色
     */
    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    /**
     * 设置填充颜色（使用颜色资源）
     */
    fun setColorRes(resId: Int) {
        paint.color = context.getColor(resId)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制填充整个View区域
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 宽度使用父容器给的尺寸
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // 高度固定
        val height = dpToPx(4f)
        setMeasuredDimension(width, height)
    }

    /**
     * dp转px辅助方法
     */
    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}