package cz.erza.instergram

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
        //webView.settings.domStorageEnabled = true
        //webView.settings.databaseEnabled = true

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
    }

}
private fun injectCSS(webView: WebView?){
    try {
        val css = "a[href^=\"/reels\"] {display: none} button[type^=\"button\"]{display: none} ._aagu{display:none}" //your css as String
        val js = "if(location == 'https://instagram.com/') location = '/?variant=following';" +
                "var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);"
        webView?.evaluateJavascript(js, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
