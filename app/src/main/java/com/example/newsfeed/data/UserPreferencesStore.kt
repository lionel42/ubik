package com.example.newsfeed.data

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey

val Context.dataStore by preferencesDataStore(name = "newsfeed_prefs")

val readLinksKey = stringSetPreferencesKey("read_links")
val filterUnreadOnlyKey = booleanPreferencesKey("filter_unread_only")
val filterHideSportKey = booleanPreferencesKey("filter_hide_sport")
val filterBlacklistTermsKey = stringSetPreferencesKey("filter_blacklist_terms")
val filterBlacklistCatalogKey = stringSetPreferencesKey("filter_blacklist_catalog")
val showPreviewKey = booleanPreferencesKey("show_preview")
val showAllArticleContentKey = booleanPreferencesKey("show_all_article_content")
val hiddenArticleElementsKey = stringSetPreferencesKey("hidden_article_elements")
val enabledSourcesKey = stringSetPreferencesKey("enabled_sources")
/**
 * Stores enabled sub-feed selections as a flat set of "providerId:subfeedId" strings.
 * An absent/empty set means no sub-feed selection → use the provider's default feed.
 */
val enabledSubFeedsKey = stringSetPreferencesKey("enabled_subfeeds")

val defaultBlacklistTerms = setOf("trump", "eurovision")
