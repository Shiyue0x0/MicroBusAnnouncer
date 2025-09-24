package com.microbus.announcer.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.microbus.announcer.bean.RunningInfo
import com.microbus.announcer.databinding.ItemStationOfRunningInfoBinding
import java.time.format.DateTimeFormatter

class StationOfRunningInfoAdapter(
    private var context: Context,
    private val runningInfoList: ArrayList<RunningInfo>
) :
    RecyclerView.Adapter<StationOfRunningInfoAdapter.ViewHolder>() {

    class StationInfo(
        var lineName: String,
        var stationName: String,
        var stationNext: String,
        var stationWillIn: String,
        var stationIn: String,
        var lineId: Int
    )

    val stationInfoList = ArrayList<StationInfo>()

    init {
        runningInfoList.forEachIndexed { i, info ->

            val currentInfo = runningInfoList[i]
            val lastInfo = if (i > 0) runningInfoList[i - 1] else runningInfoList[i]

            if (i == 0 || currentInfo.stationId != lastInfo.stationId) {
                stationInfoList.add(
                    StationInfo(
                        "${info.lineName} 开往 ${info.terminalName}",
                        info.stationName,
                        "",
                        "",
                        "",
                        info.lineId
                    )
                )
            }

            val timeStr = info.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            when (info.stationState) {
                0 -> stationInfoList.last().stationNext = "$timeStr 从上一站出站"
                1 -> stationInfoList.last().stationWillIn = "$timeStr 即将到达"
                2 -> stationInfoList.last().stationIn = "$timeStr 到达本站"
            }

        }
    }

    class ViewHolder(
        var binding: ItemStationOfRunningInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        var lineName = binding.lineName
        var stationName = binding.stationName
        var stationNext = binding.stationNext
        var stationWillIn = binding.stationWillIn
        var stationIn = binding.stationIn

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationOfRunningInfoBinding
            .inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.lineName.text = stationInfoList[position].lineName
        holder.stationName.text = stationInfoList[position].stationName

        holder.stationNext.text = stationInfoList[position].stationNext
        holder.stationWillIn.text = stationInfoList[position].stationWillIn
        holder.stationIn.text = stationInfoList[position].stationIn

        val stateViewList =
            listOf(holder.stationNext, holder.stationWillIn, holder.stationIn)

        for (stateView in stateViewList) {
            if (stateView.text == "")
                stateView.visibility = ViewGroup.GONE
            else
                stateView.visibility = ViewGroup.VISIBLE
        }

        if(position > 0 && stationInfoList[position].lineName == stationInfoList[position - 1].lineName){
            holder.lineName.visibility = ViewGroup.GONE
        }
        else{
            holder.lineName.visibility = ViewGroup.VISIBLE
        }

    }

    override fun getItemCount(): Int {
        return stationInfoList.size
    }


}