package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
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


internal class StationAdapter(
    private val context: Context,
    private val stationList: List<Station>,
    private val stationDatabaseHelper: StationDatabaseHelper,
    private val lineDatabaseHelper: LineDatabaseHelper
) :
    RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    internal class StationViewHolder(binding: ItemStationBinding) :
        ViewHolder(binding.root) {
        var stationCard = binding.stationCard
        var stationId = binding.stationId
        var stationCnName = binding.stationCnName
        var stationEnName = binding.stationEnName
        var stationLongitude = binding.stationLongitude
        var stationLatitude = binding.stationLatitude
        var stationLineList = binding.stationLineList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemStationBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return StationViewHolder(binding)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val utils = Utils(context)

        val station = stationList[position]

        holder.stationId.text = String.format("%03d", station.id)
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
            val binding = AlertDialogStationInfoBinding.inflate(LayoutInflater.from(context))

            binding.editTextCnName.setText(station.cnName)
            binding.editTextCnName.setText(station.cnName)
            binding.editTextEnName.setText(station.enName)
            binding.editTextLongitude.setText(station.longitude.toString())
            binding.editTextLatitude.setText(station.latitude.toString())

            val alertDialog: AlertDialog? = context.let { it1 ->
                AlertDialog.Builder(it1).setView(binding.root)
                    ?.setTitle("编辑站点")
                    ?.setPositiveButton("更新", null)
                    ?.setNeutralButton("删除") { _, _ ->
                        stationDatabaseHelper.delById(station.id!!)
                        notifyItemRemoved(position)
                    }
                    ?.setNegativeButton("取消") { _, _ ->

                    }
                    ?.show()
            }

            alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val cnName = binding.editTextCnName.text.toString()
                val enName = binding.editTextEnName.text.toString()

                if (cnName == "") {
                    utils.showMsg("请填写中文名称")
                    return@setOnClickListener
                }

                if (enName == "") {
                    utils.showMsg("请填写英文名称")
                    return@setOnClickListener
                }

                if (binding.editTextLongitude.text.toString() == "") {
                    utils.showMsg("请填写经度")
                    return@setOnClickListener
                }

                if (binding.editTextLatitude.text.toString() == "") {
                    val latLng = binding.editTextLongitude.text.toString().split(' ')
                    binding.editTextLongitude.setText(latLng[0])
                    binding.editTextLatitude.setText(latLng[1])
                }

                val longitude: Double = binding.editTextLongitude.text.toString().toDouble()
                val latitude: Double = binding.editTextLatitude.text.toString().toDouble()

                val stationUpdated = Station(null, cnName, enName, longitude, latitude)
                stationDatabaseHelper.updateById(station.id!!, stationUpdated)
                notifyItemChanged(position)
                alertDialog.cancel()
            }
            utils.haptic(holder.stationCard)
            return@setOnLongClickListener true
        }

    }

    override fun getItemCount(): Int {
        return stationList.size
    }

}