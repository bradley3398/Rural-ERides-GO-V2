package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.MasterSettings
import com.example.data.SettingsStore
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val UPDATE_TAG = "AppUpdateChecker"
private const val UPDATE_WORK_NAME = "rural_erides_app_update_check"

const val GITHUB_RELEASES_API_LATEST =
  "https://api.github.com/repos/bradley3398/Rural-Erides-GO/releases/latest"
const val GITHUB_RELEASES_API_ALL =
  "https://api.github.com/repos/bradley3398/Rural-Erides-GO/releases?per_page=10"
const val GITHUB_REPO_URL =
  "https://github.com/bradley3398/Rural-Erides-GO"
const val GITHUB_RELEASES_WEB_URL =
  "https://github.com/bradley3398/Rural-Erides-GO/releases"

data class AppReleaseInfo(
  val tagName: String,
  val versionNumber: Int,
  val versionLabel: String,
  val releaseTitle: String,
  val releaseNotes: String,
  val publishedAt: String,
  val releasePageUrl: String,
  val apkAssetUrl: String,
  val apkAssetName: String,
  val apkSizeBytes: Long,
  val isPrerelease: Boolean,
  val isNewerThanCurrent: Boolean,
)

sealed interface AppUpdateCheckState {
  data object Idle : AppUpdateCheckState

  data object Checking : AppUpdateCheckState

  data class UpToDate(
    val latestRelease: AppReleaseInfo?,
    val checkedAtMs: Long,
  ) : AppUpdateCheckState

  data class UpdateAvailable(
    val release: AppReleaseInfo,
    val checkedAtMs: Long,
  ) : AppUpdateCheckState

  data class Error(
    val message: String,
    val checkedAtMs: Long,
  ) : AppUpdateCheckState
}

/**
 * Periodic background worker that checks GitHub Releases for newly published builds
 * and dispatches a native Android notification when a newer build is available.
 */
class AppUpdateCheckWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val store = SettingsStore(applicationContext)
    val settings = store.settingsFlow.first()
    if (!settings.autoCheckAppUpdates) return Result.success()

    if (settings.quietHoursEnabled &&
      ReminderScheduler.isInQuietHours(settings.quietStartHour, settings.quietEndHour)
    ) {
      return Result.success()
    }

    val result = AppUpdateChecker.fetchLatestPublishedRelease(settings.includePrereleaseBuilds)
    val now = System.currentTimeMillis()
    store.update { it.copy(lastUpdateCheckMs = now) }

    val release = result.getOrNull() ?: return Result.success()
    if (
      release.isNewerThanCurrent &&
      settings.appUpdateNotifsEnabled &&
      release.tagName != settings.lastNotifiedUpdateTag &&
      release.tagName != settings.skippedUpdateTag
    ) {
      val posted =
        ReminderScheduler.postAppUpdateNotification(
          context = applicationContext,
          versionLabel = release.versionLabel,
          releaseTitle = release.releaseTitle,
          releaseNotes = release.releaseNotes,
          releaseTag = release.tagName,
          releasePageUrl = release.releasePageUrl,
        )
      if (posted) {
        store.update { it.copy(lastNotifiedUpdateTag = release.tagName) }
      }
    }
    return Result.success()
  }
}

object AppUpdateChecker {

  fun syncSchedule(context: Context, settings: MasterSettings) {
    ReminderScheduler.ensureChannels(context)
    val wm = WorkManager.getInstance(context)
    if (!settings.autoCheckAppUpdates) {
      wm.cancelUniqueWork(UPDATE_WORK_NAME)
      return
    }
    val intervalHours = settings.updateCheckIntervalHours.coerceIn(6, 48).toLong()
    val constraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request =
      PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(intervalHours, TimeUnit.HOURS)
        .setConstraints(constraints)
        .addTag(UPDATE_WORK_NAME)
        .build()
    wm.enqueueUniquePeriodicWork(
      UPDATE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  suspend fun fetchLatestPublishedRelease(includePrereleases: Boolean): Result<AppReleaseInfo?> =
    withContext(Dispatchers.IO) {
      runCatching {
        val primaryUrl = if (includePrereleases) GITHUB_RELEASES_API_ALL else GITHUB_RELEASES_API_LATEST
        val raw = httpGetJson(primaryUrl)
        val obj: JSONObject? =
          if (includePrereleases) {
            val arr = JSONArray(raw)
            (0 until arr.length())
              .mapNotNull { arr.optJSONObject(it) }
              .firstOrNull { !it.optBoolean("draft", false) }
          } else {
            runCatching { JSONObject(raw) }.getOrNull()?.takeIf { !it.optBoolean("draft", false) }
          }

        if (obj == null) return@runCatching null
        parseReleaseObject(obj)
      }.recoverCatching { err ->
        // If /releases/latest returns 404 (e.g. only pre-releases published so far), fall back to /releases
        if (!includePrereleases && (err.message?.contains("404") == true)) {
          val allRaw = httpGetJson(GITHUB_RELEASES_API_ALL)
          val arr = JSONArray(allRaw)
          val first =
            (0 until arr.length())
              .mapNotNull { arr.optJSONObject(it) }
              .firstOrNull { !it.optBoolean("draft", false) }
          if (first != null) parseReleaseObject(first) else null
        } else {
          throw err
        }
      }
    }

  private fun httpGetJson(urlStr: String): String {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 10_000
      readTimeout = 10_000
      setRequestProperty("Accept", "application/vnd.github+json")
      setRequestProperty("User-Agent", "RuralERidesGO/${BuildConfig.VERSION_NAME}")
    }
    return try {
      val code = conn.responseCode
      if (code in 200..299) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        Log.w(UPDATE_TAG, "Release check HTTP $code: ${errBody.take(180)}")
        error("GitHub Releases HTTP $code")
      }
    } finally {
      conn.disconnect()
    }
  }

  internal fun parseReleaseObject(obj: JSONObject): AppReleaseInfo {
    val tag = obj.optString("tag_name").trim()
    val name = obj.optString("name").trim().ifBlank { "Rural ERides GO $tag" }
    val body = obj.optString("body").trim()
    val publishedAt = obj.optString("published_at").trim()
    val htmlUrl = obj.optString("html_url").trim().ifBlank { GITHUB_RELEASES_WEB_URL }
    val prerelease = obj.optBoolean("prerelease", false)

    var apkUrl = ""
    var apkName = ""
    var apkSize = 0L
    val assets = obj.optJSONArray("assets")
    if (assets != null) {
      for (i in 0 until assets.length()) {
        val a = assets.optJSONObject(i) ?: continue
        val aName = a.optString("name")
        if (aName.endsWith(".apk", ignoreCase = true)) {
          apkName = aName
          apkUrl = a.optString("browser_download_url")
          apkSize = a.optLong("size", 0L)
          break
        }
      }
    }

    val remoteBuild = extractBuildNumber(tag, name)
    val versionLabel =
      tag.removePrefix("v").removePrefix("V").ifBlank {
        if (remoteBuild > 0) "$remoteBuild.0" else BuildConfig.VERSION_NAME
      }
    val newer = isRemoteNewer(remoteTag = tag, remoteTitle = name, currentCode = BuildConfig.VERSION_CODE, currentName = BuildConfig.VERSION_NAME)

    val cleanNotes =
      body
        .replace(Regex("[#*`]+"), "")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(8)
        .joinToString("\n")
        .ifBlank { "Includes the latest improvements, stability updates, and build enhancements." }

    return AppReleaseInfo(
      tagName = tag.ifBlank { "v$versionLabel" },
      versionNumber = remoteBuild,
      versionLabel = versionLabel,
      releaseTitle = name,
      releaseNotes = cleanNotes,
      publishedAt = publishedAt,
      releasePageUrl = htmlUrl,
      apkAssetUrl = apkUrl,
      apkAssetName = apkName,
      apkSizeBytes = apkSize,
      isPrerelease = prerelease,
      isNewerThanCurrent = newer,
    )
  }

  internal fun extractBuildNumber(tag: String, title: String): Int {
    val buildInTitle = Regex("Build\\s*#?(\\d+)", RegexOption.IGNORE_CASE).find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (buildInTitle != null && buildInTitle > 0) return buildInTitle
    val tagMajor = Regex("(\\d+)").find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return tagMajor ?: 0
  }

  internal fun isRemoteNewer(
    remoteTag: String,
    remoteTitle: String,
    currentCode: Int,
    currentName: String,
  ): Boolean {
    val remoteBuild = extractBuildNumber(remoteTag, remoteTitle)
    if (remoteBuild > currentCode) return true
    if (remoteBuild in 1 until currentCode) return false

    val remoteParts = Regex("\\d+").findAll(remoteTag).mapNotNull { it.value.toIntOrNull() }.toList()
    val currentParts = Regex("\\d+").findAll(currentName).mapNotNull { it.value.toIntOrNull() }.toList()
    val maxLen = maxOf(remoteParts.size, currentParts.size)
    for (i in 0 until maxLen) {
      val r = remoteParts.getOrElse(i) { 0 }
      val c = currentParts.getOrElse(i) { 0 }
      if (r != c) return r > c
    }
    return false
  }

  /**
   * Opens the Google Play listing for this app using a zero-permission system Intent.
   */
  fun openGooglePlayUpdate(context: Context): Boolean {
    val pkg = BuildConfig.APPLICATION_ID
    val marketIntent =
      Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val openedMarket = runCatching {
      context.startActivity(marketIntent)
      true
    }.getOrDefault(false)
    if (openedMarket) return true

    val webIntent =
      Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return runCatching {
      context.startActivity(webIntent)
      true
    }.getOrDefault(false)
  }

  /**
   * Opens the official GitHub Release page in the system browser (zero-permission, Play-compliant).
   */
  fun openGitHubReleaseInBrowser(context: Context, releaseUrl: String = GITHUB_RELEASES_WEB_URL): Boolean {
    val safeUrl =
      releaseUrl.takeIf { it.startsWith("https://github.com/bradley3398/Rural-Erides-GO") }
        ?: GITHUB_RELEASES_WEB_URL
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return runCatching {
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  fun openPreferredUpdateChannel(
    context: Context,
    settings: MasterSettings,
    release: AppReleaseInfo?,
  ): Boolean {
    return if (settings.preferredUpdateSource == "GITHUB_RELEASES") {
      openGitHubReleaseInBrowser(context, release?.releasePageUrl ?: GITHUB_RELEASES_WEB_URL)
    } else {
      openGooglePlayUpdate(context)
    }
  }
}
