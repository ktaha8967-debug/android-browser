package com.example.privatebrowser

import android.app.Application
import com.example.privatebrowser.settings.SettingsManager
import com.example.privatebrowser.utils.ThemeHelper

class BrowserApplication : Application() {

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SettingsManager(this)

        try {
            androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            ThemeHelper.applyTheme(settingsManager.getThemeMode())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: BrowserApplication
            private set
    }
}
