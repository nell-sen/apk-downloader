package com.example.features.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DownloadEntity
import com.example.domain.model.DownloadStatus
import com.example.downloader.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    FAILED
}

class DownloadsViewModel(private val downloadManager: DownloadManager) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> = combine(
        downloadManager.getAllDownloads(),
        _selectedFilter
    ) { allDownloads, filter ->
        when (filter) {
            DownloadFilter.ALL -> allDownloads
            DownloadFilter.ACTIVE -> allDownloads.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.PAUSED ||
                it.status == DownloadStatus.ANALYZING
            }
            DownloadFilter.COMPLETED -> allDownloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> allDownloads.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
    }

    fun pauseDownload(id: String) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
    }

    fun retryDownload(id: String) {
        downloadManager.retryDownload(id)
    }

    fun deleteDownload(id: String, deleteFile: Boolean = true) {
        downloadManager.deleteDownload(id, deleteFile)
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadManager.downloadDao.clearAll()
            downloadManager.storageManager.clearAllTempFiles()
        }
    }
}
