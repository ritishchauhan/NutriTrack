package com.example.macro_tracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.macro_tracker.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for user profile, dietary preferences, and nutritional goals,
 * adhering to MVVM architecture by communicating exclusively with [UserRepository],
 * lifecycle-aware via [LiveData] and [StateFlow], and optimized with WhileSubscribed(5_000).
 */
class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    val userName: StateFlow<String> = userRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val userNameLiveData: LiveData<String> = userName.asLiveData(viewModelScope.coroutineContext)

    val dietaryPreference: StateFlow<String> = userRepository.dietaryPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Non-veg")
    val dietaryPreferenceLiveData: LiveData<String> = dietaryPreference.asLiveData(viewModelScope.coroutineContext)

    val activityLevel: StateFlow<String> = userRepository.activityLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Sedentary")
    val activityLevelLiveData: LiveData<String> = activityLevel.asLiveData(viewModelScope.coroutineContext)

    val calorieGoal: StateFlow<Int> = userRepository.calorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2000)
    val calorieGoalLiveData: LiveData<Int> = calorieGoal.asLiveData(viewModelScope.coroutineContext)

    val proteinGoal: StateFlow<Int> = userRepository.proteinGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 120)
    val proteinGoalLiveData: LiveData<Int> = proteinGoal.asLiveData(viewModelScope.coroutineContext)

    val carbsGoal: StateFlow<Int> = userRepository.carbsGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 240)
    val carbsGoalLiveData: LiveData<Int> = carbsGoal.asLiveData(viewModelScope.coroutineContext)

    val fatGoal: StateFlow<Int> = userRepository.fatGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 70)
    val fatGoalLiveData: LiveData<Int> = fatGoal.asLiveData(viewModelScope.coroutineContext)

    val fiberGoal: StateFlow<Int> = userRepository.fiberGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)
    val fiberGoalLiveData: LiveData<Int> = fiberGoal.asLiveData(viewModelScope.coroutineContext)

    val waterGoal: StateFlow<Float> = userRepository.waterGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2.4f)
    val waterGoalLiveData: LiveData<Float> = waterGoal.asLiveData(viewModelScope.coroutineContext)

    val waterLogged: StateFlow<Float> = userRepository.waterLogged
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0f)
    val waterLoggedLiveData: LiveData<Float> = waterLogged.asLiveData(viewModelScope.coroutineContext)

    val streakDays: StateFlow<Int> = userRepository.streakDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val streakDaysLiveData: LiveData<Int> = streakDays.asLiveData(viewModelScope.coroutineContext)

    val userWeightKg: StateFlow<Float> = userRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 70f)
    val userWeightKgLiveData: LiveData<Float> = userWeightKg.asLiveData(viewModelScope.coroutineContext)

    val userHeightCm: StateFlow<Float> = userRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 170f)
    val userHeightCmLiveData: LiveData<Float> = userHeightCm.asLiveData(viewModelScope.coroutineContext)

    val userFitnessGoal: StateFlow<String> = userRepository.userFitnessGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "LOSE_WEIGHT")
    val userFitnessGoalLiveData: LiveData<String> = userFitnessGoal.asLiveData(viewModelScope.coroutineContext)

    val onboardingCompleted: StateFlow<Boolean> = userRepository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val onboardingCompletedLiveData: LiveData<Boolean> = onboardingCompleted.asLiveData(viewModelScope.coroutineContext)

    val isProfileLoaded: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        userRepository.userName,
        userRepository.onboardingCompleted
    ) { _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isProfileLoadedLiveData: LiveData<Boolean> = isProfileLoaded.asLiveData(viewModelScope.coroutineContext)

    fun saveUserName(name: String) {
        viewModelScope.launch {
            userRepository.saveUserName(name)
        }
    }

    fun saveDietaryPreference(preference: String) {
        viewModelScope.launch {
            userRepository.saveDietaryPreference(preference)
        }
    }

    fun saveActivityLevel(level: String) {
        viewModelScope.launch {
            userRepository.saveActivityLevel(level)
        }
    }

    fun saveGoals(calories: Int, protein: Int, carbs: Int, fat: Int, water: Float, fiber: Int? = null) {
        viewModelScope.launch {
            userRepository.saveGoals(calories, protein, carbs, fat, water, fiber)
        }
    }

    fun addWater(amount: Float) {
        viewModelScope.launch {
            userRepository.addWater(amount)
        }
    }

    fun decreaseWater(amount: Float) {
        viewModelScope.launch {
            userRepository.decreaseWater(amount)
        }
    }

    fun saveBodyStats(weightKg: Float, heightCm: Float, goal: String) {
        viewModelScope.launch {
            userRepository.saveBodyStats(weightKg, heightCm, goal)
        }
    }

    fun completeOnboarding(name: String, calories: Int, diet: String, activity: String) {
        viewModelScope.launch {
            userRepository.completeOnboarding(name, calories, diet, activity)
        }
    }

    /**
     * Clears profile state from active session on logout.
     */
    fun clearSessionData() {
        userRepository.clearActiveSession()
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
