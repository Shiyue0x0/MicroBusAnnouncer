package com.microbus.announcer.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microbus.announcer.R
import com.microbus.announcer.ScrollEventBus
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.LineAdapter
import com.microbus.announcer.bean.Line
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.DialogInputBinding
import com.microbus.announcer.databinding.DialogLineInfoBinding
import com.microbus.announcer.databinding.FragmentLineBinding
import com.microbus.announcer.ui.compose.NavHelpBox
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home


class LineFragment : Fragment() {

    private var tag = javaClass.simpleName

    private lateinit var binding: FragmentLineBinding

    private lateinit var stationDatabaseHelper: StationDatabaseHelper

    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var alertBinding: DialogLineInfoBinding

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        //获取ViewBinding
        binding = FragmentLineBinding.inflate(inflater, container, false)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(requireContext())
        lineDatabaseHelper = LineDatabaseHelper.getInstance(requireContext())

        //获取Utils
        utils = Utils(requireContext())

        binding.TopAppBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    SmallTopAppBar(
                        title = getString(R.string.nav_line),
                        navigationIcon = {
                            IconButton(onClick = {
                                val intent = Intent()
                                    .setAction(utils.backHomeName)
                                LocalBroadcastManager.getInstance(context)
                                    .sendBroadcast(intent)
                            }) {
                                Icon(MiuixIcons.Home, contentDescription = "返回主控")
                            }
                        },
                        actions = {
                            NavHelpBox(
                                listOf(
                                    "轻触路线名称：运行该路线",
                                    "轻触起点/终点站：切换上下/行",
                                    "长按路线：编辑路线",
                                    "轻触站点：试听报站",
                                    "*缓慢向左/右滑动查看完整途径站点",
                                )
                            )
                        }
                    )
                }
            }
        }

        refreshLineList("")
        initSwipeRefreshLayout()

        //添加点击添加站点事件
        binding.addLineFab.setOnClickListener {
            utils.haptic(binding.addLineFab)
            addLine()
        }

        startCollectingScrollEvents()

        return binding.root
    }

    private lateinit var adapter: LineAdapter
    private var hasFirstOnScrolled = false

    /**
     * 刷新路线列表
     */
    private fun refreshLineList(searchText: String) {

        binding.lineRecyclerView.setHasFixedSize(true)
        //获取所有路线，加载到界面
        adapter = LineAdapter(
            requireContext(),
            requireActivity(),
            lineDatabaseHelper,
            showStationPoint = false
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

        binding.lineRecyclerView.setAdapter(adapter)

        binding.lineRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!hasFirstOnScrolled) {
                    hasFirstOnScrolled = true
                    return
                }

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                adapter.updateItemShown(firstVisibleItem, lastVisibleItem)
//                Log.d("L190", "${firstVisibleItem} ${lastVisibleItem}")
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

                                lineNameDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener {
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

                                        utils.onSubmitLineDialog(alertBinding, "new", null) {
                                            refreshLineList("")
                                            alertDialog.cancel()
                                        }
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
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshLineList("")
            @SuppressLint("NotifyDataSetChanged")
            binding.lineRecyclerView.adapter!!.notifyDataSetChanged()
            binding.swipeRefreshLayout.isRefreshing = false
            utils.showMsg("刷新成功")
            utils.haptic(binding.swipeRefreshLayout)
        }
    }

    // 与用户交互时
    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")
        adapter.updateAllItemShown(true)
    }

    // 不再与用户交互时
    override fun onPause() {
        Log.d(tag, "onPause")
        adapter.updateAllItemShown(false)
        super.onPause()
    }

    private var scrollEventJob: Job? = null
    private fun startCollectingScrollEvents() {
        scrollEventJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ScrollEventBus.subscribeToAction(ScrollEventBus.lineListScrollToTopActionName)
                    .collect { _ ->
                        binding.lineRecyclerView.scrollToPosition(0)
                    }
            }
        }
    }

}