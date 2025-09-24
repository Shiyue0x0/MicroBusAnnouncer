package com.microbus.announcer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions
import androidx.core.net.toUri


class PermissionManager(private val context: Context, private val activity: Activity) : Fragment() {

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
            return true
        } else {
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

//    /**
//     * 请求权限结果
//     * @param requestCode
//     * @param permissions
//     * @param grantResults
//     */
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        //设置权限请求结果
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//        when (requestCode) {
//            PermissionManager.REQUEST_LOCATION -> {
//                Log.d(tag, "per2 code: REQUEST_LOCATION")
//            }
//
//            PermissionManager.REQUEST_MANAGE_FILES_ACCESS -> {
//                Log.d(tag, "per2 code: REQUEST_MANAGE_FILES_ACCESS")
//            }
//        }
//
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_MANAGE_FILES_ACCESS) {
//            if (resultCode == Activity.RESULT_OK) {
//                utils.showMsg("允许访问所有文件")
//            } else {
//                utils.showMsg("不允许访问所有文件")
//            }
//        }
//    }
}