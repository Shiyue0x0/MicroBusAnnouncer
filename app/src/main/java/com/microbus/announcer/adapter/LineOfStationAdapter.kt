package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.databinding.ItemLineOfStationBinding


internal class LineOfStationAdapter(
    private val stationIndex: Int,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<LineOfStationAdapter.LineOfStationViewHolder>() {

    private lateinit var stationList: ArrayList<String>

    internal class LineOfStationViewHolder(binding: ItemLineOfStationBinding) :
        ViewHolder(binding.root) {
        var lineName = binding.lineName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineOfStationViewHolder {
        val binding = ItemLineOfStationBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return LineOfStationViewHolder(binding)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LineOfStationViewHolder, position: Int) {
        holder.lineName.text = stationList[position]
    }

    override fun getItemCount(): Int {
        stationList = ArrayList()
        var stationCount = 0
        val lineList = lineDatabaseHelper.queryAll()
        val lineStationIndexStrSet = HashSet<String>()
        for (line in lineList) {
            lineStationIndexStrSet.clear()
            lineStationIndexStrSet.addAll(line.upLineStation.split(' '))
            lineStationIndexStrSet.addAll(line.downLineStation.split(' '))
            for (station in lineStationIndexStrSet) {
                if (station == stationIndex.toString()) {
                    stationCount++
                    stationList.add(line.name)
                }
            }
        }
        return stationCount
    }
}