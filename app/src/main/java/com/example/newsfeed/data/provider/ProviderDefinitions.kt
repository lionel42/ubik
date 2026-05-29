package com.example.newsfeed.data.provider

/** Broad thematic category for a provider or sub-feed. */
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
 * A named sub-feed belonging to a provider (e.g. "SRF – Wirtschaft").
 */
data class SubFeedDefinition(
    val id: String,
    val label: String,
    val url: String,
    val category: FeedCategory = FeedCategory.GENERAL
)

/**
 * Central list of available news providers.
 * Add a provider here to make it available in aggregation and source filters.
 *
 * [subFeeds] lists optional themed sub-feeds for providers that expose them.
 * [subFeedFactory] creates a provider instance for a given sub-feed URL; required when
 * [subFeeds] is non-empty.
 */
data class ProviderDefinition(
    val id: String,
    val label: String,
    val description: String = "",
    /** BCP-47 language tag, e.g. "fr", "de", "en". */
    val language: String = "",
    /** Country or region code / display name, e.g. "CH", "FR". */
    val region: String = "",
    /** Primary category of the provider's content. */
    val category: FeedCategory = FeedCategory.GENERAL,
    val subFeeds: List<SubFeedDefinition> = emptyList(),
    val factory: () -> NewsProvider,
    val subFeedFactory: ((url: String) -> NewsProvider)? = null
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
            factory = { RtsNewsProvider() }
        ),
        ProviderDefinition(
            id = "Blast",
            label = "Blast",
            description = "Blast – Le souffle de l'info. Independent French investigative news.",
            language = "fr",
            region = "FR",
            category = FeedCategory.NEWS,
            factory = { BlastNewsProvider() }
        ),
        ProviderDefinition(
            id = "BrokenTest",
            label = "Broken RSS (Test)",
            description = "Deliberately broken RSS feed for testing error handling.",
            language = "en",
            region = "Test",
            category = FeedCategory.GENERAL,
            factory = { SimpleRssProvider("https://invalid-feed.example.invalid/rss.xml") }
        ),
        ProviderDefinition(
            id = "SRF",
            label = "SRF",
            description = "Schweizer Radio und Fernsehen – Swiss public broadcaster in German.",
            language = "de",
            region = "CH",
            category = FeedCategory.NEWS,
            subFeeds = listOf(
                // News
                SubFeedDefinition("srf_news_latest",     "News – Das Neueste",      "https://www.srf.ch/news/bnf/rss/19032223",      FeedCategory.NEWS),
                SubFeedDefinition("srf_news_schweiz",    "News – Schweiz",           "https://www.srf.ch/news/bnf/rss/1890",           FeedCategory.NEWS),
                SubFeedDefinition("srf_news_intl",       "News – International",     "https://www.srf.ch/news/bnf/rss/1922",           FeedCategory.NEWS),
                SubFeedDefinition("srf_news_wirtschaft", "News – Wirtschaft",        "https://www.srf.ch/news/bnf/rss/1926",           FeedCategory.ECONOMY),
                // Sport
                SubFeedDefinition("srf_sport_all",       "Sport – Alle",             "https://www.srf.ch/sport/bnf/rss/718",           FeedCategory.SPORT),
                SubFeedDefinition("srf_sport_fussball",  "Sport – Fussball",         "https://www.srf.ch/sport/bnf/rss/2562",          FeedCategory.SPORT),
                SubFeedDefinition("srf_sport_eishockey", "Sport – Eishockey",        "https://www.srf.ch/sport/bnf/rss/3418",          FeedCategory.SPORT),
                SubFeedDefinition("srf_sport_tennis",    "Sport – Tennis",           "https://www.srf.ch/sport/bnf/rss/2814",          FeedCategory.SPORT),
                SubFeedDefinition("srf_sport_ski",       "Sport – Ski Alpin",        "https://www.srf.ch/sport/bnf/rss/787950",        FeedCategory.SPORT),
                SubFeedDefinition("srf_sport_motorsport","Sport – Motorsport",       "https://www.srf.ch/sport/bnf/rss/2958",          FeedCategory.SPORT),
                // Kultur
                SubFeedDefinition("srf_kultur_all",      "Kultur – Alle",            "https://www.srf.ch/kultur/bnf/rss/454",          FeedCategory.CULTURE),
                SubFeedDefinition("srf_kultur_film",     "Kultur – Film & Serien",   "https://www.srf.ch/kultur/bnf/rss/8726",         FeedCategory.CULTURE),
                SubFeedDefinition("srf_kultur_literatur","Kultur – Literatur",       "https://www.srf.ch/kultur/bnf/rss/8762",         FeedCategory.CULTURE),
                SubFeedDefinition("srf_kultur_musik",    "Kultur – Musik",           "https://www.srf.ch/kultur/bnf/rss/8738",         FeedCategory.CULTURE),
                SubFeedDefinition("srf_kultur_kunst",    "Kultur – Kunst",           "https://www.srf.ch/kultur/bnf/rss/8774",         FeedCategory.CULTURE),
                // Wissen
                SubFeedDefinition("srf_wissen_all",      "Wissen – Alle",            "https://www.srf.ch/bnf/rss/630",                 FeedCategory.SCIENCE),
                SubFeedDefinition("srf_wissen_gesund",   "Wissen – Gesundheit",      "https://www.srf.ch/bnf/rss/19919909",            FeedCategory.SCIENCE),
                SubFeedDefinition("srf_wissen_nachhal",  "Wissen – Nachhaltigkeit",  "https://www.srf.ch/bnf/rss/19920002",            FeedCategory.SCIENCE),
                SubFeedDefinition("srf_wissen_technik",  "Wissen – Technik",         "https://www.srf.ch/bnf/rss/19920122",            FeedCategory.TECHNOLOGY)
            ),
            factory = { SrfNewsProvider() },
            subFeedFactory = { url -> SrfNewsProvider(url) }
        ),
        ProviderDefinition(
            id = "Empa",
            label = "Empa",
            description = "Empa – Swiss Federal Laboratories for Materials Science and Technology.",
            language = "en",
            region = "CH",
            category = FeedCategory.SCIENCE,
            factory = { EmpaNewsProvider() }
        ),
        ProviderDefinition(
            id = "ScienceDaily",
            label = "ScienceDaily",
            description = "ScienceDaily top science headlines.",
            language = "en",
            region = "Global",
            category = FeedCategory.SCIENCE,
            subFeeds = listOf(
                SubFeedDefinition("sd_all", "All News", "https://www.sciencedaily.com/rss/all.xml", FeedCategory.NEWS),
                SubFeedDefinition("sd_top", "Top News", "https://www.sciencedaily.com/rss/top.xml", FeedCategory.NEWS),
                SubFeedDefinition("sd_top_science", "Top Science", "https://www.sciencedaily.com/rss/top/science.xml", FeedCategory.SCIENCE),
                SubFeedDefinition("sd_top_health", "Top Health", "https://www.sciencedaily.com/rss/top/health.xml", FeedCategory.SCIENCE),
                SubFeedDefinition("sd_top_technology", "Top Technology", "https://www.sciencedaily.com/rss/top/technology.xml", FeedCategory.TECHNOLOGY),
                SubFeedDefinition("sd_top_environment", "Top Environment", "https://www.sciencedaily.com/rss/top/environment.xml", FeedCategory.SCIENCE),
                SubFeedDefinition("sd_top_society", "Top Society", "https://www.sciencedaily.com/rss/top/society.xml", FeedCategory.GENERAL),
                SubFeedDefinition("sd_strange_offbeat", "Strange & Offbeat", "https://www.sciencedaily.com/rss/strange_offbeat.xml", FeedCategory.GENERAL),
                SubFeedDefinition("sd_most_popular", "Most Popular", "https://www.sciencedaily.com/rss/most_popular.xml", FeedCategory.GENERAL)
            ),
            factory = { ScienceDailyNewsProvider() },
            subFeedFactory = { url -> ScienceDailyNewsProvider(url) }
        ),
        ProviderDefinition(
            id = "PlantBasedNews",
            label = "Plant Based News",
            description = "Plant Based News – plant-based living and sustainability news.",
            language = "en",
            region = "Global",
            category = FeedCategory.NEWS,
            factory = { PlantBasedNewsProvider() }
        )
    )

    val allIds: Set<String> = all.map { definition -> definition.id }.toSet()
}
