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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import com.example.newsfeed.ui.components.UbikLogo
import java.util.Locale

private data class ToggleFilterItem(
    val label: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val infoMessage: String? = null,
    val infoContentDescription: String = "Filter info"
)

private data class FilterTooltip(
    val filterName: String,
    val message: String
)

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
    var infoTooltip by remember { mutableStateOf<FilterTooltip?>(null) }

    val allBlacklistTerms = (blacklistCatalog).toList().sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    UbikLogo(onClick = onBack)
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
            val toggleFilters = listOf(
                ToggleFilterItem(
                    label = "Unread only",
                    checked = unreadOnly,
                    onCheckedChange = onUnreadOnlyChanged
                ),
                ToggleFilterItem(
                    label = "Hide sport",
                    checked = hideSport,
                    onCheckedChange = onHideSportChanged,
                    infoMessage = "Sport is the new opium of the masses",
                    infoContentDescription = "Sport filter info"
                ),
                ToggleFilterItem(
                    label = "Cydonia",
                    checked = cydonia,
                    onCheckedChange = onCydoniaChanged,
                    infoMessage = "How can we win when fools can be kings? Time has come to make things right and remove these fools from the news",
                    infoContentDescription = "Cydonia filter info"
                )
            )

            Text("Ubik filters", style = MaterialTheme.typography.titleMedium)
            toggleFilters.forEach { filter ->
                ToggleFilterRow(
                    item = filter,
                    onInfoClick = { filterName, message ->
                        infoTooltip = FilterTooltip(filterName = filterName, message = message)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Blacklisted keywords", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add blacklist keyword"
                    )
                }
            }
            allBlacklistTerms.forEach { term ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(term, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        IconButton(
                            onClick = {
                                onBlacklistCatalogChanged(blacklistCatalog - term)
                                onBlacklistTermsChanged(blacklistTerms - term)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove blacklist keyword"
                            )
                        }
                    }
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

    val tooltip = infoTooltip
    if (tooltip != null) {
        AlertDialog(
            onDismissRequest = { infoTooltip = null },
            confirmButton = {
                TextButton(onClick = { infoTooltip = null }) {
                    Text("OK")
                }
            },
            title = { Text("${tooltip.filterName} filter") },
            text = { Text(tooltip.message) }
        )
    }
}

@Composable
private fun ToggleFilterRow(
    item: ToggleFilterItem,
    onInfoClick: (String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.label)
            val infoMessage = item.infoMessage
            if (infoMessage != null) {
                IconButton(onClick = { onInfoClick(item.label, infoMessage) }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = item.infoContentDescription
                    )
                }
            }
        }
        Switch(checked = item.checked, onCheckedChange = item.onCheckedChange)
    }
}
