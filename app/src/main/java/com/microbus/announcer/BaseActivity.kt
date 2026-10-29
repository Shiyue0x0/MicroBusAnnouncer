package com.microbus.announcer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun attachBaseContext(newBase: Context) {

        utils = Utils(newBase)

        if (utils.getEnableAppDpi()) {
            val config = Configuration(newBase.resources.configuration).apply {
                densityDpi = utils.getAppDpi()
            }
            val wrapped = newBase.createConfigurationContext(config)
            super.attachBaseContext(wrapped)
        } else {
            super.attachBaseContext(newBase)
        }
    }
}