package com.example.macro_tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun InsightsScreen(
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insightsLogs by foodViewModel.insightsFoodLogs.collectAsState()
    val dailyLogs by foodViewModel.dailyFoodLogs.collectAsState()
    val calorieGoal by profileViewModel.calorieGoal.collectAsState()
    val proteinGoal by profileViewModel.proteinGoal.collectAsState()
    val fiberGoal by profileViewModel.fiberGoal.collectAsState()
    val waterGoal by profileViewModel.waterGoal.collectAsState()
    val waterLogged by profileViewModel.waterLogged.collectAsState()
    val userName by profileViewModel.userName.collectAsState()
    val weightTrendSummary by profileViewModel.weightTrendSummary.collectAsState()

    var selectedRange by remember { mutableStateOf("7 days") }
    val rangeOptions = listOf("7 days", "30 days", "90 days")
    var showExportDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Dynamic calculations for Today's Nutrition Quality (Strictly from user's logged data)
    val totalTodayProtein = dailyLogs.sumOf { it.protein.toDouble() }.toFloat()
    val totalTodayFiber = dailyLogs.sumOf { it.fiber.toDouble() }.toFloat()
    val proteinPercent = if (proteinGoal > 0) ((totalTodayProtein / proteinGoal) * 100).toInt().coerceAtMost(100) else 0
    val fiberPercent = if (fiberGoal > 0) ((totalTodayFiber / fiberGoal) * 100).toInt().coerceAtMost(100) else 0
    val waterPercent = if (waterGoal > 0f) ((waterLogged / waterGoal) * 100).toInt().coerceAtMost(100) else 0

    // Range-based grouping
    val today = LocalDate.now()
    val logsByDate = remember(insightsLogs) {
        insightsLogs.groupBy { log ->
            Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    val activeDaysWithLogs = logsByDate.keys.size
    val totalRangeCalories = insightsLogs.sumOf { it.calories }
    val avgCalories = if (activeDaysWithLogs > 0) (totalRangeCalories / activeDaysWithLogs) else 0

    // 7 day bars ending today
    val displayDays = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val dayLabels = remember(displayDays) {
        displayDays.map { day ->
            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
        }
    }
    val dayCaloriesList = remember(displayDays, logsByDate) {
        displayDays.map { day ->
            logsByDate[day]?.sumOf { it.calories } ?: 0
        }
    }

    if (showExportDialog) {
        val rangeDays = when (selectedRange) {
            "30 days" -> 30
            "90 days" -> 90
            else -> 7
        }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
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
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Export Nutrition Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Download or share your logged nutrition, macros, and trend weight report for consultations with doctors, dietitians, or personal coaches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Surface(
                        onClick = {
                            showExportDialog = false
                            val intent = com.example.macro_tracker.util.NutritionReportExporter.exportPdfReport(
                                context = context,
                                userName = userName,
                                foodLogs = insightsLogs,
                                summary = weightTrendSummary,
                                rangeDays = rangeDays
                            )
                            if (intent != null) {
                                com.example.macro_tracker.util.NutritionReportExporter.shareReport(context, intent)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clinical PDF Document", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Formatted A4 report with trend averages & tables", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showExportDialog = false
                            val intent = com.example.macro_tracker.util.NutritionReportExporter.exportCsvReport(
                                context = context,
                                userName = userName,
                                foodLogs = insightsLogs,
                                rangeDays = rangeDays
                            )
                            if (intent != null) {
                                com.example.macro_tracker.util.NutritionReportExporter.shareReport(context, intent)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Spreadsheet CSV File", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Raw data export for Excel, Google Sheets, or Apple Numbers", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = TextSecondary)
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Header Row
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
                                text = "Insights",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Your nutrition trends",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandGreenPill,
                            contentColor = BrandGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Export Report",
                            modifier = Modifier.size(16.dp),
                            tint = BrandGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreen
                        )
                    }
                }
            }

            // Time Range Tabs
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NutritrackSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rangeOptions.forEach { range ->
                            val isSelected = range == selectedRange
                            Surface(
                                onClick = {
                                    selectedRange = range
                                    val days = when (range) {
                                        "30 days" -> 30
                                        "90 days" -> 90
                                        else -> 7
                                    }
                                    foodViewModel.setInsightsRangeDays(days)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) BrandGreenPill else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = range,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) BrandGreenDark else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Average Calories Card with Real Bar Chart
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
                        Text(
                            text = "Average calories",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${String.format("%,d", avgCalories)} kcal",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp
                                ),
                                color = TextPrimary
                            )

                            if (activeDaysWithLogs == 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NutritrackBorderLight
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "No meals logged yet",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            ),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                val diffFromGoal = avgCalories - calorieGoal
                                val diffPercent = if (calorieGoal > 0) (((diffFromGoal).toFloat() / calorieGoal.toFloat()) * 100).toInt() else 0
                                val isUnder = diffPercent <= 0
                                val badgeBg = if (isUnder) BrandGreenPill else FatBg
                                val badgeColor = if (isUnder) BrandGreen else FatColor
                                val badgeIcon = if (isUnder) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
                                val badgeText = if (isUnder) "${abs(diffPercent)}% under goal" else "$diffPercent% over goal"

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = badgeBg
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = badgeIcon,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Weekly Bar Chart Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            // Goal Reference Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 36.dp)
                            ) {
                                HorizontalDivider(
                                    color = NutritrackBorder,
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Goal (${calorieGoal} kcal)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 2.dp)
                                )
                            }

                            // Bars Row
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                displayDays.forEachIndexed { index, day ->
                                    val dayCal = dayCaloriesList[index]
                                    val isToday = day == today
                                    val ratio = if (calorieGoal > 0) (dayCal.toFloat() / calorieGoal.toFloat()) else 0f

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (dayCal > 0) {
                                            val barHeight = (120 * ratio.coerceIn(0.1f, 1.25f)).dp
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .height(barHeight)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (isToday) BrandGreen else Color(0xFFD6F0DE))
                                            )
                                        } else {
                                            // Resting baseline dot when 0 calories logged
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isToday) BrandGreen.copy(alpha = 0.4f) else NutritrackBorderLight)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = dayLabels[index],
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isToday) BrandGreen else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (activeDaysWithLogs == 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Start logging your meals to see your daily calorie bars here.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Weight Trend (MacroFactor EMA Smoothing)
            item {
                com.example.macro_tracker.ui.components.WeightTrendCard(
                    summary = weightTrendSummary,
                    onLogWeight = { weight, note ->
                        profileViewModel.logWeight(weight, note)
                    }
                )
            }

            // Nutrition Quality Section
            item {
                Text(
                    text = "Nutrition quality",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
            }

            // Quality Cards (Strictly dynamic from user's logged data)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Protein
                    NutritionQualityCard(
                        title = "Protein",
                        percent = proteinPercent,
                        target = "${totalTodayProtein.toInt()}g / ${proteinGoal}g",
                        color = ProteinColor,
                        bgColor = ProteinBg,
                        progress = (proteinPercent / 100f).coerceIn(0f, 1f)
                    )

                    // Fiber (Dynamic!)
                    NutritionQualityCard(
                        title = "Fiber",
                        percent = fiberPercent,
                        target = "${totalTodayFiber.toInt()}g / ${fiberGoal}g",
                        color = BrandGreen,
                        bgColor = BrandGreenPill,
                        progress = (fiberPercent / 100f).coerceIn(0f, 1f)
                    )

                    // Water (Dynamic!)
                    NutritionQualityCard(
                        title = "Water",
                        percent = waterPercent,
                        target = "${String.format("%.1f", waterLogged)}L / ${String.format("%.1f", waterGoal)}L",
                        color = WaterColor,
                        bgColor = FatBg,
                        progress = (waterPercent / 100f).coerceIn(0f, 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionQualityCard(
    title: String,
    percent: Int,
    target: String,
    color: Color,
    bgColor: Color,
    progress: Float
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NutritrackSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = target,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            // Linear Progress Bar on the Right
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(110.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = NutritrackBorderLight
            )
        }
    }
}
