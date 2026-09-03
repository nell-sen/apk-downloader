package com.example.features.home

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.SearchHistoryEntity
import com.example.domain.model.AnalysisResult
import com.example.domain.model.ErrorType
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import com.example.downloader.DownloadManager
import com.example.extractor.UrlAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Idle : HomeUiState()
    data class Analyzing(val step: String, val progress: Float) : HomeUiState()
    data class AnalysisSuccess(val mediaInfo: MediaInfo) : HomeUiState()
    data class AnalysisError(val errorType: ErrorType, val message: String) : HomeUiState()
    data class DrmProtected(val message: String) : HomeUiState()
}

class HomeViewModel(private val downloadManager: DownloadManager) : ViewModel() {

    private val urlAnalyzer = UrlAnalyzer()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun checkClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() &&
                clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
            ) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString()?.trim()
                if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
                    if (text != _urlInput.value) {
                        _clipboardDetectedUrl.value = text
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun useDetectedClipboardUrl() {
        _clipboardDetectedUrl.value?.let {
            _urlInput.value = it
            _clipboardDetectedUrl.value = null
            analyzeUrl(it)
        }
    }

    fun dismissClipboardUrl() {
        _clipboardDetectedUrl.value = null
    }

    fun analyzeUrl(urlToAnalyze: String? = null) {
        val targetUrl = (urlToAnalyze ?: _urlInput.value).trim()
        if (targetUrl.isBlank()) {
            _uiState.value = HomeUiState.AnalysisError(
                errorType = ErrorType.INVALID_URL,
                message = "Please enter a valid media or HLS URL."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = HomeUiState.Analyzing("Resolving URL and headers...", 0.25f)

            // Save search history
            downloadManager.downloadDao.insertHistory(
                SearchHistoryEntity(url = targetUrl, title = null)
            )

            _uiState.value = HomeUiState.Analyzing("Detecting stream and platform...", 0.55f)
            val result = urlAnalyzer.analyzeUrl(targetUrl)

            _uiState.value = HomeUiState.Analyzing("Parsing available formats and qualities...", 0.85f)

            when (result) {
                is AnalysisResult.Success -> {
                    _uiState.value = HomeUiState.AnalysisSuccess(result.mediaInfo)
                }
                is AnalysisResult.DrmProtected -> {
                    _uiState.value = HomeUiState.DrmProtected(result.message)
                }
                is AnalysisResult.Error -> {
                    _uiState.value = HomeUiState.AnalysisError(result.errorType, result.message)
                }
            }
        }
    }

    fun startDownload(
        mediaInfo: MediaInfo,
        selectedFormat: MediaFormat,
        customTitle: String? = null
    ): String {
        val downloadId = downloadManager.startDownload(mediaInfo, selectedFormat, customTitle)
        _uiState.value = HomeUiState.Idle
        _urlInput.value = ""
        return downloadId
    }

    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }
}
