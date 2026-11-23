package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PevStoreDirectory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Recognizable custom Compose/Canvas brand logo badges for major retailers
 * (Amazon, eBay, Walmart, AliExpress, Best Buy, Target, Home Depot, Google Shopping,
 * REI, RevZilla, Cycle Gear, Jenson USA, Harbor Freight) and specialty PEV stores.
 */
@Composable
fun StoreBrandLogo(
  storeId: String,
  storeName: String,
  size: Dp = 26.dp,
  modifier: Modifier = Modifier,
) {
  val id = storeId.lowercase()
  val shape = RoundedCornerShape(6.dp)
  when (id) {
    "amazon" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color(0xFF131921)).border(1.dp, Color(0xFFFF9900), shape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "a",
          color = Color.White,
          fontSize = (size.value * 0.56f).sp,
          fontWeight = FontWeight.Black,
          modifier = Modifier.padding(bottom = (size.value * 0.14f).dp),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
          val yBase = this.size.height * 0.72f
          val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.18f, yBase)
            quadraticBezierTo(
              this@Canvas.size.width * 0.50f,
              this@Canvas.size.height * 0.94f,
              this@Canvas.size.width * 0.82f,
              yBase,
            )
          }
          drawPath(path, Color(0xFFFF9900), style = Stroke(width = this.size.width * 0.09f, cap = StrokeCap.Round))
          // Arrow tip
          drawLine(
            color = Color(0xFFFF9900),
            start = Offset(this.size.width * 0.73f, this.size.height * 0.66f),
            end = Offset(this.size.width * 0.84f, yBase),
            strokeWidth = this.size.width * 0.08f,
            cap = StrokeCap.Round,
          )
        }
      }
    }

    "ebay" -> {
      Box(
        modifier = modifier.size(width = size * 1.48f, height = size).clip(shape).background(Color.White).border(1.dp, Color(0xFFD1D5DB), shape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFE53238), fontWeight = FontWeight.ExtraBold)) { append("e") }
            withStyle(SpanStyle(color = Color(0xFF0064D2), fontWeight = FontWeight.ExtraBold)) { append("b") }
            withStyle(SpanStyle(color = Color(0xFFF5AF02), fontWeight = FontWeight.ExtraBold)) { append("a") }
            withStyle(SpanStyle(color = Color(0xFF86B817), fontWeight = FontWeight.ExtraBold)) { append("y") }
          },
          fontSize = (size.value * 0.42f).sp,
          maxLines = 1,
        )
      }
    }

    "walmart" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color(0xFF0071DC)),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
          val cx = this.size.width / 2f
          val cy = this.size.height / 2f
          val innerR = this.size.minDimension * 0.19f
          val outerR = this.size.minDimension * 0.44f
          val strokeW = this.size.minDimension * 0.14f
          for (i in 0 until 6) {
            val rad = (i * 60.0 - 90.0) * PI / 180.0
            drawLine(
              color = Color(0xFFFFC220),
              start = Offset((cx + cos(rad) * innerR).toFloat(), (cy + sin(rad) * innerR).toFloat()),
              end = Offset((cx + cos(rad) * outerR).toFloat(), (cy + sin(rad) * outerR).toFloat()),
              strokeWidth = strokeW,
              cap = StrokeCap.Round,
            )
          }
        }
      }
    }

    "aliexpress" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color(0xFFFF4747)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "AE",
          color = Color(0xFFFFF5CC),
          fontSize = (size.value * 0.44f).sp,
          fontWeight = FontWeight.Black,
          maxLines = 1,
        )
      }
    }

    "bestbuy" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color(0xFF0046BE)),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier.size(size * 0.72f).clip(RoundedCornerShape(3.dp)).background(Color(0xFFFFF200)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "BB",
            color = Color(0xFF001E73),
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
          )
        }
      }
    }

    "target" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color.White).border(1.dp, Color(0xFFCC0000), shape),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
          val r = this.size.minDimension / 2f
          drawCircle(Color(0xFFCC0000), radius = r)
          drawCircle(Color.White, radius = r * 0.64f)
          drawCircle(Color(0xFFCC0000), radius = r * 0.32f)
        }
      }
    }

    "homedepot" -> {
      Box(
        modifier = modifier.size(size).clip(shape).background(Color(0xFFF96302)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "HD",
          color = Color.White,
          fontSize = (size.value * 0.44f).sp,
          fontWeight = FontWeight.Black,
          maxLines = 1,
        )
      }
    }

    "google_shop" -> {
      Box(
        modifier = modifier.size(size).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFF4285F4), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("G") }
          },
          fontSize = (size.value * 0.52f).sp,
          maxLines = 1,
        )
      }
    }

    "rei" -> StoreMonogramLogo("REI", Color(0xFF1F4E3D), Color.White, size, modifier)
    "revzilla" -> StoreMonogramLogo("RZ", Color(0xFFD9381E), Color.White, size, modifier)
    "cyclegear" -> StoreMonogramLogo("CG", Color(0xFF1E293B), Color(0xFFF59E0B), size, modifier)
    "jensonusa" -> StoreMonogramLogo("JU", Color(0xFF0F766E), Color.White, size, modifier)
    "harborfreight" -> StoreMonogramLogo("HF", Color(0xFFB91C1C), Color.White, size, modifier)
    "voro" -> StoreMonogramLogo("VM", Color(0xFF111827), Color(0xFF38BDF8), size, modifier)
    "fluid" -> StoreMonogramLogo("FF", Color(0xFF0369A1), Color.White, size, modifier)
    "alienrides" -> StoreMonogramLogo("AR", Color(0xFF14532D), Color(0xFF4ADE80), size, modifier)
    "revrides" -> StoreMonogramLogo("RR", Color(0xFF7C2D12), Color(0xFFFB923C), size, modifier)
    "ewheels" -> StoreMonogramLogo("eW", Color(0xFF312E81), Color(0xFFA5B4FC), size, modifier)
    "euco" -> StoreMonogramLogo("EU", Color(0xFF4C1D95), Color(0xFFE9D5FF), size, modifier)
    "floatlife" -> StoreMonogramLogo("TFL", Color(0xFF78350F), Color(0xFFFDE68A), size, modifier)
    "craftride" -> StoreMonogramLogo("CR", Color(0xFF18181B), Color(0xFFFACC15), size, modifier)
    "onewheel" -> StoreMonogramLogo("OW", Color(0xFF0F172A), Color(0xFF38BDF8), size, modifier)
    "lunacycle" -> StoreMonogramLogo("LC", Color(0xFF1E1B4B), Color(0xFFC084FC), size, modifier)
    "gritshift" -> StoreMonogramLogo("GS", Color(0xFF7F1D1D), Color(0xFFFCA5A5), size, modifier)
    "chargedcycle" -> StoreMonogramLogo("CC", Color(0xFF14532D), Color(0xFF86EFAC), size, modifier)
    "chibattery" -> StoreMonogramLogo("CHI", Color(0xFF1E3A8A), Color(0xFF93C5FD), size, modifier)
    "flipsky" -> StoreMonogramLogo("FS", Color(0xFF0F172A), Color(0xFF22D3EE), size, modifier)
    "spintend" -> StoreMonogramLogo("SP", Color(0xFF3F1D38), Color(0xFFF472B6), size, modifier)
    else -> {
      val initials =
        storeName
          .split(" ", "/", "-")
          .filter { it.isNotBlank() }
          .take(2)
          .joinToString("") { it.first().uppercase() }
          .ifBlank { "PEV" }
      val bgColors =
        listOf(
          Color(0xFF0F4C81),
          Color(0xFF065F46),
          Color(0xFF5B21B6),
          Color(0xFF9A3412),
          Color(0xFF1E293B),
          Color(0xFF831843),
        )
      val bg = bgColors[(id.hashCode() and 0x7FFFFFFF) % bgColors.size]
      StoreMonogramLogo(initials, bg, Color.White, size, modifier)
    }
  }
}

@Composable
private fun StoreMonogramLogo(
  text: String,
  bg: Color,
  fg: Color,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(6.dp)
  Box(
    modifier = modifier.size(size).clip(shape).background(bg).border(1.dp, fg.copy(alpha = 0.25f), shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text.take(3),
      color = fg,
      fontSize = if (text.length >= 3) (size.value * 0.30f).sp else (size.value * 0.40f).sp,
      fontWeight = FontWeight.Black,
      maxLines = 1,
    )
  }
}

/**
 * Compact 1-tap horizontal strip of major stores with logos (Amazon, eBay, Walmart,
 * AliExpress, Best Buy, Target, Home Depot, Google Shopping) attached right inside
 * Spec Engine and Parts Finder result cards.
 */
@Composable
fun QuickStoreLogoStrip(
  query: String,
  modifier: Modifier = Modifier,
) {
  val ctx = LocalContext.current
  val featuredIds = listOf("amazon", "ebay", "walmart", "aliexpress", "bestbuy", "target", "homedepot", "google_shop")
  val stores = PevStoreDirectory.KNOWN_PEV_STORES.filter { it.id in featuredIds }

  LazyRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    contentPadding = PaddingValues(vertical = 2.dp),
  ) {
    items(stores, key = { it.id }) { store ->
      val shortLabel =
        when (store.id) {
          "amazon" -> "Amazon"
          "ebay" -> "eBay"
          "walmart" -> "Walmart"
          "aliexpress" -> "AliExpress"
          "bestbuy" -> "Best Buy"
          "target" -> "Target"
          "homedepot" -> "Home Depot"
          "google_shop" -> "Google"
          else -> store.name
        }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        modifier =
          Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable {
              runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(store.buildSearchUrl(query))))
              }
            }
            .testTag("quick_store_${store.id}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          StoreBrandLogo(storeId = store.id, storeName = store.name, size = 20.dp)
          Spacer(Modifier.width(5.dp))
          Text(
            text = shortLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

/**
 * Live official PEV brand logo badge used on PEV Profiles, Cockpit Garage, and Spec Engine cards.
 * Auto-fetches and caches the brand's official logo (Base64 PNG/JPEG) on demand via LiveCloudServices.
 */
@Composable
fun PevBrandLogoBadge(
  brandOrModel: String,
  logoDataOrUrl: String = "",
  size: Dp = 28.dp,
  showBrandLabel: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val cleanInput = brandOrModel.trim()
  val resolvedBrand = androidx.compose.runtime.remember(cleanInput) {
    com.example.core.LiveCloudServices.resolvePevBrandInfo(cleanInput).first.ifBlank { "PEV" }
  }
  var fetchedLogo by androidx.compose.runtime.remember(cleanInput, logoDataOrUrl) {
    androidx.compose.runtime.mutableStateOf(
      logoDataOrUrl.ifBlank { com.example.core.LiveCloudServices.cachedPevBrandLogo(cleanInput) }
    )
  }

  androidx.compose.runtime.LaunchedEffect(cleanInput, logoDataOrUrl) {
    if (logoDataOrUrl.isNotBlank()) {
      fetchedLogo = logoDataOrUrl
    } else if (fetchedLogo.isBlank() && cleanInput.length >= 2) {
      val b64 = com.example.core.LiveCloudServices.fetchPevBrandLogoBase64(cleanInput)
      if (b64.isNotBlank()) fetchedLogo = b64
    }
  }

  val shape = RoundedCornerShape(7.dp)
  val initials =
    resolvedBrand
      .split(Regex("[\\s\\-]+"))
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "EV" }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Surface(
      color = Color.White,
      shape = shape,
      shadowElevation = 2.dp,
      modifier =
        Modifier
          .size(size)
          .clip(shape)
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape),
    ) {
      Box(
        modifier = Modifier.fillMaxSize().padding(2.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (fetchedLogo.isNotBlank()) {
          CloudImage(
            source = fetchedLogo,
            contentDescription = "$resolvedBrand brand logo",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = initials,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontSize = (size.value * 0.38f).sp,
              fontWeight = FontWeight.Black,
              maxLines = 1,
            )
          }
        }
      }
    }
    if (showBrandLabel) {
      Text(
        text = resolvedBrand,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
    }
  }
}

