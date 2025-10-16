package com.microbus.announcer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions
import androidx.core.net.toUri
import pub.devrel.easypermissions.AfterPermissionGranted


class PermissionManager(private val context: Context, private val activity: Activity) {

    companion object {
        const val REQUEST_ALL = 0
        const val REQUEST_MANAGE_FILES_ACCESS = 1
        const val REQUEST_LOCATION = 2
        const val REQUEST_NOTICE = 3
    }

    /**
     * 动态请求权限
     */
    fun requestNormalPermission(): Boolean {

        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT >= 30)
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT >= 33)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        return requestPermission(permissions, REQUEST_ALL)

    }

    fun requestPermission(permissions: List<String>, requestCode: Int): Boolean {
        val permissionsArray = permissions.toTypedArray()
        if (EasyPermissions.hasPermissions(context, *permissionsArray)) {
//            Log.d("requestPermission", "已获得权限")
            return true
        } else {
//            Log.d("requestPermission", "未获得权限")
            EasyPermissions.requestPermissions(
                activity,
                "应用运行需要一些权限",
                requestCode,
                *permissionsArray
            )
            return false
        }
    }

    fun requestLocationPermission() {
        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermission(permissions, REQUEST_LOCATION)
    }


    fun hasLocationPermission(): Boolean {
        val isGrantedCOARSELOCATION = EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val isGrantedFINEELOCATION = EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return (isGrantedCOARSELOCATION || isGrantedFINEELOCATION)
    }

    fun requestNoticePermission(): Boolean {
        if (hasNoticePermission()) {
            return true
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                val noticePermission = Manifest.permission.POST_NOTIFICATIONS
                return requestPermission(listOf(noticePermission), REQUEST_NOTICE)
            } else {
                return true

            }
        }
    }

    fun hasNoticePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val noticePermission = Manifest.permission.POST_NOTIFICATIONS
            return EasyPermissions.hasPermissions(
                context,
                noticePermission
            )
        } else {
            return true
        }
    }

    /**
     * 申请所有文件访问权限
     */
    fun requestManageFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent =
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.setData(("package:" + this::class.java.`package`?.name).toUri())
            activity.startActivityForResult(intent, REQUEST_MANAGE_FILES_ACCESS)
        } else {
            requestNormalPermission()
        }
    }

}