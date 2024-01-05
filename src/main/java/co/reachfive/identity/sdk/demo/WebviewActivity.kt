package co.reachfive.identity.sdk.demo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebViewClient
import co.reachfive.identity.sdk.demo.databinding.WebviewBinding

class WebviewActivity: Activity() {

    private val TAG = "Reach5_WebviewActivity"

        private lateinit var binding: WebviewBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = WebviewBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.webview.apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
            }

            val url = intent.data!!
Log.d(TAG,url.toString())
            binding.webview.loadUrl(url.toString())
        }

}
