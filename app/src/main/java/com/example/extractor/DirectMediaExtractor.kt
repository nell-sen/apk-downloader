package com.example.extractor

import com.example.core.network.NetworkModule
import com.example.core.parser.HlsParser
import com.example.core.parser.HlsUrlResolver
import com.example.domain.model.AudioTrackInfo
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import com.example.domain.model.MediaType
import com.example.domain.model.SubtitleTrackInfo
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class DirectMediaExtractor : PlatformExtractor {

    private val DIRECT_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "mov", "m4v",
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "zip",
        "m3u8", "ts"
    )

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().substringBefore("?")
        val ext = lower.substringAfterLast(".", "")
        return ext in DIRECT_EXTENSIONS
    }

    override suspend fun analyze(url: String, headers: Map<String, String>): MediaInfo? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.header(k, v) }
            val request = requestBuilder.build()

            val response = NetworkModule.okHttpClient.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            val disposition = response.header("Content-Disposition") ?: ""

            var fileName = "media_file"
            if (disposition.contains("filename=")) {
                val extracted = disposition.substringAfter("filename=").substringBefore(";").trim('"', ' ', '\'')
                if (extracted.isNotEmpty()) fileName = extracted
            } else {
                val pathSegment = URI(finalUrl).path.substringAfterLast("/").substringBefore("?")
                if (pathSegment.isNotEmpty()) fileName = pathSegment
            }

            val lowerUrl = finalUrl.lowercase().substringBefore("?")
            val isHlsContentType = contentType.contains("mpegurl") || contentType.contains("application/x-mpegurl") || contentType.contains("application/vnd.apple.mpegurl")
            val isHlsUrl = lowerUrl.endsWith(".m3u8")

            if (isHlsContentType || isHlsUrl) {
                val body = response.body?.string() ?: ""
                return@withContext parseHlsStream(finalUrl, body, fileName, headers)
            }

            // Check if response body is actually HLS text despite contentType
            if (contentType.startsWith("text/") || contentType.startsWith("application/octet-stream")) {
                val peekBody = response.peekBody(1024).string()
                if (HlsParser.isHlsManifest(peekBody)) {
                    val fullBody = response.body?.string() ?: ""
                    return@withContext parseHlsStream(finalUrl, fullBody, fileName, headers)
                }
            }

            val isAudio = contentType.startsWith("audio/") || lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".m4a") || lowerUrl.endsWith(".aac") || lowerUrl.endsWith(".flac") || lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".ogg") || lowerUrl.endsWith(".opus")
            val ext = fileName.substringAfterLast(".", if (isAudio) "mp3" else "mp4")
            val mime = if (contentType.isNotEmpty()) contentType else if (isAudio) "audio/$ext" else "video/$ext"

            val format = MediaFormat(
                id = UUID.randomUUID().toString(),
                qualityLabel = if (isAudio) "Original Audio" else "Standard Quality",
                url = finalUrl,
                extension = ext,
                mimeType = mime,
                estimatedBytes = contentLength,
                hasVideo = !isAudio,
                hasAudio = true
            )

            MediaInfo(
                id = UUID.randomUUID().toString(),
                title = fileName.substringBeforeLast("."),
                sourceUrl = url,
                platform = "Direct Media",
                mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
                formats = listOf(format),
                headers = headers,
                isHls = false
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun parseHlsStream(
        streamUrl: String,
        manifestContent: String,
        fallbackTitle: String,
        headers: Map<String, String>
    ): MediaInfo? {
        val isDrm = HlsParser.isDrmProtected(manifestContent)
        val isMaster = HlsParser.isMasterPlaylist(manifestContent)

        val formats = mutableListOf<MediaFormat>()
        val audioTracks = mutableListOf<AudioTrackInfo>()
        val subtitleTracks = mutableListOf<SubtitleTrackInfo>()

        var isLive = false
        var duration = 0L

        if (isMaster) {
            val master = HlsParser.parseMasterPlaylist(manifestContent, streamUrl)
            
            master.audioTracks.forEach { a ->
                audioTracks.add(
                    AudioTrackInfo(
                        id = a.groupId,
                        language = a.language ?: "und",
                        name = a.name,
                        groupId = a.groupId,
                        uri = a.uri,
                        isDefault = a.isDefault,
                        autoSelect = a.autoSelect
                    )
                )
            }

            master.subtitleTracks.forEach { s ->
                subtitleTracks.add(
                    SubtitleTrackInfo(
                        id = s.groupId,
                        language = s.language ?: "und",
                        name = s.name,
                        uri = s.uri ?: "",
                        isDefault = s.isDefault
                    )
                )
            }

            // Inspect the first media variant to get duration and check live status
            master.variants.forEachIndexed { index, variant ->
                var variantEstimatedBytes = 0L
                if (variant.bandwidth > 0 && duration > 0) {
                    variantEstimatedBytes = (variant.bandwidth * duration) / 8
                }

                formats.add(
                    MediaFormat(
                        id = "variant_$index",
                        qualityLabel = variant.qualityLabel,
                        url = variant.url,
                        extension = "ts",
                        mimeType = "video/mp2t",
                        width = variant.width,
                        height = variant.height,
                        fps = variant.frameRate,
                        bitrate = variant.bandwidth,
                        estimatedBytes = variantEstimatedBytes,
                        videoCodec = variant.codecs,
                        hasVideo = variant.height > 0 || variant.width > 0,
                        hasAudio = true,
                        isHlsVariant = true
                    )
                )
            }

            // If we have variants, fetch the highest variant to get actual duration / isLive
            if (master.variants.isNotEmpty()) {
                val primaryVariantUrl = master.variants.first().url
                try {
                    val varReq = Request.Builder().url(primaryVariantUrl).build()
                    val varResp = NetworkModule.okHttpClient.newCall(varReq).execute()
                    val varBody = varResp.body?.string() ?: ""
                    val mediaPl = HlsParser.parseMediaPlaylist(varBody, primaryVariantUrl)
                    isLive = mediaPl.isLive
                    duration = mediaPl.totalDurationSeconds.toLong()
                } catch (e: Exception) {
                    // non-fatal
                }
            }
        } else {
            val mediaPl = HlsParser.parseMediaPlaylist(manifestContent, streamUrl)
            isLive = mediaPl.isLive
            duration = mediaPl.totalDurationSeconds.toLong()

            formats.add(
                MediaFormat(
                    id = "hls_stream_0",
                    qualityLabel = "HLS Stream",
                    url = streamUrl,
                    extension = "ts",
                    mimeType = "video/mp2t",
                    estimatedBytes = 0L,
                    hasVideo = true,
                    hasAudio = true,
                    isHlsVariant = true
                )
            )
        }

        return MediaInfo(
            id = UUID.randomUUID().toString(),
            title = fallbackTitle.ifBlank { "HLS Video Stream" },
            sourceUrl = streamUrl,
            platform = "HLS",
            mediaType = MediaType.STREAM,
            formats = formats,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            headers = headers,
            isHls = true,
            isLive = isLive,
            isDrmProtected = isDrm,
            durationSeconds = duration
        )
    }
}
