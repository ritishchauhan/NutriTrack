package com.example.macro_tracker.data.repository

import com.example.macro_tracker.data.local.UserProfileManager
import com.example.macro_tracker.data.local.WeightLogDao
import com.example.macro_tracker.data.local.WeightLogEntity
import com.example.macro_tracker.data.remote.neon.NeonApiClient
import com.example.macro_tracker.util.WeightTrendCalculator
import com.example.macro_tracker.util.WeightTrendSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface WeightRepository {
    val weightLogs: Flow<List<WeightLogEntity>>
    val latestWeightLog: Flow<WeightLogEntity?>
    val weightTrendSummary: Flow<WeightTrendSummary>

    suspend fun logWeight(weightKg: Float, note: String = ""): Result<WeightLogEntity>
    suspend fun updateWeightLog(weightLog: WeightLogEntity): Result<Unit>
    suspend fun deleteWeightLog(weightLog: WeightLogEntity): Result<Unit>
    suspend fun clearAllWeightLogs(): Result<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeightRepositoryImpl(
    private val weightLogDao: WeightLogDao,
    private val userProfileManager: UserProfileManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val neonApiClient: NeonApiClient = NeonApiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeightRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private fun getAuthenticatedUserId(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return if (user != null && !user.isAnonymous && !user.uid.startsWith("guest_") && user.uid != "guest") {
            user.uid
        } else {
            null
        }
    }

    override val weightLogs: Flow<List<WeightLogEntity>> = userProfileManager.activeUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            weightLogDao.getWeightLogsForUser(uid)
        }
    }

    override val latestWeightLog: Flow<WeightLogEntity?> = userProfileManager.activeUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(null)
        } else {
            weightLogDao.getLatestWeightLog(uid)
        }
    }

    override val weightTrendSummary: Flow<WeightTrendSummary> = weightLogs.map { logs ->
        WeightTrendCalculator.calculateTrend(logs)
    }

    override suspend fun logWeight(weightKg: Float, note: String): Result<WeightLogEntity> = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext Result.failure(IllegalStateException("No user logged in"))
        val entity = WeightLogEntity(
            userId = uid,
            weightKg = weightKg,
            timestamp = System.currentTimeMillis(),
            note = note.trim()
        )
        val generatedId = weightLogDao.insert(entity)
        val inserted = entity.copy(id = generatedId)

        // Also update the user's active profile weight for dynamic calorie targets
        try {
            val currentHeight = 170f
            userProfileManager.saveBodyStats(weightKg, currentHeight, "MAINTAIN")
        } catch (ignored: Exception) {}

        repositoryScope.launch {
            try {
                neonApiClient.uploadUserProfile(uid, mapOf("weightKg" to weightKg.toDouble()))
            } catch (ignored: Exception) {}
            try {
                firestoreRepository.saveUserProfile(uid, mapOf("weightKg" to weightKg.toDouble()))
            } catch (ignored: Exception) {}
        }

        Result.success(inserted)
    }

    override suspend fun updateWeightLog(weightLog: WeightLogEntity): Result<Unit> = withContext(ioDispatcher) {
        weightLogDao.update(weightLog)
        val uid = getAuthenticatedUserId()
        if (uid != null) {
            try {
                userProfileManager.saveBodyStats(weightLog.weightKg, 170f, "MAINTAIN")
            } catch (ignored: Exception) {}
            repositoryScope.launch {
                try {
                    neonApiClient.uploadUserProfile(uid, mapOf("weightKg" to weightLog.weightKg.toDouble()))
                } catch (ignored: Exception) {}
                try {
                    firestoreRepository.saveUserProfile(uid, mapOf("weightKg" to weightLog.weightKg.toDouble()))
                } catch (ignored: Exception) {}
            }
        }
        Result.success(Unit)
    }

    override suspend fun deleteWeightLog(weightLog: WeightLogEntity): Result<Unit> = withContext(ioDispatcher) {
        weightLogDao.delete(weightLog)
        Result.success(Unit)
    }

    override suspend fun clearAllWeightLogs(): Result<Unit> = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext Result.failure(IllegalStateException("No user logged in"))
        weightLogDao.deleteAllForUser(uid)
        Result.success(Unit)
    }
}
