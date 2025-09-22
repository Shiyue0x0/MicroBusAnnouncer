package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    mLineName: String,
    mStationState: Int = -1     // onNext 0, onWillArrive 1, onArrive, 2
) :
    RecyclerView.Adapter<StationOfLineAdapter.StationOfLineViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    val mHandler = Handler(Looper.getMainLooper())

    var isScroll = false

    var lineHeight = 0

    var stationList = mStationList

    var stationCount = mStationCount
    var lineName = mLineName
    var stationState = mStationState

    val utils = Utils(context)

    internal class StationOfLineViewHolder(
        binding: ItemStationOfLineBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mClickListener: OnItemClickListener? = null // 声明自定义监听接口
        var stationIndex = binding.stationIndex
        var stationNameNestedScrollView = binding.stationNameNestedScrollView
        var stationName = binding.stationName
        var main = binding.main
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

        holder.stationNameNestedScrollView.layoutParams.width = (lineHeight * 1.1).toInt()
        holder.stationNameNestedScrollView.layoutParams.height =
            lineHeight * 4 + utils.dp2px(2F) * 2
        //设置站点名称竖直滚动
        val runnable = object : Runnable {
            var timeCount = 0
            override fun run() {

                mHandler.postDelayed(this, 25)


//                if (holder.stationName.text.length <= 4) {
//                    timeCount = 0
//                    return
//                }

                if (!isScroll)
                    return

//                Log.d("runnable", lineName)
                val scrollY = holder.stationNameNestedScrollView.scrollY
                val maxScrollY =
                    holder.stationNameNestedScrollView.getChildAt(0).height - holder.stationNameNestedScrollView.height
                if (scrollY >= maxScrollY) {
                    timeCount++
                    if (timeCount > 50) {
                        timeCount = 0
                        holder.stationNameNestedScrollView.scrollBy(0, -maxScrollY)
                    }
                } else if (scrollY == 0) {
                    timeCount++
                    if (timeCount > 50) {
                        timeCount = 0
                        holder.stationNameNestedScrollView.smoothScrollBy(0, 1)
                    }
                } else {
                    holder.stationNameNestedScrollView.smoothScrollBy(0, 1)
                }

            }
        }

        holder.stationNameNestedScrollView.post {
            mHandler.postDelayed(runnable, 25)
        }


        return holder
    }

    @SuppressLint("SetTextI18n", "ResourceType")
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


        holder.stationName.text = stationList[position].cnName

//        val typedArray = context.obtainStyledAttributes(
//            intArrayOf(
//                android.R.attr.colorPrimary,
//                com.google.android.material.R.attr.colorSurface,
//                android.R.attr.colorPrimary,
//                com.google.android.material.R.attr.colorOnSurface,
//            )
//        )
//        val bg2 = typedArray.getColor(0, 0)
//        val color2 = typedArray.getColor(1, 0)
//        val color1 = typedArray.getColor(2, 0)
//        val color3 = typedArray.getColor(3, 0)
//        typedArray.recycle()

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
        holder.constraintLayout.setPadding(padding, utils.dp2px(2F), padding, utils.dp2px(2F))

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