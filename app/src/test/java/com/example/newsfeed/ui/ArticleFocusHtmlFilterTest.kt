package com.example.newsfeed.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleFocusHtmlFilterTest {

    private val providers = listOf("rts", "srf", "blast", "empa")

    @Test
    fun fixtures_are_loaded_for_all_providers() {
        providers.forEach { provider ->
            val html = fixtureHtml(provider)
            assertTrue("Fixture for $provider should not be empty", html.isNotBlank())
        }
    }

    @Test
    fun share_controls_are_hidden_for_rts_and_srf() {
        listOf("rts", "srf").forEach { provider ->
            val original = parse(provider)
            val before = ArticleFocusHtmlFilter.countInteractiveShareControls(original)

            val filtered = ArticleFocusHtmlFilter.apply(
                fixtureHtml(provider),
                ArticleFocusHtmlFilter.Config(hideBottomArticles = false)
            )
            val after = ArticleFocusHtmlFilter.countInteractiveShareControls(filtered)

            assertTrue(
                "Share controls should not increase after filtering for $provider (before=$before after=$after)",
                after <= before
            )
            if (before > 0) {
                assertEquals("Share controls should be hidden for $provider", 0, after)
            }
        }
    }

    @Test
    fun related_headings_stay_when_hide_bottom_disabled_for_rts_and_srf() {
        listOf("rts", "srf").forEach { provider ->
            val original = parse(provider)
            val before = ArticleFocusHtmlFilter.countRelatedHeadings(original)

            val filtered = ArticleFocusHtmlFilter.apply(
                fixtureHtml(provider),
                ArticleFocusHtmlFilter.Config(hideBottomArticles = false)
            )
            val after = ArticleFocusHtmlFilter.countRelatedHeadings(filtered)

            assertTrue("Expected related headings in fixture for $provider", before > 0)
            assertEquals("Related headings should remain when toggle is off for $provider", before, after)
        }
    }

    @Test
    fun related_headings_are_hidden_when_hide_bottom_enabled_for_rts_and_srf() {
        listOf("rts", "srf").forEach { provider ->
            val original = parse(provider)
            val before = ArticleFocusHtmlFilter.countRelatedHeadings(original)

            val filtered = ArticleFocusHtmlFilter.apply(
                fixtureHtml(provider),
                ArticleFocusHtmlFilter.Config(hideBottomArticles = true)
            )
            val after = ArticleFocusHtmlFilter.countRelatedHeadings(filtered)

            assertTrue("Expected related headings in fixture for $provider", before > 0)
            assertTrue(
                "Related headings should decrease when toggle is on for $provider (before=$before after=$after)",
                after < before
            )
        }
    }

    @Test
    fun parser_runs_for_all_providers_with_both_configs() {
        providers.forEach { provider ->
            val keepBottom = ArticleFocusHtmlFilter.apply(
                fixtureHtml(provider),
                ArticleFocusHtmlFilter.Config(hideBottomArticles = false)
            )
            val hideBottom = ArticleFocusHtmlFilter.apply(
                fixtureHtml(provider),
                ArticleFocusHtmlFilter.Config(hideBottomArticles = true)
            )

            assertNotNull(keepBottom.body())
            assertNotNull(hideBottom.body())
        }
    }

    private fun parse(provider: String) = org.jsoup.Jsoup.parse(fixtureHtml(provider))

    private fun fixtureHtml(provider: String): String {
        val path = "fixtures/$provider.html"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw IllegalStateException("Missing fixture: $path")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
