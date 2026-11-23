package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

/**
 * Rich visual product image banner for Spec Engine and Parts Finder cards.
 * Loads a live product image (HTTP URL or Base64 data URI) when available, and renders a crisp,
 * category-colored PEV technical blueprint card as an instant zero-flicker visual.
 */
@Composable
fun PevProductVisualBanner(
  title: String,
  subtitle: String,
  category: String,
  badgeLeft: String,
  badgeRight: String,
  imageUrl: String = "",
  brandName: String = "",
  modifier: Modifier = Modifier,
) {
  val palette =
    when (category.uppercase()) {
      "EBIKE", "EFATBIKE", "EFOLDING" -> listOf(Color(0xFF0A2E2A), Color(0xFF0F5E4D), Color(0xFF10B981))
      "ECARGO", "ETRIKE" -> listOf(Color(0xFF122C24), Color(0xFF15803D), Color(0xFF4ADE80))
      "SCOOTER" -> listOf(Color(0xFF0A1F38), Color(0xFF0E4B78), Color(0xFF06B6D4))
      "EUC" -> listOf(Color(0xFF23103D), Color(0xFF5B21B6), Color(0xFFA855F7))
      "ONEWHEEL" -> listOf(Color(0xFF2D1B09), Color(0xFF92400E), Color(0xFFF59E0B))
      "EMOTO", "SURRON", "EDIRTBIKE" -> listOf(Color(0xFF330C18), Color(0xFF9F1239), Color(0xFFF43F5E))
      "EMOPED" -> listOf(Color(0xFF2A1424), Color(0xFF831843), Color(0xFFEC4899))
      "ESKATE" -> listOf(Color(0xFF102438), Color(0xFF1D4ED8), Color(0xFF38BDF8))
      "EGOKART", "EATV" -> listOf(Color(0xFF2C1808), Color(0xFFB45309), Color(0xFFFB923C))
      else -> listOf(Color(0xFF111827), Color(0xFF1F3A52), Color(0xFF39FF14))
    }

  val decodedBase64 =
    remember(imageUrl) {
      if (imageUrl.startsWith("data:image/")) {
        runCatching {
          val raw = imageUrl.substringAfter("base64,", "")
          if (raw.isBlank()) null
          else {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
          }
        }.getOrNull()
      } else null
    }

  val shape = RoundedCornerShape(10.dp)
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(112.dp)
        .clip(shape)
        .background(Brush.linearGradient(listOf(palette[0], palette[1])))
        .border(1.dp, palette[2].copy(alpha = 0.55f), shape),
  ) {
    // Blueprint grid & radar glow behind everything
    Canvas(modifier = Modifier.fillMaxSize()) {
      val step = 18.dp.toPx()
      var x = 0f
      while (x < size.width) {
        drawLine(Color.White.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
      }
      var y = 0f
      while (y < size.height) {
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1f)
        y += step
      }
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(palette[2].copy(alpha = 0.34f), Color.Transparent),
            center = Offset(size.width * 0.82f, size.height * 0.48f),
            radius = size.height * 0.85f,
          ),
      )
    }

    if (decodedBase64 != null) {
      Image(
        bitmap = decodedBase64,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.72f))
              )
            ),
      )
    } else if (imageUrl.startsWith("http")) {
      SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        error = { PevBlueprintSilhouetteOverlay(category, palette[2]) },
        loading = { PevBlueprintSilhouetteOverlay(category, palette[2]) },
      )
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.72f))
              )
            ),
      )
    } else {
      PevBlueprintSilhouetteOverlay(category, palette[2])
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color.Black.copy(alpha = 0.58f),
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = badgeLeft,
            color = palette[2],
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
          )
        }
        Surface(
          color = palette[2].copy(alpha = 0.90f),
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = badgeRight,
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (brandName.isNotBlank() || title.isNotBlank()) {
          PevBrandLogoBadge(
            brandOrModel = brandName.ifBlank { title },
            size = 28.dp,
          )
        }
        Column(Modifier.weight(1f)) {
          Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(1.dp))
          Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun PevBlueprintSilhouetteOverlay(category: String, accent: Color) {
  Box(
    modifier = Modifier.fillMaxSize().padding(end = 14.dp),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Icon(
      imageVector = pevCategoryIcon(category),
      contentDescription = null,
      tint = accent.copy(alpha = 0.32f),
      modifier = Modifier.size(72.dp),
    )
  }
}
