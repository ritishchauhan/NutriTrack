package com.example.macro_tracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.R
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToDietPlans: () -> Unit = {},
    onNavigateToWeightTrend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Observing lifecycle-aware LiveData for responsive, lifecycle-aware UI updates
    val dailyLogs by foodViewModel.dailyFoodLogsLiveData.observeAsState(emptyList())
    val selectedDate by foodViewModel.selectedDateLiveData.observeAsState(LocalDate.now())
    val userName by profileViewModel.userNameLiveData.observeAsState("")
    val onboardingCompleted by profileViewModel.onboardingCompletedLiveData.observeAsState(false)
    val isProfileLoaded by profileViewModel.isProfileLoadedLiveData.observeAsState(false)

    val calorieGoal by profileViewModel.calorieGoalLiveData.observeAsState(2000)
    val proteinGoal by profileViewModel.proteinGoalLiveData.observeAsState(120)
    val carbsGoal by profileViewModel.carbsGoalLiveData.observeAsState(240)
    val fatGoal by profileViewModel.fatGoalLiveData.observeAsState(70)
    val streakDays by profileViewModel.streakDaysLiveData.observeAsState(0)
    val waterLogged by profileViewModel.waterLoggedLiveData.observeAsState(0f)
    val waterGoal by profileViewModel.waterGoalLiveData.observeAsState(2.4f)

    // Memory and performance optimization: memoize derived sums to avoid recalculation on each frame
    val totalCalories = remember(dailyLogs) { dailyLogs.sumOf { it.calories } }
    val totalProtein = remember(dailyLogs) { dailyLogs.sumOf { it.protein.toDouble() }.toFloat() }
    val totalCarbs = remember(dailyLogs) { dailyLogs.sumOf { it.carbs.toDouble() }.toFloat() }
    val totalFat = remember(dailyLogs) { dailyLogs.sumOf { it.fat.toDouble() }.toFloat() }

    val caloriesLeft = remember(totalCalories, calorieGoal) { (calorieGoal - totalCalories).coerceAtLeast(0) }
    val calorieProgress = remember(totalCalories, calorieGoal) {
        if (calorieGoal > 0) (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val percentEaten = remember(calorieProgress) { (calorieProgress * 100).toInt() }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    val formattedDate = selectedDate.format(dateFormatter)

    var itemToDelete by remember { mutableStateOf<FoodLogEntity?>(null) }
    var mealToAdjustQuantity by remember { mutableStateOf<FoodLogEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // First-time only: Only display onboarding prompt after profile is loaded and user has no saved name
    LaunchedEffect(isProfileLoaded, userName, onboardingCompleted) {
        if (isProfileLoaded) {
            showOnboardingDialog = !onboardingCompleted && userName.isBlank()
        }
    }

    // Dynamic Time-based Greeting with contextual emoji
    val currentHour = remember { java.time.LocalTime.now().hour }
    val (greetingPrefix, greetingEmoji) = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning" to "☀️"
            in 12..16 -> "Good afternoon" to "🌤️"
            in 17..21 -> "Good evening" to "🌆"
            else -> "Good night" to "🌙"
        }
    }

    // First-Time Setup & Onboarding Dialog
    if (showOnboardingDialog) {
        var inputName by remember { mutableStateOf("") }
        var inputCalories by remember { mutableStateOf("2000") }
        var selectedDiet by remember { mutableStateOf("Non-veg") }
        var selectedActivity by remember { mutableStateOf("Sedentary") }

        AlertDialog(
            onDismissRequest = { /* user must complete setup */ },
            shape = RoundedCornerShape(24.dp),
            containerColor = NutritrackSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Nutritrack Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "Welcome to Nutritrack!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Let's set up your profile and daily goals:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Name
                    Column {
                        Text(
                            text = "Your Name",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            placeholder = { Text("What is your name?", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = NutritrackBorder,
                                focusedContainerColor = NutritrackBg,
                                unfocusedContainerColor = NutritrackBg,
                                cursorColor = BrandGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Daily Calorie Goal
                    Column {
                        Text(
                            text = "Daily Calorie Target (kcal)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = inputCalories,
                            onValueChange = { inputCalories = it },
                            placeholder = { Text("e.g. 2000", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = NutritrackBorder,
                                focusedContainerColor = NutritrackBg,
                                unfocusedContainerColor = NutritrackBg,
                                cursorColor = BrandGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Dietary Preference
                    Column {
                        Text(
                            text = "Dietary Preference",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Veg", "Non-veg").forEach { diet ->
                                val isSel = diet == selectedDiet
                                Surface(
                                    onClick = { selectedDiet = diet },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) BrandGreenPill else NutritrackBg,
                                    border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, BrandGreen) else androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                        Text(
                                            text = diet,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) BrandGreenDark else TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cal = inputCalories.toIntOrNull() ?: 2000
                        profileViewModel.completeOnboarding(
                            name = inputName.trim(),
                            calories = cal,
                            diet = selectedDiet,
                            activity = selectedActivity
                        )
                        showOnboardingDialog = false
                    },
                    enabled = inputName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Start Tracking", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = null
        )
    }


    // Calendar DatePicker Dialog
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

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Meal") },
            text = { Text("Are you sure you want to remove ${itemToDelete?.foodName}?") },
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
                    text = "Are you sure you want to delete all ${dailyLogs.size} logged meals for $formattedDate in a single click? This cannot be undone.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        foodViewModel.deleteFoodLogsForDate(selectedDate)
                        showDeleteAllConfirm = false
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NutritrackBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // 1. Top Navigation & Brand Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Menu button & Nutritrack Face Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onOpenDrawer,
                            shape = CircleShape,
                            color = NutritrackSurface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menu",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenDrawer() }
                                .padding(vertical = 2.dp)
                        ) {
                            Box {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Nutritrack Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                // Active indicator dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenAccent)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "NUTRITRACK",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Macro Tracker",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Right: Calendar Picker & AI Assistant
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Calendar Picker Button
                        Surface(
                            onClick = { showDatePickerDialog = true },
                            shape = CircleShape,
                            color = if (selectedDate != LocalDate.now()) BrandGreenPill else NutritrackSurface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Pick Date",
                                    tint = if (selectedDate != LocalDate.now()) BrandGreen else TextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }


            // 2. Hero Greeting & Interactive Date / Habit Strip
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val displayName = if (userName.isNotBlank()) userName.split(" ").firstOrNull() ?: userName else "Friend"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greetingPrefix, $displayName $greetingEmoji",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextPrimary
                            )
                            val subtitleText = when {
                                selectedDate != LocalDate.now() -> "Viewing past meal log • Tap 'Today' to return"
                                totalCalories == 0 -> "Let's fuel your day with mindful nutrition"
                                caloriesLeft > 0 -> "${String.format("%,d", caloriesLeft)} kcal left to hit daily goal"
                                else -> "🎯 Daily calorie goal reached! Great job!"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    // Interactive Date Navigator & Habit Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quick Day Navigator Capsule (‹ Date ›)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NutritrackSurface,
                            shadowElevation = 1.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { foodViewModel.setDate(selectedDate.minusDays(1)) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "Previous Day",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showDatePickerDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val dateLabel = when (selectedDate) {
                                        LocalDate.now() -> "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}"
                                        LocalDate.now().minusDays(1) -> "Yesterday, ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}"
                                        LocalDate.now().plusDays(1) -> "Tomorrow, ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}"
                                        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                                    }
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (selectedDate != LocalDate.now()) BrandGreen else TextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { foodViewModel.setDate(selectedDate.plusDays(1)) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Next Day",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Right: Reset Today Chip or Habit Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selectedDate != LocalDate.now()) {
                                Surface(
                                    onClick = { foodViewModel.resetToToday() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = BrandGreenPill,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreenLight)
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = BrandGreenDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Streak Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MealYellowBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔥 ${if (streakDays > 0) "$streakDays d" else "1 d"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MealYellowIcon
                                    )
                                }
                            }

                            // Hydration Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WaterBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "💧 ${String.format("%.1f", waterLogged)}L",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = WaterColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today's Calories Hero Card
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
                        // Top horizontal progress bar
                        LinearProgressIndicator(
                            progress = { calorieProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BrandGreen,
                            trackColor = BrandGreenLight
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Details
                            Column {
                                Text(
                                    text = if (selectedDate == LocalDate.now()) "TODAY'S CALORIES" else "DAILY CALORIES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = BrandGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format("%,d", totalCalories),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 36.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "kcal eaten",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${String.format("%,d", calorieGoal)} kcal goal",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = TextSecondary
                                )
                            }

                            // Right Circular Ring & Left Badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { 1f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = BrandGreenLight,
                                        strokeWidth = 7.dp
                                    )
                                    CircularProgressIndicator(
                                        progress = { calorieProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        color = BrandGreen,
                                        strokeWidth = 7.dp
                                    )
                                    Text(
                                        text = "$percentEaten%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = BrandGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BrandGreenPill,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "$caloriesLeft left",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = BrandGreenDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Macros Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Macros",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )

                        TextButton(
                            onClick = onNavigateToInsights,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "View details",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MacroCard(
                            label = "Protein",
                            amount = totalProtein.toInt(),
                            goal = proteinGoal,
                            color = ProteinColor,
                            bgColor = ProteinBg,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            label = "Carbs",
                            amount = totalCarbs.toInt(),
                            goal = carbsGoal,
                            color = CarbsColor,
                            bgColor = CarbsBg,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            label = "Fat",
                            amount = totalFat.toInt(),
                            goal = fatGoal,
                            color = FatColor,
                            bgColor = FatBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Indian Diet Plans & Recipes Banner
            item {
                Surface(
                    onClick = onNavigateToDietPlans,
                    shape = RoundedCornerShape(22.dp),
                    color = BrandGreenPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BrandGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestaurantMenu,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Indian Kitchen Diet Plans",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandGreenDark,
                                fontFamily = OutfitFontFamily
                            )
                            Text(
                                text = "BMI Calculator • Fat Loss, Muscle & Bulk Plans • 100+ Recipes",
                                fontSize = 12.sp,
                                color = TextPrimary.copy(alpha = 0.8f),
                                fontFamily = OutfitFontFamily
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Weight Trend (MacroFactor EMA Smoothing)
            item {
                val weightSummary by profileViewModel.weightTrendSummaryLiveData.observeAsState(com.example.macro_tracker.util.WeightTrendSummary())
                val weightLogs by profileViewModel.weightLogsLiveData.observeAsState(emptyList())
                com.example.macro_tracker.ui.components.WeightTrendCard(
                    summary = weightSummary,
                    weightLogs = weightLogs,
                    onLogWeight = { weight, note ->
                        profileViewModel.logWeight(weight, note)
                    },
                    onUpdateWeight = { log ->
                        profileViewModel.updateWeightLog(log)
                    },
                    onDeleteWeight = { log ->
                        profileViewModel.deleteWeightLog(log)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    onClick = onNavigateToWeightTrend,
                    shape = RoundedCornerShape(14.dp),
                    color = BrandGreenPill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View 3-Month Predictions & Trend Graph",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreenDark
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Google Health Connect (Daily Steps & Calories Burned)
            item {
                com.example.macro_tracker.ui.components.HealthConnectCard()
            }

            // Meals Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate == LocalDate.now()) "Today's meals" else "Meals for ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
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
                                foodViewModel.copyMealsFromYesterday { count ->
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

                        if (dailyLogs.isNotEmpty()) {
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
                            onClick = onNavigateToLog,
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

            // Meals List Items
            if (dailyLogs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NutritrackSurface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestaurantMenu,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (selectedDate == LocalDate.now()) "No meals logged yet today" else "No meals logged for this date",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your meals list is empty. Add what you've eaten to hit your calorie and macro goals.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        foodViewModel.copyMealsFromYesterday { count ->
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
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BrandGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Yesterday", fontWeight = FontWeight.SemiBold, color = BrandGreen)
                                }

                                Button(
                                    onClick = onNavigateToLog,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NutritrackDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Meal", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = dailyLogs,
                    key = { it.id },
                    contentType = { "dashboard_meal_item" }
                ) { log ->
                    DashboardMealItem(
                        log = log,
                        onClick = { mealToAdjustQuantity = log },
                        onIncrement = {
                            foodViewModel.updateMealServings(log, log.servings + 1)
                        },
                        onDecrement = {
                            if (log.servings > 1) {
                                foodViewModel.updateMealServings(log, log.servings - 1)
                            } else {
                                mealToAdjustQuantity = log
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MacroCard(
    label: String,
    amount: Int,
    goal: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${amount}g",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "${goal}g",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardMealItem(
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
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
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
