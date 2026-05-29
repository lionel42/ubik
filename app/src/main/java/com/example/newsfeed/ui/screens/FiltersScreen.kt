package com.example.newsfeed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.newsfeed.data.defaultCydoniaNames
import com.example.newsfeed.data.defaultBlacklistTerms
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FiltersScreen(
    unreadOnly: Boolean,
    hideSport: Boolean,
    cydonia: Boolean,
    blacklistCatalog: Set<String>,
    blacklistTerms: Set<String>,
    onUnreadOnlyChanged: (Boolean) -> Unit,
    onHideSportChanged: (Boolean) -> Unit,
    onCydoniaChanged: (Boolean) -> Unit,
    onBlacklistCatalogChanged: (Set<String>) -> Unit,
    onBlacklistTermsChanged: (Set<String>) -> Unit,
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
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show only unread articles")
                Switch(checked = unreadOnly, onCheckedChange = onUnreadOnlyChanged)
            }

            Text("Blacklisted categories", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide sport articles")
                Switch(checked = hideSport, onCheckedChange = onHideSportChanged)
            }
            Text(
                text = "Sport is the new opium of the masses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cydonia filter")
                Switch(checked = cydonia, onCheckedChange = onCydoniaChanged)
            }
            Text(
                text = "How can we win when fools can be kings? Time has come to make things right and remove these fools from the news",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Blacklisted keywords", style = MaterialTheme.typography.titleMedium)
            allBlacklistTerms.forEach { term ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$term")
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
