package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.MasterSettings
import com.example.data.RideSession
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.speedUnitLabel
import java.io.File
import java.io.FileOutputStream

object MissionCardShareUtil {

  fun parsePolylinePoints(rawPoints: List<String>): List<Pair<Double, Double>> =
    rawPoints.mapNotNull { entry ->
      val parts = entry.split(",")
      if (parts.size < 2) return@mapNotNull null
      val lat = parts[0].trim().toDoubleOrNull() ?: return@mapNotNull null
      val lng = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
      if (lat == 0.0 && lng == 0.0) null else (lat to lng)
    }

  fun renderMissionCardBitmap(
    ride: RideSession,
    settings: MasterSettings,
  ): Bitmap {
    val width = 1080
    val height = 620
    val isClassic = isWindowsClassicActive(settings)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val distUnit = distUnitLabel(settings.distanceUnit)
    val spdUnit = speedUnitLabel(settings.distanceUnit)
    val dispDist = formatDistValue(ride.distanceKm, settings.distanceUnit)
    val dispMax = formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit)
    val whPerDist =
      if (dispDist > 0.01 && ride.whUsed > 0.0) "${"%.1f".format(ride.whUsed / dispDist)} Wh/$distUnit"
      else if (ride.distanceKm > 0.05) {
        val estWh = ride.distanceKm * 24.0
        "${"%.1f".format(estWh / dispDist.coerceAtLeast(0.05))} Wh/$distUnit"
      } else "-- Wh/$distUnit"
    val durationFormatted =
      if (ride.durationSec >= 3600L) "${ride.durationSec / 3600}h ${(ride.durationSec % 3600) / 60}m"
      else "${ride.durationSec / 60}m ${ride.durationSec % 60}s"

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val accentColor: Int
    val inkColor: Int
    val mutedColor: Int
    val panelColor: Int

    if (isClassic) {
      // Win95 Teal desktop backdrop + Classic Silver Window Chrome
      canvas.drawColor(0xFF008080.toInt())
      val winRect = RectF(26f, 26f, width - 26f, height - 26f)
      bgPaint.color = 0xFFC0C0C0.toInt()
      canvas.drawRect(winRect, bgPaint)

      // 3D Raised Outer Bevel
      val bevelLight = Paint().apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 4f }
      val bevelDark = Paint().apply { color = 0xFF404040.toInt(); strokeWidth = 4f }
      canvas.drawLine(winRect.left, winRect.top, winRect.right, winRect.top, bevelLight)
      canvas.drawLine(winRect.left, winRect.top, winRect.left, winRect.bottom, bevelLight)
      canvas.drawLine(winRect.left, winRect.bottom, winRect.right, winRect.bottom, bevelDark)
      canvas.drawLine(winRect.right, winRect.top, winRect.right, winRect.bottom, bevelDark)

      // Win95 Navy -> Blue Title Bar
      val titleRect = RectF(winRect.left + 8f, winRect.top + 8f, winRect.right - 8f, winRect.top + 60f)
      val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          titleRect.left,
          0f,
          titleRect.right,
          0f,
          0xFF000080.toInt(),
          0xFF1084D0.toInt(),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(titleRect, titlePaint)

      val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 27f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
      canvas.drawText("RURAL ERIDES GO • RIDE SUMMARY", titleRect.left + 18f, titleRect.centerY() + 10f, titleTextPaint)

      // Classic _, □, × control boxes
      val ctrlLabels = listOf("_", "□", "×")
      ctrlLabels.forEachIndexed { idx, glyph ->
        val bx = titleRect.right - ((3 - idx) * 42f) - 10f
        val box = RectF(bx, titleRect.top + 8f, bx + 34f, titleRect.bottom - 8f)
        bgPaint.color = 0xFFC0C0C0.toInt()
        canvas.drawRect(box, bgPaint)
        canvas.drawLine(box.left, box.top, box.right, box.top, bevelLight)
        canvas.drawLine(box.left, box.top, box.left, box.bottom, bevelLight)
        canvas.drawLine(box.left, box.bottom, box.right, box.bottom, bevelDark)
        canvas.drawLine(box.right, box.top, box.right, box.bottom, bevelDark)
        val gPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = 0xFF000000.toInt()
          textSize = 22f
          typeface = Typeface.DEFAULT_BOLD
          textAlign = Paint.Align.CENTER
        }
        canvas.drawText(glyph, box.centerX(), box.centerY() + 8f, gPaint)
      }

      accentColor = 0xFF000080.toInt()
      inkColor = 0xFF000000.toInt()
      mutedColor = 0xFF333333.toInt()
      panelColor = 0xFFFFFFFF.toInt()
    } else {
      // High-Contrast Modern Dark Tactical Mission Card
      canvas.drawColor(0xFF06090F.toInt())
      val cardRect = RectF(24f, 24f, width - 24f, height - 24f)
      bgPaint.color = 0xFF0D131F.toInt()
      canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFF39FF14.toInt()
      }
      canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

      val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt()
        textSize = 24f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
      canvas.drawText("RURAL ERIDES GO • TELEMETRY LOG", 60f, 74f, brandPaint)

      accentColor = 0xFF39FF14.toInt()
      inkColor = 0xFFF8FAFC.toInt()
      mutedColor = 0xFF94A3B8.toInt()
      panelColor = 0xFF141D2E.toInt()
    }

    // Header: Ride Title & Vehicle Name
    val topY = if (isClassic) 130f else 124f
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = inkColor
      textSize = 40f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(ride.title.ifBlank { "Trail Mission" }.take(28), 60f, topY, headerPaint)

    val vehiclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (isClassic) 0xFF000080.toInt() else 0xFF06B6D4.toInt()
      textSize = 24f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText("RIG: ${ride.vehicleName.ifBlank { "PEV Unit" }.take(32)}", 60f, topY + 38f, vehiclePaint)

    // Route Polyline Preview Box (Right Side)
    val mapBox = RectF(610f, topY + 60f, width - 60f, height - 64f)
    val mapFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (isClassic) 0xFF08141E.toInt() else panelColor
    }
    if (isClassic) {
      canvas.drawRect(mapBox, mapFillPaint)
      val darkLine = Paint().apply { color = 0xFF404040.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
      canvas.drawRect(mapBox, darkLine)
    } else {
      canvas.drawRoundRect(mapBox, 18f, 18f, mapFillPaint)
      val mapStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF233048.toInt()
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
      }
      canvas.drawRoundRect(mapBox, 18f, 18f, mapStroke)
    }

    // Subtle Grid inside Route Map Preview
    val gridPaint = Paint().apply {
      color = 0xFF1E2D3D.toInt()
      strokeWidth = 1.5f
    }
    for (gx in 1..4) {
      val x = mapBox.left + (mapBox.width() * gx / 5f)
      canvas.drawLine(x, mapBox.top, x, mapBox.bottom, gridPaint)
    }
    for (gy in 1..3) {
      val y = mapBox.top + (mapBox.height() * gy / 4f)
      canvas.drawLine(mapBox.left, y, mapBox.right, y, gridPaint)
    }

    val routePts = parsePolylinePoints(ride.polylinePoints)
    val pad = 34f
    val innerW = (mapBox.width() - pad * 2).coerceAtLeast(10f)
    val innerH = (mapBox.height() - pad * 2).coerceAtLeast(10f)

    if (routePts.size >= 2) {
      val minLat = routePts.minOf { it.first }
      val maxLat = routePts.maxOf { it.first }
      val minLng = routePts.minOf { it.second }
      val maxLng = routePts.maxOf { it.second }
      val latSpan = (maxLat - minLat).coerceAtLeast(0.0002)
      val lngSpan = (maxLng - minLng).coerceAtLeast(0.0002)

      val screenPts =
        routePts.map { (lat, lng) ->
          val x = mapBox.left + pad + (((lng - minLng) / lngSpan) * innerW).toFloat()
          val y = mapBox.bottom - pad - (((lat - minLat) / latSpan) * innerH).toFloat()
          x to y
        }

      val path = Path().apply {
        moveTo(screenPts.first().first, screenPts.first().second)
        for (i in 1 until screenPts.size) {
          lineTo(screenPts[i].first, screenPts[i].second)
        }
      }
      val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0x5539FF14.toInt()
      }
      val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF39FF14.toInt()
      }
      canvas.drawPath(path, glowPaint)
      canvas.drawPath(path, linePaint)

      // Start (emerald) & End (crimson) markers
      val startDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt() }
      val endDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF43F5E.toInt() }
      canvas.drawCircle(screenPts.first().first, screenPts.first().second, 8f, startDot)
      canvas.drawCircle(screenPts.last().first, screenPts.last().second, 8f, endDot)
    } else {
      val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
      canvas.drawText("GPS ROUTE VECTOR", mapBox.centerX(), mapBox.centerY() - 8f, emptyPaint)
      canvas.drawText("(${routePts.size} FIXES)", mapBox.centerX(), mapBox.centerY() + 20f, emptyPaint)
    }

    val mapCaption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFFE2E8F0.toInt()
      textSize = 17f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText("ROUTE POLYLINE (${routePts.size} PTS)", mapBox.left + 14f, mapBox.top + 26f, mapCaption)

    // Left 2x2 Stat Grid (Top Speed, Distance, Duration, Wh/mi or Wh/km)
    val stats = listOf(
      "TOP SPEED" to "${"%.1f".format(dispMax)} $spdUnit",
      "DISTANCE" to "${"%.2f".format(dispDist)} $distUnit",
      "DURATION" to durationFormatted,
      "EFFICIENCY" to whPerDist,
    )
    val boxW = 245f
    val boxH = 148f
    val gap = 18f
    val startX = 60f
    val startY = topY + 60f

    stats.forEachIndexed { idx, (label, value) ->
      val col = idx % 2
      val row = idx / 2
      val left = startX + col * (boxW + gap)
      val top = startY + row * (boxH + gap)
      val r = RectF(left, top, left + boxW, top + boxH)
      val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = panelColor }
      if (isClassic) {
        canvas.drawRect(r, cellPaint)
        val dark = Paint().apply { color = 0xFF808080.toInt(); strokeWidth = 2.5f }
        val light = Paint().apply { color = 0xFFDFDFDF.toInt(); strokeWidth = 2.5f }
        canvas.drawLine(r.left, r.top, r.right, r.top, dark)
        canvas.drawLine(r.left, r.top, r.left, r.bottom, dark)
        canvas.drawLine(r.left, r.bottom, r.right, r.bottom, light)
        canvas.drawLine(r.right, r.top, r.right, r.bottom, light)
      } else {
        canvas.drawRoundRect(r, 16f, 16f, cellPaint)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 2f
          color = 0xFF233048.toInt()
        }
        canvas.drawRoundRect(r, 16f, 16f, stroke)
      }

      val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mutedColor
        textSize = 18f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      }
      val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 28f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      }
      canvas.drawText(label, r.left + 20f, r.top + 42f, lblPaint)
      canvas.drawText(value, r.left + 20f, r.top + 104f, valPaint)
    }

    // Footer metadata line
    val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = mutedColor
      textSize = 18f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    val elevM = ride.elevationGainM.toInt()
    canvas.drawText("ELEVATION +${elevM}m • WEATHER: ${ride.weatherSummary.ifBlank { "LIVE TELEMETRY" }.take(34)}", 60f, height - 48f, footPaint)

    return bmp
  }

  fun saveMissionCardToCache(
    context: Context,
    ride: RideSession,
    settings: MasterSettings,
  ): Result<File> =
    runCatching {
      val dir = File(context.cacheDir, "mission_cards")
      if (!dir.exists()) dir.mkdirs()
      val safeId = ride.id.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "mission" }
      val file = File(dir, "card_${safeId}_${System.currentTimeMillis()}.png")
      val bitmap = renderMissionCardBitmap(ride, settings)
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
      }
      bitmap.recycle()
      file
    }

  fun shareMissionCard(
    context: Context,
    ride: RideSession,
    settings: MasterSettings,
  ): Result<Uri> =
    runCatching {
      val file = saveMissionCardToCache(context, ride, settings).getOrThrow()
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(
            Intent.EXTRA_SUBJECT,
            "Rural ERides GO Mission: ${ride.title.ifBlank { "Trail Mission" }}",
          )
          putExtra(
            Intent.EXTRA_TEXT,
            "${ride.title.ifBlank { "Trail Mission" }} on ${ride.vehicleName.ifBlank { "PEV" }} • ${"%.2f".format(formatDistValue(ride.distanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} @ Top ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}",
          )
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      val chooser = Intent.createChooser(intent, "Share Mission Card").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      uri
    }
}
