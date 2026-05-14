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
 */
class AggregatedNewsProvider(
    private val providers: List<Pair<String, NewsProvider>> =
        ProviderDefinitions.all.map { definition ->
            definition.id to definition.factory()
        },
    private val enabledSources: Set<String> = ProviderDefinitions.allIds
) : NewsProvider {
    override val initialCursor: String? = null

    override suspend fun fetchLatest(): List<RtsArticle> = withContext(Dispatchers.IO) {
        val tasks = providers
            .filter { (name, _) -> name in enabledSources }
            .map { (name, provider) ->
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
