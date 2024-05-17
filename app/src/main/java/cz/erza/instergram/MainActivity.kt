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
import androidx.core.app.ActivityCompat.startActivityForResult
import cz.erza.instergram.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var filePath: ValueCallback<Array<Uri?>?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = MyWebViewClient(MainActivity())
        webView.webChromeClient = MyWebChromeClient(this);

        // Load a web page
        val url = "https://instagram.com/direct/inbox"

        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.javaScriptEnabled = true

        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess =true
        /*val newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
        webView.getSettings().setUserAgentString(newUA)*/

        webView.loadUrl(url)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 5){
            filePath!!.onReceiveValue( WebChromeClient.FileChooserParams.parseResult(RESULT_OK, data));
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
        } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data))
            filePath = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val myWebView: WebView = findViewById(R.id.webView)
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)

    }
    private class MyWebChromeClient(private val myActivity: MainActivity) : WebChromeClient(){
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>?>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            myActivity.filePath = filePathCallback

            val inte = fileChooserParams!!.createIntent()
            inte.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            //myActivity.getFile.launch(inte)
            //startActivityForResult(myActivity, inte, 5, null);
            //filePathCallback!!.onReceiveValue( FileChooserParams.parseResult(RESULT_OK, inte));

            //val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
            /*contentIntent.type = "*//*"
            contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            contentIntent.addCategory(Intent.CATEGORY_OPENABLE)*/
            //createChooser(contentIntent, "Fotky")

            //val inte = createChooser(contentIntent, "Fotky")

            startActivityForResult(myActivity, inte, 5, null);
            return true
        }
    }
    private class MyWebViewClient(private val myActivity: MainActivity) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(url == "instagram.com/upload") return true
            injectCSS(view)
            return false
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
    try {//button[type^="button"]{display: none}
        val css = "a[href^=\"/reels\"] {display: none}  a[href^=\"https://www.threads.net/\"]{display: none}" //your css as String
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
