package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.LineAdapter
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.databinding.DialogLineInfoBinding
import com.microbus.announcer.databinding.FragmentLineBinding


class LineFragment : Fragment() {

    private var tag = javaClass.simpleName

    private var binding: FragmentLineBinding? = null

    private lateinit var stationDatabaseHelper: StationDatabaseHelper

    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var alertBinding: DialogLineInfoBinding

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
    }

    @SuppressLint("InflateParams", "InternalInsetResource", "DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        //获取ViewBinding
        binding = FragmentLineBinding.inflate(inflater, container, false)
        stationDatabaseHelper = StationDatabaseHelper(context)
        lineDatabaseHelper = LineDatabaseHelper(context)

        //获取Utils
        utils = Utils(requireContext())

        //设置Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding!!.toolbar)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding!!.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        //binding!!.lineRecyclerView.itemAnimator = null
        refreshLineList()
        initSwipeRefreshLayout()

        //添加点击添加站点事件
        binding!!.addLineFab.setOnClickListener {
            utils.haptic(binding!!.addLineFab)
            addLine()
        }
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    lateinit var adapter: LineAdapter

    /**
     * 刷新路线列表
     */
    private fun refreshLineList() {

        binding!!.lineRecyclerView.setHasFixedSize(true)
        //获取所有路线，加载到界面
        adapter = LineAdapter(
            requireContext(),
            lineDatabaseHelper
        )

        //点击路线切换到主控并运行
        adapter.setOnItemClickListener(object : LineAdapter.OnItemClickListener {
            override fun onItemClick(line: Line, position: Int) {
                if (position == 0)
                    return

                val intent = Intent()
                    .setAction(utils.switchLineActionName)
                    .putExtra("id", line.id)
                LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(intent)

            }
        })

        binding!!.lineRecyclerView.setAdapter(adapter)

        binding!!.lineRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

//                Log.d("", "line now show ${firstVisibleItem}-${lastVisibleItem}")
//                adapter.firstVisibleItem = firstVisibleItem
//                adapter.lastVisibleItem = lastVisibleItem
                adapter.updateItemShown(firstVisibleItem, lastVisibleItem)

            }
        })


        @SuppressLint("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()

    }

    private fun addLine() {

//        val adapter = ArrayAdapter<String>(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            arrayOf("true", "false")
//        )
//        alertBinding.editTextIsUpAndDownInvert.setAdapter(adapter)

        val stationPreparedDialog =
            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                .setTitle("新增路线")
                .setMessage("新增路线前，请确保该路线途径的站点已添加完毕。")
                .setPositiveButton("已全部添加，继续", null)
                .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
                .show()


        stationPreparedDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            stationPreparedDialog.dismiss()
            val modeList = listOf("在地图上添加（推荐）", "通过站点ID添加")
            val modeSelectDialog =
                MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                    .setTitle("要使用哪种方式添加路线站点？")
                    .setSingleChoiceItems(
                        modeList.toTypedArray(), -1
                    ) { dialog, which ->
                        dialog.dismiss()
                        when (which) {
                            0 -> {
                                val binding =
                                    DialogInputBinding.inflate(LayoutInflater.from(context))
                                val lineNameDialog = MaterialAlertDialogBuilder(
                                    requireContext(),
                                    R.style.CustomAlertDialogStyle
                                )
                                    .setTitle("设置路线名称").setView(binding.root)
                                    .setNegativeButton(getString(android.R.string.cancel), null)
                                    .setPositiveButton("确定", null)
                                    .show()

                                lineNameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                    val lineName = binding.editText.text.toString()
                                    if (lineName == "") {
                                        utils.showMsg("请输入路线名称")
                                        return@setOnClickListener
                                    }
                                    lineNameDialog.dismiss()

                                    val intent = Intent()
                                        .setAction(utils.editLineOnMapActionName)
                                        .putExtra("id", -1)
                                        .putExtra("name", lineName)   //名称
                                        .putExtra("direction", 0)   //上行
                                        .putExtra("type", "new")   //新增
                                    LocalBroadcastManager.getInstance(requireContext())
                                        .sendBroadcast(intent)
                                }
                            }

                            1 -> {
                                alertBinding =
                                    DialogLineInfoBinding.inflate(LayoutInflater.from(context))

                                val alertDialog: AlertDialog? =
                                    MaterialAlertDialogBuilder(
                                        requireContext(),
                                        R.style.CustomAlertDialogStyle
                                    )
                                        .setView(alertBinding.root)
                                        .setTitle("新增路线")
                                        .setPositiveButton("提交", null)
                                        .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
                                        .show()

                                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
                                    ?.setOnClickListener {


                                        val name = alertBinding.editTextName.text.toString()
                                        var upLineStation =
                                            alertBinding.editTextUpLineStation.text.toString()
                                        var downLineStation =
                                            alertBinding.editTextDownLineStation.text.toString()
//            val isUpAndDownInvert = alertBinding.editTextIsUpAndDownInvert.isChecked

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
                                            upLineStation =
                                                downLineStationList.reversed().joinToString(" ")
                                        }

                                        if (downLineStation == "") {
                                            val upLineStationList = upLineStation.split(' ')
                                            downLineStation =
                                                upLineStationList.reversed().joinToString(" ")
                                        }

                                        //查找是否输入了不存在的站点
                                        val upLineStationList = upLineStation.split(' ')
                                        val downLineStationList = downLineStation.split(' ')
                                        var stationList: List<Station>
                                        for (stationIdStr in upLineStationList) {
                                            stationList =
                                                stationDatabaseHelper.queryById(stationIdStr.toInt())
                                            if (stationList.isEmpty()) {
                                                utils.showMsg("上行站点 $stationIdStr 不存在")
                                                return@setOnClickListener
                                            }
                                        }
                                        for (stationIdStr in downLineStationList) {
                                            stationList =
                                                stationDatabaseHelper.queryById(stationIdStr.toInt())
                                            if (stationList.isEmpty()) {
                                                utils.showMsg("下行站点 $stationIdStr 不存在")
                                                return@setOnClickListener
                                            }
                                        }

                                        val line =
                                            Line(null, name, upLineStation, downLineStation, true)
                                        lineDatabaseHelper.insert(line)
                                        refreshLineList()
                                        alertDialog.cancel()
                                    }
                            }
                        }
                    }
                    .show()
        }

    }


    /**
     * 初始化下拉刷新控件 SwipeRefreshLayout
     */
    private fun initSwipeRefreshLayout() {
        binding!!.swipeRefreshLayout.setOnRefreshListener {
            //refreshLineList()
            @SuppressLint("NotifyDataSetChanged")
            binding!!.lineRecyclerView.adapter!!.notifyDataSetChanged()
            binding!!.swipeRefreshLayout.isRefreshing = false
            utils.showMsg("刷新成功")
            utils.haptic(binding!!.swipeRefreshLayout)
        }
    }

//    // 与用户交互时
//    override fun onResume() {
//        super.onResume()
//        Log.d(tag, "onResume")
//        adapter.updateAllItemShown(true)
//    }
//
//    // 不再与用户交互时
//    override fun onPause() {
//        Log.d(tag, "onPause")
//        adapter.updateAllItemShown(false)
//        super.onPause()
//    }

    fun updateAllItemShown(value: Boolean) {
        if (this::adapter.isInitialized)
            adapter.updateAllItemShown(value)
    }

}