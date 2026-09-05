package com.example.newsfeed.data.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class RssProviderTest {
    @Test
    fun resolveAtomLinkTargets_prefersAlternateLink_andKeepsEnclosureAsImage() {
        val provider = RssProvider("https://example.com/feed")

        val withArticle = provider.resolveAtomLinkTargets(
            currentArticleLink = "",
            currentImageUrl = null,
            href = "https://example.com/articles/123",
            rel = "alternate",
            contentType = "text/html"
        )

        val withImage = provider.resolveAtomLinkTargets(
            currentArticleLink = withArticle.articleLink,
            currentImageUrl = withArticle.imageUrl,
            href = "https://cdn.example.com/images/123.jpg",
            rel = "enclosure",
            contentType = "image/jpeg"
        )

        assertEquals("https://example.com/articles/123", withImage.articleLink)
        assertEquals("https://cdn.example.com/images/123.jpg", withImage.imageUrl)
    }
}
