package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.FOCUSABLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
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


class StationFragment : Fragment() {

    private var binding: FragmentStationBinding? = null

    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var utils: Utils

    @SuppressLint("InflateParams", "DiscouragedApi", "InternalInsetResource")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        //获取ViewBinding
        binding = FragmentStationBinding.inflate(inflater, container, false)
        stationDatabaseHelper = StationDatabaseHelper(context)
        lineDatabaseHelper = LineDatabaseHelper(context)

        //获取Utils
        utils = Utils(requireContext())

        //设置Toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding!!.toolbar)

        //设置状态栏填充高度
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        binding!!.bar.layoutParams.height = resources.getDimensionPixelSize(resourceId)

        //获取当前Fragment的Activity，并转换为MenuHost
        val menuHost: MenuHost = requireActivity()
        //添加MenuProvider
        menuHost.addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                //创建Menu
                menuInflater.inflate(R.menu.menu_search, menu)

                //配置搜索框
                val menuItem = menu.findItem(R.id.action_search) //根据菜单项ID获取

                //获取搜索框
                val searchView = menuItem.actionView as SearchView

                searchView.focusable = FOCUSABLE

                //设置提示字符串
                searchView.setQueryHint("ID、中文或英文名称")

                searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH

                searchView.setIconifiedByDefault(true)

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        val stationList = stationDatabaseHelper.queryByKey(query!!)
                        refreshStationList(stationList)
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return true
                    }

                })

                searchView.setOnCloseListener {
                    val stationList = stationDatabaseHelper.queryAll()
                    refreshStationList(stationList)
                    false
                }

            }


            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return true
            }

        })

        val stationList = stationDatabaseHelper.queryAll()
        refreshStationList(stationList)
        initSwipeRefreshLayout()

        //添加点击添加站点事件
        binding!!.addStationFab.setOnClickListener {
            utils.haptic(binding!!.addStationFab)
            addStation()
        }


        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


    /**
     * 刷新站点列表
     */
    private fun refreshStationList(stationList: List<Station>) {

        //获取所有站点，加载到界面
        val adapter = StationAdapter(
            requireContext(),
            stationList,
            lineDatabaseHelper
        )

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.initialPrefetchItemCount = 10
        binding!!.stationRecyclerView.setLayoutManager(layoutManager)

        binding!!.stationRecyclerView.setAdapter(adapter)
        adapter.setOnItemClickListener(object : StationAdapter.OnItemClickListener {
            override fun onItemClick(station: Station) {
                val intent = Intent()
                    .setAction(utils.tryListeningAnActionName)
                    .putExtra("format", "<mscn${station.id}>|<msen${station.id}>")
                LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(intent)
            }
        })

    }

    private fun addStation() {
        utils.showStationDialog("new", stationFragment = this, onDone = {
            @SuppressLint("NotifyDataSetChanged")
            binding!!.stationRecyclerView.adapter!!.notifyDataSetChanged()
        })
    }


    /**
     * 初始化下拉刷新控件 SwipeRefreshLayout
     */
    private fun initSwipeRefreshLayout() {
        binding!!.swipeRefreshLayout.setOnRefreshListener {
            @SuppressLint("NotifyDataSetChanged")
            binding!!.stationRecyclerView.adapter!!.notifyDataSetChanged()
            requireActivity().runOnUiThread {
                binding!!.swipeRefreshLayout.isRefreshing = false
            }
            utils.showMsg("刷新成功")
            utils.haptic(binding!!.swipeRefreshLayout)
        }
    }


}