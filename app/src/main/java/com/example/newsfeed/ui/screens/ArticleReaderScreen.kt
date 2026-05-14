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

private const val ARTICLE_FOCUS_SCRIPT = """
(function() {
  var selectors = [
    'header', 'nav', 'footer',
    '[role="banner"]', '[role="navigation"]',
    '.header', '.site-header', '.topbar', '.top-bar',
    '.navbar', '.menu', '.main-menu', '.breadcrumb',
    '.cookie', '.cookies', '.consent', '.newsletter',
    '#header', '#nav', '#navbar', '#menu', '#footer'
  ];

  selectors.forEach(function(selector) {
    document.querySelectorAll(selector).forEach(function(node) {
      // Preserve any node that contains an h1 (article title)
      if (node.querySelector('h1')) return;
      node.style.setProperty('display', 'none', 'important');
      node.style.setProperty('visibility', 'hidden', 'important');
      node.style.setProperty('height', '0', 'important');
      node.style.setProperty('margin', '0', 'important');
      node.style.setProperty('padding', '0', 'important');
    });
  });

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

private fun injectArticleFocusMode(webView: WebView) {
    webView.evaluateJavascript(ARTICLE_FOCUS_SCRIPT, null)
}

private class ArticleFocusWebViewClient : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let(::injectArticleFocusMode)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArticleReaderScreen(
    article: RtsArticle,
    darkMode: Boolean,
    articleFocusMode: Boolean,
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
                        webViewClient = if (articleFocusMode) ArticleFocusWebViewClient() else WebViewClient()
                        settings.javaScriptEnabled = true
                        configureWebViewAppearance(this, darkMode)

                        loadUrl(article.link)
                    }
                },
                update = { webView ->
                    webView.webViewClient = if (articleFocusMode) ArticleFocusWebViewClient() else WebViewClient()
                    configureWebViewAppearance(webView, darkMode)
                    if (webView.url != article.link) {
                        webView.loadUrl(article.link)
                    } else if (articleFocusMode) {
                        injectArticleFocusMode(webView)
                    }
                }
            )
        }
    }
}
