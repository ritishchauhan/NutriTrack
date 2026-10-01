package com.example.macro_tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodLogs: List<FoodLogEntity>)

    @Query("SELECT * FROM food_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllFoodLogs(userId: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllFoodLogsList(userId: String): List<FoodLogEntity>

    @Query("SELECT * FROM food_logs WHERE userId = :userId AND timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp ASC")
    fun getFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE userId = :userId AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getFoodLogsSince(userId: String, sinceTimestamp: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT COUNT(*) FROM food_logs WHERE userId = :userId")
    suspend fun getCount(userId: String): Int

    @Delete
    suspend fun deleteFoodLog(foodLog: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE userId = :userId AND timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun deleteFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long)

    @Query("DELETE FROM food_logs WHERE userId = :userId")
    suspend fun deleteAllFoodLogsForUser(userId: String)

    @Query("DELETE FROM food_logs")
    suspend fun deleteAllFoodLogs()
}
