package com.example.macro_tracker.data.remote

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

data class HealthActivityData(
    val steps: Long = 0L,
    val caloriesBurned: Int = 0,
    val activeCaloriesBurned: Int = 0,
    val isAvailable: Boolean = false,
    val hasPermissions: Boolean = false
)

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy {
        try {
            if (isSupported()) HealthConnectClient.getOrCreate(context) else null
        } catch (e: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun isSupported(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readTodayActivity(): HealthActivityData {
        val client = healthConnectClient ?: return HealthActivityData(isAvailable = false)
        val hasPerms = hasPermissions()
        if (!hasPerms) {
            return HealthActivityData(isAvailable = true, hasPermissions = false)
        }

        return try {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val totalEnergyKcal = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            val activeEnergyKcal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            HealthActivityData(
                steps = steps,
                caloriesBurned = if (totalEnergyKcal > 0) totalEnergyKcal.toInt() else activeEnergyKcal.toInt(),
                activeCaloriesBurned = activeEnergyKcal.toInt(),
                isAvailable = true,
                hasPermissions = true
            )
        } catch (e: Exception) {
            HealthActivityData(isAvailable = true, hasPermissions = true)
        }
    }
}
