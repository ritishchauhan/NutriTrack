package com.example.macro_tracker.data.repository

import android.util.Log
import com.example.macro_tracker.dataconnect.NutritrackConnectorConnector
import com.example.macro_tracker.dataconnect.execute
import com.example.macro_tracker.dataconnect.instance
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface for interacting with Firebase SQL Connect (Data Connect).
 * Provides backend connectivity testing, user profile queries, and status checks.
 */
interface DataConnectRepository {
    /**
     * Checks server latency by querying the Firebase SQL Connect endpoint.
     */
    suspend fun checkServerConnectionTime(): Result<Long>

    /**
     * Fetches user profile from Firebase SQL Connect.
     */
    suspend fun getUserProfile(userId: String): Result<com.example.macro_tracker.dataconnect.GetUserProfileQuery.Data.UserProfile?>

    /**
     * Returns true if Firebase SQL Connect backend responds successfully.
     */
    suspend fun isConnectionHealthy(): Boolean
}

class DataConnectRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DataConnectRepository {

    private val connector: NutritrackConnectorConnector by lazy {
        NutritrackConnectorConnector.instance
    }

    override suspend fun checkServerConnectionTime(): Result<Long> = withContext(ioDispatcher) {
        try {
            val start = System.currentTimeMillis()
            val dummyId = "connection_check"
            connector.getUserProfile.execute(userId = dummyId)
            val duration = System.currentTimeMillis() - start
            Result.success(duration)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase SQL Connect response/notice: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<com.example.macro_tracker.dataconnect.GetUserProfileQuery.Data.UserProfile?> = withContext(ioDispatcher) {
        try {
            val response = connector.getUserProfile.execute(userId = userId)
            Result.success(response.data.userProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from Firebase SQL Connect", e)
            Result.failure(e)
        }
    }

    override suspend fun isConnectionHealthy(): Boolean = withContext(ioDispatcher) {
        checkServerConnectionTime().isSuccess
    }

    companion object {
        private const val TAG = "DataConnectRepository"
    }
}
