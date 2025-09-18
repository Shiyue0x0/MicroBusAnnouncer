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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.databinding.ItemLineHeaderBinding

class LineAdapter(
    private val context: Context,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<ViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener
    private var stationDatabaseHelper = StationDatabaseHelper(context)

    private var stationOfLineAdapterList = ArrayList<StationOfLineAdapter>()

    val commonView = 0
    val headerView = 1

    var allLineList = lineDatabaseHelper.quertAll()

    init {
        setHasStableIds(true)
    }

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

//        binding.lineStationListContainer.setScrollView(binding.lineStationList)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(line, position)
        }
    }

    class LineHeaderViewHolder(
        binding: ItemLineHeaderBinding,
        clickListener: OnItemClickListener
    ) :
        ViewHolder(binding.root), View.OnClickListener {
        private var mListener: OnItemClickListener? = null // 声明自定义监听接口
        var line = Line()
        var lineCard = binding.title
        var title = binding.title


        init {
            mListener = clickListener
            lineCard.setOnClickListener(this)
            //        stationDatabaseHelper = StationDatabaseHelper(context)
//        binding.lineStationListContainer.setScrollView(binding.lineStationList)
        }

        override fun onClick(v: View?) {
            mListener!!.onItemClick(line, position)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            headerView -> {
                val binding = ItemLineHeaderBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return LineHeaderViewHolder(binding, mClickListener)
            }

            else -> {
                val binding = ItemLineBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return LineViewHolder(binding, mClickListener)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val utils = Utils(context)


        // LineViewHolder
        if (position == 0) {
            val holder = holder as LineHeaderViewHolder
            holder.title.text = "本地路线：${allLineList.size}条"
        }
        // ItemLineHeaderHolder
        else {

            val holder = holder as LineViewHolder
            val position = position - 1
            val line = allLineList[position]

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
                    utils.showMsg("${station.cnName}[${station.id}]\n${station.enName}")
                    utils.haptic(holder.lineStationList)
                }
            })

            holder.lineCard.setOnLongClickListener {
                val binding = AlertDialogLineInfoBinding.inflate(LayoutInflater.from(context))

                binding.editTextName.setText(line.name)
                binding.editTextUpLineStation.setText(line.upLineStation)
                binding.editTextDownLineStation.setText(line.downLineStation)
//            binding.editTextIsUpAndDownInvert.isChecked = line.isUpAndDownInvert

                val alertDialog: AlertDialog? = context.let { it1 ->
                    MaterialAlertDialogBuilder(it1, R.style.CustomAlertDialogStyle)
                        .setView(binding.root)
                        .setTitle("更新路线")
                        .setPositiveButton("提交", null)
                        .setNeutralButton("删除路线") { _, _ ->
                            line.id?.let { it2 -> lineDatabaseHelper.delById(it2) }
                            notifyItemRemoved(position)
                        }
                        .setNegativeButton(context.getString(android.R.string.cancel)) { _, _ ->

                        }
                        .show()
                }

                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                    val name = binding.editTextName.text.toString()
                    var upLineStation = binding.editTextUpLineStation.text.toString()
                    var downLineStation = binding.editTextDownLineStation.text.toString()
//                val isUpAndDownInvert = binding.editTextIsUpAndDownInvert.isChecked

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
                        Line(line.id, name, upLineStation, downLineStation, false)
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
    }

    override fun getItemCount(): Int {
        return lineDatabaseHelper.quertAll().size + 1
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
        fun onItemClick(line: Line, position: Int)
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