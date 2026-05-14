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
val articleFocusModeKey = booleanPreferencesKey("article_focus_mode")
val hideBottomArticlesKey = booleanPreferencesKey("hide_bottom_articles")
val hidePromoContentKey = booleanPreferencesKey("hide_promo_content")
val enabledSourcesKey = stringSetPreferencesKey("enabled_sources")

val defaultBlacklistTerms = setOf("trump", "eurovision")
