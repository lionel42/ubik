package com.example.newsfeed.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.newsfeed.data.dataStore
import com.example.newsfeed.data.defaultBlacklistTerms
import com.example.newsfeed.data.enabledSourcesKey
import com.example.newsfeed.data.filterBlacklistCatalogKey
import com.example.newsfeed.data.filterBlacklistTermsKey
import com.example.newsfeed.data.filterHideSportKey
import com.example.newsfeed.data.filterUnreadOnlyKey
import com.example.newsfeed.data.provider.AggregatedNewsProvider
import com.example.newsfeed.data.provider.NewsProvider
import com.example.newsfeed.data.provider.ProviderDefinitions
import com.example.newsfeed.data.readLinksKey
import com.example.newsfeed.data.articleFocusModeKey
import com.example.newsfeed.data.hideBottomArticlesKey
import com.example.newsfeed.data.hidePromoContentKey
import com.example.newsfeed.data.showPreviewKey
import com.example.newsfeed.model.RtsArticle
import com.example.newsfeed.ui.components.NewsList
import com.example.newsfeed.ui.screens.ArticleReaderScreen
import com.example.newsfeed.ui.screens.FiltersScreen
import com.example.newsfeed.ui.screens.SourceToggleItem
import com.example.newsfeed.ui.screens.SettingsScreen
import com.example.newsfeed.util.canonicalArticleKey
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class AppScreen {
    FEED,
    READER,
    SETTINGS,
    FILTERS
}

private sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val items: List<RtsArticle>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtsNewsApp(defaultProvider: NewsProvider? = null) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

    val readLinks by context.dataStore.data
        .map { preferences -> preferences[readLinksKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val filterUnreadOnly by context.dataStore.data
        .map { preferences -> preferences[filterUnreadOnlyKey] ?: false }
        .collectAsState(initial = false)
    val filterHideSport by context.dataStore.data
        .map { preferences -> preferences[filterHideSportKey] ?: false }
        .collectAsState(initial = false)
    val filterBlacklistTerms by context.dataStore.data
        .map { preferences -> preferences[filterBlacklistTermsKey] ?: defaultBlacklistTerms }
        .collectAsState(initial = defaultBlacklistTerms)
    val filterBlacklistCatalog by context.dataStore.data
        .map { preferences -> preferences[filterBlacklistCatalogKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val showPreview by context.dataStore.data
        .map { preferences -> preferences[showPreviewKey] ?: true }
        .collectAsState(initial = true)
    val articleFocusMode by context.dataStore.data
        .map { preferences -> preferences[articleFocusModeKey] ?: true }
        .collectAsState(initial = true)
    val hideBottomArticles by context.dataStore.data
        .map { preferences -> preferences[hideBottomArticlesKey] ?: false }
        .collectAsState(initial = false)
    val hidePromoContent by context.dataStore.data
        .map { preferences -> preferences[hidePromoContentKey] ?: false }
        .collectAsState(initial = false)

    val sourceDefinitions = remember { ProviderDefinitions.all }
    val allSourceIds = remember { ProviderDefinitions.allIds }
    val sourceToggles = remember(sourceDefinitions) {
        sourceDefinitions.map { definition ->
            SourceToggleItem(id = definition.id, label = definition.label)
        }
    }
    val enabledSources by context.dataStore.data
        .map { preferences ->
            val saved = preferences[enabledSourcesKey]
            when {
                saved.isNullOrEmpty() -> allSourceIds
                else -> saved.intersect(allSourceIds).ifEmpty { allSourceIds }
            }
        }
        .collectAsState(initial = allSourceIds)

    val provider = remember(enabledSources) {
        defaultProvider ?: AggregatedNewsProvider(enabledSources = enabledSources)
    }

    var uiState: FeedUiState by remember { mutableStateOf(FeedUiState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var nextCursor by remember { mutableStateOf(provider.initialCursor) }
    var selectedArticle by remember { mutableStateOf<RtsArticle?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.FEED) }
    var previousScreen by remember { mutableStateOf(AppScreen.FEED) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun refresh(byPull: Boolean = false) {
        scope.launch {
            if (hasLoadedOnce && byPull) {
                isRefreshing = true
            } else {
                uiState = FeedUiState.Loading
            }

            try {
                val feed = provider.fetchLatest()
                uiState = FeedUiState.Success(feed)
                hasLoadedOnce = true
                nextCursor = provider.initialCursor
            } catch (e: Exception) {
                uiState = FeedUiState.Error(e.message ?: "Unknown error")
            } finally {
                isRefreshing = false
                isLoadingMore = false
            }
        }
    }

    fun loadMoreIfNeeded() {
        val cursor = nextCursor ?: return
        if (isLoadingMore) return

        scope.launch {
            isLoadingMore = true
            try {
                val pageResult = provider.fetchOlder(cursor)
                val currentItems = (uiState as? FeedUiState.Success)?.items.orEmpty()
                val existingKeys = currentItems
                    .asSequence()
                    .map { article -> canonicalArticleKey(article.link) }
                    .toHashSet()
                val appended = pageResult.items.filter { article ->
                    existingKeys.add(canonicalArticleKey(article.link))
                }
                uiState = FeedUiState.Success(currentItems + appended)
                nextCursor = pageResult.nextCursor
            } catch (_: Exception) {
                nextCursor = null
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun markAsRead(link: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                val current = preferences[readLinksKey] ?: emptySet()
                preferences[readLinksKey] = current + link
            }
        }
    }

    fun saveBooleanSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    fun saveStringSetSetting(key: Preferences.Key<Set<String>>, values: Set<String>) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[key] = values
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    when (currentScreen) {
        AppScreen.READER -> {
            val article = selectedArticle
            if (article == null) {
                currentScreen = AppScreen.FEED
            } else {
                ArticleReaderScreen(
                    article = article,
                    darkMode = isDarkMode,
                    articleFocusMode = articleFocusMode,
                    hideBottomArticles = hideBottomArticles,
                    hidePromoContent = hidePromoContent,
                    onBack = { currentScreen = AppScreen.FEED },
                    onOpenSettings = {
                        previousScreen = AppScreen.READER
                        currentScreen = AppScreen.SETTINGS
                    }
                )
            }
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                showPreview = showPreview,
                onShowPreviewChanged = { saveBooleanSetting(showPreviewKey, it) },
                articleFocusMode = articleFocusMode,
                onArticleFocusModeChanged = { saveBooleanSetting(articleFocusModeKey, it) },
                hideBottomArticles = hideBottomArticles,
                onHideBottomArticlesChanged = { saveBooleanSetting(hideBottomArticlesKey, it) },
                hidePromoContent = hidePromoContent,
                onHidePromoContentChanged = { saveBooleanSetting(hidePromoContentKey, it) },
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.FILTERS -> {
            FiltersScreen(
                unreadOnly = filterUnreadOnly,
                hideSport = filterHideSport,
                blacklistCatalog = filterBlacklistCatalog,
                blacklistTerms = filterBlacklistTerms,
                sourceToggles = sourceToggles,
                enabledSources = enabledSources,
                onUnreadOnlyChanged = { saveBooleanSetting(filterUnreadOnlyKey, it) },
                onHideSportChanged = { saveBooleanSetting(filterHideSportKey, it) },
                onBlacklistCatalogChanged = { saveStringSetSetting(filterBlacklistCatalogKey, it) },
                onBlacklistTermsChanged = { saveStringSetSetting(filterBlacklistTermsKey, it) },
                onSourceEnabledChanged = { sourceId, isEnabled ->
                    val nextEnabled = if (isEnabled) {
                        enabledSources + sourceId
                    } else {
                        enabledSources - sourceId
                    }
                    saveStringSetSetting(enabledSourcesKey, nextEnabled)
                },
                onBack = { currentScreen = AppScreen.FEED }
            )
        }

        AppScreen.FEED -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("News Feed") },
                        actions = {
                            IconButton(onClick = { currentScreen = AppScreen.FILTERS }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = "Filters"
                                )
                            }
                            IconButton(onClick = {
                                previousScreen = AppScreen.FEED
                                currentScreen = AppScreen.SETTINGS
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Surface(modifier = Modifier.padding(innerPadding)) {
                    when (val state = uiState) {
                        FeedUiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Loading news...",
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }

                        is FeedUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "Unable to load news feed")
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Button(
                                    onClick = { refresh() },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }

                        is FeedUiState.Success -> {
                            val filteredItems = state.items.filter { article ->
                                val filteredBySport = filterHideSport && (
                                    article.link.contains("/sport/", ignoreCase = true) ||
                                        article.category.contains("sport", ignoreCase = true)
                                    )
                                val filteredByBlacklist = filterBlacklistTerms.any { term ->
                                    term.isNotBlank() && article.title.contains(term, ignoreCase = true)
                                }
                                val filteredByUnread = filterUnreadOnly && (article.link in readLinks)

                                !filteredBySport && !filteredByBlacklist && !filteredByUnread
                            }

                            NewsList(
                                items = filteredItems,
                                isRefreshing = isRefreshing,
                                isLoadingMore = isLoadingMore,
                                canLoadMore = nextCursor != null,
                                onRefresh = { refresh(byPull = true) },
                                onLoadMore = { loadMoreIfNeeded() },
                                readLinks = readLinks,
                                showPreview = showPreview,
                                listState = listState,
                                onArticleClick = { article ->
                                    markAsRead(article.link)
                                    selectedArticle = article
                                    currentScreen = AppScreen.READER
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
