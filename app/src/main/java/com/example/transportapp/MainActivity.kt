package com.example.transportapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransportAppTheme {
                // Edge-to-edge: the surface colour paints under the status bar, but the nav
                // content is inset below it — top-bar buttons must not sit under the system
                // StatusBar's touchable region (found in the S3 demo: taps at y<156 were
                // swallowed by the system window).
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                        }
                    }
                }
            }
        }
    }

    // The headless print drive (S13) attaches its WebView to a real window — the
    // repository layer carries only the Application context, so the resumed activity
    // registers itself for the renderer to pick up.
    override fun onResume() {
        super.onResume()
        com.example.transportapp.pdf.CurrentActivity.register(this)
    }

    override fun onPause() {
        com.example.transportapp.pdf.CurrentActivity.unregister(this)
        super.onPause()
    }
}

@Preview
@Composable
fun AppPreview() {
    TransportAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            AppNavHost(navController = navController)
        }
    }
}
