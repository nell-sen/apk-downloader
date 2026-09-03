package com.example

import android.app.Application
import com.example.downloader.DownloadManager

class GlassDropApp : Application() {

    lateinit var downloadManager: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        downloadManager = DownloadManager.getInstance(this)
    }
}
