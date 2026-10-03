package com.example.macro_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.ui.theme.*

@Composable
fun AdjustMealQuantityDialog(
    log: FoodLogEntity,
    onDismissRequest: () -> Unit,
    onUpdateWeight: (Float) -> Unit,
    onDeleteMeal: () -> Unit
) {
    val initialWeight = if (log.weightGrams > 0f) log.weightGrams else 100f
    var weightInput by remember { mutableStateOf(initialWeight.toInt().toString()) }
    var currentWeight by remember { mutableFloatStateOf(initialWeight) }
    var isError by remember { mutableStateOf(false) }

    val baseWeight = if (log.weightGrams > 0f) log.weightGrams else 100f
    val ratio = if (baseWeight > 0f) currentWeight / baseWeight else 1f

    val previewCalories = kotlin.math.round(log.calories * ratio).toInt().coerceAtLeast(0)
    val previewProtein = (log.protein * ratio).coerceAtLeast(0f)
    val previewCarbs = (log.carbs * ratio).coerceAtLeast(0f)
    val previewFat = (log.fat * ratio).coerceAtLeast(0f)
    val previewFiber = (log.fiber * ratio).coerceAtLeast(0f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = NutritrackSurface,
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
                        imageVector = Icons.Rounded.Scale,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Adjust Quantity & Weight",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = log.foodName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Weight input in grams
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        weightInput = clean
                        val parsed = clean.toFloatOrNull()
                        if (parsed != null && parsed > 0f) {
                            currentWeight = parsed
                            isError = false
                        } else {
                            isError = true
                        }
                    },
                    label = { Text("Weight in Grams (gm)") },
                    trailingIcon = {
                        Text(
                            text = "gm",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreen,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                // Quick preset weight chips
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(50f, 100f, 150f, 200f, 250f).forEach { presetGrams ->
                        val isSel = currentWeight.toInt() == presetGrams.toInt()
                        Surface(
                            onClick = {
                                currentWeight = presetGrams
                                weightInput = presetGrams.toInt().toString()
                                isError = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) BrandGreen else NutritrackBg,
                            border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${presetGrams.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSel) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Live calculated nutritional values
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NutritrackBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Calories for ${currentWeight.toInt()}g",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = TextSecondary
                            )
                            Text(
                                text = "$previewCalories kcal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "P: ${previewProtein.toInt()}g",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ProteinColor
                            )
                            Text(
                                text = "C: ${previewCarbs.toInt()}g",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CarbsColor
                            )
                            Text(
                                text = "F: ${previewFat.toInt()}g",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = FatColor
                            )
                            if (previewFiber > 0f) {
                                Text(
                                    text = "Fib: ${previewFiber.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandGreenDark
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isError && currentWeight > 0f) {
                        onUpdateWeight(currentWeight)
                        onDismissRequest()
                    }
                },
                enabled = !isError && currentWeight > 0f,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Quantity", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onDeleteMeal()
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}
