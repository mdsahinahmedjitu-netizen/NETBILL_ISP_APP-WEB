package com.example

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.AppTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
        val activity = LocalActivity.current
        val window = activity?.window
        if (window != null) {
            @Suppress("DEPRECATION")
            window.statusBarColor = "#0D9488".toColorInt()
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { context ->
                RelativeLayout(context).apply {
                    setBackgroundColor("#0D9488".toColorInt())
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140)
                    
                    addView(ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_media_previous)
                        setColorFilter(Color.WHITE)
                        setBackgroundColor(Color.TRANSPARENT)
                        layoutParams = RelativeLayout.LayoutParams(150, 140).apply { addRule(RelativeLayout.ALIGN_PARENT_LEFT) }
                        setOnClickListener { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() }
                    })

                    addView(TextView(context).apply {
                        @SuppressLint("SetTextI18n")
                        text = "NETBILL ADMIN"
                        setTextColor(Color.WHITE)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        gravity = Gravity.CENTER
                        layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 140).apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
                    })

                    addView(ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_rotate)
                        setColorFilter(Color.WHITE)
                        setBackgroundColor(Color.TRANSPARENT)
                        layoutParams = RelativeLayout.LayoutParams(150, 140).apply { addRule(RelativeLayout.ALIGN_PARENT_RIGHT) }
                        setOnClickListener { webViewInstance?.reload() }
                    })
                }
            })

            AndroidView(factory = { context ->
                WebView(context).apply {
                    webViewInstance = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            }, modifier = Modifier.weight(1f))
        }
    }

    private var webViewInstance: WebView? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
