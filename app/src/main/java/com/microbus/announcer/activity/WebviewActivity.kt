package com.microbus.announcer.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.microbus.announcer.BaseActivity
import com.microbus.announcer.Utils
import com.microbus.announcer.databinding.ActivityWebviewBinding

class WebviewActivity : BaseActivity() {

    var tag: String = javaClass.simpleName

    lateinit var binding: ActivityWebviewBinding

    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        utils = Utils(this)

        binding = ActivityWebviewBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setContentView(binding.root)

        @SuppressLint("SetJavaScriptEnabled")
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.defaultTextEncodingName = "utf-8"

        binding.webview.setWebViewClient(object : WebViewClient() {
        })

        val uriStr = intent.getStringExtra("uriStr")


        if (uriStr != null) {
            binding.webview.loadUrl(uriStr)
        }

    }
}