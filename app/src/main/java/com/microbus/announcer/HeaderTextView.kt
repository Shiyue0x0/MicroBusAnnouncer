package com.microbus.announcer


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
class HeaderTextView : View {

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
        }
//        Log.d(tag, "onDraw: $text")
//        Log.d(tag, "onDraw: $textSize")
//        Log.d(tag, "onDraw: $maxWidth")
    }


    private fun initPaint() {
        paint = Paint()
        paint.textSize = textSize
        paint.color = textColor
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, textStyle))

        shaderPaint = Paint()
        shaderPaint.isAntiAlias = true

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.setColor(background)

    }

    lateinit var leftLinearGradient: LinearGradient
    lateinit var rightLinearGradient: LinearGradient
    val fillRect = RectF()

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

//        Log.d(tag, "onMeasure")
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

        val myMeasuredHeight =
            MeasureSpec.makeMeasureSpec(
                (paint.getFontMetrics().bottom - paint.getFontMetrics().top + paddingTop + paddingBottom).toInt(),
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


    }


    var frameCount = 0
    var showTimeMs = Int.MAX_VALUE
    val fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display.refreshRate
    } else {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.refreshRate
    }
    val speedPixelPerSecond = 100
    var isShowFinish = false
    var scrollX = 0F
    val shaderWidth = 20f


    //    val repeat = 0 //重复轮播时两端文本的水平间隔，为文本框宽度的比例。0表示无间隔，1表示间隔为文本框宽度
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        fillRect.top = 0F
        fillRect.bottom = height.toFloat()
        fillRect.left = 0F
        fillRect.right = width.toFloat()
        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, backgroundPaint)

        // View宽度足够容纳文本，居中显示
        if (paint.measureText(text) <= width) {
            val padding = (width - paint.measureText(text)) / 2
            canvas.drawText(text, padding, textSize + paddingTop, paint)
        }
        // View宽度不足够容纳文本，轮播显示，羽化水平边缘
        else {
            scrollX = width - (frameCount * speedPixelPerSecond / fps) - paddingEnd
            canvas.drawText(text, scrollX, textSize + paddingTop, paint)

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

            canvas.drawRect(
                0F,
                cornerRadius,
                paddingStart,
                height.toFloat() - cornerRadius,
                backgroundPaint
            )

            canvas.drawArc(
                0F,
                0F,
                cornerRadius * 2,
                cornerRadius * 2,
                180F,
                90F,
                true,
                backgroundPaint
            )

            canvas.drawArc(
                0F,
                height - cornerRadius * 2,
                cornerRadius * 2,
                height.toFloat(),
                90F,
                90F,
                true,
                backgroundPaint
            )

            canvas.drawRect(
                width - paddingEnd,
                cornerRadius,
                width.toFloat(),
                height.toFloat() - cornerRadius,
                backgroundPaint
            )

            canvas.drawArc(
                width - cornerRadius * 2,
                0F,
                width.toFloat(),
                cornerRadius * 2,
                0F,
                -90F,
                true,
                backgroundPaint
            )

            canvas.drawArc(
                width - cornerRadius * 2,
                height - cornerRadius * 2,
                width.toFloat(),
                height.toFloat(),
                0F,
                90F,
                true,
                backgroundPaint
            )

        }

    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {

            if (paint.measureText(text) > width)
                invalidate()

//            if (!isShowFinish) {
            if (paint.measureText(text) <= width) {
                if (frameCount / fps * 1000 > showTimeMs) {
//                    Log.d(tag, "showFinish $text")
                    isShowFinish = true
                    if (this@HeaderTextView::scrollFinishCallback.isInitialized)
                        scrollFinishCallback.onScrollFinish()
                }
            } else {
                if (scrollX < -paint.measureText(text) + width / 2) {
//                        Log.d(tag, "showFinish $text")
                    isShowFinish = true
                    if (this@HeaderTextView::scrollFinishCallback.isInitialized)
                        scrollFinishCallback.onScrollFinish()
                }
                if (scrollX < -paint.measureText(text) + width / 3) {
                    frameCount = 0
                }

            }
//            }

            frameCount = (frameCount + 1) % Int.MAX_VALUE

            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(this, (1000 / fps).toLong())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        Log.d(tag, "onAttachedToWindow")
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, 0)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        Log.d(tag, "onDetachedFromWindow")
        handler.removeCallbacksAndMessages(null)
    }

    fun setText(textNew: String) {

        this.text = textNew
        invalidate()

        handler.removeCallbacksAndMessages(null)
        frameCount = 0
        isShowFinish = false
        handler.postDelayed(runnable, 0)

    }

//    /**
//     * 获取滚动一次文本所需的时间（毫秒）
//     * */
//    fun getScrollTimeMs(): Long {
//        return if (paint.measureText(text) > width)
//            ((paint.measureText(text) + width / 2) / speedPixelPerSecond * 1000).toLong()//
//        else
//            0L
//    }

    lateinit var scrollFinishCallback: ScrollFinishCallback

    interface ScrollFinishCallback {
        fun onScrollFinish()
    }


}