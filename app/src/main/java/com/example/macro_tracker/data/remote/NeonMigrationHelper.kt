package com.example.macro_tracker.data.remote

import android.util.Log
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.repository.FirestoreRepository

object NeonMigrationHelper {
    private const val TAG = "NeonMigrationHelper"

    data class LegacyProfile(
        val userId: String,
        val name: String,
        val email: String,
        val calorieTarget: Int,
        val proteinTarget: Int,
        val carbsTarget: Int,
        val fatTarget: Int,
        val fiberTarget: Int,
        val waterTarget: Double,
        val waterLogged: Double,
        val streakDays: Int,
        val weightKg: Double,
        val heightCm: Double,
        val fitnessGoal: String,
        val dietaryPreference: String,
        val activityLevel: String,
        val meals: List<FoodLogEntity> = emptyList()
    )

    val legacyProfiles = listOf(
        LegacyProfile(
            userId = "EGl4nJdZolacbtq4bMLoyDG6ESp2",
            name = "Broken Love",
            email = "",
            calorieTarget = 2300,
            proteinTarget = 108,
            carbsTarget = 305,
            fatTarget = 72,
            fiberTarget = 25,
            waterTarget = 2.1,
            waterLogged = 0.0,
            streakDays = 0,
            weightKg = 60.0,
            heightCm = 168.0,
            fitnessGoal = "GAIN_WEIGHT",
            dietaryPreference = "Non-veg",
            activityLevel = "Sedentary"
        ),
        LegacyProfile(
            userId = "ebN63728GUdRtbK0aKPXaJxAH4X2",
            name = "Ritish Chauhan",
            email = "",
            calorieTarget = 2400,
            proteinTarget = 156,
            carbsTarget = 307,
            fatTarget = 61,
            fiberTarget = 25,
            waterTarget = 3.4,
            waterLogged = 0.0,
            streakDays = 0,
            weightKg = 82.0,
            heightCm = 180.0,
            fitnessGoal = "LOSE_WEIGHT",
            dietaryPreference = "Non-veg",
            activityLevel = "Sedentary"
        ),
        LegacyProfile(
            userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
            name = "Vijay Chauhan",
            email = "",
            calorieTarget = 2600,
            proteinTarget = 155,
            carbsTarget = 340,
            fatTarget = 69,
            fiberTarget = 25,
            waterTarget = 2.6,
            waterLogged = 0.0,
            streakDays = 0,
            weightKg = 74.0,
            heightCm = 173.0,
            fitnessGoal = "GAIN_MUSCLE",
            dietaryPreference = "Vegetarian",
            activityLevel = "Sedentary",
            meals = listOf(
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Indori Style Kanda Poha with Peanuts",
                    calories = 270,
                    protein = 7f,
                    carbs = 44f,
                    fat = 8f,
                    fiber = 4f,
                    servings = 1,
                    mealType = "Breakfast",
                    timestamp = 1790876581326L,
                    details = "Indian Kitchen Recipe • 18m cook"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Punjabi Rajma Masala (Kidney Beans)",
                    calories = 310,
                    protein = 16f,
                    carbs = 48f,
                    fat = 6f,
                    fiber = 12f,
                    servings = 1,
                    mealType = "Lunch",
                    timestamp = 1790876584864L,
                    details = "Indian Kitchen Recipe • 40m cook"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "High-Protein Banana Peanut Butter Shake",
                    calories = 360,
                    protein = 18f,
                    carbs = 48f,
                    fat = 12f,
                    fiber = 6f,
                    servings = 1,
                    mealType = "Snack",
                    timestamp = 1790876587550L,
                    details = "Indian Kitchen Recipe • 5m cook"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Homestyle Paneer Bhurji",
                    calories = 310,
                    protein = 22f,
                    carbs = 10f,
                    fat = 18f,
                    fiber = 4f,
                    servings = 1,
                    mealType = "Dinner",
                    timestamp = 1790876590392L,
                    details = "Indian Kitchen Recipe • 22m cook"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Plain Whole Wheat Roti / Chapati (1 medium)",
                    calories = 595,
                    protein = 21f,
                    carbs = 122.5f,
                    fat = 3.5f,
                    fiber = 19.6f,
                    servings = 7,
                    mealType = "Breakfast",
                    timestamp = 1790876665624L,
                    details = "P 3g • C 17g • F 0g • Grains"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Cow Milk - Toned 3% Fat (1 cup)",
                    calories = 280,
                    protein = 15.6f,
                    carbs = 23f,
                    fat = 14f,
                    fiber = 0f,
                    servings = 2,
                    mealType = "Snack",
                    timestamp = 1790876755967L,
                    details = "P 7g • C 11g • F 7g • Dairy"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "sIBKgsME17ZrBCl86TocvheQgLI2",
                    foodName = "Cooked White Rice (1 cup)",
                    calories = 410,
                    protein = 8.4f,
                    carbs = 89f,
                    fat = 0.8f,
                    fiber = 1.2f,
                    servings = 2,
                    mealType = "Snack",
                    timestamp = 1790876770454L,
                    details = "P 4g • C 44g • F 0g • Grains"
                )
            )
        ),
        LegacyProfile(
            userId = "si2hAsMAINf4GG6ktkLEChpyDzw1",
            name = "Yashraj Uniyal",
            email = "",
            calorieTarget = 2000,
            proteinTarget = 120,
            carbsTarget = 240,
            fatTarget = 70,
            fiberTarget = 25,
            waterTarget = 2.4,
            waterLogged = 0.0,
            streakDays = 0,
            weightKg = 70.0,
            heightCm = 170.0,
            fitnessGoal = "LOSE_WEIGHT",
            dietaryPreference = "Non-veg",
            activityLevel = "Sedentary"
        ),
        LegacyProfile(
            userId = "5dYU3wOMAwQG7lf8bbaPCIIU3iK2",
            name = "Ritish Chauhan",
            email = "",
            calorieTarget = 2000,
            proteinTarget = 133,
            carbsTarget = 252,
            fatTarget = 51,
            fiberTarget = 25,
            waterTarget = 2.5,
            waterLogged = 0.0,
            streakDays = 0,
            weightKg = 70.0,
            heightCm = 170.0,
            fitnessGoal = "LOSE_WEIGHT",
            dietaryPreference = "Non-veg",
            activityLevel = "Sedentary",
            meals = listOf(
                FoodLogEntity(
                    id = 0,
                    userId = "5dYU3wOMAwQG7lf8bbaPCIIU3iK2",
                    foodName = "Plain Whole Wheat Roti / Chapati (1 medium)",
                    calories = 85,
                    protein = 3f,
                    carbs = 17.5f,
                    fat = 0.5f,
                    fiber = 2.8f,
                    servings = 1,
                    mealType = "Breakfast",
                    timestamp = 1790837057443L,
                    details = "P 3g • C 17g • F 0g • Grains"
                ),
                FoodLogEntity(
                    id = 0,
                    userId = "5dYU3wOMAwQG7lf8bbaPCIIU3iK2",
                    foodName = "Cooked White Rice (1 cup)",
                    calories = 205,
                    protein = 4.2f,
                    carbs = 44.5f,
                    fat = 0.4f,
                    fiber = 0.6f,
                    servings = 1,
                    mealType = "Breakfast",
                    timestamp = 1790837066506L,
                    details = "P 4g • C 44g • F 0g • Grains"
                )
            )
        )
    )

    suspend fun migrateIfNecessary(userId: String, firestoreRepository: FirestoreRepository) {
        val legacy = legacyProfiles.find { it.userId == userId } ?: return
        try {
            val existing = firestoreRepository.getUserProfile(userId).getOrNull()
            if (existing == null || !existing.containsKey("calorieTarget")) {
                val profileMap = mapOf(
                    "userId" to legacy.userId,
                    "name" to legacy.name,
                    "email" to legacy.email,
                    "calorieTarget" to legacy.calorieTarget,
                    "proteinTarget" to legacy.proteinTarget,
                    "carbsTarget" to legacy.carbsTarget,
                    "fatTarget" to legacy.fatTarget,
                    "fiberTarget" to legacy.fiberTarget,
                    "waterTarget" to legacy.waterTarget,
                    "waterLogged" to legacy.waterLogged,
                    "streakDays" to legacy.streakDays,
                    "weightKg" to legacy.weightKg,
                    "heightCm" to legacy.heightCm,
                    "fitnessGoal" to legacy.fitnessGoal,
                    "dietaryPreference" to legacy.dietaryPreference,
                    "activityLevel" to legacy.activityLevel
                )
                firestoreRepository.saveUserProfile(userId, profileMap)
                Log.d(TAG, "Migrated user profile from Neon to Firestore for $userId")

                if (legacy.meals.isNotEmpty()) {
                    firestoreRepository.syncAllFoodLogsToFirestore(userId, legacy.meals)
                    Log.d(TAG, "Migrated ${legacy.meals.size} meals from Neon to Firestore for $userId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in migration from Neon to Firestore", e)
        }
    }

    suspend fun migrateAllProfiles(firestoreRepository: FirestoreRepository) {
        for (legacy in legacyProfiles) {
            try {
                val profileMap = mapOf(
                    "userId" to legacy.userId,
                    "name" to legacy.name,
                    "email" to legacy.email,
                    "calorieTarget" to legacy.calorieTarget,
                    "proteinTarget" to legacy.proteinTarget,
                    "carbsTarget" to legacy.carbsTarget,
                    "fatTarget" to legacy.fatTarget,
                    "fiberTarget" to legacy.fiberTarget,
                    "waterTarget" to legacy.waterTarget,
                    "waterLogged" to legacy.waterLogged,
                    "streakDays" to legacy.streakDays,
                    "weightKg" to legacy.weightKg,
                    "heightCm" to legacy.heightCm,
                    "fitnessGoal" to legacy.fitnessGoal,
                    "dietaryPreference" to legacy.dietaryPreference,
                    "activityLevel" to legacy.activityLevel
                )
                firestoreRepository.saveUserProfile(legacy.userId, profileMap)
                if (legacy.meals.isNotEmpty()) {
                    firestoreRepository.syncAllFoodLogsToFirestore(legacy.userId, legacy.meals)
                }
                Log.d(TAG, "Migrated legacy user ${legacy.userId} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating legacy profile ${legacy.userId}", e)
            }
        }
    }
}

