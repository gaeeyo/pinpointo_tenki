package nikeno.Tenki.ui.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import nikeno.Tenki.ui.screen.main.MainScreen
import nikeno.Tenki.ui.screen.selectarea.SelectAreaScreen

@Composable
fun MyAppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ScreenMain) {
        composable<ScreenMain> {
            MainScreen(navController)
        }
        composable<ScreenSelectArea> {
            SelectAreaScreen(navController)
        }
        composable<ScreenMainWithUrl> {
            val args = it.toRoute<ScreenMainWithUrl>()
            MainScreen(navController, url = args.url)
        }
    }
}

@Serializable
object ScreenMain

@Serializable
object ScreenSelectArea

@Serializable
data class ScreenMainWithUrl(val url: String)