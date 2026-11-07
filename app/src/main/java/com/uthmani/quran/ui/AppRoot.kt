package com.uthmani.quran.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uthmani.quran.R
import com.uthmani.quran.ui.about.AboutScreen
import com.uthmani.quran.ui.bookmarks.BookmarksScreen
import com.uthmani.quran.ui.fonttest.FontTestScreen
import com.uthmani.quran.ui.reader.ReaderScreen
import com.uthmani.quran.ui.search.SearchScreen
import com.uthmani.quran.ui.settings.SettingsScreen
import com.uthmani.quran.ui.surahs.SurahListScreen

object Routes {
    const val SURAH_LIST = "surah_list"
    const val SEARCH = "search"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val FONT_TEST = "font_test"
    const val READER = "reader/{surah}?ayah={ayah}"

    fun reader(surah: Int, ayah: Int = 0): String = "reader/$surah?ayah=$ayah"
}

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topDestinations = listOf(
        TopDestination(Routes.SURAH_LIST, R.string.tab_quran, Icons.Filled.MenuBook),
        TopDestination(Routes.SEARCH, R.string.tab_search, Icons.Filled.Search),
        TopDestination(Routes.BOOKMARKS, R.string.tab_bookmarks, Icons.Filled.Star),
        TopDestination(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in topDestinations.map { it.route }) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SURAH_LIST,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.SURAH_LIST) {
                SurahListScreen(
                    onOpenSurah = { surah, ayah ->
                        navController.navigate(Routes.reader(surah, ayah))
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onOpenAyah = { surah, ayah ->
                        navController.navigate(Routes.reader(surah, ayah))
                    },
                )
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(
                    onOpenAyah = { surah, ayah ->
                        navController.navigate(Routes.reader(surah, ayah))
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenFontTest = { navController.navigate(Routes.FONT_TEST) },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FONT_TEST) {
                FontTestScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("surah") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) {
                ReaderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
