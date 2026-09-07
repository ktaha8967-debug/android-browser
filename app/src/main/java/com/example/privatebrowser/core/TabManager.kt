package com.example.privatebrowser.core

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.privatebrowser.model.BrowserTab

class TabManager(
    private val context: Context,
    private val controller: WebViewController
) {
    private val tabList = mutableListOf<BrowserTab>()
    private var tabCounter = 1

    private val _tabs = MutableLiveData<List<BrowserTab>>(emptyList())
    val tabs: LiveData<List<BrowserTab>> = _tabs

    private val _activeTab = MutableLiveData<BrowserTab?>()
    val activeTab: LiveData<BrowserTab?> = _activeTab

    fun createNewTab(initialUrl: String? = null): BrowserTab {
        val tab = BrowserTab(
            tabNumber = tabCounter++,
            url = initialUrl ?: "",
            title = if (initialUrl.isNullOrBlank()) "New Tab" else "Loading..."
        )

        // Create isolated private WebView instance
        val webView = PrivateWebViewFactory.createPrivateWebView(
            context = context,
            client = controller.createWebViewClient(tab),
            chromeClient = controller.createWebChromeClient(tab)
        )
        tab.webView = webView

        tabList.add(tab)
        _tabs.value = tabList.toList()
        selectTab(tab.id)

        if (!initialUrl.isNullOrBlank()) {
            webView.loadUrl(initialUrl)
        }

        return tab
    }

    fun selectTab(tabId: String): BrowserTab? {
        val tab = tabList.find { it.id == tabId } ?: return null
        _activeTab.value = tab
        return tab
    }

    fun closeTab(tabId: String): BrowserTab? {
        val index = tabList.indexOfFirst { it.id == tabId }
        if (index == -1) return null

        val tabToClose = tabList.removeAt(index)
        PrivateWebViewFactory.destroyTabWebView(tabToClose.webView)
        tabToClose.webView = null

        _tabs.value = tabList.toList()

        // If we closed the active tab, switch to another tab or create a new one
        if (_activeTab.value?.id == tabId) {
            val nextTab = when {
                tabList.isNotEmpty() -> {
                    val nextIndex = if (index < tabList.size) index else tabList.size - 1
                    tabList[nextIndex]
                }
                else -> {
                    createNewTab()
                }
            }
            selectTab(nextTab.id)
            return nextTab
        }

        return _activeTab.value
    }

    fun closeAllTabs() {
        for (tab in tabList) {
            PrivateWebViewFactory.destroyTabWebView(tab.webView)
            tab.webView = null
        }
        tabList.clear()
        tabCounter = 1

        // Clear global incognito session memory
        PrivateWebViewFactory.clearGlobalIncognitoStorage(context)

        // Create one clean new tab
        createNewTab()
    }

    fun getTabById(tabId: String): BrowserTab? {
        return tabList.find { it.id == tabId }
    }

    fun getAllTabs(): List<BrowserTab> = tabList.toList()

    fun updateTabState(tab: BrowserTab) {
        val index = tabList.indexOfFirst { it.id == tab.id }
        if (index >= 0) {
            tabList[index] = tab
            _tabs.value = tabList.toList()
            if (_activeTab.value?.id == tab.id) {
                _activeTab.value = tab
            }
        }
    }
}
