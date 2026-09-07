package com.example.privatebrowser.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.example.privatebrowser.R
import com.example.privatebrowser.model.BrowserTab

interface WebViewControllerListener {
    fun onPageStarted(tab: BrowserTab, url: String)
    fun onPageFinished(tab: BrowserTab, url: String)
    fun onProgressChanged(tab: BrowserTab, newProgress: Int)
    fun onReceivedTitle(tab: BrowserTab, title: String)
    fun onReceivedFavicon(tab: BrowserTab, favicon: Bitmap?)
    fun onPageError(tab: BrowserTab, description: String, failingUrl: String)
    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean
    fun onExternalIntent(intent: Intent): Boolean
}

class WebViewController(
    private val context: Context,
    private val listener: WebViewControllerListener
) {

    fun createWebViewClient(tab: BrowserTab): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // If scheme is http or https, load internally
                if (url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("https://", ignoreCase = true) ||
                    url.startsWith("about:", ignoreCase = true)
                ) {
                    return false
                }

                // Handle external intent schemes (e.g. tel:, mailto:, market:, intent:)
                return try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    listener.onExternalIntent(intent)
                } catch (e: Exception) {
                    false
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tab.url = url ?: ""
                tab.isLoading = true
                tab.favicon = favicon
                listener.onPageStarted(tab, tab.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                tab.url = url ?: ""
                tab.isLoading = false
                tab.title = view?.title ?: "Page"
                listener.onPageFinished(tab, tab.url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    tab.isLoading = false
                    val desc = error?.description?.toString() ?: "Network Error"
                    val failingUrl = request.url?.toString() ?: ""
                    listener.onPageError(tab, desc, failingUrl)
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // Secure SSL Error handling: Never blindly ignore certificate errors!
                val sslMessage = when (error?.primaryError) {
                    SslError.SSL_EXPIRED -> "The certificate has expired."
                    SslError.SSL_IDMISMATCH -> "Hostname mismatch in certificate."
                    SslError.SSL_UNTRUSTED -> "The certificate authority is not trusted."
                    SslError.SSL_NOTYETVALID -> "The certificate is not yet valid."
                    else -> "An SSL security error occurred."
                }

                AlertDialog.Builder(context)
                    .setTitle(R.string.ssl_warning_title)
                    .setMessage("${context.getString(R.string.ssl_warning_msg)}\n\nReason: $sslMessage")
                    .setPositiveButton(R.string.proceed_anyway) { _, _ ->
                        handler?.proceed()
                    }
                    .setNegativeButton(R.string.leave_page) { _, _ ->
                        handler?.cancel()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    fun createWebChromeClient(tab: BrowserTab): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                tab.progress = newProgress
                listener.onProgressChanged(tab, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) {
                    tab.title = title
                    listener.onReceivedTitle(tab, title)
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                tab.favicon = icon
                listener.onReceivedFavicon(tab, icon)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return listener.onShowFileChooser(filePathCallback, fileChooserParams)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                // Incognito policy: do not retain permissions permanently
                AlertDialog.Builder(context)
                    .setTitle("Location Permission")
                    .setMessage("Allow '$origin' to access your device location for this session?")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        callback?.invoke(origin, true, false)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        callback?.invoke(origin, false, false)
                    }
                    .show()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Handle camera / mic web permissions
                AlertDialog.Builder(context)
                    .setTitle("Website Permission Request")
                    .setMessage("This site requests access to: ${request?.resources?.joinToString()}")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        request?.grant(request.resources)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        request?.deny()
                    }
                    .show()
            }
        }
    }
}
