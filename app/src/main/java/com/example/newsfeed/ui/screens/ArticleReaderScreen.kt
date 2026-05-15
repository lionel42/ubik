package com.example.newsfeed.ui.screens

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.newsfeed.model.RtsArticle

enum class ArticleHideElement(
    val storageKey: String,
    val label: String,
    val selectors: List<String> = emptyList()
) {
    HEADER(
        storageKey = "header",
        label = "Hide header",
        selectors = listOf(
            "header",
            "[role=\"banner\"]",
            ".header",
            ".site-header",
            ".topbar",
            ".top-bar",
            "#header"
        )
    ),
    NAVIGATION(
        storageKey = "navigation",
        label = "Hide navigation",
        selectors = listOf(
            "nav",
            "[role=\"navigation\"]",
            ".navbar",
            ".menu",
            ".main-menu",
            "#nav",
            "#navbar",
            "#menu"
        )
    ),
    BREADCRUMB(
        storageKey = "breadcrumb",
        label = "Hide breadcrumb",
        selectors = listOf(
            ".breadcrumb",
            "[class*=\"breadcrumb\"]",
            "[id*=\"breadcrumb\"]"
        )
    ),
    COOKIE_CONSENT(
        storageKey = "cookie_consent",
        label = "Hide cookie and consent banners",
        selectors = listOf(
            ".cookie",
            ".cookies",
            ".consent",
            ".modal__overlay",
            ".js-modal-overlay",
            "#usercentrics-root",
            "#usercentrics-cmp-ui",
            "[id*=\"usercentrics\"]",
            "[class*=\"usercentrics\"]",
            "[class*=\"uc-\"]",
            "[id*=\"uc-\"]",
            "[class*=\"cookie\"]",
            "[class*=\"consent\"]",
            "[id*=\"cookie\"]",
            "[id*=\"consent\"]"
        )
    ),
    NEWSLETTER_PROMPT(
        storageKey = "newsletter",
        label = "Hide newsletter prompts",
        selectors = listOf(
            ".newsletter",
            "[class*=\"newsletter\"]",
            "[id*=\"newsletter\"]"
        )
    ),
    SHARE_CONTROLS(
        storageKey = "share_controls",
        label = "Hide share controls",
        selectors = listOf(
            "[class*=\"share\"]",
            "[class*=\"sharing\"]",
            "[data-share-link]",
            "[data-share-title]",
            "[class*=\"social\"]",
            "[id*=\"share\"]"
        )
    ),
    RELATED_ARTICLES(
        storageKey = "related_articles",
        label = "Hide related/bottom articles",
        selectors = listOf(
            "[class*=\"related\"]",
            "[class*=\"more\"]",
            "[class*=\"popular\"]",
            "[id*=\"related\"]",
            "[id*=\"more\"]",
            "[id*=\"popular\"]"
        )
    ),
    FOOTER(
        storageKey = "footer",
        label = "Hide footer",
        selectors = listOf(
            "footer",
            "[role=\"contentinfo\"]",
            ".footer",
            ".site-footer",
            ".page-footer",
            "#footer"
        )
    ),
    AI_SUMMARY(
        storageKey = "ai_summary",
        label = "AI Summary",
        selectors = listOf(
            "[class=\"article-summary-btn\"]"
        )
    ),
    PROMO_CONTENT(
        storageKey = "promo_content",
        label = "Hide promotional content",
        selectors = listOf(
            "[class*=\"promo-banner\"]",
            "[class*=\"rts-download-banner-footer\"]",
        )
    );

    companion object {
        val defaultHidden: Set<ArticleHideElement> = setOf(
            HEADER,
            COOKIE_CONSENT,
            FOOTER,
            SHARE_CONTROLS
        )

        fun fromStorageKey(storageKey: String): ArticleHideElement? {
            return entries.firstOrNull { element -> element.storageKey == storageKey }
        }
    }
}

private fun jsStringLiteral(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
}

private fun jsArray(values: List<String>): String {
    return values.joinToString(prefix = "[", postfix = "]") { value -> "'${jsStringLiteral(value)}'" }
}

private fun buildArticleFocusScript(hiddenElements: Set<ArticleHideElement>): String {
    val enabledStorageKeys = hiddenElements.map { element -> element.storageKey }
    val selectorMap = ArticleHideElement.entries.joinToString(",") { element ->
        "'${jsStringLiteral(element.storageKey)}': ${jsArray(element.selectors)}"
    }

    return """
(function() {
    var enabledStorageKeys = ${jsArray(enabledStorageKeys)};
    var selectorMap = {$selectorMap};

    function hideNode(node) {
        if (!node || node.dataset.ubikHidden === '1') return;
        node.dataset.ubikDisplay = node.style.display || '';
        node.dataset.ubikVisibility = node.style.visibility || '';
        node.dataset.ubikHeight = node.style.height || '';
        node.dataset.ubikMargin = node.style.margin || '';
        node.dataset.ubikPadding = node.style.padding || '';
        node.dataset.ubikPointerEvents = node.style.pointerEvents || '';
        node.dataset.ubikHidden = '1';
        node.style.setProperty('display', 'none', 'important');
        node.style.setProperty('visibility', 'hidden', 'important');
        node.style.setProperty('height', '0', 'important');
        node.style.setProperty('margin', '0', 'important');
        node.style.setProperty('padding', '0', 'important');
        node.style.setProperty('pointer-events', 'none', 'important');
    }

    function restoreStyleProperty(node, propertyName, datasetKey) {
        var previousValue = node.dataset[datasetKey];
        if (previousValue) {
            node.style.setProperty(propertyName, previousValue);
        } else {
            node.style.removeProperty(propertyName);
        }
        delete node.dataset[datasetKey];
    }

    function unhidePreviouslyHiddenNodes() {
        document.querySelectorAll('[data-ubik-hidden="1"]').forEach(function(node) {
            restoreStyleProperty(node, 'display', 'ubikDisplay');
            restoreStyleProperty(node, 'visibility', 'ubikVisibility');
            restoreStyleProperty(node, 'height', 'ubikHeight');
            restoreStyleProperty(node, 'margin', 'ubikMargin');
            restoreStyleProperty(node, 'padding', 'ubikPadding');
            restoreStyleProperty(node, 'pointer-events', 'ubikPointerEvents');
            delete node.dataset.ubikHidden;
        });
    }

    function hideSelectorsFor(storageKey) {
        (selectorMap[storageKey] || []).forEach(function(selector) {
            document.querySelectorAll(selector).forEach(function(node) {
                if (storageKey !== 'cookie_consent' && node.querySelector && node.querySelector('h1')) return;
                hideNode(node);
            });
        });
    }

    function fixLazyImages() {
        document.querySelectorAll('img').forEach(function(img) {
            var src = img.getAttribute('src') || '';
            if (!src.startsWith('data:') && src !== '') return;

            var srcset = img.getAttribute('srcset')
                || img.getAttribute('data-srcset')
                || img.getAttribute('data-src')
                || '';

            if (!srcset) {
                var picture = img.closest('picture');
                var source = picture && picture.querySelector('source[srcset]');
                if (source) srcset = source.getAttribute('srcset') || '';
            }

            var url = srcset.split(',')[0].trim().split(/\s+/)[0];
            if (url) { img.src = url; img.loading = 'eager'; }
        });
    }

    function applyHiddenElements() {
        unhidePreviouslyHiddenNodes();
        enabledStorageKeys.forEach(function(storageKey) {
            hideSelectorsFor(storageKey);
        });
        fixLazyImages();
    }

    var pending = false;
    function scheduleApply() {
        if (pending) return;
        pending = true;
        window.setTimeout(function() {
            pending = false;
            applyHiddenElements();
        }, 50);
    }

    applyHiddenElements();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scheduleApply, { once: true });
    }

    window.addEventListener('load', scheduleApply, { once: true });

    window.setTimeout(applyHiddenElements, 250);
    window.setTimeout(applyHiddenElements, 1000);
})();
"""
}

private fun injectArticleFocusMode(webView: WebView, hiddenElements: Set<ArticleHideElement>) {
    webView.evaluateJavascript(buildArticleFocusScript(hiddenElements), null)
}

private class ArticleFocusWebViewClient(
    val hiddenElements: Set<ArticleHideElement>
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let { injectArticleFocusMode(it, hiddenElements) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArticleReaderScreen(
    webView: WebView,
    article: RtsArticle,
    hiddenElements: Set<ArticleHideElement>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = article.title, maxLines = 1) },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, article.link)
                            putExtra(Intent.EXTRA_SUBJECT, article.title)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { webView },
            update = { webView ->
                val currentClient = webView.webViewClient as? ArticleFocusWebViewClient
                if (currentClient?.hiddenElements != hiddenElements) {
                    webView.webViewClient = ArticleFocusWebViewClient(hiddenElements)
                }

                if (webView.url != article.link) {
                    webView.loadUrl(article.link)
                } else {
                    injectArticleFocusMode(webView, hiddenElements)
                }
            }
        )

    }
}
