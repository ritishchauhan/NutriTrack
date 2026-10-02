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
    suspend fun syncWeightLog(userId: String, weightLog: com.example.macro_tracker.data.local.WeightLogEntity): Result<Unit>
    suspend fun deleteWeightLog(userId: String, weightLog: com.example.macro_tracker.data.local.WeightLogEntity): Result<Unit>
    suspend fun getUserWeightLogs(userId: String): Result<List<com.example.macro_tracker.data.local.WeightLogEntity>>
    suspend fun syncAllWeightLogsToFirestore(userId: String, weightLogs: List<com.example.macro_tracker.data.local.WeightLogEntity>): Result<Unit>
    suspend fun syncActivityMetric(userId: String, recordType: String, value: Double, date: String): Result<Unit>
    suspend fun syncHydrationLog(userId: String, amountLiters: Double, timestamp: Long): Result<Unit>
    suspend fun savePreferenceSettings(userId: String, settings: Map<String, Any>): Result<Unit>
    suspend fun deleteUserAccountData(userId: String): Result<Unit>
    suspend fun checkServerConnectionTime(): Result<Long>
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
            val sanitized = HashMap<String, Any>()
            sanitized["userId"] = userId
            (profileData["name"] ?: profileData["displayName"])?.let { sanitized["name"] = it.toString() }
            (profileData["email"])?.let { sanitized["email"] = it.toString() }

            (profileData["calorieTarget"] ?: profileData["calorieGoal"])?.let { sanitized["calorieTarget"] = (it as Number).toInt() }
            (profileData["proteinTarget"] ?: profileData["proteinGoal"])?.let { sanitized["proteinTarget"] = (it as Number).toInt() }
            (profileData["carbsTarget"] ?: profileData["carbsGoal"])?.let { sanitized["carbsTarget"] = (it as Number).toInt() }
            (profileData["fatTarget"] ?: profileData["fatGoal"])?.let { sanitized["fatTarget"] = (it as Number).toInt() }
            (profileData["fiberTarget"] ?: profileData["fiberGoal"])?.let { sanitized["fiberTarget"] = (it as Number).toInt() }
            (profileData["waterTarget"] ?: profileData["waterGoal"])?.let { sanitized["waterTarget"] = (it as Number).toDouble() }

            profileData["waterLogged"]?.let { sanitized["waterLogged"] = (it as Number).toDouble() }
            profileData["streakDays"]?.let { sanitized["streakDays"] = (it as Number).toInt() }
            (profileData["weightKg"] ?: profileData["userWeightKg"])?.let { sanitized["weightKg"] = (it as Number).toDouble() }
            (profileData["heightCm"] ?: profileData["userHeightCm"])?.let { sanitized["heightCm"] = (it as Number).toDouble() }
            (profileData["fitnessGoal"] ?: profileData["userFitnessGoal"])?.let { sanitized["fitnessGoal"] = it.toString() }
            profileData["dietaryPreference"]?.let { sanitized["dietaryPreference"] = it.toString() }
            profileData["activityLevel"]?.let { sanitized["activityLevel"] = it.toString() }
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
                "calories" to foodLog.calories.toDouble(),
                "protein" to foodLog.protein.toDouble(),
                "carbs" to foodLog.carbs.toDouble(),
                "fat" to foodLog.fat.toDouble(),
                "fiber" to foodLog.fiber.toDouble(),
                "portionMultiplier" to foodLog.servings.toDouble(),
                "servings" to foodLog.servings,
                "mealType" to foodLog.mealType,
                "barcode" to "",
                "imageUrl" to "",
                "timestamp" to foodLog.timestamp,
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
                    val servings = (doc.getLong("servings") ?: 1L).toInt().coerceAtLeast(1)
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
                        servings = servings,
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
                        "calories" to log.calories.toDouble(),
                        "protein" to log.protein.toDouble(),
                        "carbs" to log.carbs.toDouble(),
                        "fat" to log.fat.toDouble(),
                        "fiber" to log.fiber.toDouble(),
                        "portionMultiplier" to log.servings.toDouble(),
                        "servings" to log.servings,
                        "mealType" to log.mealType,
                        "barcode" to "",
                        "imageUrl" to "",
                        "timestamp" to log.timestamp,
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
                val name = (data["name"] ?: data["displayName"]) as? String
                if (!name.isNullOrBlank()) userProfileManager.saveUserName(name)

                (data["email"] as? String)?.let { if (it.isNotBlank()) userProfileManager.saveUserGmail(it) }
                (data["dietaryPreference"] as? String)?.let { userProfileManager.saveDietaryPreference(it) }
                (data["activityLevel"] as? String)?.let { userProfileManager.saveActivityLevel(it) }

                val cal = (data["calorieTarget"] ?: data["calorieGoal"]) as? Number
                val prot = (data["proteinTarget"] ?: data["proteinGoal"]) as? Number
                val carbs = (data["carbsTarget"] ?: data["carbsGoal"]) as? Number
                val fat = (data["fatTarget"] ?: data["fatGoal"]) as? Number
                val rawWater = (data["waterTarget"] ?: data["waterGoal"]) as? Number
                val fiber = (data["fiberTarget"] ?: data["fiberGoal"]) as? Number
                if (cal != null && prot != null && carbs != null && fat != null && rawWater != null) {
                    val rawWaterFloat = rawWater.toFloat()
                    val water = if (rawWaterFloat > 30f) rawWaterFloat / 1000f else rawWaterFloat
                    userProfileManager.saveGoals(cal.toInt(), prot.toInt(), carbs.toInt(), fat.toInt(), water, fiber?.toInt() ?: 25)
                }

                val weight = (data["weightKg"] ?: data["userWeightKg"]) as? Number
                val height = (data["heightCm"] ?: data["userHeightCm"]) as? Number
                val goal = (data["fitnessGoal"] ?: data["userFitnessGoal"]) as? String
                if (weight != null && height != null && goal != null) {
                    userProfileManager.saveBodyStats(weight.toFloat(), height.toFloat(), goal)
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

    override suspend fun syncWeightLog(
        userId: String,
        weightLog: com.example.macro_tracker.data.local.WeightLogEntity
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val docId = "wlog_${weightLog.timestamp}"
            val data = hashMapOf(
                "userId" to userId,
                "weightKg" to weightLog.weightKg.toDouble(),
                "timestamp" to weightLog.timestamp,
                "note" to weightLog.note
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("weight_logs")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing weight log to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteWeightLog(
        userId: String,
        weightLog: com.example.macro_tracker.data.local.WeightLogEntity
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val docId = "wlog_${weightLog.timestamp}"
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("weight_logs")
                .document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting weight log from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserWeightLogs(
        userId: String
    ): Result<List<com.example.macro_tracker.data.local.WeightLogEntity>> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("weight_logs")
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val weightKg = (doc.getDouble("weightKg") ?: 70.0).toFloat()
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    com.example.macro_tracker.data.local.WeightLogEntity(
                        userId = userId,
                        weightKg = weightKg,
                        timestamp = timestamp,
                        note = note
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user weight logs from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncAllWeightLogsToFirestore(
        userId: String,
        weightLogs: List<com.example.macro_tracker.data.local.WeightLogEntity>
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank() || weightLogs.isEmpty()) return@withContext Result.success(Unit)
        try {
            val logsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("weight_logs")

            weightLogs.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { log ->
                    val docRef = logsRef.document("wlog_${log.timestamp}")
                    val data = hashMapOf(
                        "userId" to userId,
                        "weightKg" to log.weightKg.toDouble(),
                        "timestamp" to log.timestamp,
                        "note" to log.note
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch syncing weight logs to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncActivityMetric(
        userId: String,
        recordType: String,
        value: Double,
        date: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val docId = "metric_${recordType}_$date"
            val data = hashMapOf(
                "userId" to userId,
                "recordType" to recordType,
                "value" to value,
                "date" to date,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("activity_metrics")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncHydrationLog(
        userId: String,
        amountLiters: Double,
        timestamp: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val docId = "water_$timestamp"
            val data = hashMapOf(
                "userId" to userId,
                "amountLiters" to amountLiters,
                "timestamp" to timestamp
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("hydration_logs")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePreferenceSettings(
        userId: String,
        settings: Map<String, Any>
    ): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        try {
            val data = HashMap(settings)
            data["userId"] = userId
            data["updatedAt"] = System.currentTimeMillis()
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("preference_settings")
                .document("current")
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserAccountData(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        try {
            clearUserFoodLogs(userId)
            val subcollections = listOf("weight_logs", "activity_metrics", "hydration_logs", "preference_settings")
            for (sub in subcollections) {
                try {
                    val snap = firestore.collection(USERS_COLLECTION).document(userId).collection(sub).get().await()
                    snap.documents.chunked(450).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                } catch (ignored: Exception) {}
            }
            firestore.collection(USERS_COLLECTION).document(userId).delete().await()
            Log.d(TAG, "Deleted Firestore user account document for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user account data from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun checkServerConnectionTime(): Result<Long> = withContext(ioDispatcher) {
        try {
            val start = System.currentTimeMillis()
            firestore.collection(USERS_COLLECTION).limit(1).get().await()
            val duration = System.currentTimeMillis() - start
            Result.success(duration)
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Firestore connection latency", e)
            Result.failure(e)
        }
    }
}

