package com.example.macro_tracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Account-partitioned UserProfileManager.
 * 
 * Enforces strict per-account isolation for all user preferences, nutritional targets,
 * body statistics, streaks, and water logs.
 * 
 * Local storage files are explicitly separated by User ID (user_profile_{userId}.preferences_pb).
 * When an account signs out, active session state is wiped from memory, and flows emit default/blank states.
 * When a different account logs in, only that account's partitioned DataStore is loaded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileManager(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val DIETARY_PREFERENCE = stringPreferencesKey("dietary_preference")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val CARBS_GOAL = intPreferencesKey("carbs_goal")
        val FAT_GOAL = intPreferencesKey("fat_goal")
        val FIBER_GOAL = intPreferencesKey("fiber_goal")
        val WATER_GOAL = floatPreferencesKey("water_goal")
        val WATER_LOGGED = floatPreferencesKey("water_logged")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val USER_GMAIL = stringPreferencesKey("user_gmail")
        val USER_WEIGHT_KG = floatPreferencesKey("user_weight_kg")
        val USER_HEIGHT_CM = floatPreferencesKey("user_height_cm")
        val USER_FITNESS_GOAL = stringPreferencesKey("user_fitness_goal")
        val USER_THEME_MODE = stringPreferencesKey("user_theme_mode")

        const val DEFAULT_CALORIE_GOAL = 2000
        const val DEFAULT_PROTEIN_GOAL = 120
        const val DEFAULT_CARBS_GOAL = 240
        const val DEFAULT_FAT_GOAL = 70
        const val DEFAULT_FIBER_GOAL = 25
        const val DEFAULT_WATER_GOAL = 2.4f
        const val DEFAULT_WATER_LOGGED = 0.0f
        const val DEFAULT_STREAK_DAYS = 0
        const val DEFAULT_WEIGHT_KG = 70.0f
        const val DEFAULT_HEIGHT_CM = 170.0f
        const val DEFAULT_FITNESS_GOAL = "LOSE_WEIGHT"
        const val DEFAULT_DIETARY_PREFERENCE = "Non-veg"
        const val DEFAULT_ACTIVITY_LEVEL = "Sedentary"
        const val DEFAULT_THEME_MODE = "SYSTEM"
    }

    private val _activeUserId = MutableStateFlow<String?>(
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null && !user.isAnonymous && !user.uid.startsWith("guest_")) user.uid else null
        } catch (ignored: Exception) {
            null
        }
    )
    val activeUserId: StateFlow<String?> = _activeUserId.asStateFlow()

    private val dataStoreCache = ConcurrentHashMap<String, DataStore<Preferences>>()

    /**
     * Sets or switches the currently active authenticated user ID.
     */
    fun setActiveUser(userId: String?) {
        val cleanId = userId?.trim()?.ifBlank { null }
        if (cleanId != null && (cleanId.startsWith("guest_") || cleanId == "guest")) {
            _activeUserId.value = null
        } else {
            _activeUserId.value = cleanId
        }
    }

    /**
     * Wipes active session credentials from memory on sign-out.
     */
    fun clearActiveSession() {
        _activeUserId.value = null
    }

    /**
     * Obtains or creates an isolated DataStore for the given user ID.
     */
    private fun getDataStoreForUser(userId: String): DataStore<Preferences> {
        val sanitized = userId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return dataStoreCache.computeIfAbsent(sanitized) {
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("user_profile_$sanitized") }
            )
        }
    }

    /**
     * Reactive preferences flow dynamically bound to the current authenticated user.
     * When signed out, it safely yields emptyPreferences without exposing prior user data.
     */
    private val currentPreferencesFlow: Flow<Preferences> = _activeUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyPreferences())
        } else {
            getDataStoreForUser(uid).data.catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
        }
    }

    val userWeightKgFlow: Flow<Float> = currentPreferencesFlow.map { preferences ->
        preferences[USER_WEIGHT_KG] ?: DEFAULT_WEIGHT_KG
    }

    val userHeightCmFlow: Flow<Float> = currentPreferencesFlow.map { preferences ->
        preferences[USER_HEIGHT_CM] ?: DEFAULT_HEIGHT_CM
    }

    val userFitnessGoalFlow: Flow<String> = currentPreferencesFlow.map { preferences ->
        preferences[USER_FITNESS_GOAL] ?: DEFAULT_FITNESS_GOAL
    }

    val userNameFlow: Flow<String> = currentPreferencesFlow.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val dietaryPreferenceFlow: Flow<String> = currentPreferencesFlow.map { preferences ->
        preferences[DIETARY_PREFERENCE] ?: DEFAULT_DIETARY_PREFERENCE
    }

    val activityLevelFlow: Flow<String> = currentPreferencesFlow.map { preferences ->
        preferences[ACTIVITY_LEVEL] ?: DEFAULT_ACTIVITY_LEVEL
    }

    val calorieGoalFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[CALORIE_GOAL] ?: DEFAULT_CALORIE_GOAL
    }

    val proteinGoalFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[PROTEIN_GOAL] ?: DEFAULT_PROTEIN_GOAL
    }

    val carbsGoalFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[CARBS_GOAL] ?: DEFAULT_CARBS_GOAL
    }

    val fatGoalFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[FAT_GOAL] ?: DEFAULT_FAT_GOAL
    }

    val fiberGoalFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[FIBER_GOAL] ?: DEFAULT_FIBER_GOAL
    }

    val waterGoalFlow: Flow<Float> = currentPreferencesFlow.map { preferences ->
        preferences[WATER_GOAL] ?: DEFAULT_WATER_GOAL
    }

    val waterLoggedFlow: Flow<Float> = currentPreferencesFlow.map { preferences ->
        preferences[WATER_LOGGED] ?: DEFAULT_WATER_LOGGED
    }

    val streakDaysFlow: Flow<Int> = currentPreferencesFlow.map { preferences ->
        preferences[STREAK_DAYS] ?: DEFAULT_STREAK_DAYS
    }

    val onboardingCompletedFlow: Flow<Boolean> = currentPreferencesFlow.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val userGmailFlow: Flow<String> = currentPreferencesFlow.map { preferences ->
        preferences[USER_GMAIL] ?: ""
    }

    val themeModeFlow: Flow<String> = _activeUserId.flatMapLatest { uid ->
        val targetStore = if (!uid.isNullOrBlank()) getDataStoreForUser(uid) else getDataStoreForUser("global_settings")
        targetStore.data.catch { emit(emptyPreferences()) }.map { it[USER_THEME_MODE] ?: DEFAULT_THEME_MODE }
    }

    suspend fun setThemeMode(mode: String) {
        val uid = _activeUserId.value
        val targetStore = if (!uid.isNullOrBlank()) getDataStoreForUser(uid) else getDataStoreForUser("global_settings")
        targetStore.edit { preferences ->
            preferences[USER_THEME_MODE] = mode
        }
    }

    private suspend fun editCurrent(transform: suspend (MutablePreferences) -> Unit) {
        val uid = _activeUserId.value ?: return
        getDataStoreForUser(uid).edit { transform(it) }
    }

    suspend fun saveDietaryPreference(preference: String) {
        editCurrent { preferences ->
            preferences[DIETARY_PREFERENCE] = preference
        }
    }

    suspend fun saveActivityLevel(level: String) {
        editCurrent { preferences ->
            preferences[ACTIVITY_LEVEL] = level
        }
    }

    suspend fun saveGoals(calories: Int, protein: Int, carbs: Int, fat: Int, water: Float, fiber: Int? = null) {
        editCurrent { preferences ->
            preferences[CALORIE_GOAL] = calories
            preferences[PROTEIN_GOAL] = protein
            preferences[CARBS_GOAL] = carbs
            preferences[FAT_GOAL] = fat
            preferences[WATER_GOAL] = water
            if (fiber != null) {
                preferences[FIBER_GOAL] = fiber
            }
        }
    }

    suspend fun addWater(amount: Float): Float {
        var updated = 0f
        editCurrent { preferences ->
            val current = preferences[WATER_LOGGED] ?: DEFAULT_WATER_LOGGED
            updated = (current + amount).coerceAtLeast(0f)
            preferences[WATER_LOGGED] = updated
        }
        return updated
    }

    suspend fun decreaseWater(amount: Float): Float {
        var updated = 0f
        editCurrent { preferences ->
            val current = preferences[WATER_LOGGED] ?: DEFAULT_WATER_LOGGED
            updated = (current - amount).coerceAtLeast(0f)
            preferences[WATER_LOGGED] = updated
        }
        return updated
    }

    suspend fun saveUserGmail(email: String) {
        editCurrent { preferences ->
            preferences[USER_GMAIL] = email.trim()
        }
    }

    suspend fun saveUserName(name: String) {
        editCurrent { preferences ->
            preferences[USER_NAME] = name.trim()
        }
    }

    suspend fun completeOnboarding(name: String, calories: Int, diet: String, activity: String) {
        editCurrent { preferences ->
            preferences[USER_NAME] = name.trim()
            preferences[CALORIE_GOAL] = calories
            preferences[DIETARY_PREFERENCE] = diet
            preferences[ACTIVITY_LEVEL] = activity
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun saveBodyStats(weightKg: Float, heightCm: Float, goal: String) {
        editCurrent { preferences ->
            preferences[USER_WEIGHT_KG] = weightKg
            preferences[USER_HEIGHT_CM] = heightCm
            preferences[USER_FITNESS_GOAL] = goal
        }
    }

    /**
     * Account Deletion / Local Wipe: Clears local preferences for a specific account.
     */
    suspend fun clearUserLocalData(userId: String) {
        val sanitized = userId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        try {
            dataStoreCache[sanitized]?.edit { it.clear() }
            val file = context.preferencesDataStoreFile("user_profile_$sanitized")
            if (file.exists()) {
                file.delete()
            }
            dataStoreCache.remove(sanitized)
        } catch (ignored: Exception) {}
        if (_activeUserId.value == userId) {
            _activeUserId.value = null
        }
    }
}
