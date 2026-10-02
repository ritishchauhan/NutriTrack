package com.example.macro_tracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.macro_tracker.util.NotificationHelper
import com.example.macro_tracker.util.ReminderScheduler

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BREAKFAST = "com.example.macro_tracker.ACTION_BREAKFAST"
        const val ACTION_LUNCH = "com.example.macro_tracker.ACTION_LUNCH"
        const val ACTION_DINNER = "com.example.macro_tracker.ACTION_DINNER"
        const val ACTION_WATER = "com.example.macro_tracker.ACTION_WATER"
        const val ACTION_STREAK = "com.example.macro_tracker.ACTION_STREAK"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ReminderScheduler.scheduleAllReminders(context)
            }
            ACTION_BREAKFAST -> {
                NotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.NOTIF_ID_BREAKFAST,
                    title = "☀️ Time for Breakfast!",
                    message = "Kickstart your metabolism. Log your morning meal to start the day strong!",
                    channelId = NotificationHelper.CHANNEL_ID_MEALS
                )
            }
            ACTION_LUNCH -> {
                NotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.NOTIF_ID_LUNCH,
                    title = "🥗 Midday Energy Boost!",
                    message = "Don't forget to track your lunch to keep your macros balanced.",
                    channelId = NotificationHelper.CHANNEL_ID_MEALS
                )
            }
            ACTION_DINNER -> {
                NotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.NOTIF_ID_DINNER,
                    title = "🍲 Evening Dinner Time",
                    message = "Track what you're eating for dinner to complete your daily nutrition goals.",
                    channelId = NotificationHelper.CHANNEL_ID_MEALS
                )
            }
            ACTION_WATER -> {
                NotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.NOTIF_ID_WATER,
                    title = "💧 Hydration Check",
                    message = "Time for a glass of water! Stay hydrated to boost energy and recovery.",
                    channelId = NotificationHelper.CHANNEL_ID_WATER
                )
            }
            ACTION_STREAK -> {
                NotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.NOTIF_ID_STREAK,
                    title = "🔥 Protect Your Streak!",
                    message = "Log your meals before midnight to keep your active Nutritrack streak going!",
                    channelId = NotificationHelper.CHANNEL_ID_STREAK
                )
            }
        }
    }
}
