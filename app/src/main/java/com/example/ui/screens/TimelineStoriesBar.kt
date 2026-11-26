package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.CloudImage
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.decodeSampledBitmap
import com.example.ui.components.rememberHostWindowSafeInsets
import com.example.ui.components.tacticalOutlinedTextFieldColors

data class TimelineStoryItem(
  val id: String,
  val userId: String,
  val authorCallsign: String,
  val avatarBase64: String,
  val mediaUrl: String,
  val caption: String,
  val batteryStatus: String,
  val timestampEpoch: Long,
)

@Composable
fun TimelineStoriesBar(
  myCallsign: String,
  myAvatar: String,
  stories: List<TimelineStoryItem>,
  cloudSyncReady: Boolean = true,
  onAddStory: (caption: String, photoBitmap: Bitmap?, mediaUrl: String) -> Unit,
  onViewStory: (TimelineStoryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ctx = LocalContext.current
  var showAddStoryDialog by remember { mutableStateOf(false) }

  TacticalCard(modifier = modifier.fillMaxWidth().testTag("timeline_stories_bar")) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "RIDER TIMELINE STORIES (${stories.size} LIVE)",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text("Live 24h Trail Feed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Add Story Button
        item {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(enabled = cloudSyncReady) { showAddStoryDialog = true }.testTag("add_story_button"),
          ) {
            Box(
              modifier =
                Modifier
                  .size(62.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer)
                  .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              PilotAvatarThumb(avatarSource = myAvatar, callsign = myCallsign, size = 56.dp)
              Box(
                modifier =
                  Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add Story", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
              }
            }
            Spacer(Modifier.height(4.dp))
            Text("Your Story", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        // Active 24h Live Stories from Firestore
        items(stories, key = { it.id }) { story ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onViewStory(story) }.testTag("story_item_${story.id}"),
          ) {
            Box(
              modifier =
                Modifier
                  .size(62.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.sweepGradient(
                      listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                    )
                  )
                  .padding(2.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
              ) {
                PilotAvatarThumb(avatarSource = story.avatarBase64, callsign = story.authorCallsign, size = 56.dp)
              }
            }
            Spacer(Modifier.height(4.dp))
            Text(
              text = story.authorCallsign,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.width(62.dp),
            )
          }
        }
      }
    }
  }

  if (showAddStoryDialog) {
    var caption by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val storyPhotoPicker =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
          pickedBitmap = decodeSampledBitmap(ctx, uri)
        }
      }

    TacticalDialog(
      onDismissRequest = { showAddStoryDialog = false },
      title = "Publish Timeline Story",
      subtitle = "Live 24-Hour Firestore Trail Highlight",
      icon = Icons.Default.ElectricBolt,
      badgeText = "LIVE STORY",
      confirmButton = {
        TacticalButton(
          onClick = {
            onAddStory(caption.trim(), pickedBitmap, mediaUrl.trim())
            showAddStoryDialog = false
          },
          enabled = caption.isNotBlank(),
          modifier = Modifier.testTag("publish_story_confirm_button"),
        ) { Text("Post Story") }
      },
      dismissButton = {
        TacticalOutlinedButton(onClick = { showAddStoryDialog = false }) { Text("Cancel") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it },
          label = { Text("Story Caption / Trail Note") },
          colors = tacticalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("story_caption_input"),
        )
        TacticalOutlinedButton(
          onClick = {
            storyPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.fillMaxWidth().testTag("story_pick_photo_button"),
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (pickedBitmap != null) "Change Selected Photo" else "Pick Trail Photo (Photo Picker)")
        }
        pickedBitmap?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Selected story photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)),
          )
        }
        OutlinedTextField(
          value = mediaUrl,
          onValueChange = { mediaUrl = it },
          label = { Text("Or Image URL (Optional)") },
          colors = tacticalOutlinedTextFieldColors(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
fun StoryViewerDialog(
  story: TimelineStoryItem,
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
          .background(Color.Black),
    ) {
      if (story.mediaUrl.isNotBlank()) {
        CloudImage(
          source = story.mediaUrl,
          contentDescription = story.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(MaterialTheme.colorScheme.primaryContainer, Color.Black)
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Default.ElectricBolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        }
      }

      // Top Overlay Header
      Column(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .padding(
              start = safeInsets.start + 16.dp,
              end = safeInsets.end + 16.dp,
              top = safeInsets.top + 12.dp,
              bottom = 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            PilotAvatarThumb(avatarSource = story.avatarBase64, callsign = story.authorCallsign, size = 36.dp)
            Column {
              Text(story.authorCallsign, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
              Text("Live Story • 24h Active", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close Story", tint = Color.White)
          }
        }
      }

      // Bottom Caption Overlay
      Surface(
        color = Color.Black.copy(alpha = 0.74f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().widthIn(max = 700.dp),
      ) {
        Column(
          modifier = Modifier.padding(
            start = safeInsets.start + 16.dp,
            end = safeInsets.end + 16.dp,
            top = 14.dp,
            bottom = safeInsets.bottom + 16.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(story.caption, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
          if (story.batteryStatus.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.ElectricBolt, null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
              Text(story.batteryStatus, fontSize = 11.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
