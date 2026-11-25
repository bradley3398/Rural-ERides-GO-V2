package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class Pev3dArchetype {
  EMOTO,
  SCOOTER,
  EUC,
  EBIKE,
  HYDROFOIL,
}

enum class Pev3dBlueprintType(
  val shortLabel: String,
  val chassisClass: String,
) {
  EMOTO_MX_DIRT("MX Dirt Enduro", "Perimeter + High Beak"),
  EMOTO_STREET_NAKED("Street / Café Moto", "Sculpted Tank + Hugger"),
  EBIKE_SCRAMBLER_MOPED("Scrambler Moped", "Moto Halo + Fat 20x4"),
  EBIKE_MOUNTAIN_FULLSUSP("Enduro e-MTB", "Diamond 29er + Horst Link"),
  EBIKE_COMPACT_FOLDING("Utility / Folding", "Step-Thru + Rear Rack"),
  SCOOTER_DUAL_STEM_OFFROAD("Dual-Stem Off-Road", "Twin Fork + Crash Bars"),
  SCOOTER_TUBULAR_RACING("Tubular Racing", "Trellis Neck + C-Arm"),
  SCOOTER_MONOCOQUE_AERO("Aero Monocoque", "Sculpted Fairing + LED"),
  EUC_ARMORED_ROLLCAGE("Armored Roll-Cage", "Twin Bumper + Top Seat"),
  EUC_AERO_TRAIL("Aero Suspension", "Molded Shell + Power Pads"),
  HYDROFOIL_EFOIL("Carbon eFoil", "Hydrofoil Mast + Wings"),
}

data class Pev3dBlueprintSpecs(
  val blueprintType: Pev3dBlueprintType,
  val wheelbaseMm: Int,
  val suspensionTravelMm: Int,
  val wheelProfile: String,
)

enum class Pev3dRenderMode(val label: String, val shaderId: Int) {
  SOLID("PBR Solid", 0),
  CARBON_SHOWROOM("Carbon Gloss", 3),
  THERMAL_IR("Thermal IR", 4),
  WIREFRAME("CAD Wire", 1),
  XRAY("X-Ray Shader", 2),
  BLUEPRINT("Blueprint CAD", 5),
}

enum class Pev3dTechEngine(val label: String, val subtitle: String) {
  FILAMENT_GL("Filament GL-PBR", "OpenGL ES PBR + IBL + Clearcoat"),
  OPENGL_ES3("OpenGL ES", "Custom GGX + Normal Maps"),
  WEBGL_2("WebGL 2.0 GLB", "Chromium PBR + Clearcoat"),
  VECTOR_CAD("Compose 3D", "GPU Shaded Triangle Mesh"),
}

internal sealed interface Pev3dAssetLoadState {
  data object Loading : Pev3dAssetLoadState
  data class Ready(val assetName: String, val envName: String) : Pev3dAssetLoadState
  data class MissingGlb(val expectedAssetPath: String) : Pev3dAssetLoadState
  data class EngineUnavailable(val reason: String) : Pev3dAssetLoadState
}

enum class Pev3dInspectMethod(val label: String) {
  ORBIT_TOUCH("Touch Orbit"),
  GYRO_TILT("Gyro Tilt"),
  DYNO_SWEEP("Dyno Sweep"),
  TEARDOWN_EXPLODE("Explode CAD"),
}

enum class Pev3dTurnSignalMode(val label: String) {
  OFF("Signals Off"),
  LEFT("Left <"),
  RIGHT("Right >"),
  HAZARD("Hazards"),
}

enum class Pev3dLightingPreset(val label: String) {
  STUDIO("Studio Key"),
  SUNSET("Golden Dusk"),
  NEON("Cyber Neon"),
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
  operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
  operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
  operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
}

data class Pev3dHotspot(
  val partKey: String,
  val label: String,
  val worldPos: Vec3,
  val explodeDir: Vec3,
  val accent: Color,
)

data class ProjectedHotspot(
  val partKey: String,
  val label: String,
  val screenPos: Offset,
  val depth: Float,
  val accent: Color,
)

data class Pev3dModelFinish(
  val bodyRgb: FloatArray,
  val accentRgb: FloatArray,
  val caliperRgb: FloatArray,
  val wheelScale: Float = 1.0f,
  val bodyLengthScale: Float = 1.0f,
  val secondaryRgb: FloatArray = floatArrayOf(0.24f, 0.26f, 0.29f),
  val paintMetallic: Float = 0.52f,
  val blueprintType: Pev3dBlueprintType = Pev3dBlueprintType.EMOTO_MX_DIRT,
)

/**
 * Pure Kotlin column-major 4x4 matrix math (zero android.opengl dependencies)
 * using right-handed camera and transform conventions across all 3D engines.
 */
internal object Mat4Math {
  fun setIdentity(m: FloatArray) {
    m.fill(0f)
    m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
  }

  fun translate(m: FloatArray, tx: Float, ty: Float, tz: Float) {
    for (i in 0 until 4) {
      m[12 + i] += m[i] * tx + m[4 + i] * ty + m[8 + i] * tz
    }
  }

  fun scale(m: FloatArray, sx: Float, sy: Float, sz: Float) {
    for (i in 0 until 4) {
      m[i] *= sx
      m[4 + i] *= sy
      m[8 + i] *= sz
    }
  }

  fun rotateAxis(m: FloatArray, angleDeg: Float, ax: Float, ay: Float, az: Float) {
    val len = sqrt(ax * ax + ay * ay + az * az)
    if (len < 1e-6f) return
    val x = ax / len
    val y = ay / len
    val z = az / len
    val rad = (angleDeg * PI / 180.0).toFloat()
    val c = cos(rad)
    val s = sin(rad)
    val nc = 1f - c
    val r = FloatArray(16)
    r[0] = x * x * nc + c
    r[1] = y * x * nc + z * s
    r[2] = x * z * nc - y * s
    r[3] = 0f
    r[4] = x * y * nc - z * s
    r[5] = y * y * nc + c
    r[6] = y * z * nc + x * s
    r[7] = 0f
    r[8] = x * z * nc + y * s
    r[9] = y * z * nc - x * s
    r[10] = z * z * nc + c
    r[11] = 0f
    r[15] = 1f
    val tmp = FloatArray(16)
    multiplyMM(tmp, m, r)
    System.arraycopy(tmp, 0, m, 0, 16)
  }

  fun perspective(out: FloatArray, fovYDeg: Float, aspect: Float, near: Float, far: Float) {
    val f = 1f / tan((fovYDeg * PI / 360.0).toFloat())
    val rangeInv = 1f / (near - far)
    out.fill(0f)
    out[0] = f / aspect
    out[5] = f
    out[10] = (far + near) * rangeInv
    out[11] = -1f
    out[14] = 2f * far * near * rangeInv
  }

  fun lookAt(
    out: FloatArray,
    eyeX: Float, eyeY: Float, eyeZ: Float,
    centerX: Float, centerY: Float, centerZ: Float,
    upX: Float, upY: Float, upZ: Float,
  ) {
    var fx = centerX - eyeX
    var fy = centerY - eyeY
    var fz = centerZ - eyeZ
    val rlf = 1f / sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
    fx *= rlf; fy *= rlf; fz *= rlf

    var sx = fy * upZ - fz * upY
    var sy = fz * upX - fx * upZ
    var sz = fx * upY - fy * upX
    val rls = 1f / sqrt(sx * sx + sy * sy + sz * sz).coerceAtLeast(1e-6f)
    sx *= rls; sy *= rls; sz *= rls

    val ux = sy * fz - sz * fy
    val uy = sz * fx - sx * fz
    val uz = sx * fy - sy * fx

    out[0] = sx; out[4] = sy; out[8] = sz; out[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
    out[1] = ux; out[5] = uy; out[9] = uz; out[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
    out[2] = -fx; out[6] = -fy; out[10] = -fz; out[14] = fx * eyeX + fy * eyeY + fz * eyeZ
    out[3] = 0f; out[7] = 0f; out[11] = 0f; out[15] = 1f
  }

  fun multiplyMM(out: FloatArray, lhs: FloatArray, rhs: FloatArray) {
    for (col in 0 until 4) {
      val c = col * 4
      val r0 = rhs[c]
      val r1 = rhs[c + 1]
      val r2 = rhs[c + 2]
      val r3 = rhs[c + 3]
      for (row in 0 until 4) {
        out[c + row] =
          lhs[row] * r0 +
          lhs[4 + row] * r1 +
          lhs[8 + row] * r2 +
          lhs[12 + row] * r3
      }
    }
  }

  fun multiplyMV(out: FloatArray, m: FloatArray, v: FloatArray) {
    val x = v[0]; val y = v[1]; val z = v[2]; val w = v[3]
    out[0] = m[0] * x + m[4] * y + m[8] * z + m[12] * w
    out[1] = m[1] * x + m[5] * y + m[9] * z + m[13] * w
    out[2] = m[2] * x + m[6] * y + m[10] * z + m[14] * w
    out[3] = m[3] * x + m[7] * y + m[11] * z + m[15] * w
  }
}

internal object Pev3dEngine {
  fun archetypeForCategory(category: String): Pev3dArchetype {
    val c = category.lowercase()
    return when {
      c.contains("unicycle") || c.contains("euc") -> Pev3dArchetype.EUC
      c.contains("scooter") -> Pev3dArchetype.SCOOTER
      c.contains("hydrofoil") || c.contains("efoil") -> Pev3dArchetype.HYDROFOIL
      c.contains("dirt") || c.contains("moto") || c.contains("enduro") || c.contains("mx") ||
        c.contains("sur-ron") || c.contains("surron") || c.contains("talaria") || c.contains("stark") ->
        Pev3dArchetype.EMOTO
      c.contains("e-bike") || c.contains("ebike") || c.contains("moped") || c.contains("scrambler") || c.contains("folding") ->
        Pev3dArchetype.EBIKE
      else -> Pev3dArchetype.EMOTO
    }
  }

  fun glbAssetFileName(archetype: Pev3dArchetype): String =
    when (archetype) {
      Pev3dArchetype.EMOTO -> "emoto.glb"
      Pev3dArchetype.SCOOTER -> "hyperscooter.glb"
      Pev3dArchetype.EBIKE -> "ebike_fat.glb"
      Pev3dArchetype.EUC -> "euc.glb"
      Pev3dArchetype.HYDROFOIL -> "hydrofoil.glb"
    }

  fun resolveBlueprint(name: String, category: String): Pev3dBlueprintType {
    val n = name.lowercase()
    val c = category.lowercase()
    val arch = archetypeForCategory("$name $category")
    return when (arch) {
      Pev3dArchetype.HYDROFOIL -> Pev3dBlueprintType.HYDROFOIL_EFOIL
      Pev3dArchetype.EUC -> {
        if (n.contains("sherman") || n.contains("patton") || n.contains("lynx") || n.contains("v13") || n.contains("leaperkim")) {
          Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE
        } else {
          Pev3dBlueprintType.EUC_AERO_TRAIL
        }
      }
      Pev3dArchetype.SCOOTER -> {
        when {
          n.contains("kaabo") || n.contains("wolf") || n.contains("dualtron") || n.contains("zero 11") ->
            Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD
          n.contains("nami") || n.contains("rion") || n.contains("vsett") || n.contains("teverun") ->
            Pev3dBlueprintType.SCOOTER_TUBULAR_RACING
          else -> Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO
        }
      }
      Pev3dArchetype.EBIKE -> {
        when {
          n.contains("turbo levo") || c.contains("mountain") ->
            Pev3dBlueprintType.EBIKE_MOUNTAIN_FULLSUSP
          n.contains("lectric") || n.contains("rad") || c.contains("folding") || c.contains("utility") ->
            Pev3dBlueprintType.EBIKE_COMPACT_FOLDING
          else -> Pev3dBlueprintType.EBIKE_SCRAMBLER_MOPED
        }
      }
      Pev3dArchetype.EMOTO -> {
        if (n.contains("sondors") || n.contains("metacycle") || n.contains("ryvid") || n.contains("bmw") ||
          n.contains("cake") || c.contains("motorcycle")
        ) {
          Pev3dBlueprintType.EMOTO_STREET_NAKED
        } else {
          Pev3dBlueprintType.EMOTO_MX_DIRT
        }
      }
    }
  }

  fun blueprintSpecsFor(type: Pev3dBlueprintType): Pev3dBlueprintSpecs =
    when (type) {
      Pev3dBlueprintType.EMOTO_MX_DIRT -> Pev3dBlueprintSpecs(type, 1380, 220, "19\"/18\" MX Knobby")
      Pev3dBlueprintType.EMOTO_STREET_NAKED -> Pev3dBlueprintSpecs(type, 1410, 145, "17\" Street Sport")
      Pev3dBlueprintType.EBIKE_SCRAMBLER_MOPED -> Pev3dBlueprintSpecs(type, 1280, 120, "20x4.0\" Fat All-Terrain")
      Pev3dBlueprintType.EBIKE_MOUNTAIN_FULLSUSP -> Pev3dBlueprintSpecs(type, 1245, 160, "29\"/27.5\" Trail Mullet")
      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> Pev3dBlueprintSpecs(type, 1120, 80, "20x3.0\" Utility Cargo")
      Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD -> Pev3dBlueprintSpecs(type, 1160, 165, "11\" Deep-Lug Off-Road")
      Pev3dBlueprintType.SCOOTER_TUBULAR_RACING -> Pev3dBlueprintSpecs(type, 1190, 160, "11\"/13\" PMT Track Slick")
      Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO -> Pev3dBlueprintSpecs(type, 1140, 125, "11\" Self-Sealing Aero")
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE -> Pev3dBlueprintSpecs(type, 0, 90, "20\"/22\" Armored Trail")
      Pev3dBlueprintType.EUC_AERO_TRAIL -> Pev3dBlueprintSpecs(type, 0, 130, "16\"/20\" Linkage Trail")
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> Pev3dBlueprintSpecs(type, 0, 0, "4'9\" Carbon Hydrofoil")
    }

  fun finishForModel(name: String, category: String, voltage: Int): Pev3dModelFinish {
    val n = name.lowercase()
    val c = category.lowercase()
    val blueprint = resolveBlueprint(name, category)
    // Authentic OEM automotive/motorcycle sRGB paint palettes (converted to linear for GLTF PBR)
    val bodyRgb =
      when {
        n.contains("rion") || n.contains("stealth") || n.contains("black") || n.contains("dualtron") || n.contains("nami") || n.contains("weped") || n.contains("zooz") ->
          floatArrayOf(0.18f, 0.19f, 0.22f) // Obsidian Metallic Carbon
        n.contains("stark") || n.contains("red") || n.contains("ducati") || n.contains("zero 11") || n.contains("altis") || n.contains("goat") || n.contains("energica") ->
          floatArrayOf(0.70f, 0.10f, 0.11f) // Factory Corsa Metallic Red
        n.contains("bmw") || n.contains("ryvid") || n.contains("apollo") || n.contains("white") || n.contains("lectric") || n.contains("livewire") || n.contains("rawrr") ->
          floatArrayOf(0.84f, 0.85f, 0.87f) // Mineral Pearl White
        n.contains("ultra bee") || n.contains("kaabo") || n.contains("green") || n.contains("aventon") || n.contains("vsett") || n.contains("santa cruz") || n.contains("tern") ->
          floatArrayOf(0.20f, 0.34f, 0.23f) // Satin Forest Olive Metallic
        n.contains("storm bee") || n.contains("talaria") || n.contains("amber") || n.contains("ktm") || n.contains("komodo") || n.contains("arctic") || n.contains("waydoo") ->
          floatArrayOf(0.82f, 0.31f, 0.09f) // Factory Moto Burnt Orange
        n.contains("fliteboard") || n.contains("inmotion") || n.contains("blue") || n.contains("eride") || n.contains("e-ride") || n.contains("lift") || n.contains("yeti") ->
          floatArrayOf(0.11f, 0.27f, 0.44f) // Deep Marine Metallic Navy / Turquoise
        n.contains("sherman") || n.contains("patton") || n.contains("lynx") || n.contains("veteran") || n.contains("teverun") || n.contains("oryx") || n.contains("nosfet") ->
          floatArrayOf(0.26f, 0.28f, 0.32f) // Forged Titanium Graphite
        n.contains("onyx") || n.contains("cake") || n.contains("volcon") || n.contains("super73") || n.contains("revv") || n.contains("anza") || n.contains("zero") ->
          floatArrayOf(0.56f, 0.44f, 0.28f) // Matte Desert Bronze / Satin Copper
        n.contains("light bee") || n.contains("sur-ron") || n.contains("surron") || n.contains("segway") || n.contains("niu") || n.contains("79bike") ->
          floatArrayOf(0.24f, 0.27f, 0.31f) // Satin Gunmetal Slate
        n.contains("begode") || n.contains("kingsong") || n.contains("slack") || n.contains("trek") || n.contains("mukuta") ->
          floatArrayOf(0.22f, 0.24f, 0.28f) // Forged Basalt Slate
        voltage >= 100 ->
          floatArrayOf(0.21f, 0.24f, 0.29f) // Anthracite Metallic
        else ->
          floatArrayOf(0.22f, 0.30f, 0.40f) // Deep Steel Metallic Blue
      }
    // Realistic anodized CNC forks, shock springs, triple clamps, and hub trim
    val accentRgb =
      when {
        n.contains("ultra bee") || n.contains("lynx") || n.contains("sherman") || n.contains("patton") || n.contains("cake") || n.contains("sur-ron") || n.contains("vsett") ->
          floatArrayOf(0.80f, 0.64f, 0.28f) // Ohlins Champagne Anodized Gold
        n.contains("stark") || n.contains("dualtron") || n.contains("nami") || n.contains("kaabo") || n.contains("altis") || n.contains("teverun") ->
          floatArrayOf(0.74f, 0.16f, 0.16f) // Anodized Factory Crimson
        n.contains("bmw") || n.contains("ryvid") || n.contains("storm") || n.contains("talaria") || n.contains("inmotion") ->
          floatArrayOf(0.82f, 0.40f, 0.14f) // Anodized Burnt Copper
        n.contains("fliteboard") || n.contains("segway") || n.contains("lectric") || n.contains("aventon") ->
          floatArrayOf(0.74f, 0.78f, 0.82f) // Brushed Marine Aluminum
        else ->
          floatArrayOf(0.76f, 0.62f, 0.30f) // CNC Titanium Gold
      }
    val caliperRgb =
      when {
        n.contains("stark") || n.contains("dualtron") || n.contains("nami") || n.contains("rion") || n.contains("altis") ->
          floatArrayOf(0.78f, 0.12f, 0.14f) // Brembo Racing Red
        n.contains("lynx") || n.contains("storm") || n.contains("ultra bee") || n.contains("sherman") || n.contains("eride") ->
          floatArrayOf(0.80f, 0.63f, 0.24f) // Brembo Anodized Gold
        n.contains("bmw") || n.contains("fliteboard") || n.contains("lectric") ->
          floatArrayOf(0.32f, 0.35f, 0.40f) // Billet Titanium Slate
        else ->
          floatArrayOf(0.74f, 0.14f, 0.15f) // Anodized Red Caliper
      }
    val secondaryRgb =
      when {
        n.contains("bmw") || n.contains("ryvid") || n.contains("lectric") -> floatArrayOf(0.22f, 0.24f, 0.28f)
        n.contains("stark") || n.contains("talaria") -> floatArrayOf(0.20f, 0.21f, 0.24f)
        else -> floatArrayOf(0.25f, 0.27f, 0.30f)
      }
    val paintMetallic =
      when {
        n.contains("onyx") || n.contains("cake") || n.contains("volcon") || n.contains("revv") -> 0.34f
        n.contains("bmw") || n.contains("stark") || n.contains("fliteboard") || n.contains("altis") -> 0.68f
        else -> 0.54f
      }
    val wheelScale =
      when {
        c.contains("utility") || c.contains("fat") -> 1.10f
        n.contains("master pro") || n.contains("v13") || n.contains("sherman") -> 1.07f
        n.contains("climber") || n.contains("p100s") -> 0.90f
        else -> 1.0f
      }
    val bodyLengthScale =
      when {
        n.contains("storm") || n.contains("stark") || n.contains("metacycle") -> 1.06f
        n.contains("climber") || n.contains("lectric") -> 0.92f
        else -> 1.0f
      }
    return Pev3dModelFinish(
      bodyRgb = bodyRgb,
      accentRgb = accentRgb,
      caliperRgb = caliperRgb,
      wheelScale = wheelScale,
      bodyLengthScale = bodyLengthScale,
      secondaryRgb = secondaryRgb,
      paintMetallic = paintMetallic,
      blueprintType = blueprint,
    )
  }

  fun hotspotsForArchetype(archetype: Pev3dArchetype): List<Pev3dHotspot> =
    when (archetype) {
      Pev3dArchetype.SCOOTER ->
        listOf(
          Pev3dHotspot("battery", "Battery & BMS", Vec3(0f, -0.26f, 0f), Vec3(0f, 0.38f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Dual Hub Motors", Vec3(-0.92f, -0.45f, 0f), Vec3(-0.35f, -0.1f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "4-Piston Brakes", Vec3(0.92f, -0.45f, 0.2f), Vec3(0.25f, -0.15f, 0.35f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "C-Arm Shocks", Vec3(0.75f, -0.22f, 0f), Vec3(0.25f, 0.25f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "FOC Inverter", Vec3(0.32f, -0.24f, 0f), Vec3(0f, -0.35f, 0.25f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "TFT Cockpit", Vec3(0.46f, 0.96f, 0f), Vec3(0.15f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "Stem & Deck", Vec3(-0.32f, -0.18f, 0f), Vec3(0f, 0.12f, -0.25f), Color(0xFFA855F7)),
        )
      Pev3dArchetype.EUC ->
        listOf(
          Pev3dHotspot("battery", "Twin Cell Packs", Vec3(0f, 0.14f, 0.26f), Vec3(0f, 0.2f, 0.45f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Hollow-Bore Hub", Vec3(0f, -0.12f, 0f), Vec3(0f, -0.25f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "E-Brake & Rotor", Vec3(0.24f, -0.24f, 0.15f), Vec3(0.32f, -0.18f, 0.25f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "CNC Stanchions", Vec3(-0.22f, 0.18f, 0.22f), Vec3(-0.35f, 0.28f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "Mainboard & Mosfets", Vec3(0f, 0.46f, 0f), Vec3(0f, 0.45f, 0f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "Trolley & HUD", Vec3(0.22f, 0.56f, 0f), Vec3(0.25f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "CNC Pedals & Cage", Vec3(0f, -0.28f, 0.34f), Vec3(0f, -0.1f, 0.38f), Color(0xFFA855F7)),
        )
      Pev3dArchetype.HYDROFOIL ->
        listOf(
          Pev3dHotspot("battery", "Waterproof Pack", Vec3(-0.1f, 0.18f, 0f), Vec3(0f, 0.4f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Foil Pod Motor", Vec3(-0.2f, -0.78f, 0f), Vec3(-0.25f, -0.25f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "Foil Wing Stabilizer", Vec3(0.25f, -0.78f, 0f), Vec3(0.3f, -0.2f, 0f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "Carbon Mast", Vec3(0f, -0.30f, 0f), Vec3(0f, 0f, 0.2f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "Watercooled ESC", Vec3(0.22f, 0.16f, 0f), Vec3(0.25f, 0.35f, 0f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "Handheld Remote", Vec3(0.75f, 0.28f, 0f), Vec3(0.2f, 0.3f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "Carbon Board Hull", Vec3(0f, 0.12f, 0f), Vec3(0f, 0.18f, -0.22f), Color(0xFFA855F7)),
        )
      else -> // EMOTO & EBIKE
        listOf(
          Pev3dHotspot("battery", "High-V Pack", Vec3(0.08f, 0.14f, 0f), Vec3(0f, 0.44f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Mid/Hub Drive", Vec3(-0.18f, -0.22f, 0f), Vec3(0f, -0.36f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "4-Piston Rotors", Vec3(0.95f, -0.35f, 0.14f), Vec3(0.28f, -0.15f, 0.35f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "USD Inverted Fork", Vec3(0.72f, 0.22f, 0f), Vec3(0.35f, 0.2f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "FOC Inverter", Vec3(0.34f, -0.04f, 0f), Vec3(0.25f, -0.22f, 0.25f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "TFT & Bars", Vec3(0.55f, 0.68f, 0f), Vec3(0.2f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "6061 Chassis", Vec3(-0.35f, 0.18f, 0f), Vec3(-0.22f, 0.2f, -0.2f), Color(0xFFA855F7)),
        )
    }

  /**
   * Computes camera position & ViewProjection matrix matching right-handed Camera.lookAt
   * so 2D screen hotspots and touch hit-testing align pixel-for-pixel with all 3D engines.
   */
  fun buildCameraViewProjection(
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    outVpMatrix: FloatArray,
    outCameraPos: FloatArray? = null,
  ) {
    val aspect = (widthPx / heightPx.coerceAtLeast(1f)).coerceAtLeast(0.4f)
    val proj = FloatArray(16)
    val view = FloatArray(16)
    Mat4Math.perspective(proj, 38f, aspect, 0.25f, 40f)

    val dist = (4.65f / zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (yawDeg * PI / 180.0).toFloat()
    val pr = (pitchDeg * PI / 180.0).toFloat()
    val cosP = cos(pr)
    val sinP = sin(pr)
    val camX = dist * cosP * sin(yr)
    val camY = dist * sinP + 0.08f
    val camZ = dist * cosP * cos(yr)

    outCameraPos?.let {
      it[0] = camX
      it[1] = camY
      it[2] = camZ
    }
    Mat4Math.lookAt(
      view,
      camX, camY, camZ,
      0f, -0.05f, 0f,
      0f, 1f, 0f,
    )
    Mat4Math.multiplyMM(outVpMatrix, proj, view)
  }

  fun projectHotspots(
    archetype: Pev3dArchetype,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    explodePct: Float,
  ): List<ProjectedHotspot> {
    if (widthPx <= 1f || heightPx <= 1f) return emptyList()
    val vp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)
    val inVec = FloatArray(4)
    val outVec = FloatArray(4)
    return hotspotsForArchetype(archetype).map { hs ->
      val wp = hs.worldPos + hs.explodeDir * explodePct
      inVec[0] = wp.x
      inVec[1] = wp.y
      inVec[2] = wp.z
      inVec[3] = 1f
      Mat4Math.multiplyMV(outVec, vp, inVec)
      val w = outVec[3].coerceAtLeast(0.01f)
      val ndcX = outVec[0] / w
      val ndcY = outVec[1] / w
      val sx = (ndcX * 0.5f + 0.5f) * widthPx
      val sy = (0.5f - ndcY * 0.5f) * heightPx
      ProjectedHotspot(
        partKey = hs.partKey,
        label = hs.label,
        screenPos = Offset(sx, sy),
        depth = w,
        accent = hs.accent,
      )
    }
  }

  fun projectWireframeSegments(
    wireframeLines: FloatArray,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
  ): FloatArray {
    if (widthPx <= 1f || heightPx <= 1f || wireframeLines.size < 6) return FloatArray(0)
    val vp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)
    val segCount = wireframeLines.size / 6
    val out = FloatArray(segCount * 4)
    val inA = FloatArray(4)
    val inB = FloatArray(4)
    val outA = FloatArray(4)
    val outB = FloatArray(4)
    var oi = 0
    for (i in 0 until segCount) {
      val bi = i * 6
      inA[0] = wireframeLines[bi]; inA[1] = wireframeLines[bi + 1]; inA[2] = wireframeLines[bi + 2]; inA[3] = 1f
      inB[0] = wireframeLines[bi + 3]; inB[1] = wireframeLines[bi + 4]; inB[2] = wireframeLines[bi + 5]; inB[3] = 1f
      Mat4Math.multiplyMV(outA, vp, inA)
      Mat4Math.multiplyMV(outB, vp, inB)
      if (outA[3] <= 0.1f || outB[3] <= 0.1f) continue
      out[oi++] = ((outA[0] / outA[3]) * 0.5f + 0.5f) * widthPx
      out[oi++] = (0.5f - (outA[1] / outA[3]) * 0.5f) * heightPx
      out[oi++] = ((outB[0] / outB[3]) * 0.5f + 0.5f) * widthPx
      out[oi++] = (0.5f - (outB[1] / outB[3]) * 0.5f) * heightPx
    }
    return if (oi == out.size) out else out.copyOf(oi)
  }

  fun projectWireframeGroups(
    wireGroups: List<PevWireGroup>,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    explodePct: Float,
    wheelSpinDeg: Float,
    selectedPartKey: String?,
  ): Pair<FloatArray, FloatArray> {
    if (widthPx <= 1f || heightPx <= 1f || wireGroups.isEmpty()) return FloatArray(0) to FloatArray(0)
    val vp = FloatArray(16)
    val modelMat = FloatArray(16)
    val mvp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)

    val normalOut = ArrayList<Float>(4096)
    val selectedOut = ArrayList<Float>(1024)
    val inA = FloatArray(4)
    val inB = FloatArray(4)
    val outA = FloatArray(4)
    val outB = FloatArray(4)

    for (g in wireGroups) {
      Mat4Math.setIdentity(modelMat)
      if (explodePct > 0.005f && g.partKey != "floor") {
        Mat4Math.translate(modelMat, g.explodeDir.x * explodePct, g.explodeDir.y * explodePct, g.explodeDir.z * explodePct)
      }
      g.axleCenter?.let { c ->
        Mat4Math.translate(modelMat, c.x, c.y, c.z)
        Mat4Math.rotateAxis(modelMat, -wheelSpinDeg, 0f, 0f, 1f)
        Mat4Math.translate(modelMat, -c.x, -c.y, -c.z)
      }
      Mat4Math.multiplyMM(mvp, vp, modelMat)

      val target = if (selectedPartKey != null && g.partKey == selectedPartKey && g.partKey != "floor") selectedOut else normalOut
      val lines = g.lines
      val segCount = lines.size / 6
      for (i in 0 until segCount) {
        val bi = i * 6
        inA[0] = lines[bi]; inA[1] = lines[bi + 1]; inA[2] = lines[bi + 2]; inA[3] = 1f
        inB[0] = lines[bi + 3]; inB[1] = lines[bi + 4]; inB[2] = lines[bi + 5]; inB[3] = 1f
        Mat4Math.multiplyMV(outA, mvp, inA)
        Mat4Math.multiplyMV(outB, mvp, inB)
        if (outA[3] <= 0.1f || outB[3] <= 0.1f) continue
        target += ((outA[0] / outA[3]) * 0.5f + 0.5f) * widthPx
        target += (0.5f - (outA[1] / outA[3]) * 0.5f) * heightPx
        target += ((outB[0] / outB[3]) * 0.5f + 0.5f) * widthPx
        target += (0.5f - (outB[1] / outB[3]) * 0.5f) * heightPx
      }
    }
    return normalOut.toFloatArray() to selectedOut.toFloatArray()
  }
}
