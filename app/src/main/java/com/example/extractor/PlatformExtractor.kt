package com.example.extractor

import com.example.domain.model.MediaInfo

interface PlatformExtractor {
    fun canHandle(url: String): Boolean
    suspend fun analyze(url: String, headers: Map<String, String> = emptyMap()): MediaInfo?
}
