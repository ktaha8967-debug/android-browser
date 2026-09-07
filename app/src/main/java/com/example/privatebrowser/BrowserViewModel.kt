package com.example.privatebrowser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.core.WebViewController
import com.example.privatebrowser.model.BrowserTab
import com.example.privatebrowser.proxy.ProxyManager
import com.example.privatebrowser.settings.SettingsManager
import com.example.privatebrowser.utils.UrlUtils

class BrowserViewModel(
    application: Application,
    private val tabManager: TabManager,
    val proxyManager: ProxyManager,
    val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    val tabs: LiveData<List<BrowserTab>> = tabManager.tabs
    val activeTab: LiveData<BrowserTab?> = tabManager.activeTab

    fun loadUrlOrQuery(input: String) {
        val currentTab = activeTab.value ?: return
        val searchEngine = settingsManager.getSearchEngine()
        val targetUrl = UrlUtils.formatInput(input) { query ->
            searchEngine.buildSearchUrl(query)
        }

        currentTab.url = targetUrl
        currentTab.webView?.loadUrl(targetUrl)
    }

    fun goBack(): Boolean {
        val webView = activeTab.value?.webView ?: return false
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(): Boolean {
        val webView = activeTab.value?.webView ?: return false
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else {
            false
        }
    }

    fun reload() {
        activeTab.value?.webView?.reload()
    }

    fun createNewTab(initialUrl: String? = null) {
        tabManager.createNewTab(initialUrl)
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }

    fun closeAllTabs() {
        tabManager.closeAllTabs()
    }
}
