package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.settings.AppSettingsPreferences

/**
 * Posts a one-time-per-version Android notification whenever a newer release is published,
 * allowing users to tap straight into the in-app "Actualizar ahora" prompt.
 */
object UpdateNotificationHelper {
    const val CHANNEL_ID = "recetario_updates"
    private const val NOTIFICATION_ID = 7101
    const val EXTRA_OPEN_UPDATE_PROMPT = "extra_open_update_prompt"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localized = LocaleHelper.localizedContext(context)
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localized.getString(R.string.update_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyIfNewVersion(
        context: Context,
        remoteVersionCode: Int,
        remoteVersionName: String
    ) {
        if (remoteVersionCode <= BuildConfig.VERSION_CODE) return
        if (AppSettingsPreferences.getLastNotifiedUpdateVersion(context) >= remoteVersionCode) return

        AppSettingsPreferences.setLastNotifiedUpdateVersion(context, remoteVersionCode)
        ensureNotificationChannel(context)

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!canNotify) return

        val localized = LocaleHelper.localizedContext(context)
        val openAppIntent = Intent(context.applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_UPDATE_PROMPT, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val versionLabel = remoteVersionName.ifBlank { remoteVersionCode.toString() }
        val notification = NotificationCompat.Builder(context.applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localized.getString(R.string.update_notif_title, versionLabel))
            .setContentText(localized.getString(R.string.update_notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context.applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
