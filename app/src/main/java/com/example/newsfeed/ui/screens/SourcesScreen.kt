package com.example.newsfeed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsfeed.data.provider.FeedCategory
import com.example.newsfeed.data.provider.ProviderDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    providers: List<ProviderDefinition>,
    enabledSources: Set<String>,
    enabledSubFeeds: Map<String, Set<String>>,
    onSourceEnabledChanged: (id: String, enabled: Boolean) -> Unit,
    onProviderClicked: (providerId: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("News sources") })
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Tap a provider to see details and configure sub-feeds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            HorizontalDivider()
            providers.forEach { provider ->
                SourceRow(
                    provider = provider,
                    enabled = provider.id in enabledSources,
                    enabledSubFeedCount = enabledSubFeeds[provider.id]?.size ?: 0,
                    onEnabledChanged = { onSourceEnabledChanged(provider.id, it) },
                    onClick = { onProviderClicked(provider.id) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SourceRow(
    provider: ProviderDefinition,
    enabled: Boolean,
    enabledSubFeedCount: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: label + metadata tags
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.label, style = MaterialTheme.typography.bodyLarge)
                if (provider.description.isNotBlank()) {
                    Text(
                        text = provider.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (provider.language.isNotBlank() || provider.region.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (provider.category != FeedCategory.GENERAL) {
                            MetadataChip(provider.category.label)
                        }
                        if (provider.language.isNotBlank()) {
                            MetadataChip(provider.language.uppercase())
                        }
                        if (provider.region.isNotBlank()) {
                            MetadataChip(provider.region.uppercase())
                        }
                        if (provider.subFeeds.isNotEmpty()) {
                            val subLabel = if (enabledSubFeedCount > 0)
                                "$enabledSubFeedCount / ${provider.subFeeds.size} sub-feeds"
                            else
                                "${provider.subFeeds.size} sub-feeds"
                            MetadataChip(subLabel)
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Centre: enable/disable toggle (intercept click to avoid triggering row onClick)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )

            Spacer(Modifier.width(8.dp))

            // Right: drill-down chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open provider",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
