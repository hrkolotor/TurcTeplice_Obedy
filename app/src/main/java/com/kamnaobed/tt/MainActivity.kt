package com.kamnaobed.tt

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabs: TabLayout
    private lateinit var web: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var repo: MenuRepository

    private val prefs by lazy { getSharedPreferences("app", Context.MODE_PRIVATE) }
    private var restaurants: List<Restaurant> = emptyList()
    private var suppressTabEvents = false
    private var loading = false
    private var fetched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        tabs = findViewById(R.id.tabs)
        web = findViewById(R.id.web)
        swipe = findViewById(R.id.swipe)
        setSupportActionBar(toolbar)
        repo = MenuRepository(this)

        web.settings.javaScriptEnabled = true
        web.webViewClient = object : WebViewClient() {
            // Každý klik (PDF menu, telefón, e-mail, web reštaurácie) otvoríme mimo appky.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                openExternal(request.url)
                return true
            }
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!suppressTabEvents) showRestaurant(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = showRestaurant(tab.position)
        })

        swipe.setOnRefreshListener { refresh(manual = true) }

        lifecycleScope.launch {
            // Kým sa stiahne čerstvá verzia, ukáž poslednú uloženú.
            repo.loadCached()?.let { if (!fetched) applyData(it) }
        }

        // Pri spustení appky vždy stiahni aktuálnu podstránku.
        refresh(manual = false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> { refresh(manual = true); true }
        R.id.action_open_web -> { openExternal(Uri.parse(MenuRepository.SOURCE_URL)); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refresh(manual: Boolean) {
        if (loading) return
        loading = true
        swipe.isRefreshing = true
        lifecycleScope.launch {
            val result = runCatching { repo.fetch() }
            loading = false
            swipe.isRefreshing = false
            result.onSuccess {
                fetched = true
                applyData(it)
            }.onFailure {
                // Pri spustení bez siete ukáž hlášku len ak nie je nič uložené.
                if (manual || restaurants.isEmpty()) {
                    Toast.makeText(this@MainActivity, R.string.error_fetch, Toast.LENGTH_LONG).show()
                }
                if (restaurants.isEmpty()) showMessage(getString(R.string.error_no_data))
            }
        }
    }

    private fun applyData(data: MenuData) {
        val wanted = restaurants.getOrNull(tabs.selectedTabPosition)?.name
            ?: prefs.getString(KEY_TAB, null)
        restaurants = data.restaurants

        val time = SimpleDateFormat("d. M. HH:mm", Locale.forLanguageTag("sk")).format(Date(data.fetchedAt))
        toolbar.subtitle = getString(
            if (data.fromCache) R.string.subtitle_offline else R.string.subtitle_online, time
        )

        suppressTabEvents = true
        tabs.removeAllTabs()
        restaurants.forEach { tabs.addTab(tabs.newTab().setText(it.name), false) }
        val index = restaurants.indexOfFirst { it.name == wanted }.coerceAtLeast(0)
        tabs.getTabAt(index)?.select()
        suppressTabEvents = false

        if (restaurants.isEmpty()) {
            // Štruktúra stránky sa asi zmenila – zobrazíme ju aspoň celú.
            Toast.makeText(this, R.string.error_parse, Toast.LENGTH_LONG).show()
            web.loadUrl(MenuRepository.SOURCE_URL)
        } else {
            showRestaurant(index)
        }
    }

    private fun showRestaurant(index: Int) {
        val r = restaurants.getOrNull(index) ?: return
        prefs.edit().putString(KEY_TAB, r.name).apply()
        web.loadDataWithBaseURL(
            MenuRepository.SOURCE_URL,
            HtmlTemplate.build(r, Today.now()),
            "text/html", "utf-8", null,
        )
    }

    private fun showMessage(text: String) {
        val html = "<html><body style='font:16px sans-serif;padding:32px;text-align:center;color:#888'>" +
            text + "</body></html>"
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val KEY_TAB = "selected_tab"
    }
}
