package com.example.macro_tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.remote.Nutriments
import com.example.macro_tracker.data.remote.Product
import com.example.macro_tracker.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.example.macro_tracker.data.local.DietPlansData
import com.example.macro_tracker.data.model.DailyDietPlan
import com.example.macro_tracker.data.model.DietPreference
import com.example.macro_tracker.data.model.FitnessGoal
import com.example.macro_tracker.data.model.PlanMeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDietPlans: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dailyLogs by foodViewModel.dailyFoodLogs.collectAsState()
    val filteredLogs by foodViewModel.filteredFoodLogs.collectAsState()
    val selectedDate by foodViewModel.selectedDate.collectAsState()
    val activeFilter by foodViewModel.mealFilter.collectAsState()
    val calorieGoal by profileViewModel.calorieGoal.collectAsState()
    val proteinGoal by profileViewModel.proteinGoal.collectAsState()
    val carbsGoal by profileViewModel.carbsGoal.collectAsState()
    val fatGoal by profileViewModel.fatGoal.collectAsState()
    val userDietPref by profileViewModel.dietaryPreference.collectAsState()
    val userFitnessGoalStr by profileViewModel.userFitnessGoal.collectAsState()

    val userDiet = if (userDietPref.equals("Veg", ignoreCase = true)) DietPreference.VEG else DietPreference.NON_VEG
    val userGoal = try {
        FitnessGoal.valueOf(userFitnessGoalStr)
    } catch (e: Exception) {
        FitnessGoal.LOSE_WEIGHT
    }

    val suggestedPlan = remember(userDiet, userGoal, calorieGoal, proteinGoal, carbsGoal, fatGoal) {
        DietPlansData.getCalibratedPlan(
            goal = userGoal,
            preference = userDiet,
            targetCalories = calorieGoal,
            targetProtein = proteinGoal,
            targetCarbs = carbsGoal,
            targetFat = fatGoal
        )
    }

    val searchResults by foodViewModel.searchResults.collectAsState()
    val isLoading by foodViewModel.isLoading.collectAsState()
    val isBarcodeLoading by foodViewModel.isBarcodeLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMealForAdd by remember { mutableStateOf("Breakfast") }
    var showCameraScanner by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }
    var showScannedDetailModal by remember { mutableStateOf(false) }
    var activeScannedProduct by remember { mutableStateOf<Product?>(null) }

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<FoodLogEntity?>(null) }
    var mealToAdjustQuantity by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val totalCalories = remember(dailyLogs) { dailyLogs.sumOf { it.calories } }
    val caloriesRemaining = remember(totalCalories, calorieGoal) { (calorieGoal - totalCalories).coerceAtLeast(0) }
    val progress = remember(totalCalories, calorieGoal) {
        if (calorieGoal > 0) (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    val formattedDate = selectedDate.format(dateFormatter)

    val filterOptions = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")
    val mealTypeOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    // Camera Barcode Scanner Modal
    if (showCameraScanner) {
        BarcodeCameraScannerModal(
            onDismissRequest = { showCameraScanner = false },
            onBarcodeScanned = { barcode ->
                showCameraScanner = false
                lastScannedBarcode = barcode
                foodViewModel.lookupBarcode(barcode) { product, error ->
                    if (product != null) {
                        activeScannedProduct = product
                        showScannedDetailModal = true
                    } else {
                        // Product not found in database: allow entering/confirming details
                        activeScannedProduct = Product(
                            product_name = "Food Item ($barcode)",
                            brands = "Packaged Product",
                            nutriments = Nutriments(
                                energy_kcal = 180.0,
                                proteins = 6.0,
                                carbohydrates = 22.0,
                                fat = 5.0,
                                fiber = 2.0
                            )
                        )
                        showScannedDetailModal = true
                    }
                }
            }
        )
    }

    // Barcode Lookup Loading Dialog
    if (isBarcodeLoading) {
        AlertDialog(
            onDismissRequest = { foodViewModel.clearScannedProduct() },
            shape = RoundedCornerShape(24.dp),
            containerColor = NutritrackSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = BrandGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Analyzing Barcode",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Detecting food and nutritional facts for barcode: $lastScannedBarcode...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = OutfitFontFamily,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        foodViewModel.clearScannedProduct()
                    }
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Detected Food Detail Review Modal (with Add to Daily Meals & Discard buttons)
    if (showScannedDetailModal && activeScannedProduct != null) {
        ScannedFoodDetailModal(
            product = activeScannedProduct!!,
            scannedBarcode = lastScannedBarcode,
            onDismissRequest = {
                showScannedDetailModal = false
                activeScannedProduct = null
                foodViewModel.clearScannedProduct()
            },
            onAddToMeals = { mealType, servingMultiplier, customName ->
                foodViewModel.addScannedProductToMeals(
                    product = activeScannedProduct!!,
                    mealType = mealType,
                    servingMultiplier = servingMultiplier,
                    customName = customName
                )
                showScannedDetailModal = false
                activeScannedProduct = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added $customName to $mealType!")
                }
            },
            onDiscard = {
                showScannedDetailModal = false
                activeScannedProduct = null
                foodViewModel.clearScannedProduct()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Barcode scan discarded")
                }
            }
        )
    }

    // Universal Add Meal Dialog with Smart Dish Recognition & Ingredient Builder
    if (showQuickAddDialog) {
        com.example.macro_tracker.ui.components.AddCustomMealDialog(
            initialMealType = selectedMealForAdd,
            onDismissRequest = { showQuickAddDialog = false },
            onAddMeal = { mealType, name, cal, pro, carb, fat, fiber, details, wt ->
                foodViewModel.quickAddMeal(
                    mealType = mealType,
                    foodName = name,
                    calories = cal,
                    protein = pro,
                    carbs = carb,
                    fat = fat,
                    fiber = fiber,
                    details = details,
                    weightGrams = wt
                )
                showQuickAddDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added $name ($cal kcal) to $mealType in My Meals")
                }
            }
        )
    }

    // Delete single item confirmation
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Meal") },
            text = { Text("Remove ${itemToDelete?.foodName} from My Meals?") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { foodViewModel.deleteFoodLog(it) }
                    itemToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (mealToAdjustQuantity != null) {
        com.example.macro_tracker.ui.components.AdjustMealQuantityDialog(
            log = mealToAdjustQuantity!!,
            onDismissRequest = { mealToAdjustQuantity = null },
            onUpdateWeight = { newWeight ->
                mealToAdjustQuantity?.let { foodViewModel.updateMealWeight(it, newWeight) }
            },
            onDeleteMeal = {
                mealToAdjustQuantity?.let { foodViewModel.deleteFoodLog(it) }
            }
        )
    }

    // Delete All confirmation dialog
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            containerColor = NutritrackSurface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete All Meals?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all ${filteredLogs.size} logged meals for $formattedDate in a single click? This cannot be undone.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        foodViewModel.deleteFoodLogsForDate(selectedDate)
                        showDeleteAllConfirm = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("All meals for $formattedDate deleted")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllConfirm = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // DatePicker Dialog for FoodScreen
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            foodViewModel.setDate(picked)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold, color = BrandGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = filter.equals(activeFilter, ignoreCase = true)
                        Surface(
                            onClick = { foodViewModel.setMealFilter(filter) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) NutritrackDark else NutritrackSurface,
                            shadowElevation = if (isSelected) 0.dp else 1.dp,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight) else null
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Header Row (Back Button + Title + Date + Calendar Button)
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
                            text = "My Meals",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                        if (selectedDate != LocalDate.now()) {
                            Surface(
                                onClick = { foodViewModel.resetToToday() },
                                shape = RoundedCornerShape(10.dp),
                                color = BrandGreenPill,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Viewing Past Date • Tap for Today",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    ),
                                    color = BrandGreenDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { showDatePickerDialog = true },
                        shape = CircleShape,
                        color = NutritrackSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = if (selectedDate != LocalDate.now()) BrandGreen else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar with Barcode Scanner Button
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = NutritrackSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                foodViewModel.searchFood(it, immediate = false)
                            },
                            placeholder = {
                                Text(
                                    "Search fruits, meals, recipes (e.g. banana, apple)",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = BrandGreen
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                foodViewModel.searchFood(searchQuery, immediate = true)
                            }),
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                foodViewModel.searchFood("", immediate = true)
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Barcode Scan Button
                        Surface(
                            onClick = { showCameraScanner = true },
                            shape = CircleShape,
                            color = BrandGreen,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = "Barcode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Scan Barcode Banner
            item {
                Surface(
                    onClick = { showCameraScanner = true },
                    shape = RoundedCornerShape(18.dp),
                    color = BrandGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BrandGreen.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Food Barcode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontFamily = OutfitFontFamily
                            )
                            Text(
                                text = "Scan packaging with camera to detect food & macros",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = OutfitFontFamily
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Prominent Add Custom Meal Card Banner
            item {
                Surface(
                    onClick = {
                        selectedMealForAdd = if (activeFilter != "All") activeFilter else "Breakfast"
                        showQuickAddDialog = true
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = NutritrackSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NutritrackDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Custom Meal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontFamily = OutfitFontFamily
                            )
                            Text(
                                text = "Enter dish name, ingredients, calories, fiber & protein",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = OutfitFontFamily
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Search Results Section if query is active
            if (searchQuery.isNotBlank()) {
                if (isLoading && searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandGreen, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (searchResults.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Found Foods (${searchResults.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (isLoading) {
                                CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                items(
                    items = searchResults,
                    key = { product -> "${product.code ?: ""}_${product.product_name}_${product.brands}_${product.nutriments?.calories}" },
                    contentType = { "search_food_item" }
                ) { product ->
                    Surface(
                        onClick = {
                            foodViewModel.saveFood(product, selectedMealForAdd)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Logged ${product.product_name ?: "Food"}")
                            }
                            searchQuery = ""
                            foodViewModel.searchFood("", immediate = true)
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackSurface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fastfood,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.product_name ?: "Unknown food",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                val cal = product.nutriments?.calories ?: 0
                                val pro = product.nutriments?.proteinGrams ?: 0f
                                val carbs = product.nutriments?.carbsGrams ?: 0f
                                val fat = product.nutriments?.fatGrams ?: 0f
                                val brandDesc = if (!product.brands.isNullOrBlank()) " • ${product.brands}" else ""
                                Text(
                                    text = "$cal kcal • P ${pro.toInt()}g • C ${carbs.toInt()}g • F ${fat.toInt()}g$brandDesc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add",
                                tint = BrandGreen
                            )
                        }
                    }
                }
            } else if (!isLoading) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NutritrackSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No matches found for \"$searchQuery\". Use Quick Add Meal above or scan a barcode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

            // Today's intake Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NutritrackSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BrandGreen,
                            trackColor = BrandGreenLight
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Today's intake",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${String.format("%,d", totalCalories)} kcal",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$caloriesRemaining remaining",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = BrandGreen
                        )
                    }
                }
            }

            // Meals Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meals",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                foodViewModel.copyMealsFromYesterday(if (activeFilter != "All") activeFilter else null) { count ->
                                    coroutineScope.launch {
                                        if (count > 0) {
                                            snackbarHostState.showSnackbar("Copied $count meal(s) from yesterday!")
                                        } else {
                                            snackbarHostState.showSnackbar("No meals found from yesterday to copy.")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BrandGreenPill,
                                contentColor = BrandGreen
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Yesterday",
                                modifier = Modifier.size(14.dp),
                                tint = BrandGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Yesterday",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandGreen
                            )
                        }

                        if (filteredLogs.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showDeleteAllConfirm = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ErrorRed.copy(alpha = 0.12f),
                                    contentColor = ErrorRed
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete All",
                                    modifier = Modifier.size(16.dp),
                                    tint = ErrorRed
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete All",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ErrorRed
                                )
                            }
                        }

                        Button(
                            onClick = {
                                selectedMealForAdd = if (activeFilter != "All") activeFilter else "Breakfast"
                                showQuickAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NutritrackDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add meal",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Meal Items List
            if (filteredLogs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NutritrackSurface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestaurantMenu,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (activeFilter == "All") "No meals logged yet" else "No $activeFilter logged yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Your food log is currently empty. Tap a meal below to log what you've eaten:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            FilledTonalButton(
                                onClick = {
                                    foodViewModel.copyMealsFromYesterday(if (activeFilter != "All") activeFilter else null) { count ->
                                        coroutineScope.launch {
                                            if (count > 0) {
                                                snackbarHostState.showSnackbar("Copied $count meal(s) from yesterday!")
                                            } else {
                                                snackbarHostState.showSnackbar("No meals found from yesterday to copy.")
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandGreenPill,
                                    contentColor = BrandGreen
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BrandGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Repeat Yesterday's Meals",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = BrandGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4 Quick Meal Action Pills
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickMealActionCard(
                                        title = "+ Breakfast",
                                        icon = Icons.Rounded.WbSunny,
                                        bgColor = MealYellowBg,
                                        tint = MealYellowIcon,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedMealForAdd = "Breakfast"
                                            showQuickAddDialog = true
                                        }
                                    )
                                    QuickMealActionCard(
                                        title = "+ Lunch",
                                        icon = Icons.Rounded.Restaurant,
                                        bgColor = BrandGreenPill,
                                        tint = BrandGreen,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedMealForAdd = "Lunch"
                                            showQuickAddDialog = true
                                        }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickMealActionCard(
                                        title = "+ Dinner",
                                        icon = Icons.Rounded.DinnerDining,
                                        bgColor = FatBg,
                                        tint = FatColor,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedMealForAdd = "Dinner"
                                            showQuickAddDialog = true
                                        }
                                    )
                                    QuickMealActionCard(
                                        title = "+ Snack",
                                        icon = Icons.Rounded.Diamond,
                                        bgColor = ProteinBg,
                                        tint = ProteinColor,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedMealForAdd = "Snack"
                                            showQuickAddDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredLogs,
                    key = { it.id },
                    contentType = { "meal_log_item" }
                ) { log ->
                    MealLogItem(
                        log = log,
                        onClick = { mealToAdjustQuantity = log },
                        onIncrement = {
                            foodViewModel.updateMealServings(log, log.servings + 1)
                        },
                        onDecrement = {
                            if (log.servings > 1) {
                                foodViewModel.updateMealServings(log, log.servings - 1)
                            } else {
                                itemToDelete = log
                            }
                        }
                    )
                }

                // Quick Add Another Meal Card at bottom
                item {
                    Surface(
                        onClick = {
                            selectedMealForAdd = if (activeFilter != "All") activeFilter else "Snack"
                            showQuickAddDialog = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = NutritrackSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add Meal",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Log another meal",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to add breakfast, lunch, dinner or snack",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Today's Suggested Meals Section (shown in the last to user)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SuggestedMealsSection(
                    plan = suggestedPlan,
                    proteinGoal = proteinGoal,
                    calorieGoal = calorieGoal,
                    onLogMeal = { meal ->
                        foodViewModel.quickAddMeal(
                            mealType = meal.mealType,
                            foodName = meal.dishName,
                            calories = meal.calories,
                            protein = meal.protein,
                            carbs = meal.carbs,
                            fat = meal.fat,
                            fiber = meal.fiber,
                            details = meal.portionDesc,
                            weightGrams = 200f
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Logged ${meal.dishName} to ${meal.mealType}!")
                        }
                    },
                    onNavigateToDietPlans = onNavigateToDietPlans
                )
            }
        }
    }
}

@Composable
fun SuggestedMealsSection(
    plan: DailyDietPlan,
    proteinGoal: Int,
    calorieGoal: Int,
    onLogMeal: (PlanMeal) -> Unit,
    onNavigateToDietPlans: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Suggested Meals for Today",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Calibrated to hit your daily ${proteinGoal}g protein & ${calorieGoal} kcal target",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            TextButton(
                onClick = onNavigateToDietPlans,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Diet Plans →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = BrandGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggested Meal Cards
        plan.meals.forEach { meal ->
            SuggestedMealCard(
                meal = meal,
                onLog = { onLogMeal(meal) },
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun SuggestedMealCard(
    meal: PlanMeal,
    onLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var logged by remember { mutableStateOf(false) }

    val (typeColor, typeBg) = when (meal.mealType.lowercase()) {
        "breakfast" -> Pair(MealYellowIcon, MealYellowBg)
        "lunch" -> Pair(BrandGreen, BrandGreenPill)
        "dinner" -> Pair(FatColor, FatBg)
        else -> Pair(ProteinColor, ProteinBg)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = meal.mealType.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${meal.calories} kcal",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = {
                        onLog()
                        logged = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logged) BrandGreenPill else BrandGreen,
                        contentColor = if (logged) BrandGreenDark else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (logged) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (logged) "Logged" else "+ Add",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meal.dishName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = meal.portionDesc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Macro pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallMacroChip(label = "P", value = "${meal.protein.toInt()}g", color = MacroProtein)
                SmallMacroChip(label = "C", value = "${meal.carbs.toInt()}g", color = MacroCarbs)
                SmallMacroChip(label = "F", value = "${meal.fat.toInt()}g", color = MacroFat)
                if (meal.fiber > 0) {
                    SmallMacroChip(label = "Fiber", value = "${meal.fiber.toInt()}g", color = Color(0xFF00B894))
                }
            }
        }
    }
}

@Composable
private fun SmallMacroChip(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = color
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun QuickMealActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun ServingCounterPill(
    servings: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(NutritrackBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(NutritrackBorderLight)
                .clickable(onClick = onDecrement)
        ) {
            Icon(
                imageVector = if (servings <= 1) Icons.Rounded.DeleteOutline else Icons.Rounded.Remove,
                contentDescription = if (servings <= 1) "Delete" else "Decrease",
                tint = if (servings <= 1) ErrorRed else TextSecondary,
                modifier = Modifier.size(13.dp)
            )
        }

        Text(
            text = "${servings}x",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = if (servings > 1) BrandGreen else TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BrandGreenPill)
                .clickable(onClick = onIncrement)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Increase",
                tint = BrandGreen,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun MealLogItem(
    log: FoodLogEntity,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val (iconBg, iconTint, iconVector) = when (log.mealType.lowercase()) {
        "breakfast" -> Triple(MealYellowBg, MealYellowIcon, Icons.Rounded.WbSunny)
        "lunch" -> Triple(BrandGreenPill, BrandGreen, Icons.Rounded.Restaurant)
        "snack" -> Triple(ProteinBg, ProteinColor, Icons.Rounded.Diamond)
        else -> Triple(FatBg, FatColor, Icons.Rounded.DinnerDining)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = NutritrackSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.mealType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = iconTint
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = log.foodName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandGreenPill
                    ) {
                        Text(
                            text = "${log.weightGrams.toInt()} gm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = BrandGreenDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (log.details.isNotBlank()) log.details else "P ${log.protein.toInt()}g • C ${log.carbs.toInt()}g • F ${log.fat.toInt()}g",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            ServingCounterPill(
                servings = log.servings.coerceAtLeast(1),
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${log.calories} kcal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextPrimary
            )
        }
    }
}
