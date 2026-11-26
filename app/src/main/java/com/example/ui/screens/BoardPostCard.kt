package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BoardPost
import com.example.data.PublicPilotProfile
import com.example.ui.components.CloudImage
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.rememberHostWindowSafeInsets
import com.example.ui.components.SocialPlatform
import com.example.ui.components.SocialPlatformBadgeIcon
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import com.example.ui.components.dispatchSocialShare

@Composable
fun BoardPostFeedCard(
  post: BoardPost,
  authorPilot: PublicPilotProfile?,
  myUid: String,
  cloudSyncReady: Boolean,
  isSaved: Boolean,
  isFollowing: Boolean,
  translatedSummary: String?,
  sosTriage: String?,
  sosTriageBusy: Boolean,
  searchQuery: String,
  onInspectPilot: (PublicPilotProfile) -> Unit,
  onToggleFollow: () -> Unit,
  onToggleBookmark: () -> Unit,
  onToggleMute: () -> Unit,
  onTranslate: () -> Unit,
  onEdit: () -> Unit,
  onFlag: () -> Unit,
  onDelete: () -> Unit,
  onHashtagClick: (String) -> Unit,
  onVotePoll: (Int) -> Unit,
  onVolt: () -> Unit,
  onOpenComments: () -> Unit,
  onBoost: () -> Unit,
  onQuoteBoost: (String) -> Unit = {},
  onOpenDm: () -> Unit,
  onStartCall: (String) -> Unit,
  onRunSosTriage: () -> Unit,
  onOpenShare: (SocialSharePayload) -> Unit,
  onInspectImage: (String) -> Unit,
  onCopyNotice: (String) -> Unit,
) {
  val ctx = LocalContext.current
  val urgent = post.helpNeeded || post.speedTrap || post.stolenAlert || post.category == "SOS" || post.category == "STOLEN"
  val relativeTime = formatRelativeTimestamp(post.createdAt?.seconds)
  val postTags =
    remember(post.id, post.title, post.body) {
      Regex("#[A-Za-z0-9_]{2,28}").findAll("${post.title} ${post.body}").map { it.value }.distinct().take(6).toList()
    }
  var menuExpanded by remember { mutableStateOf(false) }
  var showQuickCrossPost by remember { mutableStateOf(false) }
  var showQuoteDialog by remember { mutableStateOf(false) }
  var quoteCommentary by remember { mutableStateOf("") }

  val targetPilot =
    authorPilot
      ?: PublicPilotProfile(
        userId = post.userId,
        callsign = post.authorCallsign,
        rankBadge = post.authorBadge,
      )

  val accentColor =
    when {
      urgent -> MaterialTheme.colorScheme.error
      post.category == "MARKETPLACE" -> MaterialTheme.colorScheme.tertiary
      post.category == "BUILDS" || post.category == "GARAGE" -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.primary
    }

  TacticalCard(
    highlight = urgent,
    modifier = Modifier.fillMaxWidth().testTag("board_post_card_${post.id}"),
  ) {
    // 1. CLEAN SOCIAL AUTHOR HEADER
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .clip(CircleShape)
            .border(1.5.dp, accentColor.copy(alpha = 0.65f), CircleShape)
            .clickable { onInspectPilot(targetPilot) }
            .padding(2.dp),
      ) {
        PilotAvatarThumb(
          avatarSource = authorPilot?.avatarBase64.orEmpty(),
          callsign = post.authorCallsign,
          size = 40.dp,
        )
      }

      Column(
        modifier = Modifier.weight(1f).clickable { onInspectPilot(targetPilot) },
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = post.authorCallsign.ifBlank { "PILOT" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (authorPilot?.verified == true) {
            Icon(
              Icons.Default.Verified,
              contentDescription = "Verified Pilot",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(15.dp),
            )
          }
          Text(
            text = "• $relativeTime",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
          ) {
            Text(
              text = post.category.replace('_', ' '),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
          if (post.authorBadge.isNotBlank()) {
            Text(
              text = post.authorBadge,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (authorPilot != null && (authorPilot.ridesCount > 0L || authorPilot.trustScore > 0L)) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "${authorPilot.ridesCount} rides • Trust ${authorPilot.trustScore}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      if (post.userId != myUid && post.userId.isNotBlank()) {
        Surface(
          onClick = onToggleFollow,
          color = if (isFollowing) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, if (isFollowing) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
              contentDescription = if (isFollowing) "Unfollow" else "Follow",
              tint = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (isFollowing) "Following" else "Follow",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(48.dp).testTag("post_more_menu_${post.id}"),
        ) {
          Icon(Icons.Default.MoreVert, contentDescription = "More Post Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          DropdownMenuItem(
            text = { Text(if (isSaved) "Remove Bookmark" else "Save Bookmark") },
            leadingIcon = { Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null) },
            onClick = {
              menuExpanded = false
              onToggleBookmark()
            },
          )
          DropdownMenuItem(
            text = { Text("AI Summarize / Translate") },
            leadingIcon = { Icon(Icons.Default.Translate, null) },
            onClick = {
              menuExpanded = false
              onTranslate()
            },
          )
          DropdownMenuItem(
            text = { Text("Copy Transmission") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
            onClick = {
              menuExpanded = false
              val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              cm.setPrimaryClip(ClipData.newPlainText("Post", "${post.title}\n${post.body}"))
              onCopyNotice("Copied transmission to clipboard.")
            },
          )
          if (post.userId != myUid && post.userId.isNotBlank()) {
            DropdownMenuItem(
              text = { Text("Direct Message Author") },
              leadingIcon = { Icon(Icons.Default.Mail, null) },
              onClick = {
                menuExpanded = false
                onOpenDm()
              },
            )
            if (cloudSyncReady) {
              DropdownMenuItem(
                text = { Text("Voice Call Author") },
                leadingIcon = { Icon(Icons.Default.Call, null) },
                onClick = {
                  menuExpanded = false
                  onStartCall("AUDIO")
                },
              )
              DropdownMenuItem(
                text = { Text("Video Call Author") },
                leadingIcon = { Icon(Icons.Default.Videocam, null) },
                onClick = {
                  menuExpanded = false
                  onStartCall("VIDEO")
                },
              )
              DropdownMenuItem(
                text = { Text("Flag Unsafe / Spam") },
                leadingIcon = { Icon(Icons.Default.Flag, null) },
                onClick = {
                  menuExpanded = false
                  onFlag()
                },
              )
            }
            DropdownMenuItem(
              text = { Text("Mute Author") },
              leadingIcon = { Icon(Icons.Default.VolumeOff, null) },
              onClick = {
                menuExpanded = false
                onToggleMute()
              },
            )
          }
          if (post.userId == myUid && cloudSyncReady) {
            DropdownMenuItem(
              text = { Text("Edit Post") },
              leadingIcon = { Icon(Icons.Default.Edit, null) },
              onClick = {
                menuExpanded = false
                onEdit()
              },
            )
          }
          if (post.userId == myUid) {
            DropdownMenuItem(
              text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
              leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
              onClick = {
                menuExpanded = false
                onDelete()
              },
            )
          }
        }
      }
    }

    // 2. URGENT / PRICE / SECTOR / TELEMETRY CONTEXT CHIPS
    if (urgent) {
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (post.helpNeeded || post.category == "SOS") BoardUrgentBadge("SOS HELP")
        if (post.speedTrap) BoardUrgentBadge("ROAD ALERT")
        if (post.stolenAlert || post.category == "STOLEN") BoardUrgentBadge("STOLEN PEV BOLO")
      }
    }

    if (post.priceUsd > 0.0 || post.locationLabel.isNotBlank()) {
      Spacer(Modifier.height(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (post.priceUsd > 0.0) {
          Surface(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
          ) {
            Text(
              text = "$${"%.0f".format(post.priceUsd)}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        if (post.locationLabel.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
            Text(
              text = post.locationLabel,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // 3. HEADLINE & READABLE BODY
    if (post.title.isNotBlank()) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = post.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp,
      )
    }

    if (post.body.isNotBlank()) {
      Spacer(Modifier.height(6.dp))
      Text(
        text = post.body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        lineHeight = 21.sp,
      )
    }

    if (post.telemetrySummary.isNotBlank()) {
      Spacer(Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
          Text(
            text = post.telemetrySummary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // 4. HASHTAGS
    if (postTags.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(postTags) { tag ->
          val activeTag = searchQuery.equals(tag, true)
          Surface(
            onClick = { onHashtagClick(tag) },
            color = if (activeTag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(
              text = tag,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (activeTag) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }
    }

    // 5. FULL-WIDTH RICH PHOTO MEDIA (TAP TO LIGHTBOX)
    if (post.imageUrl.isNotBlank()) {
      Spacer(Modifier.height(10.dp))
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable { onInspectImage(post.imageUrl) },
      ) {
        CloudImage(
          source = post.imageUrl,
          contentDescription = post.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Surface(
          color = Color.Black.copy(alpha = 0.55f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
          Text(
            text = "Tap to expand",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }

    // 6. MODERN INTERACTIVE LIVE POLL
    if (post.pollQuestion.isNotBlank() && post.pollOptions.isNotEmpty()) {
      val totalVotes = post.pollOptions.sumOf { raw -> raw.split("|").getOrNull(1)?.toIntOrNull() ?: 0 }
      val safeDenominator = totalVotes.coerceAtLeast(1)
      Spacer(Modifier.height(10.dp))
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = post.pollQuestion,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = "$totalVotes votes",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          post.pollOptions.forEachIndexed { idx, raw ->
            val parts = raw.split("|")
            val label = parts.firstOrNull() ?: raw
            val votes = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val pct = votes.toFloat() / safeDenominator.toFloat()
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surface)
                  .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                  .clickable { onVotePoll(idx) },
            ) {
              Box(
                modifier =
                  Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                    .background(
                      Brush.horizontalGradient(
                        listOf(
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                          MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                        )
                      )
                    ),
              )
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${(pct * 100).toInt()}% ($votes)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }

    // 7. AI SUMMARY & GEMINI SOS TRIAGE CALLOUTS
    translatedSummary?.let { tr ->
      Spacer(Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("AI SUMMARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
          Text(tr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }

    sosTriage?.let { triage ->
      Spacer(Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("GEMINI SOS AI TRIAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
          Text(triage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }

    if (post.helpNeeded || post.category == "SOS") {
      Spacer(Modifier.height(8.dp))
      OutlinedButton(
        onClick = onRunSosTriage,
        enabled = !sosTriageBusy,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (sosTriageBusy) {
          CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(8.dp))
          Text("Running Gemini SOS Triage...")
        } else {
          Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Run Gemini Roadside SOS Triage")
        }
      }
    }

    // 8. CLEAN MODERN SOCIAL ENGAGEMENT FOOTER BAR
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    Spacer(Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FeedActionPill(
        icon = Icons.Default.ElectricBolt,
        label = if (post.likesCount > 0) "${post.likesCount}" else "Volt",
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = "Volt Post",
        onClick = onVolt,
      )
      FeedActionPill(
        icon = Icons.Default.ChatBubbleOutline,
        label = if (post.commentsCount > 0) "${post.commentsCount}" else "Reply",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = "Comments",
        onClick = onOpenComments,
      )
      FeedActionPill(
        icon = Icons.Default.Repeat,
        label = "Boost",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = "Boost Post",
        onClick = onBoost,
      )
      FeedActionPill(
        icon = Icons.Default.FormatQuote,
        label = "Quote",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = "Quote Repost",
        modifier = Modifier.testTag("board_quote_post_${post.id}"),
        onClick = { showQuoteDialog = true },
      )
      if (post.userId != myUid && post.userId.isNotBlank()) {
        FeedActionPill(
          icon = Icons.Default.Mail,
          label = "DM",
          tint = MaterialTheme.colorScheme.secondary,
          contentDescription = "Direct Message",
          onClick = onOpenDm,
        )
      }
      FeedActionPill(
        icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
        label = "",
        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = "Bookmark",
        onClick = onToggleBookmark,
      )
      FeedActionPill(
        icon = Icons.Default.Share,
        label = "",
        tint = if (showQuickCrossPost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = "Share Post",
        modifier = Modifier.testTag("board_social_share_${post.id}"),
        onClick = { showQuickCrossPost = !showQuickCrossPost },
      )
    }

    if (showQuoteDialog) {
      AlertDialog(
        onDismissRequest = { showQuoteDialog = false },
        title = { Text("Quote Repost @${post.authorCallsign.ifBlank { "PILOT" }}") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(post.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (post.body.isNotBlank()) {
                  Text(post.body, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
            OutlinedTextField(
              value = quoteCommentary,
              onValueChange = { quoteCommentary = it },
              label = { Text("Add your commentary") },
              placeholder = { Text("Share trail thoughts, rig notes, or backup callout...") },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth().testTag("quote_repost_input_${post.id}"),
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              onQuoteBoost(quoteCommentary.trim())
              quoteCommentary = ""
              showQuoteDialog = false
            },
            modifier = Modifier.testTag("quote_repost_submit_${post.id}"),
          ) { Text("Publish Quote") }
        },
        dismissButton = {
          TextButton(onClick = { showQuoteDialog = false }) { Text("Cancel") }
        },
      )
    }

    AnimatedVisibility(visible = showQuickCrossPost) {
      Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val postShareText =
          "[${post.category}] ${post.title} — by ${post.authorCallsign}\n${post.body.take(180)}\n#RuralERidesGO #PEV"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            items(
              listOf(
                SocialPlatform.FACEBOOK,
                SocialPlatform.X_TWITTER,
                SocialPlatform.INSTAGRAM,
                SocialPlatform.TIKTOK,
                SocialPlatform.REDDIT,
                SocialPlatform.WHATSAPP,
              ).size,
            ) { idx ->
              val plat =
                listOf(
                  SocialPlatform.FACEBOOK,
                  SocialPlatform.X_TWITTER,
                  SocialPlatform.INSTAGRAM,
                  SocialPlatform.TIKTOK,
                  SocialPlatform.REDDIT,
                  SocialPlatform.WHATSAPP,
                )[idx]
              IconButton(
                onClick = {
                  dispatchSocialShare(
                    ctx = ctx,
                    platform = plat,
                    title = post.title,
                    fullText = postShareText,
                    url = post.imageUrl.takeIf { it.startsWith("http") }.orEmpty(),
                  )
                },
                modifier = Modifier.size(44.dp).testTag("quick_share_${plat.id}_${post.id}"),
              ) {
                SocialPlatformBadgeIcon(platform = plat, modifier = Modifier.size(26.dp))
              }
            }
          }
          Surface(
            onClick = {
              val shareTxt =
                buildString {
                  append("[${post.category}] ${post.title}\n")
                  append("By ${post.authorCallsign}")
                  if (post.priceUsd > 0.0) append(" • $${"%.0f".format(post.priceUsd)}")
                  if (post.body.isNotBlank()) append("\n\n${post.body}")
                }
              onOpenShare(
                SocialSharePayload(
                  title = post.title,
                  text = shareTxt,
                  url = post.imageUrl.takeIf { it.startsWith("http") }.orEmpty(),
                )
              )
            },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 40.dp),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Text(
                "All Apps",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeedActionPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = Color.Transparent,
    shape = RoundedCornerShape(18.dp),
    modifier = modifier.heightIn(min = 44.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
      if (label.isNotBlank()) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = tint,
        )
      }
    }
  }
}

@Composable
fun BoardUrgentBadge(label: String) {
  Surface(
    color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.error,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun PostImageLightboxDialog(
  imageUrl: String,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    ConfigureEdgeToEdgeDialogWindow(fullScreen = true)
    val safeInsets = rememberHostWindowSafeInsets()
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.94f))
          .padding(
            start = safeInsets.start,
            end = safeInsets.end,
            top = safeInsets.top,
            bottom = safeInsets.bottom,
          )
          .clickable { onDismiss() },
      contentAlignment = Alignment.Center,
    ) {
      CloudImage(
        source = imageUrl,
        contentDescription = "Full post photo",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().padding(12.dp),
      )
      IconButton(
        onClick = onDismiss,
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(14.dp)
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.68f), CircleShape),
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close Photo", tint = Color.White)
      }
    }
  }
}

internal fun formatRelativeTimestamp(epochSec: Long?): String {
  if (epochSec == null || epochSec <= 0L) return "LIVE"
  val diff = (System.currentTimeMillis() / 1000L - epochSec).coerceAtLeast(0L)
  return when {
    diff < 60L -> "${diff}s ago"
    diff < 3600L -> "${diff / 60L}m ago"
    diff < 86400L -> "${diff / 3600L}h ago"
    else -> "${diff / 86400L}d ago"
  }
}
