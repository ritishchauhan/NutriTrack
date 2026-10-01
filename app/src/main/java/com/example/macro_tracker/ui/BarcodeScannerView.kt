package com.example.macro_tracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.macro_tracker.data.remote.Nutriments
import com.example.macro_tracker.data.remote.Product
import com.example.macro_tracker.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Camera-based Barcode Scanner Modal with ML Kit and CameraX.
 * Provides a live camera viewfinder, animated scanning laser, torch control,
 * manual barcode entry, and sample food barcodes for quick testing.
 */
@Composable
fun BarcodeCameraScannerModal(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraViewFinder(
                    onBarcodeDetected = { barcode ->
                        onBarcodeScanned(barcode)
                    }
                )
            } else {
                // Permission Request Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = NutritrackSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(BrandGreen.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Camera Permission Required",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = OutfitFontFamily
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Nutritrack needs camera access to scan food package barcodes and instantly calculate nutritional macros.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = OutfitFontFamily
                                ),
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Allow Camera Access", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showManualInputDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Barcode Manually")
                            }

                            TextButton(onClick = onDismissRequest) {
                                Text("Cancel", color = TextMuted)
                            }
                        }
                    }
                }
            }

            // Top action buttons overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan Barcode",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }

                IconButton(
                    onClick = { showManualInputDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = "Manual Code",
                        tint = Color.White
                    )
                }
            }

            // Bottom quick test barcodes & instructions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Point camera at food packaging barcode",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = OutfitFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Quick test barcode chips (helps testing immediately without physical food package)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Quick Test Barcodes:",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )

                    val sampleBarcodes = listOf(
                        "3017620422003" to "Nutella",
                        "737628064502" to "Ramen Bowl",
                        "04963406" to "Coca-Cola",
                        "073852002116" to "Greek Yogurt"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(sampleBarcodes) { (code, name) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandGreen.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    onBarcodeScanned(code)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$name ($code)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Barcode Input Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            containerColor = NutritrackSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Pin, contentDescription = null, tint = BrandGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Barcode Number", fontFamily = OutfitFontFamily)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Type the numbers printed under the product's barcode:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = manualBarcodeText,
                        onValueChange = { manualBarcodeText = it },
                        label = { Text("Barcode (UPC/EAN)") },
                        placeholder = { Text("e.g. 3017620422003") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = NutritrackBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = manualBarcodeText.trim()
                        if (code.isNotEmpty()) {
                            showManualInputDialog = false
                            onBarcodeScanned(code)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    enabled = manualBarcodeText.isNotBlank()
                ) {
                    Text("Lookup Food", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * CameraX Viewfinder with ML Kit Barcode Analyzer and scanning overlay reticle.
 */
@Composable
fun CameraViewFinder(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProcessingBarcode by remember { mutableStateOf(false) }

    // Reticle laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                if (providerFuture.isDone) {
                    providerFuture.get()?.unbindAll()
                }
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39
                            )
                            .build()
                    )

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isProcessingBarcode) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawVal = barcode.rawValue
                                        if (!rawVal.isNullOrBlank() && !isProcessingBarcode) {
                                            isProcessingBarcode = true
                                            previewView.post {
                                                onBarcodeDetected(rawVal)
                                            }
                                            break
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Log.e("BarcodeScanner", "Barcode scanning failed", it)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("BarcodeScanner", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Target Frame Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 180.dp)
                    .border(
                        width = 2.dp,
                        color = BrandGreen.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                // Corner Accents
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .border(
                            width = 4.dp,
                            color = BrandGreen,
                            shape = RoundedCornerShape(topStart = 18.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .border(
                            width = 4.dp,
                            color = BrandGreen,
                            shape = RoundedCornerShape(topEnd = 18.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .border(
                            width = 4.dp,
                            color = BrandGreen,
                            shape = RoundedCornerShape(bottomStart = 18.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .border(
                            width = 4.dp,
                            color = BrandGreen,
                            shape = RoundedCornerShape(bottomEnd = 18.dp)
                        )
                )

                // Laser scan line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (176 * laserOffsetY).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    BrandGreen,
                                    BrandGreenLight,
                                    BrandGreen,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Modal dialog presenting detected food and full nutritional details,
 * with explicit action buttons:
 * "Add to Daily Meals" and "Discard".
 */
@Composable
fun ScannedFoodDetailModal(
    product: Product,
    scannedBarcode: String,
    onDismissRequest: () -> Unit,
    onAddToMeals: (mealType: String, servingMultiplier: Float, customName: String) -> Unit,
    onDiscard: () -> Unit
) {
    var selectedMeal by remember { mutableStateOf("Lunch") }
    var servingMultiplier by remember { mutableFloatStateOf(1.0f) }
    var foodName by remember {
        mutableStateOf(
            product.product_name?.ifBlank { "Scanned Food Product" } ?: "Scanned Food Product"
        )
    }

    val mealOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val servingOptions = listOf(0.5f to "½ serving", 1.0f to "1 serving", 1.5f to "1.5x", 2.0f to "2x")

    val baseCal = product.nutriments?.calories ?: 0
    val basePro = product.nutriments?.proteinGrams ?: 0f
    val baseCarbs = product.nutriments?.carbsGrams ?: 0f
    val baseFat = product.nutriments?.fatGrams ?: 0f
    val baseFiber = product.nutriments?.fiberGrams ?: 0f

    val currentCal = (baseCal * servingMultiplier).toInt()
    val currentPro = basePro * servingMultiplier
    val currentCarbs = baseCarbs * servingMultiplier
    val currentFat = baseFat * servingMultiplier
    val currentFiber = baseFiber * servingMultiplier

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = NutritrackSurface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Scan Tag & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Barcode Detected: $scannedBarcode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen,
                                fontFamily = OutfitFontFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted
                        )
                    }
                }

                // Food identity (Image + Name + Brand)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!product.image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = product.image_url,
                            contentDescription = foodName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, NutritrackBorder, RoundedCornerShape(16.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fastfood,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = OutfitFontFamily
                            ),
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!product.brands.isNullOrBlank()) {
                            Text(
                                text = "Brand: ${product.brands}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = OutfitFontFamily
                            )
                        }

                        if (!product.serving_size.isNullOrBlank()) {
                            Text(
                                text = "Serving size: ${product.serving_size}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = OutfitFontFamily
                            )
                        }
                    }
                }

                HorizontalDivider(color = NutritrackBorder.copy(alpha = 0.6f))

                // Big Calories Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NutritrackBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Energy",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = OutfitFontFamily
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$currentCal",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandGreen,
                                    fontFamily = OutfitFontFamily
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kcal",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    fontFamily = OutfitFontFamily
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = EnergyAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = EnergyAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 4 Macronutrients Grid (Protein, Carbs, Fat, Fiber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroDetailPill(
                        label = "Protein",
                        value = "${String.format("%.1f", currentPro)}g",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    MacroDetailPill(
                        label = "Carbs",
                        value = "${String.format("%.1f", currentCarbs)}g",
                        color = EnergyAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MacroDetailPill(
                        label = "Fat",
                        value = "${String.format("%.1f", currentFat)}g",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    MacroDetailPill(
                        label = "Fiber",
                        value = "${String.format("%.1f", currentFiber)}g",
                        color = BrandGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Serving Size Multiplier
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Portion / Serving Multiplier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = OutfitFontFamily
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        servingOptions.forEach { (factor, label) ->
                            val isSelected = servingMultiplier == factor
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BrandGreen else NutritrackBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandGreen else NutritrackBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { servingMultiplier = factor }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontFamily = OutfitFontFamily,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Meal Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Assign to Meal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = OutfitFontFamily
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mealOptions.forEach { meal ->
                            val isSelected = selectedMeal.equals(meal, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BrandGreen.copy(alpha = 0.15f) else NutritrackBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandGreen else NutritrackBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMeal = meal }
                            ) {
                                Text(
                                    text = meal,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandGreen else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontFamily = OutfitFontFamily,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // EXPLICIT ACTION BUTTONS AS REQUESTED:
                // 1. "Add to Daily Meals"
                // 2. "Discard"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onAddToMeals(selectedMeal, servingMultiplier, foodName)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Daily Meals",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFontFamily
                        )
                    }

                    OutlinedButton(
                        onClick = onDiscard,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Discard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = OutfitFontFamily,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroDetailPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NutritrackBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = OutfitFontFamily
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = OutfitFontFamily
            )
        }
    }
}
