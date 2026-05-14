package com.example.newsfeed.data.provider

/**
 * Central list of available news providers.
 * Add a provider here to make it available in aggregation and source filters.
 */
data class ProviderDefinition(
    val id: String,
    val label: String,
    val factory: () -> NewsProvider
)

object ProviderDefinitions {
    val all = listOf(
        ProviderDefinition(
            id = "RTS",
            label = "RTS (Swiss French)",
            factory = { RtsNewsProvider() }
        ),
        ProviderDefinition(
            id = "Blast",
            label = "Blast (French)",
            factory = { BlastNewsProvider() }
        ),
        ProviderDefinition(
            id = "SRF",
            label = "SRF (Swiss German)",
            factory = { SrfNewsProvider() }
        ),
        ProviderDefinition(
            id = "Empa",
            label = "Empa (Workplace)",
            factory = { EmpaNewsProvider() }
        )
    )

    val allIds: Set<String> = all.map { definition -> definition.id }.toSet()
}
