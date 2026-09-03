package com.example.domain.model

enum class DownloadStatus {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    PAUSED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class MediaType {
    VIDEO,
    AUDIO,
    STREAM
}

data class MediaFormat(
    val id: String,
    val qualityLabel: String, // e.g. "1080p", "720p", "480p", "360p", "Audio Only"
    val url: String,
    val extension: String, // "mp4", "ts", "m3u8", "mp3", "m4a"
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0f,
    val bitrate: Long = 0L,
    val estimatedBytes: Long = 0L,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true,
    val isHlsVariant: Boolean = false
)

data class AudioTrackInfo(
    val id: String,
    val language: String,
    val name: String,
    val groupId: String? = null,
    val uri: String? = null,
    val isDefault: Boolean = false,
    val autoSelect: Boolean = false
)

data class SubtitleTrackInfo(
    val id: String,
    val language: String,
    val name: String,
    val uri: String,
    val isDefault: Boolean = false
)

data class MediaInfo(
    val id: String,
    val title: String,
    val thumbnail: String? = null,
    val durationSeconds: Long = 0L,
    val platform: String = "Web",
    val sourceUrl: String,
    val mediaType: MediaType = MediaType.VIDEO,
    val formats: List<MediaFormat> = emptyList(),
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val isHls: Boolean = false,
    val isLive: Boolean = false,
    val isDrmProtected: Boolean = false,
    val description: String? = null,
    val author: String? = null
)

sealed class AnalysisResult {
    data class Success(val mediaInfo: MediaInfo) : AnalysisResult()
    data class DrmProtected(val message: String = "This stream is DRM-protected and cannot be downloaded.") : AnalysisResult()
    data class Error(val errorType: ErrorType, val message: String) : AnalysisResult()
}

enum class ErrorType {
    INVALID_URL,
    UNSUPPORTED_PLATFORM,
    MEDIA_NOT_FOUND,
    NETWORK_ERROR,
    TIMEOUT,
    SERVER_ERROR,
    HLS_PARSE_ERROR,
    DRM_PROTECTED,
    AUTH_REQUIRED,
    STORAGE_ERROR,
    INSUFFICIENT_STORAGE,
    UNKNOWN_ERROR
}
