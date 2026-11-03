package app.hain.common.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearxAddressTest {
    @Test
    fun usableAddresses() {
        assertEquals("https://searx.example.org", SearxAddress.normalize("searx.example.org"))
        assertEquals("https://searx.example.org", SearxAddress.normalize(" https://searx.example.org/ "))
        assertEquals("https://searx.example.org", SearxAddress.normalize("https://searx.example.org/search?q=x"))
        assertEquals("https://my.host.de:8443/searx", SearxAddress.normalize("https://my.host.de:8443/searx/"))
    }

    @Test
    fun notUsable() {
        assertNull(SearxAddress.normalize(""))
        assertNull(SearxAddress.normalize("http://searx.example.org"))
        assertNull(SearxAddress.normalize("searx"))
        assertNull(SearxAddress.normalize("such mal"))
    }
}
