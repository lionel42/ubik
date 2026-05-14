package com.example.newsfeed.data.provider

import com.example.newsfeed.model.RtsArticle

data class PagedResult(
    val items: List<RtsArticle>,
    val nextCursor: String?
)

interface NewsProvider {
    val initialCursor: String?

    suspend fun fetchLatest(): List<RtsArticle>

    suspend fun fetchOlder(cursor: String): PagedResult
}
