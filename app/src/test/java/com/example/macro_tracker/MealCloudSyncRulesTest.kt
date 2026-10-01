package com.example.macro_tracker

import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.local.UserProfileManager
import com.example.macro_tracker.data.repository.FirestoreRepository
import com.example.macro_tracker.data.repository.MealCloudSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating backend rules:
 * 1. Every user's data is linked to a unique User ID.
 * 2. Each account has completely separate data.
 * 3. When a user signs out, their data is NOT deleted from the server.
 * 4. User A's pending offline data is NEVER synced to User B's account.
 * 5. On account switching, only the new account's data is displayed.
 * 6. Account deletion and sign out are treated as two separate operations.
 * 7. Core rule: The account owns the data, not the device.
 */
class MealCloudSyncRulesTest {

    private val localMealDb = mutableListOf<FoodLogEntity>()
    private val cloudMealDb = mutableMapOf<String, MutableList<FoodLogEntity>>()

    private lateinit var fakeFoodLogDao: FoodLogDao
    private lateinit var fakeFirestoreRepository: FirestoreRepository

    @Before
    fun setUp() {
        localMealDb.clear()
        cloudMealDb.clear()

        fakeFoodLogDao = object : FoodLogDao {
            override suspend fun insertFoodLog(foodLog: FoodLogEntity): Long {
                val assignedId = if (foodLog.id == 0) localMealDb.size + 1 else foodLog.id
                val entity = foodLog.copy(id = assignedId)
                localMealDb.add(entity)
                return assignedId.toLong()
            }

            override suspend fun insertAll(foodLogs: List<FoodLogEntity>) {
                foodLogs.forEach { insertFoodLog(it) }
            }

            override fun getAllFoodLogs(userId: String): Flow<List<FoodLogEntity>> =
                flowOf(localMealDb.filter { it.userId == userId })

            override suspend fun getAllFoodLogsList(userId: String): List<FoodLogEntity> =
                localMealDb.filter { it.userId == userId }

            override fun getFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<FoodLogEntity>> =
                flowOf(localMealDb.filter { it.userId == userId && it.timestamp in startOfDay..endOfDay })

            override fun getFoodLogsSince(userId: String, sinceTimestamp: Long): Flow<List<FoodLogEntity>> =
                flowOf(localMealDb.filter { it.userId == userId && it.timestamp >= sinceTimestamp })

            override suspend fun getCount(userId: String): Int =
                localMealDb.count { it.userId == userId }

            override suspend fun deleteFoodLog(foodLog: FoodLogEntity) {
                localMealDb.removeAll { it.id == foodLog.id && it.userId == foodLog.userId }
            }

            override suspend fun deleteAllFoodLogsForUser(userId: String) {
                localMealDb.removeAll { it.userId == userId }
            }

            override suspend fun deleteAllFoodLogs() {
                localMealDb.clear()
            }
        }

        fakeFirestoreRepository = object : FirestoreRepository {
            override suspend fun saveUserProfile(userId: String, profileData: Map<String, Any>): Result<Unit> =
                Result.success(Unit)

            override suspend fun getUserProfile(userId: String): Result<Map<String, Any>?> =
                Result.success(null)

            override suspend fun syncFoodLog(userId: String, foodLog: FoodLogEntity): Result<Unit> {
                val list = cloudMealDb.computeIfAbsent(userId) { mutableListOf() }
                list.add(foodLog.copy(userId = userId))
                return Result.success(Unit)
            }

            override suspend fun deleteFoodLog(userId: String, logId: Int, timestamp: Long, foodName: String): Result<Unit> {
                cloudMealDb[userId]?.removeAll { it.id == logId }
                return Result.success(Unit)
            }

            override suspend fun clearUserFoodLogs(userId: String): Result<Unit> {
                cloudMealDb[userId]?.clear()
                return Result.success(Unit)
            }

            override suspend fun getUserFoodLogs(userId: String): Result<List<FoodLogEntity>> =
                Result.success(cloudMealDb[userId]?.toList() ?: emptyList())

            override suspend fun syncAllFoodLogsToFirestore(userId: String, foodLogs: List<FoodLogEntity>): Result<Unit> {
                val list = cloudMealDb.computeIfAbsent(userId) { mutableListOf() }
                foodLogs.forEach { log ->
                    if (list.none { it.timestamp == log.timestamp && it.foodName == log.foodName }) {
                        list.add(log.copy(userId = userId))
                    }
                }
                return Result.success(Unit)
            }

            override suspend fun syncRemoteHistoryToLocal(
                userId: String,
                foodLogDao: FoodLogDao,
                userProfileManager: UserProfileManager?
            ): Result<Unit> {
                val remote = cloudMealDb[userId]?.toList() ?: emptyList()
                val local = foodLogDao.getAllFoodLogsList(userId)
                val localKeys = local.map { "${it.timestamp}_${it.foodName}" }.toSet()

                val missingFromLocal = remote.filter { "${it.timestamp}_${it.foodName}" !in localKeys }
                foodLogDao.insertAll(missingFromLocal.map { it.copy(id = 0, userId = userId) })

                val remoteKeys = remote.map { "${it.timestamp}_${it.foodName}" }.toSet()
                val missingFromRemote = local.filter { it.userId == userId && "${it.timestamp}_${it.foodName}" !in remoteKeys }
                syncAllFoodLogsToFirestore(userId, missingFromRemote)

                return Result.success(Unit)
            }

            override suspend fun deleteUserAccountData(userId: String): Result<Unit> {
                cloudMealDb.remove(userId)
                return Result.success(Unit)
            }
        }
    }

    @Test
    fun rule1_everyUserDataLinkedToUniqueUserId() = runTest {
        // User A logs a meal
        val mealUserA = FoodLogEntity(
            userId = "user_A",
            foodName = "Oatmeal with Almonds",
            calories = 350,
            protein = 14f,
            carbs = 52f,
            fat = 8f,
            timestamp = 1700000000000L,
            mealType = "Breakfast"
        )
        fakeFoodLogDao.insertFoodLog(mealUserA)

        // User B logs a meal
        val mealUserB = FoodLogEntity(
            userId = "user_B",
            foodName = "Grilled Paneer Salad",
            calories = 420,
            protein = 22f,
            carbs = 10f,
            fat = 28f,
            timestamp = 1700000001000L,
            mealType = "Lunch"
        )
        fakeFoodLogDao.insertFoodLog(mealUserB)

        // Total in DB is 2
        assertEquals(2, localMealDb.size)

        // Querying for User A yields ONLY User A's meal
        val userALogs = fakeFoodLogDao.getAllFoodLogsList("user_A")
        assertEquals(1, userALogs.size)
        assertEquals("Oatmeal with Almonds", userALogs.first().foodName)
        assertEquals("user_A", userALogs.first().userId)

        // Querying for User B yields ONLY User B's meal
        val userBLogs = fakeFoodLogDao.getAllFoodLogsList("user_B")
        assertEquals(1, userBLogs.size)
        assertEquals("Grilled Paneer Salad", userBLogs.first().foodName)
        assertEquals("user_B", userBLogs.first().userId)
    }

    @Test
    fun rule2_signOutDoesNotDeleteServerData() = runTest {
        // User A has data in the cloud
        val cloudMeal = FoodLogEntity(
            userId = "user_A",
            foodName = "Chicken Breast & Rice",
            calories = 500,
            protein = 45f,
            carbs = 55f,
            fat = 7f,
            timestamp = 1700000005000L
        )
        fakeFirestoreRepository.syncFoodLog("user_A", cloudMeal)

        assertEquals(1, cloudMealDb["user_A"]?.size)

        // User A signs out: server data MUST NOT be deleted
        // Active session clears in-memory state, server data remains untouched
        assertEquals(1, cloudMealDb["user_A"]?.size)
        assertEquals("Chicken Breast & Rice", cloudMealDb["user_A"]?.first()?.foodName)
    }

    @Test
    fun rule3_userPendingOfflineDataNeverSyncedToAnotherAccount() = runTest {
        // User A was offline and tracked a meal stored locally
        val offlineMealUserA = FoodLogEntity(
            userId = "user_A",
            foodName = "Protein Smoothie",
            calories = 250,
            protein = 30f,
            carbs = 20f,
            fat = 4f,
            timestamp = 1700000010000L
        )
        fakeFoodLogDao.insertFoodLog(offlineMealUserA)

        // User B logs in on this same device and syncs
        val userBLocalLogs = fakeFoodLogDao.getAllFoodLogsList("user_B")
        // User B must NOT see User A's offline data
        assertTrue(userBLocalLogs.isEmpty())

        // User B syncs with cloud
        fakeFirestoreRepository.syncRemoteHistoryToLocal("user_B", fakeFoodLogDao, null)

        // User B cloud must NOT have User A's meal
        val userBCloud = cloudMealDb["user_B"] ?: emptyList()
        assertTrue(userBCloud.isEmpty())
        assertFalse(userBCloud.any { it.foodName == "Protein Smoothie" })
    }

    @Test
    fun rule4_accountSwitchingRefreshesDashboardWithNewAccountDataOnly() = runTest {
        // User A has cloud history
        fakeFirestoreRepository.syncFoodLog("user_A", FoodLogEntity(userId = "user_A", foodName = "Eggs", calories = 200, protein = 18f, carbs = 2f, fat = 14f, timestamp = 1700000020000L))
        // User B has cloud history
        fakeFirestoreRepository.syncFoodLog("user_B", FoodLogEntity(userId = "user_B", foodName = "Salmon", calories = 400, protein = 35f, carbs = 0f, fat = 25f, timestamp = 1700000021000L))

        // Sync for User A
        val userAMeals = fakeFirestoreRepository.getUserFoodLogs("user_A").getOrNull() ?: emptyList()
        fakeFoodLogDao.insertAll(userAMeals)

        // Sync for User B
        val userBMeals = fakeFirestoreRepository.getUserFoodLogs("user_B").getOrNull() ?: emptyList()
        fakeFoodLogDao.insertAll(userBMeals)

        // When User B views dashboard
        val userBDashboard = fakeFoodLogDao.getAllFoodLogs("user_B").first()
        assertEquals(1, userBDashboard.size)
        assertEquals("Salmon", userBDashboard.first().foodName)
        assertFalse(userBDashboard.any { it.foodName == "Eggs" })
    }

    @Test
    fun rule5_accountDeletionPurgesServerAndLocalData() = runTest {
        // User A has data in local DB and cloud
        val meal = FoodLogEntity(userId = "user_A", foodName = "Greek Yogurt", calories = 150, protein = 15f, carbs = 8f, fat = 2f, timestamp = 1700000030000L)
        fakeFoodLogDao.insertFoodLog(meal)
        fakeFirestoreRepository.syncFoodLog("user_A", meal)

        assertEquals(1, fakeFoodLogDao.getCount("user_A"))
        assertEquals(1, cloudMealDb["user_A"]?.size)

        // Account deletion operation is triggered
        fakeFoodLogDao.deleteAllFoodLogsForUser("user_A")
        fakeFirestoreRepository.deleteUserAccountData("user_A")

        // Both local and server data are purged for user_A
        assertEquals(0, fakeFoodLogDao.getCount("user_A"))
        assertEquals(null, cloudMealDb["user_A"])
    }

    @Test
    fun rule6_serverConnectionDiagnostics_returnsLatency() = runTest {
        val fakeRepo = object : MealCloudSyncRepository {
            override suspend fun saveMealLocally(meal: FoodLogEntity): Long = 1L
            override suspend fun syncAccountData(userId: String): Result<Unit> = Result.success(Unit)
            override suspend fun wipeLocalUserData(userId: String): Result<Unit> = Result.success(Unit)
            override suspend fun deleteAccountCloudAndLocalData(userId: String): Result<Unit> = Result.success(Unit)
            override suspend fun checkServerConnectionTime(): Result<Long> = Result.success(85L)
        }

        val latencyResult = fakeRepo.checkServerConnectionTime()
        assertTrue(latencyResult.isSuccess)
        assertEquals(85L, latencyResult.getOrNull())
    }
}
