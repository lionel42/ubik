package com.example.newsfeed.ui

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal object ArticleFocusHtmlFilter {
    data class Config(
        val hideBottomArticles: Boolean = false,
        val hideChrome: Boolean = false,
        val hidePromoContent: Boolean = false
    )

    private val relatedHeadingRegex = Regex(
        pattern = "^(mehr zum thema|à consulter également|lire aussi|voir aussi|related articles|you might also like|meistgelesene artikel|les plus lus(?:\\s*-\\s*.+)?|derniers articles(?:\\s*-\\s*.+)?)$",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val shareLabelRegex = Regex(
        pattern = "\\b(share|teilen|partager)\\b",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val promoRegex = Regex(
        pattern = "(app|download|install|promote|promo|ad|advertisement|sponsored|publicity|publicité|werbung|angebot|telecharger|installieren)",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun apply(html: String, config: Config): Document {
        val doc = Jsoup.parse(html)
        if (config.hideChrome) {
            hideChrome(doc)
        }
        hideShareControls(doc)
        if (config.hideBottomArticles) {
            hideBottomArticleBlocks(doc)
        }
        if (config.hidePromoContent) {
            hidePromoContent(doc)
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

    fun countFooterElements(doc: Document): Int {
        return doc.select("footer").size +
               doc.select("[role='contentinfo']").size +
               doc.select("aside").size
    }

    fun countPromoElements(doc: Document): Int {
        return doc.select(
            "[class*=promo], [class*=ad], [class*=advertisement], [class*=sponsored], " +
            "[class*='app-promote'], [class*='download-app'], [class*='install'], " +
            "[id*=promo], [id*=ad], [id*=advertisement], [id*=sponsored]"
        ).count { element ->
            // Only count if it has promo-like text or data attributes
            val text = element.text().lowercase()
            val classes = element.className().lowercase()
            val id = element.id().lowercase()
            promoRegex.containsMatchIn(text) || promoRegex.containsMatchIn(classes) || promoRegex.containsMatchIn(id)
        }
    }

    private fun hideChrome(doc: Document) {
        val selectors = listOf(
            "header", "nav", "footer", "aside",
            "[role=banner]", "[role=navigation]", "[role='contentinfo']",
            ".header", ".site-header", ".topbar", ".top-bar",
            ".navbar", ".menu", ".main-menu", ".breadcrumb",
            ".cookie", ".cookies", ".consent", ".newsletter",
            ".footer", ".site-footer", ".sidebar",
            "#header", "#nav", "#navbar", "#menu", "#footer", "#sidebar"
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

    private fun hidePromoContent(doc: Document) {
        // Remove elements with promo in class/id
        val selectors = listOf(
            "[class*=promo]",
            "[class*=ad]",
            "[class*=advertisement]",
            "[class*=sponsored]",
            "[class*='app-promote']",
            "[class*='download-app']",
            "[class*='install-app']",
            "[id*=promo]",
            "[id*=ad]",
            "[id*=advertisement]",
            "[id*=sponsored]"
        )

        selectors.forEach { selector ->
            doc.select(selector).forEach { element ->
                if (element.selectFirst("h1") == null) {
                    element.remove()
                }
            }
        }

        // Remove sections with promo-like headings
        val headings = doc.select("h1, h2, h3, h4, h5, h6").toList()
        headings.forEach { heading ->
            val text = heading.text().lowercase()
            if (promoRegex.containsMatchIn(text)) {
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
