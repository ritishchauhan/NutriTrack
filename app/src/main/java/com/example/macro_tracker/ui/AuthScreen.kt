package com.example.macro_tracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.R
import com.example.macro_tracker.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Observing lifecycle-aware LiveData for UI updates
    val authState by authViewModel.authStateLiveData.observeAsState(AuthUiState())

    var isSignUpMode by remember { mutableStateOf(false) }

    // Form state
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }

    // Forgot password state
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmailInput by remember { mutableStateOf("") }
    var forgotEmailTouched by remember { mutableStateOf(false) }

    // Email validation
    val cleanEmail = emailInput.trim()
    val isEmailValid = AuthViewModel.isValidEmail(cleanEmail)
    val emailWarning = when {
        emailTouched && cleanEmail.isEmpty() -> "Email address is required."
        emailTouched && !isEmailValid -> "Please enter a valid email address (e.g. name@example.com)."
        else -> null
    }

    // Setup Google Sign-In Client
    val webClientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        ""
    }

    val googleSignInClient = remember(webClientId) {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (webClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(webClientId)
        }
        GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                authViewModel.signInWithGoogle(idToken, onNavigateToHome)
            } else {
                authViewModel.setValidationError(
                    "Google Sign-In succeeded, but ID token was missing. Please verify Web Client ID in strings.xml."
                )
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                10 -> "Google Sign-In configuration error (Code 10: DEVELOPER_ERROR). Please ensure your SHA-1 fingerprint and Google Sign-in provider are added to Firebase Console (nutritrack-e5534)."
                12501 -> null // User explicitly closed or tapped outside the Google account chooser; no error message needed
                12500 -> "Google Sign-In canceled or configuration mismatch occurred."
                7 -> "Network error during Google Sign-In. Please check your connection."
                else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Check Firebase configuration"}"
            }
            if (msg != null) {
                authViewModel.setValidationError(msg)
            }
        } catch (e: Exception) {
            authViewModel.setValidationError("Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    // Reset any cached Google Sign-In session in Google Play Services so that the
    // account picker dialog is always displayed when entering the Auth screen.
    LaunchedEffect(Unit) {
        try {
            googleSignInClient.signOut()
        } catch (ignored: Exception) {}
    }

    // Clear messages when toggling mode
    LaunchedEffect(isSignUpMode) {
        authViewModel.clearMessages()
        emailTouched = false
    }

    Scaffold(
        containerColor = NutritrackBg,
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BrandGreenPill.copy(alpha = 0.6f),
                            NutritrackBg,
                            NutritrackBg
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // App Brand Logo
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = NutritrackSurface,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandGreenLight),
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Nutritrack Logo",
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Brand Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NUTRI",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "TRACK",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp
                        ),
                        color = BrandGreen
                    )
                }

                Text(
                    text = if (isSignUpMode) "Create your account to start tracking" else "Fuel your life with precision macros",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
                )

                // Segmented Tab Switcher: Sign In vs Sign Up
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackSurface,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutritrackBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        // Sign In Tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isSignUpMode) BrandGreen else Color.Transparent)
                                .clickable { isSignUpMode = false }
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (!isSignUpMode) Color.White else TextSecondary
                            )
                        }

                        // Sign Up Tab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSignUpMode) BrandGreen else Color.Transparent)
                                .clickable { isSignUpMode = true }
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (isSignUpMode) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error / Success Banners
                AnimatedVisibility(visible = authState.errorMessage != null) {
                    authState.errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = authState.successMessage != null) {
                    authState.successMessage?.let { success ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BrandGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = success,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // Email / Password Input Form Card
                // ==========================================
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = NutritrackSurface,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Full Name (Only in Sign-Up)
                        AnimatedVisibility(visible = isSignUpMode) {
                            Column {
                                Text(
                                    text = "Full Name",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    placeholder = { Text("e.g. Ritish Chauhan", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Person, contentDescription = null, tint = BrandGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Email Field with Strict Warning
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Email Address",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                if (emailWarning != null) {
                                    Text(
                                        text = "Invalid Email",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ErrorRed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    emailTouched = true
                                    if (authState.errorMessage != null) {
                                        authViewModel.clearMessages()
                                    }
                                },
                                placeholder = { Text("you@example.com", color = TextMuted) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Email,
                                        contentDescription = null,
                                        tint = if (emailWarning != null) ErrorRed else BrandGreen
                                    )
                                },
                                trailingIcon = {
                                    if (emailWarning != null) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = "Invalid email format",
                                            tint = ErrorRed
                                        )
                                    } else if (emailTouched && isEmailValid && cleanEmail.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Valid email",
                                            tint = BrandGreen
                                        )
                                    }
                                },
                                isError = emailWarning != null,
                                supportingText = {
                                    if (emailWarning != null) {
                                        Text(
                                            text = "⚠️ $emailWarning",
                                            color = ErrorRed,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(14.dp),
                                colors = authTextFieldColors(isError = emailWarning != null),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Password Field
                        Column {
                            Text(
                                text = "Password",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                placeholder = { Text("At least 6 characters", color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandGreen)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = "Toggle Password Visibility",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (!isSignUpMode) {
                                            authViewModel.signIn(emailInput, passwordInput, onNavigateToHome)
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = authTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Confirm Password (Only in Sign-Up)
                        AnimatedVisibility(visible = isSignUpMode) {
                            Column {
                                Text(
                                    text = "Confirm Password",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    placeholder = { Text("Re-enter your password", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandGreen)
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (passwordInput == confirmPasswordInput) {
                                                authViewModel.signUp(emailInput, passwordInput, nameInput, onNavigateToHome)
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Forgot Password Link
                        if (!isSignUpMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandGreen,
                                    modifier = Modifier.clickable {
                                        forgotEmailInput = emailInput
                                        showForgotDialog = true
                                    }
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                emailTouched = true
                                val targetEmail = emailInput.trim()

                                if (targetEmail.isEmpty()) {
                                    authViewModel.setValidationError("Please enter your email address.")
                                    return@Button
                                }
                                if (!AuthViewModel.isValidEmail(targetEmail)) {
                                    authViewModel.setValidationError("Please enter a valid email address (e.g. name@example.com).")
                                    return@Button
                                }
                                if (passwordInput.isEmpty()) {
                                    authViewModel.setValidationError("Please enter your password.")
                                    return@Button
                                }

                                if (isSignUpMode) {
                                    if (passwordInput.length < 6) {
                                        authViewModel.setValidationError("Password must be at least 6 characters long.")
                                        return@Button
                                    }
                                    if (passwordInput != confirmPasswordInput) {
                                        authViewModel.setValidationError("Passwords do not match. Please re-enter.")
                                        return@Button
                                    }
                                    authViewModel.signUp(targetEmail, passwordInput, nameInput.trim(), onNavigateToHome)
                                } else {
                                    authViewModel.signIn(targetEmail, passwordInput, onNavigateToHome)
                                }
                            },
                            enabled = !authState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (authState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) "Create Account" else "Sign In with Email",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider: "or connect with"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = NutritrackBorder
                    )
                    Text(
                        text = "  or connect with  ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextMuted
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = NutritrackBorder
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Google Sign-In Button
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        if (webClientId.isBlank()) {
                            authViewModel.setValidationError(
                                "Google Sign-In requires your Web Client ID from Firebase Console. In Firebase Console > Authentication > Google > Web SDK configuration, copy the 'Web client ID' and paste it into strings.xml."
                            )
                            return@Surface
                        }
                        try {
                            // Always sign out of GoogleSignInClient right before launching signInIntent.
                            // This clears any cached account session in Google Play Services, forcing
                            // the Google Account Chooser bottom sheet to appear with all accounts on the phone.
                            googleSignInClient.signOut().addOnCompleteListener {
                                try {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    authViewModel.setValidationError("Unable to open Google Sign-In: ${e.localizedMessage}")
                                }
                            }
                        } catch (e: Exception) {
                            try {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            } catch (launchEx: Exception) {
                                authViewModel.setValidationError("Unable to open Google Sign-In: ${launchEx.localizedMessage}")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = NutritrackSurface,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NutritrackBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = NutritrackSurface,
            title = {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            },
            text = {
                val cleanForgot = forgotEmailInput.trim()
                val isForgotValid = AuthViewModel.isValidEmail(cleanForgot)
                val forgotWarning = when {
                    forgotEmailTouched && cleanForgot.isEmpty() -> "Email address is required."
                    forgotEmailTouched && !isForgotValid -> "Please enter a valid email (e.g. name@example.com)."
                    else -> null
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = forgotEmailInput,
                        onValueChange = {
                            forgotEmailInput = it
                            forgotEmailTouched = true
                        },
                        placeholder = { Text("you@example.com", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Email,
                                contentDescription = null,
                                tint = if (forgotWarning != null) ErrorRed else BrandGreen
                            )
                        },
                        isError = forgotWarning != null,
                        supportingText = {
                            if (forgotWarning != null) {
                                Text(
                                    text = "⚠️ $forgotWarning",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors(isError = forgotWarning != null),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        forgotEmailTouched = true
                        val target = forgotEmailInput.trim()
                        if (target.isEmpty() || !AuthViewModel.isValidEmail(target)) {
                            return@Button
                        }
                        authViewModel.sendPasswordReset(target)
                        showForgotDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Authentic 4-color Google "G" Logo drawn vectorially in Jetpack Compose
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val stroke = sizePx * 0.22f
        val radius = (sizePx - stroke) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val arcRect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )

        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        // 1. Red Arc: ~200° to 340°
        drawArc(
            color = red,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // 2. Yellow Arc: ~135° to 210°
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 75f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // 3. Green Arc: ~35° to 145°
        drawArc(
            color = green,
            startAngle = 35f,
            sweepAngle = 110f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // 4. Blue Arc: ~ -20° to 40°
        drawArc(
            color = blue,
            startAngle = -20f,
            sweepAngle = 60f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // 5. Blue Horizontal Crossbar
        drawLine(
            color = blue,
            start = Offset(center.x - radius * 0.1f, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Reusable styling tokens for Authentication text fields.
 */
@Composable
fun authTextFieldColors(isError: Boolean = false): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = NutritrackBg,
        unfocusedContainerColor = NutritrackBg,
        focusedBorderColor = if (isError) ErrorRed else BrandGreen,
        unfocusedBorderColor = if (isError) ErrorRed.copy(alpha = 0.8f) else NutritrackBorder,
        focusedLabelColor = if (isError) ErrorRed else BrandGreen,
        unfocusedLabelColor = if (isError) ErrorRed else TextSecondary,
        cursorColor = if (isError) ErrorRed else BrandGreen,
        errorBorderColor = ErrorRed,
        errorCursorColor = ErrorRed,
        errorLeadingIconColor = ErrorRed,
        errorTrailingIconColor = ErrorRed,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )
}
