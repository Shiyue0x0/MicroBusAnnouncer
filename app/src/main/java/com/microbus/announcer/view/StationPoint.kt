package com.microbus.announcer.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.microbus.announcer.R
import androidx.core.graphics.withClip

class StationPoint @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var mHeight = 6f

    // 圆角相关
    private var cornerRadius = 0f
    private var isLeftRounded = false
    private var isRightRounded = false
    private val path = Path()
    private val rectF = RectF()

    // 预分配Rect对象，避免在onDraw中创建
    private val srcRect = Rect()
    private val dstRect = Rect()

    init {
        // 加载默认贴图并旋转90度
        loadAndRotateBitmap(R.mipmap.line_gray)

        // 从XML属性读取配置（可选）
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StationPoint,
            0, 0
        ).apply {
            try {
                isLeftRounded = getBoolean(R.styleable.StationPoint_leftRounded, false)
                isRightRounded = getBoolean(R.styleable.StationPoint_rightRounded, false)
                cornerRadius = getDimension(
                    R.styleable.StationPoint_cornerRadius,
                    dpToPx(4f).toFloat()
                )
            } finally {
                recycle()
            }
        }
    }

    /**
     * 设置贴图（使用资源ID），自动旋转90度
     */
    fun setImageResource(resId: Int) {
        loadAndRotateBitmap(resId)
        invalidate()
    }

    /**
     * 设置圆角
     * @param radius 圆角半径（px）
     * @param left 是否启用左圆角
     * @param right 是否启用右圆角
     */
    fun setCorner(left: Boolean, right: Boolean) {
        this.cornerRadius = mHeight / 2
        this.isLeftRounded = left
        this.isRightRounded = right
        invalidate()
    }

    /**
     * 设置左圆角
     */
    fun setLeftRounded(rounded: Boolean) {
        this.isLeftRounded = rounded
        invalidate()
    }

    /**
     * 设置右圆角
     */
    fun setRightRounded(rounded: Boolean) {
        this.isRightRounded = rounded
        invalidate()
    }

    /**
     * 设置圆角半径
     */
    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        invalidate()
    }

    /**
     * 加载Bitmap资源并顺时针旋转90度
     */
    private fun loadAndRotateBitmap(resId: Int) {
        bitmap?.recycle()

        val originalBitmap = BitmapFactory.decodeResource(context.resources, resId)
        bitmap = rotateBitmap(originalBitmap, 90f)

        if (originalBitmap != bitmap) {
            originalBitmap.recycle()
        }
    }

    /**
     * 旋转Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { bmp ->
            // 如果没有任何圆角，直接绘制
            if (!isLeftRounded && !isRightRounded) {
                srcRect.set(0, 0, bmp.width, bmp.height)
                dstRect.set(0, 0, width, height)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                return@let
            }

            // 有圆角时使用Path裁剪
            path.reset()
            rectF.set(0f, 0f, width.toFloat(), height.toFloat())

            // 构建圆角路径
            val radii = floatArrayOf(
                // 左上角 (x, y)
                if (isLeftRounded) cornerRadius else 0f,
                if (isLeftRounded) cornerRadius else 0f,
                // 右上角 (x, y)
                if (isRightRounded) cornerRadius else 0f,
                if (isRightRounded) cornerRadius else 0f,
                // 右下角 (x, y)
                if (isRightRounded) cornerRadius else 0f,
                if (isRightRounded) cornerRadius else 0f,
                // 左下角 (x, y)
                if (isLeftRounded) cornerRadius else 0f,
                if (isLeftRounded) cornerRadius else 0f
            )

            path.addRoundRect(rectF, radii, Path.Direction.CW)
            canvas.withClip(path) {
                srcRect.set(0, 0, bmp.width, bmp.height)
                dstRect.set(0, 0, width, height)
                drawBitmap(bmp, srcRect, dstRect, paint)

            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = dpToPx(mHeight)
        setMeasuredDimension(width, height)
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}