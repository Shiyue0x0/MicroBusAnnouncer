package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.microbus.announcer.MainActivity
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.LineAdapter
import com.microbus.announcer.bean.Line
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogLineInfoBinding
import com.microbus.announcer.databinding.FragmentLineBinding


class LineFragment : Fragment() {

    private var tag = javaClass.simpleName

    private var binding: FragmentLineBinding? = null

    private lateinit var stationDatabaseHelper: StationDatabaseHelper

    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var alertBinding: AlertDialogLineInfoBinding

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
    }

    @SuppressLint("InflateParams")
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

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        requireActivity().menuInflater.inflate(R.menu.line_toolbar, menu)
//        Log.d(tag, "Line: onCreateOptionsMenu")
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val itemId = item.itemId
//        if (itemId == R.id.addLine) {
//            addLine()
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * 刷新路线列表
     */
    private fun refreshLineList() {
        //获取所有路线，加载到界面
        val adapter = LineAdapter(
            requireContext(),
            lineDatabaseHelper
        )
        binding!!.lineRecyclerView.setAdapter(adapter)
        //点击路线切换到主控并运行
        adapter.setOnItemClickListener(object : LineAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, line: Line) {
                val activity = requireActivity() as MainActivity
                activity.binding.viewPager.currentItem = 0
                val mainFragment = activity.fragmentList[0] as MainFragment
                mainFragment.initLine(line)
            }

        })
    }

    private fun addLine() {
        alertBinding = AlertDialogLineInfoBinding.inflate(LayoutInflater.from(context))

//        val adapter = ArrayAdapter<String>(
//            requireContext(),
//            android.R.layout.simple_dropdown_item_1line,
//            arrayOf("true", "false")
//        )
//        alertBinding.editTextIsUpAndDownInvert.setAdapter(adapter)

        val alertDialog: AlertDialog? =
            AlertDialog.Builder(requireContext()).setView(alertBinding.root)
                ?.setTitle("添加路线")
                ?.setPositiveButton("提交", null)
                ?.setNegativeButton("取消") { _, _ ->
                }?.show()

        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val name = alertBinding.editTextName.text.toString()
            var upLineStation = alertBinding.editTextUpLineStation.text.toString()
            var downLineStation = alertBinding.editTextDownLineStation.text.toString()
            val isUpAndDownInvert = alertBinding.editTextIsUpAndDownInvert.isChecked

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
                stationList = stationDatabaseHelper.quertById(stationIdStr.toInt())
                if (stationList.isEmpty()) {
                    utils.showMsg("上行站点 $stationIdStr 不存在")
                    return@setOnClickListener
                }
            }
            for (stationIdStr in downLineStationList) {
                stationList = stationDatabaseHelper.quertById(stationIdStr.toInt())
                if (stationList.isEmpty()) {
                    utils.showMsg("下行站点 $stationIdStr 不存在")
                    return@setOnClickListener
                }
            }

            val line = Line(null, name, upLineStation, downLineStation, isUpAndDownInvert)
            lineDatabaseHelper.insert(line)
            refreshLineList()
            alertDialog.cancel()
        }
    }


    /**
     * 初始化下拉刷新控件 SwipeRefreshLayout
     */
    private fun initSwipeRefreshLayout() {
        binding!!.swipeRefreshLayout.setOnRefreshListener {
            //refreshLineList()
            binding!!.lineRecyclerView.adapter!!.notifyDataSetChanged()
            binding!!.swipeRefreshLayout.isRefreshing = false
            utils.haptic(binding!!.swipeRefreshLayout)
        }
    }
}