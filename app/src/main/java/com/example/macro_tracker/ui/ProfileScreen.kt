package com.example.macro_tracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.TableChart
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
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.R
import com.example.macro_tracker.ui.theme.*

@Composable
fun ProfileDialog(
    viewModel: ProfileViewModel,
    foodViewModel: FoodViewModel? = null,
    authViewModel: AuthViewModel? = null,
    onNavigateToAuth: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val currentUserName by viewModel.userNameLiveData.observeAsState("")
    val dietaryPreference by viewModel.dietaryPreferenceLiveData.observeAsState("Non-veg")
    val activityLevel by viewModel.activityLevelLiveData.observeAsState("Sedentary")
    val currentUser by (authViewModel?.currentUserLiveData?.observeAsState() ?: remember { mutableStateOf(null) })

    var nameInput by remember(currentUserName) { mutableStateOf(currentUserName) }
    var selectedDiet by remember(dietaryPreference) { mutableStateOf(dietaryPreference) }
    var selectedActivity by remember(activityLevel) { mutableStateOf(activityLevel) }

    val dietaryOptions = listOf("Veg", "Non-veg")
    val activityOptions = listOf("Sedentary", "Active", "Gym")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = NutritrackSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Nutritrack Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Settings & Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    color = NutritrackBorderLight,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Input Section
                Column {
                    Text(
                        text = "Your Name",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("What is your name?", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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

                // Dietary Preference Section
                Column {
                    Text(
                        text = "Dietary Preference",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        dietaryOptions.forEach { option ->
                            val isSelected = option == selectedDiet
                            Surface(
                                onClick = { selectedDiet = option },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BrandGreenPill else NutritrackBg,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrandGreen) else androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) BrandGreenDark else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Activity Level Section
                Column {
                    Text(
                        text = "Activity Level",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activityOptions.forEach { option ->
                            val isSelected = option == selectedActivity
                            Surface(
                                onClick = { selectedActivity = option },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BrandGreenPill else NutritrackBg,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BrandGreen) else androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) BrandGreenDark else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Theme Mode Selector
                HorizontalDivider(color = NutritrackBorderLight, thickness = 1.dp)

                Column {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentThemeMode by viewModel.themeModeLiveData.observeAsState("SYSTEM")
                        val themes = listOf(
                            Triple("SYSTEM", "System", Icons.Rounded.BrightnessAuto),
                            Triple("LIGHT", "Light", Icons.Rounded.LightMode),
                            Triple("DARK", "Dark", Icons.Rounded.DarkMode)
                        )
                        themes.forEach { (mode, label, icon) ->
                            val isSelected = currentThemeMode.equals(mode, ignoreCase = true)
                            Surface(
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BrandGreenPill else NutritrackBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BrandGreen else NutritrackBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) BrandGreen else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) BrandGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminders & Alerts
                HorizontalDivider(color = NutritrackBorderLight, thickness = 1.dp)

                val context = androidx.compose.ui.platform.LocalContext.current
                var remindersEnabled by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Meal & Water Reminders",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Smart alerts for meals, hydration & streak protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { isChecked ->
                            remindersEnabled = isChecked
                            if (isChecked) {
                                com.example.macro_tracker.util.ReminderScheduler.scheduleAllReminders(context)
                            } else {
                                com.example.macro_tracker.util.ReminderScheduler.cancelAllReminders(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandGreen,
                            uncheckedThumbColor = NutritrackBorder,
                            uncheckedTrackColor = NutritrackBg
                        )
                    )
                }

                // Data & Reports Section
                HorizontalDivider(color = NutritrackBorderLight, thickness = 1.dp)

                Column {
                    Text(
                        text = "Data & Reports",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = com.example.macro_tracker.util.NutritionReportExporter.exportCsvReport(
                                context = context,
                                userName = currentUserName,
                                foodLogs = emptyList(),
                                rangeDays = 30
                            )
                            if (intent != null) {
                                com.example.macro_tracker.util.NutritionReportExporter.shareReport(context, intent)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Nutrition Data (CSV)", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Account Section
                if (authViewModel != null) {
                    HorizontalDivider(color = NutritrackBorderLight, thickness = 1.dp)

                    Column {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NutritrackBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentUser?.email?.ifBlank { "Signed In" } ?: "Account",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Cloud Synced",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandGreen
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            authViewModel.signOut(wipeLocalData = false) {
                                                onDismissRequest()
                                                onNavigateToAuth?.invoke()
                                            }
                                        }
                                    ) {
                                        Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                var showDeleteConfirm by remember { mutableStateOf(false) }

                                if (showDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirm = false },
                                        title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
                                        text = { Text("This will permanently delete your account, tracked meals, macros, and profile data from cloud servers and this device. This operation cannot be undone.") },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    showDeleteConfirm = false
                                                    authViewModel.deleteAccount {
                                                        onDismissRequest()
                                                        onNavigateToAuth?.invoke()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                            ) {
                                                Text("Permanently Delete", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirm = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }

                                HorizontalDivider(color = NutritrackBorderLight.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Danger Zone",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    TextButton(onClick = { showDeleteConfirm = true }) {
                                        Text("Delete Account", color = ErrorRed.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
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
                    if (nameInput.isNotBlank()) {
                        viewModel.saveUserName(nameInput.trim())
                    }
                    viewModel.saveDietaryPreference(selectedDiet)
                    viewModel.saveActivityLevel(selectedActivity)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NutritrackDark,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Save Settings",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
        },
        dismissButton = null
    )
}
