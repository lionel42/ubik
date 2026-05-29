package com.example.newsfeed.data.provider

import com.example.newsfeed.model.NewsArticle

data class PagedResult(
    val items: List<NewsArticle>,
    val nextCursor: String?
)

interface NewsProvider {
    val initialCursor: String?

    suspend fun fetchLatest(): List<NewsArticle>

    suspend fun fetchOlder(cursor: String): PagedResult
}
