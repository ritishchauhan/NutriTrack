package com.example.macro_tracker.data.model

/**
 * Fitness goals supported by the Nutritrack Diet Planner.
 */
enum class FitnessGoal(val displayName: String, val shortDesc: String, val calorieAdjustmentDesc: String) {
    LOSE_WEIGHT(
        displayName = "Lose Weight (Fat Loss)",
        shortDesc = "Calorie deficit plan with high fiber & satiating lean proteins to shed fat while preserving muscle.",
        calorieAdjustmentDesc = "Deficit: -400 to -500 kcal/day"
    ),
    GAIN_MUSCLE(
        displayName = "Gain Muscle (Lean Bulk)",
        shortDesc = "High protein plan with balanced carbs and healthy fats for muscle hypertrophy and strength.",
        calorieAdjustmentDesc = "Surplus: +250 to +350 kcal/day"
    ),
    GAIN_WEIGHT(
        displayName = "Gain Weight (Healthy Bulk)",
        shortDesc = "Nutrient-dense calorie surplus plan with healthy fats, complex carbs, and whole foods.",
        calorieAdjustmentDesc = "Surplus: +500 kcal/day"
    )
}

enum class DietPreference(val displayName: String) {
    VEG("Vegetarian"),
    NON_VEG("Non-Vegetarian")
}

data class PlanMeal(
    val timing: String, // e.g., "Early Morning (7:00 AM)", "Breakfast (8:30 AM)"
    val mealType: String, // "Breakfast", "Lunch", "Snack", "Dinner"
    val dishName: String,
    val portionDesc: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val recipeId: String? = null
)

data class DailyDietPlan(
    val id: String,
    val goal: FitnessGoal,
    val preference: DietPreference,
    val title: String,
    val subtitle: String,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val totalFiber: Float,
    val meals: List<PlanMeal>,
    val nutritionTips: List<String>
)
