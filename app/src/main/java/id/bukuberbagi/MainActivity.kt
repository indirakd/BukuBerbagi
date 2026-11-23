package id.bukuberbagi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import id.bukuberbagi.ui.navigation.BukuBerbagiApp
import id.bukuberbagi.ui.theme.BukuBerbagiTheme
import id.bukuberbagi.ui.theme.Cream
import id.bukuberbagi.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Cream.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Paper.toArgb(), Paper.toArgb())
        )
        setContent {
            BukuBerbagiTheme {
                BukuBerbagiApp()
            }
        }
    }
}
