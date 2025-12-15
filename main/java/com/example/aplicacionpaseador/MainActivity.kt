package com.example.aplicacionpaseador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aplicacionpaseador.data.UserPreferences
import com.example.aplicacionpaseador.ui.screens.AboutScreen
import com.example.aplicacionpaseador.ui.screens.AccountScreen
import com.example.aplicacionpaseador.ui.screens.HomeScreen
import com.example.aplicacionpaseador.ui.screens.LoginScreen
import com.example.aplicacionpaseador.ui.screens.PaseosScreen
import com.example.aplicacionpaseador.ui.screens.RegisterScreen
import com.example.aplicacionpaseador.ui.screens.ReviewsScreen
import com.example.aplicacionpaseador.ui.theme.AplicacionPaseadorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AplicacionPaseadorTheme {
                val navController = rememberNavController()
                val userPreferences = UserPreferences(applicationContext)
                val accessToken by userPreferences.accessToken.collectAsState(initial = null)

                NavHost(
                    navController = navController,
                    startDestination = if (accessToken == null) "login" else "home"
                ) {
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("home") { HomeScreen(navController) }
                    composable("paseos") { PaseosScreen() }
                    composable("reviews") { ReviewsScreen(navController) }
                    composable("account") { AccountScreen() }
                    composable("about") { AboutScreen() }
                }
            }
        }
    }
}
