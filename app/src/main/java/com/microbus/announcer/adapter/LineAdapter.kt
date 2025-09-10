package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogLineInfoBinding
import com.microbus.announcer.databinding.ItemLineBinding
import androidx.core.content.edit
import androidx.transition.ChangeScroll

class LineAdapter(
    private val context: Context,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<LineAdapter.LineViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener
    private lateinit var stationDatabaseHelper: StationDatabaseHelper

    init {
        setHasStableIds(true)
    }

    private var stationOfLineAdapterList = ArrayList<StationOfLineAdapter>()

    class LineViewHolder(
        binding: ItemLineBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var line = Line()
        var lineId = 0
        var lineCard = binding.lineCard
        var lineName = binding.lineName
        var lineStartingStation = binding.lineStartingStation
        var lineTerminal = binding.lineTerminal
        var lineStationList = binding.lineStationList

        init {
            mListener = clickListener
            lineCard.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(line)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemLineBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        stationDatabaseHelper = StationDatabaseHelper(context)
        binding.lineStationListContainer.setScrollView(binding.lineStationList)

        return LineViewHolder(binding, mClickListener)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: LineViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val utils = Utils(context)

        val line = lineDatabaseHelper.quertAll()[position]

        //获取路线起点站、终点站下标
        val stationStrIndexList = line.upLineStation.split(" ").toMutableList()
        val stationList = ArrayList<Station>()
        for (i in stationStrIndexList.indices) {
            val stationRes = stationDatabaseHelper.queryById(stationStrIndexList[i].toInt())
            if (stationRes.isNotEmpty())
                stationList.add(stationRes[0])
        }

        holder.line = line
        holder.lineId = line.id!!
        holder.lineName.text = line.name

        if (stationList.isNotEmpty()) {
            holder.lineStartingStation.text = stationList.first().cnName
            holder.lineTerminal.text = stationList.last().cnName
        } else {
            holder.lineStartingStation.text = "-"
            holder.lineTerminal.text = "-"
        }

        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.lineStationList.setHasFixedSize(true)
        holder.lineStationList.layoutManager = linearLayoutManager
        val stationOfLineAdapter =
            StationOfLineAdapter(context, stationList, -1, holder.lineName.text.toString())
        holder.lineStationList.adapter = stationOfLineAdapter
        stationOfLineAdapterList.add(stationOfLineAdapter)

        //点击站点显示信息
        stationOfLineAdapter.setOnItemClickListener(object :
            StationOfLineAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val station = stationList[position]
                utils.showMsg("${station.id} ${station.cnName}\n${station.enName}")
                utils.haptic(holder.lineStationList)
            }
        })

        holder.lineCard.setOnLongClickListener {
            val binding = AlertDialogLineInfoBinding.inflate(LayoutInflater.from(context))

            binding.editTextName.setText(line.name)
            binding.editTextUpLineStation.setText(line.upLineStation)
            binding.editTextDownLineStation.setText(line.downLineStation)
            binding.editTextIsUpAndDownInvert.isChecked = line.isUpAndDownInvert

            val alertDialog: AlertDialog? = context.let { it1 ->
                AlertDialog.Builder(it1).setView(binding.root)
                    ?.setTitle("编辑路线")
                    ?.setPositiveButton("更新", null)
                    ?.setNeutralButton("删除") { _, _ ->
                        line.id?.let { it2 -> lineDatabaseHelper.delById(it2) }
                        notifyItemRemoved(position)
                    }
                    ?.setNegativeButton("取消") { _, _ ->

                    }
                    ?.show()
            }

            alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val name = binding.editTextName.text.toString()
                var upLineStation = binding.editTextUpLineStation.text.toString()
                var downLineStation = binding.editTextDownLineStation.text.toString()
                val isUpAndDownInvert = binding.editTextIsUpAndDownInvert.isChecked

                if (name == "") {
                    utils.showMsg("请填写路线名称")
                    return@setOnClickListener
                }

                val lineStationRegex = Regex("\\d+ \\d+( \\d+)*")

                if (!upLineStation.matches(lineStationRegex) && !downLineStation.matches(
                        lineStationRegex
                    )
                ) {
                    utils.showMsg("路线站点未填写或格式错误")
                    return@setOnClickListener
                }

                if (upLineStation == "") {
                    val downLineStationList = downLineStation.split(' ')
                    upLineStation = downLineStationList.reversed().joinToString(" ")
                }

                if (downLineStation == "") {
                    val upLineStationList = upLineStation.split(' ')
                    downLineStation = upLineStationList.reversed().joinToString(" ")
                }

                //查找是否输入了不存在的站点
                val upLineStationList = upLineStation.split(' ')
                val downLineStationList = downLineStation.split(' ')
                var stationList: List<Station>
                for (stationIdStr in upLineStationList) {
                    stationList = stationDatabaseHelper.queryById(stationIdStr.toInt())
                    if (stationList.isEmpty()) {
                        utils.showMsg("上行站点 $stationIdStr 不存在")
                        return@setOnClickListener
                    }
                }
                for (stationIdStr in downLineStationList) {
                    stationList = stationDatabaseHelper.queryById(stationIdStr.toInt())
                    if (stationList.isEmpty()) {
                        utils.showMsg("下行站点 $stationIdStr 不存在")
                        return@setOnClickListener
                    }
                }

                val lineUpdated =
                    Line(line.id, name, upLineStation, downLineStation, isUpAndDownInvert)
                lineDatabaseHelper.updateById(line.id!!, lineUpdated)
                notifyItemChanged(position)
                alertDialog.cancel()
            }

            //设置默认路线名称
            binding.setDefaultLine.setOnClickListener {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit { putString("defaultLineName", line.name) }
                alertDialog?.cancel()
            }

            utils.haptic(holder.lineCard)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return lineDatabaseHelper.quertAll().size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    interface OnItemClickListener {
        fun onItemClick(line: Line)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            this.mClickListener = listener
        }
    }

    fun setStationItemsIsScroll(isScroll: Boolean) {
        stationOfLineAdapterList.forEach { adapter ->
            adapter.isScroll = isScroll
        }
    }


}