package com.example.privatebrowser.model

import android.graphics.Bitmap
import android.webkit.WebView
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var tabNumber: Int = 1,
    var url: String = "",
    var title: String = "New Tab",
    var favicon: Bitmap? = null,
    var isLoading: Boolean = false,
    var progress: Int = 0,
    var assignedProxyId: String? = null,
    @Transient var webView: WebView? = null
)
