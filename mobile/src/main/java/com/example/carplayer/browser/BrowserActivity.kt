package com.example.carplayer.browser

import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carplayer.R
import com.example.carplayer.databinding.ActivityBrowserBinding

class BrowserActivity : AppCompatActivity() {
    private var _binding: ActivityBrowserBinding? = null
    val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        setupSearchHandler()
    }


    private fun setupWebView() = with(binding.webView) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webViewClient = WebViewClient()
    }

    private fun setupSearchHandler() = with(binding) {
        goButton.setOnClickListener {
            val input = searchBar.text.toString().trim()
            if (input.isNotEmpty()) {
                val url = buildUrlFromInput(input)
                webView.loadUrl(url)
            }
        }

        backButton.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                finish() // or navigate up
            }
        }

        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                goButton.performClick()
                true
            } else false
        }
    }

    private fun buildUrlFromInput(input: String): String {
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${Uri.encode(input)}"
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}