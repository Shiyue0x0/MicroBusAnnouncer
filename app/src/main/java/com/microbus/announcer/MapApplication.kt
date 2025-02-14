package com.microbus.announcer

import android.app.Application
import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings

class MapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val context: Context = this
        //定位隐私政策同意
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
        //地图隐私政策同意
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        //搜索隐私政策同意
        ServiceSettings.updatePrivacyShow(context, true, true)
        ServiceSettings.updatePrivacyAgree(context, true)
    }
}


