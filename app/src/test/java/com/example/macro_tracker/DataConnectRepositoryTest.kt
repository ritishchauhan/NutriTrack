package com.example.macro_tracker

import com.example.macro_tracker.data.repository.DataConnectRepository
import com.example.macro_tracker.dataconnect.GetUserProfileQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataConnectRepositoryTest {

    @Test
    fun testDataConnectConnectionHealthy() = runTest {
        val fakeRepo = object : DataConnectRepository {
            override suspend fun checkServerConnectionTime(): Result<Long> {
                return Result.success(42L)
            }

            override suspend fun getUserProfile(userId: String): Result<GetUserProfileQuery.Data.UserProfile?> {
                return Result.success(null)
            }

            override suspend fun isConnectionHealthy(): Boolean {
                return checkServerConnectionTime().isSuccess
            }
        }

        assertTrue(fakeRepo.isConnectionHealthy())
        val latency = fakeRepo.checkServerConnectionTime().getOrNull()
        assertEquals(42L, latency)
    }

    @Test
    fun testDataConnectConnectionFailure() = runTest {
        val fakeRepo = object : DataConnectRepository {
            override suspend fun checkServerConnectionTime(): Result<Long> {
                return Result.failure(RuntimeException("Network error / Backend not deployed"))
            }

            override suspend fun getUserProfile(userId: String): Result<GetUserProfileQuery.Data.UserProfile?> {
                return Result.failure(RuntimeException("Network error"))
            }

            override suspend fun isConnectionHealthy(): Boolean {
                return checkServerConnectionTime().isSuccess
            }
        }

        assertFalse(fakeRepo.isConnectionHealthy())
        assertTrue(fakeRepo.checkServerConnectionTime().isFailure)
    }
}
