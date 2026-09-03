package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val thumbnail: String?,
    val platform: String,
    val status: DownloadStatus,
    val progress: Float, // 0.0f to 1.0f
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long, // bytes per second
    val etaSeconds: Long,
    val quality: String,
    val format: String,
    val isHls: Boolean,
    val playlistUrl: String?,
    val variantUrl: String?,
    val segmentCount: Int,
    val completedSegments: Int,
    val isLive: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String?
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String?,
    val timestamp: Long = System.currentTimeMillis()
)
