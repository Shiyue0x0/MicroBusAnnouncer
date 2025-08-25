package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogStationInfoBinding
import com.microbus.announcer.databinding.ItemStationBinding
import java.util.Locale

internal class StationAdapter(
    private val context: Context,
    private val stationList: List<Station>,
    private val stationDatabaseHelper: StationDatabaseHelper,
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
        var station = Station()
        var stationCard = binding.stationCard
        var stationId = binding.stationId
        var stationType = binding.stationType
        var stationCnName = binding.stationCnName
        var stationEnName = binding.stationEnName
        var stationLongitude = binding.stationLongitude
        var stationLatitude = binding.stationLatitude
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
        holder.stationId.text = String.format(Locale.ROOT, "%03d", station.id)
        holder.stationType.text = station.type
        holder.stationCnName.text = station.cnName
        holder.stationEnName.text = station.enName
        holder.stationLongitude.text = "${station.longitude}"
        holder.stationLatitude.text = "${station.latitude}"

        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.stationLineList.layoutManager = linearLayoutManager
        holder.stationLineList.adapter =
            station.id?.let { LineOfStationAdapter(context, it, lineDatabaseHelper) }

        holder.stationCard.setOnLongClickListener {

            utils.showStationDialog("update", station)
            notifyDataSetChanged()

            return@setOnLongClickListener true

//            val binding = AlertDialogStationInfoBinding.inflate(LayoutInflater.from(context))
//
//            binding.editTextCnName.setText(station.cnName)
//            binding.editTextEnName.setText(station.enName)
//            binding.editTextType.setText(station.type)
//            binding.editTextLongitude.setText(station.longitude.toString())
//            binding.editTextLatitude.setText(station.latitude.toString())
//
//            val alertDialog =
//                AlertDialog.Builder(context).setView(binding.root)!!
//                    .setTitle("编辑站点")
//                    .setPositiveButton("更新", null)
//                    .setNeutralButton("删除") { _, _ ->
//                        stationDatabaseHelper.delById(station.id!!)
//                        notifyItemRemoved(position)
//                    }
//                    .setNegativeButton("取消") { _, _ ->
//
//                    }
//                    .show()
//
//
//            alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
//                val cnName = binding.editTextCnName.text.toString()
//                val enName = binding.editTextEnName.text.toString()
//                val type = binding.editTextType.text.toString()
//
//                if (cnName == "") {
//                    utils.showMsg("请填写中文名称")
//                    return@setOnClickListener
//                }
//
//                if (enName == "") {
//                    utils.showMsg("请填写英文名称")
//                    return@setOnClickListener
//                }
//
//                if (binding.editTextLongitude.text.toString() == "") {
//                    utils.showMsg("请填写经度")
//                    return@setOnClickListener
//                }
//
//                if (binding.editTextLatitude.text.toString() == "") {
//                    val latLng = binding.editTextLongitude.text.toString().split(' ')
//                    binding.editTextLongitude.setText(latLng[0])
//                    binding.editTextLatitude.setText(latLng[1])
//                }
//
//                val longitude: Double = binding.editTextLongitude.text.toString().toDouble()
//                val latitude: Double = binding.editTextLatitude.text.toString().toDouble()
//
//                val stationUpdated = Station(null, cnName, enName, longitude, latitude, type)
//                stationDatabaseHelper.updateById(station.id!!, stationUpdated)
//                notifyItemChanged(position)
//                alertDialog.cancel()
//            }
//            utils.haptic(holder.stationCard)
//            return@setOnLongClickListener true
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