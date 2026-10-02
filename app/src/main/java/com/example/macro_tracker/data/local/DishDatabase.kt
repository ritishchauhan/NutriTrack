package com.example.macro_tracker.data.local

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * Standard nutritional database for popular Indian and international dishes,
 * verified per 100 grams. Used to automatically compute calories and macros
 * based on portion weight, or calculate composite nutrition from ingredients.
 */
@Immutable
data class KnownDish(
    val id: String,
    val name: String,
    val category: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val defaultWeightGrams: Float = 150f,
    val aliases: List<String> = emptyList()
)

@Immutable
data class KitchenIngredient(
    val id: String,
    val name: String,
    val category: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val defaultGrams: Float = 50f
)

@Immutable
data class RecipeIngredientEntry(
    val ingredient: KitchenIngredient,
    val weightGrams: Float
) {
    val calories: Int get() = ((ingredient.caloriesPer100g * weightGrams) / 100f).roundToInt()
    val protein: Float get() = ((ingredient.proteinPer100g * weightGrams * 10f) / 100f).roundToInt() / 10f
    val carbs: Float get() = ((ingredient.carbsPer100g * weightGrams * 10f) / 100f).roundToInt() / 10f
    val fat: Float get() = ((ingredient.fatPer100g * weightGrams * 10f) / 100f).roundToInt() / 10f
    val fiber: Float get() = ((ingredient.fiberPer100g * weightGrams * 10f) / 100f).roundToInt() / 10f
}

object DishDatabase {

    val popularDishes: List<KnownDish> = listOf(
        // Lentils & Dals
        KnownDish("dal_tadka", "Dal Tadka (Yellow Dal)", "Dal & Lentils", 110, 6.0f, 14.0f, 3.5f, 3.0f, 180f,
            listOf("dal", "yellow dal", "moong dal", "toor dal", "dal fry", "arhar dal", "peeli dal")),
        KnownDish("dal_makhani", "Dal Makhani", "Dal & Lentils", 155, 5.5f, 16.0f, 8.0f, 4.0f, 180f,
            listOf("makhani dal", "kaali dal", "black dal")),
        KnownDish("chana_dal", "Chana Dal Curry", "Dal & Lentils", 125, 7.0f, 17.0f, 3.5f, 4.5f, 180f,
            listOf("chana dal")),
        KnownDish("rajma_masala", "Rajma Masala (Kidney Beans)", "Dal & Lentils", 135, 7.5f, 18.0f, 4.0f, 5.0f, 200f,
            listOf("rajma", "kidney beans curry", "rajma chawal")),
        KnownDish("chole_masala", "Chole / Chickpeas Masala", "Dal & Lentils", 165, 7.0f, 22.0f, 5.5f, 5.5f, 200f,
            listOf("chole", "chana masala", "kabuli chana")),
        KnownDish("moong_khichdi", "Moong Dal Khichdi", "Rice & Grains", 120, 4.5f, 20.0f, 2.5f, 2.5f, 220f,
            listOf("khichdi", "dal khichdi")),

        // Paneer & Vegetarian Mains
        KnownDish("paneer_bhurji", "Paneer Bhurji", "Paneer & Dairy", 190, 14.0f, 4.0f, 13.0f, 1.5f, 150f,
            listOf("paneer bhurji", "scrambled paneer")),
        KnownDish("palak_paneer", "Palak Paneer", "Paneer & Dairy", 140, 8.0f, 6.0f, 10.0f, 3.0f, 200f,
            listOf("palak paneer", "spinach paneer")),
        KnownDish("paneer_butter_masala", "Paneer Butter Masala", "Paneer & Dairy", 220, 9.0f, 10.0f, 16.0f, 2.0f, 200f,
            listOf("paneer makhani", "paneer butter", "shahi paneer", "kadai paneer", "paneer tikka masala")),
        KnownDish("matar_paneer", "Matar Paneer", "Paneer & Dairy", 160, 8.5f, 11.0f, 9.5f, 2.5f, 200f,
            listOf("mutter paneer")),
        KnownDish("soya_chunks_curry", "Soya Chunks Curry", "High Protein Veg", 130, 15.0f, 10.0f, 3.5f, 4.0f, 180f,
            listOf("soya curry", "soya chunks", "nutrela curry")),
        KnownDish("mixed_veg_sabzi", "Mixed Vegetable Sabzi", "Vegetable Sabzi", 80, 2.5f, 10.0f, 3.5f, 3.5f, 150f,
            listOf("mix veg", "sabzi", "bhindi", "gobhi sabzi", "beans sabzi", "aloo gobi")),

        // Poultry, Meat & Eggs
        KnownDish("chicken_curry", "Homestyle Chicken Curry", "Chicken & Meat", 165, 17.0f, 4.0f, 9.0f, 1.0f, 220f,
            listOf("chicken curry", "tari chicken", "desi chicken", "murgh curry")),
        KnownDish("grilled_chicken", "Grilled Chicken Breast", "Chicken & Meat", 165, 31.0f, 0.0f, 3.6f, 0.0f, 150f,
            listOf("chicken breast", "boiled chicken", "tandoori chicken breast", "roasted chicken")),
        KnownDish("butter_chicken", "Butter Chicken (Murgh Makhani)", "Chicken & Meat", 215, 14.0f, 8.0f, 14.5f, 1.5f, 220f,
            listOf("butter chicken", "chicken makhani")),
        KnownDish("chicken_biryani", "Chicken Biryani", "Rice & Grains", 180, 9.5f, 22.0f, 6.0f, 1.5f, 250f,
            listOf("biryani", "murgh biryani", "dum biryani")),
        KnownDish("chicken_tikka", "Tandoori Chicken Tikka", "Chicken & Meat", 175, 24.0f, 3.0f, 7.0f, 0.5f, 150f,
            listOf("chicken tikka", "tandoori chicken")),
        KnownDish("egg_bhurji", "Desi Egg Bhurji (3 Eggs)", "Eggs", 155, 11.5f, 3.5f, 10.5f, 0.8f, 150f,
            listOf("egg bhurji", "anda bhurji", "scrambled eggs")),
        KnownDish("boiled_egg", "Boiled Egg (Whole)", "Eggs", 155, 12.6f, 1.1f, 10.6f, 0.0f, 50f,
            listOf("boiled egg", "boiled eggs", "hard boiled egg", "anda")),
        KnownDish("egg_omelette", "Egg Omelette (with onion/chilli)", "Eggs", 170, 11.0f, 2.5f, 13.0f, 0.5f, 100f,
            listOf("omelette", "omelet", "anda omelette")),
        KnownDish("dhaba_egg_curry", "Dhaba Egg Curry", "Eggs", 150, 9.5f, 6.0f, 10.0f, 1.2f, 200f,
            listOf("egg curry", "anda curry")),
        KnownDish("fish_curry", "Homestyle Fish Curry", "Fish & Seafood", 140, 16.0f, 3.5f, 7.0f, 0.8f, 200f,
            listOf("fish curry", "machli curry", "fish fry")),
        KnownDish("mutton_curry", "Mutton / Lamb Curry", "Chicken & Meat", 220, 18.0f, 3.0f, 15.0f, 0.5f, 200f,
            listOf("mutton curry", "mutton", "gosht curry")),

        // Breads & Roti
        KnownDish("wheat_roti", "Whole Wheat Roti / Phulka", "Breads", 260, 9.0f, 50.0f, 3.0f, 7.0f, 35f,
            listOf("roti", "phulka", "chapati", "wheat roti", "fulka", "roti with ghee")),
        KnownDish("plain_paratha", "Tawa Plain Paratha", "Breads", 310, 6.5f, 46.0f, 11.0f, 5.0f, 60f,
            listOf("paratha", "plain paratha", "ghee paratha")),
        KnownDish("aloo_paratha", "Aloo Paratha", "Breads", 230, 5.0f, 34.0f, 8.5f, 4.0f, 110f,
            listOf("aloo paratha", "alu paratha")),
        KnownDish("paneer_paratha", "Paneer Stuffed Paratha", "Breads", 265, 9.5f, 32.0f, 11.0f, 4.5f, 120f,
            listOf("paneer paratha")),

        // Rice & Grains
        KnownDish("steamed_rice", "Steamed Basmati Rice (Cooked)", "Rice & Grains", 130, 2.7f, 28.0f, 0.3f, 0.5f, 150f,
            listOf("rice", "basmati rice", "chawal", "white rice", "steamed rice", "boiled rice")),
        KnownDish("jeera_rice", "Jeera Basmati Rice", "Rice & Grains", 150, 2.8f, 28.0f, 3.0f, 0.8f, 160f,
            listOf("jeera rice")),
        KnownDish("veg_pulao", "Vegetable Pulao", "Rice & Grains", 140, 3.5f, 24.0f, 3.5f, 2.5f, 200f,
            listOf("veg pulao", "veg biryani", "pulao")),

        // Breakfast & Light Meals
        KnownDish("poha", "Kanda Poha", "Breakfast", 160, 3.5f, 28.0f, 4.0f, 2.2f, 150f,
            listOf("poha", "kanda poha", "batata poha")),
        KnownDish("upma", "Rava Upma with Veggies", "Breakfast", 145, 4.0f, 24.0f, 4.0f, 2.0f, 160f,
            listOf("upma", "rava upma", "sooji upma")),
        KnownDish("idli", "Steamed Idli (Rice & Urad Dal)", "Breakfast", 140, 4.5f, 28.0f, 0.8f, 2.0f, 100f,
            listOf("idli", "idlis", "steamed idli")),
        KnownDish("plain_dosa", "Plain Dosa", "Breakfast", 170, 4.0f, 30.0f, 4.0f, 1.8f, 80f,
            listOf("dosa", "plain dosa", "sada dosa")),
        KnownDish("masala_dosa", "Masala Dosa (with potato)", "Breakfast", 195, 4.2f, 32.0f, 5.8f, 2.5f, 140f,
            listOf("masala dosa")),
        KnownDish("sambar", "Vegetable Lentil Sambar", "Dal & Lentils", 65, 2.5f, 10.0f, 1.8f, 2.5f, 150f,
            listOf("sambar", "sambhar")),
        KnownDish("moong_chilla", "Moong Dal Chilla / Pesarattu", "Breakfast", 175, 10.5f, 22.0f, 5.0f, 4.0f, 100f,
            listOf("chilla", "moong chilla", "cheela", "pesarattu")),
        KnownDish("besan_chilla", "Besan Chilla", "Breakfast", 190, 8.5f, 24.0f, 6.5f, 4.5f, 100f,
            listOf("besan chilla", "besan cheela")),
        KnownDish("oats_porridge", "Rolled Oats Porridge (with milk)", "Breakfast", 95, 4.2f, 14.5f, 2.5f, 2.0f, 200f,
            listOf("oats", "oatmeal", "oats porridge")),

        // Dairy & Healthy Snacks
        KnownDish("plain_curd", "Fresh Curd / Plain Dahi", "Dairy", 60, 3.5f, 4.5f, 3.0f, 0.0f, 100f,
            listOf("curd", "dahi", "plain curd", "yogurt")),
        KnownDish("greek_yogurt", "Greek Yogurt (Plain)", "Dairy", 90, 10.0f, 4.0f, 4.0f, 0.0f, 100f,
            listOf("greek yogurt", "hung curd")),
        KnownDish("paneer_raw", "Fresh Low-Fat Paneer", "Dairy", 265, 18.5f, 4.0f, 20.0f, 0.0f, 100f,
            listOf("raw paneer", "paneer", "cottage cheese")),
        KnownDish("whey_shake", "Whey Protein Shake (1 scoop + water)", "Supplements", 380, 75.0f, 8.0f, 5.0f, 1.0f, 33f,
            listOf("whey protein", "protein shake", "whey", "protein scoop")),
        KnownDish("roasted_makhana", "Roasted Fox Nuts / Makhana", "Snacks", 360, 9.5f, 65.0f, 6.0f, 14.0f, 30f,
            listOf("makhana", "roasted makhana", "foxnuts")),
        KnownDish("sattu_sharbat", "Desi Chana Sattu Drink", "Beverages", 380, 22.0f, 60.0f, 5.5f, 15.0f, 40f,
            listOf("sattu", "sattu drink", "sattu sharbat")),
        KnownDish("peanut_butter_toast", "Whole Wheat Bread with Peanut Butter", "Snacks", 320, 12.0f, 34.0f, 16.0f, 4.0f, 65f,
            listOf("peanut butter toast", "pb toast", "bread peanut butter"))
    )

    val standardIngredients: List<KitchenIngredient> = listOf(
        KitchenIngredient("ing_paneer", "Low-Fat Paneer", "Dairy", 265, 18.5f, 4.0f, 20.0f, 0f, 100f),
        KitchenIngredient("ing_chicken", "Raw Chicken Breast", "Meat", 165, 31.0f, 0.0f, 3.6f, 0f, 150f),
        KitchenIngredient("ing_eggs_whole", "Whole Eggs", "Eggs", 155, 13.0f, 1.1f, 11.0f, 0f, 100f),
        KitchenIngredient("ing_egg_whites", "Egg Whites", "Eggs", 52, 11.0f, 0.7f, 0.2f, 0f, 100f),
        KitchenIngredient("ing_rice_dry", "Basmati Rice (Raw)", "Grains", 360, 7.0f, 80.0f, 0.6f, 1.5f, 50f),
        KitchenIngredient("ing_rice_cooked", "Steamed Rice (Cooked)", "Grains", 130, 2.7f, 28.0f, 0.3f, 0.5f, 120f),
        KitchenIngredient("ing_atta", "Whole Wheat Atta (Flour)", "Grains", 340, 12.0f, 71.0f, 1.8f, 11.0f, 40f),
        KitchenIngredient("ing_dal_dry", "Yellow Moong / Toor Dal (Raw)", "Lentils", 345, 24.0f, 60.0f, 1.5f, 10.0f, 40f),
        KitchenIngredient("ing_ghee", "Desi Ghee / Butter", "Fats & Oils", 900, 0.0f, 0.0f, 100.0f, 0f, 5f),
        KitchenIngredient("ing_oil", "Mustard / Olive / Cooking Oil", "Fats & Oils", 884, 0.0f, 0.0f, 100.0f, 0f, 5f),
        KitchenIngredient("ing_milk", "Toned Milk (Cow / Buffalo)", "Dairy", 60, 3.2f, 4.8f, 3.0f, 0f, 200f),
        KitchenIngredient("ing_curd", "Curd / Dahi", "Dairy", 60, 3.5f, 4.5f, 3.0f, 0f, 100f),
        KitchenIngredient("ing_oats", "Rolled Oats (Raw)", "Grains", 390, 13.5f, 68.0f, 7.0f, 10.0f, 40f),
        KitchenIngredient("ing_potato", "Boiled / Raw Potato", "Vegetables", 77, 2.0f, 17.0f, 0.1f, 2.2f, 100f),
        KitchenIngredient("ing_onion_tomato", "Onion & Tomato Gravy Base", "Vegetables", 35, 1.2f, 7.5f, 0.2f, 1.8f, 100f),
        KitchenIngredient("ing_mixed_veg", "Green Veggies (Bhindi, Beans, Palak)", "Vegetables", 32, 2.5f, 5.0f, 0.3f, 3.0f, 100f),
        KitchenIngredient("ing_tofu", "Firm Tofu", "Plant Protein", 76, 8.0f, 1.9f, 4.8f, 0.8f, 100f),
        KitchenIngredient("ing_soya", "Soya Chunks (Raw Dry)", "Plant Protein", 345, 52.0f, 33.0f, 0.5f, 13.0f, 35f),
        KitchenIngredient("ing_whey", "Whey Protein Powder", "Supplements", 380, 75.0f, 8.0f, 5.0f, 1.0f, 30f),
        KitchenIngredient("ing_peanuts", "Roasted Peanuts", "Nuts & Seeds", 570, 25.0f, 16.0f, 49.0f, 8.5f, 20f),
        KitchenIngredient("ing_sattu", "Roasted Chana Sattu Flour", "Grains", 380, 22.0f, 60.0f, 5.5f, 15.0f, 40f)
    )

    private val exactLookup: Map<String, KnownDish> by lazy {
        val map = HashMap<String, KnownDish>(popularDishes.size * 4)
        for (dish in popularDishes) {
            map[dish.name.lowercase().trim()] = dish
            for (alias in dish.aliases) {
                map[alias.lowercase().trim()] = dish
            }
        }
        map
    }

    /**
     * Resolves a user input string against known dishes with fast O(1) alias/name lookup and fuzzy matching.
     */
    fun findKnownDish(query: String): KnownDish? {
        val trimmed = query.trim().lowercase()
        if (trimmed.length < 2) return null

        // 1. Instant O(1) exact name or alias match
        exactLookup[trimmed]?.let { return it }

        // 2. Substring match fallback
        popularDishes.firstOrNull { dish ->
            dish.name.lowercase().contains(trimmed) || dish.aliases.any { trimmed.contains(it) || it.contains(trimmed) }
        }?.let { return it }

        return null
    }

    /**
     * Automatically calculates nutrition based on dish and weight in grams.
     */
    fun calculateDishNutrition(dish: KnownDish, weightGrams: Float): CalculatedNutrition {
        val factor = (weightGrams / 100f).coerceAtLeast(0.01f)
        return CalculatedNutrition(
            calories = (dish.caloriesPer100g * factor).roundToInt(),
            protein = ((dish.proteinPer100g * factor * 10f).roundToInt() / 10f).coerceAtLeast(0f),
            carbs = ((dish.carbsPer100g * factor * 10f).roundToInt() / 10f).coerceAtLeast(0f),
            fat = ((dish.fatPer100g * factor * 10f).roundToInt() / 10f).coerceAtLeast(0f),
            fiber = ((dish.fiberPer100g * factor * 10f).roundToInt() / 10f).coerceAtLeast(0f)
        )
    }

    /**
     * Automatically calculates composite nutrition from list of ingredients and their weights.
     */
    fun calculateCompositeNutrition(entries: List<RecipeIngredientEntry>): CalculatedNutrition {
        var totalCal = 0
        var totalPro = 0f
        var totalCarbs = 0f
        var totalFat = 0f
        var totalFiber = 0f

        for (e in entries) {
            totalCal += e.calories
            totalPro += e.protein
            totalCarbs += e.carbs
            totalFat += e.fat
            totalFiber += e.fiber
        }

        return CalculatedNutrition(
            calories = totalCal,
            protein = (totalPro * 10f).roundToInt() / 10f,
            carbs = (totalCarbs * 10f).roundToInt() / 10f,
            fat = (totalFat * 10f).roundToInt() / 10f,
            fiber = (totalFiber * 10f).roundToInt() / 10f
        )
    }
}

@Immutable
data class CalculatedNutrition(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float
)
