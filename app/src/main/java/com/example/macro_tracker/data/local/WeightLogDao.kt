package com.example.macro_tracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY timestamp ASC")
    fun getWeightLogsForUser(userId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeightLog(userId: String): Flow<WeightLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weightLog: WeightLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WeightLogEntity>)

    @Delete
    suspend fun delete(weightLog: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
