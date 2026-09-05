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
 * Provider model used in aggregation and source filters.
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
    val feedUrl: String
)
