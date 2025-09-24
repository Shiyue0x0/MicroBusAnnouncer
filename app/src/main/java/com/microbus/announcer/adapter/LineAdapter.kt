package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogLineInfoBinding
import com.microbus.announcer.databinding.ItemLineBinding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

//    private var stationOfLineAdapterList = ArrayList<StationOfLineAdapter>()

    private var stationOfLineAdapterMap = HashMap<Int, StationOfLineAdapter>()

    val commonView = 0
    val headerView = 1

    var allLineList = lineDatabaseHelper.queryAll()

//    var firstVisibleItem = -1
//
//    var lastVisibleItem = -1

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
            mListener!!.onItemClick(line, layoutPosition)
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
            mListener!!.onItemClick(line, layoutPosition)
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
            holder.title.text = "本地路线 ${allLineList.size}条"
        }
        // ItemLineHeaderHolder
        else {


            val holder = holder as LineViewHolder
            val position = position - 1
            val line = lineDatabaseHelper.queryById(allLineList[position].id ?: -1).first()


            //获取路线起点站、终点站下标
            val stationStrIndexList = line.upLineStation.split(" ").toMutableList()
            val stationList = ArrayList<Station>()
            for (i in stationStrIndexList.indices) {
                val stationRes = stationDatabaseHelper.queryById(stationStrIndexList[i].toInt())
                if (stationRes.isNotEmpty())
                    stationList.add(stationRes[0])
                else
                    stationList.add(
                        Station(
                            id = Int.MAX_VALUE,
                            cnName = "未知站点",
                            enName = "unknown"
                        )
                    )
            }

            holder.line = line
            holder.lineName.text = line.name

            if (stationList.isNotEmpty()) {
                holder.lineStartingStation.text = stationList.first().cnName
                holder.lineTerminal.text = stationList.last().cnName
            } else {
                holder.lineStartingStation.text = "未知站点"
                holder.lineTerminal.text = "未知站点"
            }

            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            holder.lineStationList.setHasFixedSize(true)
            holder.lineStationList.layoutManager = linearLayoutManager
            val stationOfLineAdapter =
                StationOfLineAdapter(context, stationList, -1)

            holder.lineStationList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

//                    Log.d("", "station now show ${firstVisibleItem}-${lastVisibleItem}")
                    stationOfLineAdapter.firstVisibleItem = firstVisibleItem
                    stationOfLineAdapter.lastVisibleItem = lastVisibleItem
                }
            })

//            Log.d("", "stationOfLineAdapter ${position + 1} in ${firstVisibleItem}/${lastVisibleItem}")
//            stationOfLineAdapter.isShown = position + 1 in firstVisibleItem..lastVisibleItem

            holder.lineStationList.adapter = stationOfLineAdapter

            stationOfLineAdapterMap[position + 1] = stationOfLineAdapter
//            stationOfLineAdapterList.add(stationOfLineAdapter)

            //点击站点显示信息，并播报中英文
            stationOfLineAdapter.setOnItemClickListener(object :
                StationOfLineAdapter.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {

                    val station = stationList[position]
                    utils.showMsg("${station.cnName}[${station.id}]\n${station.enName}")
                    utils.haptic(holder.lineStationList)

                    val intent = Intent()
                        .setAction(utils.tryListeningAnActionName)
                        .putExtra("format", "<mscn${station.id}>|<msen${station.id}>")
                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(intent)

                }
            })

            holder.lineCard.setOnLongClickListener {
                val binding = DialogLineInfoBinding.inflate(LayoutInflater.from(context))

                binding.editTextName.setText(line.name)
                binding.editTextUpLineStation.setText(line.upLineStation)
                binding.editTextDownLineStation.setText(line.downLineStation)
                binding.editTextType.setText(line.type)
//            binding.editTextIsUpAndDownInvert.isChecked = line.isUpAndDownInvert

                val alertDialog =
                    MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                        .setView(binding.root)
                        .setTitle("更新路线")
                        .setPositiveButton("提交", null)
                        .setNeutralButton("删除路线") { _, _ ->
                            lineDatabaseHelper.delById(line.id ?: -1)
                            notifyItemRemoved(position)
                        }
                        .setNegativeButton("到地图编辑") { _, _ ->

                            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                                .setTitle("选择要编辑的方向")
                                .setNeutralButton(context.getString(android.R.string.cancel), null)
                                .setNegativeButton("上行") { _, _ ->
                                    val intent = Intent()
                                        .setAction(utils.editLineOnMapActionName)
                                        .putExtra("id", line.id)
                                        .putExtra("direction", 0)   //上行
                                    LocalBroadcastManager.getInstance(context)
                                        .sendBroadcast(intent)
                                }
                                .setPositiveButton("下行") { _, _ ->
                                    val intent = Intent()
                                        .setAction(utils.editLineOnMapActionName)
                                        .putExtra("id", line.id)
                                        .putExtra("direction", 1)   //下行
                                    LocalBroadcastManager.getInstance(context)
                                        .sendBroadcast(intent)
                                }
                                .show()
                        }
                        .show()


                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                    val name = binding.editTextName.text.toString()
                    var upLineStation = binding.editTextUpLineStation.text.toString()
                    var downLineStation = binding.editTextDownLineStation.text.toString()
                    val lineType = binding.editTextType.text.toString()
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

                    if (lineType == "") {
                        utils.showMsg("请填写路线类型")
                        return@setOnClickListener
                    }

                    if (!listOf("C", "B", "U", "T").contains(lineType)) {
                        utils.showMsg("路线类型应为CBUT之一")
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
                        Line(line.id, name, upLineStation, downLineStation, false, lineType)
                    lineDatabaseHelper.updateById(line.id!!, lineUpdated)
                    notifyItemChanged(position + 1)
                    alertDialog.cancel()
                }

                utils.haptic(holder.lineCard)
                return@setOnLongClickListener true
            }
        }
    }

    override fun getItemCount(): Int {
        return lineDatabaseHelper.queryAll().size + 1
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

    fun updateItemShown(firstVisibleItem: Int, lastVisibleItem: Int) {
        for (i in 1 until itemCount) {
            stationOfLineAdapterMap[i]?.isShown = i in firstVisibleItem .. lastVisibleItem
        }
    }

    fun updateAllItemShown(value: Boolean){
        for (i in 1 until itemCount) {
            stationOfLineAdapterMap[i]?.isShown = value
        }
    }

}