package io.github.ubik.data.provider

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object ProviderConfigLoader {
    private const val TAG = "ProviderConfigLoader"
    private const val DEFAULT_ASSET_NAME = "providers.json"
    private const val OVERRIDE_FILE_NAME = "providers_override.json"

    fun load(context: Context): List<ProviderDefinition> {
        loadOverride(context)?.let { return it }
        loadBundled(context)?.let { return it }

        Log.e(TAG, "No provider configuration could be loaded.")
        return emptyList()
    }

    private fun loadOverride(context: Context): List<ProviderDefinition>? {
        val overrideFile = File(context.filesDir, OVERRIDE_FILE_NAME)
        if (!overrideFile.exists()) return null

        return runCatching {
            parseProvidersConfig(overrideFile.readText())
        }.onFailure { error ->
            Log.e(TAG, "Failed to parse override file at ${overrideFile.absolutePath}", error)
        }.getOrNull()
    }

    private fun loadBundled(context: Context): List<ProviderDefinition>? {
        return runCatching {
            val jsonText = context.assets.open(DEFAULT_ASSET_NAME).bufferedReader().use { it.readText() }
            parseProvidersConfig(jsonText)
        }.onFailure { error ->
            Log.e(TAG, "Failed to parse bundled asset: $DEFAULT_ASSET_NAME", error)
        }.getOrNull()
    }

    internal fun parseProvidersConfig(jsonText: String): List<ProviderDefinition> {
        val root = Json.parseToJsonElement(jsonText).jsonObject
        val providerArray = root.requireArray("providers")

        val providers = providerArray.mapIndexed { index, element ->
            parseProvider(element, index)
        }

        val duplicateIds = providers
            .groupBy { provider -> provider.id }
            .filterValues { entries -> entries.size > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Duplicate provider ids found: ${duplicateIds.sorted().joinToString(", ")}"
        }

        return providers
    }

    private fun parseProvider(element: JsonElement, index: Int): ProviderDefinition {
        val provider = element.jsonObject

        val id = provider.requireString("id").trim()
        val label = provider.requireString("label").trim()
        val feedUrl = provider.requireString("feedUrl").trim()

        require(id.isNotBlank()) { "providers[$index].id cannot be blank" }
        require(label.isNotBlank()) { "providers[$index].label cannot be blank" }
        require(feedUrl.isNotBlank()) { "providers[$index].feedUrl cannot be blank" }

        val categoryRaw = provider.requireString("category").trim()
        val category = FeedCategory.entries.firstOrNull { entry ->
            entry.name.equals(categoryRaw, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "providers[$index].category has unknown value '$categoryRaw'"
        )

        return ProviderDefinition(
            id = id,
            label = label,
            description = provider.optionalString("description"),
            language = provider.optionalString("language"),
            region = provider.optionalString("region"),
            category = category,
            feedUrl = feedUrl
        )
    }

    private fun JsonObject.requireArray(key: String): JsonArray {
        return this[key]?.jsonArray ?: throw IllegalArgumentException("Missing or invalid '$key' array")
    }

    private fun JsonObject.requireString(key: String): String {
        val value = this[key] as? JsonPrimitive
            ?: throw IllegalArgumentException("Missing '$key' field")
        if (!value.isString) {
            throw IllegalArgumentException("Field '$key' must be a string")
        }
        return value.content
    }

    private fun JsonObject.optionalString(key: String): String {
        val value = this[key] ?: return ""
        val primitive = value as? JsonPrimitive
            ?: throw IllegalArgumentException("Field '$key' must be a string")
        if (!primitive.isString) {
            throw IllegalArgumentException("Field '$key' must be a string")
        }
        return primitive.content
    }
}
