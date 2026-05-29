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
        ),
        "sciencedaily" to RssFeedConfig(
            id = "sciencedaily",
            name = "ScienceDaily",
            url = "https://www.sciencedaily.com/rss/top/science.xml",
            description = "ScienceDaily top science headlines"
        ),
        "plantbasednews" to RssFeedConfig(
            id = "plantbasednews",
            name = "Plant Based News",
            url = "https://plantbasednews.org/feed/",
            description = "Plant Based News on plant-based living and sustainability"
        ),
        "bbc" to RssFeedConfig(
            id = "bbc",
            name = "BBC News",
            url = "https://feeds.bbci.co.uk/news/rss.xml?edition=int",
            description = "BBC News international headlines"
        ),
        "bbc_world" to RssFeedConfig(
            id = "bbc_world",
            name = "BBC World",
            url = "https://feeds.bbci.co.uk/news/world/rss.xml?edition=int",
            description = "BBC world news"
        ),
        "bbc_business" to RssFeedConfig(
            id = "bbc_business",
            name = "BBC Business",
            url = "https://feeds.bbci.co.uk/news/business/rss.xml?edition=int",
            description = "BBC business news"
        ),
        "bbc_technology" to RssFeedConfig(
            id = "bbc_technology",
            name = "BBC Technology",
            url = "https://feeds.bbci.co.uk/news/technology/rss.xml?edition=int",
            description = "BBC technology news"
        ),
        "bbc_science" to RssFeedConfig(
            id = "bbc_science",
            name = "BBC Science & Environment",
            url = "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml?edition=int",
            description = "BBC science and environment news"
        )
    )

    fun getFeedUrl(feedId: String): String? = FEEDS[feedId]?.url
    fun getFeedName(feedId: String): String = FEEDS[feedId]?.name ?: feedId
}
