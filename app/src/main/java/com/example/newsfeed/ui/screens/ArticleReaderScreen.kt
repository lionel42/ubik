package com.example.newsfeed.ui.screens

import android.content.Intent
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.newsfeed.model.RtsArticle

private fun buildArticleFocusScript(hideBottomArticles: Boolean): String = """
(function() {
    var hideBottomArticlesEnabled = $hideBottomArticles;

    function hideNode(node) {
        if (!node || node.dataset.ubikHidden === '1') return;
        node.dataset.ubikHidden = '1';
        node.style.setProperty('display', 'none', 'important');
        node.style.setProperty('visibility', 'hidden', 'important');
        node.style.setProperty('height', '0', 'important');
        node.style.setProperty('margin', '0', 'important');
        node.style.setProperty('padding', '0', 'important');
    }

    function normalizeText(text) {
        return (text || '').trim().replace(/\s+/g, ' ');
    }

    function isShareLabel(label) {
        return /\b(share|teilen|partager)\b/i.test(normalizeText(label));
    }

    function isRelatedHeadingText(text) {
        return /^(mehr zum thema|à consulter également|lire aussi|voir aussi|related articles|you might also like|meistgelesene artikel|les plus lus(?:\s*-\s*.+)?|derniers articles(?:\s*-\s*.+)?)$/i.test(normalizeText(text));
    }

    function ensureStyle() {
        var style = document.getElementById('ubik-reader-style');
        if (!style) {
            style = document.createElement('style');
            style.id = 'ubik-reader-style';
            style.innerHTML = `
                body { margin-top: 0 !important; padding-top: 0 !important; }
                main, article, [role="main"] { max-width: 900px; margin: 0 auto !important; }
            `;
            document.head.appendChild(style);
        }
    }

    function hideChrome() {
        var selectors = [
            'header', 'nav',
            '[role="banner"]', '[role="navigation"]',
            '.header', '.site-header', '.topbar', '.top-bar',
            '.navbar', '.menu', '.main-menu', '.breadcrumb',
            '.cookie', '.cookies', '.consent', '.newsletter',
            '#header', '#nav', '#navbar', '#menu'
        ];

        selectors.forEach(function(selector) {
            document.querySelectorAll(selector).forEach(function(node) {
                if (node.querySelector('h1')) return;
                hideNode(node);
            });
        });
    }

    function hideShareControls() {
        document.querySelectorAll('a, button, [role="button"], input[type="button"], input[type="submit"]').forEach(function(node) {
            var label = [
                node.getAttribute('aria-label'),
                node.getAttribute('title'),
                node.textContent
            ].filter(Boolean).join(' ');
            if (isShareLabel(label)) {
                hideNode(node);
            }
        });
    }

    function hideRelatedSection(heading) {
        var section = heading.closest('section, aside, [role="complementary"], [class*="related"], [class*="more"], [class*="popular"], [id*="related"], [id*="more"], [id*="popular"]');
        if (section && !section.querySelector('h1')) {
            hideNode(section);
            return;
        }

        hideNode(heading);

        var sibling = heading.nextElementSibling;
        while (sibling) {
            if (/^H[1-6]$/i.test(sibling.tagName)) break;
            var next = sibling.nextElementSibling;
            hideNode(sibling);
            sibling = next;
        }
    }

    function hideBottomArticleBlocks() {
        if (!hideBottomArticlesEnabled) return;

        document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(function(node) {
            if (isRelatedHeadingText(node.textContent || node.innerText)) {
                hideRelatedSection(node);
            }
        });
    }

    function applyFocusMode() {
        ensureStyle();
        hideChrome();
        hideShareControls();
        hideBottomArticleBlocks();
    }

    var pending = false;
    function scheduleApply() {
        if (pending) return;
        pending = true;
        window.setTimeout(function() {
            pending = false;
            applyFocusMode();
        }, 50);
    }

    applyFocusMode();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scheduleApply, { once: true });
    }

    window.addEventListener('load', scheduleApply, { once: true });

    if (window.__ubikFocusObserver) {
        window.__ubikFocusObserver.disconnect();
    }

    window.__ubikFocusObserver = new MutationObserver(function() {
        scheduleApply();
    });

    window.__ubikFocusObserver.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    window.setTimeout(applyFocusMode, 250);
    window.setTimeout(applyFocusMode, 1000);
})();
"""

private fun configureWebViewAppearance(webView: WebView, darkMode: Boolean) {
    val settings = webView.settings

    // Keep WebView dark manipulation disabled so websites with native dark themes (like SRF)
    // can decide their own rendering.
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(
            settings,
            WebSettingsCompat.FORCE_DARK_OFF
        )
    }

    webView.setBackgroundColor(if (darkMode) Color.BLACK else Color.WHITE)
}

private fun injectArticleFocusMode(webView: WebView, hideBottomArticles: Boolean) {
    webView.evaluateJavascript(buildArticleFocusScript(hideBottomArticles), null)
}

private class ArticleFocusWebViewClient(
    private val hideBottomArticles: Boolean
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let { injectArticleFocusMode(it, hideBottomArticles) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArticleReaderScreen(
    article: RtsArticle,
    darkMode: Boolean,
    articleFocusMode: Boolean,
    hideBottomArticles: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val inPreview = LocalInspectionMode.current
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
        if (inPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("WebView preview")
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = if (articleFocusMode) {
                            ArticleFocusWebViewClient(hideBottomArticles)
                        } else {
                            WebViewClient()
                        }
                        settings.javaScriptEnabled = true
                        configureWebViewAppearance(this, darkMode)

                        loadUrl(article.link)
                    }
                },
                update = { webView ->
                    webView.webViewClient = if (articleFocusMode) {
                        ArticleFocusWebViewClient(hideBottomArticles)
                    } else {
                        WebViewClient()
                    }
                    configureWebViewAppearance(webView, darkMode)
                    if (webView.url != article.link) {
                        webView.loadUrl(article.link)
                    } else if (articleFocusMode) {
                        injectArticleFocusMode(webView, hideBottomArticles)
                    }
                }
            )
        }
    }
}
