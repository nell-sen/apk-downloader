package com.example.core.parser

import java.net.URI

data class HlsMasterPlaylist(
    val variants: List<HlsVariantStream>,
    val audioTracks: List<HlsAudioTrack> = emptyList(),
    val subtitleTracks: List<HlsSubtitleTrack> = emptyList()
)

data class HlsVariantStream(
    val bandwidth: Long,
    val resolution: String? = null, // e.g. "1920x1080"
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val codecs: String? = null,
    val audioGroupId: String? = null,
    val subtitleGroupId: String? = null,
    val url: String
) {
    val qualityLabel: String
        get() = when {
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height >= 360 -> "360p"
            height >= 240 -> "240p"
            height > 0 -> "${height}p"
            bandwidth > 0 -> "${bandwidth / 1000} kbps"
            else -> "Auto"
        }
}

data class HlsAudioTrack(
    val groupId: String,
    val name: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val autoSelect: Boolean = false,
    val uri: String? = null
)

data class HlsSubtitleTrack(
    val groupId: String,
    val name: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val uri: String? = null
)

data class HlsSegment(
    val index: Int,
    val durationSeconds: Double,
    val url: String,
    val encryptionKey: HlsEncryptionKey? = null,
    val title: String? = null
)

data class HlsEncryptionKey(
    val method: String, // "AES-128", "SAMPLE-AES", "NONE"
    val uri: String?,
    val iv: String? = null,
    val keyFormat: String? = null
)

data class HlsMediaPlaylist(
    val targetDurationSeconds: Int = 0,
    val mediaSequence: Long = 0L,
    val isLive: Boolean = false,
    val isDrmProtected: Boolean = false,
    val segments: List<HlsSegment> = emptyList(),
    val totalDurationSeconds: Double = 0.0
)

object HlsUrlResolver {
    fun resolveUrl(baseUrl: String, relativeOrAbsoluteUrl: String): String {
        val child = relativeOrAbsoluteUrl.trim()
        if (child.startsWith("http://") || child.startsWith("https://")) {
            return child
        }

        return try {
            val baseUri = URI(baseUrl.trim())
            val resolvedUri = baseUri.resolve(child)
            
            // Preserve parent query parameters if child does not have query params and parent does
            var result = resolvedUri.toString()
            if (!child.contains("?") && baseUri.query != null && !baseUri.query.isNullOrBlank()) {
                val separator = if (result.contains("?")) "&" else "?"
                result = "$result$separator${baseUri.query}"
            }
            result
        } catch (e: Exception) {
            // Fallback manual resolution
            if (child.startsWith("/")) {
                val schemeAndHost = baseUrl.substringBefore("://") + "://" + baseUrl.substringAfter("://").substringBefore("/")
                schemeAndHost + child
            } else {
                val basePath = baseUrl.substringBeforeLast("/") + "/"
                basePath + child
            }
        }
    }
}
