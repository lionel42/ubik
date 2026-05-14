package com.example.newsfeed.ui

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal object ArticleFocusHtmlFilter {
    data class Config(
        val hideBottomArticles: Boolean
    )

    private val relatedHeadingRegex = Regex(
        pattern = "^(mehr zum thema|à consulter également|lire aussi|voir aussi|related articles|you might also like|meistgelesene artikel|les plus lus(?:\\s*-\\s*.+)?|derniers articles(?:\\s*-\\s*.+)?)$",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val shareLabelRegex = Regex(
        pattern = "\\b(share|teilen|partager)\\b",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun apply(html: String, config: Config): Document {
        val doc = Jsoup.parse(html)
        hideChrome(doc)
        hideShareControls(doc)
        if (config.hideBottomArticles) {
            hideBottomArticleBlocks(doc)
        }
        return doc
    }

    fun countRelatedHeadings(doc: Document): Int {
        return doc.select("h1, h2, h3, h4, h5, h6")
            .count { heading -> isRelatedHeadingText(heading.text()) }
    }

    fun countInteractiveShareControls(doc: Document): Int {
        return doc.select("a, button, [role=button], input[type=button], input[type=submit]")
            .count { node -> isShareLabel(readLabel(node)) }
    }

    private fun hideChrome(doc: Document) {
        val selectors = listOf(
            "header", "nav",
            "[role=banner]", "[role=navigation]",
            ".header", ".site-header", ".topbar", ".top-bar",
            ".navbar", ".menu", ".main-menu", ".breadcrumb",
            ".cookie", ".cookies", ".consent", ".newsletter",
            "#header", "#nav", "#navbar", "#menu"
        )

        selectors.forEach { selector ->
            doc.select(selector).forEach { node ->
                if (node.selectFirst("h1") == null) {
                    node.remove()
                }
            }
        }
    }

    private fun hideShareControls(doc: Document) {
        doc.select("a, button, [role=button], input[type=button], input[type=submit]")
            .forEach { node ->
                if (isShareLabel(readLabel(node))) {
                    node.remove()
                }
            }
    }

    private fun hideBottomArticleBlocks(doc: Document) {
        val headings = doc.select("h1, h2, h3, h4, h5, h6").toList()
        headings.forEach { heading ->
            if (isRelatedHeadingText(heading.text())) {
                hideRelatedSection(heading)
            }
        }
    }

    private fun hideRelatedSection(heading: Element) {
        val section = heading.closest(
            "section, aside, [role=complementary], [class*=related], [class*=more], [class*=popular], [id*=related], [id*=more], [id*=popular]"
        )

        if (section != null && section.selectFirst("h1") == null) {
            section.remove()
            return
        }

        var sibling = heading.nextElementSibling()
        heading.remove()
        while (sibling != null) {
            val next = sibling.nextElementSibling()
            if (sibling.tagName().matches(Regex("h[1-6]", RegexOption.IGNORE_CASE))) {
                break
            }
            sibling.remove()
            sibling = next
        }
    }

    private fun readLabel(node: Element): String {
        val aria = node.attr("aria-label")
        val title = node.attr("title")
        val text = node.text()
        return listOf(aria, title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isRelatedHeadingText(text: String): Boolean {
        return relatedHeadingRegex.matches(text.normalize())
    }

    private fun isShareLabel(text: String): Boolean {
        return shareLabelRegex.containsMatchIn(text.normalize())
    }

    private fun String.normalize(): String = trim().replace(Regex("\\s+"), " ")
}
