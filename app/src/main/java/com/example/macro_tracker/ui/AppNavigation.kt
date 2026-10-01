package com.example.macro_tracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.macro_tracker.R
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.macro_tracker.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute : NavKey

@Serializable
object DashboardRoute : NavKey

@Serializable
object FoodRoute : NavKey

@Serializable
object InsightsRoute : NavKey

@Serializable
object GoalsRoute : NavKey

@Serializable
object DietPlansRoute : NavKey

@Serializable
object RecipesRoute : NavKey

@Serializable
object AuthRoute : NavKey

@Serializable
object BuyCoffeeRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    foodViewModel: FoodViewModel,
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel
) {
    val authUser by authViewModel.currentUser.collectAsState()
    val isAuthenticated = authUser != null && !authUser!!.isAnonymous
    val isInitiallyLoggedIn = remember {
        try {
            val u = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            u != null && !u.isAnonymous
        } catch (ignored: Exception) {
            false
        }
    }
    val backStack = rememberNavBackStack(if (isInitiallyLoggedIn) DashboardRoute else SplashRoute)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDrawerDatePicker by remember { mutableStateOf(false) }

    val userName by profileViewModel.userNameLiveData.observeAsState("")
    val currentRoute = backStack.lastOrNull()

    // Strict authentication gate: users must sign up or sign in before using the app
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            foodViewModel.clearSessionData()
            profileViewModel.clearSessionData()
            if (backStack.lastOrNull() != AuthRoute && backStack.lastOrNull() != SplashRoute) {
                backStack.clear()
                backStack.add(AuthRoute)
            }
        } else {
            foodViewModel.refreshData()
            if (backStack.lastOrNull() == AuthRoute || backStack.lastOrNull() == SplashRoute) {
                backStack.clear()
                backStack.add(DashboardRoute)
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            viewModel = profileViewModel,
            foodViewModel = foodViewModel,
            authViewModel = authViewModel,
            onNavigateToAuth = {
                backStack.add(AuthRoute)
            },
            onDismissRequest = { showProfileDialog = false }
        )
    }

    if (showDrawerDatePicker) {
        val selectedDate = foodViewModel.selectedDate.collectAsState().value
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDrawerDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            foodViewModel.setDate(picked)
                            backStack.clear()
                            backStack.add(DashboardRoute)
                        }
                        showDrawerDatePicker = false
                    }
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold, color = BrandGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawerDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isAuthenticated && currentRoute != AuthRoute && currentRoute != SplashRoute,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NutritrackSurface,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header Row: Brand + Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Nutritrack App Icon",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NUTRITRACK",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Surface(
                            onClick = { scope.launch { drawerState.close() } },
                            shape = CircleShape,
                            color = NutritrackBorderLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Menu",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Profile Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                scope.launch { drawerState.close() }
                                showProfileDialog = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AvatarPurple)
                        ) {
                            Text(
                                text = if (userName.isNotBlank()) userName.first().uppercase() else "U",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6C5CE7)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (userName.isNotBlank()) userName else "Set Your Name",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (authUser != null && !authUser!!.isAnonymous) authUser!!.email else "Personal nutrition",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (authUser != null && !authUser!!.isAnonymous) BrandGreen else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Menu items matching Image 1
                    DrawerMenuItem(
                        icon = Icons.Rounded.Home,
                        label = "Dashboard",
                        isSelected = currentRoute == DashboardRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(DashboardRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.Add,
                        label = "Food Log",
                        isSelected = currentRoute == FoodRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(FoodRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.RestaurantMenu,
                        label = "Diet Plans",
                        isSelected = currentRoute == DietPlansRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(DietPlansRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        label = "100+ Indian Recipes",
                        isSelected = currentRoute == RecipesRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(RecipesRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.PieChart,
                        label = "Insights",
                        isSelected = currentRoute == InsightsRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(InsightsRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.Adjust,
                        label = "Goals",
                        isSelected = currentRoute == GoalsRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(GoalsRoute)
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Calendar & History",
                        isSelected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showDrawerDatePicker = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.Settings,
                        label = "Settings",
                        isSelected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showProfileDialog = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Rounded.Coffee,
                        label = "Buy Dev a Coffee ☕",
                        isSelected = currentRoute == BuyCoffeeRoute,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.add(BuyCoffeeRoute)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Account & Authentication Action in Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch { drawerState.close() }
                                authViewModel.signOut(wipeLocalData = false) {
                                    backStack.clear()
                                    backStack.add(AuthRoute)
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Log Out",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) {
        val showBottomBar = isAuthenticated && currentRoute != AuthRoute && currentRoute != SplashRoute

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NutritrackSurface,
                        tonalElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            NutritrackBorder.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(66.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NutritrackNavItem(
                                selected = currentRoute == DashboardRoute,
                                onClick = {
                                    if (currentRoute != DashboardRoute) {
                                        backStack.clear()
                                        backStack.add(DashboardRoute)
                                    }
                                },
                                icon = Icons.Rounded.Home,
                                label = "Home",
                                modifier = Modifier.weight(1f)
                            )

                            NutritrackNavItem(
                                selected = currentRoute == FoodRoute,
                                onClick = {
                                    if (currentRoute != FoodRoute) {
                                        backStack.clear()
                                        backStack.add(FoodRoute)
                                    }
                                },
                                icon = Icons.Rounded.AddCircleOutline,
                                label = "Log",
                                modifier = Modifier.weight(1f)
                            )

                            NutritrackNavItem(
                                selected = currentRoute == InsightsRoute,
                                onClick = {
                                    if (currentRoute != InsightsRoute) {
                                        backStack.clear()
                                        backStack.add(InsightsRoute)
                                    }
                                },
                                icon = Icons.Rounded.PieChart,
                                label = "Insights",
                                modifier = Modifier.weight(1f)
                            )

                            NutritrackNavItem(
                                selected = currentRoute == GoalsRoute,
                                onClick = {
                                    if (currentRoute != GoalsRoute) {
                                        backStack.clear()
                                        backStack.add(GoalsRoute)
                                    }
                                },
                                icon = Icons.Rounded.Adjust,
                                label = "Goals",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
                entryProvider = entryProvider {
                    entry<DashboardRoute> {
                        DashboardScreen(
                            foodViewModel = foodViewModel,
                            profileViewModel = profileViewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateToLog = {
                                backStack.clear()
                                backStack.add(FoodRoute)
                            },
                            onNavigateToInsights = {
                                backStack.clear()
                                backStack.add(InsightsRoute)
                            },
                            onNavigateToDietPlans = {
                                backStack.clear()
                                backStack.add(DietPlansRoute)
                            }
                        )
                    }
                    entry<FoodRoute> {
                        FoodScreen(
                            foodViewModel = foodViewModel,
                            profileViewModel = profileViewModel,
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            }
                        )
                    }
                    entry<InsightsRoute> {
                        InsightsScreen(
                            foodViewModel = foodViewModel,
                            profileViewModel = profileViewModel,
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            }
                        )
                    }
                    entry<GoalsRoute> {
                        GoalsScreen(
                            foodViewModel = foodViewModel,
                            profileViewModel = profileViewModel,
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            },
                            onNavigateToBuyCoffee = {
                                backStack.add(BuyCoffeeRoute)
                            }
                        )
                    }

                    entry<BuyCoffeeRoute> {
                        BuyCoffeeScreen(
                            onNavigateBack = {
                                backStack.remove(BuyCoffeeRoute)
                            }
                        )
                    }

                    entry<DietPlansRoute> {
                        DietPlansScreen(
                            foodViewModel = foodViewModel,
                            profileViewModel = profileViewModel,
                            onNavigateBack = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            },
                            onNavigateToRecipes = {
                                backStack.add(RecipesRoute)
                            }
                        )
                    }

                    entry<RecipesRoute> {
                        RecipesScreen(
                            foodViewModel = foodViewModel,
                            onNavigateBack = {
                                backStack.remove(RecipesRoute)
                            }
                        )
                    }

                    entry<AuthRoute> {
                        AuthScreen(
                            authViewModel = authViewModel,
                            onNavigateToHome = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            }
                        )
                    }

                    entry<SplashRoute> {
                        SplashScreen(
                            authViewModel = authViewModel,
                            onNavigateToHome = {
                                backStack.clear()
                                backStack.add(DashboardRoute)
                            },
                            onNavigateToAuth = {
                                backStack.clear()
                                backStack.add(AuthRoute)
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) BrandGreenPill else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) BrandGreen else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isSelected) BrandGreenDark else TextPrimary
            )
        }
    }
}

@Composable
fun NutritrackNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) BrandGreen else TextMuted,
        animationSpec = tween(220),
        label = "navIconColor"
    )
    val animatedPillColor by animateColorAsState(
        targetValue = if (selected) BrandGreen.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(220),
        label = "navPillColor"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = BrandGreen.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(animatedPillColor)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = animatedIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    fontFamily = OutfitFontFamily
                ),
                color = animatedIconColor,
                maxLines = 1
            )
        }
    }
}
