package com.example.macro_tracker.data.local

import com.example.macro_tracker.data.model.DailyDietPlan
import com.example.macro_tracker.data.model.DietPreference
import com.example.macro_tracker.data.model.FitnessGoal
import com.example.macro_tracker.data.model.PlanMeal
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Curated Indian home-cooking diet plans engineered directly from
 * "The Vegetarian Forge: Complete Indian Diet Blueprint" by Coach Dinesh Dudeja,
 * alongside high-protein Non-Veg adaptations (Chicken, Eggs, Fish).
 * Features the 16-Rung Calorie Ladder (1500 to 3000 kcal) and the 4-Meal / 5-Meal Forge Template:
 * - Meal 1: The Ignition (Breakfast)
 * - Meal 2: The Anvil (Lunch)
 * - Meal 3: The Bridge (Afternoon / Pre-Workout)
 * - Meal 4: The Furnace (Dinner)
 * - Meal 5: The Quench (Pre-Bed)
 */
object DietPlansData {

    /**
     * Calculates the starting calorie rung on the 16-Rung Calorie Ladder (1500 - 3000 kcal,
     * in 100 kcal steps) based on bodyweight and fitness goal:
     * - Fat Loss (Cut): 28 to 30 kcal per kg bodyweight
     * - Maintenance: 32 to 34 kcal per kg bodyweight
     * - Muscle Gain / Bulk: 36 to 38 kcal per kg bodyweight
     */
    fun calculateCalorieTarget(weightKg: Float, goal: FitnessGoal): Int {
        val multiplier = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> 29.0f // 28 to 30 kcal/kg (Page 2)
            FitnessGoal.GAIN_MUSCLE -> 35.0f // 34 to 36 kcal/kg (Page 2)
            FitnessGoal.GAIN_WEIGHT -> 38.0f // 36 to 38 kcal/kg (Page 2)
        }
        val raw = (weightKg * multiplier).roundToInt()
        // Round to the nearest 100 kcal rung on the ladder (1500 - 3000)
        val rounded = ((raw + 50) / 100) * 100
        return rounded.coerceIn(1500, 3000)
    }

    /**
     * Calculates the daily protein goal:
     * Page 4 & 5 rule: 1.6 to 2.2 g per kg bodyweight, with each meal anchored on a dense source.
     * - Fat loss: 1.9 g/kg (to protect lean muscle in a deficit)
     * - Muscle gain: 2.1 g/kg (for optimal hypertrophy)
     * - Weight gain: 1.8 g/kg
     */
    fun calculateProteinTarget(weightKg: Float, goal: FitnessGoal): Int {
        val multiplier = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> 1.9f
            FitnessGoal.GAIN_MUSCLE -> 2.1f
            FitnessGoal.GAIN_WEIGHT -> 1.8f
        }
        return (weightKg * multiplier).roundToInt().coerceIn(75, 230)
    }

    /**
     * Calculates daily fat target: 20-25% of total calories.
     */
    fun calculateFatTarget(calories: Int, goal: FitnessGoal): Int {
        val pct = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> 0.23f
            FitnessGoal.GAIN_MUSCLE -> 0.24f
            FitnessGoal.GAIN_WEIGHT -> 0.28f
        }
        return ((calories * pct) / 9f).roundToInt()
    }

    /**
     * Calculates daily carbohydrate target: remaining calories after protein & fat.
     */
    fun calculateCarbsTarget(calories: Int, proteinGrams: Int, fatGrams: Int): Int {
        val remainingKcal = calories - (proteinGrams * 4 + fatGrams * 9)
        return (remainingKcal / 4f).roundToInt().coerceAtLeast(100)
    }

    /**
     * Calculates daily water intake calibrated directly to the user's BMI and body weight:
     * - Underweight (BMI < 18.5): 32 ml/kg (minimum 2.0L) - gentle metabolic hydration
     * - Normal BMI (18.5 - 24.9): 35 ml/kg (Forge Blueprint baseline)
     * - Overweight (BMI 25.0 - 29.9): 38 ml/kg + 250ml metabolic allowance for elevated fat oxidation
     * - Obese (BMI >= 30.0): 42 ml/kg + 500ml for renal clearance and lipolysis
     * Result is rounded to 1 decimal place (2.0L to 4.5L ceiling).
     */
    fun calculateWaterTarget(weightKg: Float, bmi: Float): Float {
        val mlPerKg = when {
            bmi < 18.5f -> 32f
            bmi in 18.5f..24.9f -> 35f
            bmi in 25.0f..29.9f -> 38f
            else -> 42f
        }
        val bonus = when {
            bmi in 25.0f..29.9f -> 0.25f
            bmi >= 30.0f -> 0.5f
            else -> 0.0f
        }
        val liters = ((weightKg * mlPerKg) / 1000f) + bonus
        return (Math.round(liters * 10.0) / 10.0).toFloat().coerceIn(2.0f, 4.5f)
    }

    fun calculateWaterTarget(weightKg: Float): Float = calculateWaterTarget(weightKg, 22.0f)

    fun getWaterBmiExplanation(bmi: Float): String {
        return when {
            bmi < 18.5f -> "32 ml/kg (Calibrated for lean body mass)"
            bmi in 18.5f..24.9f -> "35 ml/kg (Forge Blueprint baseline)"
            bmi in 25.0f..29.9f -> "38 ml/kg + 250ml (Boosted for fat oxidation)"
            else -> "42 ml/kg + 500ml (Renal filtration & metabolic flush)"
        }
    }

    val allPlans: List<DailyDietPlan> = listOf(
        // 1. LOSE WEIGHT (VEGETARIAN) - The Lean Forge 1600 kcal Blueprint
        DailyDietPlan(
            id = "plan_lose_weight_veg",
            goal = FitnessGoal.LOSE_WEIGHT,
            preference = DietPreference.VEG,
            title = "Pure Vegetarian Fat Loss Plan",
            subtitle = "High-protein, high-satiety Indian home cooking calibrated for fat loss and lean preservation.",
            totalCalories = 1600,
            totalProtein = 117f,
            totalCarbs = 190f,
            totalFat = 41f,
            totalFiber = 34f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Masala Rolled Oats & Paneer Chilla",
                    portionDesc = "40g rolled oats cooked with 200ml toned milk + 1 medium Moong Paneer Chilla + 1 banana",
                    calories = 450,
                    protein = 36f,
                    carbs = 54f,
                    fat = 10f,
                    fiber = 6f,
                    recipeId = "veg_moong_chilla"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Basmati Rice, Dal Tadka & Fresh Curd",
                    portionDesc = "50g dry basmati rice (140g cooked) + 1 bowl Yellow Moong Dal (45g dry) + 150g cooked sabzi + 100g curd + 1 tsp ghee",
                    calories = 490,
                    protein = 21f,
                    carbs = 79f,
                    fat = 10f,
                    fiber = 9f,
                    recipeId = "veg_dal_tadka"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "Greek Yogurt, Fruit & Roasted Peanuts",
                    portionDesc = "150g hung curd / Greek yogurt + 1 fresh apple (100g) + 10g roasted peanuts (or Sattu Drink)",
                    calories = 200,
                    protein = 26f,
                    carbs = 8f,
                    fat = 7f,
                    fiber = 4f,
                    recipeId = "snack_roasted_makhana"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Soft Low-Fat Paneer Bhurji with Whole Wheat Phulkas",
                    portionDesc = "110g soft paneer bhurji / Palak Paneer + 1.5 whole wheat rotis (45g atta) + 150g cooked sabzi + raw cucumber salad + 1 tsp ghee",
                    calories = 460,
                    protein = 34f,
                    carbs = 49f,
                    fat = 14f,
                    fiber = 11f,
                    recipeId = "veg_paneer_bhurji"
                )
            ),
            nutritionTips = listOf(
                "Rule 1: Anchor every meal on a dense protein source (paneer, curd, whey/sattu, or dal).",
                "Rule 2: Soak legumes 8 hours, DISCARD the soak water, and pressure cook with hing & ajwain to prevent bloat.",
                "Rule 3: Drink 35 ml water per kg of bodyweight, drinking mostly between meals rather than chugging on a full plate.",
                "Rule 4: Cap heavy legumes (rajma/chole) at one meal a day. Dal counts as light and gentle on the gut.",
                "Rule 5: Spend 20 minutes chewing main meals without screens to eliminate gas and boost digestion."
            )
        ),

        // 2. LOSE WEIGHT (NON-VEGETARIAN) - Lean Poultry & Egg White Cut (1650 kcal)
        DailyDietPlan(
            id = "plan_lose_weight_nonveg",
            goal = FitnessGoal.LOSE_WEIGHT,
            preference = DietPreference.NON_VEG,
            title = "Desi Lean Poultry & Egg Cut",
            subtitle = "Maximum thermic effect and complete amino acid profile for rapid fat loss while retaining muscle.",
            totalCalories = 1650,
            totalProtein = 138f,
            totalCarbs = 140f,
            totalFat = 46f,
            totalFiber = 26f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Spinach & Herb Egg White Omelette with Toast",
                    portionDesc = "4 egg whites + 1 whole egg omelette + 1 slice multigrain toast + 1 glass lemon chia seed water",
                    calories = 360,
                    protein = 32f,
                    carbs = 26f,
                    fat = 11f,
                    fiber = 5f,
                    recipeId = "nonveg_egg_white_omelette"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Lemon Pepper Chicken Breast with Brown Rice",
                    portionDesc = "200g juicy pan-seared chicken breast + 50g dry brown or white rice + 150g steamed sabzi + cucumber raita",
                    calories = 490,
                    protein = 46f,
                    carbs = 48f,
                    fat = 12f,
                    fiber = 7f,
                    recipeId = "nonveg_lemon_pepper_chicken"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "High-Protein Boiled Egg Chaat",
                    portionDesc = "2 hard-boiled eggs quartered with chopped onions, tomatoes, chaat masala & fresh lemon juice",
                    calories = 190,
                    protein = 16f,
                    carbs = 8f,
                    fat = 10f,
                    fiber = 2f,
                    recipeId = "nonveg_boiled_egg_chaat"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Tariwala Homestyle Chicken Curry with Phulkas",
                    portionDesc = "1 bowl chicken curry (180g chicken) + 2 Whole Wheat Phulkas + large kachumber salad + 1 tsp cold-pressed oil",
                    calories = 480,
                    protein = 44f,
                    carbs = 44f,
                    fat = 13f,
                    fiber = 7f,
                    recipeId = "nonveg_homestyle_chicken_curry"
                )
            ),
            nutritionTips = listOf(
                "Chicken breast and egg whites provide the highest protein-to-calorie density in human nutrition.",
                "Tawa-sear your chicken with lemon, crushed black pepper, and garlic to retain moisture without deep frying.",
                "Eat high-volume raw vegetables (cucumbers, tomatoes) before the meal to trigger gastric fullness.",
                "Keep 12 hours of overnight digestive rest between dinner and breakfast."
            )
        ),

        // 3. GAIN MUSCLE (VEGETARIAN) - The Build Forge 2300 kcal Blueprint
        DailyDietPlan(
            id = "plan_gain_muscle_veg",
            goal = FitnessGoal.GAIN_MUSCLE,
            preference = DietPreference.VEG,
            title = "High-Protein Vegetarian Muscle Plan",
            subtitle = "Strategic pairing of paneer, rajma, curd, and sattu/shake to hit optimal muscle building protein.",
            totalCalories = 2300,
            totalProtein = 141f,
            totalCarbs = 288f,
            totalFat = 64f,
            totalFiber = 38f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Poha with Peanuts & Sattu Protein Drink",
                    portionDesc = "70g dry poha cooked with mustard, curry leaves & 15g peanuts + 100g fresh curd + 1 glass sattu drink / milk",
                    calories = 644,
                    protein = 42f,
                    carbs = 74f,
                    fat = 20f,
                    fiber = 6f,
                    recipeId = "veg_poha_peanuts"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Basmati Rice with Punjabi Rajma Masala & Ghee",
                    portionDesc = "85g dry basmati rice (235g cooked) + 55g dry rajma (soaked 8h, pressure-cooked) + 150g sabzi + 100g curd + 1.4 tsp ghee",
                    calories = 665,
                    protein = 25f,
                    carbs = 112f,
                    fat = 13f,
                    fiber = 12f,
                    recipeId = "veg_rajma_masala"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "Banana Peanut Butter Shake or Curd with Almonds",
                    portionDesc = "150g Greek yogurt / curd + 1 banana (120g) + 15g almonds + 1.5 tbsp peanut butter",
                    calories = 411,
                    protein = 30f,
                    carbs = 39f,
                    fat = 15f,
                    fiber = 7f,
                    recipeId = "snack_banana_peanut_smoothie"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Low-Fat Paneer Bhurji / Palak Paneer with 2 Rotis",
                    portionDesc = "130g low-fat paneer cooked with onions, tomatoes & spices + 2 rotis (60g atta) + 80g green peas + salad + 1 tsp ghee",
                    calories = 580,
                    protein = 44f,
                    carbs = 63f,
                    fat = 16f,
                    fiber = 11f,
                    recipeId = "veg_paneer_bhurji"
                )
            ),
            nutritionTips = listOf(
                "Leucine Trigger: Aim for 30-40g protein per main meal to trigger muscle protein synthesis.",
                "Swap Strategy: Poha and rice are low-residue carbs that prevent the heavy stomach bloat associated with all-wheat diets.",
                "Legume Safety: Never double rajma portions in a single jump; increase fiber by 5g per week to allow your microbiome to adapt.",
                "Consume the banana peanut butter shake within 45 minutes of training to rapidly restore muscle glycogen."
            )
        ),

        // 4. GAIN MUSCLE (NON-VEGETARIAN) - Pro Bodybuilder Chicken & Egg Protocol (2500 kcal)
        DailyDietPlan(
            id = "plan_gain_muscle_nonveg",
            goal = FitnessGoal.GAIN_MUSCLE,
            preference = DietPreference.NON_VEG,
            title = "Pro Bodybuilder Desi Chicken & Egg Protocol",
            subtitle = "165g+ complete animal protein for accelerated hypertrophy, strength, and tissue recovery.",
            totalCalories = 2500,
            totalProtein = 168f,
            totalCarbs = 260f,
            totalFat = 72f,
            totalFiber = 30f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Street Style Masala Egg Bhurji & Buttered Toast",
                    portionDesc = "3 whole eggs + 2 egg whites bhurji + 2 whole wheat slices with butter + 1 fresh banana",
                    calories = 550,
                    protein = 36f,
                    carbs = 48f,
                    fat = 22f,
                    fiber = 5f,
                    recipeId = "nonveg_masala_egg_bhurji"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Homestyle Chicken Curry with Rice, Dal & Salad",
                    portionDesc = "200g chicken in spiced onion-tomato curry + 75g basmati rice + 1 small bowl dal + cucumber raita",
                    calories = 680,
                    protein = 52f,
                    carbs = 78f,
                    fat = 16f,
                    fiber = 8f,
                    recipeId = "nonveg_homestyle_chicken_curry"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "Stovetop Tawa Chicken Tikka or Egg Roll",
                    portionDesc = "150g marinated chicken breast skewers with mint chutney + sliced onions + 15g almonds",
                    calories = 360,
                    protein = 40f,
                    carbs = 10f,
                    fat = 16f,
                    fiber = 3f,
                    recipeId = "nonveg_tawa_chicken_tikka"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Chicken Keema Matar or Fish Curry with 2.5 Phulkas",
                    portionDesc = "180g lean chicken keema with green peas + 2.5 whole wheat phulkas + mixed sabzi + 1 tsp ghee",
                    calories = 620,
                    protein = 48f,
                    carbs = 62f,
                    fat = 18f,
                    fiber = 8f,
                    recipeId = "nonveg_chicken_keema_matar"
                )
            ),
            nutritionTips = listOf(
                "Spread protein across 4 distinct feeding windows (35g-50g per meal) for round-the-clock nitrogen retention.",
                "Whole eggs provide essential choline, selenium, and dietary cholesterol for testosterone synthesis.",
                "Use basmati rice on heavy training days—it is rapidly absorbed with near-zero GI distress.",
                "Ensure at least 7-8 hours of sound sleep for growth hormone release and muscle restoration."
            )
        ),

        // 5. GAIN WEIGHT / HEALTHY BULK (VEGETARIAN) - The Mass Forge 2800 kcal Blueprint
        DailyDietPlan(
            id = "plan_gain_weight_veg",
            goal = FitnessGoal.GAIN_WEIGHT,
            preference = DietPreference.VEG,
            title = "Clean Indian Vegetarian Bulking Plan",
            subtitle = "5-meal clean surplus system with full-cream milk, peanut butter, sattu, and paneer.",
            totalCalories = 2817,
            totalProtein = 163f,
            totalCarbs = 332f,
            totalFat = 93f,
            totalFiber = 44f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Rich Rolled Oats with Full-Cream Milk & Peanut Butter",
                    portionDesc = "55g rolled oats + 250ml full-cream milk + 1 banana (100g) + 15g peanut butter + 1 scoop sattu/whey",
                    calories = 710,
                    protein = 43f,
                    carbs = 76f,
                    fat = 26f,
                    fiber = 7f,
                    recipeId = "snack_banana_peanut_smoothie"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Basmati Rice with Chole Masala & Curd",
                    portionDesc = "95g dry basmati rice (260g cooked) + 60g dry chole / rajma (soaked & cooked) + 150g sabzi + 150g curd + 1.4 tsp ghee",
                    calories = 761,
                    protein = 26f,
                    carbs = 126f,
                    fat = 17f,
                    fiber = 14f,
                    recipeId = "veg_chana_masala"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "Banana Sattu Shake with Toned Milk",
                    portionDesc = "200ml toned milk + 3 tbsp sattu flour / whey + 1 banana (80g) + pinch of cardamom",
                    calories = 320,
                    protein = 31f,
                    carbs = 31f,
                    fat = 8f,
                    fiber = 5f,
                    recipeId = "snack_sattu_protein_drink"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Low-Fat Paneer Bhurji with 3 Whole Wheat Rotis",
                    portionDesc = "150g paneer bhurji / Kadai Paneer + 3 rotis (90g atta) + 150g mixed sabzi + raw salad + 1 tsp ghee",
                    calories = 715,
                    protein = 52f,
                    carbs = 84f,
                    fat = 19f,
                    fiber = 12f,
                    recipeId = "veg_kadai_paneer"
                ),
                PlanMeal(
                    timing = "Meal 5 · The Quench (Pre-Bed)",
                    mealType = "Snack",
                    dishName = "Warm Full-Cream Milk with Almonds & Dates",
                    portionDesc = "250ml warm full-cream milk + 15g almonds + 2 dates",
                    calories = 311,
                    protein = 11f,
                    carbs = 15f,
                    fat = 23f,
                    fiber = 3f,
                    recipeId = null
                )
            ),
            nutritionTips = listOf(
                "Pro Tip: If you feel full but the scale isn't moving, shift calories from high-volume raw foods to nutrient-dense foods (peanut butter, ghee, full-cream milk).",
                "Meal 5 provides easy, liquid-based calories right before bed without disrupting digestion.",
                "Rice beats chapatis at high volume: 95g basmati rice provides 74g clean carbs with near-zero chewing fatigue.",
                "Target a weight gain of 0.25 to 0.5 kg per week for lean mass without excessive fat storage."
            )
        ),

        // 6. GAIN WEIGHT / HEALTHY BULK (NON-VEGETARIAN) - 3000 kcal Heavy Mass Protocol
        DailyDietPlan(
            id = "plan_gain_weight_nonveg",
            goal = FitnessGoal.GAIN_WEIGHT,
            preference = DietPreference.NON_VEG,
            title = "High-Calorie Non-Veg Strength & Mass Plan",
            subtitle = "5-meal heavyweight protocol featuring eggs, chicken, ghee rice, and milk shakes.",
            totalCalories = 3020,
            totalProtein = 175f,
            totalCarbs = 360f,
            totalFat = 98f,
            totalFiber = 38f,
            meals = listOf(
                PlanMeal(
                    timing = "Meal 1 · The Ignition (Breakfast)",
                    mealType = "Breakfast",
                    dishName = "Desi Chicken Keema Toast & 2 Boiled Eggs",
                    portionDesc = "150g chicken keema on 2 toasted multigrain slices + 2 boiled eggs + 250ml full-cream milk with banana",
                    calories = 740,
                    protein = 52f,
                    carbs = 68f,
                    fat = 26f,
                    fiber = 6f,
                    recipeId = "nonveg_chicken_keema_toast"
                ),
                PlanMeal(
                    timing = "Meal 2 · The Anvil (Lunch)",
                    mealType = "Lunch",
                    dishName = "Dhaba Egg Curry with Steamed Ghee Basmati Rice",
                    portionDesc = "3 whole eggs in rich tomato-onion gravy + 100g basmati rice + 1.5 tsp ghee + 1 bowl dal tadka + curd",
                    calories = 810,
                    protein = 38f,
                    carbs = 96f,
                    fat = 28f,
                    fiber = 8f,
                    recipeId = "nonveg_dhaba_egg_curry"
                ),
                PlanMeal(
                    timing = "Meal 3 · The Bridge (Afternoon Snack)",
                    mealType = "Snack",
                    dishName = "Whole Wheat Kolkata Egg Roll & Banana Shake",
                    portionDesc = "2 egg whole wheat roll with onions & green chutney + 1 glass blended banana peanut smoothie",
                    calories = 580,
                    protein = 28f,
                    carbs = 68f,
                    fat = 20f,
                    fiber = 6f,
                    recipeId = "nonveg_egg_roll_roti"
                ),
                PlanMeal(
                    timing = "Meal 4 · The Furnace (Dinner)",
                    mealType = "Dinner",
                    dishName = "Homestyle Chicken Curry with 3 Ghee Phulkas",
                    portionDesc = "220g chicken curry + 3 whole wheat phulkas with ghee + 1 bowl mixed sabzi + kachumber salad",
                    calories = 720,
                    protein = 52f,
                    carbs = 68f,
                    fat = 24f,
                    fiber = 8f,
                    recipeId = "nonveg_homestyle_chicken_curry"
                ),
                PlanMeal(
                    timing = "Meal 5 · The Quench (Pre-Bed)",
                    mealType = "Snack",
                    dishName = "Warm Full-Cream Milk with Almonds & Dates",
                    portionDesc = "250ml warm milk + 20g almonds + 3 medjool dates",
                    calories = 310,
                    protein = 10f,
                    carbs = 28f,
                    fat = 16f,
                    fiber = 4f,
                    recipeId = null
                )
            ),
            nutritionTips = listOf(
                "Aim for a consistent daily surplus of 400-500 kcal above maintenance to fuel heavy compound progressive overload.",
                "Pair this calorie surplus with heavy squats, deadlifts, and bench presses to channel calories into myofibrillar muscle growth.",
                "Drink 35 ml water per kg bodyweight throughout the day to ensure optimal electrolyte balance and muscle cell hydration.",
                "Consistency is paramount—eating in a surplus on weekdays and missing meals on weekends will stall progress."
            )
        )
    )

    fun getPlan(goal: FitnessGoal, preference: DietPreference): DailyDietPlan {
        return allPlans.firstOrNull { it.goal == goal && it.preference == preference }
            ?: allPlans.first()
    }

    /**
     * Scales food portion quantities and weights (g, ml, pieces, rotis) in a portion description string.
     */
    fun scalePortionDescription(desc: String, scaleFactor: Float): String {
        if (scaleFactor in 0.96f..1.04f) return desc
        val regex = Regex("""(\d+(?:\.\d+)?)\s*(g|gm|ml|tsp|tbsp|phulkas|rotis|eggs|egg)""", RegexOption.IGNORE_CASE)
        return regex.replace(desc) { matchResult ->
            val numStr = matchResult.groupValues[1]
            val unit = matchResult.groupValues[2]
            val originalVal = numStr.toFloatOrNull()
            if (originalVal != null) {
                val scaledVal = originalVal * scaleFactor
                val formatted = if (unit.equals("g", ignoreCase = true) || unit.equals("gm", ignoreCase = true) || unit.equals("ml", ignoreCase = true)) {
                    if (scaledVal >= 25f) ((scaledVal / 5f).roundToInt() * 5).toString()
                    else scaledVal.roundToInt().coerceAtLeast(1).toString()
                } else if (scaledVal < 5f && (scaledVal - scaledVal.toInt() in 0.25f..0.75f)) {
                    String.format(Locale.US, "%.1f", scaledVal)
                } else {
                    scaledVal.roundToInt().coerceAtLeast(1).toString()
                }
                "$formatted $unit"
            } else {
                matchResult.value
            }
        }
    }

    /**
     * Dynamically calibrates all meals for the full day to hit 100% of the user's
     * specific daily calorie, protein, carbohydrate, and fat targets (e.g. 150g protein).
     */
    fun getCalibratedPlan(
        goal: FitnessGoal,
        preference: DietPreference,
        targetCalories: Int,
        targetProtein: Int,
        targetCarbs: Int,
        targetFat: Int
    ): DailyDietPlan {
        val basePlan = getPlan(goal, preference)
        val baseProtein = basePlan.totalProtein.coerceAtLeast(1f)
        val baseCalories = basePlan.totalCalories.coerceAtLeast(1)
        val baseCarbs = basePlan.totalCarbs.coerceAtLeast(1f)
        val baseFat = basePlan.totalFat.coerceAtLeast(1f)

        val proteinScale = targetProtein.toFloat() / baseProtein
        val calorieScale = targetCalories.toFloat() / baseCalories.toFloat()
        val carbsScale = targetCarbs.toFloat() / baseCarbs
        val fatScale = targetFat.toFloat() / baseFat

        // Use balanced scale between protein and calories for food portion sizes
        val portionScale = (proteinScale * 0.7f + calorieScale * 0.3f)

        var accumulatedCalories = 0
        var accumulatedProtein = 0f
        var accumulatedCarbs = 0f
        var accumulatedFat = 0f

        val totalMeals = basePlan.meals.size
        val scaledMeals = basePlan.meals.mapIndexed { index, meal ->
            val isLast = index == totalMeals - 1

            val mealCal = if (isLast) {
                (targetCalories - accumulatedCalories).coerceAtLeast(50)
            } else {
                val cal = (meal.calories * calorieScale).roundToInt()
                accumulatedCalories += cal
                cal
            }

            val mealPro = if (isLast) {
                ((targetProtein - accumulatedProtein) * 10f).roundToInt() / 10f
            } else {
                val pro = ((meal.protein * proteinScale) * 10f).roundToInt() / 10f
                accumulatedProtein += pro
                pro
            }

            val mealCarb = if (isLast) {
                ((targetCarbs - accumulatedCarbs) * 10f).roundToInt() / 10f
            } else {
                val carb = ((meal.carbs * carbsScale) * 10f).roundToInt() / 10f
                accumulatedCarbs += carb
                carb
            }

            val mealFatVal = if (isLast) {
                ((targetFat - accumulatedFat) * 10f).roundToInt() / 10f
            } else {
                val fatVal = ((meal.fat * fatScale) * 10f).roundToInt() / 10f
                accumulatedFat += fatVal
                fatVal
            }

            val mealFiberVal = ((meal.fiber * calorieScale) * 10f).roundToInt() / 10f

            val scaledPortion = scalePortionDescription(meal.portionDesc, portionScale)

            meal.copy(
                portionDesc = scaledPortion,
                calories = mealCal,
                protein = mealPro.coerceAtLeast(0f),
                carbs = mealCarb.coerceAtLeast(0f),
                fat = mealFatVal.coerceAtLeast(0f),
                fiber = mealFiberVal.coerceAtLeast(0f)
            )
        }

        return basePlan.copy(
            totalCalories = targetCalories,
            totalProtein = targetProtein.toFloat(),
            totalCarbs = targetCarbs.toFloat(),
            totalFat = targetFat.toFloat(),
            subtitle = "Customized to your bodyweight & goal: Exactly completes ${targetProtein}g daily protein & ${targetCalories} kcal across ${totalMeals} meals.",
            meals = scaledMeals
        )
    }
}
