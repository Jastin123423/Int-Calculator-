package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CalcProTheme
import com.example.ui.tools.ToolsScreen
import com.example.ui.tools.aimath.AiMathAssistantScreen
import com.example.ui.tools.currency.CurrencyConverterScreen
import com.example.ui.tools.datetime.DateTimeScreen
import com.example.ui.tools.equations.EquationSolverScreen
import com.example.ui.tools.finance.FinanceScreen
import com.example.ui.tools.mathsolver.MathSolverScreen
import com.example.ui.tools.percentage.PercentageScreen
import com.example.ui.tools.unitconverter.UnitConverterScreen
import com.example.ui.vault.VaultAlbumsScreen
import com.example.ui.vault.VaultAuthScreen
import com.example.ui.vault.VaultContactsScreen
import com.example.ui.vault.VaultGalleryScreen
import com.example.ui.vault.VaultHomeScreen
import com.example.ui.vault.VaultMediaViewerScreen
import com.example.ui.vault.VaultSecurityScreen
import com.example.ui.vault.VaultTrashScreen
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.MediaFilter
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.VaultViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Calculator : BottomNavItem("calculator", "Calculator", Icons.Default.Calculate)
    object History : BottomNavItem("history", "History", Icons.Default.History)
    object Tools : BottomNavItem("tools", "Tools", Icons.Default.GridView)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun CalcProMainApp(
    calculatorViewModel: CalculatorViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    vaultViewModel: VaultViewModel
) {
    val currentTheme by settingsViewModel.appTheme.collectAsState()
    val currentAccent by settingsViewModel.accentColor.collectAsState()
    val toastMessage by vaultViewModel.toastMessage.collectAsState()
    val isVaultUnlocked by vaultViewModel.isVaultUnlocked.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vaultViewModel.clearToast()
        }
    }

    CalcProTheme(
        appTheme = currentTheme,
        accentColorEnum = currentAccent
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavItems = listOf(
            BottomNavItem.Calculator,
            BottomNavItem.History,
            BottomNavItem.Tools,
            BottomNavItem.Settings
        )

        // Show bottom navigation bar only on main tabs
        val showBottomBar = currentRoute in listOf("calculator", "history", "tools", "settings")

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = com.example.ui.theme.CyanAccent,
                                    selectedTextColor = com.example.ui.theme.CyanAccent,
                                    indicatorColor = com.example.ui.theme.CyanAccent.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "calculator",
                modifier = Modifier.padding(innerPadding)
            ) {
                // Calculator Main
                composable("calculator") {
                    CalculatorScreen(
                        viewModel = calculatorViewModel,
                        onNavigateToTool = { toolRoute ->
                            navController.navigate(toolRoute)
                        },
                        onNavigateToVault = {
                            if (isVaultUnlocked) {
                                navController.navigate("vault_home")
                            } else {
                                navController.navigate("vault_auth")
                            }
                        }
                    )
                }

                // History
                composable("history") {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onReuseCalculation = { expr, res ->
                            calculatorViewModel.loadExpression(expr, res)
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                // Tools Hub
                composable("tools") {
                    ToolsScreen(
                        onNavigateToTool = { toolRoute ->
                            if (toolRoute == "vault_auth") {
                                if (isVaultUnlocked) {
                                    navController.navigate("vault_home")
                                } else {
                                    navController.navigate("vault_auth")
                                }
                            } else {
                                navController.navigate(toolRoute)
                            }
                        }
                    )
                }

                // Settings
                composable("settings") {
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }

                composable("scientific_panel") {
                    calculatorViewModel.toggleScientificPanel()
                    navController.navigate("calculator") {
                        popUpTo("calculator") { inclusive = true }
                    }
                }

                // Utility tools
                composable("tool_unit_converter") {
                    UnitConverterScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_currency_converter") {
                    CurrencyConverterScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_percentage") {
                    PercentageScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_finance") {
                    FinanceScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_datetime") {
                    DateTimeScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_equation_solver") {
                    EquationSolverScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable("tool_math_solver") {
                    MathSolverScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSolveExtractedMath = { mathExpr ->
                            calculatorViewModel.loadExpression(mathExpr, "0")
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("tool_ai_assistant") {
                    AiMathAssistantScreen(onNavigateBack = { navController.popBackStack() })
                }

                // --- PRIVATE VAULT ROUTES ---
                composable("vault_auth") {
                    VaultAuthScreen(
                        viewModel = vaultViewModel,
                        onAuthenticated = {
                            navController.navigate("vault_home") {
                                popUpTo("vault_auth") { inclusive = true }
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }

                composable("vault_home") {
                    VaultHomeScreen(
                        viewModel = vaultViewModel,
                        onNavigateToPhotos = { navController.navigate("vault_photos") },
                        onNavigateToVideos = { navController.navigate("vault_videos") },
                        onNavigateToContacts = { navController.navigate("vault_contacts") },
                        onNavigateToAlbums = { navController.navigate("vault_albums") },
                        onNavigateToFavorites = { navController.navigate("vault_favorites") },
                        onNavigateToTrash = { navController.navigate("vault_trash") },
                        onNavigateToSecurity = { navController.navigate("vault_security") },
                        onNavigateToMediaViewer = { mediaId ->
                            navController.navigate("vault_media_viewer/$mediaId")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_photos") {
                    VaultGalleryScreen(
                        viewModel = vaultViewModel,
                        title = "Private Photos",
                        filterMode = MediaFilter.PHOTOS,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMediaViewer = { mediaId ->
                            navController.navigate("vault_media_viewer/$mediaId")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_videos") {
                    VaultGalleryScreen(
                        viewModel = vaultViewModel,
                        title = "Private Videos",
                        filterMode = MediaFilter.VIDEOS,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMediaViewer = { mediaId ->
                            navController.navigate("vault_media_viewer/$mediaId")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_favorites") {
                    VaultGalleryScreen(
                        viewModel = vaultViewModel,
                        title = "Starred Favorites",
                        filterMode = MediaFilter.FAVORITES,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMediaViewer = { mediaId ->
                            navController.navigate("vault_media_viewer/$mediaId")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = "vault_album/{albumName}",
                    arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumName = backStackEntry.arguments?.getString("albumName") ?: "Default"
                    VaultGalleryScreen(
                        viewModel = vaultViewModel,
                        title = "Album: $albumName",
                        filterMode = MediaFilter.ALL,
                        albumName = albumName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMediaViewer = { mediaId ->
                            navController.navigate("vault_media_viewer/$mediaId")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_albums") {
                    VaultAlbumsScreen(
                        viewModel = vaultViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenAlbum = { albumName ->
                            navController.navigate("vault_album/$albumName")
                        },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_contacts") {
                    VaultContactsScreen(
                        viewModel = vaultViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_trash") {
                    VaultTrashScreen(
                        viewModel = vaultViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable("vault_security") {
                    VaultSecurityScreen(
                        viewModel = vaultViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onLockVault = {
                            vaultViewModel.lockVault()
                            navController.navigate("calculator") {
                                popUpTo("calculator") { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = "vault_media_viewer/{mediaId}",
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                    VaultMediaViewerScreen(
                        viewModel = vaultViewModel,
                        initialMediaId = mediaId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
