package com.example.privatebrowser.model

import java.util.UUID

data class ProxyProfile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var host: String,
    var port: Int,
    var username: String? = null,
    var password: String? = null,
    var enabled: Boolean = true,
    var assignedTabId: String? = null // null means global/unassigned
) {
    fun getFormattedAddress(): String = "$host:$port"

    fun isValid(): Boolean {
        return host.isNotBlank() && port in 1..65535
    }
}
