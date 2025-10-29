package com.microbus.announcer.view


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.withStyledAttributes
import com.microbus.announcer.R
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
class ESView : View {

    private lateinit var paint: Paint
    private lateinit var shaderPaint: Paint
    private lateinit var backgroundPaint: Paint
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
        paint = Paint()
        paint.textSize = textSize
        paint.color = textColor

        val typeface =
            if (fontFamily == "")
                Typeface.DEFAULT
            else
                context.resources.getFont(R.font.galano_grotesque_bold)
        paint.setTypeface(Typeface.create(typeface, textStyle))

        shaderPaint = Paint()

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.setColor(background)

    }

    lateinit var leftLinearGradient: LinearGradient
    lateinit var rightLinearGradient: LinearGradient
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
                    (paint.measureText(text) + paddingStart + paddingEnd).toInt(),
                    MeasureSpec.EXACTLY
                )


        if (paint.measureText(text).toInt() > maxWidth.toInt())
            myMeasuredWidth = MeasureSpec.makeMeasureSpec(
                (maxWidth + paddingStart + paddingEnd).toInt(),
                MeasureSpec.EXACTLY
            )


        val fm = paint.getFontMetrics()

        val myMeasuredHeight =
            MeasureSpec.makeMeasureSpec(
                (fm.bottom - fm.top + paddingTop + paddingBottom).toInt(),
                MeasureSpec.EXACTLY
            )

        setMeasuredDimension(myMeasuredWidth, myMeasuredHeight)

        leftLinearGradient = LinearGradient(
            paddingLeft.toFloat(),
            0f,
            shaderWidth + paddingLeft.toFloat(),
            0f,
            background,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        rightLinearGradient = LinearGradient(
            measuredWidth - shaderWidth - paddingEnd,
            0f,
            measuredWidth.toFloat() - paddingEnd,
            0f,
            Color.TRANSPARENT,
            background,
            Shader.TileMode.CLAMP
        )

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

    }

    var frameCount = 0
    var allFrameCount = 0
    var minShowTimeMs = Int.MAX_VALUE
    var fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display.refreshRate
    } else {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.refreshRate
    }
    var pixelMovePerSecond = 150
    var isShowFinish = false
    var scrollX = Float.MAX_VALUE
    val shaderWidth = 20f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fm = paint.getFontMetrics()
        val y = height / 2 + (fm.bottom - fm.top) / 2 - fm.bottom

        // 绘制背景色
        canvas.clipPath(path)
        canvas.drawColor(backgroundPaint.color)

        // 绘制文字和渐隐层
        canvas.clipRect(fillRect)

        // View宽度足够容纳文本，居中显示
        if (paint.measureText(text) <= width) {
            canvas.drawText(
                text,
                (width - paint.measureText(text)) / 2,
                y,
                paint
            )
        }
        // View宽度不足够容纳文本，轮播显示，羽化水平边缘
        else {
            scrollX -= pixelMovePerSecond.toFloat() / fps
            canvas.drawText(
                text,
                scrollX,
                y,
                paint
            )

            // 左渐隐层
            shaderPaint.setShader(leftLinearGradient)
            canvas.drawRect(
                paddingStart,
                0F,
                shaderWidth + paddingStart,
                height.toFloat(),
                shaderPaint
            )

            // 右渐隐层
            shaderPaint.setShader(rightLinearGradient)
            canvas.drawRect(
                width - shaderWidth - paddingEnd,
                0F,
                width.toFloat() - paddingEnd,
                height.toFloat(),
                shaderPaint
            )

        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    // 文字滚动完毕的时机（通过最后一个字的位置来判定）。0：最后一个字进入屏幕时；0.5：最后一个字到达屏幕中央时；1：最后一个字离开屏幕时。
    var finishPositionOfLastWord = 0.5F

    fun showText(textNew: String) {

        stopAnimation()

        post {
            scrollX = width - paddingEnd
        }

        isShowFinish = false

        frameCount = 0
        allFrameCount = 0
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

    var lastFrameTimeNanos = 0L
    fun startAnimation() {
        if (!this::frameCallback.isInitialized) {
            frameCallback = object : FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {

                    Choreographer.getInstance().postFrameCallback(this)

                    val frameDelayNanos = frameTimeNanos - lastFrameTimeNanos
//                    Log.d("ES", "$text frameTimeNanos: $frameDelayNanos")
                    lastFrameTimeNanos = frameTimeNanos

                    // 动态刷新率
                    fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.display.refreshRate
                    } else {
                        val windowManager =
                            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        windowManager.defaultDisplay.refreshRate
                    }

//                    Log.d("ES", "$text $isShowFinish")

                    //  文字宽度超出屏幕时（滚动）
                    if (paint.measureText(text) > width) {
                        postInvalidate()

                        // 文字滚动完毕时
                        if (scrollX < -paint.measureText(text) + width * finishPositionOfLastWord &&
                            allFrameCount / fps * 1000 > minShowTimeMs
                        ) {
                            isShowFinish = true
                        } else if (loopCount == 0) {
                            isShowFinish = false
                        }

                        if (scrollX < -paint.measureText(text) + width * finishPositionOfLastWord * 0.95) {
                            frameCount = 0
                            scrollX = width.toFloat() - paddingEnd
                            loopCount++
                        }

                    }
                    // 文字宽度不足以超出屏幕时（静止）
                    else if (paint.measureText(text) <= width) {
//                        Log.d("ES", "$text ${allFrameCount / fps * 1000}/${minShowTimeMs}")
                        isShowFinish = if (allFrameCount / fps * 1000 > minShowTimeMs) {
                            true
                        } else {
                            false
                        }
                    }

                    frameCount = (frameCount + 1) % Int.MAX_VALUE
                    allFrameCount = (allFrameCount + 1) % Int.MAX_VALUE

                }
            }

        }
        Choreographer.getInstance().postFrameCallback(frameCallback)

    }

    fun stopAnimation() {
        if (this::frameCallback.isInitialized) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

}