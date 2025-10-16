package com.microbus.announcer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.databinding.ItemLineOfStationBinding

internal class LineOfStationAdapter() :
    RecyclerView.Adapter<LineOfStationAdapter.LineOfStationViewHolder>() {

    var lineNameList = ArrayList<String>()

    internal class LineOfStationViewHolder(binding: ItemLineOfStationBinding) :
        ViewHolder(binding.root) {
        var lineName = binding.lineName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineOfStationViewHolder {
        val binding = ItemLineOfStationBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return LineOfStationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineOfStationViewHolder, position: Int) {
        holder.lineName.text = lineNameList[position]
    }

    override fun getItemCount(): Int {
        return lineNameList.size
    }
}