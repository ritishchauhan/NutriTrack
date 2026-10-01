package com.example.macro_tracker.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.ui.theme.*
import java.util.Locale

private const val DEVELOPER_UPI_ID = "ritishchauhan.in@oksbi"
private const val DEVELOPER_NAME = "Ritish Chauhan"
private const val UPI_PAYMENT_NOTE = "Buy a coffee for the developer"

data class CoffeeTier(
    val amount: Int,
    val title: String,
    val subtitle: String,
    val iconEmoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyCoffeeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val coffeeTiers = remember {
        listOf(
            CoffeeTier(50, "Espresso", "A quick shot of love", "☕"),
            CoffeeTier(100, "Cappuccino", "Keeps the code flowing", "☕☕"),
            CoffeeTier(250, "Coffee & Croissant", "Generous supporter", "🥐"),
            CoffeeTier(500, "Super Supporter", "Fuels major updates", "🚀")
        )
    }

    var selectedTierAmount by remember { mutableIntStateOf(50) }
    var isCustomAmount by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("150") }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val effectiveAmount: Double = if (isCustomAmount) {
        customAmountText.toDoubleOrNull() ?: 50.0
    } else {
        selectedTierAmount.toDouble()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
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
                            text = "Buy Dev a Coffee ☕",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Support NutriTrack's creator",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            // Hero Coffee Card
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF78350F), // Rich Warm Espresso
                                        Color(0xFF92400E), // Roasted Coffee Amber
                                        Color(0xFFB45309)  // Warm Golden Caramel
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "☕",
                                            fontSize = 28.sp
                                        )
                                    }
                                }

                                Column {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "INDEPENDENT DEVELOPER",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp,
                                                fontSize = 10.sp
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Ritish Chauhan",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Hey there! I am the solo developer behind NutriTrack. If this app helps you stay on track, reach your nutrition targets, and build healthier habits, buying me a coffee directly helps me maintain servers and develop exciting new features.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp
                                ),
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // Developer UPI ID Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = NutritrackSurface,
                    border = BorderStroke(1.dp, NutritrackBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DEVELOPER UPI ID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextMuted
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandGreenPill
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = "Verified",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Verified VPA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = BrandGreenDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NutritrackBg)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = DEVELOPER_UPI_ID,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Name: $DEVELOPER_NAME",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = {
                                    copyUpiToClipboard(context, DEVELOPER_UPI_ID)
                                    copiedToClipboard = true
                                    Toast.makeText(context, "UPI ID copied: $DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedToClipboard) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy UPI ID",
                                    tint = if (copiedToClipboard) BrandGreen else BrandGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Choose Coffee Tier Section
            item {
                Text(
                    text = "Select a treat",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = TextPrimary
                )
            }

            // Tier Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    coffeeTiers.forEach { tier ->
                        val isSelected = !isCustomAmount && selectedTierAmount == tier.amount

                        Surface(
                            onClick = {
                                isCustomAmount = false
                                selectedTierAmount = tier.amount
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color(0xFFFEF3C7) else NutritrackSurface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD97706) else NutritrackBorderLight
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = tier.iconEmoji,
                                        fontSize = 24.sp
                                    )
                                    Column {
                                        Text(
                                            text = tier.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = tier.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFB45309) else NutritrackBg
                                ) {
                                    Text(
                                        text = "₹${tier.amount}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Amount Option
                    Surface(
                        onClick = { isCustomAmount = true },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCustomAmount) Color(0xFFFEF3C7) else NutritrackSurface,
                        border = BorderStroke(
                            width = if (isCustomAmount) 2.dp else 1.dp,
                            color = if (isCustomAmount) Color(0xFFD97706) else NutritrackBorderLight
                        ),
                        shadowElevation = if (isCustomAmount) 2.dp else 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(text = "✨", fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = "Custom Amount",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Enter any amount you'd like to support with",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isCustomAmount,
                                    onClick = { isCustomAmount = true },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFD97706)
                                    )
                                )
                            }

                            AnimatedVisibility(visible = isCustomAmount) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    OutlinedTextField(
                                        value = customAmountText,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 6) {
                                                customAmountText = input
                                            }
                                        },
                                        leadingIcon = {
                                            Text(
                                                text = "₹",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        },
                                        label = { Text("Amount in INR") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFD97706),
                                            unfocusedBorderColor = NutritrackBorder,
                                            focusedContainerColor = NutritrackBg,
                                            unfocusedContainerColor = NutritrackBg,
                                            cursorColor = Color(0xFFD97706)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Big CTA: Pay via UPI
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val amountParam = if (effectiveAmount > 0) String.format(Locale.US, "%.0f", effectiveAmount) else "50"
                        launchUpiPayment(
                            context = context,
                            amount = amountParam,
                            upiId = DEVELOPER_UPI_ID,
                            payeeName = DEVELOPER_NAME,
                            note = UPI_PAYMENT_NOTE,
                            onFailure = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF92400E), // Warm Rich Coffee
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Coffee,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pay ₹${String.format(Locale.US, "%.0f", effectiveAmount)} via any UPI App",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Opens Google Pay, PhonePe, Paytm, BHIM, Cred or any installed UPI app automatically.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Footer note
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NutritrackSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandGreenPill,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "Thank you for using NutriTrack and supporting independent software development! 💚",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp
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
 * Copies the given UPI ID string to the system clipboard.
 */
private fun copyUpiToClipboard(context: Context, upiId: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", upiId)
        clipboard.setPrimaryClip(clip)
    } catch (_: Exception) {}
}

/**
 * Launches an Android UPI Intent via standard `upi://pay` URI scheme with chooser.
 * Prompts user to select any installed UPI app (GPay, PhonePe, Paytm, BHIM, etc.)
 * and automatically populates the recipient UPI ID, payee name, and amount.
 */
fun launchUpiPayment(
    context: Context,
    amount: String,
    upiId: String = DEVELOPER_UPI_ID,
    payeeName: String = DEVELOPER_NAME,
    note: String = UPI_PAYMENT_NOTE,
    onFailure: (String) -> Unit
) {
    try {
        val uriBuilder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("cu", "INR")

        val num = amount.toDoubleOrNull()
        if (num != null && num > 0) {
            uriBuilder.appendQueryParameter("am", String.format(Locale.US, "%.2f", num))
        }

        val upiUri = uriBuilder.build()
        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
        val chooser = Intent.createChooser(upiIntent, "Pay ₹$amount with any UPI App")

        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        copyUpiToClipboard(context, upiId)
        onFailure("No UPI application found on this device. UPI ID ($upiId) has been copied to your clipboard!")
    } catch (e: Exception) {
        copyUpiToClipboard(context, upiId)
        onFailure("Could not open UPI app. UPI ID ($upiId) copied to clipboard.")
    }
}
