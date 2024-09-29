package nikeno.Tenki.ui.screen.help

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import nikeno.Tenki.R
import nikeno.Tenki.ui.app.MyTopBar

@Composable
fun HelpScreen(navController: NavController) {
    HelpScreen(onClickBack = navController.previousBackStackEntry?.let { ({ navController.popBackStack() }) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onClickBack: (() -> Unit)?) {
    Scaffold(
        topBar = {
            MyTopBar(title = { Text(stringResource(R.string.menu_help)) },
                navigationIcon = {
                    if (onClickBack != null) {
                        IconButton(onClick = onClickBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                })
        },

        ) {
        val context = LocalContext.current
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val v = WebView(context)
                    v.loadUrl("file:///android_asset/help.html")
                    v
                })
        }
    }
}