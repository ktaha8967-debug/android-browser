package com.example.privatebrowser.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngineType(val displayName: String, val queryUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    CUSTOM("Custom", "");

    companion object {
        fun fromDisplayName(name: String): SearchEngineType {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: GOOGLE
        }
    }
}

data class SearchEngine(
    val type: SearchEngineType = SearchEngineType.GOOGLE,
    val customUrl: String = ""
) {
    fun buildSearchUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val template = if (type == SearchEngineType.CUSTOM && customUrl.isNotBlank()) {
            if (customUrl.contains("%s")) customUrl else "$customUrl$encodedQuery"
        } else {
            type.queryUrl
        }
        return if (template.contains("%s")) {
            String.format(template, encodedQuery)
        } else {
            template
        }
    }
}
