package com.example.macro_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.macro_tracker.util.NotificationHelper.triggerWelcomeNotificationIfFirstTime(this)
        }
    }

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
        requestNotificationPermission()
        com.example.macro_tracker.util.NotificationHelper.triggerWelcomeNotificationIfFirstTime(this)
        setContent {
            val themeMode by profileViewModel.themeModeLiveData.observeAsState("SYSTEM")
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            Macro_trackerTheme(darkTheme = isDark) {
                AppNavigation(
                    foodViewModel = foodViewModel,
                    profileViewModel = profileViewModel,
                    authViewModel = authViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

