package nikeno.Tenki.ui.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import nikeno.Tenki.Area
import nikeno.Tenki.ui.screen.help.HelpScreen
import nikeno.Tenki.ui.screen.main.MainScreen
import nikeno.Tenki.ui.screen.selectarea.SelectAreaScreen

@Composable
fun MyAppNavHost(startDestination: Any = ScreenMain) {
    val navController = rememberNavController()
    val navigator = MyAppNavigator(navController)

    NavHost(navController = navController, startDestination = startDestination) {
        composable<ScreenMain> {
            MainScreen(navigator)
        }
        composable<ScreenSelectArea> {
            SelectAreaScreen(onSelectArea = {
                navigator.setSelectedArea(it)
                navigator.back()
            }, onBackPressed = {
                navController.popBackStack()
            })
        }
        composable<ScreenMainWithUrl> {
            val args = it.toRoute<ScreenMainWithUrl>()
            MainScreen(navigator, url = args.url)
        }
        composable<ScreenHelp> {
            HelpScreen(navigator)
        }
    }
}

@Serializable
object ScreenMain

@Serializable
object ScreenSelectArea

@Serializable
data class ScreenMainWithUrl(val url: String)

@Serializable
object ScreenHelp

class MyAppNavigator(val navController: NavController) {
    fun back() {
        navController.popBackStack()
    }

    fun toSelectArea() {
        navController.navigate(ScreenSelectArea)
    }

    fun toHelp() {
        navController.navigate(ScreenHelp)
    }

    fun getSelectedArea(): Area? {
        return navController.currentBackStackEntry?.savedStateHandle?.get<Area>("selectedArea")
    }

    fun setSelectedArea(area: Area) {
        navController.previousBackStackEntry?.savedStateHandle?.set("selectedArea", area)
    }
}