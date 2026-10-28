package vegabobo.dsusideloader.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vegabobo.dsusideloader.ui.screen.about.AboutScreen
import vegabobo.dsusideloader.ui.screen.adb.AdbScreen
import vegabobo.dsusideloader.ui.screen.history.HistoryScreen
import vegabobo.dsusideloader.ui.screen.home.Home
import vegabobo.dsusideloader.ui.screen.libraries.LibrariesScreen
import vegabobo.dsusideloader.ui.screen.onboarding.Onboarding
import vegabobo.dsusideloader.ui.screen.settings.Settings

object Destinations {
    const val Homepage = "home"
    const val Preferences = "preferences"
    const val ADBInstallation = "adb_installation"
    const val About = "about"
    const val Libraries = "libraries"
    const val History = "history"
    const val Onboarding = "onboarding"
    const val Up = "up"
}

/**
 * @param startDestination [Destinations.Homepage] when an access method is
 * already confirmed, [Destinations.Onboarding] when the user still has to pick
 * and verify one.
 */
@Composable
fun Navigation(startDestination: String = Destinations.Homepage) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        fun navigate(destination: String) {
            when (destination) {
                Destinations.Up -> navController.navigateUp()
                // Going back to the home screen must not stack a second
                // instance, otherwise the session state would be duplicated.
                Destinations.Homepage -> navController.popBackStack(
                    Destinations.Homepage,
                    inclusive = false,
                )
                else -> navController.navigate(destination)
            }
        }

        composable(Destinations.Homepage) { Home(navigate = { navigate(it) }) }
        composable(Destinations.Preferences) { Settings(navigate = { navigate(it) }) }
        composable(Destinations.ADBInstallation) { AdbScreen(navigate = { navigate(it) }) }
        composable(Destinations.About) { AboutScreen(navigate = { navigate(it) }) }
        composable(Destinations.Libraries) { LibrariesScreen(navigate = { navigate(it) }) }
        composable(Destinations.History) { HistoryScreen(navigate = { navigate(it) }) }
        composable(Destinations.Onboarding) { Onboarding(navigate = { navigate(it) }) }
    }
}
