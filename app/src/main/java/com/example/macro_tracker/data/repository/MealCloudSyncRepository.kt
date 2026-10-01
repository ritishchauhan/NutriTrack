package com.example.macro_tracker.data.repository

import android.util.Log
import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.local.UserProfileManager
import com.example.macro_tracker.data.remote.neon.NeonApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository interface governing synchronization rules for tracked meals between local storage
 * and cloud storage.
 *
 * Implements strict account-based ownership:
 * - Data belongs to the account (User ID), never the device.
 * - Local storage/cache is isolated per User ID.
 * - Sign out ends session and clears active memory without deleting server data.
 * - Account deletion purges both cloud and local data.
 * - Cross-user data contamination and sync leaks are strictly prevented.
 */
interface MealCloudSyncRepository {
    /**
     * Saves a meal locally in the device's database for the authenticated user.
     */
    suspend fun saveMealLocally(meal: FoodLogEntity): Long

    /**
     * Synchronizes all account data (meals, macros, targets, body stats, preferences)
     * between the cloud backend (Neon Postgres & Firestore) and local isolated storage.
     */
    suspend fun syncAccountData(userId: String): Result<Unit>

    /**
     * Wipes local meal cache and profile preferences for a specific account on this device.
     * Does NOT delete server data.
     */
    suspend fun wipeLocalUserData(userId: String): Result<Unit>

    /**
     * Account Deletion Operation: Permanently deletes all user data from Neon Postgres,
     * Cloud Firestore, and local device storage.
     */
    suspend fun deleteAccountCloudAndLocalData(userId: String): Result<Unit>

    /**
     * Checks server round-trip connection time to the Neon Postgres cloud backend in milliseconds.
     */
    suspend fun checkServerConnectionTime(): Result<Long>
}

/**
 * Concrete implementation of [MealCloudSyncRepository] managing isolated Room DB and Neon Postgres
 * serverless backend (with Firestore dual-backup).
 */
class MealCloudSyncRepositoryImpl(
    private val foodLogDao: FoodLogDao,
    private val userProfileManager: UserProfileManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val neonApiClient: NeonApiClient = NeonApiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MealCloudSyncRepository {

    companion object {
        private const val TAG = "MealCloudSyncRepo"
    }

    private fun verifyAuthenticatedUser(targetUserId: String): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        return !currentUid.isNullOrBlank() && currentUid == targetUserId
    }

    override suspend fun saveMealLocally(meal: FoodLogEntity): Long = withContext(ioDispatcher) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Cannot save meal locally without an authenticated account.")
        val scopedMeal = meal.copy(userId = currentUid)
        foodLogDao.insertFoodLog(scopedMeal)
    }

    override suspend fun syncAccountData(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank() || userId.startsWith("guest_") || userId == "guest") {
            return@withContext Result.failure(IllegalArgumentException("Invalid User ID for sync: must be authenticated account"))
        }

        // Rule: Verify ownership using authenticated User ID
        if (!verifyAuthenticatedUser(userId)) {
            Log.w(TAG, "Sync aborted: Provided userId $userId does not match authenticated user")
            return@withContext Result.failure(SecurityException("Unauthorized: User ID mismatch"))
        }

        try {
            Log.d(TAG, "Starting account data sync for $userId from Neon Postgres & Firestore")

            // 1. Fetch remote meals from Neon for this user
            val neonMealsResult = neonApiClient.fetchMeals(userId)
            val remoteMeals = (neonMealsResult.getOrNull() ?: emptyList()).map { it.copy(userId = userId) }

            // 2. Fetch local meals strictly belonging to this user
            val localLogs = foodLogDao.getAllFoodLogsList(userId)
            val localLogTimestamps = localLogs.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()

            // 3. Insert any meals from Neon that aren't yet in local database for this user
            val mealsToInsert = remoteMeals.filter {
                "${it.timestamp}_${it.foodName.trim().lowercase()}" !in localLogTimestamps
            }
            if (mealsToInsert.isNotEmpty()) {
                foodLogDao.insertAll(mealsToInsert)
                Log.d(TAG, "Restored ${mealsToInsert.size} meals from Neon Postgres into local database for $userId")
            }

            // 4. Upload any local meals tagged with this userId that are missing from Neon
            val remoteTimestamps = remoteMeals.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()
            val localMealsToUpload = localLogs.filter {
                it.userId == userId && "${it.timestamp}_${it.foodName.trim().lowercase()}" !in remoteTimestamps
            }
            if (localMealsToUpload.isNotEmpty()) {
                neonApiClient.uploadMeals(userId, localMealsToUpload)
                Log.d(TAG, "Uploaded ${localMealsToUpload.size} offline meals to Neon Postgres for $userId")
            }

            // 5. Restore user profile goals and daily targets from Neon Postgres into the user's isolated profile
            val profileResult = neonApiClient.fetchUserProfile(userId)
            val profile = profileResult.getOrNull()
            if (profile != null) {
                val cal = (profile["calorieGoal"] as? Number)?.toInt() ?: 2000
                val pro = (profile["proteinGoal"] as? Number)?.toInt() ?: 150
                val carbs = (profile["carbsGoal"] as? Number)?.toInt() ?: 200
                val fat = (profile["fatGoal"] as? Number)?.toInt() ?: 65
                val fiber = (profile["fiberGoal"] as? Number)?.toInt() ?: 25
                val rawWater = (profile["waterGoal"] as? Number)?.toFloat() ?: 2.4f
                val water = if (rawWater > 30f) rawWater / 1000f else rawWater
                userProfileManager.saveGoals(cal, pro, carbs, fat, water, fiber)

                val name = profile["name"] as? String
                if (!name.isNullOrBlank()) {
                    userProfileManager.saveUserName(name)
                }

                val email = profile["email"] as? String
                if (!email.isNullOrBlank()) {
                    userProfileManager.saveUserGmail(email)
                }

                val weight = (profile["userWeightKg"] as? Number)?.toFloat() ?: 70.0f
                val height = (profile["userHeightCm"] as? Number)?.toFloat() ?: 170.0f
                val fitnessGoal = (profile["userFitnessGoal"] as? String) ?: "LOSE_WEIGHT"
                userProfileManager.saveBodyStats(weight, height, fitnessGoal)

                val diet = (profile["dietaryPreference"] as? String) ?: "Non-veg"
                userProfileManager.saveDietaryPreference(diet)

                val activity = (profile["activityLevel"] as? String) ?: "Sedentary"
                userProfileManager.saveActivityLevel(activity)

                Log.d(TAG, "Restored profile targets from Neon Postgres for $userId: cal=$cal, pro=$pro, carbs=$carbs, fat=$fat, fiber=$fiber, water=$water")
            } else {
                // If user doesn't have a profile in Neon yet, sync current local preferences to Neon
                val currentCal = userProfileManager.calorieGoalFlow.first()
                val currentPro = userProfileManager.proteinGoalFlow.first()
                val currentCarbs = userProfileManager.carbsGoalFlow.first()
                val currentFat = userProfileManager.fatGoalFlow.first()
                val currentFiber = userProfileManager.fiberGoalFlow.first()
                val currentWater = userProfileManager.waterGoalFlow.first()
                val currentName = userProfileManager.userNameFlow.first()
                val currentEmail = userProfileManager.userGmailFlow.first()
                val currentDiet = userProfileManager.dietaryPreferenceFlow.first()
                val currentActivity = userProfileManager.activityLevelFlow.first()
                val currentWeight = userProfileManager.userWeightKgFlow.first()
                val currentHeight = userProfileManager.userHeightCmFlow.first()
                val currentGoal = userProfileManager.userFitnessGoalFlow.first()

                neonApiClient.uploadUserProfile(
                    userId,
                    mapOf(
                        "name" to currentName,
                        "email" to currentEmail,
                        "calorieGoal" to currentCal,
                        "proteinGoal" to currentPro,
                        "carbsGoal" to currentCarbs,
                        "fatGoal" to currentFat,
                        "fiberGoal" to currentFiber,
                        "waterGoal" to currentWater.toDouble(),
                        "dietaryPreference" to currentDiet,
                        "activityLevel" to currentActivity,
                        "userWeightKg" to currentWeight.toDouble(),
                        "userHeightCm" to currentHeight.toDouble(),
                        "userFitnessGoal" to currentGoal
                    )
                )
                Log.d(TAG, "Initialized and synced local targets to Neon for $userId")
            }

            // 6. Also sync with Firestore as backup
            try {
                firestoreRepository.syncRemoteHistoryToLocal(userId, foodLogDao, userProfileManager)
            } catch (e: Exception) {
                Log.w(TAG, "Secondary Firestore sync note: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync account data for $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun wipeLocalUserData(userId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            foodLogDao.deleteAllFoodLogsForUser(userId)
            userProfileManager.clearUserLocalData(userId)
            Log.d(TAG, "Local data for user $userId wiped successfully from device (server data preserved)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe local data for user $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccountCloudAndLocalData(userId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Executing permanent account deletion for $userId")
            // 1. Purge server data from Neon
            neonApiClient.deleteAccountData(userId)
            // 2. Purge server data from Firestore
            firestoreRepository.deleteUserAccountData(userId)
            // 3. Purge local meals for this user
            foodLogDao.deleteAllFoodLogsForUser(userId)
            // 4. Purge local profile preferences
            userProfileManager.clearUserLocalData(userId)

            Log.d(TAG, "Account deletion complete: All cloud and local data permanently deleted for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account data for $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun checkServerConnectionTime(): Result<Long> = withContext(ioDispatcher) {
        neonApiClient.checkServerConnectionTime()
    }
}
