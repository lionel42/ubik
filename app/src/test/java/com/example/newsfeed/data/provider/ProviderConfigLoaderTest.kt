package com.example.newsfeed.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigLoaderTest {
    @Test
    fun parseProvidersConfig_parsesValidConfig() {
        val json = """
            {
              "version": 1,
              "providers": [
                {
                  "id": "RTS",
                  "label": "RTS",
                  "description": "Swiss public broadcaster",
                  "language": "fr",
                  "region": "CH",
                  "category": "NEWS",
                  "feedUrl": "https://example.com/rss"
                }
              ]
            }
        """.trimIndent()

        val providers = ProviderConfigLoader.parseProvidersConfig(json)

        assertEquals(1, providers.size)
        val first = providers.first()
        assertEquals("RTS", first.id)
        assertEquals("RTS", first.label)
        assertEquals(FeedCategory.NEWS, first.category)
        assertEquals("https://example.com/rss", first.feedUrl)
    }

    @Test
    fun parseProvidersConfig_rejectsDuplicateIds() {
        val json = """
            {
              "providers": [
                {
                  "id": "same",
                  "label": "First",
                  "category": "NEWS",
                  "feedUrl": "https://example.com/one"
                },
                {
                  "id": "same",
                  "label": "Second",
                  "category": "SCIENCE",
                  "feedUrl": "https://example.com/two"
                }
              ]
            }
        """.trimIndent()

        val error = runCatching { ProviderConfigLoader.parseProvidersConfig(json) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("Duplicate provider ids") == true)
    }

    @Test
    fun parseProvidersConfig_rejectsUnknownCategory() {
        val json = """
            {
              "providers": [
                {
                  "id": "id-1",
                  "label": "Provider",
                  "category": "UNKNOWN",
                  "feedUrl": "https://example.com/rss"
                }
              ]
            }
        """.trimIndent()

        val error = runCatching { ProviderConfigLoader.parseProvidersConfig(json) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("unknown value") == true)
    }
}
