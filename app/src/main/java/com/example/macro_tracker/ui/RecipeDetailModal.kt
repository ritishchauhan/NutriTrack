package com.example.macro_tracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.data.model.Recipe
import com.example.macro_tracker.ui.theme.*

/**
 * Reusable full-detail bottom sheet / dialog displaying step-by-step Indian kitchen
 * recipe instructions, ingredients, macros, and direct "Add to Today's Meals" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailModal(
    recipe: Recipe,
    defaultMealType: String = "Breakfast",
    onDismissRequest: () -> Unit,
    onAddToMeals: (mealType: String, recipe: Recipe) -> Unit
) {
    var selectedMealType by remember { mutableStateOf(defaultMealType) }
    var showMealSelector by remember { mutableStateOf(false) }
    val mealOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NutritrackSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NutritrackBorderLight)
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            // Header: Veg/Non-Veg Tag & Cooking Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Veg / Non-Veg Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (recipe.isVeg) BrandGreenPill else Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (recipe.isVeg) BrandGreen.copy(alpha = 0.3f) else Color(0xFFEF5350).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (recipe.isVeg) BrandGreen else Color(0xFFD32F2F))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (recipe.isVeg) "PURE VEG" else "NON-VEG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (recipe.isVeg) BrandGreenDark else Color(0xFFC62828)
                        )
                    }
                }

                // Time Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NutritrackBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.totalTimeMinutes} mins (Prep: ${recipe.prepTimeMinutes}m)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recipe Title
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )

            // Tags
            if (recipe.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recipe.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NutritrackBg
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nutritional Breakdown Grid
            Text(
                text = "Nutrition per ${recipe.servingSize}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroCardItem(
                    label = "Calories",
                    value = "${recipe.calories}",
                    unit = "kcal",
                    color = BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                MacroCardItem(
                    label = "Protein",
                    value = "${recipe.protein.toInt()}",
                    unit = "g",
                    color = MacroProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroCardItem(
                    label = "Carbs",
                    value = "${recipe.carbs.toInt()}",
                    unit = "g",
                    color = MacroCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroCardItem(
                    label = "Fat",
                    value = "${recipe.fat.toInt()}",
                    unit = "g",
                    color = MacroFat,
                    modifier = Modifier.weight(1f)
                )
                MacroCardItem(
                    label = "Fiber",
                    value = "${recipe.fiber.toInt()}",
                    unit = "g",
                    color = Color(0xFF00B894),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Ingredients Section
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NutritrackBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Kitchen,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ingredients (${recipe.ingredients.size} items)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    recipe.ingredients.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step-by-Step Cooking Method
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NutritrackBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cooking Instructions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    recipe.instructions.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPill)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = BrandGreenDark
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Meal Type Selector before adding
            Text(
                text = "Log to Meal:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealOptions.forEach { type ->
                    val isSel = type.equals(selectedMealType, ignoreCase = true)
                    Surface(
                        onClick = { selectedMealType = type },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) NutritrackDark else NutritrackBg,
                        border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = type,
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

            // Action Buttons
            Button(
                onClick = {
                    onAddToMeals(selectedMealType, recipe)
                    onDismissRequest()
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add to Today's $selectedMealType",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MacroCardItem(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NutritrackBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = color
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted
            )
        }
    }
}
