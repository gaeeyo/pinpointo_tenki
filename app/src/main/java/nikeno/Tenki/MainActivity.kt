package nikeno.Tenki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nikeno.Tenki.ui.app.MyAppNavHost
import nikeno.Tenki.ui.app.MyAppTheme

class MainActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyAppTheme {
                MyAppNavHost()
            }
        }
    }
}
