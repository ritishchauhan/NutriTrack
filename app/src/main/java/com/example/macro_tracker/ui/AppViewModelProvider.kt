package com.example.macro_tracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.macro_tracker.MacroTrackerApplication

/**
 * Provides Factory to create instances of ViewModels for the app, wiring up repositories
 * from [MacroTrackerApplication.container].
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            FoodViewModel(
                foodRepository = macroTrackerApplication().container.foodRepository
            )
        }
        initializer {
            ProfileViewModel(
                userRepository = macroTrackerApplication().container.userRepository,
                weightRepository = macroTrackerApplication().container.weightRepository
            )
        }
        initializer {
            AuthViewModel(
                authRepository = macroTrackerApplication().container.authRepository,
                userRepository = macroTrackerApplication().container.userRepository
            )
        }

    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [MacroTrackerApplication].
 */
fun CreationExtras.macroTrackerApplication(): MacroTrackerApplication =
    (this[APPLICATION_KEY] as MacroTrackerApplication)
