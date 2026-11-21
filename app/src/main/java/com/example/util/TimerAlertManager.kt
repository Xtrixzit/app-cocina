package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.settings.AppSettingsPreferences
import com.example.settings.PersistedTimerState

object TimerAlertManager {
    const val CHANNEL_ID = "recetario_timer_alerts"
    private const val NOTIFICATION_ID = 7001
    private const val ALARM_REQUEST_CODE = 7002
    const val EXTRA_TIMER_LABEL = "extra_timer_label"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localized = LocaleHelper.localizedContext(context)
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localized.getString(R.string.timer_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(soundUri, audioAttr)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun startOrResumeTimer(context: Context, durationSeconds: Int, remainingSeconds: Int, label: String) {
        val safeDuration = durationSeconds.coerceAtLeast(1)
        val safeRemaining = remainingSeconds.coerceIn(1, safeDuration)
        val endTime = System.currentTimeMillis() + (safeRemaining * 1000L)

        ensureNotificationChannel(context)
        AppSettingsPreferences.saveTimerState(
            context,
            PersistedTimerState(
                endTimeMillis = endTime,
                durationSeconds = safeDuration,
                pausedRemainingSeconds = safeRemaining,
                label = label,
                isRunning = true
            )
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context, label)
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun pauseTimer(context: Context, remainingSeconds: Int) {
        val current = AppSettingsPreferences.getTimerState(context)
        cancelAlarmOnly(context)
        AppSettingsPreferences.saveTimerState(
            context,
            current.copy(
                endTimeMillis = 0L,
                pausedRemainingSeconds = remainingSeconds.coerceAtLeast(0),
                isRunning = false
            )
        )
    }

    fun cancelTimer(context: Context) {
        cancelAlarmOnly(context)
        AppSettingsPreferences.clearTimerState(context)
        try {
            NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    private fun cancelAlarmOnly(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context, "")
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    private fun buildAlarmPendingIntent(context: Context, label: String): PendingIntent {
        val intent = Intent(context.applicationContext, TimerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TIMER_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
            }
        } catch (_: Exception) {
        }
    }

    fun postFinishedNotification(context: Context, timerLabel: String) {
        ensureNotificationChannel(context)
        val localized = LocaleHelper.localizedContext(context)
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!canNotify) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppShortcutsUpdater.EXTRA_OPEN_TAB, 4)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeLabel = timerLabel.ifBlank { localized.getString(R.string.timer_default_title) }
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localized.getString(R.string.timer_notification_title))
            .setContentText(localized.getString(R.string.timer_notification_body, safeLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val current = AppSettingsPreferences.getTimerState(context)
        val label = intent.getStringExtra(TimerAlertManager.EXTRA_TIMER_LABEL)
            ?.takeIf { it.isNotBlank() }
            ?: current.label

        AppSettingsPreferences.saveTimerState(
            context,
            current.copy(isRunning = false, pausedRemainingSeconds = 0, endTimeMillis = 0L)
        )
        TimerAlertManager.triggerVibration(context)
        TimerAlertManager.postFinishedNotification(context, label)
    }
}
