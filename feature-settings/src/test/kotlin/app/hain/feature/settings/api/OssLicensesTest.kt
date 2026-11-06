package app.hain.feature.settings.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** T-285: the licences page reads what gen_licenses.py wrote, completely and offline. */
class OssLicensesTest {
    private val dir = File("src/main/assets/licenses")
    private val catalog = OssLicenses.parse(File(dir, "licenses.json").readText())

    @Test
    fun everyGroupOfTheGeneratorIsKnownToTheApp() {
        val ids = Regex("\"id\": \"([a-z]+)\"").findAll(File(dir, "licenses.json").readText()).map { it.groupValues[1] }
        assertEquals(ids.toList(), catalog.groups.map { it.first.id })
        assertEquals(OssGroup.entries.toList(), catalog.groups.map { it.first })
    }

    @Test
    fun hainItselfIsGpl3WithItsText() {
        assertEquals("GPL-3.0", catalog.app.licence)
        assertEquals("https://github.com/oskar26/hain", catalog.app.url)
        assertEquals(catalog.app, catalog.find(OssCatalog.APP_KEY))
    }

    @Test
    fun everyEntryHasALicenceAnHttpsPageAndItsTexts() {
        catalog.groups.forEach { (group, entries) ->
            assertTrue(group.id, entries.isNotEmpty())
            entries.forEach { e ->
                assertTrue(e.name, e.licence.isNotBlank())
                assertTrue(e.name, e.url.startsWith("https://"))
                assertTrue(e.name, e.texts.isNotEmpty())
                e.texts.forEach { assertTrue("${e.name}: $it", File(dir, "texts/$it").length() > 0) }
                assertEquals(e, catalog.find(e.key(group)))
            }
        }
        assertNull(catalog.find("libraries/nothing"))
    }

    @Test
    fun theNamedModelsAndEngineAreListed() {
        fun group(g: OssGroup) = catalog.groups.first { it.first == g }.second
        val models = group(OssGroup.MODELS).associateBy { it.name }
        assertEquals("LFM Open License v1.0", models.getValue("LFM3-1.7B").licence)
        assertEquals("LFM Open License v1.0", models.getValue("LFM3-Omni-2B").licence)
        assertEquals("Apache-2.0", models.getValue("Jan-v3.5-4B").licence)
        assertEquals("OFL-1.1", group(OssGroup.FONTS).first { it.name == "Hanken Grotesk" }.licence)
        assertEquals("ISC", group(OssGroup.ICONS).single().licence)
        assertNotNull(group(OssGroup.ENGINE).firstOrNull { it.name.startsWith("llama.cpp") && it.licence == "MIT" })
    }

    @Test
    fun reflowJoinsWrappedLinesButKeepsListsAndTables() {
        val text =
            "MIT License\n\nPermission is hereby granted,\nfree of charge.\n\n" +
                "  1. Definitions.\n  (a) one\n  (b) two\n\n" +
                "  Arabic      Base, Tiny\n  Korean      Base\n\n=====\nTitle"
        val out = OssLicenses.reflow(text)
        assertTrue(out, out.contains("Permission is hereby granted, free of charge."))
        assertTrue(out, out.contains("1. Definitions.\n(a) one\n(b) two"))
        assertTrue(out, out.contains("Arabic      Base, Tiny\nKorean      Base"))
        assertTrue(out, out.contains("=====\nTitle"))
        assertFalse(out, out.contains("\n\n\n"))
    }

    @Test
    fun subtitleLeavesOutAMissingVersion() {
        assertEquals("1.0 · MIT", OssEntry("a", "1.0", "MIT", "https://a", listOf("x")).subtitle())
        assertEquals("ISC", OssEntry("a", "", "ISC", "https://a", listOf("x")).subtitle())
    }
}
