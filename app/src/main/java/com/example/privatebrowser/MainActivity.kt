package com.example.privatebrowser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.example.privatebrowser.core.DownloadManagerHelper
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.core.WebViewController
import com.example.privatebrowser.core.WebViewControllerListener
import com.example.privatebrowser.databinding.ActivityMainBinding
import com.example.privatebrowser.model.BrowserTab
import com.example.privatebrowser.proxy.ProxyManager
import com.example.privatebrowser.settings.SettingsManager
import com.example.privatebrowser.ui.MultiInputDialog
import com.example.privatebrowser.ui.ProxyManagerBottomSheet
import com.example.privatebrowser.ui.SettingsBottomSheet
import com.example.privatebrowser.ui.TabSwitcherBottomSheet
import com.example.privatebrowser.utils.ThemeHelper
import com.example.privatebrowser.utils.UrlUtils

class MainActivity : AppCompatActivity(), WebViewControllerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var proxyManager: ProxyManager
    private lateinit var webViewController: WebViewController
    private lateinit var tabManager: TabManager
    private lateinit var viewModel: BrowserViewModel

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback == null) return@registerForActivityResult

        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = (application as? BrowserApplication)?.settingsManager ?: SettingsManager(this)
        proxyManager = ProxyManager(this)
        webViewController = WebViewController(this, this)
        tabManager = TabManager(this, webViewController)
        viewModel = BrowserViewModel(application, tabManager, proxyManager, settingsManager)

        try {
            setupUI()
            setupListeners()
            setupObservers()
            setupBackHandler()

            // Handle incoming VIEW intents
            handleIntent(intent)

            // If no tab exists yet, create initial start tab
            if (tabManager.getAllTabs().isEmpty()) {
                tabManager.createNewTab()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.dataString
        if (!data.isNullOrBlank()) {
            tabManager.createNewTab(data)
        }
    }

    private fun setupUI() {
        applySettingsUI()
    }

    private fun applySettingsUI() {
        // Toolbar visibility
        val showBottomToolbar = settingsManager.isBottomToolbarVisible()
        binding.bottomBarContainer.isVisible = showBottomToolbar
        binding.btnTopTabs.isVisible = !showBottomToolbar || settingsManager.isCompactMode()

        // Accent color
        val accentColor = settingsManager.getAccentColor()
        val accentColorInt = ThemeHelper.getAccentColorInt(this, accentColor)
        val colorStateList = ColorStateList.valueOf(accentColorInt)

        binding.btnIncognitoBadge.imageTintList = colorStateList
        binding.btnAddTab.imageTintList = colorStateList
        binding.fabMultiInput.backgroundTintList = colorStateList
    }

    private fun setupListeners() {
        // Address Bar
        binding.etAddressBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val input = binding.etAddressBar.text?.toString() ?: ""
                hideKeyboard()
                binding.etAddressBar.clearFocus()
                viewModel.loadUrlOrQuery(input)
                true
            } else {
                false
            }
        }

        binding.etAddressBar.setOnFocusChangeListener { _, hasFocus ->
            binding.btnClearUrl.isVisible = hasFocus && !binding.etAddressBar.text.isNullOrEmpty()
            if (hasFocus) {
                binding.etAddressBar.selectAll()
            }
        }

        binding.etAddressBar.addTextChangedListener { text ->
            binding.btnClearUrl.isVisible = binding.etAddressBar.hasFocus() && !text.isNullOrEmpty()
        }

        binding.btnClearUrl.setOnClickListener {
            binding.etAddressBar.setText("")
        }

        // Navigation Buttons
        binding.btnBack.setOnClickListener {
            if (!viewModel.goBack()) {
                Toast.makeText(this, "No previous page in this tab", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnForward.setOnClickListener {
            if (!viewModel.goForward()) {
                Toast.makeText(this, "No next page in this tab", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.reload()
        }

        binding.btnAddTab.setOnClickListener {
            tabManager.createNewTab()
        }

        binding.btnTabs.setOnClickListener {
            showTabSwitcher()
        }

        binding.btnTopTabs.setOnClickListener {
            showTabSwitcher()
        }

        binding.btnTopMore.setOnClickListener {
            showSettings()
        }

        binding.btnIncognitoBadge.setOnClickListener {
            showMultiInputDialog()
        }

        // Floating Auto-Claim Quick Button
        binding.fabMultiInput.setOnClickListener {
            showMultiInputDialog()
        }

        // Start Page & Error views
        binding.startPageView.btnStartPageNewTab.setOnClickListener {
            tabManager.createNewTab()
        }

        binding.errorView.btnErrorRetry.setOnClickListener {
            binding.errorView.root.isVisible = false
            viewModel.reload()
        }
    }

    private fun setupObservers() {
        tabManager.tabs.observe(this) { tabs ->
            val count = tabs.size.toString()
            binding.tvTabCount.text = count
            binding.tvTopTabCount.text = count
        }

        tabManager.activeTab.observe(this) { activeTab ->
            if (activeTab != null) {
                displayActiveTab(activeTab)
                proxyManager.updateProxyForActiveTab(activeTab.id)
            }
        }
    }

    private fun displayActiveTab(tab: BrowserTab) {
        val webContainer = binding.webContainer
        val currentWebView = tab.webView

        // Remove previous webview from container if needed
        for (i in 0 until webContainer.childCount) {
            val child = webContainer.getChildAt(i)
            if (child is android.webkit.WebView && child != currentWebView) {
                webContainer.removeView(child)
                break
            }
        }

        // Attach current tab webview if not already attached
        if (currentWebView != null && currentWebView.parent == null) {
            webContainer.addView(
                currentWebView,
                0,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            // Setup download listener
            currentWebView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                DownloadManagerHelper.downloadFile(this, url, userAgent, contentDisposition, mimetype)
            }
        }

        // Update address bar and visibility
        if (tab.url.isBlank() || tab.url == "about:blank") {
            binding.etAddressBar.setText("")
            binding.startPageView.root.isVisible = true
            binding.ivSecurityLock.isVisible = false
            currentWebView?.isVisible = false
        } else {
            if (!binding.etAddressBar.hasFocus()) {
                binding.etAddressBar.setText(tab.url)
            }
            binding.startPageView.root.isVisible = false
            currentWebView?.isVisible = true
            binding.ivSecurityLock.isVisible = UrlUtils.isHttps(tab.url)
        }

        binding.progressBar.isVisible = tab.isLoading
        binding.progressBar.progress = tab.progress
    }

    private fun showTabSwitcher() {
        TabSwitcherBottomSheet(viewModel, tabManager, proxyManager).show(
            supportFragmentManager,
            TabSwitcherBottomSheet.TAG
        )
    }

    private fun showMultiInputDialog() {
        MultiInputDialog(tabManager).show(
            supportFragmentManager,
            MultiInputDialog.TAG
        )
    }

    private fun showSettings() {
        SettingsBottomSheet(
            settingsManager = settingsManager,
            onSettingsChanged = {
                applySettingsUI()
            },
            onOpenProxyManager = {
                showProxyManager()
            }
        ).show(supportFragmentManager, SettingsBottomSheet.TAG)
    }

    private fun showProxyManager() {
        ProxyManagerBottomSheet(proxyManager, tabManager).show(
            supportFragmentManager,
            ProxyManagerBottomSheet.TAG
        )
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val activeWebView = tabManager.activeTab.value?.webView
                if (activeWebView != null && activeWebView.canGoBack()) {
                    activeWebView.goBack()
                } else if (tabManager.getAllTabs().size > 1) {
                    tabManager.activeTab.value?.let { currentTab ->
                        tabManager.closeTab(currentTab.id)
                    }
                } else {
                    finish()
                }
            }
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    // WebViewControllerListener Callbacks
    override fun onPageStarted(tab: BrowserTab, url: String) {
        if (tab.id == tabManager.activeTab.value?.id) {
            binding.progressBar.isVisible = true
            binding.progressBar.progress = 10
            binding.errorView.root.isVisible = false
            binding.startPageView.root.isVisible = false
            if (!binding.etAddressBar.hasFocus()) {
                binding.etAddressBar.setText(url)
            }
            binding.ivSecurityLock.isVisible = UrlUtils.isHttps(url)
        }
        tabManager.updateTabState(tab)
    }

    override fun onPageFinished(tab: BrowserTab, url: String) {
        if (tab.id == tabManager.activeTab.value?.id) {
            binding.progressBar.isVisible = false
            if (!binding.etAddressBar.hasFocus()) {
                binding.etAddressBar.setText(url)
            }
            binding.ivSecurityLock.isVisible = UrlUtils.isHttps(url)
        }
        tabManager.updateTabState(tab)
    }

    override fun onProgressChanged(tab: BrowserTab, newProgress: Int) {
        if (tab.id == tabManager.activeTab.value?.id) {
            binding.progressBar.progress = newProgress
            binding.progressBar.isVisible = newProgress < 100
        }
        tabManager.updateTabState(tab)
    }

    override fun onReceivedTitle(tab: BrowserTab, title: String) {
        tabManager.updateTabState(tab)
    }

    override fun onReceivedFavicon(tab: BrowserTab, favicon: Bitmap?) {
        tabManager.updateTabState(tab)
    }

    override fun onPageError(tab: BrowserTab, description: String, failingUrl: String) {
        if (tab.id == tabManager.activeTab.value?.id) {
            binding.progressBar.isVisible = false
            binding.errorView.root.isVisible = true
            binding.errorView.tvErrorDescription.text = "$description\n($failingUrl)"
        }
        tabManager.updateTabState(tab)
    }

    override fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = filePathCallback

        return try {
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            fileChooserLauncher.launch(intent)
            true
        } catch (e: ActivityNotFoundException) {
            fileUploadCallback = null
            Toast.makeText(this, "Cannot open file chooser", Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onExternalIntent(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
