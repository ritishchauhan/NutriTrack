package com.example.macro_tracker.data.model

import androidx.compose.ui.graphics.Color

/**
 * Data model for Indian kitchen recipes, containing ingredients,
 * step-by-step cooking instructions, and detailed nutritional macros.
 */
data class Recipe(
    val id: String,
    val title: String,
    val isVeg: Boolean,
    val category: RecipeCategory,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val ingredients: List<String>,
    val instructions: List<String>,
    val tags: List<String> = emptyList(),
    val servingSize: String = "1 serving"
) {
    val totalTimeMinutes: Int get() = prepTimeMinutes + cookTimeMinutes
}

enum class RecipeCategory(val displayName: String) {
    ALL("All"),
    VEG("Vegetarian"),
    NON_VEG("Non-Veg"),
    HIGH_PROTEIN("High Protein"),
    BREAKFAST("Breakfast"),
    LUNCH_DINNER("Lunch & Dinner"),
    SNACKS_DRINKS("Snacks & Drinks")
}
