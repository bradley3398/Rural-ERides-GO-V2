package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.GeminiAiRepository
import com.example.core.LiveCloudServices
import com.example.core.PevBatteryPhysics
import com.example.data.WeatherMatrixData
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalSlider
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class RealWorldPevModel(
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

// Complete 88-rig real-world PEV catalog mapped across OpenGL ES 3.0 PBR, WebGL 2.0 GLB, Blueprint CAD, and 3D Vector Mesh
internal val AAA_PEV_MODELS =
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
    // +30 Expanded Real-World PEV 3D Blueprint Models (Total 88)
    model("Zero FXE Supermoto", "Electric Motorcycle", 85, 102, 105, "34.0 kW Peak", "Lightweight supermoto electric motorcycle with Showa suspension and Cypher II OS."),
    model("Zero DSR/X Adventure Moto", "Electric Motorcycle", 112, 102, 180, "75.0 kW Peak", "Dual-sport adventure electric motorcycle engineered for backcountry gravel and fire roads."),
    model("LiveWire S2 Del Mar", "Electric Motorcycle", 103, 350, 113, "63.0 kW Peak", "Flat-track inspired street tracker built on the modular ARROW structural battery chassis."),
    model("Energica Experia Tourer", "Electric Motorcycle", 112, 300, 160, "75.0 kW Peak", "Long-range Italian electric green-tourer with DC fast charging and Brembo calipers."),
    model("KTM Freeride E-XC", "Electric Dirt Bike", 55, 260, 45, "18.0 kW Peak", "Competition WP Xplor enduro dirt bike with quick-swap 260V power pack."),
    model("Arctic Leopard EX800 Enduro", "Electric Dirt Bike", 68, 88, 85, "28.0 kW Peak", "Long-range technical enduro electric dirt bike with direct-gearbox torque."),
    model("Rawrr Mantis 72V eMoto", "Electric Dirt Moped", 50, 72, 75, "8.0 kW Peak", "Agile aluminum-framed 72V electric trail bike with balanced center-of-gravity battery."),
    model("79Bike Falcon M 72V", "Electric Dirt Moped", 56, 72, 74, "10.0 kW Peak", "CNC billet link 72V electric trail dirt bike with fast-swap pack and Fastace forks."),
    model("Nami Klima Max 60V", "Hyper-Scooter", 42, 60, 50, "4.2 kW Peak", "Compact welded tubular racing scooter with KKE adjustable hydraulic coil shocks."),
    model("Dualtron Victor Luxury Plus", "Hyper-Scooter", 50, 60, 65, "4.3 kW Peak", "Refined urban-to-trail dual-motor scooter with EY4 telemetry and rubber cartridge suspension."),
    model("Kaabo Wolf King+ Pro", "Hyper-Scooter", 62, 72, 93, "8.4 kW Peak", "Twin-fork all-terrain touring hyper-scooter with expanded 72V 40Ah endurance pack."),
    model("WEPED Fold 2 GT", "Hyper-Scooter", 68, 84, 90, "14.0 kW Peak", "CNC-machined Korean flagship hyper-scooter with bespoke billet swingarms and twin lights."),
    model("Slack Core 920R Track", "Hyper-Scooter", 80, 84, 55, "18.0 kW Peak", "Competition billet racing scooter engineered for extreme track launch velocity."),
    model("Mukuta 10+ Dual-Motor", "Hyper-Scooter", 46, 60, 68, "5.0 kW Peak", "Quick-swap cartridge battery scooter featuring quad-arm twist suspension and turn signals."),
    model("NIU KQi 300X All-Terrain", "Commuter Scooter", 24, 48, 37, "1.0 kW Peak", "Dual-tube hydraulic suspension commuter tuned for gravel paths and rough pavement."),
    model("Begode Blitz 134V Racing EUC", "Electric Unicycle", 58, 134, 75, "3500W Nom", "Lightweight 134V 2400Wh competition trail & track unicycle with CNC linkage."),
    model("Begode Panther 168V Apex EUC", "Electric Unicycle", 74, 168, 115, "4500W Nom", "Ultra-high-voltage 168V 22-inch touring and speed flagship electric unicycle."),
    model("LeaperKim Oryx 176V Touring EUC", "Electric Unicycle", 70, 176, 145, "4200W Nom", "Grand-touring 176V heavy-duty suspension wheel with twin front bumper roll cages."),
    model("Kingsong F22 Pro 176V EUC", "Electric Unicycle", 62, 176, 100, "5500W Peak", "Four-corner quad-stanchion 176V long-travel suspension flagship EUC."),
    model("InMotion V11Y Suspension EUC", "Electric Unicycle", 37, 84, 75, "2500W Nom", "Refined air-spring daily & gravel cruiser featuring smart Raptor BMS telemetry."),
    model("Nosfet Apex 151V Suspension EUC", "Electric Unicycle", 62, 151, 85, "4200W Nom", "CNC center-frame 151V trail EUC with integrated kickstands and balanced twin packs."),
    model("Monday Motorbikes Anza V2", "Scrambler e-Bike", 35, 48, 50, "1.2 kW Peak", "Retro café-scrambler moped bike with hydraulic inverted fork and analog-digital gauge."),
    model("Goat Power Bikes Billy Goat V3", "Scrambler e-Bike", 52, 72, 75, "5.0 kW Peak", "72V dual-suspension electric moped-scrambler built for rural gravel speed."),
    model("Zooz Urban Ultralight 1200", "Scrambler e-Bike", 33, 52, 50, "1.8 kW Peak", "Chromoly BMX-inspired tubular street-scrambler with 52V downtube pack."),
    model("Santa Cruz Bullit CC X01 eMTB", "Mountain e-Bike", 20, 36, 55, "600W Peak", "Mixed-wheel 170mm travel carbon enduro e-MTB built for steep rugged descents."),
    model("Trek Rail 9.9 XX1 AXS eMTB", "Mountain e-Bike", 20, 36, 62, "600W Peak", "Long-travel OCLV carbon trail e-MTB with wireless drivetrain and RE:aktiv shock."),
    model("Yeti 160E T-Series Enduro", "Mountain e-Bike", 20, 36, 54, "600W Peak", "Sixfinity linkage carbon race enduro e-MTB with progressive backcountry damping."),
    model("Tern GSD S10 Cargo Utility", "Utility e-Bike", 20, 36, 68, "600W Peak", "Heavy-payload dual-battery compact cargo utility e-bike with reinforced rear rack."),
    model("Lift4 Carbon 5'4\" eFoil", "Electric Hydrofoil", 34, 52, 32, "5.0 kW Peak", "Whisper-quiet carbon fiber electric hydrofoil with foldable race prop and wings."),
    model("Waydoo Flyer EVO Pro Plus", "Electric Hydrofoil", 31, 52, 28, "6.0 kW Peak", "Modular EPP-carbon hydrofoil featuring pitch-stabilized mast and GPS telemetry."),
  )

/**
 * Multi-Technology, Multi-Service, Multi-Method 3D PEV CAD & Dyno Lab:
 * - Technologies: OpenGL ES 3.0 GLSL PBR, Chromium WebGL 2.0 GLB PBR, Compose 3D GPU Shaded Mesh
 * - Inspection Methods: 360° Touch Orbit, Hardware IMU Gyro Tilt, Automated Dyno Sweep, Exploded CAD Teardown
 * - Live Services: Gemini AI Chief Diagnostics, Live Web Spec Grounding, Open-Meteo Air Density & Dyno Cooling
 */
@Composable
fun Pev3dGarageViewer(
  activeVehicleName: String = "Hyper-PEV Flagship",
  activeVoltage: Double = 74.0,
  gpsLat: Double = 0.0,
  gpsLng: Double = 0.0,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val geminiRepo = remember { GeminiAiRepository() }

  val modelCategories = remember { listOf("All", "e-Moto & Dirt", "Hyper-Scooter", "EUC", "e-Bike & Moped", "Specialty") }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedModelIdx by remember { mutableIntStateOf(0) }
  var motorThrottlePct by remember { mutableFloatStateOf(0.35f) }

  // Technology & Method Selection States
  var activeTechEngine by remember { mutableStateOf(Pev3dTechEngine.OPENGL_ES3) }
  var inspectMethod by remember { mutableStateOf(Pev3dInspectMethod.ORBIT_TOUCH) }

  // 3D Interactive Camera & Animation States
  var modelYawDeg by remember { mutableFloatStateOf(-28f) }
  var modelPitchDeg by remember { mutableFloatStateOf(16f) }
  var modelZoomScale by remember { mutableFloatStateOf(1.05f) }
  var explodePct by remember { mutableFloatStateOf(0.0f) }
  var renderMode by remember { mutableStateOf(Pev3dRenderMode.SOLID) }
  var lightingPreset by remember { mutableStateOf(Pev3dLightingPreset.STUDIO) }
  var autoSpin by remember { mutableStateOf(false) }
  var lightsOn by remember { mutableStateOf(true) }
  var turnSignalMode by remember { mutableStateOf(Pev3dTurnSignalMode.OFF) }
  var wheelSpinDeg by remember { mutableFloatStateOf(0f) }
  var yawVelocityDegPerSec by remember { mutableFloatStateOf(0f) }
  var targetCameraPreset by remember { mutableStateOf<Pair<Float, Float>?>(null) }
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var gpuBackendLabel by remember { mutableStateOf("OPENGL ES • GGX PBR") }

  // Live Service Results (Zero Mocks — 100% Real API / AI / Weather Calls)
  var aiDiagBusy by remember { mutableStateOf(false) }
  var aiDiagOutput by remember { mutableStateOf("") }
  var webSpecBusy by remember { mutableStateOf(false) }
  var webSpecOutput by remember { mutableStateOf("") }
  var weatherBusy by remember { mutableStateOf(false) }
  var dynoWeather by remember { mutableStateOf<WeatherMatrixData?>(null) }
  var weatherStatusMsg by remember { mutableStateOf("") }

  // Interactive Part Selection
  var selectedPart by remember { mutableStateOf<PevComponentPart?>(PEV_COMPONENT_PARTS[0]) }

  val filteredModelIndices = remember(selectedCategory) {
    AAA_PEV_MODELS.indices.filter { idx ->
      selectedCategory == "All" || AAA_PEV_MODELS[idx].groupCategory == selectedCategory
    }
  }
  val hasImuSensor = remember(ctx) {
    val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ||
      sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
      sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
  }
  val availableInspectMethods = remember(hasImuSensor) {
    if (hasImuSensor) Pev3dInspectMethod.entries
    else Pev3dInspectMethod.entries.filter { it != Pev3dInspectMethod.GYRO_TILT }
  }
  val currentModel = AAA_PEV_MODELS[selectedModelIdx.coerceIn(0, AAA_PEV_MODELS.lastIndex)]
  val archetype = remember(currentModel.name, currentModel.category) {
    Pev3dEngine.archetypeForCategory("${currentModel.name} ${currentModel.category}")
  }
  val packSpec = remember(currentModel.voltage) { PevBatteryPhysics.packProfile(currentModel.voltage.toDouble(), "NMC") }
  val finish = remember(currentModel) {
    Pev3dEngine.finishForModel(currentModel.name, currentModel.category, currentModel.voltage)
  }

  // Real Android Hardware SensorManager for GYRO_TILT Inspection Method
  DisposableEffect(inspectMethod) {
    if (inspectMethod != Pev3dInspectMethod.GYRO_TILT) {
      onDispose { }
    } else {
      val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      val listener = object : SensorEventListener {
        private val rotMat = FloatArray(9)
        private val orient = FloatArray(3)
        private var baseAzimuthDeg: Float? = null

        override fun onSensorChanged(event: SensorEvent) {
          when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
              SensorManager.getRotationMatrixFromVector(rotMat, event.values)
              SensorManager.getOrientation(rotMat, orient)
              val azDeg = Math.toDegrees(orient[0].toDouble()).toFloat()
              val pitchRad = orient[1]
              if (baseAzimuthDeg == null) baseAzimuthDeg = azDeg
              val deltaYaw = azDeg - (baseAzimuthDeg ?: azDeg)
              modelYawDeg = ((-28f + deltaYaw * 1.4f) + 720f) % 360f
              modelPitchDeg = (Math.toDegrees(-pitchRad.toDouble()).toFloat() * 0.7f).coerceIn(-45f, 65f)
            }
            Sensor.TYPE_ACCELEROMETER -> {
              val ax = event.values.getOrElse(0) { 0f }
              val ay = event.values.getOrElse(1) { 0f }
              modelYawDeg = ((-28f + ax * 14f) + 720f) % 360f
              modelPitchDeg = (ay * 6f).coerceIn(-45f, 65f)
            }
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }
      if (sm != null && rotSensor != null) {
        sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      onDispose {
        sm?.unregisterListener(listener)
      }
    }
  }

  // Handle TEARDOWN_EXPLODE & DYNO_SWEEP Inspection Method transitions
  LaunchedEffect(inspectMethod) {
    when (inspectMethod) {
      Pev3dInspectMethod.TEARDOWN_EXPLODE -> {
        if (explodePct < 0.25f) explodePct = 0.78f
      }
      Pev3dInspectMethod.ORBIT_TOUCH -> {
        // leave current explode/throttle intact for manual exploration
      }
      else -> Unit
    }
  }

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

  // 60fps wheel rotation + automated Dyno Sweep + inertial fling spin + smooth camera presets
  LaunchedEffect(autoSpin, motorThrottlePct, inspectMethod) {
    var lastNs = 0L
    var sweepClock = 0f
    while (true) {
      withFrameNanos { ns ->
        if (lastNs != 0L) {
          val dt = ((ns - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
          if (inspectMethod == Pev3dInspectMethod.DYNO_SWEEP) {
            sweepClock += dt * 1.15f
            motorThrottlePct = (0.5f + 0.48f * sin(sweepClock)).coerceIn(0.02f, 1.0f)
          }
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
          } else if (autoSpin && inspectMethod != Pev3dInspectMethod.GYRO_TILT) {
            modelYawDeg = (modelYawDeg + dt * 28f) % 360f
            yawVelocityDegPerSec = 0f
          } else if (abs(yawVelocityDegPerSec) > 0.8f && inspectMethod != Pev3dInspectMethod.GYRO_TILT) {
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
  val ambientTempC = dynoWeather?.tempC?.toFloat() ?: 22f
  val airDensityKgM3 = dynoWeather?.let { w ->
    val tempK = (w.tempC + 273.15).coerceAtLeast(220.0)
    (w.pressureHpa * 100.0) / (287.05 * tempK)
  } ?: 1.225
  val estMotorTempC = (ambientTempC + motorThrottlePct * 68f).roundToInt()
  val primaryCol = MaterialTheme.colorScheme.primary

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 10.dp, vertical = 8.dp),
  ) {
    TacticalCard(highlight = true, title = "MULTI-TECH 3D RIG LAB (${AAA_PEV_MODELS.size} MODELS)") {
      // 1. 3D RENDERING TECHNOLOGY SWITCHER
      Text(
        "3D RENDERING TECHNOLOGY",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(4.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Pev3dTechEngine.entries) { tech ->
          FilterChip(
            selected = activeTechEngine == tech,
            onClick = {
              activeTechEngine = tech
              gpuBackendLabel = when (tech) {
                Pev3dTechEngine.OPENGL_ES3 -> "OPENGL ES • GGX PBR"
                Pev3dTechEngine.WEBGL_2 -> "WEBGL 2.0 • GLB PBR"
                Pev3dTechEngine.VECTOR_CAD -> "COMPOSE 3D • GPU MESH"
              }
            },
            label = {
              Column {
                Text(tech.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(tech.subtitle, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF0EA5E9).copy(alpha = 0.22f),
              selectedLabelColor = Color(0xFF38BDF8),
            ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("3d_tech_${tech.name.lowercase()}"),
          )
        }
      }

      Spacer(Modifier.height(6.dp))

      // 2. INSPECTION METHOD SWITCHER
      Text(
        "INSPECTION METHOD",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.height(4.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(availableInspectMethods) { method ->
          val icon = when (method) {
            Pev3dInspectMethod.ORBIT_TOUCH -> Icons.Default.TouchApp
            Pev3dInspectMethod.GYRO_TILT -> Icons.Default.Explore
            Pev3dInspectMethod.DYNO_SWEEP -> Icons.Default.Speed
            Pev3dInspectMethod.TEARDOWN_EXPLODE -> Icons.Default.Layers
          }
          FilterChip(
            selected = inspectMethod == method,
            onClick = { inspectMethod = method },
            leadingIcon = { Icon(icon, null, Modifier.size(14.dp)) },
            label = { Text(method.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFA855F7).copy(alpha = 0.24f),
              selectedLabelColor = Color(0xFFD8B4FE),
            ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("3d_method_${method.name.lowercase()}"),
          )
        }
      }

      Spacer(Modifier.height(6.dp))

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

      // Shader Mode + Lighting + Optics Controls (supported across all 3 realistic 3D engines)
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

      // MULTI-TECHNOLOGY 3D HARDWARE VIEWPORT CONTAINER
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0E1117), Color(0xFF141820), Color(0xFF0A0C10))))
            .onSizeChanged { viewportSize = it }
            .pointerInput(selectedModelIdx, renderMode, activeTechEngine) {
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
                if (totalMoved < 16f) {
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
        when (activeTechEngine) {
          Pev3dTechEngine.OPENGL_ES3 -> {
            Pev3dOpenGlViewport(
              archetype = archetype,
              renderMode = renderMode,
              lightingPreset = lightingPreset,
              yawDeg = modelYawDeg,
              pitchDeg = modelPitchDeg,
              zoom = modelZoomScale,
              explodePct = explodePct,
              wheelSpinDeg = wheelSpinDeg,
              throttlePct = motorThrottlePct,
              lightsOn = lightsOn,
              turnSignalMode = turnSignalMode,
              selectedPartKey = selectedPart?.key,
              accentRgb = finish.accentRgb,
              bodyRgb = finish.bodyRgb,
              caliperRgb = finish.caliperRgb,
              ambientTempC = ambientTempC,
              onBackendChanged = { gpuBackendLabel = it },
              modifier = Modifier.fillMaxSize().testTag("pev_3d_interactive_canvas"),
              finish = finish,
            )
          }
          Pev3dTechEngine.WEBGL_2 -> {
            Pev3dWebGlViewport(
              archetype = archetype,
              renderMode = renderMode,
              lightingPreset = lightingPreset,
              yawDeg = modelYawDeg,
              pitchDeg = modelPitchDeg,
              zoom = modelZoomScale,
              explodePct = explodePct,
              wheelSpinDeg = wheelSpinDeg,
              throttlePct = motorThrottlePct,
              lightsOn = lightsOn,
              turnSignalMode = turnSignalMode,
              selectedPartKey = selectedPart?.key,
              accentRgb = finish.accentRgb,
              bodyRgb = finish.bodyRgb,
              caliperRgb = finish.caliperRgb,
              ambientTempC = ambientTempC,
              onBackendChanged = { gpuBackendLabel = it },
              modifier = Modifier.fillMaxSize().testTag("pev_3d_interactive_canvas"),
              finish = finish,
            )
          }
          Pev3dTechEngine.VECTOR_CAD -> {
            Pev3dComposeShadedViewport(
              archetype = archetype,
              renderMode = renderMode,
              lightingPreset = lightingPreset,
              yawDeg = modelYawDeg,
              pitchDeg = modelPitchDeg,
              zoom = modelZoomScale,
              explodePct = explodePct,
              wheelSpinDeg = wheelSpinDeg,
              throttlePct = motorThrottlePct,
              lightsOn = lightsOn,
              turnSignalMode = turnSignalMode,
              selectedPartKey = selectedPart?.key,
              accentRgb = finish.accentRgb,
              bodyRgb = finish.bodyRgb,
              caliperRgb = finish.caliperRgb,
              ambientTempC = ambientTempC,
              modifier = Modifier.fillMaxSize().testTag("pev_3d_interactive_canvas"),
              finish = finish,
            )
          }
        }

        // Interactive 3D Hotspot Pins
        Canvas(modifier = Modifier.fillMaxSize()) {
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

        // Top non-overlapping bar: Left Active Technology Badge + Right RESET pill
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
            modifier = Modifier.weight(1f, fill = false).testTag("engine_pbr_badge"),
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
                inspectMethod = Pev3dInspectMethod.ORBIT_TOUCH
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
        val blueprintSpecs = remember(finish.blueprintType) { Pev3dEngine.blueprintSpecsFor(finish.blueprintType) }
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
            "${finish.blueprintType.shortLabel} • ${blueprintSpecs.wheelProfile} • ${activeTechEngine.label}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF93C5FD),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { PevSpecPills("BLUEPRINT", finish.blueprintType.chassisClass, Color(0xFF22D3EE)) }
            item { PevSpecPills("DYNO", "$liveMph MPH ($liveRpm RPM)", primaryCol) }
            item { PevSpecPills("TEMP IR", "${estMotorTempC}°C", Color(0xFFFF6B35)) }
            item { PevSpecPills("FULL V", "${"%.1f".format(packSpec.fullChargeV)}V", Color(0xFF22C55E)) }
            item { PevSpecPills("POWER", currentModel.powerKw, Color(0xFF38BDF8)) }
            item {
              PevSpecPills(
                label = "AIR",
                value = "${"%.2f".format(airDensityKgM3)} kg/m³",
                accent = Color(0xFFA855F7),
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
              if (inspectMethod == Pev3dInspectMethod.GYRO_TILT) inspectMethod = Pev3dInspectMethod.ORBIT_TOUCH
              targetCameraPreset = angles
            },
            label = { Text(lbl, fontSize = 10.sp, maxLines = 1, softWrap = false) },
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // INTERACTIVE 3D SUBSYSTEM TEARDOWN MATRIX
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, primaryCol.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.TouchApp, contentDescription = null, tint = primaryCol, modifier = Modifier.size(16.dp))
              Text(
                "3D SUBSYSTEM TEARDOWN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = primaryCol,
              )
            }
            Text(
              "${PEV_COMPONENT_PARTS.size} Systems",
              fontSize = 10.sp,
              color = Color.White.copy(alpha = 0.65f),
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
                modifier = Modifier.heightIn(min = 48.dp).testTag("part_chip_${part.key}"),
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
                  horizontalArrangement = Arrangement.SpaceBetween,
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
                    Column {
                      Text(part.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
                      Text(part.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = part.accent)
                    }
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
        "Live Wheel Dyno Throttle: ${(motorThrottlePct * 100).roundToInt()}% (${liveMph} MPH • ${liveRpm} Wheel RPM • Est Stator ${estMotorTempC}°C)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
      )
      TacticalSlider(value = motorThrottlePct, onValueChange = { motorThrottlePct = it }, valueRange = 0f..1f)
    }

    // 3. LIVE CLOUD & AI SERVICES MATRIX FOR 3D LAB (ZERO MOCKS)
    TacticalCard(highlight = false, title = "LIVE 3D SERVICES & CLOUD GROUNDING") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // SERVICE 1: GEMINI AI ENGINEERING DIAGNOSTICS
          Button(
            onClick = {
              val part = selectedPart ?: PEV_COMPONENT_PARTS.first()
              aiDiagBusy = true
              scope.launch {
                val res = geminiRepo.analyze3dSubsystemDiagnostics(
                  vehicleName = currentModel.name,
                  voltage = currentModel.voltage,
                  partName = part.name,
                  partSpecs = part.specs,
                  throttlePct = (motorThrottlePct * 100).roundToInt(),
                  ambientTempC = ambientTempC.toDouble(),
                )
                aiDiagOutput = res.getOrElse { err -> "AI Service Error: ${err.message}" }
                aiDiagBusy = false
              }
            },
            enabled = !aiDiagBusy,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("3d_service_gemini_btn"),
          ) {
            if (aiDiagBusy) {
              CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
              Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text("Gemini Chief", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
          }

          // SERVICE 2: LIVE WEB SPEC SEARCH
          OutlinedButton(
            onClick = {
              webSpecBusy = true
              scope.launch {
                val partTitle = selectedPart?.category.orEmpty()
                val raw = LiveCloudServices.webSearchGrounding(
                  "${currentModel.name} ${currentModel.voltage}V $partTitle specs schematic",
                  deepMode = false,
                )
                webSpecOutput = raw.ifBlank { "No web search results returned right now." }
                webSpecBusy = false
              }
            },
            enabled = !webSpecBusy,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("3d_service_webspec_btn"),
          ) {
            if (webSpecBusy) {
              CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Public, null, Modifier.size(15.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text("Web Specs", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
          }

          // SERVICE 3: OPEN-METEO DYNO AIR DENSITY & THERMAL CALIBRATION
          OutlinedButton(
            onClick = {
              if (gpsLat == 0.0 && gpsLng == 0.0) {
                weatherStatusMsg = "GPS lock required on Cockpit/Radar for live Open-Meteo dyno air density."
                return@OutlinedButton
              }
              weatherBusy = true
              weatherStatusMsg = ""
              scope.launch {
                val res = LiveCloudServices.fetchWeatherMatrix(gpsLat, gpsLng)
                res.onSuccess { w ->
                  dynoWeather = w
                  weatherStatusMsg = "${w.locationLabel}: ${"%.1f".format(w.tempC)}°C • ${"%.0f".format(w.pressureHpa)} hPa • Wind ${"%.0f".format(w.windSpeedKmh)} km/h"
                }.onFailure { err ->
                  weatherStatusMsg = "Weather sync error: ${err.message}"
                }
                weatherBusy = false
              }
            },
            enabled = !weatherBusy,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("3d_service_weather_btn"),
          ) {
            if (weatherBusy) {
              CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Air, null, Modifier.size(15.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text("Dyno Air", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
          }
        }

        if (weatherStatusMsg.isNotBlank()) {
          Surface(
            color = Color(0xFF0E1B2B),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Thermostat, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
              Text(
                "$weatherStatusMsg | Air Density: ${"%.3f".format(airDensityKgM3)} kg/m³ (Feeds Thermal IR Shader)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE0F2FE),
              )
            }
          }
        }

        if (aiDiagOutput.isNotBlank()) {
          Surface(
            color = Color(0xFF170F2B),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFD8B4FE), modifier = Modifier.size(15.dp))
                Text("GEMINI AI ENGINEERING TEARDOWN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFD8B4FE))
              }
              Text(aiDiagOutput, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
          }
        }

        if (webSpecOutput.isNotBlank()) {
          Surface(
            color = Color(0xFF0C1814),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CloudSync, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(15.dp))
                Text("LIVE WEB SPEC GROUNDING", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF4ADE80))
              }
              Text(webSpecOutput.take(900), style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
            }
          }
        }
      }
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
