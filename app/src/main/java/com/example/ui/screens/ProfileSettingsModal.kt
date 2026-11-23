package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RiderProfile
import com.example.ui.components.PilotAvatarThumb

@Composable
fun ProfileSettingsModal(
  profile: RiderProfile,
  onSave: (callsign: String, bio: String, avatar: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var callsign by remember { mutableStateOf(profile.callsign) }
  var bio by remember { mutableStateOf(profile.medicalProfile) }
  var avatar by remember { mutableStateOf(profile.avatarBase64) }

  val presetAvatars = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&auto=format&fit=crop&q=80"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
      modifier = Modifier.fillMaxWidth(0.925f).padding(vertical = 24.dp),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            Text("PILOT PROFILE & CUSTOMIZATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
          }
          IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Avatar Preview & Presets
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier =
              Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            PilotAvatarThumb(avatarSource = avatar, callsign = callsign, size = 84.dp)
          }
          Spacer(Modifier.height(8.dp))
          Text("Choose Preset Avatar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
          Spacer(Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetAvatars) { preset ->
              Box(
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (avatar == preset) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .clickable { avatar = preset },
              ) {
                PilotAvatarThumb(avatarSource = preset, callsign = "Preset", size = 44.dp)
              }
            }
          }
        }

        OutlinedTextField(
          value = callsign,
          onValueChange = { callsign = it },
          label = { Text("Pilot Callsign") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text("Pilot Bio / Medical Note") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = avatar,
          onValueChange = { avatar = it },
          label = { Text("Avatar Image URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              onSave(callsign, bio, avatar)
              onDismiss()
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save Profile")
          }
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
      }
    }
  }
}
