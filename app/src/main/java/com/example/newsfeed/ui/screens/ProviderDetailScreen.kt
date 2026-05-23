package com.example.newsfeed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.newsfeed.data.provider.FeedCategory
import com.example.newsfeed.data.provider.ProviderDefinition
import com.example.newsfeed.data.provider.SubFeedDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    provider: ProviderDefinition,
    enabledSubFeeds: Set<String>,
    onSubFeedToggled: (subfeedId: String, enabled: Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(provider.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Provider description
            if (provider.description.isNotBlank()) {
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Metadata chips: category, language, region
            val chips = buildList {
                if (provider.category != FeedCategory.GENERAL) add(provider.category.label)
                if (provider.language.isNotBlank()) add(provider.language.uppercase())
                if (provider.region.isNotBlank()) add(provider.region.uppercase())
            }
            if (chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    chips.forEach { chip -> DetailChip(chip) }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            if (provider.subFeeds.isEmpty()) {
                Text(
                    text = "This provider does not expose sub-feeds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Sub-feeds",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Select the themed feeds you want to follow. When none are selected the default feed is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                // Quick-action row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            provider.subFeeds.forEach { sub ->
                                if (sub.id !in enabledSubFeeds) {
                                    onSubFeedToggled(sub.id, true)
                                }
                            }
                        }
                    ) {
                        Text("Select all")
                    }
                    TextButton(
                        onClick = {
                            provider.subFeeds.forEach { sub ->
                                if (sub.id in enabledSubFeeds) {
                                    onSubFeedToggled(sub.id, false)
                                }
                            }
                        }
                    ) {
                        Text("Clear all")
                    }
                }

                HorizontalDivider()

                provider.subFeeds.forEach { sub ->
                    SubFeedRow(
                        sub = sub,
                        checked = sub.id in enabledSubFeeds,
                        onCheckedChange = { checked -> onSubFeedToggled(sub.id, checked) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SubFeedRow(
    sub: SubFeedDefinition,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(sub.label, style = MaterialTheme.typography.bodyMedium)
                if (sub.category != FeedCategory.GENERAL) {
                    DetailChip(sub.category.label)
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun DetailChip(text: String) {
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
