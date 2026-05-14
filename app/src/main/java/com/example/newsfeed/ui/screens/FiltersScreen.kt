package com.example.newsfeed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.newsfeed.data.defaultBlacklistTerms
import java.util.Locale

data class SourceToggleItem(
    val id: String,
    val label: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FiltersScreen(
    unreadOnly: Boolean,
    hideSport: Boolean,
    blacklistCatalog: Set<String>,
    blacklistTerms: Set<String>,
    sourceToggles: List<SourceToggleItem>,
    enabledSources: Set<String>,
    onUnreadOnlyChanged: (Boolean) -> Unit,
    onHideSportChanged: (Boolean) -> Unit,
    onBlacklistCatalogChanged: (Set<String>) -> Unit,
    onBlacklistTermsChanged: (Set<String>) -> Unit,
    onSourceEnabledChanged: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showAddDialog by remember { mutableStateOf(false) }
    var newBlacklistTerm by remember { mutableStateOf("") }

    val allBlacklistTerms = (defaultBlacklistTerms + blacklistCatalog).toList().sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add blacklist keyword"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("No family", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show only unread articles")
                Switch(checked = unreadOnly, onCheckedChange = onUnreadOnlyChanged)
            }

            Text("News sources", style = MaterialTheme.typography.titleMedium)
            sourceToggles.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(source.label)
                    Switch(
                        checked = source.id in enabledSources,
                        onCheckedChange = { enabled -> onSourceEnabledChanged(source.id, enabled) }
                    )
                }
            }

            Text("Categories family", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide sport articles")
                Switch(checked = hideSport, onCheckedChange = onHideSportChanged)
            }

            Text("Blacklist family", style = MaterialTheme.typography.titleMedium)
            allBlacklistTerms.forEach { term ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Block title keyword: $term")
                    Switch(
                        checked = term in blacklistTerms,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onBlacklistTermsChanged(blacklistTerms + term)
                            } else {
                                onBlacklistTermsChanged(blacklistTerms - term)
                            }
                        }
                    )
                }
            }
            if (allBlacklistTerms.isEmpty()) {
                Text("No blacklist keywords. Use + to add one.")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newBlacklistTerm = ""
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalized = newBlacklistTerm.trim().lowercase(Locale.getDefault())
                        if (normalized.isNotBlank()) {
                            onBlacklistCatalogChanged(blacklistCatalog + normalized)
                            onBlacklistTermsChanged(blacklistTerms + normalized)
                        }
                        showAddDialog = false
                        newBlacklistTerm = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newBlacklistTerm = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Add blacklist keyword") },
            text = {
                OutlinedTextField(
                    value = newBlacklistTerm,
                    onValueChange = { newBlacklistTerm = it },
                    label = { Text("Keyword") },
                    singleLine = true
                )
            }
        )
    }
}
