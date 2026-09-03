package com.example.extractor

import com.example.core.network.NetworkModule
import com.example.core.parser.HlsParser
import com.example.core.parser.HlsUrlResolver
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaType
import java.util.UUID
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class GenericWebExtractor : PlatformExtractor {

    override fun canHandle(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.header(k, v) }
            val request = requestBuilder.build()

            val response = NetworkModule.okHttpClient.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            val html = response.body?.string() ?: return@withContext null

            // If the response is actually an HLS manifest directly
            if (HlsParser.isHlsManifest(html)) {
                val direct = DirectMediaExtractor()
                return@withContext direct.analyze(finalUrl, headers)
            }

            // Extract metadata
            val title = extractTitle(html, finalUrl)
            val thumbnail = extractThumbnail(html, finalUrl)
            val detectedMediaUrls = extractMediaUrlsFromHtml(html, finalUrl)

            if (detectedMediaUrls.isEmpty()) {
                return@withContext null
            }

            val formats = mutableListOf<MediaFormat>()
            for ((index, mediaUrl) in detectedMediaUrls.withIndex()) {
                val lower = mediaUrl.lowercase()
                val isHls = lower.contains(".m3u8")
                val isAudio = lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".aac") || lower.contains(".ogg")
                val ext = if (isHls) "ts" else if (isAudio) "mp3" else "mp4"
                val mime = if (isHls) "video/mp2t" else if (isAudio) "audio/$ext" else "video/mp4"

                formats.add(
                    MediaFormat(
                        id = "fmt_$index",
                        qualityLabel = if (isHls) "HLS Stream" else if (isAudio) "Audio Track" else "Video Stream",
                        url = mediaUrl,
                        extension = ext,
                        mimeType = mime,
                        hasVideo = !isAudio,
                        hasAudio = true,
                        isHlsVariant = isHls
                    )
                )
            }

            val isHlsStream = formats.any { it.isHlsVariant }

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = title,
                thumbnail = thumbnail,
                platform = "Web Media",
                sourceUrl = url,
                mediaType = if (isHlsStream) MediaType.STREAM else MediaType.VIDEO,
                formats = formats,
                headers = headers,
                isHls = isHlsStream
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTitle(html: String, baseUrl: String): String {
        // og:title
        val ogTitleMatch = Regex("""<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        if (ogTitleMatch != null) return unescapeHtml(ogTitleMatch.groupValues[1])

        val twitterTitleMatch = Regex("""<meta\s+name=["']twitter:title["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        if (twitterTitleMatch != null) return unescapeHtml(twitterTitleMatch.groupValues[1])

        val titleMatch = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null) return unescapeHtml(titleMatch.groupValues[1].trim())

        return "Web Media"
    }

    private fun extractThumbnail(html: String, baseUrl: String): String? {
        val ogImage = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        if (ogImage != null) return HlsUrlResolver.resolveUrl(baseUrl, ogImage.groupValues[1])

        val twitterImage = Regex("""<meta\s+name=["']twitter:image["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        if (twitterImage != null) return HlsUrlResolver.resolveUrl(baseUrl, twitterImage.groupValues[1])

        return null
    }

    private fun extractMediaUrlsFromHtml(html: String, baseUrl: String): List<String> {
        val urls = LinkedHashSet<String>()

        // 1. og:video, og:video:url, og:video:secure_url
        val ogVideoPatterns = listOf(
            Regex("""<meta\s+property=["']og:video(:url|:secure_url)?["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta\s+name=["']twitter:player:stream["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )
        for (pat in ogVideoPatterns) {
            pat.findAll(html).forEach { match ->
                val raw = match.groupValues[2]
                if (raw.isNotBlank()) urls.add(HlsUrlResolver.resolveUrl(baseUrl, raw))
            }
        }

        // 2. <video src="...">, <audio src="...">, <source src="...">
        val srcRegex = Regex("""<(?:video|audio|source)[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        srcRegex.findAll(html).forEach { match ->
            val raw = match.groupValues[1]
            if (raw.isNotBlank()) urls.add(HlsUrlResolver.resolveUrl(baseUrl, raw))
        }

        // 3. Look for explicit .m3u8 or .mp4 occurrences inside scripts or json
        val streamRegex = Regex("""https?://[^\s"'<>\\]+?\.(?:m3u8|mp4|webm|mp3|m4a)(?:\?[^\s"'<>\\]*)?""", RegexOption.IGNORE_CASE)
        streamRegex.findAll(html).forEach { match ->
            urls.add(match.value)
        }

        return urls.toList()
    }

    private fun unescapeHtml(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
}
