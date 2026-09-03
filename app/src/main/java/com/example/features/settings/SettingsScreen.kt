package com.example.features.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.downloader.DownloadManager
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.LocalGlassColors

@Composable
fun SettingsScreen(
    downloadManager: DownloadManager,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val glassColors = LocalGlassColors.current
    val context = LocalContext.current

    var maxConcurrent by remember { mutableIntStateOf(downloadManager.maxConcurrentDownloads) }
    var segmentConcurrency by remember { mutableIntStateOf(downloadManager.hlsConcurrency) }
    var wifiOnly by remember { mutableStateOf(false) }

    val freeStorage = downloadManager.storageManager.getAvailableStorageBytes()
    val freeStorageStr = String.format("%.1f GB free", freeStorage / (1024.0 * 1024.0 * 1024.0))

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                
        ) {
            GlassTopBar(
                title = "Settings",
                subtitle = "Preferences & Storage"
            )

            // Appearance Section
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance",
                            color = glassColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dark Glass Theme",
                                color = glassColors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Luminous frosted glass backdrop",
                                color = glassColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Downloader Engine Settings
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Engine",
                            color = glassColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Max Concurrent Downloads",
                        color = glassColors.textPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { count ->
                            GlassChip(
                                text = "$count Active",
                                isSelected = maxConcurrent == count,
                                onClick = {
                                    maxConcurrent = count
                                    downloadManager.maxConcurrentDownloads = count
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "HLS Segment Concurrency",
                        color = glassColors.textPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 6, 8).forEach { segs ->
                            GlassChip(
                                text = "$segs Segments",
                                isSelected = segmentConcurrency == segs,
                                onClick = {
                                    segmentConcurrency = segs
                                    downloadManager.hlsConcurrency = segs
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wi-Fi Only Downloads",
                                color = glassColors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Prevent mobile data consumption",
                                color = glassColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = { wifiOnly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Storage & Maintenance
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage & Cache",
                            color = glassColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Device Storage",
                                color = glassColors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = freeStorageStr,
                                color = glassColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        GlassSecondaryButton(
                            text = "Clear Temp Cache",
                            icon = Icons.Default.CleaningServices,
                            onClick = {
                                downloadManager.storageManager.clearAllTempFiles()
                                Toast.makeText(context, "Temporary cache cleared!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // About Application
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Nell Downloader",
                            color = glassColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nell Downloader is a high-performance native Android media downloader with complete HLS/M3U8 master playlist parsing, AES-128 decryptor, multi-thread segment worker, and Media3 ExoPlayer preview.",
                        color = glassColors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Version 1.0.0 (Native Android Build)",
                        color = glassColors.accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
