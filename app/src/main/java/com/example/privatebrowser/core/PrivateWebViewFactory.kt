package com.example.privatebrowser.core

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

object PrivateWebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    fun createPrivateWebView(
        context: Context,
        client: WebViewClient,
        chromeClient: WebChromeClient
    ): WebView {
        val webView = WebView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }

        val settings = webView.settings

        // 1. JavaScript and Modern Web standards
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        // 2. Viewport & Zoom
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        // 3. Keep login cookies & sessions active for multiple accounts
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Safe Browsing & Dark Mode support
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, true)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                val isDark = (context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDark)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        webView.webViewClient = client
        webView.webChromeClient = chromeClient

        return webView
    }

    fun destroyTabWebView(webView: WebView?) {
        if (webView == null) return
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearGlobalSessionStorage(context: Context) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearGlobalIncognitoStorage(context: Context) {
        clearGlobalSessionStorage(context)
    }
}