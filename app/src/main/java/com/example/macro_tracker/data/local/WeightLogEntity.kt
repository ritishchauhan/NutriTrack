package com.example.macro_tracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_logs",
    indices = [
        Index(value = ["userId", "timestamp"]),
        Index(value = ["userId"])
    ]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val weightKg: Float,
    val timestamp: Long,
    val note: String = ""
)
