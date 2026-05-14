package com.example.newsfeed.model

data class RtsArticle(
    val title: String,
    val link: String,
    val category: String,
    val pubDateLabel: String,
    val publishedAtEpochMs: Long,
    val summary: String,
    val imageUrl: String?,
    val source: String = "News"
)
