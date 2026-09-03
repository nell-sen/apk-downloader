package com.example.core.network

import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object UrlSecurity {
    private val BLOCKED_SCHEMES = setOf("file", "javascript", "data", "content", "intent")
    private val DEFAULT_ALLOWED_SCHEMES = setOf("http", "https")

    fun isSafeUrl(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        val trimmed = rawUrl.trim()

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return false
        }

        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme in BLOCKED_SCHEMES) return false
        if (scheme !in DEFAULT_ALLOWED_SCHEMES) return false

        val host = uri.host?.lowercase() ?: return false

        // Prevent SSRF addresses
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") {
            return false
        }

        // Check private IP ranges
        if (isPrivateIp(host)) {
            return false
        }

        return true
    }

    fun sanitizeFileName(name: String): String {
        val invalidChars = Regex("""[\\/:*?"<>|]""")
        var cleaned = name.replace(invalidChars, "-").trim()
        if (cleaned.length > 120) {
            cleaned = cleaned.substring(0, 120)
        }
        if (cleaned.isBlank()) {
            cleaned = "media_download_${System.currentTimeMillis()}"
        }
        return cleaned
    }

    private fun isPrivateIp(host: String): Boolean {
        return try {
            val parts = host.split(".")
            if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
                val p0 = parts[0].toInt()
                val p1 = parts[1].toInt()
                if (p0 == 10) return true
                if (p0 == 127) return true
                if (p0 == 172 && p1 in 16..31) return true
                if (p0 == 192 && p1 == 168) return true
                if (p0 == 169 && p1 == 254) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}

object NetworkModule {
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                if (original.header("User-Agent") == null) {
                    requestBuilder.header("User-Agent", DEFAULT_USER_AGENT)
                }
                requestBuilder.header("Accept", "*/*")
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
}
