package com.example.privatebrowser.utils

import android.util.Patterns
import android.webkit.URLUtil
import java.net.URI
import java.util.Locale

object UrlUtils {

    fun formatInput(input: String, searchEngineQueryBuilder: (String) -> String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Check if it already has scheme
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return trimmed
        }

        // Check if it matches web URL pattern without scheme (e.g., example.com or sub.domain.org/path)
        if (Patterns.WEB_URL.matcher(trimmed).matches() ||
            (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3)
        ) {
            return URLUtil.guessUrl(trimmed)
        }

        // Treat as a search query
        return searchEngineQueryBuilder(trimmed)
    }

    fun extractDomain(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try {
            val uri = URI(url)
            val host = uri.host ?: url
            if (host.startsWith("www.", ignoreCase = true)) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: Exception) {
            url
        }
    }

    fun isHttps(url: String?): Boolean {
        return url?.lowercase(Locale.ROOT)?.startsWith("https://") == true
    }
}
