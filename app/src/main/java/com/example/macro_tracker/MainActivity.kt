package com.example.macro_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.macro_tracker.ui.AppNavigation
import com.example.macro_tracker.ui.AppViewModelProvider
import com.example.macro_tracker.ui.AuthViewModel
import com.example.macro_tracker.ui.FoodViewModel
import com.example.macro_tracker.ui.ProfileViewModel
import com.example.macro_tracker.ui.theme.Macro_trackerTheme

/**
 * MainActivity serving as the entry point of the app, obtaining ViewModels via
 * [AppViewModelProvider.Factory] in accordance with Android MVVM architecture.
 */
class MainActivity : ComponentActivity() {

    private val foodViewModel: FoodViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private val authViewModel: AuthViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Macro_trackerTheme {
                AppNavigation(
                    foodViewModel = foodViewModel,
                    profileViewModel = profileViewModel,
                    authViewModel = authViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

