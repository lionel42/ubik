package com.example.newsfeed.data.provider

/**
 * Registry of RSS feeds for simple feed providers.
 * Each feed is identified by a unique key and includes its URL and metadata.
 */
data class RssFeedConfig(
    val id: String,
    val name: String,
    val url: String,
    val description: String
)

object RssFeeds {
    val FEEDS = mapOf(
        "rts" to RssFeedConfig(
            id = "rts",
            name = "RTS News",
            url = "https://www.rts.ch/info/toute-info/?format=rss/news",
            description = "Swiss French radio-televised news"
        ),
        "blast" to RssFeedConfig(
            id = "blast",
            name = "Blast",
            url = "https://api.blast-info.fr/rss.xml",
            description = "Blast - Le souffle de l'info"
        ),
        "srf" to RssFeedConfig(
            id = "srf",
            name = "SRF News",
            url = "https://www.srf.ch/news/bnf/rss/19032223",
            description = "Swiss German radio-televised news"
        ),
        "empa" to RssFeedConfig(
            id = "empa",
            name = "Empa News",
            url = "https://news.google.com/rss/search?q=site:empa.ch/web/s604&hl=en-CH&gl=CH&ceid=CH:en",
            description = "Empa news via Google News RSS fallback"
        )
    )

    fun getFeedUrl(feedId: String): String? = FEEDS[feedId]?.url
    fun getFeedName(feedId: String): String = FEEDS[feedId]?.name ?: feedId
}
