package com.kamnaobed.tt

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

data class MenuData(
    val restaurants: List<Restaurant>,
    val fetchedAt: Long,
    val fromCache: Boolean,
)

class MenuRepository(context: Context) {

    companion object {
        const val SOURCE_URL = "https://www.turciansketeplice.sk/kam-na-obed-1.html"
        private const val CACHE_FILE = "kam-na-obed.html"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36"
    }

    private val cacheFile = File(context.filesDir, CACHE_FILE)

    /** Stiahne aktuálnu stránku, rozparsuje ju a uloží pre offline režim. */
    suspend fun fetch(): MenuData = withContext(Dispatchers.IO) {
        val html = Jsoup.connect(SOURCE_URL)
            .userAgent(USER_AGENT)
            .timeout(20_000)
            // Vždy čerstvá verzia, žiadna medzipamäť po ceste.
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .execute()
            .body()
        val restaurants = MenuParser.parse(html, SOURCE_URL)
        cacheFile.writeText(html)
        MenuData(restaurants, System.currentTimeMillis(), fromCache = false)
    }

    /** Naposledy stiahnutá verzia (ak existuje). */
    suspend fun loadCached(): MenuData? = withContext(Dispatchers.IO) {
        runCatching {
            if (!cacheFile.exists()) return@runCatching null
            MenuData(
                MenuParser.parse(cacheFile.readText(), SOURCE_URL),
                cacheFile.lastModified(),
                fromCache = true,
            )
        }.getOrNull()
    }
}
