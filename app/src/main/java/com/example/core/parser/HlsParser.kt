package com.example.core.parser

object HlsParser {

    fun isHlsManifest(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("#EXTM3U") ||
               trimmed.contains("#EXT-X-STREAM-INF") ||
               trimmed.contains("#EXTINF") ||
               trimmed.contains("#EXT-X-TARGETDURATION")
    }

    fun isMasterPlaylist(content: String): Boolean {
        return content.contains("#EXT-X-STREAM-INF") || content.contains("#EXT-X-MEDIA:TYPE=")
    }

    fun isDrmProtected(content: String): Boolean {
        val lower = content.lowercase()
        return lower.contains("keyformat=\"urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed\"") || // Widevine
               lower.contains("keyformat=\"com.widevine") ||
               lower.contains("keyformat=\"com.apple.fairplay") ||
               lower.contains("keyformat=\"urn:uuid:9a04f079-9840-4286-ab92-e65be0885f95\"") || // PlayReady
               lower.contains("method=sample-aes-ctr") ||
               lower.contains("method=sample-aes")
    }

    fun parseMasterPlaylist(manifestContent: String, baseUrl: String): HlsMasterPlaylist {
        val variants = mutableListOf<HlsVariantStream>()
        val audioTracks = mutableListOf<HlsAudioTrack>()
        val subtitleTracks = mutableListOf<HlsSubtitleTrack>()

        val lines = manifestContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var currentStreamInf: Map<String, String>? = null

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    val attrString = line.substringAfter("#EXT-X-STREAM-INF:")
                    currentStreamInf = parseAttributes(attrString)
                }
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrString = line.substringAfter("#EXT-X-MEDIA:")
                    val attrs = parseAttributes(attrString)
                    val type = attrs["TYPE"]?.uppercase()
                    val groupId = attrs["GROUP-ID"] ?: ""
                    val name = attrs["NAME"] ?: "Track"
                    val lang = attrs["LANGUAGE"]
                    val isDefault = attrs["DEFAULT"]?.equals("YES", ignoreCase = true) == true
                    val autoSelect = attrs["AUTOSELECT"]?.equals("YES", ignoreCase = true) == true
                    val rawUri = attrs["URI"]
                    val uri = rawUri?.let { HlsUrlResolver.resolveUrl(baseUrl, it) }

                    if (type == "AUDIO") {
                        audioTracks.add(
                            HlsAudioTrack(
                                groupId = groupId,
                                name = name,
                                language = lang,
                                isDefault = isDefault,
                                autoSelect = autoSelect,
                                uri = uri
                            )
                        )
                    } else if (type == "SUBTITLES") {
                        subtitleTracks.add(
                            HlsSubtitleTrack(
                                groupId = groupId,
                                name = name,
                                language = lang,
                                isDefault = isDefault,
                                uri = uri
                            )
                        )
                    }
                }
                !line.startsWith("#") && currentStreamInf != null -> {
                    val streamUrl = HlsUrlResolver.resolveUrl(baseUrl, line)
                    val bandwidth = currentStreamInf["BANDWIDTH"]?.toLongOrNull() ?: 0L
                    val resString = currentStreamInf["RESOLUTION"]
                    var width = 0
                    var height = 0
                    if (resString != null && resString.contains("x")) {
                        width = resString.substringBefore("x").toIntOrNull() ?: 0
                        height = resString.substringAfter("x").toIntOrNull() ?: 0
                    }
                    val frameRate = currentStreamInf["FRAME-RATE"]?.toFloatOrNull() ?: 0f
                    val codecs = currentStreamInf["CODECS"]
                    val audioGroup = currentStreamInf["AUDIO"]
                    val subGroup = currentStreamInf["SUBTITLES"]

                    variants.add(
                        HlsVariantStream(
                            bandwidth = bandwidth,
                            resolution = resString,
                            width = width,
                            height = height,
                            frameRate = frameRate,
                            codecs = codecs,
                            audioGroupId = audioGroup,
                            subtitleGroupId = subGroup,
                            url = streamUrl
                        )
                    )
                    currentStreamInf = null
                }
            }
        }

        // Sort variants by resolution / bandwidth descending
        val sortedVariants = variants.sortedWith(
            compareByDescending<HlsVariantStream> { it.height }
                .thenByDescending { it.bandwidth }
        )

        return HlsMasterPlaylist(
            variants = sortedVariants,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )
    }

    fun parseMediaPlaylist(manifestContent: String, baseUrl: String): HlsMediaPlaylist {
        val segments = mutableListOf<HlsSegment>()
        var targetDuration = 0
        var mediaSequence = 0L
        var hasEndList = false
        var currentDuration = 0.0
        var currentTitle: String? = null
        var currentKey: HlsEncryptionKey? = null
        var totalDuration = 0.0
        val drmProtected = isDrmProtected(manifestContent)

        val lines = manifestContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var segmentIndex = 0

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.substringAfter("#EXT-X-TARGETDURATION:").toIntOrNull() ?: 0
                }
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    mediaSequence = line.substringAfter("#EXT-X-MEDIA-SEQUENCE:").toLongOrNull() ?: 0L
                }
                line.startsWith("#EXT-X-ENDLIST") -> {
                    hasEndList = true
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val attrs = parseAttributes(line.substringAfter("#EXT-X-KEY:"))
                    val method = attrs["METHOD"] ?: "NONE"
                    val rawUri = attrs["URI"]
                    val uri = rawUri?.let { HlsUrlResolver.resolveUrl(baseUrl, it) }
                    val iv = attrs["IV"]
                    val keyFormat = attrs["KEYFORMAT"]
                    currentKey = HlsEncryptionKey(method = method, uri = uri, iv = iv, keyFormat = keyFormat)
                }
                line.startsWith("#EXTINF:") -> {
                    val infData = line.substringAfter("#EXTINF:")
                    val durStr = infData.substringBefore(",").trim()
                    currentDuration = durStr.toDoubleOrNull() ?: 0.0
                    currentTitle = infData.substringAfter(",", "").trim().takeIf { it.isNotEmpty() }
                }
                !line.startsWith("#") -> {
                    val segmentUrl = HlsUrlResolver.resolveUrl(baseUrl, line)
                    segments.add(
                        HlsSegment(
                            index = segmentIndex++,
                            durationSeconds = currentDuration,
                            url = segmentUrl,
                            encryptionKey = currentKey,
                            title = currentTitle
                        )
                    )
                    totalDuration += currentDuration
                    currentDuration = 0.0
                    currentTitle = null
                }
            }
        }

        return HlsMediaPlaylist(
            targetDurationSeconds = targetDuration,
            mediaSequence = mediaSequence,
            isLive = !hasEndList,
            isDrmProtected = drmProtected,
            segments = segments,
            totalDurationSeconds = totalDuration
        )
    }

    fun parseAttributes(attrString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("""([A-Z0-9\-]+)=("[^"]*"|[^,]*)""")
        val matches = regex.findAll(attrString)
        for (match in matches) {
            val key = match.groupValues[1]
            var value = match.groupValues[2]
            if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                value = value.substring(1, value.length - 1)
            }
            result[key] = value
        }
        return result
    }
}
