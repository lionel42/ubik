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
 * Generic RSS provider for simple RSS feeds.
 * Parses standard RSS 2.0 feeds and returns articles.
 * Does not support pagination or HTML-based older articles.
 */
open class SimpleRssProvider(val feedUrl: String) : NewsProvider {
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
            parseRss(input).sortedByDescending { article -> article.publishedAtEpochMs }
        }
    }

    override suspend fun fetchOlder(cursor: String): PagedResult {
        // Simple RSS feeds don't support pagination
        return PagedResult(items = emptyList(), nextCursor = null)
    }

    private fun parseRss(input: InputStream): List<NewsArticle> {
        val items = mutableListOf<NewsArticle>()
        val parserFactory = XmlPullParserFactory.newInstance()
        val parser = parserFactory.newPullParser().apply {
            setInput(input, null)
        }

        var eventType = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var category = ""
        var pubDate = ""
        var description = ""
        var imageUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true)) {
                        inItem = true
                        title = ""
                        link = ""
                        category = ""
                        pubDate = ""
                        description = ""
                        imageUrl = null
                    } else if (inItem) {
                        when (parser.name.lowercase()) {
                            "title" -> title = parser.nextText().trim()
                            "link" -> link = parser.nextText().trim()
                            "category" -> {
                                val catText = parser.nextText().trim()
                                if (category.isBlank()) category = catText
                            }
                            "pubdate" -> pubDate = parser.nextText().trim()
                            "description" -> description = parser.nextText().trim()
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
                    if (parser.name.equals("item", ignoreCase = true)) {
                        inItem = false
                        if (title.isNotBlank() && link.isNotBlank()) {
                            items += NewsArticle(
                                title = title,
                                link = link,
                                category = categoryFromItem(title, link, category),
                                pubDateLabel = formatPubDate(pubDate),
                                publishedAtEpochMs = parsePubDateEpoch(pubDate),
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
}
