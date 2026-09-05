package io.github.ubik.data.provider

import io.github.ubik.model.NewsArticle

data class PagedResult(
    val items: List<NewsArticle>,
    val nextCursor: String?
)

interface NewsProvider {
    val initialCursor: String?

    suspend fun fetchLatest(): List<NewsArticle>

    suspend fun fetchOlder(cursor: String): PagedResult
}
