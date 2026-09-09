package com.microbus.announcer.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.StationAdapter
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.FragmentStationBinding
import com.microbus.announcer.ui.compose.NavHelpBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home


class StationFragment : Fragment() {

    private lateinit var binding: FragmentStationBinding

    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var utils: Utils

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        //获取ViewBinding
        binding = FragmentStationBinding.inflate(inflater, container, false)
        stationDatabaseHelper = StationDatabaseHelper.getInstance(requireContext())
        lineDatabaseHelper = LineDatabaseHelper.getInstance(requireContext())

        //获取Utils
        utils = Utils(requireContext())

        // 设置 TopAppBar
        binding.TopAppBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    SmallTopAppBar(
                        title = getString(R.string.nav_station),
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
                                    "轻触站点：试听报站",
                                    "长按站点：编辑站点",
                                )
                            )
                        }
                    )

                    var searchText by remember { mutableStateOf("") }

                    SearchBar(
                        expanded = false,
                        onExpandedChange = {  },
                        inputField = {
                            InputField(
                                query = searchText,
                                expanded = false,
                                onExpandedChange = { },
                                label = "通过 站点ID 或 中英文名 搜索",
                                onQueryChange = {
                                    searchText = it
                                    if (searchText == "") {
                                        refreshStationList(searchText)
                                    }
                                },
                                onSearch = {
                                    searchText = it
                                    refreshStationList(searchText)
                                },

                                )
                        }
                    ) {
                    }
                }
            }
        }

        refreshStationList("")
        initSwipeRefreshLayout()

        //添加点击添加站点事件
        binding.addStationFab.setOnClickListener {
            utils.haptic(binding.addStationFab)
            addStation()
        }


        val mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (isAdded) {
                    when (intent.action) {
                        utils.stationListScrollToTopActionName -> {
                            binding.stationRecyclerView.scrollToPosition(0)
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(utils.stationListScrollToTopActionName)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, intentFilter)

        return binding.root
    }

    private lateinit var adapter: StationAdapter

    /**
     * 刷新站点列表
     */
    private fun refreshStationList(key: String) {

        if (!::adapter.isInitialized) {

            //获取站点，加载到界面
            adapter = StationAdapter(
                requireContext(),
                requireActivity(),
                lineDatabaseHelper,
                key
            )

            val layoutManager = LinearLayoutManager(requireContext())
            layoutManager.initialPrefetchItemCount = 10
            binding.stationRecyclerView.setLayoutManager(layoutManager)

            binding.stationRecyclerView.setAdapter(adapter)
            adapter.setOnItemClickListener(object : StationAdapter.OnItemClickListener {
                override fun onItemClick(station: Station) {

                    if (station.id == null || station.id!! <= 0) {
                        return
                    }

                    val intent = Intent()
                        .setAction(utils.tryListeningAnActionName)
                        .putExtra("format", "<mscn${station.id}>|<msen${station.id}>")
                    LocalBroadcastManager.getInstance(requireContext())
                        .sendBroadcast(intent)
                }
            })
        } else {
            adapter.updateSearchKey(key)
        }


    }

    private fun addStation() {
        utils.showStationDialog(requireActivity(), "new", onAddDone = {
            val adapter = binding.stationRecyclerView.adapter!!
            adapter.notifyItemInserted(adapter.itemCount)
        })
    }


    /**
     * 初始化下拉刷新控件 SwipeRefreshLayout
     */
    private fun initSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            @SuppressLint("NotifyDataSetChanged")
            binding.stationRecyclerView.adapter!!.notifyDataSetChanged()
            requireActivity().runOnUiThread {
                binding.swipeRefreshLayout.isRefreshing = false
            }
            utils.showMsg("刷新成功")
            utils.haptic(binding.swipeRefreshLayout)
        }
    }


}