package com.example.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.core.network.UrlSecurity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class StorageManager(private val context: Context) {

    private val tempRootDir: File by lazy {
        File(context.cacheDir, "glassdrop_temp").apply { mkdirs() }
    }

    private val internalDownloadsDir: File by lazy {
        File(context.filesDir, "glassdrop_downloads").apply { mkdirs() }
    }

    fun getTempDirForTask(taskId: String): File {
        return File(tempRootDir, taskId).apply { mkdirs() }
    }

    fun cleanupTempDirForTask(taskId: String) {
        try {
            val dir = File(tempRootDir, taskId)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clearAllTempFiles() {
        try {
            tempRootDir.deleteRecursively()
            tempRootDir.mkdirs()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            500L * 1024L * 1024L // fallback 500MB
        }
    }

    fun generateUniqueFile(title: String, extension: String, isAudio: Boolean): File {
        val sanitized = UrlSecurity.sanitizeFileName(title)
        val ext = extension.removePrefix(".")
        var target = File(internalDownloadsDir, "$sanitized.$ext")
        var counter = 1
        while (target.exists()) {
            target = File(internalDownloadsDir, "$sanitized ($counter).$ext")
            counter++
        }
        return target
    }

    fun saveToMediaStore(file: File, title: String, mimeType: String, isAudio: Boolean): String {
        if (!file.exists() || file.length() == 0L) {
            return file.absolutePath
        }

        try {
            val relativeDir = if (isAudio) "Music/GlassDrop" else "Movies/GlassDrop"
            val contentUri = if (isAudio) {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, file.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri: Uri? = resolver.insert(contentUri, values)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                return uri.toString()
            }
        } catch (e: Exception) {
            // fallback to internal file path
        }
        return file.absolutePath
    }

    fun getShareableUri(filePath: String): Uri? {
        return try {
            if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
