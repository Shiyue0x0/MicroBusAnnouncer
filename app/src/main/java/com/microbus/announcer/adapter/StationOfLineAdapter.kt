package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.R
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.ItemStationOfLineBinding
import java.util.Collections
import java.util.Locale
import kotlin.math.ceil


internal class StationOfLineAdapter(
    private val context: Context,
    private val stationIndexList: ArrayList<Int>,
    private val stationDatabaseHelper: StationDatabaseHelper,
    private val currentLineStationCount: Int
) :
    RecyclerView.Adapter<StationOfLineAdapter.StationOfLineViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener
    val mHandler = Handler(Looper.getMainLooper())

    internal class StationOfLineViewHolder(
        binding: ItemStationOfLineBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var id = 0
        var main = binding.main
        var stationIndex = binding.stationIndex
        var stationNameNestedScrollView = binding.stationNameNestedScrollView
        var stationName = binding.stationName

        init {
            mListener = clickListener
            stationNameNestedScrollView.setOnClickListener(this)
            stationName.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(v, layoutPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationOfLineViewHolder {
        val binding = ItemStationOfLineBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = StationOfLineViewHolder(binding, mClickListener)
        //设置站点名称竖直滚动
        val runnable = object : Runnable {
            var timeCount = 0
            override fun run() {
//                Log.d("", "positio1n " + position.toString())
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
                } else
                    holder.stationNameNestedScrollView.smoothScrollBy(0, 1)
                mHandler.postDelayed(this, 25)
            }
        }

        holder.stationNameNestedScrollView.post {
            mHandler.postDelayed(runnable, 25)
        }

        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StationOfLineViewHolder, position: Int) {
        Log.d("1", "position $position")
        when (position) {
            0 -> holder.stationIndex.text = "始发"
            stationIndexList.size - 1 -> holder.stationIndex.text = "终到"
            else -> holder.stationIndex.text = String.format(Locale.CHINA, "%02d", position + 1)
        }

        val station = stationDatabaseHelper.quertById(stationIndexList[position])
        holder.stationName.text = if (station.isNotEmpty())
            station.first().cnName
        else
            "未知"

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
        if (position < currentLineStationCount || currentLineStationCount == -1) {
            color = context.getColor(R.color.textColor3)
            style = Typeface.NORMAL
        } else if (position == currentLineStationCount) {
            color = context.getColor(R.color.textColor1)
            style = Typeface.BOLD
        } else {
            color = context.getColor(R.color.textColor2)
            style = Typeface.NORMAL
        }
        holder.stationIndex.setTextColor(color)
        holder.stationName.setTextColor(color)
        holder.stationIndex.setTypeface(null, style)
        holder.stationName.setTypeface(null, style)

        //获取一个字的高度
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

    }

    override fun getItemCount(): Int {
        return stationIndexList.size
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