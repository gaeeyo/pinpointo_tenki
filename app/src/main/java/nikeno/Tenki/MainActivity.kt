package nikeno.Tenki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nikeno.Tenki.ui.app.MyAppNavHost
import nikeno.Tenki.ui.app.MyAppTheme
import nikeno.Tenki.ui.app.ScreenMain
import nikeno.Tenki.ui.app.ScreenMainWithUrl

class MainActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        var startDestination: Any = ScreenMain
        if (uri != null) {
            startDestination = ScreenMainWithUrl(uri.toString())
        }
        setContent {
            MyAppTheme {
                MyAppNavHost(startDestination)
            }
        }
    }
}
