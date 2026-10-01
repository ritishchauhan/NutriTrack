package com.example.macro_tracker.data.repository

import android.util.Log
import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.local.UserProfileManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository interface for Cloud Firestore operations.
 * Backs up and synchronizes user profiles and full food tracking history with Firebase backend.
 */
interface FirestoreRepository {
    suspend fun saveUserProfile(userId: String, profileData: Map<String, Any>): Result<Unit>
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>?>
    suspend fun syncFoodLog(userId: String, foodLog: FoodLogEntity): Result<Unit>
    suspend fun deleteFoodLog(userId: String, logId: Int, timestamp: Long = 0L, foodName: String = ""): Result<Unit>
    suspend fun deleteFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long): Result<Unit>
    suspend fun clearUserFoodLogs(userId: String): Result<Unit>
    suspend fun getUserFoodLogs(userId: String): Result<List<FoodLogEntity>>
    suspend fun syncAllFoodLogsToFirestore(userId: String, foodLogs: List<FoodLogEntity>): Result<Unit>
    suspend fun syncRemoteHistoryToLocal(
        userId: String,
        foodLogDao: FoodLogDao,
        userProfileManager: UserProfileManager? = null
    ): Result<Unit>
    suspend fun deleteUserAccountData(userId: String): Result<Unit>
}

class FirestoreRepositoryImpl(
    firestoreInstance: FirebaseFirestore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FirestoreRepository {

    private val firestore: FirebaseFirestore by lazy {
        firestoreInstance ?: FirebaseFirestore.getInstance()
    }

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val USERS_COLLECTION = "users"
        private const val FOOD_LOGS_COLLECTION = "food_logs"

        fun getDocId(foodLog: FoodLogEntity): String {
            val nameHash = foodLog.foodName.trim().lowercase().hashCode()
            return "log_${foodLog.timestamp}_$nameHash"
        }
    }

    override suspend fun saveUserProfile(
        userId: String,
        profileData: Map<String, Any>
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val sanitized = HashMap<String, Any>(profileData)
            sanitized["uid"] = userId
            sanitized["updatedAt"] = System.currentTimeMillis()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(sanitized, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced user profile for $userId to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<Map<String, Any>?> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                Result.success(snapshot.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFoodLog(
        userId: String,
        foodLog: FoodLogEntity
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val docId = getDocId(foodLog)
            val data = hashMapOf(
                "id" to foodLog.id,
                "foodName" to foodLog.foodName,
                "calories" to foodLog.calories,
                "protein" to foodLog.protein.toDouble(),
                "carbs" to foodLog.carbs.toDouble(),
                "fat" to foodLog.fat.toDouble(),
                "fiber" to foodLog.fiber.toDouble(),
                "timestamp" to foodLog.timestamp,
                "mealType" to foodLog.mealType,
                "details" to foodLog.details,
                "userId" to userId
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced food log $docId to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing food log to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFoodLog(
        userId: String,
        logId: Int,
        timestamp: Long,
        foodName: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val logsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)

            // Try direct docId if timestamp and name are present
            if (timestamp > 0 && foodName.isNotBlank()) {
                val dummy = FoodLogEntity(
                    id = logId,
                    foodName = foodName,
                    calories = 0,
                    protein = 0f,
                    carbs = 0f,
                    fat = 0f,
                    timestamp = timestamp
                )
                logsRef.document(getDocId(dummy)).delete().await()
            }

            // Also delete by id attribute if exists
            val querySnapshot = logsRef.whereEqualTo("id", logId).get().await()
            for (doc in querySnapshot.documents) {
                doc.reference.delete().await()
            }

            Log.d(TAG, "Deleted food log ID $logId from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting food log from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun clearUserFoodLogs(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)
                .get()
                .await()

            snapshot.documents.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            Log.d(TAG, "Cleared all food logs in Firestore for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user food logs from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFoodLogsByDateRange(
        userId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val logsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)

            val snapshot = logsRef
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .get()
                .await()

            snapshot.documents.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            Log.d(TAG, "Deleted food logs in date range from Firestore for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting food logs in date range from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserFoodLogs(userId: String): Result<List<FoodLogEntity>> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = (doc.getLong("id") ?: 0L).toInt()
                    val foodName = doc.getString("foodName") ?: return@mapNotNull null
                    val calories = (doc.getLong("calories") ?: 0L).toInt()
                    val protein = (doc.getDouble("protein") ?: 0.0).toFloat()
                    val carbs = (doc.getDouble("carbs") ?: 0.0).toFloat()
                    val fat = (doc.getDouble("fat") ?: 0.0).toFloat()
                    val fiber = (doc.getDouble("fiber") ?: 0.0).toFloat()
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val mealType = doc.getString("mealType") ?: "Breakfast"
                    val details = doc.getString("details") ?: ""

                    FoodLogEntity(
                        id = id,
                        userId = userId,
                        foodName = foodName,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        fiber = fiber,
                        timestamp = timestamp,
                        mealType = mealType,
                        details = details
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user food logs from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncAllFoodLogsToFirestore(
        userId: String,
        foodLogs: List<FoodLogEntity>
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank() || foodLogs.isEmpty()) {
            return@withContext Result.success(Unit)
        }
        try {
            val logsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FOOD_LOGS_COLLECTION)

            // Firestore batch write supports up to 500 operations per batch
            foodLogs.chunked(450).forEach { chunk ->
                val batch: WriteBatch = firestore.batch()
                chunk.forEach { log ->
                    val docRef = logsRef.document(getDocId(log))
                    val data = hashMapOf(
                        "id" to log.id,
                        "foodName" to log.foodName,
                        "calories" to log.calories,
                        "protein" to log.protein.toDouble(),
                        "carbs" to log.carbs.toDouble(),
                        "fat" to log.fat.toDouble(),
                        "fiber" to log.fiber.toDouble(),
                        "timestamp" to log.timestamp,
                        "mealType" to log.mealType,
                        "details" to log.details,
                        "userId" to userId
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }
            Log.d(TAG, "Successfully synced ${foodLogs.size} logs to Firestore for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch syncing food logs to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncRemoteHistoryToLocal(
        userId: String,
        foodLogDao: FoodLogDao,
        userProfileManager: UserProfileManager?
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            // 1. Fetch & Restore Remote Profile
            val profileSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (profileSnapshot.exists() && userProfileManager != null) {
                val data = profileSnapshot.data ?: emptyMap()
                (data["displayName"] as? String)?.let { if (it.isNotBlank()) userProfileManager.saveUserName(it) }
                (data["email"] as? String)?.let { if (it.isNotBlank()) userProfileManager.saveUserGmail(it) }
                (data["dietaryPreference"] as? String)?.let { userProfileManager.saveDietaryPreference(it) }
                (data["activityLevel"] as? String)?.let { userProfileManager.saveActivityLevel(it) }

                val cal = (data["calorieGoal"] as? Number)?.toInt()
                val prot = (data["proteinGoal"] as? Number)?.toInt()
                val carbs = (data["carbsGoal"] as? Number)?.toInt()
                val fat = (data["fatGoal"] as? Number)?.toInt()
                val rawWater = (data["waterGoal"] as? Number)?.toFloat()
                val fiber = (data["fiberGoal"] as? Number)?.toInt()
                if (cal != null && prot != null && carbs != null && fat != null && rawWater != null) {
                    val water = if (rawWater > 30f) rawWater / 1000f else rawWater
                    userProfileManager.saveGoals(cal, prot, carbs, fat, water, fiber)
                }

                val weight = (data["userWeightKg"] as? Number)?.toFloat()
                val height = (data["userHeightCm"] as? Number)?.toFloat()
                val goal = (data["userFitnessGoal"] as? String)
                if (weight != null && height != null && goal != null) {
                    userProfileManager.saveBodyStats(weight, height, goal)
                }
            }

            // 2. Fetch Remote Food Logs History
            val remoteLogsResult = getUserFoodLogs(userId)
            val remoteLogs = remoteLogsResult.getOrNull() ?: emptyList()

            // 3. Fetch Local Food Logs for this userId ONLY
            val localLogs = foodLogDao.getAllFoodLogsList(userId)
            val localTimestamps = localLogs.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()

            // 4. Insert Missing Remote Logs into Local Database with userId
            val missingFromLocal = remoteLogs.filter { remote ->
                val key = "${remote.timestamp}_${remote.foodName.trim().lowercase()}"
                key !in localTimestamps
            }.map { it.copy(id = 0, userId = userId) } // let Room assign local auto IDs, strictly scoped to userId

            if (missingFromLocal.isNotEmpty()) {
                foodLogDao.insertAll(missingFromLocal)
                Log.d(TAG, "Restored ${missingFromLocal.size} remote food logs into local database for $userId")
            }

            // 5. Upload any local logs belonging to this user missing from remote to Firestore
            val remoteTimestamps = remoteLogs.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()
            val missingFromRemote = localLogs.filter { local ->
                local.userId == userId && "${local.timestamp}_${local.foodName.trim().lowercase()}" !in remoteTimestamps
            }
            if (missingFromRemote.isNotEmpty()) {
                syncAllFoodLogsToFirestore(userId, missingFromRemote)
                Log.d(TAG, "Uploaded ${missingFromRemote.size} offline food logs to Firestore for $userId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in bidirectional history synchronization", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUserAccountData(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            clearUserFoodLogs(userId)
            firestore.collection(USERS_COLLECTION).document(userId).delete().await()
            Log.d(TAG, "Deleted Firestore user account document for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user account data from Firestore", e)
            Result.failure(e)
        }
    }
}
