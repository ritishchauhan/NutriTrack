package com.example.macro_tracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.macro_tracker.data.local.FoodLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NutritionReportExporter {

    fun exportCsvReport(
        context: Context,
        userName: String,
        foodLogs: List<FoodLogEntity>,
        rangeDays: Int
    ): Intent? {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val csvFile = File(reportsDir, "nutritrack_nutrition_report.csv")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateGenerated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val sb = StringBuilder()
            sb.appendLine("NUTRITRACK NUTRITION & MACRONUTRIENT REPORT")
            sb.appendLine("User,${escapeCsv(if (userName.isBlank()) "Nutritrack User" else userName)}")
            sb.appendLine("Time Period,Last $rangeDays Days")
            sb.appendLine("Report Generated,$dateGenerated")
            sb.appendLine()
            sb.appendLine("Date,Meal,Food Item,Calories (kcal),Protein (g),Carbohydrates (g),Fat (g),Fiber (g),Servings")

            val sortedLogs = foodLogs.sortedByDescending { it.timestamp }
            for (log in sortedLogs) {
                val itemDate = dateFormat.format(Date(log.timestamp))
                sb.append(escapeCsv(itemDate)).append(",")
                sb.append(escapeCsv(log.mealType)).append(",")
                sb.append(escapeCsv(log.foodName)).append(",")
                sb.append(log.calories).append(",")
                sb.append(String.format(Locale.US, "%.1f", log.protein)).append(",")
                sb.append(String.format(Locale.US, "%.1f", log.carbs)).append(",")
                sb.append(String.format(Locale.US, "%.1f", log.fat)).append(",")
                sb.append(String.format(Locale.US, "%.1f", log.fiber)).append(",")
                sb.append(log.servings)
                sb.appendLine()
            }

            FileOutputStream(csvFile).use { it.write(sb.toString().toByteArray()) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            return Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Nutritrack Nutrition Report - $userName")
                putExtra(Intent.EXTRA_TEXT, "Attached is the Nutritrack nutrition and macronutrient report for the last $rangeDays days.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportPdfReport(
        context: Context,
        userName: String,
        foodLogs: List<FoodLogEntity>,
        summary: WeightTrendSummary,
        rangeDays: Int
    ): Intent? {
        val pdfDocument = PdfDocument()
        try {
            // A4 page: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val nowStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

            // 1. Header Banner (Brand Emerald: #059669)
            paint.color = Color.parseColor("#059669")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("NUTRITRACK HEALTH & NUTRITION REPORT", 30f, 44f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Personalized Macronutrient, Calorie & Weight Trend Summary", 30f, 65f, paint)

            // 2. Metadata Box
            paint.color = Color.parseColor("#F1F5F2")
            val metaRect = RectF(30f, 105f, 565f, 155f)
            canvas.drawRoundRect(metaRect, 8f, 8f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            val displayName = if (userName.isBlank()) "Nutritrack User" else userName
            canvas.drawText("Client: $displayName", 45f, 126f, paint)
            canvas.drawText("Period: Last $rangeDays Days", 45f, 143f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Generated: $nowStr", 360f, 126f, paint)
            canvas.drawText("Status: Certified Private & Offline-First", 360f, 143f, paint)

            // 3. Nutrition & Weight Trend Summary Cards
            val totalCals = foodLogs.sumOf { it.calories }
            val avgCals = if (foodLogs.isNotEmpty()) totalCals / rangeDays.coerceAtLeast(1) else 0
            val totalProtein = foodLogs.sumOf { it.protein.toDouble() }.toFloat()
            val avgProtein = if (foodLogs.isNotEmpty()) totalProtein / rangeDays.coerceAtLeast(1) else 0f
            val totalCarbs = foodLogs.sumOf { it.carbs.toDouble() }.toFloat()
            val avgCarbs = if (foodLogs.isNotEmpty()) totalCarbs / rangeDays.coerceAtLeast(1) else 0f
            val totalFat = foodLogs.sumOf { it.fat.toDouble() }.toFloat()
            val avgFat = if (foodLogs.isNotEmpty()) totalFat / rangeDays.coerceAtLeast(1) else 0f

            paint.color = Color.parseColor("#0C241B")
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("Key Metric Averages (Daily)", 30f, 185f, paint)

            // 4 Mini Metric Badges
            val badgeWidth = 125f
            val badgeHeight = 55f
            val badges = listOf(
                "Daily Calories" to "$avgCals kcal",
                "Daily Protein" to "${avgProtein.toInt()} g",
                "Daily Carbs" to "${avgCarbs.toInt()} g",
                "Daily Fat" to "${avgFat.toInt()} g"
            )

            badges.forEachIndexed { index, (label, value) ->
                val startX = 30f + (index * (badgeWidth + 8f))
                paint.color = Color.parseColor("#F8FAF8")
                canvas.drawRoundRect(RectF(startX, 195f, startX + badgeWidth, 195f + badgeHeight), 6f, 6f, paint)

                paint.color = Color.parseColor("#475569")
                paint.textSize = 9f
                paint.isFakeBoldText = false
                canvas.drawText(label, startX + 10f, 215f, paint)

                paint.color = Color.parseColor("#059669")
                paint.textSize = 14f
                paint.isFakeBoldText = true
                canvas.drawText(value, startX + 10f, 238f, paint)
            }

            // Weight Trend Section
            if (summary.trendPoints.isNotEmpty()) {
                paint.color = Color.parseColor("#0C241B")
                paint.textSize = 13f
                paint.isFakeBoldText = true
                canvas.drawText("Weight & Body Composition Trend (EMA Smoothed)", 30f, 280f, paint)

                paint.color = Color.parseColor("#F1F5F2")
                val weightRect = RectF(30f, 290f, 565f, 335f)
                canvas.drawRoundRect(weightRect, 6f, 6f, paint)

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Trend Weight: ${summary.currentTrendKg} kg", 45f, 310f, paint)
                canvas.drawText("Scale Weight: ${summary.currentActualKg} kg", 45f, 325f, paint)

                val rateStr = "${if (summary.weeklyRateKg > 0) "+" else ""}${summary.weeklyRateKg} kg/week"
                canvas.drawText("Weekly Rate: $rateStr", 240f, 310f, paint)
                val totalStr = "${if (summary.totalChangeKg > 0) "+" else ""}${summary.totalChangeKg} kg total"
                canvas.drawText("Net Change: $totalStr", 240f, 325f, paint)
            }

            // 4. Detailed Meal Log Table
            val tableTop = if (summary.trendPoints.isNotEmpty()) 360f else 280f
            paint.color = Color.parseColor("#0C241B")
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("Recent Logged Items & Meals", 30f, tableTop, paint)

            // Table Header Bar
            paint.color = Color.parseColor("#ECFDF5")
            canvas.drawRect(30f, tableTop + 10f, 565f, tableTop + 32f, paint)

            paint.color = Color.parseColor("#064E3B")
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("Date", 38f, tableTop + 24f, paint)
            canvas.drawText("Meal", 110f, tableTop + 24f, paint)
            canvas.drawText("Food Description", 185f, tableTop + 24f, paint)
            canvas.drawText("Calories", 400f, tableTop + 24f, paint)
            canvas.drawText("Protein", 455f, tableTop + 24f, paint)
            canvas.drawText("Carbs", 500f, tableTop + 24f, paint)
            canvas.drawText("Fat", 540f, tableTop + 24f, paint)

            // Table Rows
            val itemDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            var currentY = tableTop + 48f
            paint.isFakeBoldText = false

            val previewLogs = foodLogs.take(18)
            for ((index, log) in previewLogs.withIndex()) {
                if (currentY > 780f) break

                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAF8")
                    canvas.drawRect(30f, currentY - 12f, 565f, currentY + 6f, paint)
                }

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 9f
                canvas.drawText(itemDateFormat.format(Date(log.timestamp)), 38f, currentY, paint)
                canvas.drawText(log.mealType, 110f, currentY, paint)

                val truncatedName = if (log.foodName.length > 30) log.foodName.take(28) + "…" else log.foodName
                canvas.drawText(truncatedName, 185f, currentY, paint)
                canvas.drawText("${log.calories}", 400f, currentY, paint)
                canvas.drawText("${log.protein.toInt()}g", 455f, currentY, paint)
                canvas.drawText("${log.carbs.toInt()}g", 500f, currentY, paint)
                canvas.drawText("${log.fat.toInt()}g", 540f, currentY, paint)

                currentY += 20f
            }

            // Footer
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 8f
            canvas.drawText(
                "Nutritrack Health Technologies • Confidential Patient Report • Generated on-device with zero tracking",
                30f,
                820f,
                paint
            )

            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val pdfFile = File(reportsDir, "nutritrack_nutrition_report.pdf")
            FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            return Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Nutritrack Clinical Nutrition Report - $userName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the official Nutritrack Clinical Nutrition & Macro Report for $displayName covering the last $rangeDays days."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    fun shareReport(context: Context, intent: Intent) {
        val chooser = Intent.createChooser(intent, "Share Nutrition Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
