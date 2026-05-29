package com.example.newsfeed.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.newsfeed.data.dataStore
import com.example.newsfeed.data.defaultCydoniaNames
import com.example.newsfeed.data.defaultBlacklistTerms
import com.example.newsfeed.data.enabledSourcesKey
import com.example.newsfeed.data.filterBlacklistCatalogKey
import com.example.newsfeed.data.filterBlacklistTermsKey
import com.example.newsfeed.data.filterCydoniaKey
import com.example.newsfeed.data.filterHideSportKey
import com.example.newsfeed.data.filterUnreadOnlyKey
import com.example.newsfeed.data.provider.ProviderDefinition
import com.example.newsfeed.data.provider.AggregatedNewsProvider
import com.example.newsfeed.data.provider.NewsProvider
import com.example.newsfeed.data.provider.ProviderDefinitions
import com.example.newsfeed.data.provider.SimpleRssProvider
import com.example.newsfeed.data.readLinksKey
import com.example.newsfeed.data.hiddenArticleElementsKey
import com.example.newsfeed.data.showAllArticleContentKey
import com.example.newsfeed.data.showPreviewKey
import com.example.newsfeed.model.RtsArticle
import com.example.newsfeed.ui.components.NewsList
import com.example.newsfeed.ui.components.UbikLogo
import com.example.newsfeed.ui.screens.ArticleHideElement
import com.example.newsfeed.ui.screens.ArticleReaderScreen
import com.example.newsfeed.ui.screens.FiltersScreen
import com.example.newsfeed.ui.screens.ProviderDetailScreen
import com.example.newsfeed.ui.screens.SettingsScreen
import com.example.newsfeed.ui.screens.SourcesScreen
import com.example.newsfeed.util.canonicalArticleKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val STALE_THRESHOLD_MS = 10 * 60 * 1000L

private enum class AppScreen {
    FEED,
    READER,
    SETTINGS,
    FILTERS,
    SOURCES,
    PROVIDER_DETAIL
}

private sealed interface FeedUiState {
    data class Loading(val items: List<RtsArticle> = emptyList()) : FeedUiState
    data class Success(val items: List<RtsArticle>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

private data class FeedLoadTarget(
    val key: String,
    val sourceLabel: String,
    val provider: NewsProvider
)

private fun mergeFeedItems(articles: Collection<RtsArticle>): List<RtsArticle> {
    val seenKeys = mutableSetOf<String>()
    val deduped = mutableListOf<RtsArticle>()

    for (article in articles.sortedByDescending { it.publishedAtEpochMs }) {
        val articleKey = canonicalArticleKey(article.link)
        if (seenKeys.add(articleKey)) {
            deduped.add(article)
        }
    }

    return deduped
}

private fun buildFeedLoadTargets(
    providers: List<ProviderDefinition>,
    enabledSources: Set<String>
): List<FeedLoadTarget> {
    return providers
        .filter { definition -> definition.id in enabledSources }
        .map { definition ->
            FeedLoadTarget(
                key = definition.id,
                sourceLabel = definition.label,
                provider = SimpleRssProvider(definition.defaultFeedUrl)
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtsNewsApp(defaultProvider: NewsProvider? = null) {
    val context = LocalContext.current
    val sharedReaderWebView = remember(context) {
        WebView(context).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }
    }

    DisposableEffect(sharedReaderWebView) {
        onDispose {
            sharedReaderWebView.stopLoading()
            sharedReaderWebView.destroy()
        }
    }

    val readLinks by context.dataStore.data
        .map { preferences -> preferences[readLinksKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val filterUnreadOnly by context.dataStore.data
        .map { preferences -> preferences[filterUnreadOnlyKey] ?: false }
        .collectAsState(initial = false)
    val filterHideSport by context.dataStore.data
        .map { preferences -> preferences[filterHideSportKey] ?: false }
        .collectAsState(initial = false)
    val filterCydonia by context.dataStore.data
        .map { preferences -> preferences[filterCydoniaKey] ?: false }
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
    val showAllArticleContent by context.dataStore.data
        .map { preferences -> preferences[showAllArticleContentKey] ?: false }
        .collectAsState(initial = false)
    val hiddenArticleElements by context.dataStore.data
        .map { preferences ->
            preferences[hiddenArticleElementsKey]?.let { savedElements ->
                savedElements
                    .mapNotNull { storageKey -> ArticleHideElement.fromStorageKey(storageKey) }
                    .toSet()
            } ?: ArticleHideElement.defaultHidden
        }
        .collectAsState(initial = ArticleHideElement.defaultHidden)

    val sourceDefinitions = remember { ProviderDefinitions.all }
    val allSourceIds = remember { ProviderDefinitions.allIds }
    // null = DataStore hasn't emitted yet; non-null = real saved value
    val enabledSourcesLoaded: Set<String>? by context.dataStore.data
        .map { preferences ->
            val saved = preferences[enabledSourcesKey]
            // null means first run/no saved preference yet -> all sources enabled by default.
            // A saved empty set is a valid user choice and must stay empty.
            if (saved == null) allSourceIds else saved.intersect(allSourceIds)
        }
        .collectAsState(initial = null)
    val enabledSources = enabledSourcesLoaded ?: allSourceIds
    var selectedProviderForDetail by remember { mutableStateOf<String?>(null) }

    val provider = remember(enabledSources) {
        defaultProvider ?: AggregatedNewsProvider(
            enabledSources = enabledSources
        )
    }

    var uiState: FeedUiState by remember { mutableStateOf(FeedUiState.Loading()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var lastRefreshTimeMs by remember { mutableStateOf(0L) }
    var refreshJob: Job? by remember { mutableStateOf(null) }
    var nextCursor by remember { mutableStateOf(provider.initialCursor) }
    var selectedArticle by remember { mutableStateOf<RtsArticle?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.FEED) }
    var previousScreen by remember { mutableStateOf(AppScreen.FEED) }
    var showFilteredArticles by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun hiddenReasonsForArticle(article: RtsArticle): List<String> {
            val reasons = mutableListOf<String>()
            val articleText = listOf(article.title, article.summary, article.category)
                .joinToString(" ")

            val filteredBySport = filterHideSport && (
                article.link.contains("/sport/", ignoreCase = true) ||
                    article.category.contains("sport", ignoreCase = true)
                )
            if (filteredBySport) reasons += "sport"

            val matchedCydoniaName = if (filterCydonia) {
                defaultCydoniaNames.firstOrNull { name ->
                    name.isNotBlank() && articleText.contains(name, ignoreCase = true)
                }
            } else {
                null
            }
            if (matchedCydoniaName != null) reasons += "cydonia:$matchedCydoniaName"

            val matchedBlacklistTerm = filterBlacklistTerms.firstOrNull { term ->
                term.isNotBlank() && article.title.contains(term, ignoreCase = true)
            }
            if (matchedBlacklistTerm != null) reasons += "blacklist:$matchedBlacklistTerm"

            val filteredByUnread = filterUnreadOnly && (article.link in readLinks)
            if (filteredByUnread) reasons += "already read"

            return reasons
    }

    fun visibleArticles(items: List<RtsArticle>): List<RtsArticle> {
        return if (showFilteredArticles) items else items.filter { article -> hiddenReasonsForArticle(article).isEmpty() }
    }

    fun hiddenArticleReasons(items: List<RtsArticle>): Map<String, String> {
        if (!showFilteredArticles) return emptyMap()

        val reasonsByKey = linkedMapOf<String, MutableSet<String>>()
        items.forEach { article ->
            val reasons = hiddenReasonsForArticle(article)
            if (reasons.isNotEmpty()) {
                val key = canonicalArticleKey(article.link)
                val bucket = reasonsByKey.getOrPut(key) { linkedSetOf() }
                bucket.addAll(reasons)
            }
        }

        return reasonsByKey.mapValues { (_, reasons) -> reasons.joinToString(separator = ", ") }
    }

    fun refresh(byPull: Boolean = false) {
        val previousJob = refreshJob
        refreshJob = scope.launch {
            previousJob?.cancelAndJoin()

            val currentItems = when (val state = uiState) {
                is FeedUiState.Loading -> state.items
                is FeedUiState.Success -> state.items
                is FeedUiState.Error -> emptyList()
            }

            if (hasLoadedOnce && byPull) {
                isRefreshing = true
            } else {
                uiState = FeedUiState.Loading(currentItems)
            }

            try {
                if (defaultProvider != null) {
                    val feed = defaultProvider.fetchLatest()
                    uiState = FeedUiState.Success(feed)
                    nextCursor = defaultProvider.initialCursor
                    hasLoadedOnce = true
                } else {
                    val loadTargets = buildFeedLoadTargets(
                        providers = sourceDefinitions,
                        enabledSources = enabledSources
                    )

                    if (loadTargets.isEmpty()) {
                        uiState = FeedUiState.Success(emptyList())
                        nextCursor = null
                        hasLoadedOnce = true
                    } else {
                        val loadedByTarget = linkedMapOf<String, List<RtsArticle>>()
                        var failedTargets = 0
                        val mutex = Mutex()

                        suspend fun publishSnapshot() {
                            val combined = visibleArticles(mergeFeedItems(loadedByTarget.values.flatten()))
                            val remaining = loadTargets.size - loadedByTarget.size - failedTargets
                            uiState = if (remaining > 0) {
                                FeedUiState.Loading(combined)
                            } else if (combined.isNotEmpty()) {
                                FeedUiState.Success(combined)
                            } else {
                                FeedUiState.Error("Unable to load news feed")
                            }
                        }

                        coroutineScope {
                            loadTargets.map { target ->
                                launch(Dispatchers.IO) {
                                    try {
                                        val items = target.provider.fetchLatest().map { article ->
                                            article.copy(source = target.sourceLabel)
                                        }
                                        mutex.withLock {
                                            loadedByTarget[target.key] = items
                                            publishSnapshot()
                                        }
                                    } catch (_: Exception) {
                                        mutex.withLock {
                                            failedTargets += 1
                                            publishSnapshot()
                                        }
                                    }
                                }
                            }.joinAll()
                        }

                        hasLoadedOnce = true
                        nextCursor = null
                    }
                }
            } catch (e: Exception) {
                uiState = FeedUiState.Error(e.message ?: "Unknown error")
            } finally {
                isRefreshing = false
                isLoadingMore = false
                lastRefreshTimeMs = System.currentTimeMillis()
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

    // On first composition (and whenever sources change), wait for DataStore to have
    // emitted the real saved preferences before loading, so startup matches manual reload.
    LaunchedEffect(defaultProvider ?: provider) {
        if (defaultProvider == null && enabledSourcesLoaded == null) {
            snapshotFlow { enabledSourcesLoaded }
                .filter { loaded -> loaded != null }
                .first()
        }
        refresh()
    }

    // Refresh on app resume if the feed is stale, using the same refresh() as manual reload.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRefresh by rememberUpdatedState(newValue = { refresh(byPull = true) })
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME
                && hasLoadedOnce
                && System.currentTimeMillis() - lastRefreshTimeMs > STALE_THRESHOLD_MS
            ) {
                scope.launch { currentRefresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (currentScreen) {
        AppScreen.READER -> {
            val article = selectedArticle
            if (article == null) {
                currentScreen = AppScreen.FEED
            } else {
                ArticleReaderScreen(
                    webView = sharedReaderWebView,
                    article = article,
                    hiddenElements = if (showAllArticleContent) emptySet() else hiddenArticleElements,
                    onBack = {
                        sharedReaderWebView.loadData(
                            "<html><body style='display:flex;align-items:center;justify-content:center;height:100vh;margin:0;font-family:sans-serif;color:#888;'>Loading...</body></html>",
                            "text/html",
                            "UTF-8"
                        )
                        currentScreen = AppScreen.FEED
                    },
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
                showAllArticleContent = showAllArticleContent,
                onShowAllArticleContentChanged = { saveBooleanSetting(showAllArticleContentKey, it) },
                hiddenArticleElements = hiddenArticleElements,
                onHiddenArticleElementsChanged = { elements ->
                    saveStringSetSetting(
                        hiddenArticleElementsKey,
                        elements.map { element -> element.storageKey }.toSet()
                    )
                },
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.SOURCES -> {
            SourcesScreen(
                providers = sourceDefinitions,
                enabledSources = enabledSources,
                onSourceEnabledChanged = { sourceId, isEnabled ->
                    val nextEnabled = if (isEnabled) enabledSources + sourceId else enabledSources - sourceId
                    saveStringSetSetting(enabledSourcesKey, nextEnabled)
                },
                onProviderClicked = { providerId ->
                    selectedProviderForDetail = providerId
                    currentScreen = AppScreen.PROVIDER_DETAIL
                },
                onBack = { currentScreen = AppScreen.FEED }
            )
        }

        AppScreen.PROVIDER_DETAIL -> {
            val providerId = selectedProviderForDetail
            val providerDef = sourceDefinitions.firstOrNull { it.id == providerId }
            if (providerDef == null) {
                currentScreen = AppScreen.SOURCES
            } else {
                ProviderDetailScreen(
                    provider = providerDef,
                    onBack = { currentScreen = AppScreen.SOURCES }
                )
            }
        }

        AppScreen.FILTERS -> {
            FiltersScreen(
                unreadOnly = filterUnreadOnly,
                hideSport = filterHideSport,
                cydonia = filterCydonia,
                blacklistCatalog = filterBlacklistCatalog,
                blacklistTerms = filterBlacklistTerms,
                onUnreadOnlyChanged = { saveBooleanSetting(filterUnreadOnlyKey, it) },
                onHideSportChanged = { saveBooleanSetting(filterHideSportKey, it) },
                onCydoniaChanged = { saveBooleanSetting(filterCydoniaKey, it) },
                onBlacklistCatalogChanged = { saveStringSetSetting(filterBlacklistCatalogKey, it) },
                onBlacklistTermsChanged = { saveStringSetSetting(filterBlacklistTermsKey, it) },
                onBack = { currentScreen = AppScreen.FEED }
            )
        }

        AppScreen.FEED -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            UbikLogo(
                                onClick = {
                                    scope.launch {
                                        listState.scrollToItem(0)
                                    }
                                }
                            )
                        },
                        actions = {
                            IconButton(onClick = { currentScreen = AppScreen.SOURCES }) {
                                Icon(
                                    imageVector = Icons.Filled.Newspaper,
                                    contentDescription = "Sources"
                                )
                            }
                            IconButton(onClick = { currentScreen = AppScreen.FILTERS }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = "Filters"
                                )
                            }
                            IconButton(onClick = { showFilteredArticles = !showFilteredArticles }) {
                                Icon(
                                    imageVector = if (showFilteredArticles) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = if (showFilteredArticles) {
                                        "Hide filtered articles"
                                    } else {
                                        "Show filtered articles"
                                    },
                                    tint = if (showFilteredArticles) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalContentColor.current
                                    }
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
                        is FeedUiState.Loading -> {
                            val displayItems = visibleArticles(state.items)
                            val hiddenReasons = hiddenArticleReasons(state.items)
                            if (displayItems.isEmpty()) {
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
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    NewsList(
                                        modifier = Modifier.fillMaxSize(),
                                        items = displayItems,
                                        isRefreshing = isRefreshing,
                                        isLoadingMore = isLoadingMore,
                                        canLoadMore = nextCursor != null,
                                        onRefresh = { refresh(byPull = true) },
                                        onLoadMore = { loadMoreIfNeeded() },
                                        readLinks = readLinks,
                                        hiddenArticleReasons = hiddenReasons,
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
                            val displayItems = visibleArticles(state.items)
                            val hiddenReasons = hiddenArticleReasons(state.items)

                            NewsList(
                                modifier = Modifier.fillMaxSize(),
                                items = displayItems,
                                isRefreshing = isRefreshing,
                                isLoadingMore = isLoadingMore,
                                canLoadMore = nextCursor != null,
                                onRefresh = { refresh(byPull = true) },
                                onLoadMore = { loadMoreIfNeeded() },
                                readLinks = readLinks,
                                hiddenArticleReasons = hiddenReasons,
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
