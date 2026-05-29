package com.example.newsfeed.data.provider

import com.example.newsfeed.model.RtsArticle
import com.example.newsfeed.util.canonicalArticleKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Aggregated news provider that combines multiple news sources.
 * Fetches from all providers in parallel and merges results.
 * Deduplicates by canonical link and sorts by publication date.
 *
 * [enabledSubFeeds] maps providerId → set of enabled sub-feed IDs.
 * When a provider has sub-feeds and some are selected, one provider instance is
 * created per selected sub-feed URL instead of using the default feed.
 */
class AggregatedNewsProvider(
    private val providers: List<ProviderDefinition> =
        ProviderDefinitions.all,
    private val enabledSources: Set<String> = ProviderDefinitions.allIds,
    private val enabledSubFeeds: Map<String, Set<String>> = emptyMap()
) : NewsProvider {
    override val initialCursor: String? = null

    override suspend fun fetchLatest(): List<RtsArticle> = withContext(Dispatchers.IO) {
        val providerInstances: List<Pair<String, NewsProvider>> = providers
            .filter { def -> def.id in enabledSources }
            .flatMap { def ->
                val selectedSubFeedIds = enabledSubFeeds[def.id]
                if (!selectedSubFeedIds.isNullOrEmpty() && def.subFeeds.isNotEmpty()) {
                    // Instantiate one provider per selected sub-feed
                    def.subFeeds
                        .filter { sub -> sub.id in selectedSubFeedIds }
                        .map { sub -> def.id to SimpleRssProvider(sub.url) }
                } else {
                    listOf(def.id to SimpleRssProvider(def.defaultFeedUrl))
                }
            }

        val tasks = providerInstances.map { (name, provider) ->
            async {
                provider.fetchLatest().map { article ->
                    article.copy(source = name)
                }
            }
        }

        val allResults = tasks.awaitAll().flatten()

        // Deduplicate by canonical link
        val seen = mutableSetOf<String>()
        val deduped = mutableListOf<RtsArticle>()

        for (article in allResults) {
            val key = canonicalArticleKey(article.link)
            if (seen.add(key)) {
                deduped.add(article)
            }
        }

        // Sort by publication date descending
        deduped.sortedByDescending { it.publishedAtEpochMs }
    }

    override suspend fun fetchOlder(cursor: String): PagedResult {
        // Multi-source aggregation doesn't support pagination
        return PagedResult(items = emptyList(), nextCursor = null)
    }
}
