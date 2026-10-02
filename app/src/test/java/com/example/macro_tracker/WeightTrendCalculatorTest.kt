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

    @Test
    fun testFoodItemWeightAdjustmentScaling() {
        val initialMeal = com.example.macro_tracker.data.local.FoodLogEntity(
            id = 1,
            foodName = "Paneer Tikka",
            calories = 300,
            protein = 20f,
            carbs = 10f,
            fat = 20f,
            fiber = 4f,
            weightGrams = 100f,
            servings = 1,
            timestamp = System.currentTimeMillis(),
            details = "P 20g • C 10g • F 20g • Fib 4g"
        )

        // User adjusts weight from 100g to 250g
        val newWeight = 250f
        val ratio = newWeight / initialMeal.weightGrams
        assertEquals(2.5f, ratio, 0.001f)

        val scaledCalories = Math.round(initialMeal.calories * ratio).toInt()
        val scaledProtein = initialMeal.protein * ratio
        val scaledCarbs = initialMeal.carbs * ratio
        val scaledFat = initialMeal.fat * ratio
        val scaledFiber = initialMeal.fiber * ratio

        assertEquals(750, scaledCalories)
        assertEquals(50f, scaledProtein, 0.01f)
        assertEquals(25f, scaledCarbs, 0.01f)
        assertEquals(50f, scaledFat, 0.01f)
        assertEquals(10f, scaledFiber, 0.01f)
    }

    @Test
    fun testThreeMonthPredictionsMath() {
        val startWeight = 80f
        val weeklyLossPace = 0.5f // 0.5 kg/week standard weight loss
        val weeklyGainPace = 0.35f // 0.35 kg/week lean bulk

        // 3 Months = 12 weeks
        val threeMonthLoss = startWeight - (weeklyLossPace * 12f)
        assertEquals(74.0f, threeMonthLoss, 0.01f)

        val threeMonthGain = startWeight + (weeklyGainPace * 12f)
        assertEquals(84.2f, threeMonthGain, 0.01f)

        // Milestones
        val m1Loss = startWeight - (weeklyLossPace * 4f)
        val m2Loss = startWeight - (weeklyLossPace * 8f)
        val m3Loss = startWeight - (weeklyLossPace * 12f)

        assertEquals(78.0f, m1Loss, 0.01f)
        assertEquals(76.0f, m2Loss, 0.01f)
        assertEquals(74.0f, m3Loss, 0.01f)

        // Calorie deficit calculation: ~1100 kcal / kg per week deficit
        val dailyLossDeficit = (weeklyLossPace * 1100f).toInt()
        assertEquals(550, dailyLossDeficit)

        val dailyGainSurplus = (weeklyGainPace * 1100f).toInt()
        assertEquals(385, dailyGainSurplus)
    }
}
