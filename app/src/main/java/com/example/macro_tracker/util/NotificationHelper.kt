package com.example.macro_tracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.macro_tracker.MainActivity
import com.example.macro_tracker.R

object NotificationHelper {

    const val CHANNEL_ID_MEALS = "nutritrack_meals_channel"
    const val CHANNEL_ID_WATER = "nutritrack_water_channel"
    const val CHANNEL_ID_STREAK = "nutritrack_streak_channel"

    const val NOTIF_ID_BREAKFAST = 1001
    const val NOTIF_ID_LUNCH = 1002
    const val NOTIF_ID_DINNER = 1003
    const val NOTIF_ID_WATER = 1004
    const val NOTIF_ID_STREAK = 1005

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mealsChannel = NotificationChannel(
                CHANNEL_ID_MEALS,
                "Meal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily breakfast, lunch, and dinner logging prompts"
                enableVibration(true)
            }

            val waterChannel = NotificationChannel(
                CHANNEL_ID_WATER,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Friendly hydration and water intake reminders"
            }

            val streakChannel = NotificationChannel(
                CHANNEL_ID_STREAK,
                "Streak Saver Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts to help you keep your daily tracking streak alive"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(mealsChannel, waterChannel, streakChannel))
        }
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_MEALS
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (channelId == CHANNEL_ID_STREAK) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
