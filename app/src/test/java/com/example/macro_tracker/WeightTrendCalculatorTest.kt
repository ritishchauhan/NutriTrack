package com.example.macro_tracker

import com.example.macro_tracker.data.local.WeightLogEntity
import com.example.macro_tracker.util.WeightTrendCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WeightTrendCalculatorTest {

    @Test
    fun testEmptyLogsReturnDefaultSummary() {
        val summary = WeightTrendCalculator.calculateTrend(emptyList())
        assertEquals(0f, summary.currentActualKg, 0.01f)
        assertEquals(0f, summary.currentTrendKg, 0.01f)
        assertEquals(0f, summary.weeklyRateKg, 0.01f)
        assertTrue(summary.trendPoints.isEmpty())
    }

    @Test
    fun testSingleLogReturnsSameScaleAndTrend() {
        val log = WeightLogEntity(
            id = 1,
            userId = "user_123",
            weightKg = 74.5f,
            timestamp = System.currentTimeMillis()
        )
        val summary = WeightTrendCalculator.calculateTrend(listOf(log))
        assertEquals(74.5f, summary.currentActualKg, 0.01f)
        assertEquals(74.5f, summary.currentTrendKg, 0.01f)
        assertEquals(1, summary.trendPoints.size)
    }

    @Test
    fun testEmaSmoothingDampensWaterWeightSpikes() {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        val log1 = WeightLogEntity(id = 1, userId = "user_123", weightKg = 70.0f, timestamp = now)
        // Sudden spike due to high-sodium cheat meal / water retention
        val log2 = WeightLogEntity(id = 2, userId = "user_123", weightKg = 72.0f, timestamp = now + oneDay)

        val summary = WeightTrendCalculator.calculateTrend(listOf(log1, log2))

        assertEquals(72.0f, summary.currentActualKg, 0.01f)
        // EMA: 70 + 0.15 * (72 - 70) = 70.3 kg
        assertEquals(70.3f, summary.currentTrendKg, 0.05f)
        // Trend weight is significantly lower than scale weight spike, preventing panic!
        assertTrue(summary.currentTrendKg < summary.currentActualKg)
    }

    @Test
    fun testWeeklyRateCalculationOverSevenDays() {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        val logs = listOf(
            WeightLogEntity(id = 1, userId = "u1", weightKg = 80.0f, timestamp = now),
            WeightLogEntity(id = 2, userId = "u1", weightKg = 79.8f, timestamp = now + (oneDay * 2)),
            WeightLogEntity(id = 3, userId = "u1", weightKg = 79.5f, timestamp = now + (oneDay * 4)),
            WeightLogEntity(id = 4, userId = "u1", weightKg = 79.2f, timestamp = now + (oneDay * 7))
        )

        val summary = WeightTrendCalculator.calculateTrend(logs)

        // Over 7 days, weight trend has decreased
        assertTrue(summary.weeklyRateKg < 0f)
        assertTrue(summary.totalChangeKg < 0f)
    }
}
