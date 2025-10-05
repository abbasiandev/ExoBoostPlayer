package dev.abbasian.exoboostplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dev.abbasian.exoboostplayer.presentation.ui.demo.MainScreen
import dev.abbasian.exoboostplayer.presentation.ui.theme.ExoBoostPlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ExoBoostPlayerTheme {
                MainScreen()
            }
        }
    }
}