package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference

private const val LOCAL_ORIGIN = "https://starchess.local"
private const val ENTRY_URL = "$LOCAL_ORIGIN/index.html"
private const val TAG = "StarChessWeb"

class StarChessWebController(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val initialEntriesProvider: () -> Map<String, String>,
    private val onStorageSet: (String, String) -> Unit,
    private val onStorageRemove: (String) -> Unit,
    private val onStorageClear: () -> Unit,
    private val onEngineBackDepthChanged: (Boolean) -> Unit,
    private val onPlayDurationTick: (String, Long) -> Unit = { _, _ -> },
) {
    private var webViewRef: WeakReference<WebView>? = null
    @Volatile
    private var cachedInjectedHtmlBytes: ByteArray? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun createOrReuseWebView(context: Context): WebView {
        webViewRef?.get()?.let { return it }

        val webView = WebView(context).apply {
            setBackgroundColor(Color.parseColor("#05051A"))
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            addJavascriptInterface(NativeJsBridge(), "StarChessAndroid")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.scheme == "https" && uri.host == "starchess.local") {
                        return try {
                            val bytes = loadInjectedHtmlBytes()
                            WebResourceResponse(
                                "text/html",
                                "UTF-8",
                                ByteArrayInputStream(bytes),
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed loading local starchess.html asset", e)
                            null
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val uri = request?.url ?: return true
                    // Keep navigation confined inside our offline Star Chess bundle
                    return !(uri.scheme == "https" && uri.host == "starchess.local")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    installNavigationObserver(view)
                }
            }

            loadUrl(ENTRY_URL)
        }

        webViewRef = WeakReference(webView)
        return webView
    }

    private fun loadInjectedHtmlBytes(): ByteArray {
        cachedInjectedHtmlBytes?.let { return it }
        synchronized(this) {
            cachedInjectedHtmlBytes?.let { return it }
            val rawHtml = appContext.assets.open("starchess.html").bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            val injection = buildString {
                append(
                    """
                    <style id="starchess-native-shell-overrides">
                    .lang-switch, #btnSound, #btnBgmToggle { display: none !important; }
                    body { padding-top: 8px !important; padding-bottom: 16px !important; }
                    </style>
                    <script>
                    (function() {
                      try {
                        var bridge = window.StarChessAndroid;
                        if (!bridge) return;
                        var initialJson = bridge.getInitialStorageJson();
                        if (initialJson) {
                          var map = JSON.parse(initialJson);
                          Object.keys(map).forEach(function(k) {
                            if (localStorage.getItem(k) === null) {
                              localStorage.setItem(k, map[k]);
                            }
                          });
                        }
                        var _set = Storage.prototype.setItem;
                        var _rem = Storage.prototype.removeItem;
                        var _clr = Storage.prototype.clear;
                        Storage.prototype.setItem = function(k, v) {
                          _set.call(this, k, v);
                          if (this === window.localStorage && String(k).indexOf("starchess.") === 0) {
                            try { bridge.onStorageSet(String(k), String(v)); } catch (e) {}
                          }
                        };
                        Storage.prototype.removeItem = function(k) {
                          _rem.call(this, k);
                          if (this === window.localStorage && String(k).indexOf("starchess.") === 0) {
                            try { bridge.onStorageRemove(String(k)); } catch (e) {}
                          }
                        };
                        Storage.prototype.clear = function() {
                          _clr.call(this);
                          if (this === window.localStorage) {
                            try { bridge.onStorageClear(); } catch (e) {}
                          }
                        };
                        setTimeout(function() {
                          try {
                            for (var i = 0; i < localStorage.length; i++) {
                              var key = localStorage.key(i);
                              if (key && key.indexOf("starchess.") === 0) {
                                bridge.onStorageSet(key, localStorage.getItem(key) || "");
                              }
                            }
                          } catch (e) {}
                        }, 450);
                      } catch (e) {}
                    })();
                    </script>
                    """.trimIndent()
                )
            }
            val headIdx = rawHtml.indexOf("<head>", ignoreCase = true)
            val injected = if (headIdx >= 0) {
                val insertAt = headIdx + "<head>".length
                rawHtml.substring(0, insertAt) + "\n" + injection + "\n" + rawHtml.substring(insertAt)
            } else {
                injection + "\n" + rawHtml
            }
            return injected.toByteArray(Charsets.UTF_8).also { cachedInjectedHtmlBytes = it }
        }
    }

    private fun installNavigationObserver(webView: WebView?) {
        webView ?: return
        val script = """
            (function() {
              if (window.__starchessNavObserverInstalled) return;
              window.__starchessNavObserverInstalled = true;
              function computeBackDepth() {
                var openOverlay = document.querySelector('.overlay.active, #overlay-hub.active, #overlay-generic.active, #overlay-rules.active, #overlay-result.active');
                var activeView = document.querySelector('.view.active');
                var isRootMenu = activeView && activeView.id === 'view-menu';
                var hasSub = Boolean(openOverlay) || !Boolean(isRootMenu);
                if (window.StarChessAndroid) {
                  window.StarChessAndroid.onBackDepthChanged(hasSub);
                }
              }
              var obs = new MutationObserver(computeBackDepth);
              obs.observe(document.body, { attributes: true, subtree: true, attributeFilter: ['class', 'style'] });
              computeBackDepth();
              if (!window.__starchessDurationTimer) {
                window.__starchessDurationTimer = setInterval(function() {
                  try {
                    if (document.hidden) return;
                    var gameView = document.getElementById('view-game');
                    if (gameView && gameView.classList.contains('active') && window.StarChessAndroid) {
                      var activeGame = (window.S && window.S.game) || localStorage.getItem('starchess.lastGame') || '';
                      if (activeGame) {
                        window.StarChessAndroid.onPlayDurationTick(String(activeGame), 10);
                      }
                    }
                  } catch (e) {}
                }, 10000);
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun updateStarCoinsLive(newBalance: Int) {
        runJs(
            """
            (function() {
              try {
                var val = $newBalance;
                localStorage.setItem('starchess.starCoins', String(val));
                if (window.S) {
                  window.S.starCoins = val;
                }
                if (typeof window.updateCoinDisplay === 'function') {
                  window.updateCoinDisplay();
                }
              } catch (e) {}
            })();
            """.trimIndent()
        )
    }

    fun applyCosmicPalette(primaryHex: String, secondaryHex: String, bgHex: String) {
        val p = JSONObject.quote(primaryHex)
        val s = JSONObject.quote(secondaryHex)
        val b = JSONObject.quote(bgHex)
        runJs(
            """
            (function() {
              try {
                var root = document.documentElement;
                if (!root) return;
                root.style.setProperty('--cyan', $p);
                root.style.setProperty('--accent', $p);
                root.style.setProperty('--magenta', $s);
                root.style.setProperty('--bg', $b);
                if (document.body) {
                  document.body.style.backgroundColor = $b;
                }
              } catch (e) {}
            })();
            """.trimIndent()
        )
    }

    fun setLanguage(languageCode: String) {
        val safeCode = JSONObject.quote(languageCode)
        runJs(
            """
            (function() {
              var btn = document.querySelector('#langSwitch .lang-btn[data-lang=' + $safeCode + ']');
              if (btn) btn.click();
              else localStorage.setItem('starchess.lang', $safeCode);
            })();
            """.trimIndent()
        )
    }

    fun toggleSound() {
        runJs("document.getElementById('btnSound') && document.getElementById('btnSound').click();")
    }

    fun toggleBgm() {
        runJs("document.getElementById('btnBgmToggle') && document.getElementById('btnBgmToggle').click();")
    }

    fun openArena() {
        runJs(
            """
            (function() {
              ['overlay-hub','overlay-generic','overlay-rules','overlay-result'].forEach(function(id) {
                var el = document.getElementById(id);
                if (el && el.classList.contains('active')) {
                  var closeBtn = el.querySelector('.gen-close, .top-back, .r-menu');
                  if (closeBtn) closeBtn.click();
                  else el.classList.remove('active');
                }
              });
              var active = document.querySelector('.view.active');
              if (active && (active.id === 'view-quests' || active.id === 'view-odyssey')) {
                var backBtn = active.querySelector('.top-back');
                if (backBtn) backBtn.click();
              }
            })();
            """.trimIndent()
        )
    }

    fun openAstralNexus() {
        runJs(
            """
            (function() {
              var active = document.querySelector('.view.active');
              if (active && active.id === 'view-game') {
                var btn = document.getElementById('btnInGameNexus');
                if (btn) { btn.click(); return; }
              }
              var hubBtn = document.getElementById('btnAstralNexusHub');
              if (hubBtn) hubBtn.click();
            })();
            """.trimIndent()
        )
    }

    fun openTrials() {
        runJs(
            """
            (function() {
              ['overlay-hub','overlay-generic','overlay-rules'].forEach(function(id) {
                var el = document.getElementById(id);
                if (el && el.classList.contains('active')) {
                  var closeBtn = el.querySelector('.gen-close, .top-back');
                  if (closeBtn) closeBtn.click();
                  else el.classList.remove('active');
                }
              });
              var qBtn = document.getElementById('btnQuests');
              if (qBtn) qBtn.click();
            })();
            """.trimIndent()
        )
    }

    fun dispatchEngineBack() {
        runJs(
            """
            (function() {
              var overlays = Array.from(document.querySelectorAll('.overlay.active, #overlay-hub.active, #overlay-generic.active, #overlay-rules.active, #overlay-result.active'));
              if (overlays.length > 0) {
                var top = overlays[overlays.length - 1];
                var btn = top.querySelector('.gen-close, .top-back, .r-menu, button[id*="Close"]');
                if (btn) { btn.click(); return; }
                top.classList.remove('active');
                return;
              }
              var active = document.querySelector('.view.active');
              if (active && active.id !== 'view-menu') {
                var backBtn = active.querySelector('.top-back');
                if (backBtn) backBtn.click();
              }
            })();
            """.trimIndent()
        )
    }

    fun applyEntriesAndReload(entries: Map<String, String>) {
        val webView = webViewRef?.get() ?: return
        val jsonPayload = JSONObject().apply {
            entries.forEach { (k, v) -> put(k, v) }
        }.toString()
        val quoted = JSONObject.quote(jsonPayload)
        webView.post {
            webView.evaluateJavascript(
                """
                (function() {
                  try {
                    localStorage.clear();
                    var map = JSON.parse($quoted);
                    Object.keys(map).forEach(function(k) {
                      localStorage.setItem(k, map[k]);
                    });
                  } catch (e) {}
                })();
                """.trimIndent()
            ) {
                webView.loadUrl(ENTRY_URL)
            }
        }
    }

    fun runJs(script: String) {
        val webView = webViewRef?.get() ?: return
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun destroy() {
        webViewRef?.get()?.let { wv ->
            wv.stopLoading()
            wv.destroy()
        }
        webViewRef = null
    }

    private inner class NativeJsBridge {
        @JavascriptInterface
        fun getInitialStorageJson(): String {
            val entries = initialEntriesProvider()
            return JSONObject().apply {
                entries.forEach { (k, v) -> put(k, v) }
            }.toString()
        }

        @JavascriptInterface
        fun onStorageSet(key: String, value: String) {
            scope.launch(Dispatchers.IO) {
                onStorageSet.invoke(key, value)
            }
        }

        @JavascriptInterface
        fun onStorageRemove(key: String) {
            scope.launch(Dispatchers.IO) {
                onStorageRemove.invoke(key)
            }
        }

        @JavascriptInterface
        fun onStorageClear() {
            scope.launch(Dispatchers.IO) {
                onStorageClear.invoke()
            }
        }

        @JavascriptInterface
        fun onBackDepthChanged(hasSubView: Boolean) {
            onEngineBackDepthChanged(hasSubView)
        }

        @JavascriptInterface
        fun onPlayDurationTick(gameId: String, deltaSeconds: Long) {
            scope.launch(Dispatchers.IO) {
                onPlayDurationTick.invoke(gameId, deltaSeconds)
            }
        }
    }
}
