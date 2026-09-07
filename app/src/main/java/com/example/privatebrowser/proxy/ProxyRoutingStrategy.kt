package com.example.privatebrowser.proxy

import com.example.privatebrowser.model.ProxyProfile

interface ProxyRoutingStrategy {
    fun isSupported(): Boolean
    fun applyProxy(profile: ProxyProfile?, callback: (Boolean, String) -> Unit)
    fun clearProxy(callback: (Boolean, String) -> Unit)
    fun testProxyConnection(profile: ProxyProfile, callback: (Boolean, String) -> Unit)
}
