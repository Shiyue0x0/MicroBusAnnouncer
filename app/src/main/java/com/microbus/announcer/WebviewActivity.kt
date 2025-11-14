package com.microbus.announcer

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.microbus.announcer.databinding.ActivityWebviewBinding

class WebviewActivity : AppCompatActivity() {

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

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.defaultTextEncodingName = "utf-8"

//        binding.webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//        binding.webview.settings.allowFileAccess = true
//        binding.webview.settings.allowContentAccess = true
//        binding.webview.settings.cacheMode = WebSettings.LOAD_DEFAULT;
//
//        binding.webview.settings.setSupportZoom(true)
//        binding.webview.settings.builtInZoomControls = true
//        binding.webview.settings.displayZoomControls = false
//
//        binding.webview.clearHistory()
//        binding.webview.clearCache(true)
//
//        binding.webview.webViewClient = object :WebViewClient(){
//        }
//
//        binding.webview.webChromeClient = object :WebChromeClient(){
//
//        }

        binding.webview.setWebViewClient(object : WebViewClient() {
//            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//                view.loadUrl(url)
//                return true
//            }
        })

        val uriStr = intent.getStringExtra("uriStr")


        if (uriStr != null) {
            binding.webview.loadUrl(uriStr)
        }

    }
}