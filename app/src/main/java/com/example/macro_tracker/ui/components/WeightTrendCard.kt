package com.example.macro_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.ui.theme.*
import com.example.macro_tracker.util.WeightTrendSummary

@Composable
fun WeightTrendCard(
    summary: WeightTrendSummary,
    onLogWeight: (Float, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogDialog by remember { mutableStateOf(false) }

    if (showLogDialog) {
        var inputWeight by remember {
            mutableStateOf(if (summary.currentActualKg > 0) summary.currentActualKg.toString() else "70.0")
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
                        text = "Log Body Weight",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "MacroFactor uses daily weigh-ins to smooth out sodium & water fluctuations into a steady Trend Weight.",
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
                            onLogWeight(weight, inputNote)
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

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = NutritrackSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Title + Action Button
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPill)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Trend Weight",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Smoothed Exponential Average",
                            style = MaterialTheme.typography.labelSmall,
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Log Weight",
                        modifier = Modifier.size(14.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            if (summary.trendPoints.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NutritrackBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No weight logged yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap 'Log' above to track your weight and see your true smoothed trend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                // Key Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Smoothed Trend
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackBg,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trend",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BrandGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "EMA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BrandGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${summary.currentTrendKg} kg",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreen
                            )
                            Text(
                                text = "Scale: ${summary.currentActualKg} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Rate of change
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NutritrackBg,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Rate / week",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val rate = summary.weeklyRateKg
                            val (rateColor, rateIcon) = when {
                                rate < -0.05f -> BrandGreen to Icons.Rounded.TrendingDown
                                rate > 0.05f -> EnergyAmber to Icons.Rounded.TrendingUp
                                else -> TextSecondary to Icons.Rounded.TrendingFlat
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = rateIcon,
                                    contentDescription = null,
                                    tint = rateColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (rate > 0) "+" else ""}$rate kg",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = rateColor
                                )
                            }

                            Text(
                                text = "${if (summary.totalChangeKg > 0) "+" else ""}${summary.totalChangeKg} kg total",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
