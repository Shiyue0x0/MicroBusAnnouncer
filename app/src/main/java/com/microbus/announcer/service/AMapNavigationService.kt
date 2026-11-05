package com.microbus.announcer.service

import android.content.Context
import com.amap.api.maps.model.LatLng

class AMapNavigationService private constructor(context: Context){

    private val appContext: Context = context.applicationContext

    fun startNavigation(locations: List<LatLng>) {
        if (locations.isEmpty()) {
            return
        }


    }
}