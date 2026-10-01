package com.example.macro_tracker.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.R
import com.example.macro_tracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Loading splash screen that displays only while the background application
 * is checking whether the user is logged in or not. As soon as the auth check
 * resolves, it immediately routes to Dashboard (if logged in) or Auth (if not).
 */
@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    // Smooth pulse animation for the app logo
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLogoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Run loading screen strictly while the background application checks authentication status
    LaunchedEffect(Unit) {
        // 1. Direct check: If Firebase already has an authenticated user cached, navigate home immediately
        val directUser = try {
            val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (fbUser != null && !fbUser.isAnonymous) fbUser else null
        } catch (ignored: Exception) {
            null
        }

        if (directUser != null) {
            onNavigateToHome()
            return@LaunchedEffect
        }

        // 2. Wait while background auth initialization resolves
        withTimeoutOrNull(2000L) {
            authViewModel.isAuthReady.first { it }
        }

        val user = authViewModel.currentUser.value ?: try {
            val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (fbUser != null && !fbUser.isAnonymous) fbUser else null
        } catch (ignored: Exception) {
            null
        }

        val isUserLoggedIn = when (user) {
            is com.example.macro_tracker.data.repository.AuthUser -> !user.isAnonymous
            is com.google.firebase.auth.FirebaseUser -> !user.isAnonymous
            else -> false
        }

        if (isUserLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NutritrackBg,
                        Color(0xFF131A24),
                        NutritrackBg
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Glowing circular container with app logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(136.dp)
                    .scale(pulseScale)
            ) {
                // Ambient glow circle
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .background(
                            BrandGreen.copy(alpha = glowAlpha * 0.30f),
                            shape = CircleShape
                        )
                )

                // Logo container
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(NutritrackSurface)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Nutritrack Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name
            Text(
                text = "Nutritrack",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Smart Calorie & Macro Tracker",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = BrandGreen,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Modern circular loading indicator
            CircularProgressIndicator(
                color = BrandGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Checking session...",
                fontFamily = OutfitFontFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
