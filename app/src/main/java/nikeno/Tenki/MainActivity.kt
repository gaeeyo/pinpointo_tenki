package nikeno.Tenki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nikeno.Tenki.activity.AreaSelectActivity
import nikeno.Tenki.activity.HelpActivity
import nikeno.Tenki.ui.main.ClickEvent
import nikeno.Tenki.ui.main.MainScreen
import nikeno.Tenki.ui.main.MainViewModel
import nikeno.Tenki.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private var mPrefUrl: String? = null
    private val mViewModel: MainViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                MainScreen(mViewModel,
                    onClick = {
                        when (it) {
                            ClickEvent.OPEN_BROWSER -> {
                                openBrowser()
                            }

                            ClickEvent.CHANGE_AREA -> {
                                navigateChangeArea()
                            }

                            ClickEvent.RELOAD -> {
                                mViewModel.reload()
                            }

                            ClickEvent.CHANGE_THEME -> {
                                val prefs = prefs()
                                prefs.setTheme(
                                    if (prefs.theme.value == Prefs.ThemeNames.DEFAULT)
                                        Prefs.ThemeNames.DARK else Prefs.ThemeNames.DEFAULT
                                )
                            }

                            ClickEvent.HELP -> {
                                navigateHelp()
                            }
                        }
                    }
                )
            }
        }

        if (intent.dataString != null) {
            mViewModel.setUrl(
                Utils.httpsUrl(intent.dataString)
            )
        } else {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    prefs().currentAreaUrl.collect {
                        mViewModel.setUrl(Utils.httpsUrl(it))
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val state = mViewModel.state.value
                if (state.data == null) {
                    mViewModel.requestData()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult")
        when (requestCode) {
            REQUEST_AREA -> if (resultCode == RESULT_OK) {
                mPrefUrl = Utils.httpsUrl(data?.getStringExtra("url"))
                if (mPrefUrl != null) {
                    prefs().setCurrentAreaUrl(mPrefUrl!!)
                }
            }
        }
    }

    private fun navigateChangeArea() {
        val i = Intent(applicationContext, AreaSelectActivity::class.java)
        startActivityForResult(i, REQUEST_AREA)
    }

    private fun navigateHelp() {
        val i = Intent(applicationContext, HelpActivity::class.java)
        startActivityForResult(i, REQUEST_AREA)
    }

    private fun openBrowser() {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(mPrefUrl))
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val REQUEST_AREA: Int = 1
        private const val TAG = "MainActivity"
    }
}
