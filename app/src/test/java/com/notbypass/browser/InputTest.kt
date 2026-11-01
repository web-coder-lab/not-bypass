package com.notbypass.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTest {
    private val ddg = Defaults.engines.first()

    @Test
    fun domainBecomesHttps() {
        assertEquals("https://example.com", normalizeInput("example.com", ddg))
        assertEquals("https://example.com/a?b=1", normalizeInput("example.com/a?b=1", ddg))
    }

    @Test
    fun localAddressesUseHttp() {
        assertEquals("http://localhost:8080", normalizeInput("localhost:8080", ddg))
        assertEquals("http://192.168.1.1", normalizeInput("192.168.1.1", ddg))
    }

    @Test
    fun explicitSchemesAreKept() {
        assertEquals("https://a.b/c", normalizeInput("https://a.b/c", ddg))
        assertEquals("about:blank", normalizeInput("about:blank", ddg))
    }

    @Test
    fun textBecomesSearch() {
        assertTrue(normalizeInput("kotlin docs", ddg).startsWith("https://duckduckgo.com/?q=kotlin"))
        assertFalse(isUrlLike("3.5"))
        assertFalse(isUrlLike("hello"))
    }

    @Test
    fun hostDisplay() {
        assertEquals("example.com", displayHost("https://www.example.com/path?q=1"))
    }

    @Test
    fun historyIsOffByDefault() {
        assertFalse(Defaults.settings().has("history"))
        assertFalse(Defaults.settings().getBoolean("clearOnExit"))
    }
}
