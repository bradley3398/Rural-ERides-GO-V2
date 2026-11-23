package com.example.core

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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.MasterSettings
import com.example.data.SettingsStore
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

private const val WORK_NAME = "volttrail_rider_reminders"
private const val CHANNEL_ID = "volttrail_reminders"

class ReminderWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val settings = SettingsStore(applicationContext).settingsFlow.first()
    if (!settings.remindersEnabled) return Result.success()

    if (settings.quietHoursEnabled && isInQuietHours(settings.quietStartHour, settings.quietEndHour)) {
      return Result.success()
    }

    ReminderScheduler.postNotification(
      context = applicationContext,
      title = "VoltTrail Pre-Ride Readiness Check",
      body =
        buildString {
          append("Check tire PSI (every ${settings.psiReminderDays}d) & torque bolts every ${settings.maintReminderKm} km.")
          if (settings.weatherAlertsEnabled) append(" Trail weather briefing ready in Cockpit.")
        },
    )
    return Result.success()
  }

  private fun isInQuietHours(startHour: Int, endHour: Int): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (startHour <= endHour) {
      hour in startHour until endHour
    } else {
      hour >= startHour || hour < endHour
    }
  }
}

object ReminderScheduler {

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "VoltTrail Rider Reminders",
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = "Scheduled tire PSI checks, maintenance intervals, and trail weather alerts"
        }
      manager.createNotificationChannel(channel)
    }
  }

  fun syncSchedule(context: Context, settings: MasterSettings) {
    ensureChannel(context)
    val wm = WorkManager.getInstance(context)
    if (!settings.remindersEnabled) {
      wm.cancelUniqueWork(WORK_NAME)
      return
    }
    val intervalHours = (settings.psiReminderDays.coerceIn(1, 14) * 12L).coerceAtLeast(12L)
    val request =
      PeriodicWorkRequestBuilder<ReminderWorker>(intervalHours, TimeUnit.HOURS)
        .addTag(WORK_NAME)
        .build()
    wm.enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun postNotification(context: Context, title: String, body: String): Boolean {
    ensureChannel(context)
    if (!hasNotificationPermission(context)) return false
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pending =
      PendingIntent.getActivity(
        context,
        104,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build()
    return runCatching {
      NotificationManagerCompat.from(context).notify(1041, notification)
      true
    }.getOrDefault(false)
  }
}
