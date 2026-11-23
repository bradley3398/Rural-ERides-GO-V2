package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricMoped
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTacticalSettings

/**
 * Curated & bespoke custom vector icon catalog for Rural ERides GO.
 * Includes custom-crafted high-precision ImageVectors for main navigation tabs,
 * tactical header actions, and Rural/PEV telemetry badges.
 */
object RuralPevIcons {

  // ============================================================================
  // BESPOKE CUSTOM VECTOR ICONS (HIGH-CRAFT NAVIGATION & HEADER SILHOUETTES)
  // ============================================================================

  /** Custom Speedometer Arc + Needle + Electric Bolt Trailhead Emblem */
  val NavTrailheadGauge: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavTrailheadGauge",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(4.2f, 17.8f)
        arcTo(9.2f, 9.2f, 0f, true, true, 19.8f, 17.8f)
      }
      // Precision ticks
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 4.2f)
        lineTo(12f, 6.4f)
        moveTo(5.8f, 9.6f)
        lineTo(7.4f, 10.6f)
        moveTo(18.2f, 9.6f)
        lineTo(16.6f, 10.6f)
      }
      // Speed needle pointing briskly right-up
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.1f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 14.2f)
        lineTo(16.8f, 9.5f)
      }
      // Center hub + live lightning base accent
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 12.6f)
        arcTo(1.7f, 1.7f, 0f, true, true, 11.99f, 12.6f)
        close()
      }
      path(fill = SolidColor(Color.Black)) {
        moveTo(12.6f, 16.4f)
        lineTo(9.4f, 20.1f)
        lineTo(11.7f, 20.1f)
        lineTo(11.2f, 22.8f)
        lineTo(14.7f, 18.8f)
        lineTo(12.3f, 18.8f)
        close()
      }
    }.build()
  }

  /** Custom Tactical Radar Sweep + Dual Rings + Convoy Target Ping */
  val NavRadarDish: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavRadarDish",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 2.5f)
        arcTo(9.5f, 9.5f, 0f, true, true, 11.99f, 2.5f)
      }
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 6.5f)
        arcTo(5.5f, 5.5f, 0f, true, true, 11.99f, 6.5f)
      }
      // Sweep beam
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.1f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 12f)
        lineTo(18.6f, 5.4f)
      }
      // Center + live blip
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 10.4f)
        arcTo(1.6f, 1.6f, 0f, true, true, 11.99f, 10.4f)
        close()
        moveTo(16.6f, 13.6f)
        arcTo(1.5f, 1.5f, 0f, true, true, 16.59f, 13.6f)
        close()
      }
    }.build()
  }

  /** Custom Barn Mechanic Wrench + High-Voltage Core */
  val NavBarnMechanic: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavBarnMechanic",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      // Barn roof silhouette framing wrench
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(3f, 10.2f)
        lineTo(12f, 3.2f)
        lineTo(21f, 10.2f)
      }
      // Precision diagonal wrench + bolt
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(7.4f, 19.6f)
        lineTo(14.8f, 12.2f)
      }
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(13.6f, 10.1f)
        arcTo(3.2f, 3.2f, 0f, true, true, 17.9f, 14.4f)
        lineTo(16.0f, 12.5f)
        lineTo(14.5f, 14.0f)
        lineTo(16.4f, 15.9f)
      }
      path(fill = SolidColor(Color.Black)) {
        moveTo(9.2f, 10.2f)
        lineTo(6.6f, 13.6f)
        lineTo(8.5f, 13.6f)
        lineTo(7.9f, 16.4f)
        lineTo(10.8f, 12.7f)
        lineTo(8.9f, 12.7f)
        close()
      }
    }.build()
  }

  /** Custom Co-Pilot Tactical Diamond Star + Orbit Ring */
  val NavCopilotCore: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavCopilotCore",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2.2f)
        lineTo(14.5f, 9.5f)
        lineTo(21.8f, 12f)
        lineTo(14.5f, 14.5f)
        lineTo(12f, 21.8f)
        lineTo(9.5f, 14.5f)
        lineTo(2.2f, 12f)
        lineTo(9.5f, 9.5f)
        close()
      }
      path(fill = SolidColor(Color.Black)) {
        moveTo(18.8f, 3.2f)
        lineTo(19.6f, 5.2f)
        lineTo(21.6f, 6.0f)
        lineTo(19.6f, 6.8f)
        lineTo(18.8f, 8.8f)
        lineTo(18.0f, 6.8f)
        lineTo(16.0f, 6.0f)
        lineTo(18.0f, 5.2f)
        close()
      }
    }.build()
  }

  /** Custom Channel Broadcast Frame + Crisp Play Triangle + Signal Arcs */
  val NavChannelBroadcast: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavChannelBroadcast",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(4.2f, 5.8f)
        lineTo(19.8f, 5.8f)
        arcTo(2f, 2f, 0f, false, true, 21.8f, 7.8f)
        lineTo(21.8f, 16.2f)
        arcTo(2f, 2f, 0f, false, true, 19.8f, 18.2f)
        lineTo(4.2f, 18.2f)
        arcTo(2f, 2f, 0f, false, true, 2.2f, 16.2f)
        lineTo(2.2f, 7.8f)
        arcTo(2f, 2f, 0f, false, true, 4.2f, 5.8f)
        close()
      }
      path(fill = SolidColor(Color.Black)) {
        moveTo(10.0f, 9.0f)
        lineTo(16.2f, 12.0f)
        lineTo(10.0f, 15.0f)
        close()
      }
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
        moveTo(8.5f, 20.8f)
        lineTo(15.5f, 20.8f)
      }
    }.build()
  }

  /** Custom Porch Board Community Hub Emblem */
  val NavPorchBoard: ImageVector by lazy {
    ImageVector.Builder(
      name = "NavPorchBoard",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      ) {
        moveTo(3.5f, 6.2f)
        lineTo(20.5f, 6.2f)
        arcTo(1.8f, 1.8f, 0f, false, true, 22.3f, 8.0f)
        lineTo(22.3f, 15.2f)
        arcTo(1.8f, 1.8f, 0f, false, true, 20.5f, 17.0f)
        lineTo(11.5f, 17.0f)
        lineTo(7.2f, 20.8f)
        lineTo(7.2f, 17.0f)
        lineTo(3.5f, 17.0f)
        arcTo(1.8f, 1.8f, 0f, false, true, 1.7f, 15.2f)
        lineTo(1.7f, 8.0f)
        arcTo(1.8f, 1.8f, 0f, false, true, 3.5f, 6.2f)
        close()
      }
      // Signal lines inside board bubble
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
        moveTo(6.5f, 10.3f)
        lineTo(17.5f, 10.3f)
        moveTo(6.5f, 13.5f)
        lineTo(13.8f, 13.5f)
      }
    }.build()
  }

  /** Custom AR HUD Corner Reticle + Isometric Cube */
  val ActionArReticle: ImageVector by lazy {
    ImageVector.Builder(
      name = "ActionArReticle",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        // 4 corner HUD brackets
        moveTo(3f, 7.5f); lineTo(3f, 3f); lineTo(7.5f, 3f)
        moveTo(16.5f, 3f); lineTo(21f, 3f); lineTo(21f, 7.5f)
        moveTo(3f, 16.5f); lineTo(3f, 21f); lineTo(7.5f, 21f)
        moveTo(16.5f, 21f); lineTo(21f, 21f); lineTo(21f, 16.5f)
      }
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 6.8f)
        lineTo(16.6f, 9.4f)
        lineTo(16.6f, 14.6f)
        lineTo(12f, 17.2f)
        lineTo(7.4f, 14.6f)
        lineTo(7.4f, 9.4f)
        close()
        moveTo(12f, 12f)
        lineTo(16.6f, 9.4f)
        moveTo(12f, 12f)
        lineTo(7.4f, 9.4f)
        moveTo(12f, 12f)
        lineTo(12f, 17.2f)
      }
    }.build()
  }

  /** Custom Diagnostics Shield + Live Heartbeat Pulse */
  val ActionHealthPulse: ImageVector by lazy {
    ImageVector.Builder(
      name = "ActionHealthPulse",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 2.5f)
        lineTo(19.6f, 5.6f)
        lineTo(19.6f, 11.8f)
        curveTo(19.6f, 16.8f, 16.4f, 20.1f, 12f, 21.8f)
        curveTo(7.6f, 20.1f, 4.4f, 16.8f, 4.4f, 11.8f)
        lineTo(4.4f, 5.6f)
        close()
      }
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(6.8f, 12.2f)
        lineTo(9.4f, 12.2f)
        lineTo(10.8f, 8.8f)
        lineTo(13.1f, 15.2f)
        lineTo(14.7f, 11.2f)
        lineTo(17.2f, 11.2f)
      }
    }.build()
  }

  /** Custom Precision Machined Settings Gear */
  val ActionSettingsCog: ImageVector by lazy {
    ImageVector.Builder(
      name = "ActionSettingsCog",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 8.4f)
        arcTo(3.6f, 3.6f, 0f, true, true, 11.99f, 8.4f)
        close()
      }
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.1f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 2.4f); lineTo(12f, 5.0f)
        moveTo(12f, 19.0f); lineTo(12f, 21.6f)
        moveTo(2.4f, 12f); lineTo(5.0f, 12f)
        moveTo(19.0f, 12f); lineTo(21.6f, 12f)
        moveTo(5.2f, 5.2f); lineTo(7.1f, 7.1f)
        moveTo(16.9f, 16.9f); lineTo(18.8f, 18.8f)
        moveTo(18.8f, 5.2f); lineTo(16.9f, 7.1f)
        moveTo(7.1f, 16.9f); lineTo(5.2f, 18.8f)
      }
    }.build()
  }

  /** Custom Theme Studio Swatch + Star */
  val ActionThemeStudio: ImageVector by lazy {
    ImageVector.Builder(
      name = "ActionThemeStudio",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, false, 20.6f, 14.5f)
        curveTo(20.9f, 13.4f, 20.0f, 12.4f, 18.8f, 12.4f)
        lineTo(16.2f, 12.4f)
        arcTo(2.2f, 2.2f, 0f, false, true, 14f, 10.2f)
        lineTo(14f, 6.2f)
        curveTo(14f, 4.3f, 13.2f, 3f, 12f, 3f)
        close()
      }
      path(fill = SolidColor(Color.Black)) {
        moveTo(8.2f, 8.2f); arcTo(1.3f, 1.3f, 0f, true, true, 8.19f, 8.2f); close()
        moveTo(6.8f, 12.5f); arcTo(1.3f, 1.3f, 0f, true, true, 6.79f, 12.5f); close()
        moveTo(10.0f, 16.2f); arcTo(1.3f, 1.3f, 0f, true, true, 9.99f, 16.2f); close()
      }
    }.build()
  }

  // PEV Vehicle Categories
  val Scooter: ImageVector = Icons.Default.ElectricScooter
  val Ebike: ImageVector = Icons.Default.ElectricBike
  val Emoto: ImageVector = Icons.Default.TwoWheeler
  val Moped: ImageVector = Icons.Default.ElectricMoped
  val OnewheelSkate: ImageVector = Icons.Default.Skateboarding
  val EucTrail: ImageVector = Icons.Default.DirectionsBike

  // Rural Country & Trail Elements
  val BarnShop: ImageVector = Icons.Default.Cabin
  val Homestead: ImageVector = Icons.Default.HomeWork
  val FarmTractor: ImageVector = Icons.Default.Agriculture
  val PineForest: ImageVector = Icons.Default.Forest
  val TrailPark: ImageVector = Icons.Default.Park
  val GravelTerrain: ImageVector = Icons.Default.Terrain
  val CountryHills: ImageVector = Icons.Default.Landscape
  val PastureGrass: ImageVector = Icons.Default.Grass
  val CreekWater: ImageVector = Icons.Default.Water
  val MudRain: ImageVector = Icons.Default.WaterDrop
  val WildlifeLivestock: ImageVector = Icons.Default.Pets
  val GeneralStore: ImageVector = Icons.Default.Storefront
  val AirPumpStop: ImageVector = Icons.Default.LocalGasStation
  val SunsetDaylight: ImageVector = Icons.Default.WbTwilight
  val OffGridSolar: ImageVector = Icons.Default.SolarPower

  // PEV Engineering, Comms & Analytics
  val HighVoltage: ImageVector = Icons.Default.Bolt
  val BatteryCharge: ImageVector = Icons.Default.BatteryChargingFull
  val OutletPlug: ImageVector = Icons.Default.Power
  val WrenchBench: ImageVector = Icons.Default.Build
  val ShopTools: ImageVector = Icons.Default.Hardware
  val BenchCamera: ImageVector = Icons.Default.CameraAlt
  val ConvoyRadio: ImageVector = Icons.Default.Radio
  val TrailCompass: ImageVector = Icons.Default.Explore
  val AhrsGyro: ImageVector = Icons.Default.CompassCalibration
  val SpeedGauge: ImageVector = Icons.Default.Speed
  val RouteTrack: ImageVector = Icons.Default.Route
  val AnalyticsLine: ImageVector = Icons.Default.ShowChart
  val AnalyticsPie: ImageVector = Icons.Default.PieChart

  fun forVehicleCategory(category: String): ImageVector =
    when (category.uppercase()) {
      "EBIKE", "EMTB" -> Ebike
      "EMOTO", "DIRTBIKE" -> Emoto
      "MOPED" -> Moped
      "ONEWHEEL", "ESKATE" -> OnewheelSkate
      "EUC" -> EucTrail
      else -> Scooter
    }

  fun forRuralCondition(tag: String): ImageVector =
    when (tag.uppercase()) {
      "MUDDY", "WET", "RAIN" -> MudRain
      "GRAVEL FRESH", "GRAVEL", "DIRT" -> GravelTerrain
      "CREEK HIGH", "CREEK", "WATER" -> CreekWater
      "WILDLIFE SEEN", "LIVESTOCK", "DEER" -> WildlifeLivestock
      "CHARGER LIVE", "SOLAR", "OUTLET" -> OffGridSolar
      "BARN BUILD", "WORKSHOP", "DIY" -> BarnShop
      "PINE TRAIL", "FOREST" -> PineForest
      else -> CountryHills
    }

  fun forRuralWaypoint(type: String): ImageVector =
    when (type.uppercase()) {
      "GENERAL_STORE" -> GeneralStore
      "AIR_PUMP" -> AirPumpStop
      "TRAILHEAD" -> PineForest
      "CREEK_CROSSING" -> CreekWater
      "SOLAR_SHELTER" -> OffGridSolar
      "BARN_STOP" -> BarnShop
      else -> OutletPlug
    }
}

/**
 * Upgraded duotone/neon-framed icon badge that adapts dynamically to the user's
 * chosen Icon Style Pack ("Tactical Neon", "Framed Duotone", "Classic Minimal", "Retro Pixel")
 * and Icon Neon Glow setting.
 */
@Composable
fun UpgradedIconBadge(
  icon: ImageVector,
  contentDescription: String?,
  tint: Color = MaterialTheme.colorScheme.primary,
  containerSize: Dp = 32.dp,
  iconSize: Dp = 18.dp,
  active: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val settings = LocalTacticalSettings.current
  val pack = settings.iconStylePack
  val glow = settings.iconNeonGlowEnabled && active && !isAnyWindowsDesktopActive(settings)
  val shape =
    when (pack) {
      "Retro Pixel" -> RoundedCornerShape(3.dp)
      "Framed Duotone" -> RoundedCornerShape(10.dp)
      else -> CircleShape
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(containerSize)
        .then(
          if (glow) {
            Modifier.drawBehind {
              drawCircle(
                brush = Brush.radialGradient(listOf(tint.copy(alpha = 0.34f), Color.Transparent)),
                radius = size.maxDimension * 0.78f,
              )
            }
          } else Modifier
        )
        .then(
          when (pack) {
            "Classic Minimal" -> Modifier
            "Retro Pixel" ->
              Modifier
                .clip(shape)
                .background(tint.copy(alpha = if (active) 0.18f else 0.08f))
                .border(1.2.dp, tint.copy(alpha = if (active) 0.72f else 0.25f), shape)
            else ->
              Modifier
                .clip(shape)
                .background(tint.copy(alpha = if (active) 0.18f else 0.08f))
                .border(1.dp, tint.copy(alpha = if (active) 0.55f else 0.22f), shape)
          }
        ),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(iconSize),
    )
  }
}
