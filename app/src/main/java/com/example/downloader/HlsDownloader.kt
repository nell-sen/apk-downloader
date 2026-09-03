package com.example.downloader

import com.example.core.network.NetworkModule
import com.example.core.parser.HlsEncryptionKey
import com.example.core.parser.HlsParser
import com.example.core.parser.HlsSegment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request

class HlsDownloader {

    suspend fun downloadHls(
        variantOrMediaPlaylistUrl: String,
        targetFile: File,
        tempDir: File,
        headers: Map<String, String> = emptyMap(),
        concurrency: Int = 4,
        onProgress: (progress: Float, completedSegments: Int, totalSegments: Int, downloadedBytes: Long, speed: Long, etaSeconds: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Fetch Playlist
        val req = Request.Builder().url(variantOrMediaPlaylistUrl)
        headers.forEach { (k, v) -> req.header(k, v) }
        val resp = try {
            NetworkModule.okHttpClient.newCall(req.build()).execute()
        } catch (e: Exception) {
            return@withContext false
        }

        val manifestContent = resp.body?.string() ?: return@withContext false
        val finalUrl = resp.request.url.toString()

        var mediaPlaylistUrl = finalUrl
        var manifestToUse = manifestContent

        // If master playlist, pick best variant
        if (HlsParser.isMasterPlaylist(manifestContent)) {
            val master = HlsParser.parseMasterPlaylist(manifestContent, finalUrl)
            if (master.variants.isEmpty()) return@withContext false
            val chosenVariant = master.variants.first()
            mediaPlaylistUrl = chosenVariant.url

            val vReq = Request.Builder().url(mediaPlaylistUrl)
            headers.forEach { (k, v) -> vReq.header(k, v) }
            val vResp = try {
                NetworkModule.okHttpClient.newCall(vReq.build()).execute()
            } catch (e: Exception) {
                return@withContext false
            }
            manifestToUse = vResp.body?.string() ?: return@withContext false
        }

        val mediaPlaylist = HlsParser.parseMediaPlaylist(manifestToUse, mediaPlaylistUrl)
        val segments = mediaPlaylist.segments
        if (segments.isEmpty()) return@withContext false

        val segmentsDir = File(tempDir, "segments").apply { mkdirs() }
        val totalSegments = segments.size
        val completedCount = AtomicInteger(0)
        val totalDownloadedBytes = AtomicLong(0L)
        val keyCache = ConcurrentHashMap<String, ByteArray>()
        val speedCalculator = SpeedCalculator()

        // Check already downloaded segments for resume
        segments.forEach { seg ->
            val segFile = File(segmentsDir, "seg_${String.format("%05d", seg.index)}.ts")
            if (segFile.exists() && segFile.length() > 0) {
                completedCount.incrementAndGet()
                totalDownloadedBytes.addAndGet(segFile.length())
            }
        }

        val semaphore = Semaphore(concurrency.coerceIn(1, 8))
        val startTime = System.currentTimeMillis()
        var lastUpdateMs = startTime

        val downloadSuccess = coroutineScope {
            val tasks = segments.map { segment ->
                async(Dispatchers.IO) {
                    val segFile = File(segmentsDir, "seg_${String.format("%05d", segment.index)}.ts")
                    if (segFile.exists() && segFile.length() > 0) {
                        return@async true
                    }

                    semaphore.withPermit {
                        if (!currentCoroutineContext().isActive) return@withPermit false

                        val success = downloadSegmentWithRetry(
                            segment = segment,
                            outputFile = segFile,
                            headers = headers,
                            keyCache = keyCache,
                            maxRetries = 3
                        )

                        if (success) {
                            val done = completedCount.incrementAndGet()
                            val bytes = totalDownloadedBytes.addAndGet(segFile.length())
                            val now = System.currentTimeMillis()

                            if (now - lastUpdateMs >= 250 || done == totalSegments) {
                                speedCalculator.addSample(now, bytes)
                                val speed = speedCalculator.calculateSpeed()
                                val avgRate = if (now - startTime > 500) done / ((now - startTime) / 1000.0) else 0.0
                                val eta = if (avgRate > 0) ((totalSegments - done) / avgRate).toLong() else 0L
                                val progress = done.toFloat() / totalSegments
                                onProgress(progress, done, totalSegments, bytes, speed, eta)
                                lastUpdateMs = now
                            }
                        }
                        success
                    }
                }
            }

            val results = tasks.awaitAll()
            results.all { it }
        }

        if (!downloadSuccess) return@withContext false

        // Merge all segments in order into single file
        val merged = mergeSegments(segmentsDir, segments, targetFile)
        if (merged) {
            onProgress(1.0f, totalSegments, totalSegments, targetFile.length(), 0L, 0L)
            // Cleanup segment files
            segmentsDir.deleteRecursively()
            return@withContext true
        }
        false
    }

    private suspend fun downloadSegmentWithRetry(
        segment: HlsSegment,
        outputFile: File,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>,
        maxRetries: Int
    ): Boolean {
        var attempt = 0
        var backoffMs = 500L

        while (attempt < maxRetries) {
            if (!currentCoroutineContext().isActive) return false
            attempt++

            try {
                val reqBuilder = Request.Builder().url(segment.url)
                headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                val resp = NetworkModule.okHttpClient.newCall(reqBuilder.build()).execute()

                if (resp.isSuccessful) {
                    val rawBytes = resp.body?.bytes() ?: return false
                    val processedBytes = if (segment.encryptionKey != null && segment.encryptionKey.method == "AES-128") {
                        decryptSegment(rawBytes, segment, headers, keyCache) ?: rawBytes
                    } else {
                        rawBytes
                    }

                    val tempSeg = File(outputFile.parentFile, "${outputFile.name}.tmp")
                    FileOutputStream(tempSeg).use { it.write(processedBytes) }
                    tempSeg.renameTo(outputFile)
                    return true
                }
            } catch (e: Exception) {
                if (attempt >= maxRetries) return false
                delay(backoffMs)
                backoffMs *= 2
            }
        }
        return false
    }

    private fun decryptSegment(
        encryptedBytes: ByteArray,
        segment: HlsSegment,
        headers: Map<String, String>,
        keyCache: ConcurrentHashMap<String, ByteArray>
    ): ByteArray? {
        val enc = segment.encryptionKey ?: return encryptedBytes
        val keyUri = enc.uri ?: return encryptedBytes

        val keyBytes = keyCache.getOrPut(keyUri) {
            try {
                val req = Request.Builder().url(keyUri)
                headers.forEach { (k, v) -> req.header(k, v) }
                NetworkModule.okHttpClient.newCall(req.build()).execute().body?.bytes() ?: return null
            } catch (e: Exception) {
                return null
            }
        }

        val ivBytes = if (!enc.iv.isNullOrBlank()) {
            val hex = enc.iv.removePrefix("0x").removePrefix("0X")
            hexStringToByteArray(hex)
        } else {
            // Default IV is sequence number as 16-byte big endian
            val buf = ByteBuffer.allocate(16)
            buf.putLong(8, segment.index.toLong())
            buf.array()
        }

        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val padded = if (len < 32) s.padStart(32, '0') else s
        val data = ByteArray(16)
        for (i in 0 until 32 step 2) {
            data[i / 2] = ((Character.digit(padded[i], 16) shl 4) + Character.digit(padded[i + 1], 16)).toByte()
        }
        return data
    }

    private fun mergeSegments(segmentsDir: File, segments: List<HlsSegment>, outputFile: File): Boolean {
        return try {
            val tempMerged = File(outputFile.parentFile, "${outputFile.name}.merging")
            FileOutputStream(tempMerged).use { outStream ->
                val buffer = ByteArray(64 * 1024)
                for (seg in segments) {
                    val segFile = File(segmentsDir, "seg_${String.format("%05d", seg.index)}.ts")
                    if (!segFile.exists()) return false
                    FileInputStream(segFile).use { inStream ->
                        var read: Int
                        while (inStream.read(buffer).also { read = it } != -1) {
                            outStream.write(buffer, 0, read)
                        }
                    }
                }
                outStream.flush()
            }
            outputFile.delete()
            tempMerged.renameTo(outputFile)
            true
        } catch (e: Exception) {
            false
        }
    }
}
