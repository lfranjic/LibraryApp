package com.example.libraryapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.libraryapp.models.ToastUtils

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val js = """
                    javascript:(function() {
                        try {
                            // Hide <div> and <li> elements containing specific keywords in their text
                            function hideElementsWithText(tag, keywords) {
                                var elems = document.getElementsByTagName(tag);
                                for (var i = 0; i < elems.length; i++) {
                                    var elem = elems[i];
                                    var text = elem.innerText || elem.textContent || '';
                                    for (var k = 0; k < keywords.length; k++) {
                                        if (text.includes(keywords[k])) {
                                            elem.style.display = 'none';
                                            break;
                                        }
                                    }
                                }
                            }

                            // Define keywords to hide
                            var keywords = [
                                "Sort alphabetically by title",
                                "Sort by release date",
                                "Authors",
                                "Subjects"
                            ];

                            hideElementsWithText('div', keywords);
                            hideElementsWithText('li', keywords);

                        } catch(e) {
                            console.log('JS Error:', e);
                        }
                    })();
                """
                view?.evaluateJavascript(js, null)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    view?.loadUrl(it)
                    return true
                }
                return false
            }
        }

        val url = intent.getStringExtra("url")
        if (url != null) {
            webView.loadUrl(url)
        } else {
            ToastUtils.showCustomToast(this, "No URL provided")
        }
    }
}