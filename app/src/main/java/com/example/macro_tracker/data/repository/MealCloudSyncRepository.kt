package com.example.macro_tracker.data.repository

import android.util.Log
import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.local.UserProfileManager
import com.example.macro_tracker.data.remote.NeonMigrationHelper
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
     * between Cloud Firestore and local isolated storage.
     */
    suspend fun syncAccountData(userId: String): Result<Unit>

    /**
     * Wipes local meal cache and profile preferences for a specific account on this device.
     * Does NOT delete server data.
     */
    suspend fun wipeLocalUserData(userId: String): Result<Unit>

    /**
     * Account Deletion Operation: Permanently deletes all user data from Cloud Firestore
     * and local device storage.
     */
    suspend fun deleteAccountCloudAndLocalData(userId: String): Result<Unit>

    /**
     * Checks server round-trip connection time to Cloud Firestore in milliseconds.
     */
    suspend fun checkServerConnectionTime(): Result<Long>
}

/**
 * Concrete implementation of [MealCloudSyncRepository] managing isolated Room DB and Cloud Firestore.
 */
class MealCloudSyncRepositoryImpl(
    private val foodLogDao: FoodLogDao,
    private val userProfileManager: UserProfileManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
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
            Log.d(TAG, "Starting account data sync for $userId with Cloud Firestore")

            // 1. Migrate legacy data if this user has pre-existing Neon data needing initial Firestore migration
            NeonMigrationHelper.migrateIfNecessary(userId, firestoreRepository)

            // 2. Synchronize Firestore remote history with local database & user profile
            firestoreRepository.syncRemoteHistoryToLocal(userId, foodLogDao, userProfileManager)

            // 3. Ensure local profile preferences are pushed to Firestore if not yet stored
            val remoteProfile = firestoreRepository.getUserProfile(userId).getOrNull()
            if (remoteProfile == null || !remoteProfile.containsKey("calorieTarget")) {
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

                firestoreRepository.saveUserProfile(
                    userId,
                    mapOf(
                        "name" to currentName,
                        "email" to currentEmail,
                        "calorieTarget" to currentCal,
                        "proteinTarget" to currentPro,
                        "carbsTarget" to currentCarbs,
                        "fatTarget" to currentFat,
                        "fiberTarget" to currentFiber,
                        "waterTarget" to currentWater.toDouble(),
                        "dietaryPreference" to currentDiet,
                        "activityLevel" to currentActivity,
                        "weightKg" to currentWeight.toDouble(),
                        "heightCm" to currentHeight.toDouble(),
                        "fitnessGoal" to currentGoal
                    )
                )
                Log.d(TAG, "Initialized and synced local targets to Firestore for $userId")
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
            // 1. Purge server data from Firestore
            firestoreRepository.deleteUserAccountData(userId)
            // 2. Purge local meals for this user
            foodLogDao.deleteAllFoodLogsForUser(userId)
            // 3. Purge local profile preferences
            userProfileManager.clearUserLocalData(userId)

            Log.d(TAG, "Account deletion complete: All cloud and local data permanently deleted for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account data for $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun checkServerConnectionTime(): Result<Long> = withContext(ioDispatcher) {
        firestoreRepository.checkServerConnectionTime()
    }
}
