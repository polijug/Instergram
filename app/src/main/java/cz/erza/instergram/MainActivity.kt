package cz.erza.instergram

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import cz.erza.instergram.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var filePath: ValueCallback<Array<Uri?>?>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.webViewClient = MyWebViewClient()
        binding.webView.webChromeClient = MyWebChromeClient(this)

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

    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            uriFormate(result.data)
        } else {
            null
        }
        filePath?.onReceiveValue(uris)
        filePath = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private class MyWebChromeClient(private val myActivity: MainActivity) : WebChromeClient(){
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>?>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            myActivity.filePath = filePathCallback

            val intent = fileChooserParams?.createIntent()
            if (intent != null) {
                myActivity.getFile.launch(intent)
                return true
            }
            return false
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

fun uriFormate(data: Intent?): Array<Uri?>? {
    if (data == null) return null
    
    val clipData = data.clipData
    if (clipData != null && clipData.itemCount > 0) {
        val ret = arrayOfNulls<Uri>(clipData.itemCount)
        for (i in 0 until clipData.itemCount) {
            ret[i] = clipData.getItemAt(i).uri
        }
        return ret
    }
    
    val dataUri = data.data
    if (dataUri != null) {
        return arrayOf(dataUri)
    }
    
    return null
}
