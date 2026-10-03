package com.example.macro_tracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.data.local.DietPlansData
import com.example.macro_tracker.data.local.RecipesData
import com.example.macro_tracker.data.model.*
import com.example.macro_tracker.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlansScreen(
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedWeight by profileViewModel.userWeightKg.collectAsState()
    val savedHeight by profileViewModel.userHeightCm.collectAsState()
    val savedGoalStr by profileViewModel.userFitnessGoal.collectAsState()
    val savedDietStr by profileViewModel.dietaryPreference.collectAsState()

    // Store user text input independently with rememberSaveable to allow effortless typing, clearing and no jumping
    var weightInput by rememberSaveable {
        mutableStateOf(if (savedWeight > 0f) savedWeight.toInt().toString() else "")
    }
    var heightInput by rememberSaveable {
        mutableStateOf(if (savedHeight > 0f) savedHeight.toInt().toString() else "")
    }

    val parsedWeight = weightInput.toFloatOrNull()
    val parsedHeight = heightInput.toFloatOrNull()

    // Humanly valid range check to avoid saving mid-typing or clamping to 25/250
    val hasValidMetrics = parsedWeight != null && parsedWeight in 25f..250f &&
                          parsedHeight != null && parsedHeight in 60f..250f

    // Working values for preview & ladder: use entered values if valid, else saved profile or gentle defaults
    val weightKg = parsedWeight ?: (if (savedWeight > 0f) savedWeight else 70f)
    val heightCm = parsedHeight ?: (if (savedHeight > 0f) savedHeight else 170f)
    val heightM = (heightCm / 100f).coerceAtLeast(0.5f)
    val bmi = (weightKg / (heightM * heightM)).coerceIn(10f, 60f)

    val initialGoal = try {
        FitnessGoal.valueOf(savedGoalStr)
    } catch (e: Exception) {
        FitnessGoal.LOSE_WEIGHT
    }
    var selectedGoal by remember(savedGoalStr) { mutableStateOf(initialGoal) }

    val initialDiet = if (savedDietStr.equals("Vegetarian", ignoreCase = true) || savedDietStr.equals("Veg", ignoreCase = true)) {
        DietPreference.VEG
    } else {
        DietPreference.NON_VEG
    }
    var selectedDiet by remember(savedDietStr) { mutableStateOf(initialDiet) }

    // Active Recipe for Detail Sheet
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var selectedMealTypeForRecipe by remember { mutableStateOf("Breakfast") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Scientific calculations directly based on The Vegetarian Forge Blueprint & BMI-calibrated hydration
    val targetCalories = remember(weightKg, selectedGoal) {
        DietPlansData.calculateCalorieTarget(weightKg, selectedGoal)
    }
    val targetProtein = remember(weightKg, selectedGoal) {
        DietPlansData.calculateProteinTarget(weightKg, selectedGoal)
    }
    val targetFat = remember(targetCalories, selectedGoal) {
        DietPlansData.calculateFatTarget(targetCalories, selectedGoal)
    }
    val targetCarbs = remember(targetCalories, targetProtein, targetFat) {
        DietPlansData.calculateCarbsTarget(targetCalories, targetProtein, targetFat)
    }
    // Set daily water intake calibrated directly as per BMI
    val targetWater = remember(weightKg, bmi) {
        DietPlansData.calculateWaterTarget(weightKg, bmi)
    }

    // Automatically set target calories, protein & BMI-based water goal ONLY when valid user inputs exist
    LaunchedEffect(parsedWeight, parsedHeight, selectedGoal, targetCalories, targetProtein, targetWater) {
        if (hasValidMetrics) {
            profileViewModel.saveBodyStats(parsedWeight!!, parsedHeight!!, selectedGoal.name)
            profileViewModel.saveGoals(
                calories = targetCalories,
                protein = targetProtein,
                carbs = targetCarbs,
                fat = targetFat,
                water = targetWater
            )
        }
    }

    val currentPlan = remember(selectedGoal, selectedDiet, targetCalories, targetProtein, targetCarbs, targetFat) {
        DietPlansData.getCalibratedPlan(
            goal = selectedGoal,
            preference = selectedDiet,
            targetCalories = targetCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat
        )
    }

    // Modal for displaying recipe instructions & adding to food log
    if (selectedRecipeForDetail != null) {
        RecipeDetailModal(
            recipe = selectedRecipeForDetail!!,
            defaultMealType = selectedMealTypeForRecipe,
            onDismissRequest = { selectedRecipeForDetail = null },
            onAddToMeals = { mealType, recipe ->
                foodViewModel.quickAddMeal(
                    mealType = mealType,
                    foodName = recipe.title,
                    calories = recipe.calories,
                    protein = recipe.protein,
                    carbs = recipe.carbs,
                    fat = recipe.fat,
                    fiber = recipe.fiber,
                    details = "Indian Kitchen Recipe • ${recipe.prepTimeMinutes + recipe.cookTimeMinutes}m cook"
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added ${recipe.title} to today's $mealType!")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NutritrackBg,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onNavigateBack,
                        shape = CircleShape,
                        color = NutritrackSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diet Plans",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Personalized Indian Kitchen Nutrition",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }

                    // Browse 100+ Recipes Shortcut
                    Surface(
                        onClick = onNavigateToRecipes,
                        shape = RoundedCornerShape(14.dp),
                        color = BrandGreenPill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100+ Recipes",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = BrandGreenDark
                            )
                        }
                    }
                }
            }

            // Step 1: Weight, Height & BMI Calculator Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = NutritrackSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Scale,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Step 1: Your Body Metrics",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Enter weight and height to calculate your real-time BMI",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Input fields for Weight and Height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '.' }
                                    weightInput = filtered
                                },
                                label = { Text("Weight (kg)") },
                                placeholder = { Text("e.g. 70", color = TextMuted) },
                                trailingIcon = {
                                    if (weightInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = { weightInput = "" },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear weight",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    color = TextPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandGreen,
                                    unfocusedBorderColor = NutritrackBorderLight,
                                    focusedContainerColor = NutritrackBg,
                                    unfocusedContainerColor = NutritrackBg,
                                    cursorColor = BrandGreen,
                                    focusedLabelColor = BrandGreen,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedPlaceholderColor = TextMuted,
                                    unfocusedPlaceholderColor = TextMuted
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '.' }
                                    heightInput = filtered
                                },
                                label = { Text("Height (cm)") },
                                placeholder = { Text("e.g. 170", color = TextMuted) },
                                trailingIcon = {
                                    if (heightInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = { heightInput = "" },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear height",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    color = TextPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = BrandGreen,
                                    unfocusedBorderColor = NutritrackBorderLight,
                                    focusedContainerColor = NutritrackBg,
                                    unfocusedContainerColor = NutritrackBg,
                                    cursorColor = BrandGreen,
                                    focusedLabelColor = BrandGreen,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedPlaceholderColor = TextMuted,
                                    unfocusedPlaceholderColor = TextMuted
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic BMI Result Badge & Gauge
                        BmiDisplayBadge(bmi = bmi)
                    }
                }
            }

            // Step 2: What do you want to do? (Fitness Goal Selection)
            item {
                Text(
                    text = "Step 2: Choose Your Goal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select what you want to achieve with your daily diet",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FitnessGoal.values().forEach { goal ->
                        val isSelected = goal == selectedGoal
                        GoalSelectionCard(
                            goal = goal,
                            isSelected = isSelected,
                            onClick = { selectedGoal = goal }
                        )
                    }
                }
            }

            // Step 3: Your Target Goals & Daily Protein (The Vegetarian Forge Blueprint)
            item {
                TargetNutritionCard(
                    targetCalories = targetCalories,
                    targetProtein = targetProtein,
                    targetCarbs = targetCarbs,
                    targetFat = targetFat,
                    targetWater = targetWater,
                    weightKg = weightKg,
                    bmi = bmi,
                    goal = selectedGoal,
                    onApplyGoals = {
                        profileViewModel.saveGoals(
                            calories = targetCalories,
                            protein = targetProtein,
                            carbs = targetCarbs,
                            fat = targetFat,
                            water = targetWater
                        )
                        profileViewModel.saveBodyStats(weightKg, heightCm, selectedGoal.name)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Target set: $targetCalories kcal & ${targetProtein}g daily protein saved to your tracker!")
                        }
                    }
                )
            }

            // Step 4: Food Preference (Veg vs Non-Veg) - Full Width Screen Layout
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Step 4: Choose Food Preference",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your daily dietary style for customized Indian kitchen recipes",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Vegetarian Option Card
                        Surface(
                            onClick = {
                                selectedDiet = DietPreference.VEG
                                profileViewModel.saveDietaryPreference(DietPreference.VEG.displayName)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedDiet == DietPreference.VEG) BrandGreenPill else NutritrackSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedDiet == DietPreference.VEG) 2.dp else 1.dp,
                                if (selectedDiet == DietPreference.VEG) BrandGreen else NutritrackBorderLight
                            ),
                            shadowElevation = if (selectedDiet == DietPreference.VEG) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Indian Green Veg Dot Badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .border(1.5.dp, Color(0xFF27AE60), RoundedCornerShape(6.dp))
                                            .padding(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF27AE60))
                                        )
                                    }

                                    if (selectedDiet == DietPreference.VEG) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = BrandGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Vegetarian",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (selectedDiet == DietPreference.VEG) BrandGreenDark else TextPrimary
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "Paneer, Dals, Sattu, Curd & Legumes",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Non-Vegetarian Option Card
                        Surface(
                            onClick = {
                                selectedDiet = DietPreference.NON_VEG
                                profileViewModel.saveDietaryPreference(DietPreference.NON_VEG.displayName)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedDiet == DietPreference.NON_VEG) NutritrackDark else NutritrackSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedDiet == DietPreference.NON_VEG) 2.dp else 1.dp,
                                if (selectedDiet == DietPreference.NON_VEG) NutritrackDark else NutritrackBorderLight
                            ),
                            shadowElevation = if (selectedDiet == DietPreference.NON_VEG) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Indian Red Non-Veg Dot Badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .border(1.5.dp, Color(0xFFE74C3C), RoundedCornerShape(6.dp))
                                            .padding(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE74C3C))
                                        )
                                    }

                                    if (selectedDiet == DietPreference.NON_VEG) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Non-Veg",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (selectedDiet == DietPreference.NON_VEG) Color.White else TextPrimary
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "Chicken, Egg Whites, Fish & Lean Meats",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (selectedDiet == DietPreference.NON_VEG) Color.White.copy(alpha = 0.75f) else TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Plan Summary Banner (Step 4 Recommended Plan)
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = NutritrackDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGreen.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECOMMENDED DIET PLAN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = BrandGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandGreen
                            ) {
                                Text(
                                    text = "${currentPlan.totalCalories} kcal/day",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentPlan.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentPlan.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = Color.White.copy(alpha = 0.82f)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Target Macros Row with elegant cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlanMacroStat(label = "Protein", value = "${currentPlan.totalProtein.toInt()}g", color = MacroProtein, modifier = Modifier.weight(1f))
                            PlanMacroStat(label = "Carbs", value = "${currentPlan.totalCarbs.toInt()}g", color = MacroCarbs, modifier = Modifier.weight(1f))
                            PlanMacroStat(label = "Fats", value = "${currentPlan.totalFat.toInt()}g", color = MacroFat, modifier = Modifier.weight(1f))
                            PlanMacroStat(label = "Fiber", value = "${currentPlan.totalFiber.toInt()}g", color = Color(0xFF00B894), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Nutrition Tips Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BrandGreenPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Indian Kitchen Nutrition Rules",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = BrandGreenDark
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        currentPlan.nutritionTips.forEach { tip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenDark,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Meal Plan Section Title & Goal Fulfillment Banner
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Full Day Meals (${currentPlan.meals.size} items)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Click 'Recipe' for instructions",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }

                    // Goal Calibration Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandGreenPill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Goal Calibrated: Completes 100% of Daily Targets",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = BrandGreenDark
                                )
                                Text(
                                    text = "Target Protein: ${targetProtein}g • Target Calories: ${targetCalories} kcal. All ${currentPlan.meals.size} suggested meals add up to complete your daily requirements with scaled portion sizes.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Meals List with individual "Recipe" button below each meal
            items(
                items = currentPlan.meals,
                key = { it.timing + "_" + it.dishName },
                contentType = { "plan_meal_item" }
            ) { meal ->
                MealCardWithRecipeButton(
                    meal = meal,
                    onViewRecipe = {
                        val recipe = if (meal.recipeId != null) {
                            RecipesData.findRecipeById(meal.recipeId)
                        } else {
                            // Synthesize a quick recipe model if not pre-linked
                            Recipe(
                                id = "synthesized_${meal.dishName.hashCode()}",
                                title = meal.dishName,
                                isVeg = selectedDiet == DietPreference.VEG,
                                category = RecipeCategory.BREAKFAST,
                                prepTimeMinutes = 5,
                                cookTimeMinutes = 5,
                                calories = meal.calories,
                                protein = meal.protein,
                                carbs = meal.carbs,
                                fat = meal.fat,
                                fiber = meal.fiber,
                                ingredients = listOf(meal.portionDesc, "Pinch of rock salt / herbs"),
                                instructions = listOf(
                                    "Clean and assemble ingredients according to portion: ${meal.portionDesc}.",
                                    "Consume freshly prepared as recommended in your Indian kitchen meal plan."
                                ),
                                tags = listOf("Healthy", "Quick")
                            )
                        }

                        if (recipe != null) {
                            selectedRecipeForDetail = recipe
                            selectedMealTypeForRecipe = meal.mealType
                        }
                    },
                    onQuickLogMeal = {
                        foodViewModel.quickAddMeal(
                            mealType = meal.mealType,
                            foodName = meal.dishName,
                            calories = meal.calories,
                            protein = meal.protein,
                            carbs = meal.carbs,
                            fat = meal.fat,
                            fiber = meal.fiber,
                            details = meal.portionDesc
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Logged ${meal.dishName} to ${meal.mealType}!")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Visual BMI category badge and health meter.
 */
@Composable
fun BmiDisplayBadge(bmi: Float) {
    val (categoryText, categoryColor, categoryDesc) = when {
        bmi < 18.5f -> Triple("Underweight", Color(0xFFF39C12), "Focus on calorie surplus with protein-rich lentils, paneer, nuts & ghee.")
        bmi in 18.5f..24.9f -> Triple("Normal Weight", BrandGreen, "Ideal healthy weight! Maintain muscle mass and metabolic fitness.")
        bmi in 25.0f..29.9f -> Triple("Overweight", Color(0xFFE67E22), "A moderate calorie deficit with high-protein Indian meals will shed fat.")
        else -> Triple("Obese", Color(0xFFE74C3C), "Focus on high-fiber, low glycemic index foods with regular brisk walks.")
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NutritrackBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Calculated BMI",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", bmi),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kg/m²",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = categoryText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BMI Range Progress Line
            val normalizedProgress = ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFF39C12),
                                BrandGreen,
                                Color(0xFFE67E22),
                                Color(0xFFE74C3C)
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = categoryDesc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
    }
}

/**
 * Goal selection card with icon and description.
 */
@Composable
fun GoalSelectionCard(
    goal: FitnessGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (goal) {
        FitnessGoal.LOSE_WEIGHT -> Icons.AutoMirrored.Rounded.DirectionsRun
        FitnessGoal.GAIN_MUSCLE -> Icons.Rounded.FitnessCenter
        FitnessGoal.GAIN_WEIGHT -> Icons.AutoMirrored.Rounded.TrendingUp
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) BrandGreenPill else NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) BrandGreen else NutritrackBorderLight
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) BrandGreen else NutritrackBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isSelected) BrandGreenDark else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = goal.shortDesc,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Meal card with comprehensive macros and prominent "Recipe" button below each meal.
 */
@Composable
fun MealCardWithRecipeButton(
    meal: PlanMeal,
    onViewRecipe: () -> Unit,
    onQuickLogMeal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NutritrackSurface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Meal Timing & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.timing,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BrandGreenDark
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NutritrackBg
                ) {
                    Text(
                        text = meal.mealType,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dish Name
            Text(
                text = meal.dishName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Portion description
            Text(
                text = meal.portionDesc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Macros Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MealMacroBadge(label = "Cal", value = "${meal.calories}", color = BrandGreen)
                MealMacroBadge(label = "Pro", value = "${meal.protein.toInt()}g", color = MacroProtein)
                MealMacroBadge(label = "Carb", value = "${meal.carbs.toInt()}g", color = MacroCarbs)
                MealMacroBadge(label = "Fat", value = "${meal.fat.toInt()}g", color = MacroFat)
                if (meal.fiber > 0f) {
                    MealMacroBadge(label = "Fib", value = "${meal.fiber.toInt()}g", color = Color(0xFF00B894))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recipe Button below each meal!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // View Recipe Button
                Button(
                    onClick = onViewRecipe,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "How to Make (Recipe)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }

                // Quick Log to Daily Meals
                OutlinedButton(
                    onClick = onQuickLogMeal,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log Meal",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun MealMacroBadge(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = color
            )
        }
    }
}

@Composable
fun PlanMacroStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = color
        )
    }
}

/**
 * Personalized Target Goals card calculating the 16-Rung Calorie Ladder & Daily Protein Goal
 * from "The Vegetarian Forge: The Complete Indian Diet Blueprint" by Coach Dinesh Dudeja.
 */
@Composable
fun TargetNutritionCard(
    targetCalories: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFat: Int,
    targetWater: Float,
    weightKg: Float,
    bmi: Float,
    goal: FitnessGoal,
    onApplyGoals: () -> Unit
) {
    val (calRuleText, proteinRuleText) = when (goal) {
        FitnessGoal.LOSE_WEIGHT -> Pair(
            "Fat Loss: 29 kcal/kg (moderate deficit to protect lean muscle)",
            "High Satiety: 1.9 g/kg (preserves muscle mass during deficit)"
        )
        FitnessGoal.GAIN_MUSCLE -> Pair(
            "Hypertrophy: 35 kcal/kg (lean surplus for muscle synthesis)",
            "Hypertrophy: 2.1 g/kg (anchored with leucine-rich dense protein)"
        )
        FitnessGoal.GAIN_WEIGHT -> Pair(
            "Clean Surplus: 38 kcal/kg (clean caloric surplus for mass)",
            "Growth Support: 1.8 g/kg (paired with high-density healthy carbs & fats)"
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step 3: Target Goals & Calorie Ladder",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scientifically calculated based on your weight, height, and activity level",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Calorie Ladder Button / Card & Daily Protein Card placed right below Step 3 (matching Step 4 Veg/Non-Veg layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Card: Calorie Ladder Rung
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandGreenPill,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandGreen),
                shadowElevation = 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandGreen
                        ) {
                            Text(
                                text = "Calorie Ladder",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$targetCalories",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = BrandGreenDark,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Rung: $targetCalories • 1500–3000 Scale",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = BrandGreenDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = calRuleText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextMuted,
                        lineHeight = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Card: Daily Protein Anchor
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NutritrackSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                shadowElevation = 1.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MacroProtein.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Daily Protein",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MacroProtein,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = MacroProtein,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$targetProtein",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "g/day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MacroProtein,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val proteinRatio = if (weightKg > 0) String.format(Locale.US, "%.1f", targetProtein / weightKg) else "2.0"
                    Text(
                        text = "Anchor: ${proteinRatio}g / kg bodyweight",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = MacroProtein,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = proteinRuleText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextMuted,
                        lineHeight = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Info Card (Carbs, Fats, Water & Save Button)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NutritrackSurface,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Sub-Macro Split (Carbs, Fats, Water)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetMiniChip(label = "Carbs", value = "${targetCarbs}g", color = MacroCarbs, modifier = Modifier.weight(1f))
                    TargetMiniChip(label = "Fats", value = "${targetFat}g", color = MacroFat, modifier = Modifier.weight(1f))
                    TargetMiniChip(label = "Water", value = "${targetWater}L", color = Color(0xFF0984E3), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BMI-calibrated hydration badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0984E3).copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0984E3).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💧 Water (BMI ${String.format(Locale.US, "%.1f", bmi)}): ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF0984E3)
                        )
                        Text(
                            text = "${targetWater}L/day • ${DietPlansData.getWaterBmiExplanation(bmi)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Auto-Synced Indicator & Manual Sync Button
                Surface(
                    onClick = onApplyGoals,
                    shape = RoundedCornerShape(14.dp),
                    color = BrandGreenPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Targets Set & Synced with Tracker",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = BrandGreenDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TargetMiniChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NutritrackBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = color
            )
        }
    }
}

