package com.example.macro_tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.data.local.WeightLogEntity
import com.example.macro_tracker.ui.theme.*
import com.example.macro_tracker.util.WeightTrendSummary
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrendScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weightTrendSummary by profileViewModel.weightTrendSummary.collectAsState()
    val weightLogs by profileViewModel.weightLogs.collectAsState()
    val calorieGoal by profileViewModel.calorieGoal.collectAsState()
    val userFitnessGoal by profileViewModel.userFitnessGoal.collectAsState()
    val userWeightKg by profileViewModel.userWeightKg.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WeightLogEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<WeightLogEntity?>(null) }

    // User's Choice for 3-Month Future Predictions: "LOSE" or "GAIN"
    var predictionGoal by remember(userFitnessGoal) {
        mutableStateOf(if (userFitnessGoal.contains("GAIN", ignoreCase = true)) "GAIN" else "LOSE")
    }

    // Weekly rate selection (kg per week)
    var weeklyPace by remember(predictionGoal) {
        mutableFloatStateOf(if (predictionGoal == "GAIN") 0.35f else 0.5f)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Base current weight to start from
    val currentWeight = remember(weightTrendSummary, weightLogs, userWeightKg) {
        when {
            weightTrendSummary.currentTrendKg > 0 -> weightTrendSummary.currentTrendKg
            weightLogs.isNotEmpty() -> weightLogs.maxByOrNull { it.timestamp }?.weightKg ?: 70f
            userWeightKg > 0 -> userWeightKg
            else -> 70f
        }
    }

    // Performance optimization: Memoize sorted weigh-ins to prevent recalculating on every scroll frame
    val sortedLogs = remember(weightLogs) { weightLogs.sortedByDescending { it.timestamp } }

    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayLog = remember(weightLogs, todayStartMillis) {
        weightLogs.firstOrNull { it.timestamp >= todayStartMillis }
    }

    // Log Body Weight Dialog
    if (showLogDialog) {
        var inputWeight by remember {
            mutableStateOf(if (currentWeight > 0) String.format(Locale.US, "%.1f", currentWeight) else "70.0")
        }
        var inputNote by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            containerColor = NutritrackSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPill)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Log Weigh-in",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Daily weigh-ins are smoothed with an Exponential Moving Average (EMA) to filter out sodium & water fluctuations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = {
                            inputWeight = it
                            isError = it.toFloatOrNull() == null || (it.toFloatOrNull() ?: 0f) <= 0f
                        },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Note (optional, e.g. morning fasting)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val weight = inputWeight.toFloatOrNull()
                        if (weight != null && weight > 0) {
                            profileViewModel.logWeight(weight, inputNote.trim())
                            showLogDialog = false
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Weigh-in", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Weigh-in Dialog
    entryToEdit?.let { entry ->
        var editWeight by remember(entry) { mutableStateOf(entry.weightKg.toString()) }
        var editNote by remember(entry) { mutableStateOf(entry.note) }
        var isEditError by remember(entry) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { entryToEdit = null },
            containerColor = NutritrackSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPill)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Edit Weigh-in",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Correct your weigh-in value. Your Weight trend will automatically update.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = {
                            editWeight = it
                            isEditError = it.toFloatOrNull() == null || (it.toFloatOrNull() ?: 0f) <= 0f
                        },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        isError = isEditError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val weight = editWeight.toFloatOrNull()
                        if (weight != null && weight > 0) {
                            profileViewModel.updateWeightLog(entry.copy(weightKg = weight, note = editNote.trim()))
                            entryToEdit = null
                        } else {
                            isEditError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Weigh-in Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = NutritrackSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Delete Weigh-in?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the weigh-in of ${entry.weightKg} kg from ${dateFormat.format(Date(entry.timestamp))}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.deleteWeightLog(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NutritrackBg,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // 1. Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                        Column {
                            Text(
                                text = "Weight trend",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Smoothed analysis & 3-month forecast",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showLogDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandGreenPill,
                            contentColor = BrandGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log Weight",
                            modifier = Modifier.size(16.dp),
                            tint = BrandGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Log",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreen
                        )
                    }
                }
            }

            // 2. Prominent On-Screen Daily Weigh-In Card (Clear & Understandable)
            item {
                TodayWeighInCard(
                    currentWeight = currentWeight,
                    todayLog = todayLog,
                    onSaveWeight = { weight, note ->
                        if (todayLog != null) {
                            profileViewModel.updateWeightLog(todayLog.copy(weightKg = weight, note = note))
                        } else {
                            profileViewModel.logWeight(weight, note)
                        }
                    }
                )
            }

            // 3. Current Weight Trend Summary Cards (Hero Display)
            item {
                val scaleWeight = if (weightTrendSummary.currentActualKg > 0) weightTrendSummary.currentActualKg else currentWeight
                WeightTrendHeroCards(
                    currentTrendKg = currentWeight,
                    scaleWeightKg = scaleWeight,
                    weeklyRateKg = weightTrendSummary.weeklyRateKg,
                    totalChangeKg = weightTrendSummary.totalChangeKg,
                    userFitnessGoal = userFitnessGoal
                )
            }

            // 4. Historical Weight Trend Chart (Scatter scale dots + smooth EMA curve)
            item {
                HistoricalTrendChartCard(
                    summary = weightTrendSummary,
                    weightLogs = weightLogs,
                    onLogWeightClick = { showLogDialog = true }
                )
            }

            // 5. Smart Trend Coaching & Insights (Reading Chart and Daily Trends for Improvement)
            item {
                val scaleWeight = if (weightTrendSummary.currentActualKg > 0) weightTrendSummary.currentActualKg else currentWeight
                SmartTrendCoachingCard(
                    summary = weightTrendSummary,
                    userFitnessGoal = userFitnessGoal,
                    scaleWeight = scaleWeight
                )
            }

            // 4. 3-Month Future Predictions Section (Weight Gain vs. Weight Loss with 3-Month Timeline Graph)
            item {
                ThreeMonthPredictionSection(
                    currentWeight = currentWeight,
                    calorieGoal = calorieGoal,
                    predictionGoal = predictionGoal,
                    onGoalChange = { newGoal ->
                        predictionGoal = newGoal
                        weeklyPace = if (newGoal == "GAIN") 0.35f else 0.5f
                    },
                    weeklyPace = weeklyPace,
                    onPaceChange = { weeklyPace = it }
                )
            }

            // 5. Weight History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Weigh-in History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )

                    Text(
                        text = "${weightLogs.size} recorded",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (weightLogs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NutritrackSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MonitorWeight,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No weigh-ins logged yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weigh yourself consistently in the morning before food to build your trend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FilledTonalButton(
                                onClick = { showLogDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandGreenPill,
                                    contentColor = BrandGreen
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Weigh-in", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(sortedLogs, key = { it.id }, contentType = { "weight_log" }) { log ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenPill)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonitorWeight,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", log.weightKg)} kg",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    if (log.note.isNotBlank()) {
                                        Text(
                                            text = log.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { entryToEdit = log },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit entry",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { entryToDelete = log },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete entry",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper state class for rate status display in WeightTrendHeroCards
 */
private data class RateStatus(
    val badgeText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isLoss: Boolean,
    val isGain: Boolean
)

/**
 * Weight Trend Hero Cards
 * High-aesthetic dual-card hero section displaying:
 * 1) Current Trend (Noise-filtered Exponential Moving Average) with water/scale discrepancy indicator.
 * 2) Weekly Rate (Calorie deficit/surplus speed of change) with user-fitness-goal context.
 */
@Composable
fun WeightTrendHeroCards(
    currentTrendKg: Float,
    scaleWeightKg: Float,
    weeklyRateKg: Float,
    totalChangeKg: Float,
    userFitnessGoal: String,
    modifier: Modifier = Modifier
) {
    val deltaFromScale = scaleWeightKg - currentTrendKg

    val isGoalGain = userFitnessGoal.contains("GAIN", ignoreCase = true)
    val isGoalLose = userFitnessGoal.contains("LOSE", ignoreCase = true)

    val rateStatus = remember(weeklyRateKg, isGoalGain, isGoalLose) {
        when {
            weeklyRateKg < -0.15f -> {
                val badge = if (isGoalLose) "Target Deficit" else "Weight Loss"
                RateStatus(badge, Icons.AutoMirrored.Rounded.TrendingDown, isLoss = true, isGain = false)
            }
            weeklyRateKg > 0.15f -> {
                val badge = if (isGoalGain) "Lean Bulk Pace" else "Weight Gain"
                RateStatus(badge, Icons.AutoMirrored.Rounded.TrendingUp, isLoss = false, isGain = true)
            }
            else -> {
                RateStatus("Holding Steady", Icons.AutoMirrored.Rounded.TrendingFlat, isLoss = false, isGain = false)
            }
        }
    }

    val rateColor = when {
        rateStatus.isLoss -> BrandGreen
        rateStatus.isGain -> EnergyAmber
        else -> TextSecondary
    }

    val rateBg = when {
        rateStatus.isLoss -> BrandGreenPill
        rateStatus.isGain -> EnergyAmber.copy(alpha = 0.12f)
        else -> NutritrackBorderLight
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Current Trend Card ---
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NutritrackSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1.1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header: Icon + Title + Pill Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandGreenPill)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorWeight,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Trend",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Smoothed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BrandGreenDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Trend Number
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (currentTrendKg > 0) String.format(Locale.US, "%.1f", currentTrendKg) else "--",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scale weight & Water/glycogen Discrepancy Micro-Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NutritrackBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Scale: ${if (scaleWeightKg > 0) String.format(Locale.US, "%.1f", scaleWeightKg) else "--"} kg",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )

                        if (scaleWeightKg > 0 && currentTrendKg > 0) {
                            val (dispText, dispColor) = when {
                                deltaFromScale > 0.05f -> {
                                    "+${String.format(Locale.US, "%.1f", deltaFromScale)} (water)" to Color(0xFF00897B)
                                }
                                deltaFromScale < -0.05f -> {
                                    "${String.format(Locale.US, "%.1f", deltaFromScale)} (dip)" to EnergyAmber
                                }
                                else -> {
                                    "In sync" to BrandGreen
                                }
                            }
                            Text(
                                text = dispText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = dispColor
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Weekly Rate Card ---
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NutritrackSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header: Icon + Title + Goal Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(rateBg)
                        ) {
                            Icon(
                                imageVector = rateStatus.icon,
                                contentDescription = null,
                                tint = rateColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Rate",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rateBg
                    ) {
                        Text(
                            text = rateStatus.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = rateColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Rate Number
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${if (weeklyRateKg > 0) "+" else ""}${String.format(Locale.US, "%.2f", weeklyRateKg)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = rateColor
                    )
                    Text(
                        text = "kg/wk",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Net Total Change Micro-Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NutritrackBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val totalSign = if (totalChangeKg > 0) "+" else ""
                        Text(
                            text = "$totalSign${String.format(Locale.US, "%.1f", totalChangeKg)} kg total",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Historical Weight Trend Chart Card
 * Renders actual weigh-in dots and a smoothed EMA trend line.
 */
@Composable
fun HistoricalTrendChartCard(
    summary: WeightTrendSummary,
    weightLogs: List<WeightLogEntity>,
    onLogWeightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPill)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Historical Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Scale weigh-ins vs smoothed trend",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TextMuted)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scale", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(BrandGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trend", style = MaterialTheme.typography.labelSmall, color = BrandGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val points = summary.trendPoints
            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NutritrackBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Log at least 2 weigh-ins to plot your curve",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onLogWeightClick) {
                            Text("+ Log Weigh-in", color = BrandGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Interactive Canvas Chart (Optimized: single-pass min/max & memoized dash effect)
                val (minWeight, maxWeight, weightRange) = remember(points) {
                    var minVal = Float.MAX_VALUE
                    var maxVal = Float.MIN_VALUE
                    for (pt in points) {
                        if (pt.actualWeight < minVal) minVal = pt.actualWeight
                        if (pt.actualWeight > maxVal) maxVal = pt.actualWeight
                        if (pt.trendWeight < minVal) minVal = pt.trendWeight
                        if (pt.trendWeight > maxVal) maxVal = pt.trendWeight
                    }
                    if (minVal == Float.MAX_VALUE) {
                        minVal = 60f
                        maxVal = 80f
                    }
                    val min = minVal - 1.0f
                    val max = maxVal + 1.0f
                    Triple(min, max, (max - min).coerceAtLeast(1.0f))
                }

                val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
                val gridColor = NutritrackBorderLight
                val dotMutedColor = TextMuted.copy(alpha = 0.6f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingTop = 12f
                        val paddingBottom = 16f
                        val usableHeight = height - paddingTop - paddingBottom

                        // Gridlines (3 levels)
                        for (i in 0..2) {
                            val y = paddingTop + (usableHeight * (i / 2f))
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f,
                                pathEffect = gridDashEffect
                            )
                        }

                        if (points.size == 1) {
                            val p = points[0]
                            val y = paddingTop + usableHeight * (1f - ((p.actualWeight - minWeight) / weightRange))
                            drawCircle(color = BrandGreen, radius = 6f, center = Offset(width / 2f, y))
                            drawCircle(color = Color.White, radius = 3f, center = Offset(width / 2f, y))
                        } else {
                            val stepX = width / (points.size - 1).toFloat()

                            // 1. Shaded area below the trend line
                            val fillPath = Path()
                            val linePath = Path()

                            points.forEachIndexed { index, pt ->
                                val x = index * stepX
                                val y = paddingTop + usableHeight * (1f - ((pt.trendWeight - minWeight) / weightRange))

                                if (index == 0) {
                                    fillPath.moveTo(x, height)
                                    fillPath.lineTo(x, y)
                                    linePath.moveTo(x, y)
                                } else {
                                    val prevX = (index - 1) * stepX
                                    val prevY = paddingTop + usableHeight * (1f - ((points[index - 1].trendWeight - minWeight) / weightRange))
                                    val cx = (prevX + x) / 2f
                                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                                    linePath.cubicTo(cx, prevY, cx, y, x, y)
                                }
                            }

                            val lastX = (points.size - 1) * stepX
                            fillPath.lineTo(lastX, height)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(BrandGreen.copy(alpha = 0.20f), BrandGreen.copy(alpha = 0.01f)),
                                    startY = paddingTop,
                                    endY = height
                                )
                            )

                            // 2. Draw actual weigh-in dots (scale fluctuations)
                            points.forEachIndexed { index, pt ->
                                val x = index * stepX
                                val y = paddingTop + usableHeight * (1f - ((pt.actualWeight - minWeight) / weightRange))
                                drawCircle(
                                    color = dotMutedColor,
                                    radius = 4f,
                                    center = Offset(x, y)
                                )
                            }

                            // 3. Draw smooth trend line
                            drawPath(
                                path = linePath,
                                color = BrandGreen,
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                            )

                            // 4. Highlight latest point
                            val latestIndex = points.size - 1
                            val latestX = latestIndex * stepX
                            val latestY = paddingTop + usableHeight * (1f - ((points.last().trendWeight - minWeight) / weightRange))
                            drawCircle(color = BrandGreen, radius = 7f, center = Offset(latestX, latestY))
                            drawCircle(color = Color.White, radius = 3.5f, center = Offset(latestX, latestY))
                        }
                    }
                }

                // Range footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Low: ${String.format(Locale.US, "%.1f", minWeight + 1.0f)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${points.size} weigh-ins plotted",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "High: ${String.format(Locale.US, "%.1f", maxWeight - 1.0f)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private data class PredictionMilestones(
    val m1Change: Float,
    val m2Change: Float,
    val m3Change: Float,
    val m1Weight: Float,
    val m2Weight: Float,
    val m3Weight: Float,
    val dailyCalorieAdjustment: Int,
    val recommendedCalories: Int
)

/**
 * 3-Month Future Predictions Section
 * Provides user choice for Weight Gain or Weight Loss and displays an interactive 3-month forecast graph.
 */
@Composable
fun ThreeMonthPredictionSection(
    currentWeight: Float,
    calorieGoal: Int,
    predictionGoal: String,
    onGoalChange: (String) -> Unit,
    weeklyPace: Float,
    onPaceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGain = predictionGoal == "GAIN"

    // Memoize 3-Month Milestones Calculation to optimize performance & memory
    val milestones = remember(currentWeight, weeklyPace, isGain, calorieGoal) {
        val sign = if (isGain) 1f else -1f
        val m1C = sign * weeklyPace * 4f
        val m2C = sign * weeklyPace * 8f
        val m3C = sign * weeklyPace * 12f
        val m1W = Math.round((currentWeight + m1C) * 10f) / 10f
        val m2W = Math.round((currentWeight + m2C) * 10f) / 10f
        val m3W = Math.round((currentWeight + m3C) * 10f) / 10f
        val calAdj = (weeklyPace * 1100f).toInt()
        val recCal = if (isGain) {
            calorieGoal + calAdj
        } else {
            (calorieGoal - calAdj).coerceAtLeast(1200)
        }
        PredictionMilestones(m1C, m2C, m3C, m1W, m2W, m3W, calAdj, recCal)
    }

    val m1Change = milestones.m1Change
    val m2Change = milestones.m2Change
    val m3Change = milestones.m3Change
    val m1Weight = milestones.m1Weight
    val m2Weight = milestones.m2Weight
    val m3Weight = milestones.m3Weight
    val dailyCalorieAdjustment = milestones.dailyCalorieAdjustment
    val recommendedCalories = milestones.recommendedCalories

    val targetDate = remember {
        LocalDate.now().plusMonths(3).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isGain) EnergyAmber.copy(alpha = 0.15f) else BrandGreenPill)
                    ) {
                        Icon(
                            imageVector = if (isGain) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = if (isGain) EnergyAmber else BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "3-Month Future Predictions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Timeline: 12 Weeks (90 Days Forecast)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandGreenPill
                ) {
                    Text(
                        text = "3 Months",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = BrandGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Choice Toggle: Weight Loss vs. Weight Gain
            Text(
                text = "Prediction Goal",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NutritrackBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Weight Loss Option
                val isLoseSelected = !isGain
                Surface(
                    onClick = { onGoalChange("LOSE") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLoseSelected) NutritrackDark else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = if (isLoseSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weight Loss",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isLoseSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isLoseSelected) Color.White else TextSecondary
                        )
                    }
                }

                // Weight Gain Option
                Surface(
                    onClick = { onGoalChange("GAIN") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGain) NutritrackDark else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = if (isGain) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weight Gain",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isGain) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isGain) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Select Target Weekly Pace
            Text(
                text = "Target Weekly Pace",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            val paceOptions = if (isGain) {
                listOf(0.20f to "Gentle", 0.35f to "Optimal", 0.50f to "Fast", 0.70f to "High")
            } else {
                listOf(0.25f to "Gentle", 0.50f to "Standard", 0.75f to "Faster", 1.00f to "Aggressive")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                paceOptions.forEach { (pace, label) ->
                    val isSelected = abs(weeklyPace - pace) < 0.05f
                    Surface(
                        onClick = { onPaceChange(pace) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BrandGreen else NutritrackBg,
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", pace)} kg",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) Color.White else TextPrimary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Month Projection Graph (Canvas)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NutritrackBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                val (minY, maxY, rangeY) = remember(currentWeight, m1Weight, m2Weight, m3Weight) {
                    val min = minOf(currentWeight, m1Weight, m2Weight, m3Weight) - 1.0f
                    val max = maxOf(currentWeight, m1Weight, m2Weight, m3Weight) + 1.0f
                    Triple(min, max, (max - min).coerceAtLeast(1.0f))
                }

                val accentColor = if (isGain) EnergyAmber else BrandGreen
                val predictionGridColor = NutritrackBorderLight
                val predGridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
                val predLineDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f) }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingTop = 24f
                    val paddingBottom = 26f
                    val usableHeight = height - paddingTop - paddingBottom

                    // Horizontal reference grid lines
                    for (i in 0..2) {
                        val y = paddingTop + (usableHeight * (i / 2f))
                        drawLine(
                            color = predictionGridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = predGridDashEffect
                        )
                    }

                    // 4 timeline points: Today (0%), M1 (33.3%), M2 (66.6%), M3 (100%)
                    val x0 = 0f
                    val y0 = paddingTop + usableHeight * (1f - ((currentWeight - minY) / rangeY))

                    val x1 = width * 0.333f
                    val y1 = paddingTop + usableHeight * (1f - ((m1Weight - minY) / rangeY))

                    val x2 = width * 0.666f
                    val y2 = paddingTop + usableHeight * (1f - ((m2Weight - minY) / rangeY))

                    val x3 = width
                    val y3 = paddingTop + usableHeight * (1f - ((m3Weight - minY) / rangeY))

                    // Shaded gradient corridor below curve
                    val areaPath = Path()
                    areaPath.moveTo(x0, height)
                    areaPath.lineTo(x0, y0)
                    areaPath.cubicTo((x0 + x1) / 2f, y0, (x0 + x1) / 2f, y1, x1, y1)
                    areaPath.cubicTo((x1 + x2) / 2f, y1, (x1 + x2) / 2f, y2, x2, y2)
                    areaPath.cubicTo((x2 + x3) / 2f, y2, (x2 + x3) / 2f, y3, x3, y3)
                    areaPath.lineTo(x3, height)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.02f)),
                            startY = paddingTop,
                            endY = height
                        )
                    )

                    // Dashed forecast trajectory line
                    val linePath = Path()
                    linePath.moveTo(x0, y0)
                    linePath.cubicTo((x0 + x1) / 2f, y0, (x0 + x1) / 2f, y1, x1, y1)
                    linePath.cubicTo((x1 + x2) / 2f, y1, (x1 + x2) / 2f, y2, x2, y2)
                    linePath.cubicTo((x2 + x3) / 2f, y2, (x2 + x3) / 2f, y3, x3, y3)

                    drawPath(
                        path = linePath,
                        color = accentColor,
                        style = Stroke(
                            width = 3.5f,
                            cap = StrokeCap.Round,
                            pathEffect = predLineDashEffect
                        )
                    )

                    // Milestone marker dots (zero allocation)
                    val ptCoords = floatArrayOf(x0, y0, x1, y1, x2, y2, x3, y3)
                    for (i in 0 until 4) {
                        val px = ptCoords[i * 2]
                        val py = ptCoords[i * 2 + 1]
                        val isFinal = (i == 3)
                        val outerRadius = if (isFinal) 9f else 7f
                        val innerRadius = if (isFinal) 4.5f else 3.5f

                        drawCircle(color = accentColor.copy(alpha = 0.35f), radius = outerRadius + 4f, center = Offset(px, py))
                        drawCircle(color = accentColor, radius = outerRadius, center = Offset(px, py))
                        drawCircle(color = Color.White, radius = innerRadius, center = Offset(px, py))
                    }
                }
            }

            // Timeline Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Today", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("${String.format(Locale.US, "%.1f", currentWeight)} kg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Month 1", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("${String.format(Locale.US, "%.1f", m1Weight)} kg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Month 2", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("${String.format(Locale.US, "%.1f", m2Weight)} kg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Month 3 (Goal)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isGain) EnergyAmber else BrandGreen)
                    Text("${String.format(Locale.US, "%.1f", m3Weight)} kg", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isGain) EnergyAmber else BrandGreen)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Month Summary Milestone Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3-Month Target Weight
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "3-Mo Projected",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", m3Weight)} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isGain) EnergyAmber else BrandGreen
                        )
                        Text(
                            text = "${if (m3Change > 0) "+" else ""}${String.format(Locale.US, "%.1f", m3Change)} kg total",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Daily Calorie Target Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackBg,
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Daily Calorie Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$recommendedCalories kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isGain) "+$dailyCalorieAdjustment kcal surplus" else "-$dailyCalorieAdjustment kcal deficit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isGain) EnergyAmber else BrandGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target Date Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandGreenPill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estimated target date: $targetDate (12 weeks)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandGreenDark
                    )
                }
            }
        }
    }
}

/**
 * Prominent, clear, and user-friendly daily weigh-in card directly on the screen.
 * Resolves user confusion by providing explicit number input, +/- 0.1kg steppers,
 * and a clear "Save Today's Weigh-in" call-to-action.
 */
@Composable
fun TodayWeighInCard(
    currentWeight: Float,
    todayLog: WeightLogEntity?,
    onSaveWeight: (weight: Float, note: String) -> Unit
) {
    val todayDateStr = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    var weightInput by remember(todayLog, currentWeight) {
        val initial = todayLog?.weightKg ?: (if (currentWeight > 0f) currentWeight else 70f)
        mutableStateOf(String.format(Locale.US, "%.1f", initial))
    }

    var selectedNote by remember(todayLog) {
        mutableStateOf(todayLog?.note?.ifBlank { "Morning Fasting" } ?: "Morning Fasting")
    }

    var justSaved by remember { mutableStateOf(false) }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            kotlinx.coroutines.delay(2500)
            justSaved = false
        }
    }

    val parsedWeight = weightInput.toFloatOrNull()
    val isValid = parsedWeight != null && parsedWeight in 25f..300f

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (todayLog != null) BrandGreen.copy(alpha = 0.5f) else NutritrackBorderLight
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
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
                            .background(if (todayLog != null) BrandGreenPill else Color(0xFFF1F2F6))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = if (todayLog != null) BrandGreenDark else TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (todayLog != null) "Today's Weigh-in Recorded" else "Log Today's Weight",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                if (todayLog != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandGreenPill
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recorded",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = BrandGreenDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (todayLog != null)
                    "You've recorded ${todayLog.weightKg} kg today. Use the +/- steppers or edit below to adjust anytime."
                else
                    "Where to enter: Input your morning weight below and tap Save to update your chart and moving trend.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper and Input Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Decrement button (-0.1 kg)
                Surface(
                    onClick = {
                        val current = weightInput.toFloatOrNull() ?: 70f
                        val updated = (current - 0.1f).coerceAtLeast(25f)
                        weightInput = String.format(Locale.US, "%.1f", updated)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "-0.1",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }

                // Main weight text field
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        weightInput = filtered
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = TextPrimary
                    ),
                    suffix = {
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark
                            )
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

                // Increment button (+0.1 kg)
                Surface(
                    onClick = {
                        val current = weightInput.toFloatOrNull() ?: 70f
                        val updated = (current + 0.1f).coerceAtMost(300f)
                        weightInput = String.format(Locale.US, "%.1f", updated)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+0.1",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick condition chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Morning Fasting", "Post Workout", "Night").forEach { tag ->
                    val isSel = selectedNote.equals(tag, ignoreCase = true)
                    Surface(
                        onClick = { selectedNote = tag },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) NutritrackDark else NutritrackBg,
                        border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSel) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save / Update Button
            Button(
                onClick = {
                    if (isValid) {
                        onSaveWeight(parsedWeight!!, selectedNote)
                        justSaved = true
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (justSaved) BrandGreen else NutritrackDark,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (justSaved) Icons.Rounded.Check else Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        justSaved -> "✓ Weight Saved Successfully!"
                        todayLog != null -> "Update Today's Weigh-in"
                        else -> "Save Today's Weigh-in"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

/**
 * Smart Trend Coaching card that analyzes the user's weight chart and moving average
 * to provide actionable, intelligent advice for fat loss or muscle gain improvement.
 */
@Composable
fun SmartTrendCoachingCard(
    summary: WeightTrendSummary,
    userFitnessGoal: String,
    scaleWeight: Float
) {
    val weeklyRate = summary.weeklyRateKg
    val trendWeight = summary.currentTrendKg
    val isCutting = !userFitnessGoal.contains("GAIN", ignoreCase = true)
    val waterDelta = scaleWeight - trendWeight

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BrandGreenPill)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = BrandGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Smart Trend Coaching & Insights",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Actionable suggestions derived from your weight chart & trends",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Pace Coaching Box
            val (paceTitle, paceDesc, paceColor, paceIcon) = when {
                isCutting -> when {
                    weeklyRate <= -1.0f -> Quadruple(
                        "⚠️ Fat Loss Pace Too Fast (-${String.format(Locale.US, "%.2f", abs(weeklyRate))} kg/wk)",
                        "Losing more than 1.0 kg/week risks muscle breakdown and metabolic slowdown. Add +150-200 kcal in My Meals and ensure you hit your protein target to preserve lean tissue.",
                        Color(0xFFE67E22),
                        Icons.Rounded.Warning
                    )
                    weeklyRate in -0.9f..-0.35f -> Quadruple(
                        "🔥 Optimal Sustainable Fat Loss (-${String.format(Locale.US, "%.2f", abs(weeklyRate))} kg/wk)",
                        "You are in the ideal sweet spot (0.4 to 0.8 kg/wk) where fat oxidation is maximized while 100% of lean muscle is protected. Continue your current routine!",
                        BrandGreen,
                        Icons.Rounded.CheckCircle
                    )
                    weeklyRate in -0.34f..0.15f -> Quadruple(
                        "📊 Weight Plateau / Stalling Detected",
                        "Your trend has stalled recently. Don't starve! Instead, introduce a gentle 100-150 kcal reduction in My Meals or add 2,000 steps to restart fat burn.",
                        Color(0xFF3498DB),
                        Icons.AutoMirrored.Rounded.TrendingFlat
                    )
                    else -> Quadruple(
                        "📈 Upward Weight Drift (+${String.format(Locale.US, "%.2f", weeklyRate)} kg/wk)",
                        "Your trend is rising. Double-check portion sizes in My Meals, watch for hidden cooking oils, and stay in a consistent daily deficit.",
                        Color(0xFFE74C3C),
                        Icons.AutoMirrored.Rounded.TrendingUp
                    )
                }
                else -> when {
                    weeklyRate in 0.15f..0.45f -> Quadruple(
                        "💪 Perfect Lean Bulking Pace (+${String.format(Locale.US, "%.2f", weeklyRate)} kg/wk)",
                        "Gaining at 0.2 - 0.4 kg/week matches human muscle protein synthesis limits. Excellent lean muscle accumulation with minimal fat gain!",
                        BrandGreen,
                        Icons.Rounded.FitnessCenter
                    )
                    weeklyRate > 0.5f -> Quadruple(
                        "⚠️ Surplus Too High (+${String.format(Locale.US, "%.2f", weeklyRate)} kg/wk)",
                        "Gaining faster than 0.5 kg/week stores excess calories as body fat. Dial back your surplus by 150 kcal to keep gains lean.",
                        Color(0xFFE67E22),
                        Icons.Rounded.Warning
                    )
                    else -> Quadruple(
                        "📉 Bulking Stalled (Surplus Insufficient)",
                        "Weight is not increasing. Increase your daily calories by +200 kcal (e.g. an extra banana smoothie or handful of nuts) to fuel hypertrophy.",
                        Color(0xFF3498DB),
                        Icons.AutoMirrored.Rounded.TrendingFlat
                    )
                }
            }

            CoachingTipItem(title = paceTitle, description = paceDesc, accentColor = paceColor, icon = paceIcon)

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Water Retention vs Fat Analysis
            if (abs(waterDelta) >= 0.4f) {
                if (waterDelta > 0f) {
                    CoachingTipItem(
                        title = "💧 Water Spike (+${String.format(Locale.US, "%.1f", waterDelta)} kg above trend)",
                        description = "Today's scale dot is above your smoothed trend line. Each gram of dietary carbohydrate stores 3-4g of water in muscle glycogen. High sodium, stress, or training soreness also hold water. This is temporary water, NOT fat gain! Trust your smoothed trend (${String.format(Locale.US, "%.1f", trendWeight)} kg).",
                        accentColor = Color(0xFF0984E3),
                        icon = Icons.Rounded.WaterDrop
                    )
                } else {
                    CoachingTipItem(
                        title = "💧 Low Hydration Reading (-${String.format(Locale.US, "%.1f", abs(waterDelta))} kg below trend)",
                        description = "Today's scale is temporarily lower than your smoothed trend due to mild dehydration or lower digestive food mass. Keep drinking your recommended daily water goal.",
                        accentColor = Color(0xFF00CEC9),
                        icon = Icons.Rounded.WaterDrop
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Action Recommendation
            CoachingTipItem(
                title = "🎯 Daily Action For Improvement",
                description = "Weigh yourself under identical conditions: every morning immediately after waking up and using the washroom, before eating or drinking. Consistency creates the sharpest trend clarity.",
                accentColor = TextPrimary,
                icon = Icons.Rounded.TaskAlt
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun CoachingTipItem(
    title: String,
    description: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = if (accentColor == TextPrimary) TextPrimary else accentColor
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = TextPrimary
                )
            }
        }
    }
}
