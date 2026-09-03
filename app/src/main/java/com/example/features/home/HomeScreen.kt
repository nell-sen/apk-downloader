package com.example.features.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MediaFormat
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRed
import com.example.ui.theme.LocalGlassColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDownloads: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onPreviewMedia: (MediaFormat) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val glassColors = LocalGlassColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val uiState by viewModel.uiState.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val clipboardUrl by viewModel.clipboardDetectedUrl.collectAsState()

    // Check clipboard for media links when screen opens
    LaunchedEffect(Unit) {
        viewModel.checkClipboard(context)
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = spring())
                .verticalScroll(rememberScrollState())
                .padding(bottom = 116.dp)
        ) {
            // App Top Bar
            GlassTopBar(
                title = "Nell Downloader",
                subtitle = "Universal Media & HLS Engine"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Clipboard Detection Banner
            AnimatedVisibility(
                visible = clipboardUrl != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                clipboardUrl?.let { detectedLink ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        borderColor = AccentCyan.copy(alpha = 0.5f),
                        backgroundColor = glassColors.glassCard
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Link detected in clipboard",
                                    color = glassColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = detectedLink,
                                    color = glassColors.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassButton(
                                text = "Analyze",
                                onClick = { viewModel.useDetectedClipboardUrl() },
                                modifier = Modifier.heightIn(min = 36.dp),
                                testTag = "btn_use_clipboard"
                            )
                            IconButton(
                                onClick = { viewModel.dismissClipboardUrl() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = glassColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main URL Input Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Media or HLS Stream",
                            color = glassColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text Field
                    GlassTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = "Paste direct URL, web page, or .m3u8...",
                        leadingIcon = Icons.Default.Link,
                        trailingIcon = Icons.Default.ContentPaste,
                        onTrailingIconClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                viewModel.onUrlChanged(clipText)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.analyzeUrl() }
                        ),
                        testTag = "input_media_url"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val isAnalyzing = uiState is HomeUiState.Analyzing

                    GlassButton(
                        text = if (isAnalyzing) "Analyzing URL..." else "Analyze & Download",
                        icon = Icons.Default.Download,
                        isLoading = isAnalyzing,
                        enabled = urlInput.isNotBlank() && !isAnalyzing,
                        onClick = { viewModel.analyzeUrl() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_analyze_url"
                    )
                }
            }

            // Analysis Pipeline State View
            AnimatedVisibility(visible = uiState is HomeUiState.Analyzing) {
                if (uiState is HomeUiState.Analyzing) {
                    val state = uiState as HomeUiState.Analyzing
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        borderColor = AccentCyan.copy(alpha = 0.5f)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.step,
                                    color = glassColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = AccentCyan,
                                trackColor = glassColors.glassCardBorder,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // DRM Protection Warning Dialog / Card
            AnimatedVisibility(visible = uiState is HomeUiState.DrmProtected) {
                if (uiState is HomeUiState.DrmProtected) {
                    val state = uiState as HomeUiState.DrmProtected
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        borderColor = AccentAmber,
                        backgroundColor = glassColors.glassCard
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DRM Protected Stream",
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message,
                                    color = glassColors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                GlassSecondaryButton(
                                    text = "Dismiss",
                                    onClick = { viewModel.resetState() }
                                )
                            }
                        }
                    }
                }
            }

            // Analysis Error View
            AnimatedVisibility(visible = uiState is HomeUiState.AnalysisError) {
                if (uiState is HomeUiState.AnalysisError) {
                    val errorState = uiState as HomeUiState.AnalysisError
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        borderColor = AccentRed.copy(alpha = 0.8f)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unable to Extract Media",
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorState.message,
                                    color = glassColors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassSecondaryButton(
                                        text = "Retry",
                                        onClick = { viewModel.analyzeUrl() }
                                    )
                                    GlassSecondaryButton(
                                        text = "Dismiss",
                                        onClick = { viewModel.resetState() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Only show informational sections if the user isn't interacting with input or analyzing
            AnimatedVisibility(
                visible = urlInput.isBlank() && clipboardUrl == null && uiState !is HomeUiState.Analyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    // Supported Platform Badges
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Supported Formats & Protocols",
                            color = glassColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlatformBadge("HLS / M3U8", AccentCyan)
                            PlatformBadge("Direct MP4 / MKV", AccentBlue)
                            PlatformBadge("MP3 / M4A / FLAC", AccentIndigo)
                            PlatformBadge("AES-128 Encrypted HLS", AccentGreen)
                            PlatformBadge("Web Pages & Feeds", AccentCyan)
                            PlatformBadge("Vimeo & Dailymotion", AccentBlue)
                            PlatformBadge("SoundCloud", AccentAmber)
                            PlatformBadge("YouTube & TikTok (Public)", AccentRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Engine Highlights Card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Engine Highlights",
                                    color = glassColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FeatureRow(
                                icon = Icons.Default.Stream,
                                title = "Full HLS & Master Playlist Engine",
                                desc = "Resolves relative URLs, nested playlists, query tokens, and multi-bitrate streams."
                            )
                            FeatureRow(
                                icon = Icons.Default.Layers,
                                title = "Multi-segment Concurrency & Resume",
                                desc = "Fetches TS segments concurrently with automatic retry and seamless concatenation."
                            )
                            FeatureRow(
                                icon = Icons.Default.PlayCircle,
                                title = "Media3 ExoPlayer Preview",
                                desc = "Inspect video streams, audio tracks, and subtitles before downloading."
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Download Sheet when analysis succeeds
    if (uiState is HomeUiState.AnalysisSuccess) {
        val success = uiState as HomeUiState.AnalysisSuccess
        DownloadOptionsSheet(
            mediaInfo = success.mediaInfo,
            onDismiss = { viewModel.resetState() },
            onStartDownload = { format, title ->
                viewModel.startDownload(success.mediaInfo, format, title)
                onNavigateToDownloads()
            },
            onPreview = { format ->
                onPreviewMedia(format)
            }
        )
    }
}

@Composable
fun PlatformBadge(name: String, accent: Color) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .background(glassColors.glassCard, RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = name,
            color = glassColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    val glassColors = LocalGlassColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = glassColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = desc,
                color = glassColors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

