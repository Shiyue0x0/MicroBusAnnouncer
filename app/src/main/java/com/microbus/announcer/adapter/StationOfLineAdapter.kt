package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.databinding.ItemStationOfLineBinding
import java.util.Locale


internal class StationOfLineAdapter(
    private val context: Context,
    mStationList: ArrayList<Station>,
    mStationCount: Int,
    mStationState: Int = -1     // onNext 0, onWillArrive 1, onArrive, 2
) :
    RecyclerView.Adapter<StationOfLineAdapter.StationOfLineViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    val mHandler = Handler(Looper.getMainLooper())

    var isShown = false

    var lineHeight = 0

    var stationList = mStationList

    var stationCount = mStationCount
    var stationState = mStationState

    val utils = Utils(context)

    var firstVisibleItem = -1
    var lastVisibleItem = -1

    internal class StationOfLineViewHolder(
        binding: ItemStationOfLineBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mClickListener: OnItemClickListener? = null // 声明自定义监听接口
        var stationIndex = binding.stationIndex
        var stationNameNestedScrollView = binding.stationNameNestedScrollView
        var stationName = binding.stationName
        var main = binding.itemStationOfLineMain
        var constraintLayout = binding.constraintLayout


        init {
            mClickListener = clickListener
            stationNameNestedScrollView.setOnClickListener(this)
            stationName.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mClickListener!!.onItemClick(v, layoutPosition)
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationOfLineViewHolder {
        val binding = ItemStationOfLineBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = StationOfLineViewHolder(binding, mClickListener)

        lineHeight = holder.stationIndex.lineHeight


        // todo 适配英文

//        if (utils.getUILang() == "zh") {
        holder.stationName.rotation = 0F
        holder.stationName.maxLines = Int.MAX_VALUE
        holder.stationName.ellipsize = TextUtils.TruncateAt.END
        holder.stationNameNestedScrollView.layoutParams.width = (lineHeight * 1.1).toInt()
//        } else {
//            holder.stationName.rotation = 90F
//            holder.stationName.maxLines = 1
//            holder.stationName.ellipsize = TextUtils.TruncateAt.MARQUEE
//            holder.stationNameNestedScrollView.layoutParams.width =
//                lineHeight * 4 + utils.dp2px(2F) * 2
//        }

        holder.stationNameNestedScrollView.layoutParams.height =
            lineHeight * 4 + utils.dp2px(2F) * 2

        holder.stationNameNestedScrollView.post {

            val pixelMovePerSecond = 100
            val frameCallback = object : FrameCallback {

                val delayPixel = pixelMovePerSecond * 3.0
                var frameCount = 0
                var scrollY = -delayPixel

                override fun doFrame(frameTimeNanos: Long) {

                    Choreographer.getInstance().postFrameCallback(this)

                    if (!isShown)
                        return

                    if (holder.layoutPosition !in firstVisibleItem..lastVisibleItem)
                        return

                    // 动态刷新率
                    val fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.display.refreshRate
                    } else {
                        val windowManager =
                            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.refreshRate
                    }

//                    scrollY += ceil((pixelMovePerSecond.toFloat() / fps).toDouble()).toInt()
                    scrollY += (pixelMovePerSecond.toDouble() / fps).toDouble()

//                    Log.d("offset add", "${ceil((pixelMovePerSecond.toFloat() / fps).toDouble()).toInt()}")

                    val maxScrollY =
                        holder.stationNameNestedScrollView.getChildAt(0).height - holder.stationNameNestedScrollView.height

                    if (scrollY > maxScrollY + delayPixel) {
//                        Log.d("Station", "$lineName ${holder.layoutPosition} ${holder.stationName.text}")
                        scrollY = -delayPixel
                    }

                    holder.stationNameNestedScrollView.scrollTo(0, scrollY.toInt())

                    frameCount++

                }
            }
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        @SuppressLint("ClickableViewAccessibility")
        holder.stationNameNestedScrollView.setOnTouchListener { v, event -> true }


        return holder
    }

    override fun onBindViewHolder(holder: StationOfLineViewHolder, position: Int) {


        when (position) {
            0 -> holder.stationIndex.text = "始发"
            stationList.size - 1 -> holder.stationIndex.text = "终到"
            stationCount -> {
                when (stationState) {
                    0 -> holder.stationIndex.text = "→"
                    1 -> holder.stationIndex.text = "↘"
                    2 -> holder.stationIndex.text = "↓"
                }
            }

            else -> {
                holder.stationIndex.text =
                    String.format(Locale.CHINA, "%02d", position + 1)
            }
        }

        // 站点名称

        // todo 适配英文
        holder.stationName.text = if (utils.getUILang() == "zh") {
            stationList[position].cnName
        } else {
            stationList[position].cnName
        }

        //当前站点样式
        val color: Int
        val style: Int
        val bg: Int
        val padding: Int
        if (position < stationCount) {
            color = context.getColor(R.color.an_text_1)
            style = Typeface.NORMAL
            bg = context.getColor(android.R.color.transparent)
            padding = 0
        } else if (position == stationCount) {
            color = context.getColor(R.color.md_theme_onSurface)
            style = Typeface.BOLD
            bg = context.getColor(R.color.md_theme_surface)
            padding = utils.dp2px(2F)
        } else {
            color = context.getColor(R.color.md_theme_onSurface)
            style = Typeface.NORMAL
            bg = context.getColor(android.R.color.transparent)
            padding = 0
        }
        holder.stationIndex.setTextColor(color)
        holder.stationName.setTextColor(color)
        holder.stationIndex.setTypeface(
            context.resources.getFont(R.font.galano_grotesque_bold),
            style
        )
        holder.stationName.setTypeface(
            context.resources.getFont(R.font.galano_grotesque_bold),
            style
        )
        holder.main.setCardBackgroundColor(bg)
        holder.constraintLayout.setPadding(padding, utils.dp2px(8F), padding, utils.dp2px(8F))

//        if (utils.getUILang() == "zh") {
        when (holder.stationName.text.length) {
            2 -> {
                holder.stationName.setLineSpacing(
                    holder.stationIndex.lineHeight * 2f + utils.dp2px(
                        2F
                    ), 1f
                )
            }

            3 -> {
                holder.stationName.setLineSpacing(
                    holder.stationIndex.lineHeight * 0.5f + utils.dp2px(
                        0.5F
                    ), 1f
                )
            }

            else -> {
                holder.stationName.setLineSpacing(0f, 1f)
            }
        }
//        }
//    else {
//            holder.stationName.setLineSpacing(0f, 1f)
//        }

    }

    override fun getItemCount(): Int {
        return stationList.size
    }


    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            this.mClickListener = listener
        }
    }


}