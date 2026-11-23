package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder

data class SocialSharePayload(
  val title: String,
  val text: String,
  val url: String = "",
)

enum class SocialPlatform(
  val id: String,
  val label: String,
  val packageName: String?,
  val brandColor: Color,
) {
  FACEBOOK("facebook", "Facebook", "com.facebook.katana", Color(0xFF1877F2)),
  X_TWITTER("x", "X", "com.twitter.android", Color(0xFF0F1419)),
  INSTAGRAM("instagram", "Instagram", "com.instagram.android", Color(0xFFE1306C)),
  TIKTOK("tiktok", "TikTok", "com.zhiliaoapp.musically", Color(0xFF010101)),
  REDDIT("reddit", "Reddit", "com.reddit.frontpage", Color(0xFFFF4500)),
  YOUTUBE("youtube", "YouTube", "com.google.android.youtube", Color(0xFFFF0000)),
  WHATSAPP("whatsapp", "WhatsApp", "com.whatsapp", Color(0xFF25D366)),
  TELEGRAM("telegram", "Telegram", "org.telegram.messenger", Color(0xFF229ED9)),
  DISCORD("discord", "Discord", "com.discord", Color(0xFF5865F2)),
  SMS("sms", "SMS", null, Color(0xFF16A34A)),
  EMAIL("email", "Email", null, Color(0xFFEA4335)),
  MORE("more", "More Apps", null, Color(0xFF475569)),
}

@Composable
fun SocialShareDialog(
  payload: SocialSharePayload,
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val fullShareText =
    buildString {
      if (payload.title.isNotBlank()) append("${payload.title}\n")
      append(payload.text)
      if (payload.url.isNotBlank() && !payload.text.contains(payload.url)) {
        append("\n${payload.url}")
      }
      if (!contains("#RuralERides")) {
        append("\n#RuralERidesGO #PEV")
      }
    }

  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "Share to Social Media",
    subtitle = payload.title.ifBlank { "Rural ERides GO Dispatch" },
    icon = Icons.Default.Share,
    badgeText = "LIVE SHARE",
    dismissButton = {
      TacticalOutlinedButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      val rows = SocialPlatform.entries.chunked(4)
      rows.forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          rowItems.forEach { platform ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    dispatchSocialShare(ctx, platform, payload.title, fullShareText, payload.url)
                    onDismiss()
                  }
                  .padding(vertical = 6.dp, horizontal = 2.dp)
                  .testTag("social_share_${platform.id}"),
            ) {
              SocialPlatformBadgeIcon(platform = platform, modifier = Modifier.size(46.dp))
              Spacer(Modifier.height(4.dp))
              Text(
                text = platform.label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = fullShareText.take(180) + if (fullShareText.length > 180) "…" else "",
          style = MaterialTheme.typography.bodySmall,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(8.dp),
        )
      }
      TacticalOutlinedButton(
        onClick = {
          copyToClipboard(ctx, payload.title, fullShareText)
          Toast.makeText(ctx, "Copied caption & link for pasting anywhere!", Toast.LENGTH_SHORT).show()
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().testTag("social_share_copy_button"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Copy Post & Hashtags")
      }
    }
  }
}

@Composable
fun SocialPlatformBadgeIcon(
  platform: SocialPlatform,
  modifier: Modifier = Modifier,
) {
  val bgBrush =
    when (platform) {
      SocialPlatform.INSTAGRAM ->
        Brush.linearGradient(
          listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFD1D1D), Color(0xFFFCAF45))
        )
      SocialPlatform.TIKTOK ->
        Brush.linearGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1F1F24)))
      else -> Brush.linearGradient(listOf(platform.brandColor, platform.brandColor))
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.clip(CircleShape).background(bgBrush),
  ) {
    when (platform) {
      SocialPlatform.FACEBOOK -> {
        Text("f", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
      }
      SocialPlatform.X_TWITTER -> {
        Text("𝕏", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
      }
      SocialPlatform.INSTAGRAM -> {
        Canvas(modifier = Modifier.size(22.dp)) {
          val stroke = 2.2.dp.toPx()
          drawRoundRect(
            color = Color.White,
            topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2, size.height - stroke * 2),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = stroke),
          )
          drawCircle(Color.White, radius = size.minDimension * 0.22f, style = Stroke(width = stroke))
          drawCircle(Color.White, radius = 1.6.dp.toPx(), center = Offset(size.width * 0.74f, size.height * 0.26f))
        }
      }
      SocialPlatform.TIKTOK -> {
        Canvas(modifier = Modifier.size(22.dp)) {
          val w = size.width
          val h = size.height
          // Cyan & Magenta glitch stem + music note loop
          drawLine(Color(0xFF25F4EE), Offset(w * 0.52f, h * 0.14f), Offset(w * 0.52f, h * 0.72f), 2.5.dp.toPx(), StrokeCap.Round)
          drawCircle(Color(0xFF25F4EE), radius = w * 0.18f, center = Offset(w * 0.36f, h * 0.72f), style = Stroke(2.2.dp.toPx()))
          drawLine(Color(0xFFFE2C55), Offset(w * 0.58f, h * 0.18f), Offset(w * 0.58f, h * 0.76f), 2.5.dp.toPx(), StrokeCap.Round)
          drawCircle(Color(0xFFFE2C55), radius = w * 0.18f, center = Offset(w * 0.42f, h * 0.76f), style = Stroke(2.2.dp.toPx()))
          drawLine(Color.White, Offset(w * 0.55f, h * 0.16f), Offset(w * 0.55f, h * 0.74f), 2.4.dp.toPx(), StrokeCap.Round)
          drawLine(Color.White, Offset(w * 0.55f, h * 0.18f), Offset(w * 0.80f, h * 0.30f), 2.4.dp.toPx(), StrokeCap.Round)
          drawCircle(Color.White, radius = w * 0.18f, center = Offset(w * 0.39f, h * 0.74f), style = Stroke(2.2.dp.toPx()))
        }
      }
      SocialPlatform.REDDIT -> {
        Canvas(modifier = Modifier.size(22.dp)) {
          val w = size.width
          val h = size.height
          drawOval(Color.White, topLeft = Offset(w * 0.12f, h * 0.34f), size = Size(w * 0.76f, h * 0.52f))
          drawCircle(Color(0xFFFF4500), radius = w * 0.07f, center = Offset(w * 0.36f, h * 0.56f))
          drawCircle(Color(0xFFFF4500), radius = w * 0.07f, center = Offset(w * 0.64f, h * 0.56f))
          drawLine(Color.White, Offset(w * 0.50f, h * 0.34f), Offset(w * 0.66f, h * 0.14f), 2.dp.toPx(), StrokeCap.Round)
          drawCircle(Color.White, radius = w * 0.08f, center = Offset(w * 0.72f, h * 0.14f))
        }
      }
      SocialPlatform.YOUTUBE -> {
        Canvas(modifier = Modifier.size(22.dp)) {
          val path =
            Path().apply {
              moveTo(size.width * 0.36f, size.height * 0.24f)
              lineTo(size.width * 0.78f, size.height * 0.50f)
              lineTo(size.width * 0.36f, size.height * 0.76f)
              close()
            }
          drawPath(path, Color.White)
        }
      }
      SocialPlatform.WHATSAPP -> {
        Text("WA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
      }
      SocialPlatform.TELEGRAM -> {
        Canvas(modifier = Modifier.size(22.dp)) {
          val path =
            Path().apply {
              moveTo(size.width * 0.14f, size.height * 0.48f)
              lineTo(size.width * 0.86f, size.height * 0.18f)
              lineTo(size.width * 0.68f, size.height * 0.84f)
              lineTo(size.width * 0.46f, size.height * 0.60f)
              close()
            }
          drawPath(path, Color.White)
        }
      }
      SocialPlatform.DISCORD -> {
        Text("DC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
      }
      SocialPlatform.SMS -> {
        Icon(Icons.Default.Sms, contentDescription = "SMS", tint = Color.White, modifier = Modifier.size(22.dp))
      }
      SocialPlatform.EMAIL -> {
        Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.White, modifier = Modifier.size(22.dp))
      }
      SocialPlatform.MORE -> {
        Icon(Icons.Default.Share, contentDescription = "More Apps", tint = Color.White, modifier = Modifier.size(22.dp))
      }
    }
  }
}

private fun copyToClipboard(ctx: Context, label: String, text: String) {
  val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  cm?.setPrimaryClip(ClipData.newPlainText(label.ifBlank { "Rural ERides GO" }, text))
}

fun dispatchSocialShare(
  ctx: Context,
  platform: SocialPlatform,
  title: String,
  fullText: String,
  url: String,
) {
  val encText = URLEncoder.encode(fullText, "UTF-8")
  val encTitle = URLEncoder.encode(title.ifBlank { "Rural ERides GO" }, "UTF-8")
  val shareUrl = url.ifBlank { "https://www.youtube.com/@RuralERides" }
  val encUrl = URLEncoder.encode(shareUrl, "UTF-8")

  // Always copy caption for platforms like Instagram/TikTok/Facebook that restrict prefilled text
  if (platform == SocialPlatform.INSTAGRAM || platform == SocialPlatform.TIKTOK || platform == SocialPlatform.FACEBOOK || platform == SocialPlatform.YOUTUBE) {
    copyToClipboard(ctx, title, fullText)
  }

  when (platform) {
    SocialPlatform.SMS -> {
      val smsIntent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
          putExtra("sms_body", fullText)
        }
      runCatching { ctx.startActivity(smsIntent) }.onFailure { launchGenericChooser(ctx, title, fullText) }
      return
    }
    SocialPlatform.EMAIL -> {
      val emailIntent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
          putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Rural ERides GO Update" })
          putExtra(Intent.EXTRA_TEXT, fullText)
        }
      runCatching { ctx.startActivity(emailIntent) }.onFailure { launchGenericChooser(ctx, title, fullText) }
      return
    }
    SocialPlatform.MORE -> {
      launchGenericChooser(ctx, title, fullText)
      return
    }
    else -> Unit
  }

  // Try direct native app share first
  val pkg = platform.packageName
  if (pkg != null) {
    val direct =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage(pkg)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, fullText)
      }
    if (runCatching { ctx.startActivity(direct) }.isSuccess) {
      if (platform == SocialPlatform.INSTAGRAM || platform == SocialPlatform.TIKTOK) {
        Toast.makeText(ctx, "Caption copied — paste into ${platform.label}!", Toast.LENGTH_SHORT).show()
      }
      return
    }
  }

  // Graceful web composer fallback if the app is not installed
  val webUrl =
    when (platform) {
      SocialPlatform.FACEBOOK -> "https://www.facebook.com/sharer/sharer.php?u=$encUrl&quote=$encText"
      SocialPlatform.X_TWITTER -> "https://x.com/intent/tweet?text=$encText"
      SocialPlatform.REDDIT -> "https://www.reddit.com/submit?title=$encTitle&text=$encText"
      SocialPlatform.WHATSAPP -> "https://wa.me/?text=$encText"
      SocialPlatform.TELEGRAM -> "https://t.me/share/url?url=$encUrl&text=$encText"
      SocialPlatform.INSTAGRAM -> "https://www.instagram.com/"
      SocialPlatform.TIKTOK -> "https://www.tiktok.com/"
      SocialPlatform.YOUTUBE -> "https://www.youtube.com/"
      SocialPlatform.DISCORD -> "https://discord.com/app"
      else -> null
    }
  if (webUrl != null) {
    if (platform == SocialPlatform.INSTAGRAM || platform == SocialPlatform.TIKTOK || platform == SocialPlatform.FACEBOOK) {
      Toast.makeText(ctx, "Caption copied to clipboard — opening ${platform.label}!", Toast.LENGTH_SHORT).show()
    }
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
      .onFailure { launchGenericChooser(ctx, title, fullText) }
  } else {
    launchGenericChooser(ctx, title, fullText)
  }
}

private fun launchGenericChooser(ctx: Context, title: String, text: String) {
  runCatching {
    ctx.startActivity(
      Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, title)
          putExtra(Intent.EXTRA_TEXT, text)
        },
        "Share via",
      )
    )
  }
}
