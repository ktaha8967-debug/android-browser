package com.example.privatebrowser.proxy

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.privatebrowser.model.ProxyProfile
import org.json.JSONArray
import org.json.JSONObject

class ProxyManager(
    private val context: Context,
    val routingStrategy: ProxyRoutingStrategy = SystemWebViewProxyStrategy()
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profiles = MutableLiveData<List<ProxyProfile>>(emptyList())
    val profiles: LiveData<List<ProxyProfile>> = _profiles

    private val _currentActiveProxy = MutableLiveData<ProxyProfile?>()
    val currentActiveProxy: LiveData<ProxyProfile?> = _currentActiveProxy

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val rawJson = prefs.getString(KEY_PROFILES, null)
        val list = mutableListOf<ProxyProfile>()
        if (!rawJson.isNullOrBlank()) {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ProxyProfile(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            host = obj.getString("host"),
                            port = obj.getInt("port"),
                            username = obj.optString("username").takeIf { it.isNotEmpty() },
                            password = obj.optString("password").takeIf { it.isNotEmpty() },
                            enabled = obj.optBoolean("enabled", true),
                            assignedTabId = obj.optString("assignedTabId").takeIf { it.isNotEmpty() }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _profiles.value = list
    }

    private fun saveProfiles() {
        val list = _profiles.value ?: emptyList()
        val array = JSONArray()
        for (p in list) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("host", p.host)
            obj.put("port", p.port)
            obj.put("username", p.username ?: "")
            obj.put("password", p.password ?: "")
            obj.put("enabled", p.enabled)
            obj.put("assignedTabId", p.assignedTabId ?: "")
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun addOrUpdateProfile(profile: ProxyProfile) {
        val currentList = (_profiles.value ?: emptyList()).toMutableList()
        val index = currentList.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            currentList[index] = profile
        } else {
            currentList.add(profile)
        }
        _profiles.value = currentList
        saveProfiles()
    }

    fun deleteProfile(profileId: String) {
        val currentList = (_profiles.value ?: emptyList()).toMutableList()
        currentList.removeAll { it.id == profileId }
        _profiles.value = currentList
        saveProfiles()
    }

    fun getProfileForTab(tabId: String): ProxyProfile? {
        val list = _profiles.value ?: return null
        // First look for a proxy explicitly assigned to this tab
        val tabSpecific = list.firstOrNull { it.assignedTabId == tabId && it.enabled }
        if (tabSpecific != null) return tabSpecific

        // Otherwise look for a global enabled proxy (assignedTabId == null)
        return list.firstOrNull { it.assignedTabId == null && it.enabled }
    }

    fun updateProxyForActiveTab(tabId: String, callback: ((Boolean, String) -> Unit)? = null) {
        val targetProxy = getProfileForTab(tabId)
        _currentActiveProxy.value = targetProxy

        if (targetProxy != null && targetProxy.enabled) {
            routingStrategy.applyProxy(targetProxy) { success, msg ->
                callback?.invoke(success, msg)
            }
        } else {
            routingStrategy.clearProxy { success, msg ->
                callback?.invoke(success, msg)
            }
        }
    }

    fun testProxy(profile: ProxyProfile, callback: (Boolean, String) -> Unit) {
        routingStrategy.testProxyConnection(profile, callback)
    }

    companion object {
        private const val PREFS_NAME = "proxy_prefs"
        private const val KEY_PROFILES = "proxy_profiles_list"
    }
}
