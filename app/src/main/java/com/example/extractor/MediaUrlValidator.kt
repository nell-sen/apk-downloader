package com.example.extractor

import com.example.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class ValidationResult(
    val isValidMedia: Boolean,
    val contentType: String?,
    val contentLength: Long?,
    val acceptRanges: Boolean,
    val finalUrl: String
)

object MediaUrlValidator {
    suspend fun validate(url: String, headers: Map<String, String> = emptyMap()): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder().url(url).head()
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            
            var response = NetworkModule.okHttpClient.newCall(reqBuilder.build()).execute()
            
            // If HEAD is not allowed, fallback to GET
            if (!response.isSuccessful && response.code == 405) {
                response.close()
                val getReq = Request.Builder().url(url).build()
                response = NetworkModule.okHttpClient.newCall(getReq).execute()
            }
            
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val contentLength = response.header("Content-Length")?.toLongOrNull()
            val acceptRanges = response.header("Accept-Ranges")?.lowercase() == "bytes"
            
            response.close()
            
            val isMedia = isMediaContentType(contentType) || hasMediaExtension(finalUrl)
            val isHtmlPage = contentType.contains("text/html")
            
            ValidationResult(
                isValidMedia = isMedia && !isHtmlPage,
                contentType = contentType,
                contentLength = contentLength,
                acceptRanges = acceptRanges,
                finalUrl = finalUrl
            )
        } catch (e: Exception) {
            ValidationResult(false, null, null, false, url)
        }
    }

    private fun isMediaContentType(contentType: String): Boolean {
        return contentType.startsWith("video/") || 
               contentType.startsWith("audio/") || 
               contentType.contains("application/vnd.apple.mpegurl") ||
               contentType.contains("application/x-mpegurl") ||
               contentType.contains("application/dash+xml") ||
               contentType.contains("application/octet-stream")
    }

    private fun hasMediaExtension(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".m3u8") || lower.endsWith(".mp3") ||
               lower.endsWith(".m4a") || lower.endsWith(".webm") || lower.endsWith(".mkv") ||
               lower.endsWith(".flv") || lower.endsWith(".avi") || lower.endsWith(".ts")
    }
}
