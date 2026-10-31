package com.notbypass.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTest {
    @Test
    fun urlIsNormalized() {
        val engine = Defaults.engines.first()
        assertEquals("https://example.com", normalizeInput("example.com", engine))
        assertTrue(normalizeInput("kotlin docs", engine).contains("duckduckgo.com"))
    }

    @Test
    fun historyDefaultOff() {
        assertEquals(false, JsonStoreDefaults.history)
    }
}

object JsonStoreDefaults {
    const val history = false
}
