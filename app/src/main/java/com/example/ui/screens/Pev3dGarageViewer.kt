package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.PevBatteryPhysics
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalSlider
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

private data class RealWorldPevModel(
  val name: String,
  val category: String,
  val groupCategory: String,
  val topSpeedMph: Int,
  val voltage: Int,
  val rangeMiles: Int,
  val powerKw: String,
  val description: String,
)

private data class PevComponentPart(
  val key: String,
  val name: String,
  val category: String,
  val icon: ImageVector,
  val accent: Color,
  val summary: String,
  val deepDescription: String,
  val specs: String,
)

private val PEV_COMPONENT_PARTS = listOf(
  PevComponentPart(
    key = "battery",
    name = "High-Voltage Battery Pack & Smart BMS",
    category = "Power System",
    icon = Icons.Default.ElectricBolt,
    accent = Color(0xFF22C55E),
    summary = "Grade-A lithium cells with intelligent cell balancing and over-current protection.",
    deepDescription = "Constructed using high-density 21700 or 18650 lithium-ion cells configured in high-voltage matrices (48V–151V+). Features a Bluetooth Smart BMS that monitors individual cell groups, thermal limits, short-circuit protection, and voltage sag compensation under steep rural hill climbs.",
    specs = "Cells: 21700 Li-Ion | BMS: Smart Bluetooth 200A | Thermal Rating: IP67 Sealed",
  ),
  PevComponentPart(
    key = "motor",
    name = "Brushless Direct-Drive / Hub Motor",
    category = "Propulsion",
    icon = Icons.Default.Speed,
    accent = Color(0xFFFF6B35),
    summary = "High-torque rare-earth neodymium magnetic stator engineered for instant acceleration.",
    deepDescription = "Multi-strand high-temp copper windings paired with N42SH curved neodymium magnets deliver intense magnetic flux density. Handles high phase amperage on steep climbs while feeding variable regenerative braking back into the pack.",
    specs = "Stator: 60mm Copper | Magnets: N42SH Neodymium | Efficiency: 94.5%",
  ),
  PevComponentPart(
    key = "brakes",
    name = "Hydraulic 4-Piston Caliper Brakes",
    category = "Stopping & Safety",
    icon = Icons.Default.Shield,
    accent = Color(0xFF38BDF8),
    summary = "Motorcycle-grade hydraulic braking with ventilated steel floating rotors.",
    deepDescription = "Dual 4-piston CNC hydraulic calipers clamp down on 180mm–203mm floating steel rotors. Hall cutoff switches disable throttle immediately while blending progressive e-brake regen.",
    specs = "Calipers: Quad-Piston CNC | Rotors: 203mm Floating | Fluid: Mineral / DOT 5.1",
  ),
  PevComponentPart(
    key = "suspension",
    name = "Inverted Hydraulic Suspension & Shocks",
    category = "Chassis & Ride",
    icon = Icons.Default.Settings,
    accent = Color(0xFF6366F1),
    summary = "Adjustable rebound and compression damping for plush gravel & trail absorption.",
    deepDescription = "Heavy-duty inverted front forks and rear nitrogen/coil shocks absorb washboard gravel, rock gardens, and ruts with tunable preload, compression, and rebound clickers.",
    specs = "Travel: 150–200mm | Damping: Rebound & Compression | Stanchions: 38mm Anodized",
  ),
  PevComponentPart(
    key = "controller",
    name = "Sine-Wave FOC Controller & Inverter",
    category = "Brain & Electronics",
    icon = Icons.Default.AutoAwesome,
    accent = Color(0xFFEC4899),
    summary = "Field-Oriented Control (FOC) sinewave inverter for silent, high-torque modulation.",
    deepDescription = "Runs Field-Oriented Control (FOC) MOSFET switching to eliminate low-speed motor cogging. Extruded aluminum deck heatsinks dissipate continuous hill-climb current safely.",
    specs = "Inverter: Sine-Wave FOC | Phase Rating: 150A–350A | PWM Frequency: 20kHz",
  ),
  PevComponentPart(
    key = "cockpit",
    name = "Smart TFT Cockpit & Throttle Suite",
    category = "Interface",
    icon = Icons.Default.Computer,
    accent = Color(0xFFF59E0B),
    summary = "Anti-glare color display with real-time voltage telemetry, NFC lock & LED optics.",
    deepDescription = "High-luminance IPS TFT dash displays live pack voltage, amperage, motor temp, speed limit alerts, and drive modes over Bluetooth/CAN.",
    specs = "Display: 4.3\" IPS TFT | Security: NFC / PIN | Ingress: IP66 Weatherproof",
  ),
  PevComponentPart(
    key = "frame",
    name = "Aerospace Monocoque Frame & Swingarm",
    category = "Structural Foundation",
    icon = Icons.Default.Build,
    accent = Color(0xFFA855F7),
    summary = "Forged 6061-T6 aircraft aluminum and steel-reinforced swingarm for zero flex.",
    deepDescription = "CNC-machined forged 6061-T6 chassis with sealed pivot bearings and reinforced neck gussets eliminates high-speed oscillation on rough rural backroads.",
    specs = "Alloy: Forged 6061-T6 Aluminum | Pivot: Sealed Needle Bearings",
  ),
)

private fun groupCatFor(cat: String): String {
  val c = cat.lowercase()
  return when {
    c.contains("unicycle") -> "EUC"
    c.contains("scooter") -> "Hyper-Scooter"
    c.contains("dirt") || c.contains("moto") || c.contains("utility moto") -> "e-Moto & Dirt"
    c.contains("bike") || c.contains("moped") || c.contains("scrambler") || c.contains("folding") -> "e-Bike & Moped"
    else -> "Specialty"
  }
}

private fun model(
  name: String,
  category: String,
  topSpeedMph: Int,
  voltage: Int,
  rangeMiles: Int,
  powerKw: String,
  description: String,
) = RealWorldPevModel(name, category, groupCatFor(category), topSpeedMph, voltage, rangeMiles, powerKw, description)

// Complete 58-rig real-world PEV catalog mapped to Vulkan Filament .glb archetypes & dynamic PBR MaterialInstance overrides
private val AAA_PEV_MODELS =
  listOf(
    model("Sur-Ron Ultra Bee eMoto", "Electric Dirt Moped", 56, 74, 85, "12.5 kW Peak", "Heavy-duty off-road e-moto featuring adjustable suspension, forged aluminum frame, and instant high-torque brushless drive."),
    model("Dualtron X Limited Hyperscooter", "Hyper-Scooter", 68, 84, 90, "13.4 kW Peak", "Flagship dual-motor hyperscooter with hydraulic steering damper, massive 84V battery, and quad piston brakes."),
    model("Begode Master v4 High-Voltage EUC", "Electric Unicycle", 64, 134, 95, "4500W Nom / 12kW Peak", "Extreme 134V suspension electric unicycle engineered for high-speed backcountry trail carving and hill climbs."),
    model("Super73-RX Mojave Edition e-Bike", "Off-Road e-Bike", 35, 52, 75, "2.0 kW Peak", "Rugged adventure e-bike with fully adjustable inverted coil-spring forks and aggressive knobby tires."),
    model("InMotion V13 Challenger EUC", "Electric Unicycle", 88, 126, 90, "4500W Nom / 10kW Peak", "High-speed flagship EUC with intelligent BMS telemetry, massive honeycomb pedals, and dual shock damping."),
    model("Segway Ninebot GT2 SuperScooter", "Hyper-Scooter", 43, 52, 55, "6.0 kW Peak", "Futuristic superscooter featuring digital transparent PM OLED cockpit display and front/rear adjustable damping."),
    model("Talaria Sting R MX4 eMoto", "Electric Dirt Moped", 53, 60, 78, "10.8 kW Peak", "Gearbox-driven electric dirt bike built for brutal single-track enduro trails and steep climbs."),
    model("Veteran Sherman-S Suspension EUC", "Electric Unicycle", 70, 100, 110, "4000W Nom / 10kW Peak", "Armored 100.8V suspension unicycle with integrated roll bar, 3600Wh pack, and hydraulic suspension."),
    model("Sur-Ron Light Bee X", "Electric Dirt Moped", 48, 60, 60, "6.0 kW Peak", "The iconic lightweight electric dirt bike that started a global trail revolution."),
    model("Sur-Ron Storm Bee", "Electric Dirt Bike", 70, 104, 75, "22.5 kW Peak", "Full-size electric dirt bike matching 250cc ICE motocross performance with instant torque."),
    model("Nami Burn-E 2 Max", "Hyper-Scooter", 60, 72, 80, "8.4 kW Peak", "Welded tubular steel frame hyperscooter with adjustable hydraulic suspension and smart display."),
    model("Kaabo Wolf King GTR", "Hyper-Scooter", 65, 72, 90, "8.0 kW Peak", "Dual-stem beast with sinewave controllers, removable battery pack, and motorcycle-grade forks."),
    model("Segway SuperScooter P100S", "Commuter Scooter", 30, 48, 62, "1.2 kW Peak", "All-season high-performance commuter with NFC unlock and self-sealing tires."),
    model("Apollo Pro 2024 Flagship", "Hyper-Scooter", 44, 52, 65, "3.6 kW Peak", "CNC aluminum flagship scooter with thumb regen throttle and integrated turn signals."),
    model("InMotion RS Hyper-Scooter", "Hyper-Scooter", 68, 72, 75, "8.4 kW Peak", "Dual-motor powerhouse with 11-stage adjustable hydraulic suspension and dual charging ports."),
    model("Begode EX.N High-Speed", "Electric Unicycle", 55, 100, 70, "3500W Nom", "Legendary high-speed cruiser unicycle with hollow bore motor and crisp audio speakers."),
    model("Begode Master Pro 134V", "Electric Unicycle", 65, 134, 120, "4800W Nom", "Ultra-long-range expedition EUC with 4800Wh battery pack and advanced thermal management."),
    model("LeaperKim Patton 126V", "Electric Unicycle", 55, 126, 75, "3000W Nom", "Agile, armored suspension EUC built for technical off-road stunts and rapid transit."),
    model("LeaperKim Lynx 151V", "Electric Unicycle", 70, 151, 95, "3500W Nom", "Magnesium-alloy frame hyper-wheel with rapid charge capability and custom coil shocks."),
    model("Kingsong S22 Pro Eagle", "Electric Unicycle", 44, 126, 70, "4000W Peak", "Sleek suspended unicycle with integrated power pads and smart modular controller."),
    model("Kingsong S19 Pro", "Electric Unicycle", 37, 100, 60, "3500W Peak", "Mid-sized suspension wheel with ergonomic handle and Bluetooth stereo sound."),
    model("Cake Kalk AP Electric Motorcycle", "Electric Dirt Bike", 56, 52, 50, "11.0 kW Peak", "Minimalist Swedish trail dirt bike crafted with precision CNC components."),
    model("Stark Varg EX Electric Motocross", "Electric Dirt Bike", 80, 360, 60, "60.0 kW Peak", "Ultra-high-voltage electric motocross bike with customizable power maps."),
    model("Zero 11X Hyperscooter", "Hyper-Scooter", 62, 72, 70, "3200W Dual", "Twin hydraulic suspension hyperscooter built for high-speed stability."),
    model("Rion Apex Carbon Hyper-Scooter", "Hyper-Scooter", 75, 84, 50, "15.0 kW Peak", "Aerospace carbon fiber hyperscooter designed for track acceleration dominance."),
    model("Onyx RCR Vintage Moped", "Electric Moped", 60, 72, 75, "5.4 kW Peak", "Cafe racer moped with pedals and neck-snapping hub motor acceleration."),
    model("Sondors Metacycle", "Electric Motorcycle", 80, 72, 80, "14.5 kW Peak", "Naked cast-aluminum frame electric motorcycle designed for highway and country roads."),
    model("Ryvid Anthem", "Electric Motorcycle", 75, 72, 75, "18.0 kW Peak", "Lightweight electric motorcycle featuring adjustable seat height and removable battery pack."),
    model("Stealth Bomber B-52 eMoto", "Electric Dirt Bike", 50, 72, 80, "6.2 kW Peak", "Heavy-duty monocoque e-bike with 9-speed sequential gearbox drive."),
    model("Fliteboard Ultra eFoil", "Electric Hydrofoil", 30, 52, 25, "4.5 kW Peak", "High-performance carbon electric hydrofoil for flying over rural lakes."),
    model("Rad Power RadRunner 3 Plus", "Utility e-Bike", 20, 48, 45, "750W Nom", "Versatile utility compact cargo e-bike with hydraulic disc brakes."),
    model("Juiced HyperScrambler 2", "Off-Road e-Bike", 33, 52, 100, "2.0 kW Dual Battery", "Dual-battery long-range moped-style e-bike with dual suspension."),
    model("Ariel Rider Grizzly", "Off-Road e-Bike", 38, 52, 75, "2.0 kW Dual Motor", "Dual-motor dual-suspension powerhouse built to conquer steep dirt hills."),
    model("Specialized Turbo Levo Pro", "Mountain e-Bike", 20, 48, 60, "565W Peak", "Benchmark full-suspension electric mountain bike with seamless pedal assist."),
    model("BMW CE 04 Electric Scooter", "Electric Scooter", 75, 148, 80, "31.0 kW Peak", "Futuristic maxi-scooter with TFT connectivity and motorway acceleration."),
    model("Volcon Grunt EVO 2WD", "Electric Utility Moto", 40, 60, 70, "8.0 kW Peak", "Fat-tire rugged two-wheel-drive electric utility bike built for farm & ranch trails."),
    model("Kaabo Mantis King GT", "Hyper-Scooter", 43, 60, 55, "4.0 kW Peak", "Agile dual-motor midsize hyperscooter with dual shock suspension and TFT display."),
    model("InMotion Climber", "Commuter Scooter", 24, 36, 35, "1.5 kW Dual Motor", "Dual-motor compact scooter built to climb 30% rural hills effortlessly."),
    model("Begode T4 Pro Suspension EUC", "Electric Unicycle", 45, 100, 55, "2500W Nom", "Compact 16-inch suspension wheel with air shock and snappy torque response."),
    model("Dualtron Thunder 3", "Hyper-Scooter", 60, 72, 70, "10.0 kW Peak", "Racing hyperscooter with IPX5 deck waterproofing and NUTT 4-piston hydraulics."),
    model("EMove Cruiser V2", "Commuter Scooter", 35, 52, 100, "1000W Nom", "Long-range commuter with massive 30Ah battery and tubeless tires."),
    model("E-Ride Pro SS 2.0 72V", "Electric Dirt Bike", 60, 72, 65, "12.0 kW Peak", "72V 40Ah trail slayer with forged 6061 frame, turbo button, and rapid regen braking."),
    model("Talaria XXX Black Edition", "Electric Dirt Moped", 47, 60, 50, "6.5 kW Peak", "Nimble belt-and-chain urban & dirt e-moto with supermoto agility."),
    model("Altis Sigma 98V Hyper-Moto", "Electric Dirt Bike", 75, 98, 70, "22.0 kW Peak", "98V hairpin-motor electric motocross machine with forged linkage suspension."),
    model("Talaria Komodo Enduro", "Electric Dirt Bike", 65, 72, 75, "16.0 kW Peak", "Long-travel enduro electric dirt bike tuned for technical rocky backcountry climbs."),
    model("Segway Dirt eBike X260", "Electric Dirt Moped", 46, 60, 74, "5.0 kW Peak", "Lightweight aluminum-frame electric trail bike with mobile telemetry tuning."),
    model("Dualtron Storm Limited", "Hyper-Scooter", 62, 84, 110, "11.5 kW Peak", "84V 45Ah long-haul hyper-scooter with removable battery pack and dual 60A controllers."),
    model("Kaabo Wolf Warrior X GT", "Hyper-Scooter", 44, 60, 62, "4.2 kW Peak", "Compact dual-fork off-road scooter with sinewave controllers and knobby tires."),
    model("VSETT 10+ Apex", "Hyper-Scooter", 50, 60, 65, "5.2 kW Peak", "Dual-motor removable-battery scooter with hydraulic brakes and sport boost mode."),
    model("Teverun Fighter Supreme 7260R", "Hyper-Scooter", 75, 72, 120, "15.0 kW Peak", "13-inch tire 72V 60Ah apex hyper-scooter with smart TFT and dual steering dampers."),
    model("Segway Ninebot GT1", "Hyper-Scooter", 37, 52, 43, "3.0 kW Peak", "Rear-wheel-drive superscooter with aircraft-grade aluminum chassis and hydraulic damping."),
    model("Super73-S2 Adventure", "Scrambler e-Bike", 28, 48, 60, "2.0 kW Peak", "Iconic café-style fat-tire scrambler e-bike with air suspension front fork."),
    model("Lectric XP 3.0 Fat-Tire", "Folding e-Bike", 28, 48, 65, "1.0 kW Peak", "Compact 20x3 fat-tire folding utility e-bike with hydraulic brakes and rear rack."),
    model("Aventon Aventure.2 Fat-Tire", "Off-Road e-Bike", 28, 48, 60, "1.1 kW Peak", "Torque-sensor 4-inch fat-tire all-terrain e-bike built for gravel, sand, and snow."),
    model("Ride1UP Revv 1 DRT", "Scrambler e-Bike", 35, 52, 55, "1.8 kW Peak", "Off-road scrambler moped e-bike with full-suspension frame and 52V 20Ah pack."),
    model("Begode Extreme 134V EUC", "Electric Unicycle", 56, 134, 70, "3500W Nom", "High-torque 16-inch trail suspension EUC with 130mm DS200 shock travel."),
    model("LeaperKim Sherman-L 151V", "Electric Unicycle", 68, 151, 135, "3200W Nom", "Long-range 4000Wh 151.2V touring & backcountry suspension flagship EUC."),
    model("InMotion V14 Adventure EUC", "Electric Unicycle", 44, 134, 75, "4000W Nom", "Modular quick-swap 50S cell pack off-road EUC with progressive coil linkage."),
  )

/**
 * Interactive Native Vulkan 3D PEV CAD & Dyno Lab powered by Google Filament 1.81.0.
 * Loads real .glb models from app/src/main/assets/models/ with dynamic per-model PBR overrides
 * for all 58 PEV rigs, 360° touch orbit, fling inertia, pinch zoom, camera presets, and teardown HUD.
 */
@Composable
fun Pev3dGarageViewer(
  activeVehicleName: String = "Hyper-PEV Flagship",
  activeVoltage: Double = 74.0,
) {
  val modelCategories = remember { listOf("All", "e-Moto & Dirt", "Hyper-Scooter", "EUC", "e-Bike & Moped", "Specialty") }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedModelIdx by remember { mutableIntStateOf(0) }
  var motorThrottlePct by remember { mutableFloatStateOf(0.35f) }

  // True 3D Interactive Camera & Animation States
  var modelYawDeg by remember { mutableFloatStateOf(-28f) }
  var modelPitchDeg by remember { mutableFloatStateOf(16f) }
  var modelZoomScale by remember { mutableFloatStateOf(1.05f) }
  var explodePct by remember { mutableFloatStateOf(0.0f) }
  var renderMode by remember { mutableStateOf(Pev3dRenderMode.SOLID) }
  var lightingPreset by remember { mutableStateOf(Pev3dLightingPreset.STUDIO) }
  var autoSpin by remember { mutableStateOf(false) }
  var lightsOn by remember { mutableStateOf(true) }
  var ssaoEnabled by remember { mutableStateOf(true) }
  var turnSignalMode by remember { mutableStateOf(Pev3dTurnSignalMode.OFF) }
  var wheelSpinDeg by remember { mutableFloatStateOf(0f) }
  var yawVelocityDegPerSec by remember { mutableFloatStateOf(0f) }
  var targetCameraPreset by remember { mutableStateOf<Pair<Float, Float>?>(null) }
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var gpuBackendLabel by remember { mutableStateOf("VULKAN • FILAMENT PBR") }
  var cachedWireGroups by remember { mutableStateOf<List<PevWireGroup>>(emptyList()) }
  var assetLoadState by remember { mutableStateOf<Pev3dAssetLoadState>(Pev3dAssetLoadState.Loading) }

  // Interactive Part Selection
  var selectedPart by remember { mutableStateOf<PevComponentPart?>(PEV_COMPONENT_PARTS[0]) }

  val filteredModelIndices = remember(selectedCategory) {
    AAA_PEV_MODELS.indices.filter { idx ->
      selectedCategory == "All" || AAA_PEV_MODELS[idx].groupCategory == selectedCategory
    }
  }
  val currentModel = AAA_PEV_MODELS[selectedModelIdx]
  val archetype = remember(currentModel.name, currentModel.category) {
    Pev3dEngine.archetypeForCategory("${currentModel.name} ${currentModel.category}")
  }
  val packSpec = remember(currentModel.voltage) { PevBatteryPhysics.packProfile(currentModel.voltage.toDouble(), "NMC") }

  val projectedHotspots =
    remember(archetype, viewportSize, modelYawDeg, modelPitchDeg, modelZoomScale, explodePct) {
      Pev3dEngine.projectHotspots(
        archetype = archetype,
        widthPx = viewportSize.width.toFloat(),
        heightPx = viewportSize.height.toFloat(),
        yawDeg = modelYawDeg,
        pitchDeg = modelPitchDeg,
        zoom = modelZoomScale,
        explodePct = explodePct,
      )
    }
  val latestHotspots by rememberUpdatedState(projectedHotspots)

  // 60fps wheel rotation + inertial fling spin + smooth camera preset transitions + auto-orbit loop
  LaunchedEffect(autoSpin, motorThrottlePct) {
    var lastNs = 0L
    while (true) {
      withFrameNanos { ns ->
        if (lastNs != 0L) {
          val dt = ((ns - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
          if (motorThrottlePct > 0.01f) {
            wheelSpinDeg = (wheelSpinDeg + dt * (120f + motorThrottlePct * 1100f)) % 360f
          }
          val presetTarget = targetCameraPreset
          if (presetTarget != null) {
            val yawDiff = ((presetTarget.first - modelYawDeg + 540f) % 360f) - 180f
            val pitchDiff = presetTarget.second - modelPitchDeg
            if (abs(yawDiff) < 0.35f && abs(pitchDiff) < 0.35f) {
              modelYawDeg = (presetTarget.first + 360f) % 360f
              modelPitchDeg = presetTarget.second
              targetCameraPreset = null
            } else {
              val alpha = (dt * 11f).coerceIn(0.05f, 0.45f)
              modelYawDeg = (modelYawDeg + yawDiff * alpha + 360f) % 360f
              modelPitchDeg = modelPitchDeg + pitchDiff * alpha
            }
          } else if (autoSpin) {
            modelYawDeg = (modelYawDeg + dt * 28f) % 360f
            yawVelocityDegPerSec = 0f
          } else if (abs(yawVelocityDegPerSec) > 0.8f) {
            modelYawDeg = (modelYawDeg + yawVelocityDegPerSec * dt + 360f) % 360f
            yawVelocityDegPerSec *= 0.05f.pow(dt)
          } else if (yawVelocityDegPerSec != 0f) {
            yawVelocityDegPerSec = 0f
          }
        }
        lastNs = ns
      }
    }
  }

  val liveMph = (currentModel.topSpeedMph * motorThrottlePct).roundToInt()
  val liveRpm = (liveMph * 26.5).roundToInt()
  val primaryCol = MaterialTheme.colorScheme.primary

  Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
    TacticalCard(highlight = true, title = "VULKAN 3D RIG LAB (${AAA_PEV_MODELS.size} MODELS)") {
      // Category Filter Strip
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(modelCategories) { cat ->
          val count = if (cat == "All") AAA_PEV_MODELS.size else AAA_PEV_MODELS.count { it.groupCategory == cat }
          FilterChip(
            selected = selectedCategory == cat,
            onClick = {
              selectedCategory = cat
              val first = AAA_PEV_MODELS.indices.firstOrNull { cat == "All" || AAA_PEV_MODELS[it].groupCategory == cat }
              if (first != null && (cat != "All" && currentModel.groupCategory != cat)) {
                selectedModelIdx = first
              }
            },
            label = { Text("$cat ($count)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_category_${cat.lowercase().replace(" ", "_")}"),
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      // Categorized Model Selector Chips + Active Rig Quick Match
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
          FilterChip(
            selected = false,
            onClick = {
              val tokens = activeVehicleName.lowercase().split(" ").filter { it.length >= 3 }
              val matchedIdx =
                AAA_PEV_MODELS.indexOfFirst { m -> tokens.any { t -> m.name.lowercase().contains(t) } }
                  .takeIf { it >= 0 }
                  ?: AAA_PEV_MODELS.indices.minByOrNull { abs(AAA_PEV_MODELS[it].voltage - activeVoltage.toInt()) }
                  ?: 0
              selectedCategory = "All"
              selectedModelIdx = matchedIdx
            },
            label = { Text("Match Rig (${activeVoltage.toInt()}V)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
        items(filteredModelIndices) { idx ->
          val m = AAA_PEV_MODELS[idx]
          FilterChip(
            selected = selectedModelIdx == idx,
            onClick = {
              selectedModelIdx = idx
              modelYawDeg = -28f
              modelPitchDeg = 16f
              modelZoomScale = 1.05f
            },
            label = { Text(m.name.split(" ").take(2).joinToString(" "), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("aaa_pev_model_$idx"),
          )
        }
      }

      Spacer(Modifier.height(6.dp))

      // Render Mode + Lighting + Optics Controls
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Pev3dRenderMode.entries) { mode ->
          FilterChip(
            selected = renderMode == mode,
            onClick = { renderMode = mode },
            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_render_${mode.name.lowercase()}"),
          )
        }
        item {
          FilterChip(
            selected = autoSpin,
            onClick = { autoSpin = !autoSpin },
            leadingIcon = { Icon(if (autoSpin) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(14.dp)) },
            label = { Text(if (autoSpin) "Spin ON" else "Auto-Spin", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_autospin_chip"),
          )
        }
        item {
          FilterChip(
            selected = lightsOn,
            onClick = { lightsOn = !lightsOn },
            leadingIcon = { Icon(Icons.Default.FlashlightOn, null, Modifier.size(14.dp)) },
            label = { Text(if (lightsOn) "Head/Tail ON" else "Lights OFF", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_lights_toggle_chip"),
          )
        }
        item {
          FilterChip(
            selected = ssaoEnabled,
            onClick = { ssaoEnabled = !ssaoEnabled },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.rig_lab_ssao_toggle_desc),
                modifier = Modifier.size(14.dp),
              )
            },
            label = {
              Text(
                text = stringResource(if (ssaoEnabled) R.string.rig_lab_ssao_on else R.string.rig_lab_ssao_off),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
              )
            },
            modifier = Modifier.testTag("3d_ssao_toggle_chip"),
          )
        }
        items(listOf(Pev3dTurnSignalMode.LEFT, Pev3dTurnSignalMode.HAZARD, Pev3dTurnSignalMode.RIGHT)) { sig ->
          val active = turnSignalMode == sig
          val tagSuffix = when (sig) {
            Pev3dTurnSignalMode.LEFT -> "left"
            Pev3dTurnSignalMode.RIGHT -> "right"
            Pev3dTurnSignalMode.HAZARD -> "hazard"
            Pev3dTurnSignalMode.OFF -> "off"
          }
          FilterChip(
            selected = active,
            onClick = { turnSignalMode = if (active) Pev3dTurnSignalMode.OFF else sig },
            label = {
              Text(
                sig.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                color = if (active) Color(0xFFFFB020) else Color.Unspecified,
                maxLines = 1,
                softWrap = false,
              )
            },
            modifier = Modifier.testTag("3d_signal_$tagSuffix"),
          )
        }
        item {
          FilterChip(
            selected = explodePct > 0.1f,
            onClick = { explodePct = if (explodePct > 0.1f) 0f else 0.75f },
            leadingIcon = { Icon(Icons.Default.Layers, null, Modifier.size(14.dp)) },
            label = { Text(if (explodePct > 0.1f) "Assemble" else "Explode 3D", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_explode_toggle_chip"),
          )
        }
        items(Pev3dLightingPreset.entries) { preset ->
          FilterChip(
            selected = lightingPreset == preset,
            onClick = { lightingPreset = preset },
            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("3d_lighting_${preset.name.lowercase()}"),
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // NATIVE ANDROID VULKAN PBR VIEWPORT (GOOGLE FILAMENT 1.81.0 • ACES • 4x MSAA • SSAO • PCF SHADOWS)
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0E1117), Color(0xFF141820), Color(0xFF0A0C10))))
            .onSizeChanged { viewportSize = it }
            .pointerInput(selectedModelIdx, renderMode) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val downPos = down.position
                var totalMoved = 0f
                var prevCentroidSize = 0f
                var lastDragSpeedYaw = 0f
                var lastMoveMs = System.currentTimeMillis()
                yawVelocityDegPerSec = 0f
                targetCameraPreset = null
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Initial)
                  val pressed = event.changes.filter { it.pressed }
                  if (pressed.isEmpty()) break

                  if (pressed.size >= 2) {
                    val zoomDelta = event.calculateZoom()
                    val cSize = event.calculateCentroidSize(useCurrent = true)
                    if (prevCentroidSize > 0f && zoomDelta != 1f) {
                      modelZoomScale = (modelZoomScale * zoomDelta).coerceIn(0.65f, 2.2f)
                    }
                    prevCentroidSize = cSize
                    totalMoved += 20f
                    lastDragSpeedYaw = 0f
                    event.changes.forEach { it.consume() }
                  } else {
                    val pan = event.calculatePan()
                    totalMoved += hypot(pan.x, pan.y)
                    val nowMs = System.currentTimeMillis()
                    val dtSec = ((nowMs - lastMoveMs).coerceAtLeast(8L)) / 1000f
                    lastMoveMs = nowMs
                    if (totalMoved > 6f) {
                      autoSpin = false
                      val dYaw = pan.x * 0.55f
                      modelYawDeg = (modelYawDeg + dYaw + 360f) % 360f
                      modelPitchDeg = (modelPitchDeg - pan.y * 0.45f).coerceIn(-65f, 75f)
                      lastDragSpeedYaw = (dYaw / dtSec).coerceIn(-280f, 280f)
                      event.changes.forEach { it.consume() }
                    }
                  }
                }
                if (totalMoved >= 16f && abs(lastDragSpeedYaw) > 25f) {
                  yawVelocityDegPerSec = lastDragSpeedYaw
                }
                if (totalMoved < 16f && assetLoadState is Pev3dAssetLoadState.Loaded) {
                  val hit =
                    latestHotspots
                      .map { it to hypot(it.screenPos.x - downPos.x, it.screenPos.y - downPos.y) }
                      .filter { it.second <= 72f }
                      .minByOrNull { it.second }
                      ?.first
                  if (hit != null) {
                    selectedPart = PEV_COMPONENT_PARTS.firstOrNull { it.key == hit.partKey }
                  }
                }
              }
            }
            .testTag("aaa_pev_photo_viewport"),
      ) {
        val finish = remember(currentModel) {
          Pev3dEngine.finishForModel(currentModel.name, currentModel.category, currentModel.voltage)
        }
        Pev3dVulkanViewport(
          archetype = archetype,
          renderMode = renderMode,
          lightingPreset = lightingPreset,
          modelName = currentModel.name,
          modelCategory = currentModel.category,
          modelVoltage = currentModel.voltage,
          yawDeg = modelYawDeg,
          pitchDeg = modelPitchDeg,
          zoom = modelZoomScale,
          explodePct = explodePct,
          wheelSpinDeg = wheelSpinDeg,
          lightsOn = lightsOn,
          ssaoEnabled = ssaoEnabled,
          turnSignalMode = turnSignalMode,
          selectedPartKey = selectedPart?.key,
          accentRgb = finish.accentRgb,
          onBackendChanged = { gpuBackendLabel = it },
          onWireframeLinesReady = { cachedWireGroups = it },
          onAssetStateChanged = { assetLoadState = it },
          modifier = Modifier.fillMaxSize().testTag("pev_3d_interactive_canvas"),
        )

        // Draw interactive 3D hotspot pins only when a real .glb model is loaded
        if (assetLoadState is Pev3dAssetLoadState.Loaded) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            if (renderMode == Pev3dRenderMode.WIREFRAME && cachedWireGroups.isNotEmpty()) {
              val (baseSegs, selSegs) = Pev3dEngine.projectWireframeGroups(
                wireGroups = cachedWireGroups,
                widthPx = size.width,
                heightPx = size.height,
                yawDeg = modelYawDeg,
                pitchDeg = modelPitchDeg,
                zoom = modelZoomScale,
                explodePct = explodePct,
                wheelSpinDeg = wheelSpinDeg,
                selectedPartKey = selectedPart?.key,
              )
              val lineCol = Color(finish.accentRgb[0], finish.accentRgb[1], finish.accentRgb[2], 0.72f)
              var idx = 0
              while (idx + 3 < baseSegs.size) {
                drawLine(
                  color = lineCol,
                  start = Offset(baseSegs[idx], baseSegs[idx + 1]),
                  end = Offset(baseSegs[idx + 2], baseSegs[idx + 3]),
                  strokeWidth = 1.5f,
                )
                idx += 4
              }
              var sIdx = 0
              while (sIdx + 3 < selSegs.size) {
                drawLine(
                  color = Color.White.copy(alpha = 0.92f),
                  start = Offset(selSegs[sIdx], selSegs[sIdx + 1]),
                  end = Offset(selSegs[sIdx + 2], selSegs[sIdx + 3]),
                  strokeWidth = 2.6f,
                )
                sIdx += 4
              }
            }
            projectedHotspots.forEach { hs ->
              val active = selectedPart?.key == hs.partKey
              drawCircle(
                color = hs.accent.copy(alpha = if (active) 0.92f else 0.50f),
                radius = if (active) 13f else 9f,
                center = hs.screenPos,
              )
              drawCircle(
                color = Color.White.copy(alpha = if (active) 1f else 0.72f),
                radius = if (active) 16.5f else 11.5f,
                center = hs.screenPos,
                style = Stroke(width = if (active) 3.2f else 1.5f),
              )
            }
          }
        }

        // Clean in-viewport prompt when a real .glb asset hasn't been dropped into app/src/main/assets/models/ yet
        when (val loadState = assetLoadState) {
          is Pev3dAssetLoadState.MissingGlb -> {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xDC0B101A),
              border = BorderStroke(1.dp, primaryCol.copy(alpha = 0.65f)),
              modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .testTag("glb_missing_prompt_card"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(Icons.Default.FolderOpen, contentDescription = null, tint = primaryCol, modifier = Modifier.size(18.dp))
                  Text(
                    "DROP REAL .GLB MESH TO RENDER",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFF162033),
                  border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                ) {
                  Text(
                    text = loadState.expectedAssetPath,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
                Text(
                  "Vulkan PBR Showroom Ready (${loadState.envSource}). Add this .glb file for ${currentModel.name} — per-model metallic paint, Carbon Gloss & LED overrides apply automatically.",
                  color = Color(0xFFCBD5E1),
                  fontSize = 10.sp,
                  lineHeight = 13.sp,
                  textAlign = TextAlign.Center,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  val coreFiles = listOf("emoto.glb", "hyperscooter.glb", "ebike_fat.glb", "euc.glb")
                  items(coreFiles) { fn ->
                    val present = fn in loadState.presentGlbFiles
                    val isCurrent = fn == loadState.expectedFileName
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (present) Color(0x3322C55E) else if (isCurrent) Color(0x3338BDF8) else Color(0xFF1E293B),
                      border = BorderStroke(
                        1.dp,
                        if (present) Color(0xFF22C55E) else if (isCurrent) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.18f),
                      ),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                      ) {
                        if (present) {
                          Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(11.dp))
                        }
                        Text(
                          text = fn,
                          color = if (present) Color(0xFF86EFAC) else if (isCurrent) Color(0xFF7DD3FC) else Color(0xFF94A3B8),
                          fontSize = 9.sp,
                          fontFamily = FontFamily.Monospace,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          softWrap = false,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
          is Pev3dAssetLoadState.VulkanUnavailable -> {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xE6140C12),
              border = BorderStroke(1.dp, Color(0xFFEF4444)),
              modifier = Modifier.align(Alignment.Center).padding(16.dp).fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text("VULKAN 1.1+ GPU REQUIRED", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(
                  loadState.reason,
                  color = Color(0xFFE2E8F0),
                  fontSize = 10.sp,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
          else -> Unit
        }

        // Top non-overlapping bar: Left "VULKAN • FILAMENT PBR" badge + Right "RESET" pill
        Row(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC0A101D),
            border = BorderStroke(1.dp, primaryCol.copy(alpha = 0.65f)),
            modifier = Modifier.weight(1f, fill = false).testTag("vulkan_pbr_badge"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(Icons.Default.ViewInAr, contentDescription = null, tint = primaryCol, modifier = Modifier.size(14.dp))
              Text(
                text = gpuBackendLabel,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(Modifier.width(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC0A101D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
              .clickable {
                autoSpin = false
                yawVelocityDegPerSec = 0f
                targetCameraPreset = -28f to 16f
                modelZoomScale = 1.05f
                explodePct = 0f
              }
              .testTag("3d_reset_camera_btn"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset Camera", tint = Color.White, modifier = Modifier.size(14.dp))
              Text("RESET", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }
        }

        // Bottom live rig title & spec pills
        Column(
          modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            "${currentModel.name} (${packSpec.seriesCells}S ${packSpec.chemistry})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            "Orbit 360° • Fling • Pinch Zoom • ${Pev3dEngine.glbAssetFileName(archetype)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF93C5FD),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { PevSpecPills("DYNO", "$liveMph MPH ($liveRpm RPM)", primaryCol) }
            item { PevSpecPills("FULL V", "${"%.1f".format(packSpec.fullChargeV)}V", Color(0xFF22C55E)) }
            item { PevSpecPills("POWER", currentModel.powerKw, Color(0xFFFF6B35)) }
            item {
              PevSpecPills(
                label = "SSAO",
                value = if (ssaoEnabled) "HIGH • ON" else "OFF",
                accent = if (ssaoEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8),
              )
            }
          }
        }
      }

      Spacer(Modifier.height(6.dp))

      // Camera Angle Quick Presets (Smooth Animated Transitions)
      LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(
          listOf(
            "3/4 Hero" to (-28f to 16f),
            "Side" to (0f to 4f),
            "Front" to (90f to 8f),
            "Top" to (0f to 68f),
            "Low" to (-42f to 4f),
          )
        ) { (lbl, angles) ->
          val activeAngles = targetCameraPreset ?: (modelYawDeg to modelPitchDeg)
          val yawDelta = abs(((activeAngles.first - angles.first + 540f) % 360f) - 180f)
          FilterChip(
            selected = yawDelta < 8f && abs(activeAngles.second - angles.second) < 8f,
            onClick = {
              autoSpin = false
              yawVelocityDegPerSec = 0f
              targetCameraPreset = angles
            },
            label = { Text(lbl, fontSize = 10.sp, maxLines = 1, softWrap = false) },
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // INTERACTIVE 3D SUBSYSTEM TEARDOWN MATRIX (fixed horizontal layout — zero 1-char vertical wrapping!)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, primaryCol.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.TouchApp, contentDescription = null, tint = primaryCol, modifier = Modifier.size(16.dp))
              Text(
                "3D SUBSYSTEM TEARDOWN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = primaryCol,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              "${PEV_COMPONENT_PARTS.size} Systems",
              fontSize = 10.sp,
              color = Color.White.copy(alpha = 0.65f),
              maxLines = 1,
              softWrap = false,
            )
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PEV_COMPONENT_PARTS, key = { it.key }) { part ->
              val isSelected = selectedPart?.key == part.key
              Surface(
                onClick = { selectedPart = part },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) part.accent.copy(alpha = 0.25f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isSelected) part.accent else Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.testTag("part_chip_${part.key}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(part.icon, contentDescription = null, tint = part.accent, modifier = Modifier.size(16.dp))
                  Text(
                    part.name.split(" ").take(2).joinToString(" "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }

          selectedPart?.let { part ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF0A0F1D),
              border = BorderStroke(1.5.dp, part.accent.copy(alpha = 0.8f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Surface(shape = CircleShape, color = part.accent.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(part.icon, contentDescription = null, tint = part.accent, modifier = Modifier.size(18.dp))
                      }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        part.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        part.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = part.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = part.accent.copy(alpha = 0.2f)) {
                    Text(
                      "ACTIVE",
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Bold,
                      color = part.accent,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                  }
                }
                Text(part.summary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
                Text(part.deepDescription, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
                  Text(
                    text = "SPECS: ${part.specs}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(8.dp),
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(8.dp))

      // Interactive 3D Sliders (Yaw, Pitch, Zoom, Explode) + Live Dyno Throttle
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Yaw: ${modelYawDeg.toInt()}°", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          TacticalSlider(value = ((modelYawDeg + 360f) % 360f), onValueChange = { autoSpin = false; modelYawDeg = it }, valueRange = 0f..360f)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Pitch: ${modelPitchDeg.toInt()}°", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          TacticalSlider(value = modelPitchDeg, onValueChange = { modelPitchDeg = it }, valueRange = -65f..75f)
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Zoom: ${"%.2f".format(modelZoomScale)}x", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          TacticalSlider(value = modelZoomScale, onValueChange = { modelZoomScale = it }, valueRange = 0.65f..2.2f)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Explode 3D Parts: ${(explodePct * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          TacticalSlider(value = explodePct, onValueChange = { explodePct = it }, valueRange = 0f..1f)
        }
      }

      Spacer(Modifier.height(4.dp))
      Text(
        "Live Wheel Dyno Throttle: ${(motorThrottlePct * 100).roundToInt()}% (${liveMph} MPH • ${liveRpm} Wheel RPM)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
      )
      TacticalSlider(value = motorThrottlePct, onValueChange = { motorThrottlePct = it }, valueRange = 0f..1f)
    }
  }
}

@Composable
private fun PevSpecPills(label: String, value: String, accent: Color) {
  Surface(
    color = Color(0xCC111827),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, softWrap = false)
      Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = accent, maxLines = 1, softWrap = false)
    }
  }
}
