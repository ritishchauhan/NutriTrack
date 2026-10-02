package com.example.macro_tracker

import com.example.macro_tracker.data.local.DietPlansData
import com.example.macro_tracker.data.local.DishDatabase
import com.example.macro_tracker.data.local.KitchenIngredient
import com.example.macro_tracker.data.local.RecipeIngredientEntry
import com.example.macro_tracker.data.model.DietPreference
import com.example.macro_tracker.data.model.FitnessGoal
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DietPlanAndCustomMealTest {

    @Test
    fun testCalibratedPlanMeetsUserProteinAndCalorieGoals() {
        val targetProtein = 150
        val targetCalories = 2100
        val targetCarbs = 230
        val targetFat = 55

        val plan = DietPlansData.getCalibratedPlan(
            goal = FitnessGoal.LOSE_WEIGHT,
            preference = DietPreference.VEG,
            targetCalories = targetCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat
        )

        assertEquals(targetProtein.toFloat(), plan.totalProtein, 0.01f)
        assertEquals(targetCalories, plan.totalCalories)

        // Sum of all meals in the day should complete the 150g protein and 2100 kcal targets!
        val sumOfMealProteins = plan.meals.map { it.protein }.sum()
        val sumOfMealCalories = plan.meals.sumOf { it.calories }

        assertTrue(
            "Sum of meal proteins ($sumOfMealProteins) should match target ($targetProtein)",
            abs(sumOfMealProteins - targetProtein) <= 1.0f
        )
        assertEquals(
            "Sum of meal calories ($sumOfMealCalories) should match target ($targetCalories)",
            targetCalories,
            sumOfMealCalories
        )
    }

    @Test
    fun testGainMuscleCalibratedPlan() {
        val targetProtein = 180
        val targetCalories = 2700
        val targetCarbs = 320
        val targetFat = 75

        val plan = DietPlansData.getCalibratedPlan(
            goal = FitnessGoal.GAIN_MUSCLE,
            preference = DietPreference.NON_VEG,
            targetCalories = targetCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat
        )

        assertEquals(targetProtein.toFloat(), plan.totalProtein, 0.01f)
        assertEquals(targetCalories, plan.totalCalories)

        val sumOfMealProteins = plan.meals.map { it.protein }.sum()
        val sumOfMealCalories = plan.meals.sumOf { it.calories }

        assertTrue(abs(sumOfMealProteins - targetProtein) <= 1.0f)
        assertEquals(targetCalories, sumOfMealCalories)
    }

    @Test
    fun testPortionDescriptionScaling() {
        val baseDesc = "40g rolled oats cooked with 200ml toned milk"
        val scaledDesc = DietPlansData.scalePortionDescription(baseDesc, 1.25f)
        // 40 * 1.25 = 50g, 200 * 1.25 = 250ml
        assertTrue("Portion description should scale grams", scaledDesc.contains("50 g") || scaledDesc.contains("50g"))
        assertTrue("Portion description should scale ml", scaledDesc.contains("250 ml") || scaledDesc.contains("250ml"))
    }

    @Test
    fun testKnownDishRecognition() {
        val paneer = DishDatabase.findKnownDish("paneer bhurji")
        assertNotNull(paneer)
        assertEquals("paneer_bhurji", paneer!!.id)

        val dal = DishDatabase.findKnownDish("dal tadka")
        assertNotNull(dal)
        assertEquals("dal_tadka", dal!!.id)

        val roti = DishDatabase.findKnownDish("roti")
        assertNotNull(roti)
        assertEquals("wheat_roti", roti!!.id)

        val chicken = DishDatabase.findKnownDish("chicken curry")
        assertNotNull(chicken)
        assertEquals("chicken_curry", chicken!!.id)

        val biryani = DishDatabase.findKnownDish("chicken biryani")
        assertNotNull(biryani)
        assertEquals("chicken_biryani", biryani!!.id)

        val unknown = DishDatabase.findKnownDish("NonExistentSpecialDish123")
        assertNull(unknown)
    }

    @Test
    fun testDishAutoCalculationByWeight() {
        val dish = DishDatabase.findKnownDish("paneer bhurji")!!
        // Paneer bhurji per 100g: 190 kcal, 14g P
        val calc100g = DishDatabase.calculateDishNutrition(dish, 100f)
        assertEquals(190, calc100g.calories)
        assertEquals(14.0f, calc100g.protein, 0.1f)

        // For 200g portion: should be exactly double
        val calc200g = DishDatabase.calculateDishNutrition(dish, 200f)
        assertEquals(380, calc200g.calories)
        assertEquals(28.0f, calc200g.protein, 0.1f)

        // For 150g portion: should be 1.5x
        val calc150g = DishDatabase.calculateDishNutrition(dish, 150f)
        assertEquals(285, calc150g.calories)
        assertEquals(21.0f, calc150g.protein, 0.1f)
    }

    @Test
    fun testCompositeIngredientCalculationForUnknownDish() {
        // Build a recipe: 100g Paneer (265 kcal, 18.5g P) + 10g Ghee (90 kcal, 0g P)
        val paneer = DishDatabase.standardIngredients.first { it.id == "ing_paneer" }
        val ghee = DishDatabase.standardIngredients.first { it.id == "ing_ghee" }

        val entries = listOf(
            RecipeIngredientEntry(paneer, 100f),
            RecipeIngredientEntry(ghee, 10f)
        )

        val composite = DishDatabase.calculateCompositeNutrition(entries)
        // 265 + 90 = 355 kcal
        assertEquals(355, composite.calories)
        // 18.5g protein
        assertEquals(18.5f, composite.protein, 0.1f)
        // 10g fat from ghee + 20g from paneer = 30g fat
        assertEquals(30.0f, composite.fat, 0.1f)
    }
}
