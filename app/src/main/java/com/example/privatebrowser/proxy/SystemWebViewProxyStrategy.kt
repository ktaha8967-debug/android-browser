package com.example.privatebrowser.proxy

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.example.privatebrowser.model.ProxyProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SystemWebViewProxyStrategy : ProxyRoutingStrategy {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun isSupported(): Boolean {
        return try {
            WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
        } catch (e: Throwable) {
            false
        }
    }

    override fun applyProxy(profile: ProxyProfile?, callback: (Boolean, String) -> Unit) {
        if (!isSupported()) {
            callback(false, "ProxyOverride is not supported on this Android WebView engine.")
            return
        }

        if (profile == null || !profile.enabled || !profile.isValid()) {
            clearProxy(callback)
            return
        }

        try {
            val proxyUrl = "http://${profile.host}:${profile.port}"
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                executor,
                Runnable {
                    callback(true, "Proxy active: ${profile.getFormattedAddress()}")
                }
            )
        } catch (e: Exception) {
            callback(false, "Failed to apply proxy: ${e.message}")
        }
    }

    override fun clearProxy(callback: (Boolean, String) -> Unit) {
        if (!isSupported()) {
            callback(true, "Proxy cleared (default routing)")
            return
        }

        try {
            ProxyController.getInstance().clearProxyOverride(
                executor,
                Runnable {
                    callback(true, "Proxy override cleared")
                }
            )
        } catch (e: Exception) {
            callback(false, "Failed to clear proxy: ${e.message}")
        }
    }

    override fun testProxyConnection(profile: ProxyProfile, callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket()
                val socketAddress = InetSocketAddress(profile.host, profile.port)
                // Connect with 4 second timeout
                socket.connect(socketAddress, 4000)
                socket.close()
                withContext(Dispatchers.Main) {
                    callback(true, "Connection successful to ${profile.host}:${profile.port}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "Connection failed: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }
}
