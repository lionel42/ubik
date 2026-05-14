package com.example.newsfeed.data.provider

import com.example.newsfeed.model.RtsArticle

/**
 * Blast news provider.
 * Simple RSS feed provider for Blast (https://blast-info.fr)
 */
class BlastNewsProvider : SimpleRssProvider(RssFeeds.FEEDS["blast"]?.url ?: "")

/**
 * SRF news provider.
 * SRF RSS has no <category> tags; derive category from URL path segments.
 */
class SrfNewsProvider : SimpleRssProvider(RssFeeds.FEEDS["srf"]?.url ?: "") {
    override fun categoryFromItem(title: String, link: String, xmlCategory: String): String {
        // URL pattern: https://www.srf.ch/<section>/<subsection>/...
        val path = link.removePrefix("https://www.srf.ch").trimStart('/')
        val segments = path.split("/")
        return when {
            segments.size >= 2 -> "${segments[0].replaceFirstChar { it.uppercase() }} / ${segments[1].replaceFirstChar { it.uppercase() }}"
            segments.isNotEmpty() -> segments[0].replaceFirstChar { it.uppercase() }
            else -> ""
        }
    }
}

/**
 * Empa news provider.
 * Uses Google News RSS filtered on Empa communication pages because Empa endpoints are JS/WAF protected.
 */
class EmpaNewsProvider : SimpleRssProvider(RssFeeds.FEEDS["empa"]?.url ?: "")
