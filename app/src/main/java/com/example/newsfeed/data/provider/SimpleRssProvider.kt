package com.example.newsfeed.data.provider

import com.example.newsfeed.model.RtsArticle
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

    override suspend fun fetchLatest(): List<RtsArticle> = withContext(Dispatchers.IO) {
        val connection = URL(feedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
        connection.setRequestProperty("User-Agent", "NewsFeedAndroid/1.0")

        connection.inputStream.use { input ->
            parseRss(input).sortedByDescending { article -> article.publishedAtEpochMs }
        }
    }

    override suspend fun fetchOlder(cursor: String): PagedResult {
        // Simple RSS feeds don't support pagination
        return PagedResult(items = emptyList(), nextCursor = null)
    }

    private fun parseRss(input: InputStream): List<RtsArticle> {
        val items = mutableListOf<RtsArticle>()
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
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true)) {
                        inItem = false
                        if (title.isNotBlank() && link.isNotBlank()) {
                            items += RtsArticle(
                                title = title,
                                link = link,
                                category = categoryFromItem(title, link, category),
                                pubDateLabel = formatPubDate(pubDate),
                                publishedAtEpochMs = parsePubDateEpoch(pubDate),
                                summary = extractSummary(description),
                                imageUrl = extractImageUrl(description)
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }
}
