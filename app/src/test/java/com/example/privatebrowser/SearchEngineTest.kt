package com.example.privatebrowser

import com.example.privatebrowser.model.SearchEngine
import com.example.privatebrowser.model.SearchEngineType
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEngineTest {

    @Test
    fun testGoogleSearchUrlBuilding() {
        val engine = SearchEngine(SearchEngineType.GOOGLE)
        val url = engine.buildSearchUrl("android studio")
        assertEquals("https://www.google.com/search?q=android+studio", url)
    }

    @Test
    fun testDuckDuckGoSearchUrlBuilding() {
        val engine = SearchEngine(SearchEngineType.DUCKDUCKGO)
        val url = engine.buildSearchUrl("privacy browser")
        assertEquals("https://duckduckgo.com/?q=privacy+browser", url)
    }

    @Test
    fun testCustomSearchUrlBuilding() {
        val engine = SearchEngine(SearchEngineType.CUSTOM, "https://kagi.com/search?q=%s")
        val url = engine.buildSearchUrl("kotlin")
        assertEquals("https://kagi.com/search?q=kotlin", url)
    }
}
