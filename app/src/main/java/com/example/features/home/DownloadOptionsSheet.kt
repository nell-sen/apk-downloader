package com.example.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.MediaFormat
import com.example.domain.model.MediaInfo
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalGlassColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadOptionsSheet(
    mediaInfo: MediaInfo,
    onDismiss: () -> Unit,
    onStartDownload: (MediaFormat, String?) -> Unit,
    onPreview: (MediaFormat) -> Unit
) {
    val glassColors = LocalGlassColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember {
        mutableStateOf(mediaInfo.formats.firstOrNull() ?: MediaFormat("def", "Standard", mediaInfo.sourceUrl, "mp4", "video/mp4"))
    }
    var selectedAudioTrackId by remember {
        mutableStateOf(mediaInfo.audioTracks.firstOrNull { it.isDefault }?.id ?: mediaInfo.audioTracks.firstOrNull()?.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassColors.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .background(glassColors.glassCardBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Info Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!mediaInfo.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaInfo.thumbnail,
                        contentDescription = "Media thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(GlassTokens.CornerRadiusSm))
                            .border(1.dp, glassColors.glassCardBorder, RoundedCornerShape(GlassTokens.CornerRadiusSm))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Platform Tag
                        Box(
                            modifier = Modifier
                                .background(glassColors.glassCard, RoundedCornerShape(6.dp))
                                .border(1.dp, glassColors.glassCardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mediaInfo.platform,
                                color = glassColors.accentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (mediaInfo.isLive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .border(1.dp, AccentRed, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = AccentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (mediaInfo.isHls) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .border(1.dp, AccentGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HLS",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mediaInfo.title,
                        color = glassColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (mediaInfo.durationSeconds > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Duration: ${formatDuration(mediaInfo.durationSeconds)}",
                            color = glassColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quality / Format Selector
            Text(
                text = "Available Qualities & Formats",
                color = glassColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                mediaInfo.formats.forEach { format ->
                    val isSelected = format.id == selectedFormat.id
                    val isAudioOnly = !format.hasVideo && format.hasAudio

                    GlassChip(
                        text = "${format.qualityLabel} • ${format.extension.uppercase()}",
                        isSelected = isSelected,
                        icon = if (isAudioOnly) Icons.Default.Audiotrack else Icons.Default.Videocam,
                        onClick = { selectedFormat = format },
                        testTag = "chip_format_${format.id}"
                    )
                }
            }

            // Audio Tracks Selector (if multiple)
            if (mediaInfo.audioTracks.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Audio Track",
                    color = glassColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaInfo.audioTracks.forEach { track ->
                        val isSelected = track.id == selectedAudioTrackId
                        GlassChip(
                            text = track.name + if (track.isDefault) " (Default)" else "",
                            isSelected = isSelected,
                            icon = Icons.Default.Audiotrack,
                            onClick = { selectedAudioTrackId = track.id }
                        )
                    }
                }
            }

            // Subtitle Tracks Info (if available)
            if (mediaInfo.subtitleTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = glassColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Subtitles: ${mediaInfo.subtitleTracks.joinToString { it.name }}",
                        color = glassColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Estimated Size Info
            if (selectedFormat.estimatedBytes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Estimated File Size: ~${formatBytes(selectedFormat.estimatedBytes)}",
                    color = glassColors.accentCyan,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassSecondaryButton(
                    text = "Preview",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onPreview(selectedFormat) },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_preview_media"
                )

                GlassButton(
                    text = "Download",
                    icon = Icons.Default.Download,
                    onClick = { onStartDownload(selectedFormat, null) },
                    modifier = Modifier.weight(1.4f),
                    testTag = "btn_start_download_sheet"
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
