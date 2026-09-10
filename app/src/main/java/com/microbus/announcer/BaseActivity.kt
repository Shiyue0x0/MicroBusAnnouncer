package com.microbus.announcer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {

        utils = Utils(newBase)

        if(false){
            super.attachBaseContext(newBase)
        }

        // todo 强制平板 dpi（仅横屏时生效）
        if (newBase.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val config = Configuration(newBase.resources.configuration).apply {
                densityDpi = 320
            }
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}