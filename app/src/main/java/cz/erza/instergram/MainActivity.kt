package cz.erza.instergram

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cz.erza.instergram.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val CHANNEL_ID = "instergram_notifications"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Instagram Notifications"
            val descriptionText = "Notifications from Instagram Web"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        binding.webView.webViewClient = MyWebViewClient()
        binding.webView.addJavascriptInterface(NotificationInterface(this, binding.webView), "AndroidNotification")

        // Load a web page
        val url = "https://instagram.com/direct/inbox"

        CookieManager.getInstance().setAcceptCookie(true)
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        binding.webView.loadUrl(url)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    inner class NotificationInterface(private val context: Context, private val webView: WebView) {

        @JavascriptInterface
        fun requestPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                } else {
                    sendPermissionResult("granted")
                }
            } else {
                sendPermissionResult("granted")
            }
        }

        @JavascriptInterface
        fun getPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    "granted"
                } else {
                    "default"
                }
            } else {
                "granted"
            }
        }

        @JavascriptInterface
        fun showNotification(title: String, body: String) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                with(NotificationManagerCompat.from(context)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            }
        }

        private fun sendPermissionResult(result: String) {
            webView.post {
                webView.evaluateJavascript("if (window.onNotificationPermissionResult) window.onNotificationPermissionResult('$result');", null)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val result = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) "granted" else "denied"
            binding.webView.evaluateJavascript("if (window.onNotificationPermissionResult) window.onNotificationPermissionResult('$result');", null)
        }
    }

    private class MyWebViewClient() : WebViewClient() {

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(url != null && url.contains("instagram.com/upload")) return true
            injectCSS(view)
            return false
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            // Removed injectCSS from here to prevent resource exhaustion and crashes
            if(view?.url == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")

            super.onLoadResource(view, url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if(view?.url == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")

            injectCSS(view)
            super.onPageFinished(view, url)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if(url == "https://www.instagram.com/") {
                view?.loadUrl("https://www.instagram.com/?variant=following")
            } else {
                injectCSS(view)
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        }
    }
}

fun injectCSS(webView: WebView?, upload: Boolean = false){
    try {
        val css = "a[href^=\"/reels\"] {display: none}  a[href^=\"https://www.threads.net/\"]{display: none}"
        val js = """
            (function() {
                // Polyfill for Notification API
                if (!window.NotificationPolyfilled) {
                    window.Notification = function(title, options) {
                        this.title = title;
                        this.options = options || {};
                        AndroidNotification.showNotification(this.title, this.options.body || "");
                    };

                    window.Notification.permission = AndroidNotification.getPermission();
                    
                    window.Notification.requestPermission = function(callback) {
                        return new Promise(function(resolve, reject) {
                            window.onNotificationPermissionResult = function(result) {
                                window.Notification.permission = result;
                                if (callback) callback(result);
                                resolve(result);
                            };
                            AndroidNotification.requestPermission();
                        });
                    };
                    
                    window.NotificationPolyfilled = true;
                    console.log("Notification API polyfilled");
                }

                var style = document.getElementById('injected-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'injected-style';
                    document.head.appendChild(style);
                }
                style.innerHTML = '$css';
                
                if (!window.injectedObserver) {
                    var observer = new MutationObserver(function(mutations) {
                        if(document.location.href == 'https://www.instagram.com/?variant=following') {
                            var backBtn = document.querySelectorAll("svg[aria-label='Back']")[0];
                            if (backBtn) backBtn.style.display = "none";
                        }
                        document.querySelectorAll("._abl-").forEach((elem) => elem.style.display = "block");
                        if(document.location.href.includes("/explore/")) {
                            document.querySelectorAll("._aagu").forEach((elem) => elem.style.display = "none");
                        }
                        document.querySelectorAll("a[href='/']").forEach((elem) => elem.href = "/?variant=following");
                        if(document.location.href == 'https://www.instagram.com/') document.location = '/?variant=following';
                    });
                    observer.observe(document.body, {childList: true, subtree: true});
                    window.injectedObserver = true;
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
        
        if(upload){
            val uploadJs = """
                (function() {
                    if (!window.uploadObserver) {
                        var observ = new MutationObserver(function(mutations) {
                            var maxHeightDiv = document.querySelector('div[style^="max-height"]');
                            if (maxHeightDiv) maxHeightDiv.style = "max-height: 100%; min-width: 100px; max-width: 80%; width: 100px;";
                            
                            var minWidthDiv = document.querySelector('div[style^="min-width"]');
                            if (minWidthDiv) minWidthDiv.style = "max-height: 100%; min-width: 100px; max-width: 80%; width: 100px;";
                            
                            var photoDiv = document.querySelector('div:has(> div > div > div > div > img[alt="Photo for tag placement"])');
                            if (photoDiv) photoDiv.style = "height: 50px; width: 50px";
                        });
                        observ.observe(document.body, {childList: true, subtree: true});
                        window.uploadObserver = true;
                    }
                })();
            """.trimIndent()
            webView?.evaluateJavascript(uploadJs, null)
        }
    } catch (e: Exception) {
        Log.e("Instergram", "Error injecting CSS/JS", e)
    }
}

