package com.example.extractor

import com.example.core.network.NetworkModule
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaType
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class YouTubePlatformExtractor : PlatformExtractor {
    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            // Use public oEmbed API for legal title & thumbnail extraction
            val oembedUrl = "https://www.youtube.com/oembed?url=$url&format=json"
            val req = Request.Builder().url(oembedUrl).build()
            val resp = NetworkModule.okHttpClient.newCall(req).execute()

            var title = "YouTube Video"
            var thumbnail: String? = null
            var author: String? = null

            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                title = json.optString("title", "YouTube Video")
                thumbnail = json.optString("thumbnail_url", null)
                author = json.optString("author_name", null)
            }

            // Fallback to GenericWebExtractor if direct downloadable media link is present
            val generic = GenericWebExtractor()
            val webMedia = generic.analyze(url, headers)

            val formats = webMedia?.formats?.ifEmpty { null } ?: listOf(
                MediaFormat(
                    id = "yt_direct_standard",
                    qualityLabel = "Standard Stream",
                    url = url,
                    extension = "mp4",
                    mimeType = "video/mp4",
                    hasVideo = true,
                    hasAudio = true
                )
            )

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = title,
                thumbnail = thumbnail ?: webMedia?.thumbnail,
                platform = "YouTube",
                sourceUrl = url,
                mediaType = MediaType.VIDEO,
                formats = formats,
                author = author,
                headers = headers,
                isHls = webMedia?.isHls ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}

class TikTokPlatformExtractor : PlatformExtractor {
    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("tiktok.com")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val oembedUrl = "https://www.tiktok.com/oembed?url=$url"
            val req = Request.Builder().url(oembedUrl).build()
            val resp = NetworkModule.okHttpClient.newCall(req).execute()

            var title = "TikTok Video"
            var thumbnail: String? = null
            var author: String? = null

            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                title = json.optString("title", "TikTok Video")
                thumbnail = json.optString("thumbnail_url", null)
                author = json.optString("author_name", null)
            }

            val generic = GenericWebExtractor()
            val webMedia = generic.analyze(url, headers)

            val formats = webMedia?.formats ?: listOf(
                MediaFormat(
                    id = "tiktok_fmt_0",
                    qualityLabel = "HD Video",
                    url = url,
                    extension = "mp4",
                    mimeType = "video/mp4",
                    hasVideo = true,
                    hasAudio = true
                )
            )

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = title,
                thumbnail = thumbnail ?: webMedia?.thumbnail,
                platform = "TikTok",
                sourceUrl = url,
                mediaType = MediaType.VIDEO,
                formats = formats,
                author = author,
                headers = headers
            )
        } catch (e: Exception) {
            null
        }
    }
}

class VimeoPlatformExtractor : PlatformExtractor {
    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("vimeo.com")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val oembedUrl = "https://vimeo.com/api/oembed.json?url=$url"
            val req = Request.Builder().url(oembedUrl).build()
            val resp = NetworkModule.okHttpClient.newCall(req).execute()

            var title = "Vimeo Video"
            var thumbnail: String? = null
            var duration = 0L

            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                title = json.optString("title", "Vimeo Video")
                thumbnail = json.optString("thumbnail_url", null)
                duration = json.optLong("duration", 0L)
            }

            val generic = GenericWebExtractor()
            val webMedia = generic.analyze(url, headers)

            val formats = webMedia?.formats ?: listOf(
                MediaFormat(
                    id = "vimeo_fmt_0",
                    qualityLabel = "Standard Quality",
                    url = url,
                    extension = "mp4",
                    mimeType = "video/mp4",
                    hasVideo = true,
                    hasAudio = true
                )
            )

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = title,
                thumbnail = thumbnail ?: webMedia?.thumbnail,
                platform = "Vimeo",
                sourceUrl = url,
                durationSeconds = duration,
                mediaType = MediaType.VIDEO,
                formats = formats,
                headers = headers
            )
        } catch (e: Exception) {
            null
        }
    }
}

class SoundCloudPlatformExtractor : PlatformExtractor {
    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("soundcloud.com")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val oembedUrl = "https://soundcloud.com/oembed?format=json&url=$url"
            val req = Request.Builder().url(oembedUrl).build()
            val resp = NetworkModule.okHttpClient.newCall(req).execute()

            var title = "SoundCloud Track"
            var thumbnail: String? = null
            var author: String? = null

            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                title = json.optString("title", "SoundCloud Track")
                thumbnail = json.optString("thumbnail_url", null)
                author = json.optString("author_name", null)
            }

            val generic = GenericWebExtractor()
            val webMedia = generic.analyze(url, headers)

            val formats = webMedia?.formats ?: listOf(
                MediaFormat(
                    id = "sc_audio_0",
                    qualityLabel = "HQ Audio",
                    url = url,
                    extension = "mp3",
                    mimeType = "audio/mpeg",
                    hasVideo = false,
                    hasAudio = true
                )
            )

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = title,
                thumbnail = thumbnail ?: webMedia?.thumbnail,
                platform = "SoundCloud",
                sourceUrl = url,
                mediaType = MediaType.AUDIO,
                formats = formats,
                author = author,
                headers = headers
            )
        } catch (e: Exception) {
            null
        }
    }
}

class TwitchPlatformExtractor : PlatformExtractor {
    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("twitch.tv")
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        val generic = GenericWebExtractor()
        val webMedia = generic.analyze(url, headers)
        webMedia?.copy(platform = "Twitch") ?: MediaInfo(
            id = UUID.randomUUID().toString(),
            title = "Twitch Stream",
            sourceUrl = url,
            platform = "Twitch",
            mediaType = MediaType.STREAM,
            formats = listOf(
                MediaFormat(
                    id = "twitch_stream",
                    qualityLabel = "Live Stream",
                    url = url,
                    extension = "m3u8",
                    mimeType = "application/x-mpegURL",
                    isHlsVariant = true
                )
            ),
            isHls = true,
            isLive = true
        )
    }
}
