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
    val FEEDS: Map<String, RssFeedConfig> = buildMap {
        ProviderDefinitions.all.forEach { definition ->
            val config = RssFeedConfig(
                id = definition.id,
                name = definition.label,
                url = definition.defaultFeedUrl,
                description = definition.description
            )
            put(definition.id, config)
        }
    }

    fun getFeedUrl(feedId: String): String? = FEEDS[feedId]?.url

    fun getFeedName(feedId: String): String = FEEDS[feedId]?.name ?: feedId
}
