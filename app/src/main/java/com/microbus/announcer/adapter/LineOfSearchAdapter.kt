package com.microbus.announcer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.microbus.announcer.R
import com.microbus.announcer.bean.Line
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.ItemLineOfSearchBinding

class LineOfSearchAdapter(private var context: Context, private val lineList: ArrayList<Line>) :
    RecyclerView.Adapter<LineOfSearchAdapter.ViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    private var stationDatabaseHelper = StationDatabaseHelper(context)


    class ViewHolder(
        var binding: ItemLineOfSearchBinding, clickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var mListener: OnItemClickListener? = null // 声明自定义监听接口

        init {
            mListener = clickListener
            binding.main.setOnClickListener(this)
        }



        var name = binding.name
        var text = binding.text
        var line = Line()

        override fun onClick(v: View?) {
            mListener!!.onItemClick(line)
        }

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineOfSearchBinding
            .inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding, mClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.line = lineList[position]

        val lineStationIndexListStr =
            holder.line.upLineStation.split(' ')

        val lineStartingStation =
            stationDatabaseHelper.queryById(
                lineStationIndexListStr.first().toInt()
            )
        val lineTerminal =
            stationDatabaseHelper.queryById(
                lineStationIndexListStr.last().toInt()
            )

        val lineStartingStationCnName =
            if (lineStartingStation.isNotEmpty()) lineStartingStation.first().cnName
            else "-"

        val lineTerminalCnName =
            if (lineTerminal.isNotEmpty()) lineTerminal.first().cnName
            else "-"


        holder.name.text = lineList[position].name
        holder.text.text = context.getString(R.string.S2T, lineStartingStationCnName, lineTerminalCnName)
    }

    override fun getItemCount() = lineList.size

    interface OnItemClickListener {
        fun onItemClick(line: Line)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            this.mClickListener = listener
        }
    }


}