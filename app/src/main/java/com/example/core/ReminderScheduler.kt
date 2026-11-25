package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first

private const val TAG = "ReminderScheduler"
private const val WORK_NAME = "volttrail_rider_reminders"

class ReminderWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val settings = SettingsStore(applicationContext).settingsFlow.first()
    if (!settings.remindersEnabled) return Result.success()

    if (settings.quietHoursEnabled && ReminderScheduler.isInQuietHours(settings.quietStartHour, settings.quietEndHour)) {
      return Result.success()
    }

    ReminderScheduler.postNotification(
      context = applicationContext,
      title = "Rural ERides GO Pre-Ride Check",
      body =
        buildString {
          append("Check tire PSI (every ${settings.psiReminderDays}d) & torque bolts every ${settings.maintReminderKm} km.")
          if (settings.weatherAlertsEnabled) append(" Trail weather briefing ready in Cockpit.")
        },
    )
    return Result.success()
  }
}

object ReminderScheduler {

  const val CHANNEL_REMINDERS = "volttrail_reminders"
  const val CHANNEL_AI = "volttrail_ai_insights"
  const val CHANNEL_SOCIAL = "volttrail_social"
  const val CHANNEL_DM = "volttrail_direct_messages"
  const val CHANNEL_UPDATES = "volttrail_app_updates"

  const val EXTRA_TARGET_TAB = "extra_target_tab"
  const val EXTRA_OPEN_DM = "extra_open_dm"
  const val EXTRA_DM_THREAD_ID = "extra_dm_thread_id"
  const val EXTRA_OPEN_UPDATES = "extra_open_updates"

  private const val NOTIF_ID_REMINDER = 1041
  private const val NOTIF_ID_APP_UPDATE = 1050
  private const val NOTIF_ID_AI_BASE = 2000
  private const val NOTIF_ID_SOCIAL_BASE = 3000
  private const val NOTIF_ID_DM_BASE = 4000

  private val aiCounter = AtomicInteger(0)
  private val socialCounter = AtomicInteger(0)

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val remindersChannel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Rural ERides GO Rider Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Scheduled tire PSI checks, maintenance intervals, and trail safety"
        enableLights(true)
        lightColor = Color.YELLOW
      }

      val aiChannel = NotificationChannel(
        CHANNEL_AI,
        "Rural ERides GO AI & Diagnostics",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "AI Co-Pilot tactical advice, AI mechanic diagnoses, and ride debriefs"
        enableLights(true)
        lightColor = Color.CYAN
        enableVibration(true)
      }

      val socialChannel = NotificationChannel(
        CHANNEL_SOCIAL,
        "Rural ERides GO Community & Social",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "New board dispatches, community discussions, and trail condition reports"
        enableLights(true)
        lightColor = Color.GREEN
      }

      val dmChannel = NotificationChannel(
        CHANNEL_DM,
        "Rural ERides GO Direct Messages & Squad Comms",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Direct private messages and squad communication alerts"
        enableLights(true)
        lightColor = Color.BLUE
        enableVibration(true)
      }

      val updatesChannel = NotificationChannel(
        CHANNEL_UPDATES,
        "Rural ERides GO App Updates",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Alerts when a new Rural ERides GO build or release is published"
        enableLights(true)
        lightColor = Color.GREEN
        enableVibration(true)
      }

      manager.createNotificationChannels(
        listOf(remindersChannel, aiChannel, socialChannel, dmChannel, updatesChannel),
      )
    }
  }

  fun ensureChannel(context: Context) = ensureChannels(context)

  fun isInQuietHours(startHour: Int, endHour: Int): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (startHour <= endHour) {
      hour in startHour until endHour
    } else {
      hour >= startHour || hour < endHour
    }
  }

  fun syncSchedule(context: Context, settings: MasterSettings) {
    ensureChannels(context)
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

  /**
   * Posts an AI Intelligence notification (e.g. Co-Pilot advice, mechanic diagnosis, or ride debrief).
   */
  fun postAiNotification(
    context: Context,
    title: String,
    body: String,
    targetTab: String = "COPILOT",
  ): Boolean {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return false

    val notifId = NOTIF_ID_AI_BASE + (aiCounter.getAndIncrement() % 20)
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_TAB, targetTab)
    }
    val pending = PendingIntent.getActivity(
      context,
      notifId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_AI)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
      .setContentIntent(pending)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(notifId, notification)
      Log.d(TAG, "Posted AI notification: $title")
      true
    }.getOrDefault(false)
  }

  /**
   * Posts a Social / Community Board notification (new dispatch, comment, or trail report).
   */
  fun postSocialNotification(
    context: Context,
    title: String,
    body: String,
    targetTab: String = "BOARD",
  ): Boolean {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return false

    val notifId = NOTIF_ID_SOCIAL_BASE + (socialCounter.getAndIncrement() % 20)
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_TAB, targetTab)
    }
    val pending = PendingIntent.getActivity(
      context,
      notifId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setCategory(NotificationCompat.CATEGORY_SOCIAL)
      .setContentIntent(pending)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(notifId, notification)
      Log.d(TAG, "Posted Social notification: $title")
      true
    }.getOrDefault(false)
  }

  /**
   * Posts a Direct Message (DM) notification from another rider.
   */
  fun postDmNotification(
    context: Context,
    senderCallsign: String,
    body: String,
    threadId: String = "",
  ): Boolean {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return false

    val notifId = if (threadId.isNotBlank()) {
      NOTIF_ID_DM_BASE + (threadId.hashCode() and 0x3FF)
    } else {
      NOTIF_ID_DM_BASE + (senderCallsign.hashCode() and 0x3FF)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_TAB, "BOARD")
      putExtra(EXTRA_OPEN_DM, true)
      if (threadId.isNotBlank()) putExtra(EXTRA_DM_THREAD_ID, threadId)
    }
    val pending = PendingIntent.getActivity(
      context,
      notifId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_DM)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Direct Message: $senderCallsign")
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .setContentIntent(pending)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(notifId, notification)
      Log.d(TAG, "Posted DM notification from $senderCallsign")
      true
    }.getOrDefault(false)
  }

  /**
   * Posts a general rider reminder (maintenance, tire PSI, or weather).
   */
  fun postNotification(context: Context, title: String, body: String): Boolean {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return false

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_TAB, "COCKPIT")
    }
    val pending = PendingIntent.getActivity(
      context,
      NOTIF_ID_REMINDER,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setContentIntent(pending)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(NOTIF_ID_REMINDER, notification)
      Log.d(TAG, "Posted reminder notification: $title")
      true
    }.getOrDefault(false)
  }

  /**
   * Posts a high-visibility notification when a new Rural ERides GO build is published.
   * Tapping opens the GitHub repository / release page directly in the system browser
   * (zero restricted permissions, 100% Google Play Creator Program compliant).
   */
  fun postAppUpdateNotification(
    context: Context,
    versionLabel: String,
    releaseTitle: String,
    releaseNotes: String,
    releaseTag: String = "",
    releasePageUrl: String = GITHUB_RELEASES_WEB_URL,
  ): Boolean {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return false

    val safeRepoUrl =
      releasePageUrl.takeIf { it.startsWith("https://github.com/bradley3398/Rural-Erides-GO") }
        ?: GITHUB_RELEASES_WEB_URL

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeRepoUrl)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pending = PendingIntent.getActivity(
      context,
      NOTIF_ID_APP_UPDATE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val title = "New Build Published: v$versionLabel"
    val summary =
      releaseNotes.lineSequence().firstOrNull { it.isNotBlank() }?.take(140)
        ?: "$releaseTitle is ready. Tap to open the GitHub repo page."
    val expanded =
      buildString {
        append(releaseTitle.ifBlank { "Rural ERides GO v$versionLabel" })
        if (releaseTag.isNotBlank()) append(" ($releaseTag)")
        append("\n")
        append(releaseNotes.take(400).ifBlank { "Tap to open the GitHub repository release page." })
      }

    val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(summary)
      .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setContentIntent(pending)
      .setAutoCancel(true)
      .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(NOTIF_ID_APP_UPDATE, notification)
      Log.d(TAG, "Posted App Update notification for $versionLabel -> $safeRepoUrl")
      true
    }.getOrDefault(false)
  }
}
