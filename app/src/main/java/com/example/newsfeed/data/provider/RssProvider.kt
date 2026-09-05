package com.example.newsfeed.data.provider

import com.example.newsfeed.model.NewsArticle
import com.example.newsfeed.util.extractImageUrl
import com.example.newsfeed.util.extractSummary
import com.example.newsfeed.util.formatPubDate
import com.example.newsfeed.util.parsePubDateEpoch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Generic provider for RSS/Atom feeds.
 * Parses standard RSS 2.0 and Atom feeds and returns articles.
 * Does not support pagination or HTML-based older articles.
 */
open class RssProvider(val feedUrl: String) : NewsProvider {
    override val initialCursor: String? = null

    /**
     * Override to customize category derivation. [xmlCategory] is whatever the RSS
     * <category> tag contained (may be blank). Default just returns it unchanged.
     */
    open fun categoryFromItem(title: String, link: String, xmlCategory: String): String = xmlCategory

    override suspend fun fetchLatest(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val connection = URL(feedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
        connection.setRequestProperty("User-Agent", "UbikAndroid/1.0")

        connection.inputStream.use { input ->
            parseFeed(input).sortedByDescending { article -> article.publishedAtEpochMs }
        }
    }

    override suspend fun fetchOlder(cursor: String): PagedResult {
        // Simple RSS feeds don't support pagination
        return PagedResult(items = emptyList(), nextCursor = null)
    }

    private fun parseFeed(input: InputStream): List<NewsArticle> {
        val items = mutableListOf<NewsArticle>()
        val parserFactory = XmlPullParserFactory.newInstance()
        val parser = parserFactory.newPullParser().apply {
            setInput(input, null)
        }

        var eventType = parser.eventType
        var inEntry = false
        var entryTag = ""
        var title = ""
        var link = ""
        var category = ""
        var published = ""
        var description = ""
        var imageUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.lowercase()
                    if (tagName == "item" || tagName == "entry") {
                        inEntry = true
                        entryTag = tagName
                        title = ""
                        link = ""
                        category = ""
                        published = ""
                        description = ""
                        imageUrl = null
                    } else if (inEntry) {
                        when (tagName) {
                            "title" -> title = parser.nextText().trim()
                            "link" -> {
                                if (entryTag == "entry") {
                                    val href = parser.findAttributeValue("href")?.trim().orEmpty()
                                    val rel = parser.findAttributeValue("rel")?.trim()?.lowercase().orEmpty()
                                    val contentType = parser.findAttributeValue("type")?.trim().orEmpty()
                                    val resolved = resolveAtomLinkTargets(
                                        currentArticleLink = link,
                                        currentImageUrl = imageUrl,
                                        href = href,
                                        rel = rel,
                                        contentType = contentType
                                    )
                                    link = resolved.articleLink
                                    imageUrl = resolved.imageUrl
                                } else {
                                    link = parser.nextText().trim()
                                }
                            }
                            "category" -> {
                                val catText = if (entryTag == "entry") {
                                    parser.findAttributeValue("term")?.trim().orEmpty()
                                } else {
                                    parser.nextText().trim()
                                }
                                if (category.isBlank()) category = catText
                            }
                            "pubdate", "published", "updated" -> {
                                if (published.isBlank()) {
                                    published = parser.nextText().trim()
                                }
                            }
                            "description" -> description = parser.nextText().trim()
                            "summary", "content" -> {
                                if (description.isBlank()) {
                                    description = parser.nextText().trim()
                                }
                            }
                            "media:thumbnail", "media:content", "enclosure" -> {
                                // BBC and other feeds commonly expose lead image in media/enclosure tags.
                                val candidateUrl = parser.findAttributeValue("url")
                                val contentType = parser.findAttributeValue("type")
                                if (!candidateUrl.isNullOrBlank()) {
                                    val isLikelyImage = contentType.isNullOrBlank() ||
                                        contentType.startsWith("image/", ignoreCase = true) ||
                                        parser.name.equals("media:thumbnail", ignoreCase = true)
                                    if (isLikelyImage && imageUrl.isNullOrBlank()) {
                                        imageUrl = candidateUrl.trim()
                                    }
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val tagName = parser.name.lowercase()
                    if (tagName == entryTag && (tagName == "item" || tagName == "entry")) {
                        inEntry = false
                        entryTag = ""
                        if (title.isNotBlank() && link.isNotBlank()) {
                            items += NewsArticle(
                                title = title,
                                link = link,
                                category = categoryFromItem(title, link, category),
                                pubDateLabel = formatPubDate(published),
                                publishedAtEpochMs = parsePubDateEpoch(published),
                                summary = extractSummary(description),
                                imageUrl = imageUrl ?: extractImageUrl(description)
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }

    private fun XmlPullParser.findAttributeValue(attributeName: String): String? {
        for (index in 0 until attributeCount) {
            if (getAttributeName(index).equals(attributeName, ignoreCase = true)) {
                return getAttributeValue(index)
            }
        }
        return null
    }

    internal data class AtomLinkResolution(
        val articleLink: String,
        val imageUrl: String?
    )

    internal fun resolveAtomLinkTargets(
        currentArticleLink: String,
        currentImageUrl: String?,
        href: String,
        rel: String,
        contentType: String
    ): AtomLinkResolution {
        if (href.isBlank()) {
            return AtomLinkResolution(currentArticleLink, currentImageUrl)
        }

        if (rel == "enclosure") {
            val isLikelyImage = contentType.isBlank() || contentType.startsWith("image/", ignoreCase = true)
            val nextImage = if (isLikelyImage && currentImageUrl.isNullOrBlank()) href else currentImageUrl
            return AtomLinkResolution(currentArticleLink, nextImage)
        }

        val isPreferredArticleLink = rel.isBlank() || rel == "alternate"
        val nextArticle = if ((isPreferredArticleLink || currentArticleLink.isBlank()) && !href.startsWith("urn:", ignoreCase = true)) {
            href
        } else {
            currentArticleLink
        }

        return AtomLinkResolution(nextArticle, currentImageUrl)
    }
}
