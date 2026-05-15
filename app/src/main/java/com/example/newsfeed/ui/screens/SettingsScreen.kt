package com.example.newsfeed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    showPreview: Boolean,
    onShowPreviewChanged: (Boolean) -> Unit,
    showAllArticleContent: Boolean,
    onShowAllArticleContentChanged: (Boolean) -> Unit,
    hiddenArticleElements: Set<ArticleHideElement>,
    onHiddenArticleElementsChanged: (Set<ArticleHideElement>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Display", style = MaterialTheme.typography.titleMedium)

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show article preview text")
                Switch(checked = showPreview, onCheckedChange = onShowPreviewChanged)
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Article content cleanup")
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show all article content")
                Switch(
                    checked = showAllArticleContent,
                    onCheckedChange = onShowAllArticleContentChanged
                )
            }

            ArticleHideElement.entries.forEach { element ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(element.label)
                    Switch(
                        checked = element in hiddenArticleElements,
                        onCheckedChange = { isEnabled ->
                            val nextElements = if (isEnabled) {
                                hiddenArticleElements + element
                            } else {
                                hiddenArticleElements - element
                            }
                            onHiddenArticleElementsChanged(nextElements)
                        }
                    )
                }
            }
        }
    }
}
