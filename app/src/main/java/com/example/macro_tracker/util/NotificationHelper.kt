package com.example.macro_tracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.macro_tracker.MainActivity
import com.example.macro_tracker.R

object NotificationHelper {

    const val CHANNEL_ID_WELCOME = "nutritrack_welcome_channel"
    const val CHANNEL_ID_MEALS = "nutritrack_meals_channel"
    const val CHANNEL_ID_WATER = "nutritrack_water_channel"
    const val CHANNEL_ID_STREAK = "nutritrack_streak_channel"

    const val NOTIF_ID_WELCOME = 1000
    const val NOTIF_ID_BREAKFAST = 1001
    const val NOTIF_ID_LUNCH = 1002
    const val NOTIF_ID_DINNER = 1003
    const val NOTIF_ID_WATER = 1004
    const val NOTIF_ID_STREAK = 1005

    private const val PREFS_NAME = "nutritrack_notif_prefs"
    private const val KEY_WELCOME_SENT = "has_sent_welcome_banner_v2"

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val welcomeChannel = NotificationChannel(
            CHANNEL_ID_WELCOME,
            "Welcome & Announcements",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Welcome alerts and essential setup announcements"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFF059669.toInt()
            setShowBadge(true)
        }

        val mealsChannel = NotificationChannel(
            CHANNEL_ID_MEALS,
            "Meal Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily breakfast, lunch, and dinner logging prompts"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFF10B981.toInt()
            setShowBadge(true)
        }

        val waterChannel = NotificationChannel(
            CHANNEL_ID_WATER,
            "Hydration Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Friendly hydration and water intake reminders"
            setShowBadge(false)
        }

        val streakChannel = NotificationChannel(
            CHANNEL_ID_STREAK,
            "Streak Saver Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts to help you keep your daily tracking streak alive"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFFF59E0B.toInt()
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(listOf(welcomeChannel, mealsChannel, waterChannel, streakChannel))
    }

    /**
     * Checks if the welcome notification has already been delivered; if not, delivers it immediately.
     */
    fun triggerWelcomeNotificationIfFirstTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadySent = prefs.getBoolean(KEY_WELCOME_SENT, false)
        if (!alreadySent) {
            val sent = showWelcomeNotification(context)
            if (sent) {
                prefs.edit().putBoolean(KEY_WELCOME_SENT, true).apply()
            }
        }
    }

    /**
     * Forces sending the animated welcome notification (useful for first install or testing).
     */
    fun showWelcomeNotification(context: Context): Boolean {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_action", "welcome")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_WELCOME,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsedView = RemoteViews(context.packageName, R.layout.notification_banner_collapsed).apply {
            setTextViewText(R.id.notif_collapsed_badge, "WELCOME")
            setTextViewText(R.id.notif_collapsed_title, "Welcome to NutriTrack! 🥗")
            setTextViewText(R.id.notif_collapsed_message, "Your smart macro & calorie planner is ready.")
        }

        val expandedView = RemoteViews(context.packageName, R.layout.notification_banner_expanded).apply {
            setTextViewText(R.id.notif_expanded_badge, "✨ Smart Companion")
            setTextViewText(R.id.notif_expanded_title, "Welcome to NutriTrack! 🥗")
            setTextViewText(R.id.notif_expanded_message, "Achieve your fitness goals with smart macro counting & curated recipes.")
            setTextViewText(R.id.notif_btn_action, "Open NutriTrack ➔")
            setOnClickPendingIntent(R.id.notif_btn_action, pendingIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_WELCOME)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setColor(0xFF10B981.toInt())
            .setContentTitle("Welcome to NutriTrack! 🥗")
            .setContentText("Your intelligent macro tracker & calorie counter is ready. Tap to get started!")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIF_ID_WELCOME, builder.build())
        return true
    }

    /**
     * Shows a scheduled or on-demand reminder notification with animated banner UI.
     */
    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_MEALS
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val badgeText = when (channelId) {
            CHANNEL_ID_WATER -> "HYDRATE 💧"
            CHANNEL_ID_STREAK -> "STREAK 🔥"
            else -> "REMINDER 🥗"
        }

        val actionBtnText = when (channelId) {
            CHANNEL_ID_WATER -> "Log Water ➔"
            CHANNEL_ID_STREAK -> "Save Streak ➔"
            else -> "Log Meal ➔"
        }

        val collapsedView = RemoteViews(context.packageName, R.layout.notification_banner_collapsed).apply {
            setTextViewText(R.id.notif_collapsed_badge, badgeText)
            setTextViewText(R.id.notif_collapsed_title, title)
            setTextViewText(R.id.notif_collapsed_message, message)
        }

        val expandedView = RemoteViews(context.packageName, R.layout.notification_banner_expanded).apply {
            setTextViewText(R.id.notif_expanded_badge, badgeText)
            setTextViewText(R.id.notif_expanded_title, title)
            setTextViewText(R.id.notif_expanded_message, message)
            setTextViewText(R.id.notif_btn_action, actionBtnText)
            setOnClickPendingIntent(R.id.notif_btn_action, pendingIntent)
        }

        val isHighPriority = channelId == CHANNEL_ID_STREAK || channelId == CHANNEL_ID_MEALS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setColor(0xFF10B981.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(if (isHighPriority) longArrayOf(0, 250, 150, 250) else null)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

