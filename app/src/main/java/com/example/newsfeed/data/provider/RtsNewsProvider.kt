package com.example.newsfeed.data.provider

import com.example.newsfeed.model.RtsArticle
import com.example.newsfeed.util.extractImageUrl
import com.example.newsfeed.util.extractSummary
import com.example.newsfeed.util.formatEpochToDisplay
import com.example.newsfeed.util.formatPubDate
import com.example.newsfeed.util.parsePubDateEpoch
import com.example.newsfeed.util.parseRtsRelativeTimeToEpoch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private const val RTS_FEED_URL = "https://www.rts.ch/info/toute-info/?format=rss/news"

class RtsNewsProvider : NewsProvider {
    override val initialCursor: String? = "/info/page/1"

    override suspend fun fetchLatest(): List<RtsArticle> = withContext(Dispatchers.IO) {
        val connection = URL(RTS_FEED_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
        connection.setRequestProperty("User-Agent", "NewsFeedAndroid/1.0")

        connection.inputStream.use { input ->
            parseRss(input).sortedByDescending { article -> article.publishedAtEpochMs }
        }
    }

    override suspend fun fetchOlder(cursor: String): PagedResult = withContext(Dispatchers.IO) {
        val absoluteUrl = if (cursor.startsWith("http")) {
            cursor
        } else {
            "https://www.rts.ch$cursor"
        }

        val connection = URL(absoluteUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "NewsFeedAndroid/1.0")
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")

        val html = connection.inputStream.bufferedReader().use { it.readText() }
        val doc = Jsoup.parse(html)

        val cards = doc.select(".grid-item .rts-card")
        val parsedItems = mutableListOf<RtsArticle>()

        cards.forEach { card ->
            val href = when {
                card.tagName().equals("a", ignoreCase = true) -> card.attr("href")
                else -> card.selectFirst("a.card-caption")?.attr("href").orEmpty()
            }
            val title = card.selectFirst(".card-title")?.text()?.trim().orEmpty()
            if (href.isBlank() || title.isBlank()) {
                return@forEach
            }

            val absoluteLink = URI("https://www.rts.ch").resolve(href).toString()
            val category = card.selectFirst(".card-bait")?.text()?.trim().orEmpty()
            val timeLabel = card.selectFirst(".card-time")?.text()?.trim().orEmpty()
            val image = card.selectFirst("img.embed-responsive-item")?.attr("src")
                ?.replace("&amp;", "&")
                ?.takeIf { it.isNotBlank() }
            val summary = card.selectFirst(".card-lead")?.text()?.trim().orEmpty()

            val parsedEpoch = parseRtsRelativeTimeToEpoch(timeLabel)
            val pubDateLabel = if (parsedEpoch > 0L) formatEpochToDisplay(parsedEpoch) else timeLabel

            parsedItems += RtsArticle(
                title = title,
                link = absoluteLink,
                category = category,
                pubDateLabel = pubDateLabel,
                publishedAtEpochMs = parsedEpoch,
                summary = summary,
                imageUrl = image
            )
        }

        val nextSrc = doc.selectFirst("a.page-load-more-btn")?.attr("data-loadmore-src")
            ?.takeIf { it.isNotBlank() }

        PagedResult(
            items = parsedItems,
            nextCursor = nextSrc
        )
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
                            "category" -> category = parser.nextText().trim()
                            "pubdate" -> pubDate = parser.nextText().trim()
                            "description" -> description = parser.nextText().trim()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true)) {
                        inItem = false
                        items += RtsArticle(
                            title = title,
                            link = link,
                            category = categoryFromUrl(link, category),
                            pubDateLabel = formatPubDate(pubDate),
                            publishedAtEpochMs = parsePubDateEpoch(pubDate),
                            summary = extractSummary(description),
                            imageUrl = extractImageUrl(description)
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }

    // URL pattern: https://www.rts.ch/<section>/<subsection>/year/article/...
    // Produces e.g. "Info / Culture", "Sport / Football"
    private fun categoryFromUrl(link: String, fallback: String): String {
        val path = link.removePrefix("https://www.rts.ch").trimStart('/').split("?").first()
        val segments = path.split("/").filter { it.isNotBlank() }
        return if (segments.size >= 2) {
            "${segments[0].replaceFirstChar { it.uppercase() }} / ${segments[1].replaceFirstChar { it.uppercase() }}"
        } else {
            fallback
        }
    }
}
