package com.example.macro_tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.ui.theme.*

@Composable
fun GoalsScreen(
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBuyCoffee: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dailyLogs by foodViewModel.dailyFoodLogs.collectAsState()
    val calorieGoal by profileViewModel.calorieGoal.collectAsState()
    val proteinGoal by profileViewModel.proteinGoal.collectAsState()
    val carbsGoal by profileViewModel.carbsGoal.collectAsState()
    val fatGoal by profileViewModel.fatGoal.collectAsState()
    val fiberGoal by profileViewModel.fiberGoal.collectAsState()
    val waterGoal by profileViewModel.waterGoal.collectAsState()
    val waterLogged by profileViewModel.waterLogged.collectAsState()
    val streakDays by profileViewModel.streakDays.collectAsState()

    val totalCalories = dailyLogs.sumOf { it.calories }
    val totalProtein = dailyLogs.sumOf { it.protein.toDouble() }.toFloat()

    val calProgress = if (calorieGoal > 0) (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val proteinProgress = if (proteinGoal > 0) (totalProtein / proteinGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val waterProgress = if (waterGoal > 0f) (waterLogged / waterGoal).coerceIn(0f, 1f) else 0f

    var showEditTargetsDialog by remember { mutableStateOf(false) }

    if (showEditTargetsDialog) {
        var calInput by remember { mutableStateOf(calorieGoal.toString()) }
        var proInput by remember { mutableStateOf(proteinGoal.toString()) }
        var carbsInput by remember { mutableStateOf(carbsGoal.toString()) }
        var fatInput by remember { mutableStateOf(fatGoal.toString()) }
        var fiberInput by remember { mutableStateOf(fiberGoal.toString()) }
        var waterInput by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", waterGoal)) }

        AlertDialog(
            onDismissRequest = { showEditTargetsDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = NutritrackSurface,
            title = { Text("Edit Daily Targets", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = NutritrackBorder,
                        focusedContainerColor = NutritrackBg,
                        unfocusedContainerColor = NutritrackBg,
                        cursorColor = BrandGreen
                    )
                    OutlinedTextField(
                        value = calInput,
                        onValueChange = { calInput = it },
                        label = { Text("Daily Calories (kcal)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proInput,
                        onValueChange = { proInput = it },
                        label = { Text("Daily Protein (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it },
                        label = { Text("Daily Carbs (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it },
                        label = { Text("Daily Fat (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fiberInput,
                        onValueChange = { fiberInput = it },
                        label = { Text("Daily Fiber (g)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waterInput,
                        onValueChange = { waterInput = it },
                        label = { Text("Daily Water (Liters)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = (calInput.toIntOrNull() ?: calorieGoal).coerceIn(500, 10000)
                        val p = (proInput.toIntOrNull() ?: proteinGoal).coerceIn(10, 500)
                        val cb = (carbsInput.toIntOrNull() ?: carbsGoal).coerceIn(10, 1000)
                        val f = (fatInput.toIntOrNull() ?: fatGoal).coerceIn(5, 300)
                        val fb = (fiberInput.toIntOrNull() ?: fiberGoal).coerceIn(5, 150)
                        val w = (waterInput.toFloatOrNull() ?: waterGoal).coerceIn(0.5f, 15f)
                        profileViewModel.saveGoals(c, p, cb, f, w, fb)
                        showEditTargetsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NutritrackDark)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTargetsDialog = false }) {
                    Text("Cancel")
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

                    Column {
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Set targets that fit you",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            // Daily Targets Card
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
                            text = "DAILY TARGETS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = BrandGreen
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TargetRow(label = "Calories", value = "${String.format(java.util.Locale.US, "%,d", calorieGoal)} kcal")
                        HorizontalDivider(color = NutritrackBorderLight, modifier = Modifier.padding(vertical = 12.dp))
                        TargetRow(label = "Protein", value = "$proteinGoal g")
                        HorizontalDivider(color = NutritrackBorderLight, modifier = Modifier.padding(vertical = 12.dp))
                        TargetRow(label = "Carbs", value = "$carbsGoal g")
                        HorizontalDivider(color = NutritrackBorderLight, modifier = Modifier.padding(vertical = 12.dp))
                        TargetRow(label = "Fat", value = "$fatGoal g")
                        HorizontalDivider(color = NutritrackBorderLight, modifier = Modifier.padding(vertical = 12.dp))
                        TargetRow(label = "Fiber", value = "$fiberGoal g")
                        HorizontalDivider(color = NutritrackBorderLight, modifier = Modifier.padding(vertical = 12.dp))
                        TargetRow(label = "Water", value = "${String.format(java.util.Locale.US, "%.1f", waterGoal)} L")
                    }
                }
            }

            // Your Progress Section
            item {
                Text(
                    text = "Your progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Calories Progress Card
                    ProgressTargetCard(
                        label = "Calories",
                        current = String.format("%,d", totalCalories),
                        target = String.format("%,d", calorieGoal),
                        progress = calProgress,
                        color = BrandGreen
                    )

                    // Protein Progress Card
                    ProgressTargetCard(
                        label = "Protein",
                        current = "${totalProtein.toInt()}",
                        target = "$proteinGoal g",
                        progress = proteinProgress,
                        color = ProteinColor
                    )

                    // Water Progress Card (with tap to log +0.25L and -0.25L)
                    ProgressTargetCard(
                        label = "Water",
                        current = String.format("%.1f", waterLogged),
                        target = "${String.format("%.1f", waterGoal)} L",
                        progress = waterProgress,
                        color = WaterColor,
                        onAddWater = { profileViewModel.addWater(0.25f) },
                        onDecreaseWater = { profileViewModel.decreaseWater(0.25f) }
                    )
                }
            }

            // Edit Daily Targets Button
            item {
                Button(
                    onClick = { showEditTargetsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritrackDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Edit daily targets",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            // Consistency Section
            item {
                Text(
                    text = "Consistency",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
            }

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
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔥 $streakDays day streak",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )

                        Text(
                            text = "${7 - (streakDays % 7)} days to 7",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = BrandGreen
                        )
                    }
                }
            }

            // Buy a Coffee for the Dev Section
            item {
                Text(
                    text = "Support the Developer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
            }

            item {
                Surface(
                    onClick = onNavigateToBuyCoffee,
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD97706),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Coffee,
                                        contentDescription = "Buy Coffee",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Buy a Coffee to the Dev ☕",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "UPI: ritishchauhan.in@oksbi",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Enjoying NutriTrack? Buy a coffee for Ritish to support ongoing updates, nutrition databases, and keeping the app 100% ad-free!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = Color(0xFF78350F)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToBuyCoffee,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF92400E),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Coffee,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buy a Coffee via UPI ☕",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}

@Composable
fun ProgressTargetCard(
    label: String,
    current: String,
    target: String,
    progress: Float,
    color: Color,
    onAddWater: (() -> Unit)? = null,
    onDecreaseWater: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NutritrackSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = NutritrackBorderLight
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$current / $target",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )

                    if (onDecreaseWater != null) {
                        Surface(
                            onClick = onDecreaseWater,
                            shape = CircleShape,
                            color = NutritrackBorderLight,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease Water",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (onAddWater != null) {
                        Surface(
                            onClick = onAddWater,
                            shape = CircleShape,
                            color = BrandGreenPill,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add Water",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
