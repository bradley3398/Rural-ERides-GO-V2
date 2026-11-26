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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TacticalCard
import com.example.ui.screens.AAA_PEV_MODELS
import com.example.ui.screens.Pev3dGarageViewer

/**
 * Dedicated Compartmentalized 3D Lab Full-Screen Overlay.
 * Renders directly in the main Activity window (zero Dialog window surface conflicts)
 * with Studio 3D PBR, OpenGL ES PBR, and WebGL 2.0 GLB engines for 88 PEV rigs.
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
  val safeVoltage = if (activeVoltage.isFinite() && activeVoltage > 0.0) activeVoltage else 72.0

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF070B0E))
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
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "3D CAD & DYNO LAB",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Studio 3D PBR • OpenGL ES • WebGL 2.0 • $activeVehicleName (${safeVoltage.toInt()}V)",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFCBD5E1),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        OutlinedButton(
          onClick = onDismiss,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("exit_3d_lab_btn"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close 3D Lab", modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Exit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
      }

      HorizontalDivider(thickness = 1.dp, color = Color(0xFFA855F7).copy(alpha = 0.4f))

      // 3D VIEWER CONTAINER
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(Color.Black),
      ) {
        Pev3dGarageViewer(
          activeVehicleName = activeVehicleName,
          activeVoltage = safeVoltage,
          gpsLat = gpsLat,
          gpsLng = gpsLng,
        )
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
            text = "${AAA_PEV_MODELS.size} realistic PEV rigs on crash-proof Studio 3D PBR (ACES + Rim), OpenGL ES PBR, and WebGL 2.0 GLB with FLIR Thermal, Gemini Chief AI & Live Dyno Air.",
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
        val safeCardVoltage = if (activeVoltage.isFinite() && activeVoltage > 0.0) activeVoltage else 72.0
        Text(
          "Paired Rig: $activeVehicleName (${safeCardVoltage.toInt()}V)",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFA855F7),
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("3 Engines • 3 Live Services", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, softWrap = false)
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
