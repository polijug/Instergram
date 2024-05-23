package cz.erza.instergram

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = MyWebViewClient()

        // Load a web page
        val url = "https://instagram.com/direct/inbox"

        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.javaScriptEnabled = true

        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess =true

        webView.loadUrl(url)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val myWebView: WebView = findViewById(R.id.webView)
        // Check whether the key event is the Back button and if there's history.
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }
        // If it isn't the Back button or there isn't web page history, bubble up to
        // the default system behavior. Probably exit the activity.
        return super.onKeyDown(keyCode, event)

    }
    private class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(url == "instagram.com/upload") return true;
            injectCSS(view)
            return false;
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            injectCSS(view)
            print(view?.url);
            if(view?.getUrl() == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            super.onLoadResource(view, url)
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            if(view?.getUrl() == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            injectCSS(view)
            super.onPageFinished(view, url)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if(view?.getUrl() == "https://www.instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            injectCSS(view)
            super.doUpdateVisitedHistory(view, url, isReload)
        }
    }

}
private fun injectCSS(webView: WebView?){
    try {
        val css = "a[href^=\"/reels\"] {display: none} button[type^=\"button\"]{display: none}  a[href^=\"https://www.threads.net/\"]{display: none}" //your css as String
        val js = "var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);"
        webView?.evaluateJavascript(js, null)
        webView?.evaluateJavascript("window.onload = function() {\n" +
                "    var bodyList = document.querySelector(\"body\")\n" +
                "    var observer = new MutationObserver(function(mutations) {\n" +
                "if(document.location.href == 'https://www.instagram.com/?variant=following') document.querySelectorAll(\"svg[aria-label='Back']\")[0].style.display =  \"none\";" +
                "   document.querySelectorAll(\"._abl-\").forEach( (elem) => elem.style.display = \"block\");" +
                "if(document.location.href == 'https://www.instagram.com/explore/') document.querySelectorAll(\"._aagu\").forEach( (elem) => elem.style.display = \"none\");" +
                "document.querySelectorAll(\"a[href='/']\").forEach( (elem) => elem.href = \"/?variant=following\");" +
                "        if(document.location.href == 'https://www.instagram.com/') document.location = '/?variant=following';\n" +
                "    });\n" +
                "    var config = {childList: true, subtree: false};\n" +
                "    observer.observe(bodyList, config);}; \n", null);
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
