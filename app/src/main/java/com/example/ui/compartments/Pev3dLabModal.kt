package com.example.ui.compartments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.TacticalCard
import com.example.ui.screens.AAA_PEV_MODELS
import com.example.ui.screens.Pev3dGarageViewer

/**
 * Dedicated Compartmentalized 3D Lab Pop-up Modal.
 * Opens as a full-screen, hardware-accelerated dialog window isolated from background screens.
 * Contains OpenGL PBR, WebGL 2.0 GLB, and Compose 3D Shaded Mesh engines for 58 PEV rigs.
 */
@Composable
fun Pev3dLabModal(
  activeVehicleName: String = "Hyper-PEV Flagship",
  activeVoltage: Double = 74.0,
  gpsLat: Double = 0.0,
  gpsLng: Double = 0.0,
  onDismiss: () -> Unit,
) {
  BackHandler { onDismiss() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
    ),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      color = Color(0xFF070B0E),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // TOP MODAL COMMAND BAR
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                listOf(
                  Color(0xFFA855F7).copy(alpha = 0.28f),
                  Color(0xFF101720),
                )
              )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFA855F7).copy(alpha = 0.22f))
                .border(1.dp, Color(0xFFA855F7), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = null,
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(20.dp),
              )
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "3D CAD & DYNO LAB",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                )
                Text(
                  text = "MODAL POP-UP",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFA855F7),
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFA855F7).copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                )
              }
              Text(
                text = "OpenGL PBR • WebGL 2.0 GLB • Compose 3D • $activeVehicleName (${activeVoltage.toInt()}V)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onDismiss,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("exit_3d_lab_btn"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close 3D Lab", modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Exit 3D Lab", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
          }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFA855F7).copy(alpha = 0.4f))

        // 3D VIEWER CONTAINER (Fills dialog viewport)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color.Black),
        ) {
          Pev3dGarageViewer(
            activeVehicleName = activeVehicleName,
            activeVoltage = activeVoltage,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
          )
        }
      }
    }
  }
}

/**
 * Preview and quick launchpad card for the 3D Lab.
 * Placed in Creator Hub, Cockpit, or Mechanic to compartmentalize 3D Lab entry into a clean pop-up.
 */
@Composable
fun Pev3dLabLaunchpadCard(
  activeVehicleName: String = "Rural Hyper-PEV",
  activeVoltage: Double = 72.0,
  onLaunchPopUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TacticalCard(
    highlight = true,
    title = "3D CAD & DYNO LAB (POP-UP)",
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFA855F7).copy(alpha = 0.18f))
            .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.ViewInAr,
            contentDescription = null,
            tint = Color(0xFFA855F7),
            modifier = Modifier.size(30.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Multi-Engine 3D Hardware CAD & Dyno Lab",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${AAA_PEV_MODELS.size} PEV rigs across OpenGL PBR, WebGL 2.0 GLB & Compose 3D with Normal Maps, Studio IBL, FLIR Thermal, Gemini Chief AI & Live Dyno Air.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF121820))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Paired Rig: $activeVehicleName (${activeVoltage.toInt()}V)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
        Text("3 Engines • 3 Cloud Services", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
      }

      Button(
        onClick = onLaunchPopUp,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("launch_3d_lab_modal_btn"),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFA855F7),
          contentColor = Color.White,
        ),
        shape = RoundedCornerShape(10.dp),
      ) {
        Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          "LAUNCH 3D LAB POP-UP",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.5.sp,
        )
      }
    }
  }
}
