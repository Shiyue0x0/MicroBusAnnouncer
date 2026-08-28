package com.microbus.announcer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogLineInfoBinding
import com.microbus.announcer.databinding.ItemLineBinding
import com.microbus.announcer.databinding.ItemLineHeaderBinding

class LineAdapter(
    private val context: Context,
    private val activity: Activity,
    private val lineDatabaseHelper: LineDatabaseHelper,
) :
    RecyclerView.Adapter<ViewHolder>() {

    private lateinit var mClickListener: OnItemClickListener
    private var stationDatabaseHelper = StationDatabaseHelper(context)

//    private var stationOfLineAdapterList = ArrayList<StationOfLineAdapter>()

    private var stationOfLineAdapterMap = HashMap<Int, StationOfLineAdapter>()

    val commonView = 0
    val headerView = 1

    var allLineList: MutableList<Line>

    val utils = Utils(context)


//    var firstVisibleItem = -1
//
//    var lastVisibleItem = -1

    init {
        setHasStableIds(true)
        val comparator = utils.getDefaultLineComparator()
        allLineList = ArrayList(lineDatabaseHelper.queryAll().sortedWith(comparator))
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
        var lineStationListHost = binding.lineStationListHost

        init {
            mListener = clickListener
            lineName.setOnClickListener(this)


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

    val lineWithDirectionMap = HashMap<Int, Int>()  //<lineId, Direction(0 or 1)>

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {

        // LineViewHolder
        if (position == 0) {
            val holder = holder as LineHeaderViewHolder
            holder.title.text = "本地路线 ${allLineList.size}条"
        }
        // ItemLineHeaderHolder
        else {
            val holder = holder as LineViewHolder
            val position = position - 1
            holder.line = lineDatabaseHelper.queryById(allLineList[position].id ?: -1).first()
            holder.lineName.text = holder.line.name

            lineWithDirectionMap[holder.line.id ?: -1] = 0

            //获取路线站点信息
            val stationList = ArrayList<Station>()
            val upStationStrIndexList = holder.line.upLineStation.split(" ").toMutableList()
            for (i in upStationStrIndexList.indices) {
                val stationRes = stationDatabaseHelper.queryById(upStationStrIndexList[i].toInt())
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


            if (stationList.isNotEmpty()) {
                holder.lineStartingStation.text = stationList.first().cnName
                holder.lineTerminal.text = stationList.last().cnName
            } else {
                holder.lineStartingStation.text = "未知站点"
                holder.lineTerminal.text = "未知站点"
            }

//            val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(context) {
//                override fun canScrollHorizontally(): Boolean {
//                    return true
//                }
//
//                override fun canScrollVertically(): Boolean {
//                    return false
//                }
//
//            }
//
//            linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL


            val linearLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            holder.lineStationList.setHasFixedSize(true)
            holder.lineStationList.layoutManager = linearLayoutManager
            val stationOfLineAdapter =
                StationOfLineAdapter(context, activity, stationList, stationList.size)

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
                    val adapter = holder.lineStationList.adapter as StationOfLineAdapter
                    val station = adapter.stationList[position]
                    utils.showMsg("${station.cnName}[${station.id}]\n${station.enName}")
                    utils.haptic(holder.lineStationList)

                    val intent = Intent()
                        .setAction(utils.tryListeningAnActionName)
                        .putExtra("format", "<mscn${station.id}>|<msen${station.id}>")
                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(intent)

                }
            })

            // 长按编辑路线
            holder.lineCard.setOnLongClickListener {
                showEditLineDialog(holder, position, utils)
                return@setOnLongClickListener true
            }
            holder.lineStartingStation.setOnLongClickListener {
                showEditLineDialog(holder, position, utils)
                return@setOnLongClickListener true
            }
            holder.lineName.setOnLongClickListener {
                showEditLineDialog(holder, position, utils)
                return@setOnLongClickListener true
            }
            holder.lineTerminal.setOnLongClickListener {
                showEditLineDialog(holder, position, utils)
                return@setOnLongClickListener true
            }

            holder.lineStartingStation.setOnClickListener {
                switchDirection(holder)
            }
            holder.lineTerminal.setOnClickListener {
                switchDirection(holder)
            }


        }
    }

    /*
    * 切换上下行
    * */
    fun switchDirection(holder: LineViewHolder) {

        if (holder.line.isRingRoute) {
            utils.showMsg("该路线为环线", true)
            return
        }

        val stationList = ArrayList<Station>()
        val stationStr = when (lineWithDirectionMap[holder.line.id!!]) {
            0 -> holder.line.downLineStation
            1 -> holder.line.upLineStation
            else -> ""
        }
        val upStationStrIndexList = stationStr.split(" ").toMutableList()
        for (i in upStationStrIndexList.indices) {
            val stationRes = stationDatabaseHelper.queryById(upStationStrIndexList[i].toInt())
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

        if (stationList.isNotEmpty()) {
            holder.lineStartingStation.text = stationList.first().cnName
            holder.lineTerminal.text = stationList.last().cnName
        } else {
            holder.lineStartingStation.text = "未知站点"
            holder.lineTerminal.text = "未知站点"
        }

        val adapter = holder.lineStationList.adapter as StationOfLineAdapter
        adapter.stationList = stationList
        @SuppressLint("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()

        // 滚动到起点站
        val manager = holder.lineStationList.layoutManager as LinearLayoutManager
        manager.scrollToPositionWithOffset(0, 0)

        lineWithDirectionMap[holder.line.id!!] = when (lineWithDirectionMap[holder.line.id!!]) {
            0 -> 1
            1 -> 0
            else -> 1
        }

        utils.showMsg(
            when (lineWithDirectionMap[holder.line.id!!]) {
                0 -> "已切换到上行"
                1 -> "已切换到下行"
                else -> "切换错误"
            },true
        )

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
            stationOfLineAdapterMap[i]?.isShown = i in firstVisibleItem..lastVisibleItem
        }
    }

    fun updateAllItemShown(value: Boolean) {
        for (i in 1 until itemCount) {
            stationOfLineAdapterMap[i]?.isShown = value
        }
    }

    fun showEditLineDialog(holder: LineViewHolder, position: Int, utils: Utils) {
        val binding = DialogLineInfoBinding.inflate(LayoutInflater.from(context))

        binding.editTextName.setText(holder.line.name)
        binding.editTextUpLineStation.setText(holder.line.upLineStation)
        binding.editTextDownLineStation.setText(holder.line.downLineStation)
        binding.editTextType.setText(holder.line.type)
        binding.editIsRingRoute.isChecked = holder.line.isRingRoute
//            binding.editTextIsUpAndDownInvert.isChecked = line.isUpAndDownInvert

        val alertDialog =
            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                .setView(binding.root)
                .setTitle("更新路线")
                .setPositiveButton("提交", null)
                .setNeutralButton("删除路线") { _, _ ->
                    lineDatabaseHelper.delById(holder.line.id ?: -1)
                    notifyItemRemoved(position)
                }
                .setNegativeButton("到地图编辑") { _, _ ->

                    val upStationList = holder.line.upLineStation.split(" ")
                    val upStartingStationCnName =
                        stationDatabaseHelper.queryById(upStationList.first().toInt())
                            .first().cnName
                    val upTerminalStationCnName =
                        stationDatabaseHelper.queryById(upStationList.last().toInt()).first().cnName

                    val downStationList = holder.line.downLineStation.split(" ")
                    val downStartingStationCnName =
                        stationDatabaseHelper.queryById(downStationList.first().toInt())
                            .first().cnName
                    val downTerminalStationCnName =
                        stationDatabaseHelper.queryById(downStationList.last().toInt())
                            .first().cnName

                    MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogStyle)
                        .setTitle("选择要编辑的方向")
                        .setMessage("上行：${upStartingStationCnName} - ${upTerminalStationCnName}\n下行：${downStartingStationCnName} - $downTerminalStationCnName")
                        .setNeutralButton(context.getString(android.R.string.cancel), null)
                        .setNegativeButton("上行") { _, _ ->
                            val intent = Intent()
                                .setAction(utils.editLineOnMapActionName)
                                .putExtra("id", holder.line.id)
                                .putExtra("direction", 0)   //上行
                            LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intent)
                        }
                        .setPositiveButton("下行") { _, _ ->
                            val intent = Intent()
                                .setAction(utils.editLineOnMapActionName)
                                .putExtra("id", holder.line.id)
                                .putExtra("direction", 1)   //下行
                            LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intent)
                        }
                        .show()
                }
                .show()


        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {

            utils.onSubmitLineDialog(binding, "update", holder.line.id) {
                notifyItemChanged(position + 1)
                alertDialog.cancel()
            }

        }

        utils.haptic(holder.lineCard)
    }

}