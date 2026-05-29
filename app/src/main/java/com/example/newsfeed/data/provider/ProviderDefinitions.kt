package com.example.newsfeed.data.provider

/** Broad thematic category for a provider. */
enum class FeedCategory(val label: String) {
    NEWS("News"),
    SPORT("Sport"),
    CULTURE("Culture"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology"),
    ECONOMY("Economy"),
    GENERAL("General")
}

/**
 * Central list of available news providers.
 * Add a provider here to make it available in aggregation and source filters.
 */
data class ProviderDefinition(
    val id: String,
    val label: String,
    val description: String = "",
    /** BCP-47 language tag, e.g. "fr", "de", "en". */
    val language: String = "",
    /** Country or region code / display name, e.g. "CH", "FR". */
    val region: String = "",
    /** Primary category of the provider content. */
    val category: FeedCategory = FeedCategory.GENERAL,
    val defaultFeedUrl: String
)

object ProviderDefinitions {
    val all = listOf(
        ProviderDefinition(
            id = "RTS",
            label = "RTS",
            description = "Radio Télévision Suisse – Swiss public broadcaster in French.",
            language = "fr",
            region = "CH",
            category = FeedCategory.NEWS,
            defaultFeedUrl = "https://www.rts.ch/info/toute-info/?format=rss/news"
        ),
        ProviderDefinition(
            id = "Blast",
            label = "Blast",
            description = "Blast – Le souffle de l'info. Independent French investigative news.",
            language = "fr",
            region = "FR",
            category = FeedCategory.NEWS,
            defaultFeedUrl = "https://api.blast-info.fr/rss.xml"
        ),
        ProviderDefinition(
            id = "BrokenTest",
            label = "Broken RSS (Test)",
            description = "Deliberately broken RSS feed for testing error handling.",
            language = "en",
            region = "Test",
            category = FeedCategory.GENERAL,
            defaultFeedUrl = "https://invalid-feed.example.invalid/rss.xml"
        ),
        ProviderDefinition(
            id = "SRF",
            label = "SRF",
            description = "Schweizer Radio und Fernsehen – Swiss public broadcaster in German.",
            language = "de",
            region = "CH",
            category = FeedCategory.NEWS,
            defaultFeedUrl = "https://www.srf.ch/news/bnf/rss/19032223"
        ),
        ProviderDefinition(
            id = "Empa",
            label = "Empa",
            description = "Empa – Swiss Federal Laboratories for Materials Science and Technology.",
            language = "en",
            region = "CH",
            category = FeedCategory.SCIENCE,
            defaultFeedUrl = "https://news.google.com/rss/search?q=site:empa.ch/web/s604&hl=en-CH&gl=CH&ceid=CH:en"
        ),
        ProviderDefinition(
            id = "ScienceDaily",
            label = "ScienceDaily",
            description = "ScienceDaily top science headlines.",
            language = "en",
            region = "Global",
            category = FeedCategory.SCIENCE,
            defaultFeedUrl = "https://www.sciencedaily.com/rss/top/science.xml"
        ),
        ProviderDefinition(
            id = "PlantBasedNews",
            label = "Plant Based News",
            description = "Plant Based News – plant-based living and sustainability news.",
            language = "en",
            region = "Global",
            category = FeedCategory.NEWS,
            defaultFeedUrl = "https://plantbasednews.org/feed/"
        )
    )

    val allIds: Set<String> = all.map { definition -> definition.id }.toSet()
}
