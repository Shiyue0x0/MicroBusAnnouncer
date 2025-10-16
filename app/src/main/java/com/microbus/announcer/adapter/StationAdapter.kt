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
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.ItemStationBinding
import com.microbus.announcer.databinding.ItemStationHeaderBinding

internal class StationAdapter(
    private val context: Context,
    private val stationList: List<Station>,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<ViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener

    val commonView = 0

    val headerView = 1

    val stationDatabaseHelper = StationDatabaseHelper(context)

    val utils = Utils(context)
    val comparator = utils.getDefaultLineComparator()

    internal class StationViewHolder(
        binding: ItemStationBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var station = Station(-1, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
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

    internal class StationHeaderViewHolder(
        binding: ItemStationHeaderBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var station = Station(null, "MicroBus 欢迎您", "MicroBus", 0.0, 0.0)
        var stationCard = binding.stationCard
        var title = binding.title

        init {
            mListener = clickListener
            stationCard.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(station)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            headerView -> {
                val binding = ItemStationHeaderBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return StationHeaderViewHolder(binding, mClickListener)
            }

            else -> {
                val binding = ItemStationBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                binding.stationLineList.setHasFixedSize(true)
                val linearLayoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.stationLineList.layoutManager = linearLayoutManager
                binding.stationLineList.adapter = LineOfStationAdapter()
                return StationViewHolder(binding, mClickListener)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // LineViewHolder
        if (position == 0) {
            val holder = holder as StationHeaderViewHolder
            holder.title.text = "查找到站点 ${stationList.size}个"
        }
        // ItemLineHeaderHolder
        else {
            val holder = holder as StationViewHolder
            val position = position - 1
            val station = stationDatabaseHelper.queryById(stationList[position].id ?: -1).first()

            holder.station = station
            holder.stationId.text = station.id.toString()
            holder.stationType.text = station.type
            holder.stationCnName.text = station.cnName
            holder.stationEnName.text = station.enName

            val lineNameList = ArrayList<String>()
            val lineList = ArrayList(lineDatabaseHelper.queryAll().sortedWith(comparator))
            val lineStationIndexStrSet = HashSet<String>()
            for (line in lineList) {
                lineStationIndexStrSet.clear()
                lineStationIndexStrSet.addAll(line.upLineStation.split(' '))
                lineStationIndexStrSet.addAll(line.downLineStation.split(' '))
                for (stationId in lineStationIndexStrSet) {
                    if (stationId == station.id.toString()) {
                        lineNameList.add(line.name)
                    }
                }
            }

            val adapter = holder.stationLineList.adapter as LineOfStationAdapter
            adapter.lineNameList = lineNameList
            @SuppressLint("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()

            holder.stationCard.setOnLongClickListener {
                utils.showStationDialog(
                    "update",
                    stationDatabaseHelper.queryById(station.id ?: -1).first(), onDone = {
                        notifyItemChanged(position + 1)
                    }
                )
                return@setOnLongClickListener true
            }

        }
    }

    override fun getItemCount(): Int {
        return stationList.size + 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0)
            headerView
        else
            commonView
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