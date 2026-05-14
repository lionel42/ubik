package com.example.newsfeed.util

import androidx.core.text.HtmlCompat
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun canonicalArticleKey(link: String): String {
    val trimmed = link.trim()
    if (trimmed.isBlank()) return ""

    return runCatching {
        val uri = URI(trimmed)
        val scheme = (uri.scheme ?: "https").lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val rawPath = uri.path.orEmpty()
            .replace(Regex("/{2,}"), "/")
            .trimEnd('/')
        val path = if (rawPath.isBlank()) "/" else rawPath

        if (host.isBlank()) {
            trimmed.substringBefore('#').substringBefore('?').trimEnd('/')
        } else {
            "$scheme://$host$path"
        }
    }.getOrElse {
        trimmed.substringBefore('#').substringBefore('?').trimEnd('/')
    }
}

fun extractSummary(descriptionHtml: String): String {
    return HtmlCompat.fromHtml(descriptionHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()
}

fun extractImageUrl(descriptionHtml: String): String? {
    val regex = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    return regex.find(descriptionHtml)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace("&amp;", "&")
}

fun parsePubDateEpoch(pubDate: String): Long {
    return runCatching {
        ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(0L)
}

fun formatPubDate(pubDate: String): String {
    val epoch = parsePubDateEpoch(pubDate)
    if (epoch == 0L) return ""

    return formatEpochToDisplay(epoch)
}

fun formatEpochToDisplay(epoch: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(epoch).atZone(zone)
    val nowInstant = Instant.now()
    val diffMillis = nowInstant.toEpochMilli() - epoch
    if (diffMillis in 0 until 24L * 60L * 60L * 1000L) {
        val hoursAgo = (diffMillis / (60L * 60L * 1000L)).toInt().coerceAtLeast(1)
        return "il y a ${hoursAgo}h"
    }

    val month = dateTime.month
        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.FRENCH) else char.toString()
        }

    return "${dateTime.dayOfMonth} $month ${dateTime.hour}h"
}

fun parseRtsRelativeTimeToEpoch(label: String): Long {
    val normalized = label.lowercase(Locale.getDefault()).trim()
    if (normalized.isBlank()) return 0L

    val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
    val timeMatch = timeRegex.find(normalized) ?: return 0L
    val hour = timeMatch.groupValues[1].toIntOrNull() ?: return 0L
    val minute = timeMatch.groupValues[2].toIntOrNull() ?: return 0L

    val zone = ZoneId.systemDefault()
    val now = ZonedDateTime.now(zone)
    val time = runCatching { LocalTime.of(hour, minute) }.getOrNull() ?: return 0L

    if (normalized.contains("aujourd")) {
        return now.toLocalDate().atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    if (normalized.contains("hier")) {
        return now.toLocalDate().minusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    val weekdayMap = mapOf(
        "lundi" to 1,
        "mardi" to 2,
        "mercredi" to 3,
        "jeudi" to 4,
        "vendredi" to 5,
        "samedi" to 6,
        "dimanche" to 7
    )
    weekdayMap.entries.firstOrNull { normalized.startsWith(it.key) }?.let { entry ->
        var delta = now.dayOfWeek.value - entry.value
        if (delta <= 0) delta += 7
        return now.toLocalDate().minusDays(delta.toLong()).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    val dateRegex = Regex("(\\d{1,2})\\s+([a-zéûîô]+)\\s+(\\d{4})")
    val dateMatch = dateRegex.find(normalized)
    if (dateMatch != null) {
        val day = dateMatch.groupValues[1].toIntOrNull()
        val month = frenchMonthToNumber(dateMatch.groupValues[2])
        val year = dateMatch.groupValues[3].toIntOrNull()
        if (day != null && month != null && year != null) {
            val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (date != null) {
                return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            }
        }
    }

    return 0L
}

private fun frenchMonthToNumber(monthLabel: String): Int? {
    return when (monthLabel.lowercase(Locale.getDefault())) {
        "janvier" -> 1
        "fevrier", "février" -> 2
        "mars" -> 3
        "avril" -> 4
        "mai" -> 5
        "juin" -> 6
        "juillet" -> 7
        "aout", "août" -> 8
        "septembre" -> 9
        "octobre" -> 10
        "novembre" -> 11
        "decembre", "décembre" -> 12
        else -> null
    }
}
