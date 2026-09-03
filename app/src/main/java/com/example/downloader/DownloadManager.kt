package com.example.downloader

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.MainActivity
import com.example.core.storage.StorageManager
import com.example.data.database.AppDatabase
import com.example.data.database.DownloadDao
import com.example.data.database.DownloadEntity
import com.example.domain.model.DownloadStatus
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DownloadManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "glassdrop_db"
    ).build()

    val downloadDao: DownloadDao = database.downloadDao()
    val storageManager = StorageManager(context)

    private val directDownloader = DirectDownloader()
    private val hlsDownloader = HlsDownloader()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    var maxConcurrentDownloads: Int = 2
    var hlsConcurrency: Int = 4

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        // Crash / process death recovery: reset interrupted DOWNLOADING or ANALYZING tasks to PAUSED
        scope.launch {
            downloadDao.resetInterruptedDownloads()
        }
    }

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadFlow(id: String): Flow<DownloadEntity?> = downloadDao.getDownloadFlow(id)

    fun startDownload(
        mediaInfo: MediaInfo,
        selectedFormat: MediaFormat,
        customTitle: String? = null
    ): String {
        val title = (customTitle ?: mediaInfo.title).ifBlank { "Media Download" }
        val isAudio = !selectedFormat.hasVideo && selectedFormat.hasAudio
        val extension = selectedFormat.extension.ifBlank { if (isAudio) "mp3" else "mp4" }
        val targetFile = storageManager.generateUniqueFile(title, extension, isAudio)
        val downloadId = UUID.randomUUID().toString()

        val entity = DownloadEntity(
            id = downloadId,
            url = selectedFormat.url,
            title = title,
            fileName = targetFile.name,
            mimeType = selectedFormat.mimeType,
            filePath = targetFile.absolutePath,
            thumbnail = mediaInfo.thumbnail,
            platform = mediaInfo.platform,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = selectedFormat.estimatedBytes,
            speed = 0L,
            etaSeconds = 0L,
            quality = selectedFormat.qualityLabel,
            format = extension.uppercase(),
            isHls = mediaInfo.isHls || selectedFormat.isHlsVariant,
            playlistUrl = if (mediaInfo.isHls) mediaInfo.sourceUrl else null,
            variantUrl = if (selectedFormat.isHlsVariant) selectedFormat.url else null,
            segmentCount = 0,
            completedSegments = 0,
            isLive = mediaInfo.isLive,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            errorMessage = null
        )

        scope.launch {
            downloadDao.insertDownload(entity)
            processNextInQueue()
        }

        return downloadId
    }

    fun resumeDownload(downloadId: String) {
        scope.launch {
            val entity = downloadDao.getDownloadById(downloadId) ?: return@launch
            if (entity.status == DownloadStatus.PAUSED || entity.status == DownloadStatus.FAILED) {
                downloadDao.updateStatus(downloadId, DownloadStatus.QUEUED)
                processNextInQueue()
            }
        }
    }

    fun pauseDownload(downloadId: String) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
            updateNotificationState()
            processNextInQueue()
        }
    }

    fun cancelDownload(downloadId: String) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val entity = downloadDao.getDownloadById(downloadId)
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
            storageManager.cleanupTempDirForTask(downloadId)
            entity?.let {
                val file = File(it.filePath)
                if (file.exists()) file.delete()
            }
            updateNotificationState()
            processNextInQueue()
        }
    }

    fun retryDownload(downloadId: String) {
        resumeDownload(downloadId)
    }

    fun deleteDownload(downloadId: String, deleteFile: Boolean = true) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val entity = downloadDao.getDownloadById(downloadId)
            downloadDao.deleteDownload(downloadId)
            storageManager.cleanupTempDirForTask(downloadId)
            if (deleteFile && entity != null) {
                val file = File(entity.filePath)
                if (file.exists()) file.delete()
            }
            updateNotificationState()
        }
    }

    private fun processNextInQueue() {
        if (activeJobs.size >= maxConcurrentDownloads) return

        scope.launch {
            val entityList = downloadDao.getDownloadById("") // dummy query trigger
            // Get queued items
            val all = downloadDao.getAllDownloads()
            // We find the first QUEUED download
            val queued = database.openHelper.readableDatabase.let {
                // query directly
            }
        }
        scope.launch {
            val allDownloads = database.openHelper.readableDatabase
            // Let's use downloadDao
            val queuedItem = downloadDao.getDownloadById("non-existent") // fallback
        }
        startNextQueuedTask()
    }

    private fun startNextQueuedTask() {
        scope.launch {
            val downloads = database.runInTransaction<List<DownloadEntity>> {
                // get queued downloads
                emptyList()
            }
            // Execute queued query
            val list = downloadDao.getAllDownloads()
        }
        scope.launch {
            val entities = downloadDao.getDownloadsByStatus(DownloadStatus.QUEUED)
            // collect first item
            // or query
            executeQueuedDownloads()
        }
    }

    private suspend fun executeQueuedDownloads() {
        if (activeJobs.size >= maxConcurrentDownloads) return

        val queuedEntities = database.openHelper.let {
            // Find queued entity
            val cursor = database.query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1", null)
            val idList = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val idIdx = cursor.getColumnIndex("id")
                if (idIdx >= 0) idList.add(cursor.getString(idIdx))
            }
            cursor.close()
            idList
        }

        for (id in queuedEntities) {
            if (activeJobs.size >= maxConcurrentDownloads) break
            if (!activeJobs.containsKey(id)) {
                startExecution(id)
            }
        }
    }

    private fun startExecution(downloadId: String) {
        val job = scope.launch {
            val entity = downloadDao.getDownloadById(downloadId) ?: return@launch
            val isAudio = entity.mimeType.startsWith("audio/")
            val targetFile = File(entity.filePath)
            val tempDir = storageManager.getTempDirForTask(downloadId)

            // Storage space verification
            if (storageManager.getAvailableStorageBytes() < 50L * 1024L * 1024L) {
                downloadDao.markFailed(downloadId, error = "Insufficient storage space.")
                showErrorNotification(entity.title, "Insufficient storage space.")
                processNextInQueue()
                return@launch
            }

            downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
            DownloadService.start(context)
            updateNotificationState()

            val success: Boolean = if (entity.isHls) {
                val urlToDownload = entity.variantUrl ?: entity.url
                hlsDownloader.downloadHls(
                    variantOrMediaPlaylistUrl = urlToDownload,
                    targetFile = targetFile,
                    tempDir = tempDir,
                    concurrency = hlsConcurrency,
                    onProgress = { progress, completedSegments, totalSegments, downloadedBytes, speed, etaSeconds ->
                        scope.launch {
                            downloadDao.updateHlsProgress(
                                id = downloadId,
                                completed = completedSegments,
                                progress = progress,
                                speed = speed,
                                etaSeconds = etaSeconds,
                                downloadedBytes = downloadedBytes
                            )
                            showProgressNotification(entity.title, progress, speed, etaSeconds)
                        }
                    }
                )
            } else {
                val tempFile = File(tempDir, "direct_download.tmp")
                directDownloader.download(
                    url = entity.url,
                    targetFile = targetFile,
                    tempFile = tempFile,
                    onProgress = { progress, downloadedBytes, totalBytes, speed, etaSeconds ->
                        scope.launch {
                            downloadDao.updateProgress(
                                id = downloadId,
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speed = speed,
                                etaSeconds = etaSeconds
                            )
                            showProgressNotification(entity.title, progress, speed, etaSeconds)
                        }
                    }
                )
            }

            activeJobs.remove(downloadId)

            if (success && targetFile.exists() && targetFile.length() > 0) {
                // Save to MediaStore
                val mediaStoreUri = storageManager.saveToMediaStore(
                    file = targetFile,
                    title = entity.title,
                    mimeType = entity.mimeType,
                    isAudio = isAudio
                )
                downloadDao.markCompleted(
                    id = downloadId,
                    filePath = mediaStoreUri
                )
                storageManager.cleanupTempDirForTask(downloadId)
                showCompletedNotification(entity.title)
            } else {
                val freshEntity = downloadDao.getDownloadById(downloadId)
                if (freshEntity?.status != DownloadStatus.PAUSED && freshEntity?.status != DownloadStatus.CANCELLED) {
                    downloadDao.markFailed(downloadId, error = "Download failed or connection interrupted.")
                    showErrorNotification(entity.title, "Download failed.")
                }
            }

            updateNotificationState()
            processNextInQueue()
        }

        activeJobs[downloadId] = job
    }

    private fun showProgressNotification(title: String, progress: Float, speed: Long, etaSeconds: Long) {
        val percent = (progress * 100).toInt()
        val speedStr = formatSpeed(speed)
        val etaStr = formatEta(etaSeconds)

        val pauseIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$percent% • $speedStr • ETA $etaStr")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .build()

        notificationManager.notify(DownloadService.NOTIFICATION_ID, notif)
    }

    private fun showCompletedNotification(title: String) {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun showErrorNotification(title: String, error: String) {
        val notif = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun updateNotificationState() {
        if (activeJobs.isEmpty()) {
            notificationManager.cancel(DownloadService.NOTIFICATION_ID)
            DownloadService.stop(context)
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
            bytesPerSec > 0 -> "$bytesPerSec B/s"
            else -> "0 KB/s"
        }
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "Calculating..."
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
