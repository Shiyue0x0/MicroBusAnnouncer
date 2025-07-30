package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.R
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.ItemStationOfLineBinding
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
//    private lateinit var mLongClickListener: OnItemLongClickListener

    val mHandler = Handler(Looper.getMainLooper())

    internal class StationOfLineViewHolder(
        binding: ItemStationOfLineBinding,
        clickListener: OnItemClickListener
//        longClickListener: OnItemLongClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mClickListener: OnItemClickListener? = null // 声明自定义监听接口
//        private var mLongClickListener: OnItemLongClickListener? = null // 声明自定义监听接口
        var id = 0
        var stationIndex = binding.stationIndex
        var stationNameNestedScrollView = binding.stationNameNestedScrollView
        var stationName = binding.stationName

        init {
            mClickListener = clickListener
//            mLongClickListener = longClickListener
            stationNameNestedScrollView.setOnClickListener(this)
            stationName.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mClickListener!!.onItemClick(v, layoutPosition)
        }

//        override fun onLongClick(v: View?): Boolean {
//            mLongClickListener!!.onItemLongClick(v, layoutPosition)
//            return true
//        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationOfLineViewHolder {
        val binding = ItemStationOfLineBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = StationOfLineViewHolder(binding, mClickListener)
//        val holder = StationOfLineViewHolder(binding, mClickListener, mLongClickListener)
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
        when (position) {
            0 -> holder.stationIndex.text = "始发"
            stationIndexList.size - 1 -> holder.stationIndex.text = "终到"
            else -> holder.stationIndex.text = String.format(Locale.CHINA, "%02d", position + 1)
        }

        val station = stationDatabaseHelper.queryById(stationIndexList[position])
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

//    interface OnItemLongClickListener{
//        fun onItemLongClick(view: View?, position: Int)
//    }
//
//    fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
//        if (listener != null) {
//            this.mLongClickListener = listener
//        }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }
//    }

}