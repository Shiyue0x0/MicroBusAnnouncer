package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.R
import com.microbus.announcer.bean.Station
import com.microbus.announcer.databinding.ItemStationOfLineBinding
import java.util.Locale
import kotlin.math.ceil


internal class StationOfLineAdapter(
    private val context: Context,
    private val stationList: ArrayList<Station>,
    private val currentLineStationCount: Int,
    private val lineName: String
) :
    RecyclerView.Adapter<StationOfLineAdapter.StationOfLineViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    val mHandler = Handler(Looper.getMainLooper())

    var isScroll = true

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
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StationOfLineViewHolder, position: Int) {
        when (position) {
            0 -> holder.stationIndex.text = "始发"
            stationList.size - 1 -> holder.stationIndex.text = "终到"
            else -> holder.stationIndex.text = String.format(Locale.CHINA, "%02d", position + 1)
        }

        holder.stationName.text = stationList[position].cnName

        val paint = Paint()
        paint.textSize = holder.stationIndex.textSize
        val fm = paint.fontMetrics
        val lineHeight = ceil((fm.descent - fm.ascent).toDouble()).toInt()
        val mainLayoutParams = holder.stationNameNestedScrollView.layoutParams
        mainLayoutParams.height = lineHeight * 4
        mainLayoutParams.width = lineHeight
        holder.stationNameNestedScrollView.layoutParams = mainLayoutParams

        //当前站点样式
        val color: Int
        val style: Int
        val bg: Int
        val padding: Int
        val dp2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            2F, Resources.getSystem().displayMetrics).toInt()
        if (position < currentLineStationCount || currentLineStationCount == -1) {
            color = context.getColor(R.color.textColor3)
            style = Typeface.NORMAL
            bg = context.getColor(android.R.color.transparent)
            padding = 0
        } else if (position == currentLineStationCount) {
            com.google.android.material.R.styleable.CardView_cardBackgroundColor
            color = holder.main.cardBackgroundColor.defaultColor
            style = Typeface.BOLD
            bg = context.getColor(R.color.textColor2)
            padding = dp2
        } else {
            color = context.getColor(R.color.textColor2)
            style = Typeface.NORMAL
            bg = context.getColor(android.R.color.transparent)
            padding = 0
        }
        holder.stationIndex.setTextColor(color)
        holder.stationName.setTextColor(color)
        holder.stationIndex.setTypeface(null, style)
        holder.stationName.setTypeface(null, style)
        holder.main.setCardBackgroundColor(bg)
        holder.constraintLayout.setPadding(padding, dp2, padding, dp2)

        when (holder.stationName.text.length) {
            2 -> {
                holder.stationName.setLineSpacing(lineHeight * 2f, 1f)
            }

            3 -> {
                holder.stationName.setLineSpacing(lineHeight * 0.5f, 1f)
            }

            else -> {
                holder.stationName.setLineSpacing(0f, 1f)
            }
        }


        //设置站点名称竖直滚动
        val runnable = object : Runnable {
            var timeCount = 0
            override fun run() {

                mHandler.postDelayed(this, 25)

                if(!isScroll)
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