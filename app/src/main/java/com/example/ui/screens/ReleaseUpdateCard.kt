package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalOutlinedButton
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val GITHUB_LATEST_RELEASE_API =
  "https://api.github.com/repos/bradley3398/Rural-ERides-GO-V2/releases/latest"
private const val OFFICIAL_DOWNLOAD_PAGE_URL =
  "https://bradley3398.github.io/Rural-ERides-GO-V2/"

sealed class ReleaseUpdateUiState {
  data object Checking : ReleaseUpdateUiState()

  data class UpToDate(
    val installedVersion: String,
    val latestTag: String,
    val publishedDate: String,
    val releaseUrl: String,
    val apkSizeLabel: String,
  ) : ReleaseUpdateUiState()

  data class UpdateAvailable(
    val installedVersion: String,
    val latestTag: String,
    val publishedDate: String,
    val summaryLines: List<String>,
    val releaseUrl: String,
    val apkSizeLabel: String,
  ) : ReleaseUpdateUiState()

  data class NoPublishedReleaseYet(
    val installedVersion: String,
  ) : ReleaseUpdateUiState()

  data class Error(
    val message: String,
  ) : ReleaseUpdateUiState()
}

private val releaseCheckHttpClient: OkHttpClient by lazy {
  OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()
}

internal fun compareSemanticVersions(remoteTag: String, installedVersion: String): Int {
  val remoteParts = extractNumericVersionParts(remoteTag)
  val localParts = extractNumericVersionParts(installedVersion)
  val maxLen = maxOf(remoteParts.size, localParts.size)
  for (i in 0 until maxLen) {
    val r = remoteParts.getOrElse(i) { 0 }
    val l = localParts.getOrElse(i) { 0 }
    if (r != l) return r.compareTo(l)
  }
  return 0
}

private fun extractNumericVersionParts(raw: String): List<Int> {
  val cleaned = raw.trim().removePrefix("v").removePrefix("V")
  val core = cleaned.substringBefore("-").substringBefore("+")
  return core.split(".").mapNotNull { token ->
    token.filter { it.isDigit() }.toIntOrNull()
  }
}

private fun formatApkMegabytes(bytes: Long): String {
  if (bytes <= 0L) return ""
  val mb = bytes.toDouble() / (1024.0 * 1024.0)
  return "%.1f MB".format(mb)
}

internal suspend fun fetchLatestGitHubReleaseUpdate(
  installedVersion: String = BuildConfig.VERSION_NAME,
): ReleaseUpdateUiState =
  withContext(Dispatchers.IO) {
    try {
      val request =
        Request.Builder()
          .url(GITHUB_LATEST_RELEASE_API)
          .header("Accept", "application/vnd.github+json")
          .header("User-Agent", "RuralEridesGO-Android/$installedVersion")
          .build()

      releaseCheckHttpClient.newCall(request).execute().use { response ->
        if (response.code == 404) {
          return@withContext ReleaseUpdateUiState.NoPublishedReleaseYet(installedVersion)
        }
        if (!response.isSuccessful) {
          return@withContext ReleaseUpdateUiState.Error(
            "GitHub Releases returned HTTP ${response.code}. Tap Retry when online."
          )
        }
        val bodyStr = response.body?.string().orEmpty()
        if (bodyStr.isBlank()) {
          return@withContext ReleaseUpdateUiState.Error("Empty response from GitHub Releases.")
        }
        val json = JSONObject(bodyStr)
        val tagName = json.optString("tag_name", "").ifBlank { "v$installedVersion" }
        val htmlUrl = json.optString("html_url", OFFICIAL_DOWNLOAD_PAGE_URL)
          .ifBlank { OFFICIAL_DOWNLOAD_PAGE_URL }
        val publishedAt = json.optString("published_at", "").take(10)
        val bodyText = json.optString("body", "")

        var apkSizeBytes = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
          for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "").lowercase()
            if (name.endsWith(".apk")) {
              apkSizeBytes = asset.optLong("size", 0L)
              break
            }
          }
        }
        val sizeLabel = formatApkMegabytes(apkSizeBytes)

        val summaryLines =
          bodyText
            .lineSequence()
            .map { it.trim().trimStart('-', '*', '•', ' ') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .take(3)
            .toList()

        val comparison = compareSemanticVersions(tagName, installedVersion)
        if (comparison > 0) {
          ReleaseUpdateUiState.UpdateAvailable(
            installedVersion = installedVersion,
            latestTag = tagName,
            publishedDate = publishedAt,
            summaryLines = summaryLines,
            releaseUrl = htmlUrl,
            apkSizeLabel = sizeLabel,
          )
        } else {
          ReleaseUpdateUiState.UpToDate(
            installedVersion = installedVersion,
            latestTag = tagName,
            publishedDate = publishedAt,
            releaseUrl = htmlUrl,
            apkSizeLabel = sizeLabel,
          )
        }
      }
    } catch (e: Exception) {
      ReleaseUpdateUiState.Error(
        "Offline or unreachable (${e.javaClass.simpleName}). Check connection and retry."
      )
    }
  }

@Composable
fun ReleaseUpdateStatusCard(
  onStatusNotice: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  var state by remember { mutableStateOf<ReleaseUpdateUiState>(ReleaseUpdateUiState.Checking) }

  fun runCheck(manual: Boolean) {
    scope.launch {
      state = ReleaseUpdateUiState.Checking
      val result = fetchLatestGitHubReleaseUpdate(BuildConfig.VERSION_NAME)
      state = result
      if (manual) {
        val msg =
          when (result) {
            is ReleaseUpdateUiState.UpToDate -> "You are running the latest release (${result.latestTag})."
            is ReleaseUpdateUiState.UpdateAvailable -> "New release ${result.latestTag} is available."
            is ReleaseUpdateUiState.NoPublishedReleaseYet -> "Installed v${result.installedVersion}. No public GitHub tag published yet."
            is ReleaseUpdateUiState.Error -> result.message
            ReleaseUpdateUiState.Checking -> ""
          }
        if (msg.isNotBlank()) onStatusNotice(msg)
      }
    }
  }

  LaunchedEffect(Unit) {
    runCheck(manual = false)
  }

  fun openExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { ctx.startActivity(intent) }.onFailure {
      Toast.makeText(ctx, "No browser found to open $url", Toast.LENGTH_SHORT).show()
    }
  }

  TacticalCard(
    highlight = state is ReleaseUpdateUiState.UpdateAvailable,
    modifier = modifier.testTag("release_update_status_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val icon =
          when (state) {
            is ReleaseUpdateUiState.UpdateAvailable -> Icons.Default.SystemUpdate
            is ReleaseUpdateUiState.UpToDate -> Icons.Default.CheckCircle
            is ReleaseUpdateUiState.Error -> Icons.Default.CloudOff
            else -> Icons.Default.NewReleases
          }
        Icon(
          imageVector = icon,
          contentDescription = "Release update status",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Column {
          Text(
            "LIVE RELEASE & UPDATE CHECK",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = { runCheck(manual = true) },
        enabled = state !is ReleaseUpdateUiState.Checking,
        modifier = Modifier.testTag("check_updates_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Check for updates now",
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Spacer(Modifier.height(8.dp))

    AnimatedContent(targetState = state, label = "release_update_state") { ui ->
      when (ui) {
        ReleaseUpdateUiState.Checking -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
            )
            Text(
              "Checking GitHub Releases for latest signed build…",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        is ReleaseUpdateUiState.UpToDate -> {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val meta =
              buildString {
                append("Up to date with public release ${ui.latestTag}")
                if (ui.publishedDate.isNotBlank()) append(" (${ui.publishedDate})")
                if (ui.apkSizeLabel.isNotBlank()) append(" • ${ui.apkSizeLabel}")
              }
            Text(
              meta,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
            )
            TacticalOutlinedButton(
              onClick = { openExternalUrl(OFFICIAL_DOWNLOAD_PAGE_URL) },
              modifier = Modifier.fillMaxWidth().testTag("open_update_download_button"),
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Open Official Download Hub")
            }
          }
        }

        is ReleaseUpdateUiState.UpdateAvailable -> {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val header =
              buildString {
                append("Update Available: ${ui.latestTag}")
                if (ui.publishedDate.isNotBlank()) append(" • ${ui.publishedDate}")
                if (ui.apkSizeLabel.isNotBlank()) append(" • ${ui.apkSizeLabel}")
              }
            Text(
              header,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
            )
            if (ui.summaryLines.isNotEmpty()) {
              Text(
                ui.summaryLines.joinToString("\n") { "• $it" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Button(
                onClick = { openExternalUrl(OFFICIAL_DOWNLOAD_PAGE_URL) },
                modifier = Modifier.weight(1f).testTag("open_update_download_button"),
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download Page", maxLines = 1)
              }
              TacticalOutlinedButton(
                onClick = { openExternalUrl(ui.releaseUrl) },
                modifier = Modifier.weight(1f).testTag("open_github_release_notes_button"),
              ) {
                Text("Release Notes", maxLines = 1)
              }
            }
          }
        }

        is ReleaseUpdateUiState.NoPublishedReleaseYet -> {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "Running v${ui.installedVersion}. Live updates will appear here automatically as soon as your first GitHub Release tag (e.g. v${ui.installedVersion}) is published.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TacticalOutlinedButton(
              onClick = { openExternalUrl(OFFICIAL_DOWNLOAD_PAGE_URL) },
              modifier = Modifier.fillMaxWidth().testTag("open_update_download_button"),
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Open Official Download Hub")
            }
          }
        }

        is ReleaseUpdateUiState.Error -> {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              ui.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              TacticalOutlinedButton(
                onClick = { runCheck(manual = true) },
                modifier = Modifier.weight(1f).testTag("retry_update_check_button"),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry Check")
              }
              TacticalOutlinedButton(
                onClick = { openExternalUrl(OFFICIAL_DOWNLOAD_PAGE_URL) },
                modifier = Modifier.weight(1f).testTag("open_update_download_button"),
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download Page")
              }
            }
          }
        }
      }
    }
  }
}
