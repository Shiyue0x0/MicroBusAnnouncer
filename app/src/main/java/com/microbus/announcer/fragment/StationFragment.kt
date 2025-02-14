package com.microbus.announcer.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.FOCUSABLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.microbus.announcer.R
import com.microbus.announcer.Utils
import com.microbus.announcer.adapter.StationAdapter
import com.microbus.announcer.bean.Station
import com.microbus.announcer.database.LineDatabaseHelper
import com.microbus.announcer.database.StationDatabaseHelper
import com.microbus.announcer.databinding.AlertDialogStationInfoBinding
import com.microbus.announcer.databinding.FragmentStationBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class StationFragment : Fragment(), AMapLocationListener {

    private var tag = javaClass.simpleName

    private var binding: FragmentStationBinding? = null

    private lateinit var stationDatabaseHelper: StationDatabaseHelper
    private lateinit var lineDatabaseHelper: LineDatabaseHelper

    private lateinit var mLocationClient: AMapLocationClient
    private lateinit var mLocationOption: AMapLocationClientOption

    private lateinit var alertBinding: AlertDialogStationInfoBinding

    private lateinit var utils: Utils

    @SuppressLint("InflateParams")
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

                searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH;

                searchView.setIconifiedByDefault(true);

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        val stationList = stationDatabaseHelper.quertByKey(query!!)
                        refreshStationList(stationList)
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return true
                    }

                })

                searchView.setOnCloseListener {
                    val stationList = stationDatabaseHelper.quertAll()
                    refreshStationList(stationList)
                    false
                }

            }


            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return true
            }

        })

        val stationList = stationDatabaseHelper.quertAll()
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
        binding!!.stationRecyclerView.setAdapter(
            StationAdapter(
                requireContext(),
                stationList,
                stationDatabaseHelper,
                lineDatabaseHelper
            )
        )
    }

    private fun addStation() {
        alertBinding = AlertDialogStationInfoBinding.inflate(LayoutInflater.from(context))

        val alertDialog: AlertDialog? =
            AlertDialog.Builder(requireContext()).setView(alertBinding.root)
                ?.setTitle("添加站点")
                ?.setNeutralButton("获取当前位置", null)?.setPositiveButton("提交", null)
                ?.setNegativeButton("取消") { _, _ ->

                }?.show()

        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val cnName = alertBinding.editTextCnName.text.toString()
            var enName = alertBinding.editTextEnName.text.toString()

            if (cnName == "") {
                utils.showMsg("请填写中文名称")
                return@setOnClickListener
            }

            if (enName == "") {
                enName = cnName
            }

            if (alertBinding.editTextLongitude.text.toString() == "") {
                utils.showMsg("请填写经度")
                return@setOnClickListener
            }

            val longitudeRegex = Regex("(\\d+(\\.\\d+)?) ([0-9]+(\\.\\d+)?)?")
            if (!alertBinding.editTextLongitude.text.matches(longitudeRegex)) {
                utils.showMsg("经度格式错误")
                return@setOnClickListener
            }


            if (alertBinding.editTextLatitude.text.toString() == "") {
                if (!alertBinding.editTextLongitude.text.matches(longitudeRegex)) {
                    utils.showMsg("请填写经度")
                    return@setOnClickListener
                }
                val latLng = alertBinding.editTextLongitude.text.toString().split(' ')
                alertBinding.editTextLongitude.setText(latLng[0])
                alertBinding.editTextLatitude.setText(latLng[1])
            }

            val latitudeRegex = Regex("\\d+(\\.\\d+)?")
            if (!alertBinding.editTextLatitude.text.matches(latitudeRegex)) {
                utils.showMsg("纬度格式错误")
                return@setOnClickListener
            }

            val longitude: Double = alertBinding.editTextLongitude.text.toString().toDouble()
            val latitude: Double = alertBinding.editTextLatitude.text.toString().toDouble()

            val station = Station(null, cnName, enName, longitude, latitude)
            stationDatabaseHelper.insert(station)

            val stationList = stationDatabaseHelper.quertAll()
            refreshStationList(stationList)

            alertDialog.cancel()
        }

        alertDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            initLocation()
            mLocationClient.startLocation()
            object : CountDownTimer(4 * 1000, 10000) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    mLocationClient.stopLocation()
                }
            }.start()
        }
    }


    /**
     * 初始化下拉刷新控件 SwipeRefreshLayout
     */
    private fun initSwipeRefreshLayout() {
        binding!!.swipeRefreshLayout.setOnRefreshListener {
            binding!!.stationRecyclerView.adapter!!.notifyDataSetChanged()
            GlobalScope.launch { binding!!.swipeRefreshLayout.isRefreshing = false
            }
            utils.haptic(binding!!.swipeRefreshLayout)
        }
    }

    /**
     * 初始化定位
     */
    private fun initLocation() {
        //初始化定位
        try {
            mLocationClient = AMapLocationClient(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //设置定位回调监听
        mLocationClient.setLocationListener(this)
        //初始化AMapLocationClientOption对象
        mLocationOption = AMapLocationClientOption()
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true)
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true)
        //设置定位请求超时时间，单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(30000)
        //关闭缓存机制，高精度定位会产生缓存。
        mLocationOption.setLocationCacheEnable(false)
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption)
    }

    /**
     * 接收异步返回的定位结果
     *
     * @param aMapLocation
     */
    override fun onLocationChanged(aMapLocation: AMapLocation?) {
        if (aMapLocation != null) {
            if (aMapLocation.errorCode == 0) {
                alertBinding.editTextLongitude.setText(aMapLocation.longitude.toString())
                alertBinding.editTextLatitude.setText(aMapLocation.latitude.toString())

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e(
                    "AmapError",
                    "location Error, ErrCode:" + aMapLocation.errorCode + ", errInfo:" + aMapLocation.errorInfo
                )
            }
        }
    }

}