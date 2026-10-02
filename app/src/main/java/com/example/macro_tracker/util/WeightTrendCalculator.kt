package com.example.macro_tracker.util

import androidx.compose.runtime.Immutable
import com.example.macro_tracker.data.local.WeightLogEntity
import java.util.concurrent.TimeUnit

@Immutable
data class WeightTrendPoint(
    val timestamp: Long,
    val actualWeight: Float,
    val trendWeight: Float
)

@Immutable
data class WeightTrendSummary(
    val currentActualKg: Float = 0f,
    val currentTrendKg: Float = 0f,
    val weeklyRateKg: Float = 0f,
    val totalChangeKg: Float = 0f,
    val trendPoints: List<WeightTrendPoint> = emptyList()
)

object WeightTrendCalculator {

    private const val ALPHA = 0.15f // Exponential smoothing factor (MacroFactor style)

    fun calculateTrend(logs: List<WeightLogEntity>): WeightTrendSummary {
        if (logs.isEmpty()) return WeightTrendSummary()

        val sorted = logs.sortedBy { it.timestamp }
        val points = mutableListOf<WeightTrendPoint>()

        var runningTrend = sorted.first().weightKg

        for (log in sorted) {
            runningTrend = (ALPHA * log.weightKg) + ((1f - ALPHA) * runningTrend)
            points.add(
                WeightTrendPoint(
                    timestamp = log.timestamp,
                    actualWeight = log.weightKg,
                    trendWeight = (Math.round(runningTrend * 10f) / 10f)
                )
            )
        }

        val latestPoint = points.last()
        val firstPoint = points.first()

        val daysElapsed = TimeUnit.MILLISECONDS.toDays(latestPoint.timestamp - firstPoint.timestamp).coerceAtLeast(1)
        val deltaTrend = latestPoint.trendWeight - firstPoint.trendWeight
        val weeklyRate = if (points.size > 1 && daysElapsed > 0) {
            val rawRate = (deltaTrend / daysElapsed.toFloat()) * 7f
            Math.round(rawRate * 100f) / 100f
        } else {
            0f
        }

        val totalChange = Math.round((latestPoint.trendWeight - firstPoint.trendWeight) * 10f) / 10f

        return WeightTrendSummary(
            currentActualKg = latestPoint.actualWeight,
            currentTrendKg = latestPoint.trendWeight,
            weeklyRateKg = weeklyRate,
            totalChangeKg = totalChange,
            trendPoints = points
        )
    }
}
