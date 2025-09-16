package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.databinding.ItemStationBinding
import java.util.Locale

internal class StationAdapter(
    private val context: Context,
    private val stationList: List<Station>,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    internal class StationViewHolder(
        binding: ItemStationBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
        var stationCard = binding.stationCard
        var stationId = binding.stationId
        var stationType = binding.stationType
        var stationCnName = binding.stationCnName
        var stationEnName = binding.stationEnName

        //        var stationLongitude = binding.stationLongitude
//        var stationLatitude = binding.stationLatitude
        var stationLineList = binding.stationLineList

        init {
            mListener = clickListener
            stationCard.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(station)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemStationBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return StationViewHolder(binding, mClickListener)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val utils = Utils(context)

        val station = stationList[position]

        holder.station = station
        holder.stationId.text = station.id.toString()
//        holder.stationId.text = String.format(Locale.ROOT, "%03d", station.id)
        holder.stationType.text = station.type
        holder.stationCnName.text = station.cnName
        holder.stationEnName.text = station.enName
//        holder.stationLongitude.text = "${station.longitude}"
//        holder.stationLatitude.text = "${station.latitude}"

        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.stationLineList.setHasFixedSize(true)
        holder.stationLineList.layoutManager = linearLayoutManager
        holder.stationLineList.adapter =
            station.id?.let { LineOfStationAdapter(it, lineDatabaseHelper) }

        holder.stationCard.setOnLongClickListener {

            utils.showStationDialog("update", station)
            notifyDataSetChanged()

            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return stationList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    interface OnItemClickListener {
        fun onItemClick(station: Station)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            this.mClickListener = listener
        }
    }
}