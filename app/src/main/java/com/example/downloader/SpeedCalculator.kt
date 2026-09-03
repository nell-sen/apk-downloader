package com.example.downloader

import java.util.ArrayDeque

class SpeedCalculator(private val windowSize: Int = 5) {
    private val samples = ArrayDeque<Pair<Long, Long>>() // timestampMs, downloadedBytes

    fun reset() {
        samples.clear()
    }

    fun addSample(timestampMs: Long, downloadedBytes: Long) {
        samples.addLast(Pair(timestampMs, downloadedBytes))
        if (samples.size > windowSize) {
            samples.removeFirst()
        }
    }

    fun calculateSpeed(): Long {
        if (samples.size < 2) return 0L
        val first = samples.first
        val last = samples.last
        val timeDeltaSec = (last.first - first.first) / 1000.0
        val bytesDelta = last.second - first.second
        if (timeDeltaSec <= 0.05 || bytesDelta <= 0) return 0L
        return (bytesDelta / timeDeltaSec).toLong()
    }

    fun calculateEta(remainingBytes: Long, currentSpeed: Long): Long {
        if (currentSpeed <= 0 || remainingBytes <= 0) return 0L
        return remainingBytes / currentSpeed
    }
}
