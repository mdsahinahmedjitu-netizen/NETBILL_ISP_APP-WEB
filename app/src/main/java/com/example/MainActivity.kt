package com.example

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.AppTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val currentCustomer by viewModel.currentCustomer.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AppTheme(darkTheme = isDarkMode) {
                when {
                    currentCustomer != null -> {
                        CustomerDashboardScreen(viewModel = viewModel)
                    }
                    currentUser != null -> {
                        AdminWebShell(url = "https://netbill-isp.web.app/")
                    }
                    else -> {
                        LoginScreen(viewModel = viewModel, onLoginSuccess = {})
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun AdminWebShell(url: String) {
        // MATCH STATUS BAR TO WEB THEME
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#0D9488")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        AndroidView(
            factory = { context ->
                val root = FrameLayout(context)
                root.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Add top padding for status bar to prevent overlapping
                root.fitsSystemWindows = true 

                val wv = WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Optional: can inject CSS here if needed
                        }
                    }
                    
                    loadUrl(url)
                }
                webViewInstance = wv
                root.addView(wv)
                root
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
