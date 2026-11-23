package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable PEV visual hero graphic banner with full-bleed artwork, atmospheric readability scrim,
 * live status pulse badge, title/subtitle, and compact telemetry or feature pills.
 */
@Composable
fun PevScreenHeroBanner(
  @DrawableRes imageRes: Int,
  badgeLabel: String,
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  metrics: List<Pair<String, String>> = emptyList(),
  height: Dp = 128.dp,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth().height(height),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    color = Color(0xFF0A0F14),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = painterResource(id = imageRes),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors =
                  listOf(
                    Color(0xE8070B10),
                    Color(0xB8091017),
                    Color(0x55091017),
                  )
              )
            )
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xCC06090D))
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = accentColor.copy(alpha = 0.22f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
              Text(
                badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                fontSize = 9.sp,
              )
            }
          }
          Surface(
            color = Color(0x99060A0E),
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.padding(6.dp).size(16.dp),
            )
          }
        }

        Column {
          Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD6E2EB),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (metrics.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              metrics.take(3).forEach { (k, v) ->
                Surface(
                  color = Color(0xAA0C131B),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      "$k ",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFF9CB0C0),
                      fontSize = 9.sp,
                    )
                    Text(
                      v,
                      style = MaterialTheme.typography.labelSmall,
                      color = accentColor,
                      fontWeight = FontWeight.Black,
                      fontSize = 10.sp,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
