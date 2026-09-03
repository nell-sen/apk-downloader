package com.example.downloader

import com.example.core.network.NetworkModule
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request

class DirectDownloader {

    suspend fun download(
        url: String,
        targetFile: File,
        tempFile: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val speedCalculator = SpeedCalculator()
        var existingBytes = 0L
        if (tempFile.exists()) {
            existingBytes = tempFile.length()
        }

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val response = try {
            NetworkModule.okHttpClient.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            return@withContext DownloadResult.Error("Network error: ${e.localizedMessage}")
        }

        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        if (contentType.contains("text/html")) {
            response.close()
            return@withContext DownloadResult.Error("Access Denied or Not a Media File (HTML response)")
        }

        if (!response.isSuccessful && response.code != 206) {
            // If Range was not accepted (e.g. 416), retry from beginning
            if (response.code == 416 || existingBytes > 0) {
                response.close()
                tempFile.delete()
                existingBytes = 0L
                val freshReq = Request.Builder().url(url)
                headers.forEach { (k, v) -> freshReq.header(k, v) }
                val freshResp = try {
                    NetworkModule.okHttpClient.newCall(freshReq.build()).execute()
                } catch (e: Exception) {
                    return@withContext DownloadResult.Error("Network error on retry")
                }
                
                val freshType = freshResp.header("Content-Type")?.lowercase() ?: ""
                if (freshType.contains("text/html")) {
                    freshResp.close()
                    return@withContext DownloadResult.Error("Access Denied or Not a Media File (HTML response)")
                }
                
                if (!freshResp.isSuccessful) {
                    val code = freshResp.code
                    freshResp.close()
                    return@withContext DownloadResult.Error("HTTP Error: $code")
                }
                return@withContext processResponseBody(freshResp.body, tempFile, targetFile, 0L, freshResp.header("Content-Length")?.toLongOrNull() ?: 0L, speedCalculator, onProgress)
            }
            val code = response.code
            response.close()
            return@withContext DownloadResult.Error("HTTP Error: $code")
        }

        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
        val totalBytes = if (response.code == 206) {
            existingBytes + contentLength
        } else {
            existingBytes = 0L // Server ignored Range and returned full body
            tempFile.delete()
            contentLength
        }

        processResponseBody(response.body, tempFile, targetFile, existingBytes, totalBytes, speedCalculator, onProgress)
    }

    private suspend fun processResponseBody(
        body: okhttp3.ResponseBody?,
        tempFile: File,
        targetFile: File,
        initialBytes: Long,
        totalBytes: Long,
        speedCalculator: SpeedCalculator,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (body == null) return@withContext DownloadResult.Error("Empty response body")

        var downloadedBytes = initialBytes
        var lastUpdateMs = System.currentTimeMillis()
        speedCalculator.addSample(lastUpdateMs, downloadedBytes)

        val outputStream = if (initialBytes > 0) {
            FileOutputStream(tempFile, true)
        } else {
            FileOutputStream(tempFile, false)
        }

        val buffer = ByteArray(64 * 1024) // 64KB buffer for better performance
        val inputStream = body.byteStream().buffered()

        try {
            outputStream.use { out ->
                inputStream.use { inStream ->
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            return@withContext DownloadResult.Cancelled
                        }
                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMs >= 200) {
                            speedCalculator.addSample(now, downloadedBytes)
                            val speed = speedCalculator.calculateSpeed()
                            val eta = if (totalBytes > 0) speedCalculator.calculateEta(totalBytes - downloadedBytes, speed) else 0L
                            val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                            onProgress(progress, downloadedBytes, totalBytes, speed, eta)
                            lastUpdateMs = now
                        }
                    }
                    out.flush()
                }
            }

            // Move temp file to target
            if (tempFile.exists() && tempFile.length() > 0) {
                targetFile.delete()
                tempFile.renameTo(targetFile)
                onProgress(1.0f, downloadedBytes, downloadedBytes, 0L, 0L)
                return@withContext DownloadResult.Success
            }
            DownloadResult.Error("Downloaded file is empty or missing")
        } catch (e: Exception) {
            DownloadResult.Error("I/O error during download: ${e.localizedMessage}")
        }
    }
}

