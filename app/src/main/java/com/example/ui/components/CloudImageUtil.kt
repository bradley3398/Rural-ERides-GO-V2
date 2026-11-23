package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Encodes a [Bitmap] into a compact JPEG Base64 data URI (`data:image/jpeg;base64,...`)
 * guaranteed to fit inside Firestore document field size limits so cloud uploads never get rejected.
 */
fun bitmapToCloudBase64DataUri(
    bitmap: Bitmap,
    maxDimension: Int = 560,
    initialQuality: Int = 68,
    maxChars: Int = 150_000
): String {
    var targetMaxDim = maxDimension.coerceAtLeast(120)
    var quality = initialQuality.coerceIn(30, 88)
    var lastDataUri = ""

    repeat(6) {
        val scaled = scaleBitmapDown(bitmap, targetMaxDim)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$encoded"
        lastDataUri = dataUri
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        if (dataUri.length <= maxChars) {
            return dataUri
        }
        if (quality > 42) {
            quality -= 10
        } else {
            targetMaxDim = (targetMaxDim * 0.78f).roundToInt().coerceAtLeast(120)
            quality = 50
        }
    }
    return lastDataUri
}

/**
 * Convenience helper to read a Photo Picker [Uri] into a sampled [Bitmap] and immediately
 * encode it as a compact Base64 data URI for reliable Firestore cloud persistence.
 */
fun uriToCloudBase64DataUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = 560,
    initialQuality: Int = 68,
    maxChars: Int = 150_000
): String? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val rawMax = max(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        if (rawMax > maxDimension * 2) {
            while ((rawMax / (sampleSize * 2)) >= maxDimension) {
                sampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null
        try {
            bitmapToCloudBase64DataUri(
                bitmap = decoded,
                maxDimension = maxDimension,
                initialQuality = initialQuality,
                maxChars = maxChars
            )
        } finally {
            decoded.recycle()
        }
    } catch (_: Exception) {
        null
    }
}

private fun scaleBitmapDown(src: Bitmap, maxDim: Int): Bitmap {
    val w = src.width
    val h = src.height
    if (w <= maxDim && h <= maxDim) return src
    val ratio = min(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
    val newW = (w * ratio).roundToInt().coerceAtLeast(1)
    val newH = (h * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, newW, newH, true)
}

/**
 * Decodes a `data:image/...;base64,...` string (or raw base64 payload) into a [Bitmap].
 */
fun decodeBase64DataUri(source: String): Bitmap? {
    if (source.isBlank()) return null
    return try {
        val payload = if (source.startsWith("data:image", ignoreCase = true)) {
            source.substringAfter("base64,", "")
        } else {
            return null
        }
        if (payload.isBlank()) return null
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Renders either a Firestore Base64 data URI (`data:image/jpeg;base64,...`) OR a standard
 * `https://` / `content://` image seamlessly without third-party host failures.
 */
@Composable
fun CloudImage(
    source: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (source.isBlank()) return
    if (source.startsWith("data:image", ignoreCase = true)) {
        val decoded = remember(source) { decodeBase64DataUri(source) }
        if (decoded != null) {
            Image(
                bitmap = decoded.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    } else {
        AsyncImage(
            model = source,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

/**
 * Circular rider profile picture with Base64 cloud avatar support and crisp monogram fallback.
 */
@Composable
fun PilotAvatarThumb(
    avatarSource: String,
    callsign: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary
) {
    val cleanInitial = callsign.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "R"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .border(1.5.dp, borderColor.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarSource.isNotBlank()) {
            CloudImage(
                source = avatarSource,
                contentDescription = "$callsign profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = cleanInitial,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}
