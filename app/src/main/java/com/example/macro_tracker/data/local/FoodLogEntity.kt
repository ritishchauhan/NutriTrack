package com.example.macro_tracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_logs",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "timestamp"]),
        Index(value = ["userId", "mealType"]),
        Index(value = ["timestamp"])
    ]
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val timestamp: Long,
    val mealType: String = "Breakfast",
    val details: String = "",
    val fiber: Float = 0f,
    val servings: Int = 1,
    val weightGrams: Float = 100f
)
