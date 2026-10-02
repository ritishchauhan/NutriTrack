package com.example.macro_tracker.data.repository

import com.example.macro_tracker.data.local.UserProfileManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing user preferences, macro targets, and profile settings.
 * Strictly scoped to the currently authenticated account.
 */
interface UserRepository {
    val userName: Flow<String>
    val dietaryPreference: Flow<String>
    val activityLevel: Flow<String>
    val calorieGoal: Flow<Int>
    val proteinGoal: Flow<Int>
    val carbsGoal: Flow<Int>
    val fatGoal: Flow<Int>
    val fiberGoal: Flow<Int>
    val waterGoal: Flow<Float>
    val waterLogged: Flow<Float>
    val streakDays: Flow<Int>
    val onboardingCompleted: Flow<Boolean>
    val userGmail: Flow<String>
    val userWeightKg: Flow<Float>
    val userHeightCm: Flow<Float>
    val userFitnessGoal: Flow<String>
    val themeMode: Flow<String>

    fun setActiveUser(userId: String?)
    fun clearActiveSession()

    suspend fun saveUserGmail(email: String)
    suspend fun saveUserName(name: String)
    suspend fun saveDietaryPreference(preference: String)
    suspend fun saveActivityLevel(level: String)
    suspend fun saveGoals(calories: Int, protein: Int, carbs: Int, fat: Int, water: Float, fiber: Int? = null)
    suspend fun saveBodyStats(weightKg: Float, heightCm: Float, goal: String)
    suspend fun addWater(amount: Float)
    suspend fun decreaseWater(amount: Float)
    suspend fun completeOnboarding(name: String, calories: Int, diet: String, activity: String)
    suspend fun setThemeMode(mode: String)
}

/**
 * Concrete implementation of [UserRepository] delegating to [UserProfileManager]
 * and synchronizing user preferences and goals with Cloud Firestore and Firebase SQL Connect.
 */
class UserRepositoryImpl(
    private val userProfileManager: UserProfileManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val userName: Flow<String> = userProfileManager.userNameFlow
    override val dietaryPreference: Flow<String> = userProfileManager.dietaryPreferenceFlow
    override val activityLevel: Flow<String> = userProfileManager.activityLevelFlow
    override val calorieGoal: Flow<Int> = userProfileManager.calorieGoalFlow
    override val proteinGoal: Flow<Int> = userProfileManager.proteinGoalFlow
    override val carbsGoal: Flow<Int> = userProfileManager.carbsGoalFlow
    override val fatGoal: Flow<Int> = userProfileManager.fatGoalFlow
    override val fiberGoal: Flow<Int> = userProfileManager.fiberGoalFlow
    override val waterGoal: Flow<Float> = userProfileManager.waterGoalFlow
    override val waterLogged: Flow<Float> = userProfileManager.waterLoggedFlow
    override val streakDays: Flow<Int> = userProfileManager.streakDaysFlow
    override val onboardingCompleted: Flow<Boolean> = userProfileManager.onboardingCompletedFlow
    override val userGmail: Flow<String> = userProfileManager.userGmailFlow
    override val userWeightKg: Flow<Float> = userProfileManager.userWeightKgFlow
    override val userHeightCm: Flow<Float> = userProfileManager.userHeightCmFlow
    override val userFitnessGoal: Flow<String> = userProfileManager.userFitnessGoalFlow
    override val themeMode: Flow<String> = userProfileManager.themeModeFlow

    override fun setActiveUser(userId: String?) {
        userProfileManager.setActiveUser(userId)
    }

    override fun clearActiveSession() {
        userProfileManager.clearActiveSession()
    }

    private fun syncToCloud(fieldMap: Map<String, Any>) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.isAnonymous || user.uid.startsWith("guest_") || user.uid == "guest") return
        val uid = user.uid
        repositoryScope.launch {
            try {
                firestoreRepository.saveUserProfile(uid, fieldMap)
            } catch (ignored: Exception) {}
        }
    }

    override suspend fun saveUserGmail(email: String) = withContext(ioDispatcher) {
        userProfileManager.saveUserGmail(email)
        syncToCloud(mapOf("email" to email))
    }

    override suspend fun saveUserName(name: String) = withContext(ioDispatcher) {
        userProfileManager.saveUserName(name)
        syncToCloud(mapOf("displayName" to name, "name" to name))
    }

    override suspend fun saveDietaryPreference(preference: String) = withContext(ioDispatcher) {
        userProfileManager.saveDietaryPreference(preference)
        syncToCloud(mapOf("dietaryPreference" to preference))
    }

    override suspend fun saveActivityLevel(level: String) = withContext(ioDispatcher) {
        userProfileManager.saveActivityLevel(level)
        syncToCloud(mapOf("activityLevel" to level))
    }

    override suspend fun saveGoals(
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        water: Float,
        fiber: Int?
    ) = withContext(ioDispatcher) {
        userProfileManager.saveGoals(calories, protein, carbs, fat, water, fiber)
        val fieldMap = mutableMapOf<String, Any>(
            "calorieGoal" to calories,
            "proteinGoal" to protein,
            "carbsGoal" to carbs,
            "fatGoal" to fat,
            "waterGoal" to water.toDouble()
        )
        if (fiber != null) {
            fieldMap["fiberGoal"] = fiber
        }
        syncToCloud(fieldMap)
    }

    override suspend fun saveBodyStats(
        weightKg: Float,
        heightCm: Float,
        goal: String
    ) = withContext(ioDispatcher) {
        userProfileManager.saveBodyStats(weightKg, heightCm, goal)
        syncToCloud(
            mapOf(
                "userWeightKg" to weightKg.toDouble(),
                "userHeightCm" to heightCm.toDouble(),
                "userFitnessGoal" to goal
            )
        )
    }

    override suspend fun addWater(amount: Float) = withContext(ioDispatcher) {
        val updated = userProfileManager.addWater(amount)
        syncToCloud(mapOf("waterLogged" to updated.toDouble()))
    }

    override suspend fun decreaseWater(amount: Float) = withContext(ioDispatcher) {
        val updated = userProfileManager.decreaseWater(amount)
        syncToCloud(mapOf("waterLogged" to updated.toDouble()))
    }

    override suspend fun completeOnboarding(
        name: String,
        calories: Int,
        diet: String,
        activity: String
    ) = withContext(ioDispatcher) {
        userProfileManager.completeOnboarding(name, calories, diet, activity)
        syncToCloud(
            mapOf(
                "displayName" to name,
                "name" to name,
                "calorieGoal" to calories,
                "dietaryPreference" to diet,
                "activityLevel" to activity
            )
        )
    }

    override suspend fun setThemeMode(mode: String) = withContext(ioDispatcher) {
        userProfileManager.setThemeMode(mode)
        syncToCloud(mapOf("themeMode" to mode))
    }
}
