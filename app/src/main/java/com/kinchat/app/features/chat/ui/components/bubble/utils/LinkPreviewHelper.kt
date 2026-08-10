package com.kinchat.app.features.chat.ui.components.bubble.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.concurrent.ConcurrentHashMap

data class LinkPreviewData(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

object LinkPreviewCacheManager {
    val cache = ConcurrentHashMap<String, LinkPreviewData>()
}

suspend fun fetchLinkPreview(urlStr: String): LinkPreviewData? = withContext(Dispatchers.IO) {
    try {
        var url = urlStr
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get()
        val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() } ?: document.title()
        val imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() } ?: ""
        
        if (title.isBlank() && imageUrl.isBlank()) return@withContext null
        
        LinkPreviewData(url, title, "", imageUrl)
    } catch (e: Exception) { 
        null 
    }
}
