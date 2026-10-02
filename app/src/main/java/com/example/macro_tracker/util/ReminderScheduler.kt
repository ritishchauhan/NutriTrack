package com.example.macro_tracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.macro_tracker.receiver.DailyReminderReceiver
import java.util.Calendar

object ReminderScheduler {

    private const val REQUEST_CODE_BREAKFAST = 2001
    private const val REQUEST_CODE_LUNCH = 2002
    private const val REQUEST_CODE_DINNER = 2003
    private const val REQUEST_CODE_WATER_1 = 2004
    private const val REQUEST_CODE_WATER_2 = 2005
    private const val REQUEST_CODE_STREAK = 2006

    fun scheduleAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Breakfast at 09:00 AM
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_BREAKFAST,
            action = DailyReminderReceiver.ACTION_BREAKFAST,
            hour = 9,
            minute = 0
        )

        // 2. Hydration 1 at 11:30 AM
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_WATER_1,
            action = DailyReminderReceiver.ACTION_WATER,
            hour = 11,
            minute = 30
        )

        // 3. Lunch at 01:30 PM (13:30)
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_LUNCH,
            action = DailyReminderReceiver.ACTION_LUNCH,
            hour = 13,
            minute = 30
        )

        // 4. Hydration 2 at 04:30 PM (16:30)
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_WATER_2,
            action = DailyReminderReceiver.ACTION_WATER,
            hour = 16,
            minute = 30
        )

        // 5. Dinner at 08:30 PM (20:30)
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_DINNER,
            action = DailyReminderReceiver.ACTION_DINNER,
            hour = 20,
            minute = 30
        )

        // 6. Streak Saver at 09:45 PM (21:45)
        scheduleDailyAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = REQUEST_CODE_STREAK,
            action = DailyReminderReceiver.ACTION_STREAK,
            hour = 21,
            minute = 45
        )
    }

    private fun scheduleDailyAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (ignored: Exception) {}
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val actionsWithCodes = listOf(
            DailyReminderReceiver.ACTION_BREAKFAST to REQUEST_CODE_BREAKFAST,
            DailyReminderReceiver.ACTION_LUNCH to REQUEST_CODE_LUNCH,
            DailyReminderReceiver.ACTION_DINNER to REQUEST_CODE_DINNER,
            DailyReminderReceiver.ACTION_WATER to REQUEST_CODE_WATER_1,
            DailyReminderReceiver.ACTION_WATER to REQUEST_CODE_WATER_2,
            DailyReminderReceiver.ACTION_STREAK to REQUEST_CODE_STREAK
        )

        for ((action, code) in actionsWithCodes) {
            val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
