package com.example.privatebrowser.core

import com.example.privatebrowser.model.BrowserTab
import org.json.JSONObject

object AutoClaimManager {

    data class ClaimResult(
        val totalTabs: Int,
        val successCount: Int,
        val failCount: Int,
        val details: List<String>
    )

    fun claimAcrossTabs(
        tabs: List<BrowserTab>,
        codeText: String,
        autoClickRedeem: Boolean = true,
        onComplete: (ClaimResult) -> Unit
    ) {
        if (tabs.isEmpty() || codeText.isBlank()) {
            onComplete(ClaimResult(0, 0, 0, listOf("No tabs or empty code")))
            return
        }

        val encodedCode = JSONObject.quote(codeText.trim())

        val jsScript = """
            (function() {
                try {
                    var code = $encodedCode;
                    var autoClick = $autoClickRedeem;
                    
                    // 1. Locate the best matching Gift/Redeem Code Input Field
                    function findCodeInput() {
                        var active = document.activeElement;
                        if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA') && active.type !== 'hidden') {
                            return active;
                        }
                        
                        var selectors = [
                            'input[placeholder*="code" i]',
                            'input[placeholder*="gift" i]',
                            'input[placeholder*="redeem" i]',
                            'input[placeholder*="cdk" i]',
                            'input[placeholder*="promo" i]',
                            'input[placeholder*="bonus" i]',
                            'input[placeholder*="enter" i]',
                            'input[name*="code" i]',
                            'input[name*="gift" i]',
                            'input[name*="redeem" i]',
                            'input[id*="code" i]',
                            'input[id*="gift" i]',
                            'input[id*="redeem" i]',
                            'input[class*="code" i]',
                            'input[class*="gift" i]',
                            'input[class*="redeem" i]',
                            'input[type="text"]',
                            'input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]):not([type="submit"]):not([type="button"])',
                            'textarea'
                        ];
                        
                        for (var s = 0; s < selectors.length; s++) {
                            var matches = document.querySelectorAll(selectors[s]);
                            for (var i = 0; i < matches.length; i++) {
                                var el = matches[i];
                                var rect = el.getBoundingClientRect();
                                if (rect.width > 0 && rect.height > 0) {
                                    return el;
                                }
                            }
                        }
                        return null;
                    }
                    
                    // 2. Locate the Redeem/Claim/Submit button
                    function findRedeemButton() {
                        var buttonKeywords = ['redeem', 'claim', 'receive', 'exchange', 'submit', 'confirm', 'get', 'apply', 'ok', 'recharge'];
                        var allClickables = document.querySelectorAll('button, input[type="submit"], input[type="button"], [role="button"], a.btn, div.btn, .van-button, .nut-button, .btn, .submit');
                        
                        for (var k = 0; k < buttonKeywords.length; k++) {
                            var kw = buttonKeywords[k];
                            for (var b = 0; b < allClickables.length; b++) {
                                var btn = allClickables[b];
                                var text = (btn.innerText || btn.textContent || btn.value || '').trim().toLowerCase();
                                var rect = btn.getBoundingClientRect();
                                if (rect.width > 0 && rect.height > 0 && text.indexOf(kw) !== -1) {
                                    return btn;
                                }
                            }
                        }
                        
                        // Fallback: search any element with redeem/claim class or id
                        var classCandidates = document.querySelectorAll('[class*="redeem" i], [class*="claim" i], [id*="redeem" i], [id*="claim" i], [class*="submit" i]');
                        for (var c = 0; c < classCandidates.length; c++) {
                            var el = classCandidates[c];
                            var r = el.getBoundingClientRect();
                            if (r.width > 0 && r.height > 0 && el.tagName !== 'INPUT') {
                                return el;
                            }
                        }
                        
                        return null;
                    }
                    
                    var targetInput = findCodeInput();
                    if (!targetInput) {
                        return JSON.stringify({ success: false, reason: "Gift/Redeem input box not found on page." });
                    }
                    
                    // Focus and insert code using native descriptor
                    targetInput.focus();
                    var proto = targetInput.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
                    var nativeSetter = Object.getOwnPropertyDescriptor(proto, 'value');
                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(targetInput, code);
                    } else {
                        targetInput.value = code;
                    }
                    
                    // Trigger DOM events for framework reactivity
                    targetInput.dispatchEvent(new Event('input', { bubbles: true }));
                    targetInput.dispatchEvent(new Event('change', { bubbles: true }));
                    targetInput.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'Enter' }));
                    
                    var clickedBtnText = "";
                    if (autoClick) {
                        var redeemBtn = findRedeemButton();
                        if (redeemBtn) {
                            clickedBtnText = (redeemBtn.innerText || redeemBtn.textContent || redeemBtn.value || 'Redeem').trim();
                            // Click with small synthetic delay
                            setTimeout(function() {
                                redeemBtn.click();
                            }, 100);
                        }
                    }
                    
                    var status = clickedBtnText ? ("Pasted & Clicked '" + clickedBtnText + "'") : "Code Pasted Successfully";
                    return JSON.stringify({ success: true, reason: status });
                } catch(e) {
                    return JSON.stringify({ success: false, reason: "Error: " + e.message });
                }
            })();
        """.trimIndent()

        var pending = tabs.size
        var successes = 0
        var fails = 0
        val messages = mutableListOf<String>()

        for (tab in tabs) {
            val webView = tab.webView
            if (webView == null) {
                fails++
                pending--
                messages.add("Tab #${tab.tabNumber}: Offline / WebView closed")
                if (pending == 0) {
                    onComplete(ClaimResult(tabs.size, successes, fails, messages))
                }
                continue
            }

            webView.evaluateJavascript(jsScript) { rawResult ->
                try {
                    val cleaned = if (rawResult != null && rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                        org.json.JSONTokener(rawResult).nextValue().toString()
                    } else {
                        rawResult ?: "{}"
                    }
                    val json = JSONObject(cleaned)
                    val ok = json.optBoolean("success", false)
                    val reason = json.optString("reason", "")
                    if (ok) {
                        successes++
                        messages.add("Tab #${tab.tabNumber} (${tab.title.take(12)}): $reason")
                    } else {
                        fails++
                        messages.add("Tab #${tab.tabNumber}: $reason")
                    }
                } catch (e: Exception) {
                    fails++
                    messages.add("Tab #${tab.tabNumber}: Script execution error")
                } finally {
                    pending--
                    if (pending == 0) {
                        onComplete(ClaimResult(tabs.size, successes, fails, messages))
                    }
                }
            }
        }
    }

    fun reloadAllTabs(tabs: List<BrowserTab>) {
        for (tab in tabs) {
            tab.webView?.reload()
        }
    }
}
