package com.example.downloader

sealed class DownloadResult {
    object Success : DownloadResult()
    object Cancelled : DownloadResult()
    data class Error(val message: String, val cause: Throwable? = null) : DownloadResult()
}
