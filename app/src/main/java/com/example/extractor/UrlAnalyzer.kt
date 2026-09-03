package com.example.extractor

import com.example.core.network.NetworkModule
import com.example.core.network.UrlSecurity
import com.example.core.parser.HlsParser
import com.example.domain.model.AnalysisResult
import com.example.domain.model.ErrorType
import com.example.domain.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object PlatformDetector {
    private val extractors: List<PlatformExtractor> = listOf(
        DirectMediaExtractor(),
        YouTubePlatformExtractor(),
        TikTokPlatformExtractor(),
        VimeoPlatformExtractor(),
        SoundCloudPlatformExtractor(),
        TwitchPlatformExtractor(),
        GenericWebExtractor()
    )

    fun findExtractor(url: String): PlatformExtractor {
        return extractors.firstOrNull { it.canHandle(url) } ?: GenericWebExtractor()
    }
}

class UrlAnalyzer {

    suspend fun analyzeUrl(rawUrl: String, customHeaders: Map<String, String> = emptyMap()): AnalysisResult = withContext(Dispatchers.IO) {
        val trimmedUrl = rawUrl.trim()

        if (!UrlSecurity.isSafeUrl(trimmedUrl)) {
            return@withContext AnalysisResult.Error(
                errorType = ErrorType.INVALID_URL,
                message = "The entered URL is invalid or not supported."
            )
        }

        try {
            // Pipeline Step 1 & 2: Normalize URL & Resolve Redirects
            val requestBuilder = Request.Builder().url(trimmedUrl)
            customHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
            val initialRequest = requestBuilder.build()

            val response = try {
                NetworkModule.okHttpClient.newCall(initialRequest).execute()
            } catch (e: Exception) {
                return@withContext AnalysisResult.Error(
                    errorType = ErrorType.NETWORK_ERROR,
                    message = "Network connection failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }

            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val peekSample = try {
                response.peekBody(2048).string()
            } catch (e: Exception) {
                ""
            }

            // Pipeline Step 3: Check DRM Protected stream immediately
            if (HlsParser.isDrmProtected(peekSample)) {
                return@withContext AnalysisResult.DrmProtected()
            }

            // Pipeline Step 4: Check if direct HLS / Media
            val isHls = contentType.contains("mpegurl") || finalUrl.contains(".m3u8") || HlsParser.isHlsManifest(peekSample)
            val extractor: PlatformExtractor = if (isHls) {
                DirectMediaExtractor()
            } else {
                PlatformDetector.findExtractor(finalUrl)
            }

            val mediaInfo = extractor.analyze(finalUrl, customHeaders)

            if (mediaInfo != null) {
                if (mediaInfo.isDrmProtected) {
                    return@withContext AnalysisResult.DrmProtected()
                }
                AnalysisResult.Success(mediaInfo)
            } else {
                // Fallback to generic web extractor
                val generic = GenericWebExtractor()
                val fallbackMedia = generic.analyze(finalUrl, customHeaders)
                if (fallbackMedia != null && fallbackMedia.formats.isNotEmpty()) {
                    AnalysisResult.Success(fallbackMedia)
                } else {
                    AnalysisResult.Error(
                        errorType = ErrorType.MEDIA_NOT_FOUND,
                        message = "Could not find a downloadable media stream on this page."
                    )
                }
            }
        } catch (e: Exception) {
            AnalysisResult.Error(
                errorType = ErrorType.UNKNOWN_ERROR,
                message = "An error occurred during media analysis: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
