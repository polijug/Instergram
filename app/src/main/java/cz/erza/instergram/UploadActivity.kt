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
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UploadActivity : AppCompatActivity() {
    var filePath: ValueCallback<Array<Uri?>?>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_upload)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button: Button = findViewById(R.id.upload)
        button.setOnClickListener {
            val intent = Intent(this@UploadActivity, MainActivity::class.java)
            startActivity(intent)
        }

        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = UploadActivity.MyWebViewClient(MainActivity())
        webView.webChromeClient = UploadActivity.MyWebChromeClient(this);

        // Load a web page
        val url = "https://instagram.com/direct/inbox"

        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.javaScriptEnabled = true

        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess =true
        val newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
        webView.getSettings().setUserAgentString(newUA)

        webView.loadUrl(url)
    }
    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
        } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
            filePath!!.onReceiveValue(
                uriFormate(it.data, it.resultCode))
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
    private class MyWebChromeClient(private val myActivity: UploadActivity) : WebChromeClient(){
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>?>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            myActivity.filePath = filePathCallback

            val inte = fileChooserParams!!.createIntent()
            //inte.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            myActivity.getFile.launch(inte)
            return true
        }
    }

    //parse array of files - how? i need tp know the format
    private class MyWebViewClient(private val myActivity: MainActivity) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(url == "instagram.com/upload") return true
            injectCSS(view, true)
            return false
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            injectCSS(view, true)
            print(view?.url);
            if(view?.getUrl() == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            super.onLoadResource(view, url)
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            if(view?.getUrl() == "https://instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            injectCSS(view, true)
            super.onPageFinished(view, url)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if(view?.getUrl() == "https://www.instagram.com/")
                view.loadUrl("https://www.instagram.com/?variant=following")
            injectCSS(view, true)
            super.doUpdateVisitedHistory(view, url, isReload)
        }
    }
}
