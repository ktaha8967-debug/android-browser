package com.example.privatebrowser.core

import com.example.privatebrowser.model.BrowserTab

object MultiInputManager {

    data class InjectionResult(
        val totalTargeted: Int,
        val successCount: Int,
        val failureCount: Int,
        val messages: List<String>
    )

    fun sendTextToTabs(
        tabs: List<BrowserTab>,
        textToInject: String,
        onComplete: (InjectionResult) -> Unit
    ) {
        AutoClaimManager.claimAcrossTabs(tabs, textToInject, autoClickRedeem = true) { result ->
            onComplete(
                InjectionResult(
                    totalTargeted = result.totalTabs,
                    successCount = result.successCount,
                    failureCount = result.failCount,
                    messages = result.details
                )
            )
        }
    }
}
