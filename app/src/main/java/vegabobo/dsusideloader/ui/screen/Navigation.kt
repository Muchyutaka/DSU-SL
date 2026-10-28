package vegabobo.dsusideloader.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.screen.about.AboutScreen
import vegabobo.dsusideloader.ui.screen.adb.AdbScreen
import vegabobo.dsusideloader.ui.screen.history.HistoryScreen
import vegabobo.dsusideloader.ui.screen.libraries.LibrariesScreen
import vegabobo.dsusideloader.ui.screen.log.LogScreen
import vegabobo.dsusideloader.ui.screen.main.MainScreen
import vegabobo.dsusideloader.ui.screen.onboarding.Onboarding
import vegabobo.dsusideloader.ui.screen.settings.Settings
import vegabobo.dsusideloader.ui.util.motionDuration

object Destinations {
    const val Homepage = "home"
    const val Log = "log"
    const val Preferences = "preferences"
    const val ADBInstallation = "adb_installation"
    const val About = "about"
    const val Libraries = "libraries"
    const val History = "history"
    const val Onboarding = "onboarding"
    const val Up = "up"
}

/** One entry of the bottom navigation bar. */
private data class BottomTab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

private val bottomTabs = listOf(
    BottomTab(Destinations.Homepage, Icons.Outlined.Home, R.string.nav_main),
    BottomTab(Destinations.Log, Icons.Outlined.List, R.string.log_title),
    BottomTab(Destinations.Preferences, Icons.Outlined.Settings, R.string.settings),
)

/**
 * Hosts every screen of the app.
 *
 * The three top level tabs share a bottom navigation bar; the detail screens
 * (installation commands, about, libraries, history, onboarding) are full
 * screen routes without it. The bar animates in and out so switching between a
 * tab and a detail screen does not jump.
 *
 * @param startDestination [Destinations.Homepage] when an access method is
 * already confirmed, [Destinations.Onboarding] when the user still has to pick
 * and verify one.
 */
@Composable
fun Navigation(startDestination: String = Destinations.Homepage) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1F)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(Destinations.Homepage) {
                    MainScreen(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.Log) { LogScreen() }
                composable(Destinations.Preferences) {
                    Settings(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.ADBInstallation) {
                    AdbScreen(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.About) {
                    AboutScreen(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.Libraries) {
                    LibrariesScreen(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.History) {
                    HistoryScreen(navigate = { navigateTo(navController, it) })
                }
                composable(Destinations.Onboarding) {
                    Onboarding(navigate = { navigateTo(navController, it) })
                }
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(motionDuration(180))) +
                expandVertically(tween(motionDuration(220))),
            exit = fadeOut(tween(motionDuration(120))) +
                shrinkVertically(tween(motionDuration(180))),
        ) {
            BottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navigateTo(navController, route)
                },
            )
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(id = tab.labelRes),
                    )
                },
                label = { Text(text = stringResource(id = tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Single place that knows how to move between destinations:
 * - [Destinations.Up]        goes back
 * - [Destinations.Homepage]  returns to the existing instance (no duplicate)
 * - a tab route              pops back to the tab, or pushes it once
 * - anything else            is pushed normally
 */
private fun navigateTo(navController: NavHostController, destination: String) {
    when (destination) {
        Destinations.Up -> navController.navigateUp()

        Destinations.Homepage -> {
            if (!navController.popBackStack(Destinations.Homepage, inclusive = false)) {
                navController.navigate(Destinations.Homepage) {
                    launchSingleTop = true
                }
            }
        }

        Destinations.Log, Destinations.Preferences -> {
            if (!navController.popBackStack(destination, inclusive = false)) {
                navController.navigate(destination) {
                    launchSingleTop = true
                }
            }
        }

        else -> navController.navigate(destination) {
            launchSingleTop = true
        }
    }
}
