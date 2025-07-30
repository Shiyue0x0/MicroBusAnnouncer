package com.microbus.announcer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions
import androidx.core.net.toUri


class PermissionManager(context: Context, private val fragment: Fragment) : Fragment() {

    companion object {
        const val REQUEST_PERMISSIONS = 0
        const val REQUEST_MANAGE_FILES_ACCESS = 1
    }

    private var utils: Utils = Utils(context)

    init {
        requestPermission(context)
    }

    /**
     * 动态请求权限
     */
    private fun requestPermission(context: Context) {
        val permissions = ArrayList<String>()
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT >= 30)
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT >= 33)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        val permissionsArray= permissions.toTypedArray()

        if (EasyPermissions.hasPermissions(context, *permissionsArray)) {
            //true 有权限 开始定位
            utils.showMsg("已获得权限")
        } else {
            //false 无权限
            EasyPermissions.requestPermissions(
                fragment,
                "应用运行需要一些权限",
                REQUEST_PERMISSIONS,
                *permissionsArray
            )
        }
    }

    /**
     * 请求权限结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, fragment)
    }


    /**
     * 申请所有文件访问权限
     */
    fun requestManageFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent =
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            Log.d(tag, context?.packageName.toString())
            intent.setData(("package:" + this::class.java.`package`?.name).toUri())
            //startActivityForResult(intent, REQUEST_MANAGE_FILES_ACCESS)
            fragment.startActivityForResult(intent, REQUEST_MANAGE_FILES_ACCESS)
        } else {
            //非android11及以上版本，走正常申请权限流程
            requestPermission(requireContext())
        }
    }


}