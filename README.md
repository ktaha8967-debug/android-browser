# Private Browser (Android) 🌐🛡️

A lightweight, production-grade Android browser focused on **permanent incognito browsing**, **multi-tab session isolation**, **synchronized multi-tab input injection**, and a **modular proxy profile routing architecture**.

---

## 🚀 Key Features

1. **Permanent Incognito Engine**
   - Operates strictly in private mode 100% of the time (no normal browsing mode).
   - Never saves browsing history, form autofill data, or credentials.
   - Cache, session cookies, and DOM storage are automatically sanitized upon tab closure and completely wiped when the application terminates.

2. **Isolated Multi-Tab Management**
   - Independent WebView instance per tab with its own navigation history.
   - Dynamic tab switching, creation, and individual/bulk tab closure.
   - Modern grid-based tab switcher displaying real-time titles, URLs, favicons, and assigned proxy status.

3. **Global / Multi-Tab Synchronized Input ("Multi Input")**
   - Floating input modal capable of broadcasting text to:
     - The currently active tab
     - All open tabs
     - Any custom subset of selected tabs
   - Injects text into focused `<input>`, `<textarea>`, or `[contenteditable]` elements via safe JavaScript dispatching native DOM `input` and `change` events for reactive web apps (React, Vue, etc.).
   - Non-intrusive error notifications if a webpage restricts programmatic input.

4. **Per-Tab IP / Proxy Profile Architecture**
   - Real `ProxyProfile` entity (Host, Port, Username, Password, Tab Assignment).
   - Dedicated Proxy Manager UI to Add, Edit, Delete, and Test socket connectivity to proxies.
   - Pluggable `ProxyRoutingStrategy` utilizing AndroidX `androidx.webkit.ProxyController` for real network proxy routing.
   - *Honest Architecture:* Does not fake IP addresses.

5. **Customizable UI & Theming**
   - Instant Dark, Light, and System theme switching.
   - 6 Predefined accent color choices (Blue, Emerald, Purple, Amber, Rose, Cyan) applied in real-time.
   - Configurable toolbar (top address bar, toggleable bottom toolbar, compact mode).

6. **Search & Navigation**
   - Fast address bar with intelligent URL detection and query formatting.
   - Supported engines: **Google**, **Bing**, **DuckDuckGo**, and **Custom Query URL**.
   - DownloadManager integration with cookie preservation and Scoped Storage support.
   - File upload support via Android Activity Result File Chooser.
   - Strict SSL certificate verification warning dialogs.

---

## 🏗️ Architecture & Component Design

```
com.example.privatebrowser/
├── BrowserApplication.kt         # Global state & incognito initialization / wipe on exit
├── MainActivity.kt               # Central UI coordinator, navigation & intent handler
├── BrowserViewModel.kt           # MVVM ViewModel coordinating tab & proxy actions
├── model/
│   ├── BrowserTab.kt             # Tab state (ID, title, URL, WebView reference, proxy ID)
│   ├── ProxyProfile.kt           # Proxy model (Host, Port, Auth, Tab assignment)
│   ├── SearchEngine.kt           # Search engine definitions and URL query builder
│   └── UserSettings.kt           # ThemeMode, ToolbarPosition, AccentColor enums
├── core/
│   ├── PrivateWebViewFactory.kt  # Creates hardened private WebViews & manages wiping
│   ├── WebViewController.kt      # WebViewClient, WebChromeClient, SSL, File chooser
│   ├── TabManager.kt             # Lifecycle management for open tabs and isolated WebViews
│   ├── MultiInputManager.kt      # Safe DOM input injection across single/multiple tabs
│   ├── DownloadManagerHelper.kt  # Android system DownloadManager integration
│   └── PermissionManager.kt      # Scoped runtime permission handling (Android 10–14)
├── proxy/
│   ├── ProxyRoutingStrategy.kt   # Strategy interface for network proxy routing
│   ├── SystemWebViewProxyStrategy.kt # AndroidX ProxyController & Socket verification
│   └── ProxyManager.kt           # Proxy profiles persistence and active tab listener
├── settings/
│   └── SettingsManager.kt        # SharedPreferences manager for non-sensitive UI settings
├── ui/
│   ├── TabAdapter.kt             # RecyclerView adapter for Tab Switcher grid
│   ├── ProxyAdapter.kt           # RecyclerView adapter for Proxy profiles
│   ├── TabSwitcherBottomSheet.kt # BottomSheet tab overview & management
│   ├── ProxyManagerBottomSheet.kt# BottomSheet for proxy configuration & testing
│   ├── MultiInputDialog.kt       # Synchronized cross-tab input dialog
│   └── SettingsBottomSheet.kt    # Preferences & theme customization dialog
└── utils/
    ├── UrlUtils.kt               # URL parser, domain extractor & validator
    └── ThemeHelper.kt            # Dynamic DayNight theme changer
```

---

## 🔒 Incognito & Tab Isolation Details

- **No Persistent Database:** The application deliberately avoids creating SQLite/Room databases for browsing history or visited URLs.
- **Cache Isolation & Wipe:** WebViews are created with `LOAD_NO_CACHE` and destroyed with `clearCache(true)`, `clearFormData()`, `clearHistory()`, and `clearSslPreferences()`.
- **Global Wipe:** On launch and process exit, `CookieManager.getInstance().removeAllCookies()` and `WebStorage.getInstance().deleteAllData()` ensure zero residual session footprints.

---

## 🌐 Per-Tab Proxy Routing & Technical Limitations

### How Proxy Routing Works:
- Android WebView leverages the Chromium network stack.
- The browser uses AndroidX `androidx.webkit.ProxyController.getInstance().setProxyOverride()`.
- Whenever the user switches tabs, `ProxyManager.updateProxyForActiveTab()` activates the proxy assigned specifically to that tab (or clears the proxy if the tab uses default routing).
- The built-in **Test Proxy** tool performs a live direct socket handshake to verify that the specified host/port is reachable.

### Technical Realities & Limitations:
> [!IMPORTANT]
> Standard Android WebView does not expose separate underlying network sockets per individual `WebView` instance on an OS level. All WebViews within a single app process share the Chromium network stack. The browser dynamically sets the active proxy override as tabs are brought to the foreground. True simultaneous independent network routing for background tabs without process isolation requires an external VPN TUN interface or multiple sandboxed app processes. We adhere to honest engineering principles and do not display fake IP badges.

---

## 🛠️ Build & Setup Instructions

### Prerequisites:
- **Android Studio** (Hedgehog 2023.1.1 or newer recommended)
- **JDK 17+** (JDK 21 fully compatible)
- **Android SDK Platform 34** (Min SDK: 29 / Android 10, Target SDK: 34)

### Building the Project:
1. Clone or open the `D:\Android browser` directory in Android Studio.
2. Allow Gradle sync to complete.
3. To build the Debug APK via command line:
   ```bash
   ./gradlew assembleDebug
   ```
4. Output APK location:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Running Unit Tests:
```bash
./gradlew test
```

---

## 📱 Testing Checklist

- [x] Application launches directly into private start page.
- [x] Search query automatically opens configured search engine (Google / Bing / DuckDuckGo / Custom).
- [x] Multiple tabs can be created, switched, and closed with independent navigation stacks.
- [x] Multi-Input modal broadcasts text to `<input>`, `<textarea>`, and `contenteditable` elements.
- [x] Proxy profiles can be added, tested, enabled/disabled, and assigned per tab.
- [x] Instant theme and accent color toggles work without activity recreation glitches.
- [x] SSL errors present a security warning rather than silently proceeding.
- [x] Downloads trigger Android system `DownloadManager`.
- [x] App termination wipes all transient cookies and cache.
