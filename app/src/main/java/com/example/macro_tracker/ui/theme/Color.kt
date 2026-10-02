package com.example.macro_tracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nutritrack Brand Colors - Inspired by botanical nutrition & official App Icon
val BrandGreen = Color(0xFF059669)          // Rich Botanical Emerald (Vitality, fresh nutrition)
val BrandGreenAccent = Color(0xFF10B981)    // Fresh Mint Green (Active progress)
val BrandGreenDark = Color(0xFF064E3B)      // Deep Forest Pine (High-contrast text & accents)
val BrandGreenLight = Color(0xFFD1FAE5)     // Soft emerald track

// Energy & Calorie Balance (matches the golden solar arc in the official App Icon)
val EnergyAmber = Color(0xFFF59E0B)         // Solar Amber (Daily caloric energy & metabolism)
val EnergyAmberLight = Color(0xFFFEF3C7)    // Soft energy glow
val EnergyOrange = Color(0xFFF97316)        // Active burn accent

// Dynamic Surfaces & Obsidian Dark Palette (AMOLED pure black with subtle rich emerald undertones)
val NutritrackDark: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF18382B) else Color(0xFF0C241B)

val NutritrackBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF09120E) else Color(0xFFF8FAF8)

val NutritrackSurface: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF111E18) else Color(0xFFFFFFFF)

val NutritrackBorder: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF1B3127) else Color(0xFFE5E9E6)

val NutritrackBorderLight: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF162720) else Color(0xFFF0F4F1)

// Typography System
val TextPrimary: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFF2F6F3) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF475569)

val TextMuted: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF64748B) else Color(0xFF94A3B8)

// Macronutrient Science Palette
val ProteinColor = Color(0xFF7C3AED)       // Royal Violet (Muscle synthesis & cellular recovery)
val ProteinBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF231738) else Color(0xFFF5F3FF)

val CarbsColor = Color(0xFFF59E0B)         // Solar Amber (Clean energetic carbohydrate fuel)
val CarbsBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF2C220E) else Color(0xFFFEF3C7)

val FatColor = Color(0xFF0284C7)           // Ocean Cobalt (Essential healthy lipids & brain function)
val FatBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF0F2636) else Color(0xFFE0F2FE)

val WaterColor = Color(0xFF0EA5E9)         // Pure Cyan (Cellular hydration)
val WaterBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF0D2830) else Color(0xFFE0F7FA)

val FiberColor = Color(0xFF059669)         // Fresh Leaf Green (Digestive fiber & gut microbiome)
val FiberBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF132B20) else Color(0xFFECFDF5)

val MacroProtein = ProteinColor
val MacroCarbs = CarbsColor
val MacroFat = FatColor
val MacroFiber = FiberColor

// Dynamic Pills / Status / Actions
val BrandGreenPill: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF132B20) else Color(0xFFECFDF5)

val AvatarPurple: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF241838) else Color(0xFFEDE9FE)

val MealYellowBg: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF2C220E) else Color(0xFFFFFBEB)

val MealYellowIcon = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)