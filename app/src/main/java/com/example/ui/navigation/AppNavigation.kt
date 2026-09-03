package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.GlassDropApp
import com.example.downloader.DownloadManager
import com.example.features.browser.BrowserScreen
import com.example.features.downloads.DownloadsScreen
import com.example.features.downloads.DownloadsViewModel
import com.example.features.history.HistoryScreen
import com.example.features.home.HomeScreen
import com.example.features.home.HomeViewModel
import com.example.features.player.PlayerScreen
import com.example.features.settings.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentBlue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalGlassColors
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    object Browser : Screen("browser", "Browser", Icons.Default.Language)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppNav(
    downloadManager: DownloadManager,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    sharedUrl: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val homeViewModel = remember { HomeViewModel(downloadManager) }
    val downloadsViewModel = remember { DownloadsViewModel(downloadManager) }
    val snackbarHostState = remember { SnackbarHostState() }

    // If shared from another app via ACTION_SEND
    remember(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            homeViewModel.onUrlChanged(sharedUrl)
            homeViewModel.analyzeUrl(sharedUrl)
        }
        Unit
    }

    val downloads by downloadManager.getAllDownloads().collectAsState(initial = emptyList())
    val activeDownloadsCount = downloads.count {
        it.status == com.example.domain.model.DownloadStatus.DOWNLOADING ||
        it.status == com.example.domain.model.DownloadStatus.QUEUED
    }

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Downloads,
        Screen.Browser,
        Screen.History,
        Screen.Settings
    )

    val isPlayerRoute = currentRoute.startsWith("player/")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isPlayerRoute) {
                GlassFloatingNavBar(
                    screens = bottomNavScreens,
                    currentRoute = currentRoute,
                    activeCount = activeDownloadsCount,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                    onNavigateToBrowser = { navController.navigate(Screen.Browser.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onPreviewMedia = { format ->
                        val encoded = URLEncoder.encode(format.url, StandardCharsets.UTF_8.toString())
                        navController.navigate("player/$encoded")
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel,
                    onPlayMedia = { filePath ->
                        val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                        navController.navigate("player/$encoded")
                    }
                )
            }

            composable(Screen.Browser.route) {
                BrowserScreen(
                    onInspectUrl = { url ->
                        homeViewModel.onUrlChanged(url)
                        homeViewModel.analyzeUrl(url)
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    downloadManager = downloadManager,
                    onSelectUrl = { url ->
                        homeViewModel.onUrlChanged(url)
                        homeViewModel.analyzeUrl(url)
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    downloadManager = downloadManager,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme
                )
            }

            composable("player/{url}") { backStack ->
                val encodedUrl = backStack.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                PlayerScreen(
                    mediaUrl = decodedUrl,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun GlassFloatingNavBar(
    screens: List<Screen>,
    currentRoute: String,
    activeCount: Int,
    onNavigate: (Screen) -> Unit
) {
    val glassColors = LocalGlassColors.current
    NavigationBar(
        containerColor = glassColors.surface,
        contentColor = glassColors.textSecondary,
        tonalElevation = 0.dp
    ) {
        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    if (screen == Screen.Downloads && activeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = AccentBlue,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$activeCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    unselectedIconColor = glassColors.textSecondary,
                    selectedTextColor = AccentBlue,
                    unselectedTextColor = glassColors.textSecondary,
                    indicatorColor = AccentBlue.copy(alpha = 0.15f)
                )
            )
        }
    }
}
