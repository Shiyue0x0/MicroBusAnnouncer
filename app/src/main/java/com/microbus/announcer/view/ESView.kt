package com.microbus.announcer.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.View
import android.view.ViewGroup
import androidx.core.content.withStyledAttributes
import com.microbus.announcer.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
class ESView : View {

    private lateinit var textPaint: Paint
    private lateinit var backgroundPaint: Paint

    private lateinit var maskPaint: Paint
    private var leftMaskShader: LinearGradient? = null
    private var rightMaskShader: LinearGradient? = null

    private lateinit var text: String
    private var textSize by Delegates.notNull<Float>()
    private var maxWidth by Delegates.notNull<Float>()
    private var textColor by Delegates.notNull<Int>()
    private var textStyle by Delegates.notNull<Int>()
    private var background by Delegates.notNull<Int>()
    private var paddingStart by Delegates.notNull<Float>()
    private var paddingTop by Delegates.notNull<Float>()
    private var paddingEnd by Delegates.notNull<Float>()
    private var paddingBottom by Delegates.notNull<Float>()
    private var cornerRadius by Delegates.notNull<Float>()
    private var fontFamily: String? = ""


    var loopCount = 0

    constructor(context: Context, attrs: AttributeSet) : super(
        context, attrs
    ) {
        loadAttrs(attrs)
        initPaint()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context, attrs, defStyle
    ) {
        loadAttrs(attrs)
        initPaint()
    }

    constructor(context: Context) : super(context) {
        initPaint()
    }

    fun loadAttrs(attrs: AttributeSet) {
        context.withStyledAttributes(attrs, R.styleable.HeaderTextView) {
            text = getString(R.styleable.HeaderTextView_android_text)!!
            textSize = getDimension(R.styleable.HeaderTextView_android_textSize, 0F)
            maxWidth = getDimension(R.styleable.HeaderTextView_android_maxWidth, Float.MAX_VALUE)
            textColor = getColor(R.styleable.HeaderTextView_android_textColor, 0xffffff)
            textStyle = getInt(R.styleable.HeaderTextView_android_textStyle, 0)
            background = getColor(R.styleable.HeaderTextView_background, 0)
            paddingStart = getDimension(R.styleable.HeaderTextView_android_paddingStart, 0F)
            paddingTop = getDimension(R.styleable.HeaderTextView_android_paddingTop, 0F)
            paddingEnd = getDimension(R.styleable.HeaderTextView_android_paddingEnd, 0F)
            paddingBottom = getDimension(R.styleable.HeaderTextView_android_paddingBottom, 0F)
            cornerRadius = getDimension(R.styleable.HeaderTextView_cornerRadius, 0F)
            fontFamily = getString(R.styleable.HeaderTextView_android_fontFamily)
        }
    }


    private fun initPaint() {

        val typeface =
            if (fontFamily == "")
                Typeface.DEFAULT
            else
                context.resources.getFont(R.font.galano_grotesque_bold)

        // 文字画笔
        textPaint = Paint()
        textPaint.textSize = textSize
        textPaint.color = textColor
        textPaint.typeface = Typeface.create(typeface, textStyle)

        // 背景色画笔
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = background

        // 新增：遮罩画笔
        maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.style = Paint.Style.FILL
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    }

    val fillRect = RectF(0F, 0F, 0F, 0F)
    val path = Path()

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var myMeasuredWidth = (measuredWidth + paddingStart + paddingEnd).toInt()

        if (layoutParams.width > maxWidth)
            myMeasuredWidth = MeasureSpec.makeMeasureSpec(
                (maxWidth + paddingStart + paddingEnd).toInt(),
                MeasureSpec.EXACTLY
            )

        if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT)
            myMeasuredWidth =
                MeasureSpec.makeMeasureSpec(
                    (textPaint.measureText(text) + paddingStart + paddingEnd).toInt(),
                    MeasureSpec.EXACTLY
                )


        if (textPaint.measureText(text).toInt() > maxWidth.toInt())
            myMeasuredWidth = MeasureSpec.makeMeasureSpec(
                (maxWidth + paddingStart + paddingEnd).toInt(),
                MeasureSpec.EXACTLY
            )


        val fm = textPaint.fontMetrics

        val myMeasuredHeight =
            MeasureSpec.makeMeasureSpec(
                (fm.bottom - fm.top + paddingTop + paddingBottom).toInt(),
                MeasureSpec.EXACTLY
            )

        setMeasuredDimension(myMeasuredWidth, myMeasuredHeight)

        fillRect.top = 0F
        fillRect.bottom = measuredHeight.toFloat() - paddingBottom
        fillRect.left = paddingStart
        fillRect.right = measuredWidth.toFloat() - paddingEnd

        path.reset()
        path.addRoundRect(
            RectF(0F, 0F, measuredWidth.toFloat(), measuredHeight.toFloat()),
            FloatArray(8) { cornerRadius },
            Path.Direction.CW
        )

        leftMaskShader = LinearGradient(
            paddingLeft.toFloat(),
            0f,
            shaderWidth + paddingLeft.toFloat(),
            0f,
            intArrayOf(Color.TRANSPARENT, Color.WHITE),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        rightMaskShader = LinearGradient(
            measuredWidth - shaderWidth - paddingEnd,
            0f,
            measuredWidth.toFloat() - paddingEnd,
            0f,
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

    }

    var frameCount = 0F
    var allFrameCount = 0F
    var minShowTimeMs = Int.MAX_VALUE

    //    var fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        context.display.refreshRate
//    } else {
//        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        windowManager.defaultDisplay.refreshRate
//    }
    var pixelMovePerSecond = 150F
    var isShowFinish = false
    var scrollX = Float.MAX_VALUE
    val shaderWidth = 40f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fm = textPaint.fontMetrics
        val y = height / 2 + (fm.bottom - fm.top) / 2 - fm.bottom

        canvas.clipPath(path)

        // 绘制背景色
        if (background != Color.TRANSPARENT) {
            canvas.drawColor(backgroundPaint.color)
        }

        // 绘制文字和渐隐层
        canvas.clipRect(fillRect)

        // View宽度足够容纳文本，居中显示
        if (textPaint.measureText(text) <= width - paddingStart - paddingEnd) {
            canvas.drawText(
                text,
                (width - textPaint.measureText(text)) / 2,
                y,
                textPaint
            )
        }
        // View宽度不足够容纳文本，轮播显示，羽化水平边缘
        else {

            val saveCount = canvas.saveLayer(
                0f, 0f, width.toFloat(), height.toFloat(),
                null,
                Canvas.ALL_SAVE_FLAG
            )

            // 注意：scrollX 的更新现在在 FrameCallback 中基于实际时间增量进行
            canvas.drawText(
                text,
                scrollX,
                y,
                textPaint
            )

//            // 左渐隐层
//            shaderPaint.shader = leftLinearGradient
//            canvas.drawRect(
//                paddingStart,
//                0F,
//                shaderWidth + paddingStart,
//                height.toFloat(),
//                shaderPaint
//            )
//
//            // 右渐隐层
//            shaderPaint.shader = rightLinearGradient
//            canvas.drawRect(
//                width - shaderWidth - paddingEnd,
//                0F,
//                width.toFloat() - paddingEnd,
//                height.toFloat(),
//                shaderPaint
//            )
            // 应用左遮罩：使用 DST_IN 模式裁剪文字边缘
            maskPaint.shader = leftMaskShader
            canvas.drawRect(
                paddingStart,
                0F,
                shaderWidth + paddingStart,
                height.toFloat(),
                maskPaint
            )

            // 应用右遮罩
            maskPaint.shader = rightMaskShader
            canvas.drawRect(
                width - shaderWidth - paddingEnd,
                0F,
                width.toFloat() - paddingEnd,
                height.toFloat(),
                maskPaint
            )

            canvas.restoreToCount(saveCount)

        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
//        if (visibility == VISIBLE) {
//            startAnimation()
//        } else {
//            stopAnimation()
//        }
    }

    // 文字滚动完毕的时机（通过最后一个字的位置来判定）。0：最后一个字进入屏幕时；0.5：最后一个字到达屏幕中央时；1：最后一个字离开屏幕时。
    var finishPositionOfLastWord = 0.5F

    fun showText(textNew: String) {
        stopAnimation()
        post {
            scrollX = width - paddingEnd
        }
        isShowFinish = false
        frameCount = 0F
        allFrameCount = 0F
        loopCount = 0
        setText(textNew)
        startAnimation()
    }

    fun setText(textNew: String) {
        text = textNew
        postInvalidate()
    }

    fun getText(): String {
        return if (this::text.isInitialized)
            text
        else
            ""
    }

    private lateinit var frameCallback: FrameCallback
    private var isAnimationRunning = AtomicBoolean(false)

    var lastFrameTimeNanos = 0L
    fun startAnimation() {
//        Log.d(id.toString(), "$text startAnimation")
        isAnimationRunning.set(true)

        if (!this::frameCallback.isInitialized) {
            frameCallback = object : FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isAnimationRunning.get()) {
                        Choreographer.getInstance().removeFrameCallback(frameCallback)
                        return
                    }

                    Choreographer.getInstance().postFrameCallback(this)

                    // 计算实际时间增量（秒）
                    val deltaSeconds = if (lastFrameTimeNanos == 0L) {
                        0.0
                    } else {
                        (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0
                    }

                    // 限制最大时间增量，防止应用切到后台再返回时出现大幅跳跃
                    val clampedDeltaSeconds = deltaSeconds.coerceAtMost(0.05) // 最大50ms

                    lastFrameTimeNanos = frameTimeNanos

                    // 文字宽度超出屏幕时（滚动）
                    if (textPaint.measureText(text) > width - paddingStart - paddingEnd) {
                        // 使用实际时间增量更新滚动位置（单位：像素/秒）
                        scrollX -= (pixelMovePerSecond * clampedDeltaSeconds).toFloat()

                        postInvalidate()

                        // 文字滚动完毕时
                        if (scrollX < -textPaint.measureText(text) + width * finishPositionOfLastWord &&
                            allFrameCount / 1.0 * 1000 > minShowTimeMs  // 此处改用实际经过时间
                        ) {
                            isShowFinish = true
                        } else if (loopCount == 0) {
                            isShowFinish = false
                        }

                        if (scrollX < -textPaint.measureText(text) + width * finishPositionOfLastWord * 0.95) {
                            frameCount = 0F
                            scrollX = width.toFloat() - paddingEnd
                            loopCount++
                        }
                    }
                    // 文字宽度不足以超出屏幕时（静止）
                    else if (textPaint.measureText(text) <= width) {
                        isShowFinish = if (allFrameCount / 1.0 * 1000 > minShowTimeMs) {
                            true
                        } else {
                            false
                        }
                    }

                    // 使用实际时间增量累计
                    frameCount += clampedDeltaSeconds.toFloat()
                    allFrameCount += clampedDeltaSeconds.toFloat()
                }
            }
        }

        lastFrameTimeNanos = 0L  // 重置，让第一帧计算delta为0
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stopAnimation() {
//        Log.d(id.toString(), "$text stopAnimation")
        isAnimationRunning.set(false)
        if (this::frameCallback.isInitialized) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

}