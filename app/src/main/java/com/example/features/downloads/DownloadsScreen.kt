package com.example.features.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.storage.StorageManager
import com.example.data.database.DownloadEntity
import com.example.domain.model.DownloadStatus
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalGlassColors

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onPlayMedia: (String) -> Unit
) {
    val glassColors = LocalGlassColors.current
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val storageManager = StorageManager(context)

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                
        ) {
            GlassTopBar(
                title = "Downloads",
                subtitle = "${downloads.size} items"
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassChip(
                    text = "All",
                    isSelected = currentFilter == DownloadFilter.ALL,
                    onClick = { viewModel.setFilter(DownloadFilter.ALL) },
                    testTag = "tab_filter_all"
                )
                GlassChip(
                    text = "Active",
                    isSelected = currentFilter == DownloadFilter.ACTIVE,
                    onClick = { viewModel.setFilter(DownloadFilter.ACTIVE) },
                    testTag = "tab_filter_active"
                )
                GlassChip(
                    text = "Completed",
                    isSelected = currentFilter == DownloadFilter.COMPLETED,
                    onClick = { viewModel.setFilter(DownloadFilter.COMPLETED) },
                    testTag = "tab_filter_completed"
                )
                GlassChip(
                    text = "Failed",
                    isSelected = currentFilter == DownloadFilter.FAILED,
                    onClick = { viewModel.setFilter(DownloadFilter.FAILED) },
                    testTag = "tab_filter_failed"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (downloads.isEmpty()) {
                EmptyDownloadsView(currentFilter)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onPause = { viewModel.pauseDownload(item.id) },
                            onResume = { viewModel.resumeDownload(item.id) },
                            onCancel = { viewModel.cancelDownload(item.id) },
                            onRetry = { viewModel.retryDownload(item.id) },
                            onDelete = { viewModel.deleteDownload(item.id) },
                            onOpen = {
                                if (item.status == DownloadStatus.COMPLETED) {
                                    openMediaFile(context, item.filePath, item.mimeType)
                                }
                            },
                            onShare = {
                                val shareUri = storageManager.getShareableUri(item.filePath)
                                if (shareUri != null) {
                                    shareMediaFile(context, shareUri, item.mimeType, item.title)
                                }
                            },
                            onPlay = {
                                onPlayMedia(item.filePath)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val isAudio = item.mimeType.startsWith("audio/")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (item.status == DownloadStatus.COMPLETED) onPlay else null
    ) {
        Column {
            // Header Row: Thumbnail/Icon + Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(glassColors.surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = glassColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = glassColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quality / Format Tag
                        Box(
                            modifier = Modifier
                                .background(glassColors.surface.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${item.quality} • ${item.format}",
                                color = glassColors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Status Badge
                        StatusBadge(item.status)
                    }
                }
            }

            // Progress & Metrics Section (for active/paused downloads)
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.QUEUED) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.status == DownloadStatus.PAUSED) AccentAmber else AccentCyan,
                    trackColor = glassColors.glassCardBorder,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val percent = (item.progress * 100).toInt()
                    val downloadedStr = formatBytes(item.downloadedBytes)
                    val totalStr = if (item.totalBytes > 0) formatBytes(item.totalBytes) else "?"

                    Text(
                        text = "$percent% ($downloadedStr / $totalStr)",
                        color = glassColors.textSecondary,
                        fontSize = 12.sp
                    )

                    if (item.status == DownloadStatus.DOWNLOADING) {
                        val speedStr = formatSpeed(item.speed)
                        val etaStr = formatEta(item.etaSeconds)
                        Text(
                            text = if (item.isHls && item.segmentCount > 0) {
                                "Seg ${item.completedSegments}/${item.segmentCount} • $speedStr"
                            } else {
                                "$speedStr • ETA $etaStr"
                            },
                            color = glassColors.accentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error Message for Failed
            if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.errorMessage,
                    color = AccentRed,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        ActionIconButton(
                            icon = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = AccentAmber,
                            onClick = onPause
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActionIconButton(
                            icon = Icons.Default.Stop,
                            contentDescription = "Cancel",
                            tint = AccentRed,
                            onClick = onCancel
                        )
                    }
                    DownloadStatus.PAUSED -> {
                        ActionIconButton(
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = AccentCyan,
                            onClick = onResume
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = glassColors.textSecondary,
                            onClick = onDelete
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        ActionIconButton(
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "Play Media",
                            tint = AccentCyan,
                            onClick = onPlay
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActionIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = AccentBlue,
                            onClick = onShare
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = glassColors.textSecondary,
                            onClick = onDelete
                        )
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        ActionIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = AccentCyan,
                            onClick = onRetry
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = glassColors.textSecondary,
                            onClick = onDelete
                        )
                    }
                    DownloadStatus.QUEUED, DownloadStatus.ANALYZING, DownloadStatus.PROCESSING -> {
                        ActionIconButton(
                            icon = Icons.Default.Stop,
                            contentDescription = "Cancel",
                            tint = AccentRed,
                            onClick = onCancel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.DOWNLOADING -> "DOWNLOADING" to AccentCyan
        DownloadStatus.COMPLETED -> "COMPLETED" to AccentGreen
        DownloadStatus.PAUSED -> "PAUSED" to AccentAmber
        DownloadStatus.FAILED -> "FAILED" to AccentRed
        DownloadStatus.CANCELLED -> "CANCELLED" to Color.Gray
        DownloadStatus.QUEUED -> "QUEUED" to AccentBlue
        DownloadStatus.ANALYZING -> "ANALYZING" to AccentCyan
        DownloadStatus.PROCESSING -> "PROCESSING" to AccentIndigo
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(glassColors.glassCard)
            .border(1.dp, glassColors.glassCardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EmptyDownloadsView(filter: DownloadFilter) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Downloads Found",
                    color = glassColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (filter) {
                        DownloadFilter.ACTIVE -> "There are no active downloads in progress."
                        DownloadFilter.COMPLETED -> "No completed media files yet."
                        DownloadFilter.FAILED -> "No failed downloads."
                        DownloadFilter.ALL -> "Paste a video, audio, or HLS link to start downloading."
                    },
                    color = glassColors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        bytes > 0 -> "$bytes B"
        else -> "0 B"
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
        bytesPerSec > 0 -> "$bytesPerSec B/s"
        else -> "0 KB/s"
    }
}

private fun formatEta(seconds: Long): String {
    if (seconds <= 0) return "--:--"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun openMediaFile(context: Context, filePath: String, mimeType: String) {
    try {
        val uri = if (filePath.startsWith("content://")) {
            Uri.parse(filePath)
        } else {
            val file = java.io.File(filePath)
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun shareMediaFile(context: Context, uri: Uri, mimeType: String, title: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        // ignore
    }
}
