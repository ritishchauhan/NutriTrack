package com.example.macro_tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.macro_tracker.data.local.DishDatabase
import com.example.macro_tracker.data.local.KitchenIngredient
import com.example.macro_tracker.data.local.KnownDish
import com.example.macro_tracker.data.local.RecipeIngredientEntry
import com.example.macro_tracker.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMealDialog(
    initialMealType: String = "Breakfast",
    onDismissRequest: () -> Unit,
    onAddMeal: (
        mealType: String,
        foodName: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        details: String,
        weightGrams: Float
    ) -> Unit
) {
    val mealTypeOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var mealTypeSelection by remember { mutableStateOf(initialMealType) }

    var mealNameInput by remember { mutableStateOf("") }
    var weightGramsInput by remember { mutableStateOf("150") }

    val recognizedDish = remember(mealNameInput) {
        if (mealNameInput.trim().length >= 2) DishDatabase.findKnownDish(mealNameInput) else null
    }

    // Auto-update weight when a known dish is recognized for the first time
    LaunchedEffect(recognizedDish) {
        if (recognizedDish != null) {
            weightGramsInput = recognizedDish.defaultWeightGrams.toInt().toString()
        }
    }

    // Ingredient Builder state for custom/unknown dishes
    val addedIngredients = remember { mutableStateListOf<RecipeIngredientEntry>() }
    var selectedIngredientForAdd by remember {
        mutableStateOf<KitchenIngredient>(DishDatabase.standardIngredients.first())
    }
    var ingredientWeightGramsInput by remember { mutableStateOf("100") }
    var showIngredientDropdown by remember { mutableStateOf(false) }

    // Manual numbers override mode
    var showManualMode by remember { mutableStateOf(false) }
    var manualCaloriesInput by remember { mutableStateOf("") }
    var manualProteinInput by remember { mutableStateOf("") }
    var manualCarbsInput by remember { mutableStateOf("") }
    var manualFatInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val quickDishSuggestions = listOf(
        "Paneer Bhurji", "Dal Tadka", "Chicken Curry", "Poha", "Whole Wheat Roti",
        "Steamed Basmati Rice", "Egg Bhurji", "Moong Dal Khichdi", "Chicken Biryani"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NutritrackSurface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BrandGreenPill)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Add Custom Meal",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Auto-calculate by weight or ingredients",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Meal Category Selector Pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Meal Category",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = TextSecondary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            mealTypeOptions.forEach { type ->
                                val isSel = type.equals(mealTypeSelection, ignoreCase = true)
                                Surface(
                                    onClick = { mealTypeSelection = type },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) NutritrackDark else NutritrackBg,
                                    border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = type,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSel) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Dish Name Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Dish / Food Name",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = mealNameInput,
                            onValueChange = { mealNameInput = it },
                            placeholder = { Text("e.g. Dal Tadka, Paneer Bhurji, Chicken Curry...", color = TextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = if (recognizedDish != null) BrandGreen else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (mealNameInput.isNotBlank()) {
                                    IconButton(onClick = { mealNameInput = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = if (recognizedDish != null) BrandGreen else NutritrackDark,
                                unfocusedBorderColor = if (recognizedDish != null) BrandGreen.copy(alpha = 0.5f) else NutritrackBorderLight,
                                focusedContainerColor = NutritrackBg,
                                unfocusedContainerColor = NutritrackBg,
                                cursorColor = BrandGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick suggestion pills if empty
                        if (mealNameInput.isBlank()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                items(quickDishSuggestions) { suggestion ->
                                    Surface(
                                        onClick = { mealNameInput = suggestion },
                                        shape = RoundedCornerShape(10.dp),
                                        color = NutritrackBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Conditional Layout: CASE A: Recognized Dish vs. CASE B: Unknown Dish with Ingredient Builder
                    if (recognizedDish != null) {
                        // ==========================================
                        // CASE A: RECOGNIZED KNOWN DISH
                        // ==========================================
                        val parsedWeight = weightGramsInput.toFloatOrNull() ?: 150f
                        val calculated = remember(recognizedDish, parsedWeight) {
                            DishDatabase.calculateDishNutrition(recognizedDish, parsedWeight)
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = BrandGreenPill,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = null,
                                            tint = BrandGreenDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Recognized Dish: ${recognizedDish.name}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = BrandGreenDark
                                        )
                                    }
                                    Text(
                                        text = recognizedDish.category,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = BrandGreenDark.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Calories & macros calculate automatically as you adjust the weight.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextPrimary
                                )
                            }
                        }

                        // Weight Input Stepper Row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Portion Weight (grams)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        val cur = weightGramsInput.toFloatOrNull() ?: 100f
                                        val next = (cur - 25f).coerceAtLeast(10f)
                                        weightGramsInput = next.roundToInt().toString()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = NutritrackBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("-25g", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    }
                                }

                                OutlinedTextField(
                                    value = weightGramsInput,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() || it == '.' }
                                        weightGramsInput = filtered
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    ),
                                    suffix = {
                                        Text(
                                            "grams",
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreenDark,
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = BrandGreen,
                                        unfocusedBorderColor = NutritrackBorderLight,
                                        focusedContainerColor = NutritrackBg,
                                        unfocusedContainerColor = NutritrackBg,
                                        cursorColor = BrandGreen
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    onClick = {
                                        val cur = weightGramsInput.toFloatOrNull() ?: 100f
                                        val next = (cur + 25f).coerceAtMost(1000f)
                                        weightGramsInput = next.roundToInt().toString()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = NutritrackBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+25g", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    }
                                }
                            }

                            // Quick weight preset pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(100, 150, 200, 250, 300).forEach { grams ->
                                    val isCur = weightGramsInput == grams.toString()
                                    Surface(
                                        onClick = { weightGramsInput = grams.toString() },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCur) BrandGreenPill else NutritrackBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCur) BrandGreen else NutritrackBorderLight
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${grams}g",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (isCur) BrandGreenDark else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Auto-Calculated Nutrition Display Badges
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = NutritrackDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Auto-Calculated Nutrition",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandGreen
                                    ) {
                                        Text(
                                            text = "${calculated.calories} kcal",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NutritionBadge(label = "Protein", value = "${calculated.protein}g", color = MacroProtein, modifier = Modifier.weight(1f))
                                    NutritionBadge(label = "Carbs", value = "${calculated.carbs}g", color = MacroCarbs, modifier = Modifier.weight(1f))
                                    NutritionBadge(label = "Fat", value = "${calculated.fat}g", color = MacroFat, modifier = Modifier.weight(1f))
                                    if (calculated.fiber > 0f) {
                                        NutritionBadge(label = "Fiber", value = "${calculated.fiber}g", color = Color(0xFF00B894), modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                    } else if (mealNameInput.isNotBlank()) {
                        // ==========================================
                        // CASE B: UNKNOWN DISH -> INGREDIENT BUILDER
                        // ==========================================
                        val compositeNutrition = remember(addedIngredients) {
                            DishDatabase.calculateCompositeNutrition(addedIngredients)
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF39C12),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Custom Recipe / Unknown Dish",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFFB7791F)
                                    )
                                    Text(
                                        text = "We don't have a standard profile for \"$mealNameInput\". Add the ingredients and their weights below to calculate exact calories & macros.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Added Ingredients List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recipe Ingredients (${addedIngredients.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = TextPrimary
                                )
                                if (addedIngredients.isNotEmpty()) {
                                    val totalGrams = addedIngredients.sumOf { it.weightGrams.toDouble() }.toInt()
                                    Text(
                                        text = "Total: ${totalGrams}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (addedIngredients.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = NutritrackBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No ingredients added yet. Pick an ingredient below and enter grams to calculate nutrition.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = TextMuted,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                addedIngredients.forEachIndexed { index, entry ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = NutritrackSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.ingredient.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${entry.weightGrams.toInt()}g • ${entry.calories} kcal • P: ${entry.protein}g • C: ${entry.carbs}g • F: ${entry.fat}g",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = TextSecondary
                                                )
                                            }

                                            IconButton(
                                                onClick = { addedIngredients.removeAt(index) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFFE74C3C),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Ingredient Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = NutritrackBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Add Ingredient to Recipe",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = TextPrimary
                                )

                                // Ingredient Selection Chips / Dropdown
                                Box {
                                    Surface(
                                        onClick = { showIngredientDropdown = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = NutritrackSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = selectedIngredientForAdd.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${selectedIngredientForAdd.caloriesPer100g} kcal / 100g • P ${selectedIngredientForAdd.proteinPer100g}g",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = TextSecondary
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = TextPrimary
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showIngredientDropdown,
                                        onDismissRequest = { showIngredientDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 280.dp)
                                    ) {
                                        DishDatabase.standardIngredients.forEach { ing ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(ing.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            "${ing.caloriesPer100g} kcal • P: ${ing.proteinPer100g}g • C: ${ing.carbsPer100g}g • F: ${ing.fatPer100g}g (per 100g)",
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedIngredientForAdd = ing
                                                    ingredientWeightGramsInput = ing.defaultGrams.toInt().toString()
                                                    showIngredientDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Weight in grams & Add Button Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ingredientWeightGramsInput,
                                        onValueChange = { input ->
                                            ingredientWeightGramsInput = input.filter { it.isDigit() || it == '.' }
                                        },
                                        label = { Text("Weight (g)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            val wt = ingredientWeightGramsInput.toFloatOrNull() ?: 50f
                                            if (wt > 0f) {
                                                addedIngredients.add(
                                                    RecipeIngredientEntry(
                                                        ingredient = selectedIngredientForAdd,
                                                        weightGrams = wt
                                                    )
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BrandGreenDark,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Composite Nutrition Banner if ingredients exist
                        if (addedIngredients.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = NutritrackDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recipe Composite Nutrition",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BrandGreen
                                        ) {
                                            Text(
                                                text = "${compositeNutrition.calories} kcal",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NutritionBadge(label = "Protein", value = "${compositeNutrition.protein}g", color = MacroProtein, modifier = Modifier.weight(1f))
                                        NutritionBadge(label = "Carbs", value = "${compositeNutrition.carbs}g", color = MacroCarbs, modifier = Modifier.weight(1f))
                                        NutritionBadge(label = "Fat", value = "${compositeNutrition.fat}g", color = MacroFat, modifier = Modifier.weight(1f))
                                        if (compositeNutrition.fiber > 0f) {
                                            NutritionBadge(label = "Fiber", value = "${compositeNutrition.fiber}g", color = Color(0xFF00B894), modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Optional Manual Override Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualMode = !showManualMode }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showManualMode) "Hide manual macro inputs" else "Or type exact macros manually",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = BrandGreenDark
                        )
                        Icon(
                            imageVector = if (showManualMode) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showManualMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualCaloriesInput,
                                    onValueChange = { manualCaloriesInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Calories (kcal)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualProteinInput,
                                    onValueChange = { manualProteinInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Protein (g)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualCarbsInput,
                                    onValueChange = { manualCarbsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Carbs (g)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualFatInput,
                                    onValueChange = { manualFatInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Fat (g)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Button
                val canSubmit = when {
                    showManualMode -> mealNameInput.isNotBlank() && (manualCaloriesInput.isNotBlank() || manualProteinInput.isNotBlank())
                    recognizedDish != null -> mealNameInput.isNotBlank() && (weightGramsInput.toFloatOrNull() ?: 0f) > 0f
                    else -> mealNameInput.isNotBlank() && addedIngredients.isNotEmpty()
                }

                Button(
                    onClick = {
                        val cleanName = mealNameInput.trim()
                        if (showManualMode) {
                            val pro = manualProteinInput.toFloatOrNull() ?: 0f
                            val carb = manualCarbsInput.toFloatOrNull() ?: 0f
                            val fat = manualFatInput.toFloatOrNull() ?: 0f
                            val cal = manualCaloriesInput.toIntOrNull() ?: ((pro * 4f) + (carb * 4f) + (fat * 9f)).roundToInt()
                            val wt = weightGramsInput.toFloatOrNull() ?: 100f
                            onAddMeal(
                                mealTypeSelection,
                                cleanName,
                                cal,
                                pro,
                                carb,
                                fat,
                                0f,
                                "Custom Manual Meal • P ${pro.toInt()}g • C ${carb.toInt()}g • F ${fat.toInt()}g",
                                wt
                            )
                        } else if (recognizedDish != null) {
                            val wt = weightGramsInput.toFloatOrNull() ?: 150f
                            val calc = DishDatabase.calculateDishNutrition(recognizedDish, wt)
                            onAddMeal(
                                mealTypeSelection,
                                recognizedDish.name,
                                calc.calories,
                                calc.protein,
                                calc.carbs,
                                calc.fat,
                                calc.fiber,
                                "Standard Dish (${recognizedDish.category}) • ${wt.toInt()}g portion",
                                wt
                            )
                        } else if (addedIngredients.isNotEmpty()) {
                            val composite = DishDatabase.calculateCompositeNutrition(addedIngredients)
                            val totalWt = addedIngredients.sumOf { it.weightGrams.toDouble() }.toFloat()
                            val ingredientListDesc = addedIngredients.joinToString(", ") { "${it.ingredient.name} ${it.weightGrams.toInt()}g" }
                            onAddMeal(
                                mealTypeSelection,
                                cleanName,
                                composite.calories,
                                composite.protein,
                                composite.carbs,
                                composite.fat,
                                composite.fiber,
                                "Ingredients: $ingredientListDesc",
                                totalWt
                            )
                        }
                    },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White,
                        disabledContainerColor = NutritrackDark.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            recognizedDish != null -> "Add ${recognizedDish.name} to $mealTypeSelection"
                            addedIngredients.isNotEmpty() -> "Add ${mealNameInput.trim()} (${DishDatabase.calculateCompositeNutrition(addedIngredients).calories} kcal) to $mealTypeSelection"
                            mealNameInput.isNotBlank() -> "Add Ingredients to Calculate Calories"
                            else -> "Enter Dish Name"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = Color.White
            )
        }
    }
}
