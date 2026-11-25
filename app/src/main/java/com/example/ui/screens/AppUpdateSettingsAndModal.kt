package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.AppReleaseInfo
import com.example.core.AppUpdateCheckState
import com.example.core.AppUpdateChecker
import com.example.core.GITHUB_RELEASES_WEB_URL
import com.example.core.GITHUB_REPO_URL
import com.example.core.ReminderScheduler
import com.example.data.MasterSettings
import com.example.ui.components.TacticalCard
import com.example.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppUpdatesSettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  onRequestNotifPermission: () -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val ctx = LocalContext.current
  val updateState by sessionVm.appUpdateState.collectAsStateWithLifecycle()

  val lastCheckedText =
    remember(settings.lastUpdateCheckMs) {
      if (settings.lastUpdateCheckMs > 0L) {
        "Last checked: ${SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(settings.lastUpdateCheckMs))}"
      } else {
        "Not checked yet on this device"
      }
    }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // 1. CURRENT BUILD & LIVE CHECK CARD
    TacticalCard(highlight = true, title = "APP_UPDATES.SYS") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector =
            if (updateState is AppUpdateCheckState.UpdateAvailable) Icons.Default.NewReleases
            else Icons.Default.SystemUpdate,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f)) {
          Text(
            "Rural ERides GO v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            lastCheckedText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      when (val state = updateState) {
        AppUpdateCheckState.Idle -> {
          Text(
            "Tap below to check live published builds on GitHub Releases and Google Play.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        AppUpdateCheckState.Checking -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Checking for newly published builds...", style = MaterialTheme.typography.bodySmall)
          }
        }
        is AppUpdateCheckState.UpToDate -> {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              val latestStr = state.latestRelease?.tagName?.takeIf { it.isNotBlank() }
              Text(
                if (latestStr != null) {
                  "You are on the latest build (Latest published: $latestStr)."
                } else {
                  "You are up to date on Build ${BuildConfig.VERSION_CODE}."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
        is AppUpdateCheckState.UpdateAvailable -> {
          val rel = state.release
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                "NEW BUILD AVAILABLE: ${rel.tagName}" +
                  if (rel.versionNumber > 0) " (Build ${rel.versionNumber})" else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                rel.releaseTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                rel.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(Modifier.height(4.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                  onClick = { AppUpdateChecker.openGitHubReleaseInBrowser(ctx, rel.releasePageUrl) },
                  modifier = Modifier.weight(1f).testTag("settings_update_github_release_button"),
                ) {
                  Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Open Repo Page", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                  onClick = { AppUpdateChecker.openGooglePlayUpdate(ctx) },
                  modifier = Modifier.weight(1f).testTag("settings_update_google_play_button"),
                ) {
                  Icon(Icons.Default.Shop, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Google Play", maxLines = 1, softWrap = false)
                }
              }
            }
          }
        }
        is AppUpdateCheckState.Error -> {
          Text(
            "Update check failed: ${state.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = {
            sessionVm.checkForAppUpdates(manual = true)
            onStatusMessage("Checking for newly published builds...")
          },
          enabled = updateState !is AppUpdateCheckState.Checking,
          modifier = Modifier.weight(1f).testTag("check_updates_now_button"),
        ) {
          Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            if (updateState is AppUpdateCheckState.Checking) "Checking..." else "Check Now",
            maxLines = 1,
            softWrap = false,
          )
        }
        OutlinedButton(
          onClick = { AppUpdateChecker.openGitHubReleaseInBrowser(ctx, GITHUB_REPO_URL) },
          modifier = Modifier.weight(1f).testTag("open_repo_page_updates_button"),
        ) {
          Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Repo Page", maxLines = 1, softWrap = false)
        }
      }
    }

    // 2. NOTIFICATION & RECEIVE BUILD PREFERENCES
    TacticalCard(title = "BUILD_NOTIFICATIONS.CFG") {
      Text(
        "NEW BUILD ALERTS & DELIVERY OPTIONS",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(4.dp))
      AndroidSettingSwitch(
        label = "Auto-Check for New Builds (On Launch & Background)",
        checked = settings.autoCheckAppUpdates,
      ) { enabled ->
        sessionVm.updateSettings { it.copy(autoCheckAppUpdates = enabled) }
        onStatusMessage(
          if (enabled) "Background new-build checks enabled (every ${settings.updateCheckIntervalHours}h)."
          else "Automatic new-build checks paused.",
        )
      }

      AndroidSettingSwitch(
        label = "Notify Me When a New Build Is Published",
        checked = settings.appUpdateNotifsEnabled,
      ) { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasNotificationPermission(ctx)) {
          onRequestNotifPermission()
        }
        sessionVm.updateSettings { it.copy(appUpdateNotifsEnabled = enabled) }
        onStatusMessage(
          if (enabled) "New build notifications enabled." else "New build notifications muted.",
        )
      }

      AndroidSettingSwitch(
        label = "Include Pre-Release / Early Test Builds",
        checked = settings.includePrereleaseBuilds,
      ) { enabled ->
        sessionVm.updateSettings { it.copy(includePrereleaseBuilds = enabled) }
        sessionVm.checkForAppUpdates(manual = true)
      }

      Spacer(Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Spacer(Modifier.height(8.dp))

      Text(
        "BACKGROUND CHECK FREQUENCY",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(6 to "Every 6h", 12 to "Every 12h", 24 to "Daily").forEach { (hours, label) ->
          FilterChip(
            selected = settings.updateCheckIntervalHours == hours,
            onClick = {
              sessionVm.updateSettings { it.copy(updateCheckIntervalHours = hours) }
              onStatusMessage("Update check frequency set to $label.")
            },
            label = { Text(label, maxLines = 1, softWrap = false) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      Text(
        "PREFERRED UPDATE CHANNEL",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
          selected = settings.preferredUpdateSource == "GITHUB_RELEASES",
          onClick = {
            sessionVm.updateSettings { it.copy(preferredUpdateSource = "GITHUB_RELEASES") }
            onStatusMessage("Preferred update link: GitHub Repo Page.")
          },
          label = { Text("GitHub Repo", maxLines = 1, softWrap = false) },
          leadingIcon = { Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.weight(1f).testTag("update_source_github_chip"),
        )
        FilterChip(
          selected = settings.preferredUpdateSource != "GITHUB_RELEASES",
          onClick = {
            sessionVm.updateSettings { it.copy(preferredUpdateSource = "GOOGLE_PLAY") }
            onStatusMessage("Preferred update link: Google Play Store.")
          },
          label = { Text("Google Play", maxLines = 1, softWrap = false) },
          leadingIcon = { Icon(Icons.Default.Shop, null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.weight(1f).testTag("update_source_play_chip"),
        )
      }

      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasNotificationPermission(ctx)) {
              onRequestNotifPermission()
              onStatusMessage("Grant notification permission to preview build alerts.")
            } else {
              val posted =
                ReminderScheduler.postAppUpdateNotification(
                  context = ctx,
                  versionLabel = BuildConfig.VERSION_NAME,
                  releaseTitle = "Rural ERides GO v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                  releaseNotes = "Tap this notification to open the GitHub repository releases page.",
                  releaseTag = "v${BuildConfig.VERSION_NAME}",
                  releasePageUrl = GITHUB_RELEASES_WEB_URL,
                )
              onStatusMessage(
                if (posted) "Test notification sent! Tap it to open the GitHub repo page."
                else "Enable notifications in Android Settings to receive build alerts.",
              )
            }
          },
          modifier = Modifier.weight(1f).testTag("test_update_notification_button"),
        ) {
          Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Test Alert", maxLines = 1, softWrap = false)
        }
        OutlinedButton(
          onClick = { AppUpdateChecker.openGitHubReleaseInBrowser(ctx, GITHUB_RELEASES_WEB_URL) },
          modifier = Modifier.weight(1f).testTag("open_all_github_releases_button"),
        ) {
          Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("All Releases", maxLines = 1, softWrap = false)
        }
      }

      if (settings.skippedUpdateTag.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        TextButton(
          onClick = {
            sessionVm.updateSettings { it.copy(skippedUpdateTag = "") }
            sessionVm.checkForAppUpdates(manual = true)
            onStatusMessage("Cleared skipped build filter.")
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Reset Skipped Build (${settings.skippedUpdateTag})")
        }
      }
    }
  }
}

@Composable
fun AppUpdateAvailableBanner(
  release: AppReleaseInfo,
  onOpenUpdateSheet: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onOpenUpdateSheet,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
    shadowElevation = 6.dp,
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("app_update_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        Icons.Default.NewReleases,
        contentDescription = "New Build Available",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
      Column(Modifier.weight(1f)) {
        Text(
          "New Build Published: ${release.tagName}",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          "Tap to see what's new & update from Build ${BuildConfig.VERSION_CODE}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(36.dp).testTag("dismiss_app_update_banner_button"),
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Dismiss update banner",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun AppUpdateAvailableDialog(
  updateState: AppUpdateCheckState,
  settings: MasterSettings,
  onCheckNow: () -> Unit,
  onSkipBuild: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val release = (updateState as? AppUpdateCheckState.UpdateAvailable)?.release

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = if (release != null) Icons.Default.NewReleases else Icons.Default.SystemUpdate,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text =
          if (release != null) "New Build Ready: ${release.tagName}"
          else "Rural ERides GO Updates",
        fontWeight = FontWeight.Black,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
        )

        when (updateState) {
          AppUpdateCheckState.Checking -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Text("Checking latest published build...", style = MaterialTheme.typography.bodySmall)
            }
          }
          is AppUpdateCheckState.UpdateAvailable -> {
            val rel = updateState.release
            Text(
              text = rel.releaseTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = rel.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
              )
            }
            Text(
              text = "Tap below to open the GitHub repository release page in your browser or check Google Play.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is AppUpdateCheckState.UpToDate -> {
            Text(
              text = "You are already running the latest published build (v${BuildConfig.VERSION_NAME}).",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
          is AppUpdateCheckState.Error -> {
            Text(
              text = "Could not check right now: ${updateState.message}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
          AppUpdateCheckState.Idle -> {
            Text(
              text = "Tap Check Now to query newly published builds.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    },
    confirmButton = {
      if (release != null) {
        Button(
          onClick = {
            AppUpdateChecker.openPreferredUpdateChannel(ctx, settings, release)
            onDismiss()
          },
          modifier = Modifier.testTag("update_dialog_primary_button"),
        ) {
          Text(
            if (settings.preferredUpdateSource == "GITHUB_RELEASES") "Open Repo Page"
            else "Update on Play",
          )
        }
      } else {
        Button(
          onClick = onCheckNow,
          enabled = updateState !is AppUpdateCheckState.Checking,
          modifier = Modifier.testTag("update_dialog_check_now_button"),
        ) {
          Text("Check Now")
        }
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (release != null) {
          TextButton(
            onClick = {
              if (settings.preferredUpdateSource == "GITHUB_RELEASES") {
                AppUpdateChecker.openGooglePlayUpdate(ctx)
              } else {
                AppUpdateChecker.openGitHubReleaseInBrowser(ctx, release.releasePageUrl)
              }
              onDismiss()
            },
            modifier = Modifier.testTag("update_dialog_secondary_source_button"),
          ) {
            Text(if (settings.preferredUpdateSource == "GITHUB_RELEASES") "Play Store" else "GitHub")
          }
          TextButton(
            onClick = { onSkipBuild(release.tagName) },
            modifier = Modifier.testTag("update_dialog_skip_button"),
          ) {
            Text("Skip")
          }
        }
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("update_dialog_close_button"),
        ) {
          Text(if (release != null) "Later" else "Close")
        }
      }
    },
  )
}
