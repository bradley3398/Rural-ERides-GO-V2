package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// GENERIC UI STATE & AUTH SESSION MODELS
// ============================================================================
sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class AuthProviderType(val label: String) {
  GOOGLE("Google Sign-In"),
  SUPABASE("Supabase Cloud"),
}

data class ActiveSessionUser(
  val uid: String = "",
  val rawId: String = "",
  val email: String = "",
  val displayName: String = "",
  val provider: AuthProviderType = AuthProviderType.GOOGLE,
  val firebaseAuthenticated: Boolean = true,
)

// ============================================================================
// VISUAL STYLES, THEMES, HUD & WALLPAPERS
// ============================================================================
enum class VisualStyleMode(val id: String, val label: String, val description: String = "") {
  DARK_STUDIO("dark_studio", "Dark Studio", "Tactical Dark Studio"),
  NEON_CYBERPUNK("neon_cyberpunk", "Neon Cyberpunk", "High-contrast cyber HUD"),
  BRUTALIST_HIGH_VIS("brutalist_high_vis", "Brutalist High-Vis", "High-visibility sharp edges"),
  OLED_STEALTH("oled_stealth", "OLED Stealth", "Pure black OLED stealth"),
  VAPORWAVE_RETRO("vaporwave_retro", "Vaporwave Retro", "Outrun synthwave horizon"),
  CUPERTINO_FROST("cupertino_frost", "Cupertino Frost", "Frosted glass & brushed metal"),
  MATERIAL_EXPRESSIVE("material_expressive", "Material Expressive", "Dynamic Material 3 expressive"),
  PEV_TACTICAL_NEON("pev_tactical_neon", "PEV Tactical Neon", "VESC carbon & neon lime"),
  PEV_APEX_TRACK("pev_apex_track", "PEV Apex Track", "Hyper-scooter track telemetry"),
  PEV_GRAVEL_DUST("pev_gravel_dust", "PEV Gravel Dust", "Backroad gravel & amber dust"),
  PEV_TITANIUM_PRO("pev_titanium_pro", "PEV Titanium Pro", "CNC titanium & cyan telemetry"),
  PEV_LITHIUM_LAB("pev_lithium_lab", "PEV Lithium Lab", "High-voltage cell diagnostics"),
  PEV_NIGHT_CONVOY("pev_night_convoy", "PEV Night Convoy", "Midnight convoy tactical"),
  PEV_TRAIL_FOREST("pev_trail_forest", "PEV Trail Forest", "Deep woods singletrack"),
  PEV_MIDNIGHT_NEON("pev_midnight_neon", "PEV Midnight Neon", "Electric horizon neon"),
  PEV_DESERT_ENDURO("pev_desert_enduro", "PEV Desert Enduro", "Canyon enduro warmth"),
  PEV_SUNSET_CRUISE("pev_sunset_cruise", "PEV Sunset Cruise", "Coastal sunset carving"),
  PEV_VOLT_RACER("pev_volt_racer", "PEV Volt Racer", "High-amp racing cockpit"),
  PEV_CYBER_CARBON("pev_cyber_carbon", "PEV Cyber Carbon", "Forged carbon weave"),
  PEV_ARCTIC_ICE("pev_arctic_ice", "PEV Arctic Ice", "Alpine glacier clarity"),
  RURAL_BARNWOOD("rural_barnwood", "Rural Barnwood", "Warm homestead timber & iron"),
  RURAL_SUNSET_GRAVEL("rural_sunset_gravel", "Rural Sunset Gravel", "Golden hour county gravel"),
  RURAL_PINE_HOLLER("rural_pine_holler", "Rural Pine Holler", "Appalachian pine & moss"),
  RURAL_PRAIRIE_GOLD("rural_prairie_gold", "Rural Prairie Gold", "Harvest wheat & brass"),
  RURAL_CREEK_MIST("rural_creek_mist", "Rural Creek Mist", "Cool creek slate & mist"),
  RURAL_HARVEST_IRON("rural_harvest_iron", "Rural Harvest Iron", "Barn workshop forged iron"),
  WINDOWS_CLASSIC("windows_classic", "Windows 95 Classic", "Authentic Win95 3D bevel desktop"),
  WINDOWS_95("windows_95", "Windows 95", "Classic 1995 desktop"),
  WINDOWS_98("windows_98", "Windows 98", "Classic 1998 desktop"),
  WINDOWS_2000("windows_2000", "Windows 2000 Pro", "Professional slate bevel desktop"),
  WINDOWS_XP("windows_xp", "Windows XP Luna", "Luna blue & olive Bliss desktop"),
  WINDOWS_7("windows_7", "Windows 7 Aero", "Translucent Aero glass desktop"),
  WINDOWS_10("windows_10", "Windows 10 Fluent", "Fluent dark acrylic desktop"),
  WINDOWS_11("windows_11", "Windows 11 Mica", "Rounded Mica bloom desktop"),
  HOLIDAY_NEW_YEARS("holiday_new_years", "New Year's Midnight", "Gold champagne & midnight fireworks"),
  HOLIDAY_VALENTINES("holiday_valentines", "Valentine's Crimson", "Rose neon & velvet hearts"),
  HOLIDAY_ST_PATRICKS("holiday_st_patricks", "St. Patrick's Clover", "Emerald clover & lucky gold"),
  HOLIDAY_EASTER("holiday_easter", "Easter Spring Bloom", "Pastel spring & meadow bloom"),
  HOLIDAY_MEMORIAL_DAY("holiday_memorial_day", "Memorial Honor", "Stars, stripes & honor poppy"),
  HOLIDAY_FOURTH_JULY("holiday_fourth_july", "4th of July Liberty", "Patriotic fireworks & liberty stars"),
  HOLIDAY_LABOR_DAY("holiday_labor_day", "Labor Day Iron", "American workshop & industrial brass"),
  HOLIDAY_HALLOWEEN("holiday_halloween", "Halloween Midnight", "Jack-o'-lantern amber & violet mist"),
  HOLIDAY_VETERANS_DAY("holiday_veterans_day", "Veterans Valor", "Service gold star & honor ribbon"),
  HOLIDAY_THANKSGIVING("holiday_thanksgiving", "Thanksgiving Harvest", "Autumn maple & harvest gold"),
  HOLIDAY_CHRISTMAS("holiday_christmas", "Christmas Pine & Holly", "Festive pine, holly & string lights"),
}

enum class AppThemePreset(val id: String, val label: String) {
  LIME("lime", "Lime Neon"),
  CYAN("cyan", "Cyber Cyan"),
  EMERALD("emerald", "Emerald Trail"),
  PURPLE("purple", "Deep Purple"),
  AMBER("amber", "Amber Warning"),
  HOT_PINK("hot_pink", "Hot Pink"),
  ELECTRIC_BLUE("electric_blue", "Electric Blue"),
  SOLARIZED("solarized", "Solarized"),
}

enum class ColorModeOverride(val id: String, val label: String) {
  DARK("dark", "Dark"),
  LIGHT("light", "Light"),
  SYSTEM("system", "System"),
}

enum class HudStyle(val id: String, val label: String) {
  ORB("orb", "Neon Orb"),
  ANALOG("analog", "Analog Dial"),
  CYBER("cyber", "Cyber Arc"),
  MINIMAL("minimal", "Minimal Digital"),
  BAR("bar", "LED Bar"),
  AVIATION("aviation", "Aviation Horizon"),
  SUPERBIKE("superbike", "Superbike Tach"),
  F1_TELEMETRY("f1_telemetry", "F1 Telemetry"),
  FIGHTER_JET("fighter_jet", "Fighter Jet HUD"),
  COMPASS_ROSE("compass_rose", "Compass Rose"),
  DUAL_TWIN("dual_twin", "Dual Twin Cluster"),
  RETRO_SYNTH("retro_synth", "Retro Synth"),
  EUC_LEAN("euc_lean", "EUC Lean Carver"),
}

enum class FontPreset(val id: String, val label: String, val familyName: String = label) {
  TACTICAL_MONO("tactical_mono", "Tactical Mono"),
  HIGHWAY_SANS("highway_sans", "Highway Sans"),
  CLASSIC_SERIF("classic_serif", "Classic Serif"),
  BRUTAL_CONDENSED("brutal_condensed", "Brutal Condensed"),
  SPACE_GROTESK("space_grotesk", "Space Grotesk"),
  JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono"),
  ROBOTO("roboto", "Roboto"),
  INTER("inter", "Inter"),
}

enum class TypographyScaleMode(val id: String, val label: String, val multiplier: Float) {
  COMPACT("compact", "Compact", 0.92f),
  STANDARD("standard", "Standard", 1.0f),
  LARGE("large", "Large", 1.12f),
}

enum class BgTexture(val id: String, val label: String) {
  NONE("none", "None"),
  GRID("grid", "Tactical Grid"),
  COUNTY_GRID("county_grid", "County Section Grid"),
  ASPHALT("asphalt", "Asphalt Grain"),
  GRAVEL("gravel", "Crushed Gravel"),
  TRAIL_DUST("trail_dust", "Trail Dust"),
  CONTOUR("contour", "Elevation Contour"),
  TOPO("topo", "Topographic"),
  WAVES("waves", "Sine Waves"),
  FOREST("forest", "Pine Forest"),
  FARMLAND("farmland", "Farmland Furrows"),
  HEX("hex", "Hex Honeycomb"),
  MOUNTAIN("mountain", "Mountain Ridge"),
  CIRCUIT("circuit", "PCB Circuit"),
  CARBON("carbon", "Carbon Weave"),
  STEEL_PLATE("steel_plate", "Diamond Steel"),
  RADAR_GRID("radar_grid", "Radar Polar"),
  MATRIX("matrix", "Digital Matrix"),
  BLUEPRINT("blueprint", "CAD Blueprint"),
  SCANLINES("scanlines", "CRT Scanlines"),
  DIAMOND_GRID("diamond_grid", "Diamond Lattice"),
  STARFIELD("starfield", "Night Starfield"),
  WOODGRAIN("woodgrain", "Barn Woodgrain"),
  ISOMETRIC("isometric", "3D Isometric"),
}

enum class PevWallpaper(val id: String, val label: String) {
  NONE("none", "None"),
  AUTO_THEME("auto_theme", "Auto Match Theme"),
  HYPER_TRACK("hyper_track", "Hyper Track"),
  VESC_CARBON("vesc_carbon", "VESC Carbon"),
  ALPINE_RIDGE("alpine_ridge", "Alpine Ridge"),
  CYBER_CITY_EUC("cyber_city_euc", "Cyber City EUC"),
  FOREST_EUC("forest_euc", "Forest EUC"),
  NEON_SCOOTER("neon_scooter", "Neon Scooter"),
  CANYON_EMOTO("canyon_emoto", "Canyon E-Moto"),
  COASTAL_ONEWHEEL("coastal_onewheel", "Coastal Onewheel"),
  MOUNTAIN_TRAIL("mountain_trail", "Mountain Trail"),
  COCKPIT_HERO("cockpit_hero", "Cockpit Hero"),
  BOARD_CONVOY("board_convoy", "Board Convoy"),
  CYBER_ALLEY("cyber_alley", "Cyber Alley"),
  GRAVEL_SUNSET("gravel_sunset", "Gravel Sunset"),
  BARN_WORKBENCH("barn_workbench", "Barn Workbench"),
  SYNTHWAVE_GRID("synthwave_grid", "Synthwave Grid"),
  ALPINE_GLACIER("alpine_glacier", "Alpine Glacier"),
}

enum class PevLegalClass(val id: String, val label: String, val maxMph: Int) {
  CLASS_1_15("class_1_15", "Class 1 (15 MPH Assist)", 15),
  CLASS_2_20("class_2_20", "Class 2 (20 MPH Throttle)", 20),
  CLASS_3_28("class_3_28", "Class 3 (28 MPH Fast)", 28),
  OFFROAD_UNLIMITED("offroad_unlimited", "Off-Road Unlimited", 99);

  val maxSpeedMph: Int
    get() = maxMph
}

// ============================================================================
// AUDIO, VOICE & HAPTIC PRESETS
// ============================================================================
enum class VoiceDspProfile(
  val id: String,
  val label: String,
  val bassGainMb: Int = 250,
  val highShelfHz: Int = 3600,
) {
  DEEP_BARN("deep_barn", "Deep Barn Resonance", 450, 2800),
  WARM_PORCH("warm_porch", "Warm Porch Acoustic", 340, 3200),
  WARM_SCOUT("warm_scout", "Warm Creek Scout", 280, 3600),
  BRIGHT_DISPATCH("bright_dispatch", "Bright Trail Dispatch", 180, 4400),
  TACTICAL_CRISP("tactical_crisp", "Tactical Crisp Presence", 160, 4800),
  OUTBACK_AU("outback_au", "Outback Open Air", 260, 3900),
  HIGHLAND_UK("highland_uk", "Highland Studio Warm", 310, 3500),
  CB_RADIO("cb_radio", "Bandpass CB Convoy", 120, 2400),
}

enum class AiVoiceProfile(
  val id: String,
  val label: String,
  val description: String,
  val geminiVoiceName: String,
  val dspProfile: VoiceDspProfile,
  val speechRate: Float,
  val pitch: Float,
  val localeTag: String,
  val preferFemale: Boolean,
  val styleDirective: String,
  val sampleLine: String,
) {
  WARM_RURAL_PORCH(
    id = "warm_rural_porch",
    label = "Warm Rural Porch",
    description = "Friendly backroad trail guide with warm country cadence",
    geminiVoiceName = "Aoede",
    dspProfile = VoiceDspProfile.WARM_PORCH,
    speechRate = 0.96f,
    pitch = 0.94f,
    localeTag = "en-US",
    preferFemale = false,
    styleDirective = "Speak with a warm, unhurried rural porch cadence.",
    sampleLine = "Evening partner, battery pack is balanced and the county gravel is clear ahead.",
  ),
  BARN_CHIEF_DEEP(
    id = "barn_chief_deep",
    label = "Barn Chief Deep",
    description = "Deep workshop mechanic voice with steady bass authority",
    geminiVoiceName = "Charon",
    dspProfile = VoiceDspProfile.DEEP_BARN,
    speechRate = 0.93f,
    pitch = 0.86f,
    localeTag = "en-US",
    preferFemale = false,
    styleDirective = "Speak with a deep, calm barn-mechanic baritone.",
    sampleLine = "Phase leads look clean and your 72-volt pack is holding zero sag under load.",
  ),
  BRIGHT_TRAIL_DISPATCH(
    id = "bright_trail_dispatch",
    label = "Bright Trail Dispatch",
    description = "Clear upbeat trail dispatcher for wind & road noise clarity",
    geminiVoiceName = "Kore",
    dspProfile = VoiceDspProfile.BRIGHT_DISPATCH,
    speechRate = 1.02f,
    pitch = 1.04f,
    localeTag = "en-US",
    preferFemale = true,
    styleDirective = "Speak clearly and briskly like a trail radio dispatcher.",
    sampleLine = "Trail dispatch copy: headwind at 8 miles per hour, charger waypoint 4 miles north.",
  ),
  TACTICAL_COACH_CRISP(
    id = "tactical_coach_crisp",
    label = "Tactical Coach Crisp",
    description = "High-precision cockpit telemetry & apex pacing coach",
    geminiVoiceName = "Fenrir",
    dspProfile = VoiceDspProfile.TACTICAL_CRISP,
    speechRate = 1.04f,
    pitch = 0.98f,
    localeTag = "en-US",
    preferFemale = false,
    styleDirective = "Deliver concise, high-clarity tactical PEV telemetry callouts.",
    sampleLine = "Telemetry locked: 9-axis AHRS calibrated, lean angle nominal, ready to roll.",
  ),
  CREEK_SCOUT_FRIENDLY(
    id = "creek_scout_friendly",
    label = "Creek Scout Friendly",
    description = "Relaxed backwoods singletrack scout",
    geminiVoiceName = "Puck",
    dspProfile = VoiceDspProfile.WARM_SCOUT,
    speechRate = 0.98f,
    pitch = 1.00f,
    localeTag = "en-US",
    preferFemale = true,
    styleDirective = "Speak like an encouraging backcountry trail scout.",
    sampleLine = "Creek crossing ahead looks shallow, keep steady throttle through the pine bend.",
  ),
  OUTBACK_OVERLAND_AU(
    id = "outback_overland_au",
    label = "Outback Overland",
    description = "Australian overland trail guide",
    geminiVoiceName = "Puck",
    dspProfile = VoiceDspProfile.OUTBACK_AU,
    speechRate = 0.99f,
    pitch = 0.96f,
    localeTag = "en-AU",
    preferFemale = false,
    styleDirective = "Speak with a relaxed Australian overland trail tone.",
    sampleLine = "G'day rider, solar top-up looks solid and the ridge track is dry.",
  ),
  HIGHLAND_RIDGE_UK(
    id = "highland_ridge_uk",
    label = "Highland Ridge UK",
    description = "Crisp British rally co-driver cadence",
    geminiVoiceName = "Kore",
    dspProfile = VoiceDspProfile.HIGHLAND_UK,
    speechRate = 1.00f,
    pitch = 0.98f,
    localeTag = "en-GB",
    preferFemale = true,
    styleDirective = "Speak like a composed British rally co-driver.",
    sampleLine = "Crest into slight left over gravel, sixty meters to the scenic overlook.",
  ),
  MIDNIGHT_CB_CONVOY(
    id = "midnight_cb_convoy",
    label = "Midnight CB Convoy",
    description = "Classic highway CB radio convoy tone",
    geminiVoiceName = "Fenrir",
    dspProfile = VoiceDspProfile.CB_RADIO,
    speechRate = 0.95f,
    pitch = 0.90f,
    localeTag = "en-US",
    preferFemale = false,
    styleDirective = "Speak with classic CB convoy radio warmth and brevity.",
    sampleLine = "Breaker one-nine, convoy beacon locked and clear all the way to the county line.",
  ),
}

data class CustomVoiceProfile(
  val id: String = "",
  val name: String = "Custom Voice",
  val description: String = "",
  val baseProfileId: String = AiVoiceProfile.WARM_RURAL_PORCH.id,
  val geminiVoiceName: String = "Aoede",
  val dspProfile: VoiceDspProfile = VoiceDspProfile.WARM_PORCH,
  val speechRate: Float = 0.96f,
  val pitch: Float = 0.94f,
  val localeTag: String = "en-US",
  val preferFemale: Boolean = false,
  val warmthPct: Int = 72,
  val clarityPct: Int = 65,
  val styleDirective: String = "Speak with a warm, unhurried rural cadence.",
  val sampleLine: String = "Testing custom rural PEV voice tuning on the county trail.",
  val audioSampleBase64: String = "",
  val audioSampleFileName: String = "",
  val audioSampleDurationMs: Long = 0L,
  val isCustomUploaded: Boolean = false,
  val createdAtMs: Long = System.currentTimeMillis(),
)

fun CustomVoiceProfile.resolveEffectiveBase(): AiVoiceProfile =
  AiVoiceProfile.entries.firstOrNull { it.id == baseProfileId } ?: AiVoiceProfile.WARM_RURAL_PORCH

fun parseCustomVoiceProfiles(json: String): List<CustomVoiceProfile> =
  runCatching {
    if (json.isBlank()) return emptyList()
    val arr = JSONArray(json)
    (0 until arr.length()).mapNotNull { i ->
      val o = arr.optJSONObject(i) ?: return@mapNotNull null
      val id = o.optString("id").trim()
      if (id.isBlank()) return@mapNotNull null
      val sampleB64 = o.optString("audioSampleBase64", "")
      CustomVoiceProfile(
        id = id,
        name = o.optString("name", "Custom Voice"),
        description = o.optString("description", ""),
        baseProfileId = o.optString("baseProfileId", AiVoiceProfile.WARM_RURAL_PORCH.id),
        geminiVoiceName = o.optString("geminiVoiceName", "Aoede"),
        dspProfile =
          VoiceDspProfile.entries.firstOrNull { it.id == o.optString("dspProfile") }
            ?: VoiceDspProfile.WARM_PORCH,
        speechRate = o.optDouble("speechRate", 0.96).toFloat().coerceIn(0.75f, 1.30f),
        pitch = o.optDouble("pitch", 0.94).toFloat().coerceIn(0.70f, 1.30f),
        localeTag = o.optString("localeTag", "en-US"),
        preferFemale = o.optBoolean("preferFemale", false),
        warmthPct = o.optInt("warmthPct", 70).coerceIn(0, 100),
        clarityPct = o.optInt("clarityPct", 65).coerceIn(0, 100),
        styleDirective = o.optString("styleDirective", "Speak with a warm rural cadence."),
        sampleLine = o.optString("sampleLine", "Testing custom rural PEV voice tuning."),
        audioSampleBase64 = sampleB64,
        audioSampleFileName = o.optString("audioSampleFileName", ""),
        audioSampleDurationMs = o.optLong("audioSampleDurationMs", 0L),
        isCustomUploaded = o.optBoolean("isCustomUploaded", sampleB64.isNotBlank()),
        createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
      )
    }
  }.getOrDefault(emptyList())

fun serializeCustomVoiceProfiles(list: List<CustomVoiceProfile>): String {
  val arr = JSONArray()
  list.forEach { v ->
    arr.put(
      JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("description", v.description)
        put("baseProfileId", v.baseProfileId)
        put("geminiVoiceName", v.geminiVoiceName)
        put("dspProfile", v.dspProfile.id)
        put("speechRate", v.speechRate.toDouble())
        put("pitch", v.pitch.toDouble())
        put("localeTag", v.localeTag)
        put("preferFemale", v.preferFemale)
        put("warmthPct", v.warmthPct)
        put("clarityPct", v.clarityPct)
        put("styleDirective", v.styleDirective)
        put("sampleLine", v.sampleLine)
        put("audioSampleBase64", v.audioSampleBase64)
        put("audioSampleFileName", v.audioSampleFileName)
        put("audioSampleDurationMs", v.audioSampleDurationMs)
        put("isCustomUploaded", v.isCustomUploaded)
        put("createdAtMs", v.createdAtMs)
      }
    )
  }
  return arr.toString()
}

fun MasterSettings.getActiveCustomVoice(): CustomVoiceProfile? {
  if (activeCustomVoiceId.isBlank()) return null
  return parseCustomVoiceProfiles(customVoiceProfilesJson).firstOrNull {
    it.id == activeCustomVoiceId
  }
}

enum class RingtonePreset(val id: String, val label: String) {
  VOLT_PULSE("volt_pulse", "Volt Pulse"),
  CYBER_ARPEGGIO("cyber_arpeggio", "Cyber Arpeggio"),
  TACTICAL_RADAR("tactical_radar", "Tactical Radar"),
  HYPER_SCOOTER("hyper_scooter", "Hyper Scooter"),
  CLASSIC_BELL("classic_bell", "Classic Trail Bell"),
  RETRO_CHIME_95("retro_chime_95", "Retro Chime '95"),
  AVIATION_COCKPIT("aviation_cockpit", "Aviation Cockpit"),
  LASER_PING("laser_ping", "Laser Ping"),
  TRAIL_HORN("trail_horn", "Trail Horn"),
  SYNTH_WAVE("synth_wave", "Synth Wave"),
  SONAR_ECHO("sonar_echo", "Sonar Echo"),
  SYSTEM_DEFAULT("system_default", "System Default"),
}

enum class AlertTonePreset(val id: String, val label: String) {
  CHIRP_TACTICAL("chirp_tactical", "Tactical Chirp"),
  ROGER_BEEPS("roger_beeps", "Roger Beeps"),
  BELL_CLEAR("bell_clear", "Clear Bell"),
  CYBER_BLIP("cyber_blip", "Cyber Blip"),
  TWO_TONE_ALERT("two_tone_alert", "Two-Tone Alert"),
  SUBTLE_TAP("subtle_tap", "Subtle Tap"),
  ALARM_SIREN("alarm_siren", "Alarm Siren"),
  SILENT("silent", "Silent"),
}

enum class VibrationPatternPreset(val id: String, val label: String, val timings: LongArray) {
  OFF("off", "Off", longArrayOf()),
  SINGLE_PULSE("single_pulse", "Single Pulse", longArrayOf(0, 40)),
  DOUBLE_TAP("double_tap", "Double Tap", longArrayOf(0, 28, 60, 42)),
  TRIPLE_BURST("triple_burst", "Triple Burst", longArrayOf(0, 25, 45, 25, 45, 40)),
  HEARTBEAT("heartbeat", "Heartbeat", longArrayOf(0, 35, 70, 55, 220, 30)),
  SOS_MORSE("sos_morse", "SOS Pulse", longArrayOf(0, 30, 40, 30, 40, 30, 90, 75, 50, 75)),
}

// ============================================================================
// NAVIGATION & RIDER MATCHING ENUMS
// ============================================================================
enum class NavRouteProfile(
  val id: String,
  val label: String,
  val mapboxProfile: String,
  val orsProfile: String,
  val osrmProfile: String,
) {
  PEV_BIKE_LANE("pev_bike_lane", "PEV Bike Lane", "cycling", "cycling-electric", "bike"),
  FAST_STREET("fast_street", "Fast Street", "driving", "driving-car", "driving"),
  GRAVEL_TRAIL("gravel_trail", "Gravel / Trail", "cycling", "cycling-mountain", "bike"),
  FOOT_WALK("foot_walk", "Slow Path / Walk", "walking", "foot-walking", "foot"),
}

enum class RiderPaceTier(val id: String, val code: String, val label: String, val mphBand: String) {
  ANY("any", "ANY", "Any Pace", "All Speeds"),
  CHILL("chill", "CHL", "Chill (10-16 mph)", "10-16 mph"),
  CRUISE("cruise", "CRZ", "Cruise (16-24 mph)", "16-24 mph"),
  FAST("fast", "FST", "Fast (25-38 mph)", "25-38 mph"),
  HYPER("hyper", "HYP", "Hyper (40+ mph)", "40+ mph"),
}

enum class RiderTerrainPref(val id: String, val code: String, val label: String) {
  ANY("any", "ANY", "Any Terrain"),
  PAVED("paved", "PAV", "Paved & Bike Lanes"),
  GRAVEL("gravel", "GRV", "Gravel & Backroads"),
  SINGLETRACK("singletrack", "TRL", "Dirt & Singletrack"),
  MIXED("mixed", "MIX", "Mixed All-Terrain"),
}

enum class RiderScheduleVibe(val id: String, val code: String, val label: String) {
  ANY("any", "ANY", "Any Schedule"),
  GROUP_CONVOY("group_convoy", "GRP", "Group Convoys"),
  WEEKEND("weekend", "WKD", "Weekend Trail Runs"),
  EVENING("evening", "EVE", "Sunset & Night Rides"),
  COMMUTE("commute", "COM", "Daily Commute & Coffee"),
}

// ============================================================================
// MASTER SETTINGS
// ============================================================================
data class MasterSettings(
  val themePreset: AppThemePreset = AppThemePreset.LIME,
  val visualStyle: VisualStyleMode = VisualStyleMode.DARK_STUDIO,
  val colorModeOverride: ColorModeOverride = ColorModeOverride.DARK,
  val hudStyle: HudStyle = HudStyle.ORB,
  val hud3dTilt: Boolean = true,
  val nightVisionMode: Boolean = false,
  val sunVisibilityMode: Boolean = false,
  val fontPreset: FontPreset = FontPreset.SPACE_GROTESK,
  val bgTexture: BgTexture = BgTexture.GRID,
  val locale: String = "en",
  val uiScale: String = "normal",
  val reducedMotion: Boolean = false,
  val cornerStyle: String = "rounded",
  val borderWidthDp: Float = 1.2f,
  val borderGlowIntensity: Float = 0.65f,
  val cardOpacity: Float = 0.86f,
  val hapticFeedback: Boolean = true,
  val hapticDurationMs: Int = 20,
  val globalVolume: Int = 90,
  val soundPack: String = "Tactical",
  val autoMuteOnVideo: Boolean = true,
  val distanceUnit: String = "mi",
  val tempUnit: String = "f",
  val pressureUnit: String = "psi",
  val decimalPrecision: Int = 1,
  val gpsAccuracyLimitM: Int = 25,
  val telemetryRateHz: Int = 2,
  val imuPollingRateHz: Int = 10,
  val privacyZoneMeters: Int = 200,
  val autoStartTracking: Boolean = false,
  val autoWeatherSync: Boolean = true,
  val weatherUpdateMin: Int = 15,
  val shareLeaderboard: Boolean = true,
  val publicProfile: Boolean = true,
  val biometricAppLock: Boolean = false,
  val crashSensitivityG: Float = 8.0f,
  val sosCancelWindowSec: Int = 12,
  val sosTemplate: String =
    "EMERGENCY: Rural ERides GO pilot requires immediate assistance at GPS coordinates.",
  val voiceHotwordActive: Boolean = false,
  val wakeLockEnabled: Boolean = true,
  val autoDimmer: Boolean = false,
  val smartBatteryAi: Boolean = true,
  val sagCompensation: String = "AI Dynamic",
  val voltDisplayMode: String = "Percentage",
  val motorTempWarnC: Int = 85,
  val schoolZoneStrict: Boolean = true,
  val legalComplianceMode: Boolean = BuildConfig.LEGAL_COMPLIANCE_MODE,
  val pevLegalClass: PevLegalClass = PevLegalClass.CLASS_2_20,
  val legalAudioAlert: Boolean = true,
  val hudBlinkMax: Boolean = true,
  val speedOffsetTolerance: Int = 5,
  val radarRadiusMiles: Int = 50,
  val dialSweepDeg: Int = 240,
  val elevationGradient: Boolean = true,
  val quickReplies: String = "On my way!, Need a charge, Trail blocked, Copy that!",
  val pilotRankBadge: String = "Rookie Pilot",
  val autoCloudSyncOnFinish: Boolean = true,
  val kwhRateUsd: Double = 0.14,
  val gasPriceUsd: Double = 3.45,
  val carMpg: Double = 22.0,
  val copilotPersona: String = "Tactical Coach",
  val aiModelId: String = "gemini-3.5-flash-lite",
  val aiThinkingLevel: String = "medium",
  val aiCreativity: Float = 0.7f,
  val aiDetailLevel: String = "standard",
  val aiWebGrounding: Boolean = true,
  val batteryChemistry: String = "NMC",
  val regenAlgorithm: String = "Linear",
  val maxFps: Int = 60,
  val dmChime: Boolean = true,
  val defaultFeedCategory: String = "ALL",
  val boardPostLimit: Int = 50,
  val dataSaverMode: Boolean = false,
  val dataRetentionPolicy: String = "30 Days",
  val remindersEnabled: Boolean = true,
  val psiReminderDays: Int = 3,
  val maintReminderKm: Int = 250,
  val weatherAlertsEnabled: Boolean = true,
  val aiNotifsEnabled: Boolean = true,
  val socialNotifsEnabled: Boolean = true,
  val dmNotifsEnabled: Boolean = true,
  val quietHoursEnabled: Boolean = false,
  val quietStartHour: Int = 22,
  val quietEndHour: Int = 7,
  val debugMode: Boolean = false,
  val uiChromeStyle: String = "tactical",
  val navBarStyle: String = "pill",
  val dashboardDensity: String = "full",
  val showClassicTitleBars: Boolean = true,
  val radarAutoFollow: Boolean = true,
  val radarSpeedZoom: Boolean = false,
  val callRingtoneEnabled: Boolean = true,
  val autoCloudBackupEnabled: Boolean = false,
  val lastCloudBackupMs: Long = 0L,
  val lastCloudBackupProvider: String = "",
  val ytPlaybackSpeed: Float = 1.0f,
  val ytShuffleEnabled: Boolean = false,
  val ytRepeatMode: String = "off",
  val retroAudioCuesEnabled: Boolean = true,
  val rideMode: String = "STREET",
  val riderWeightLbs: Int = 175,
  val dailyDistanceGoalKm: Float = 20f,
  val customWeatherCity: String = "",
  val customWeatherLat: Double = 0.0,
  val customWeatherLng: Double = 0.0,
  val useCustomWeatherLoc: Boolean = false,
  val customNewsCity: String = "",
  val newsLocationScope: String = "GPS",
  val pevWallpaper: PevWallpaper = PevWallpaper.AUTO_THEME,
  val wallpaperOpacity: Float = 0.42f,
  val highContrastIcons: Boolean = true,
  val callRingtonePreset: RingtonePreset = RingtonePreset.VOLT_PULSE,
  val customRingtoneUri: String = "",
  val dmAlertTone: AlertTonePreset = AlertTonePreset.CHIRP_TACTICAL,
  val walkieRogerTone: AlertTonePreset = AlertTonePreset.ROGER_BEEPS,
  val navTurnTone: AlertTonePreset = AlertTonePreset.BELL_CLEAR,
  val lowBatteryAlarmTone: AlertTonePreset = AlertTonePreset.TWO_TONE_ALERT,
  val ringtoneVolumePct: Int = 85,
  val vibrationPattern: VibrationPatternPreset = VibrationPatternPreset.DOUBLE_TAP,
  val ttsSpeechRate: Float = 1.03f,
  val ttsPitch: Float = 1.0f,
  val autoMuteAlertsWhileRiding: Boolean = false,
  val navDefaultProfile: NavRouteProfile = NavRouteProfile.FAST_STREET,
  val navVoiceGuidance: Boolean = true,
  val navAutoReroute: Boolean = true,
  val navAvoidHighways: Boolean = true,
  val navShowChargersOnMap: Boolean = true,
  val navHeadingUpMode: Boolean = true,
  val autoPauseRideWhenStopped: Boolean = true,
  val lowBatteryWarningPct: Int = 20,
  val criticalVoltageAlertV: Float = 44.0f,
  val regenBrakingChime: Boolean = false,
  val batterySaverTelemetry: Boolean = false,
  val autoAcceptConvoyInvites: Boolean = false,
  val stealthPrivacyMode: Boolean = false,
  val confirmBeforeEndRide: Boolean = true,
  val showSpeedLimitOverlay: Boolean = true,
  val showAhrsTelemetryCard: Boolean = true,
  val ahrsPeakLeanHold: Boolean = true,
  val ahrsLeanAlertDeg: Int = 42,
  val ahrsGaugeDamping: String = "Balanced",
  val aiChatAutoScroll: Boolean = true,
  val aiChatCompactTools: Boolean = true,
  val keepScreenBrightOnRide: Boolean = true,
  val aiVoiceProfile: AiVoiceProfile = AiVoiceProfile.WARM_RURAL_PORCH,
  val ruralPersonaMode: Boolean = true,
  val autoFetchPevPhoto: Boolean = true,
  val showRideChartsOnDash: Boolean = true,
  val showStartupAnimation: Boolean = true,
  val showSatelliteStatusOnDash: Boolean = true,
  val showEnvironmentSensorsOnDash: Boolean = true,
  val buttonHapticFeedback: Boolean = true,
  val rideMilestoneHaptics: Boolean = true,
  val autoNightFromLightSensor: Boolean = false,
  val arShowSatelliteOverlay: Boolean = true,
  val spaciousLayoutMode: Boolean = true,
  val splashDurationSec: Int = 7,
  val confirmCancelNavigation: Boolean = false,
  val navAutoEndOnArrival: Boolean = true,
  val keepScreenAwakeRiding: Boolean = true,
  val mapAutoFollowHeading: Boolean = true,
  val showSpeedLimitBadge: Boolean = true,
  val autoSpeakAiReplies: Boolean = false,
  val mechanicDefaultDepth: String = "STANDARD",
  val batteryReserveWarningPct: Int = 20,
  val compassTrueNorth: Boolean = true,
  val touchSoundsEnabled: Boolean = true,
  val touchSoundPack: String = "Country Acoustic",
  val chartNeonGlowEnabled: Boolean = true,
  val chartEntranceAnimMs: Int = 900,
  val chartHoverTooltipsEnabled: Boolean = true,
  val compactNumericHud: Boolean = false,
  val odometerDecimals: Int = 1,
  val autoPauseSpeedKmh: Float = 2.0f,
  val regenStrengthPreset: String = "Medium",
  val coldWeatherBatteryGuard: Boolean = true,
  val barnBreakerLimitAmps: Int = 15,
  val solarChargeTargetPct: Int = 85,
  val headlightDuskReminder: Boolean = true,
  val duskAutoHeadlightReminderMin: Int = 30,
  val chainLubeIntervalKm: Int = 250,
  val wildlifeAlertRadiusMi: Int = 5,
  val farmEquipmentAlertEnabled: Boolean = true,
  val creekDepthWarningEnabled: Boolean = true,
  val trailSurfaceAutoDetect: Boolean = true,
  val ruralRoadSignAlerts: Boolean = true,
  val convoyBeaconIntervalSec: Int = 15,
  val gpxIncludeElevation: Boolean = true,
  val voiceCalloutVerbosity: String = "Balanced",
  val audioDuckingOnSpeech: Boolean = true,
  val windNoiseFilter: Boolean = true,
  val nightRedShiftIntensity: Float = 0.75f,
  val winShowMenuBar: Boolean = true,
  val winShowTaskbarClock: Boolean = true,
  val winGlassIntensity: Float = 0.85f,
  val winTaskbarCompact: Boolean = false,
  val iconStylePack: String = "Tactical Neon",
  val iconNeonGlowEnabled: Boolean = true,
  val showQuickActionBarOnDash: Boolean = true,
  val showRideReadinessScore: Boolean = true,
  val autoNightDimHudPct: Int = 70,
  val batteryVoltageAlarmEnabled: Boolean = true,
  val hapticScrollDetents: Boolean = true,
  val quickThemeBarEnabled: Boolean = true,
  val matchPaceTier: RiderPaceTier = RiderPaceTier.CRUISE,
  val matchTerrainPref: RiderTerrainPref = RiderTerrainPref.MIXED,
  val matchMinRangeMi: Int = 20,
  val matchScheduleVibe: RiderScheduleVibe = RiderScheduleVibe.GROUP_CONVOY,
  val matchHomeZipOrTown: String = "",
  val matchMinScorePct: Int = 0,
  val matchPaceFilter: RiderPaceTier = RiderPaceTier.ANY,
  val matchTerrainFilter: RiderTerrainPref = RiderTerrainPref.ANY,
  val matchVibeFilter: RiderScheduleVibe = RiderScheduleVibe.ANY,
  val customVoiceProfilesJson: String = "[]",
  val activeCustomVoiceId: String = "",
  val ttsPreloadBufferEnabled: Boolean = true,
  val ttsPreloadBufferDepth: Int = 2,
) {
  val cardCornerRadiusDp: Int
    get() =
      when (cornerStyle.lowercase()) {
        "sharp", "square" -> 4
        "pill", "extra" -> 22
        else -> 14
      }

  val glassAlpha: Float
    get() = cardOpacity.coerceIn(0.45f, 1.0f)

  val useDynamicColors: Boolean
    get() = visualStyle == VisualStyleMode.MATERIAL_EXPRESSIVE

  val typographyScaleMode: TypographyScaleMode
    get() =
      when (uiScale.lowercase()) {
        "compact", "small" -> TypographyScaleMode.COMPACT
        "large", "expanded" -> TypographyScaleMode.LARGE
        else -> TypographyScaleMode.STANDARD
      }

  val soundVolumePct: Int
    get() = globalVolume

  val hapticsEnabled: Boolean
    get() = hapticFeedback
}

// ============================================================================
// PEV SPECS, PARTS & FLEET MODELS
// ============================================================================
data class PevSpecRecord(
  val name: String = "",
  val brand: String = "",
  val category: String = "SCOOTER",
  val price: String = "",
  val topSpeed: String = "",
  val range: String = "",
  val motorPower: String = "",
  val battery: String = "",
  val nominalVoltage: Int = 52,
  val weight: String = "",
  val chargingTime: String = "",
  val efficiency: String = "",
  val brakingSystem: String = "",
  val suspensionType: String = "",
  val tireProfile: String = "",
  val ipRating: String = "",
  val maxLoad: String = "",
  val siteUrl: String = "",
  val imageUrls: List<String> = emptyList(),
)

data class PevPartItem(
  val partName: String = "",
  val category: String = "",
  val exactDimensions: String = "",
  val compatibility: String = "",
  val estimatedPrice: String = "",
  val recommendedBrands: String = "",
  val sourcePlatform: String = "",
  val partUrl: String = "",
  val difficulty: String = "Easy",
  val partType: String = "Part",
  val weightLbs: Double = 1.0,
  val imageUrl: String = "",
)

data class PevVehicle(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = "SCOOTER",
  val batteryWh: Long = 1200L,
  val batteryAh: Double = 24.0,
  val voltage: Double = 52.0,
  val batteryChemistry: String = "NMC",
  val motorWatts: Int = 2000,
  val topSpeedKmh: Double = 55.0,
  val vehicleWeightLbs: Double = 68.0,
  val targetPsi: Double = 36.0,
  val odometerKm: Double = 0.0,
  val serviceIntervalKm: Double = 500.0,
  val tireSize: String = "10x3.0",
  val photoUri: String = "",
  val brandLogoUri: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RiderProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val callsign: String = "",
  val email: String = "",
  val activeVehicleId: String = "",
  val unitSystem: String = "IMPERIAL",
  val emergencyContactName: String = "",
  val sosContactNumber: String = "",
  val medicalProfile: String = "",
  val acceptedLegal: Boolean = false,
  val riderWeightLbs: Double = 175.0,
  val avatarBase64: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class PublicPilotProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "",
  val rankBadge: String = "Rookie Pilot",
  val xpPoints: Long = 0L,
  val totalKm: Double = 0.0,
  val ridesCount: Long = 0L,
  val verified: Boolean = false,
  val banned: Boolean = false,
  val trustScore: Long = 100L,
  val vehicleSummary: String = "PEV Unit",
  val visibility: String = "PUBLIC",
  val avatarBase64: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RideSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val vehicleId: String = "",
  val vehicleName: String = "",
  val title: String = "",
  val durationSec: Long = 0L,
  val distanceKm: Double = 0.0,
  val maxSpeedKmh: Double = 0.0,
  val avgSpeedKmh: Double = 0.0,
  val elevationGainM: Double = 0.0,
  val whUsed: Double = 0.0,
  val startBatteryPct: Int = -1,
  val endBatteryPct: Int = -1,
  val weatherSummary: String = "",
  val aiDebrief: String = "",
  val polylinePoints: List<String> = emptyList(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class MaintenanceTask(
  @DocumentId val id: String = "",
  val userId: String = "",
  val vehicleId: String = "",
  val vehicleName: String = "",
  val title: String = "",
  val notes: String = "",
  val dueOdometerKm: Double = 0.0,
  val completed: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class MaintenanceWearState(
  val itemId: String = "",
  val label: String = "",
  val ratedKm: Double = 1000.0,
  val lastResetKm: Double = 0.0,
)

data class ServiceLogRecord(
  val id: String = "",
  val dateLabel: String = "",
  val component: String = "",
  val notes: String = "",
  val costUsd: Double = 0.0,
  val odometerKm: Double = 0.0,
)

data class SavedVideo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  val title: String = "",
  val channelTitle: String = "",
  val thumbnailUrl: String = "",
  val publishedAt: String = "",
  val categoryTag: String = "PEV Tech",
  val personalNote: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class PrayerJournalEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val category: String = "Trail Safety",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class UserNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val message: String = "",
  val type: String = "INFO",
  val read: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

// ============================================================================
// COMMUNITY BOARD, RADAR, WALKIE & DIRECT MESSAGES
// ============================================================================
data class BoardPost(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val authorBadge: String = "Rookie Pilot",
  val category: String = "UNIVERSAL",
  val title: String = "",
  val body: String = "",
  val imageUrl: String = "",
  val pollQuestion: String = "",
  val pollOptions: List<String> = emptyList(),
  val priceUsd: Double = 0.0,
  val locationLabel: String = "",
  val telemetrySummary: String = "",
  val helpNeeded: Boolean = false,
  val speedTrap: Boolean = false,
  val stolenAlert: Boolean = false,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val flaggedCount: Long = 0L,
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class BoardComment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val body: String = "",
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RadarRider(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "",
  val vehicleName: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val speedKmh: Double = 0.0,
  val headingDeg: Double = 0.0,
  val batteryPct: Long = 100L,
  val statusTag: String = "CRUISING",
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RadarMeetup(
  @DocumentId val id: String = "",
  val userId: String = "",
  val creatorCallsign: String = "",
  val title: String = "",
  val description: String = "",
  val scheduledTime: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val attendeeCount: Long = 1L,
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RadarHazard(
  @DocumentId val id: String = "",
  val userId: String = "",
  val creatorCallsign: String = "",
  val hazardType: String = "POTHOLE",
  val description: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val upvotes: Long = 1L,
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class RadarWaypoint(
  @DocumentId val id: String = "",
  val userId: String = "",
  val creatorCallsign: String = "",
  val waypointType: String = "CHARGER",
  val title: String = "",
  val notes: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class ChannelPresence(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "",
  val channelId: String = "",
  val active: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class ChannelSpeakerState(
  @DocumentId val id: String = "",
  val speakerUid: String = "",
  val speakerCallsign: String = "",
  val channelId: String = "",
  val active: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class WalkieVoiceLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val senderCallsign: String = "",
  val channelId: String = "",
  val audioBase64: String = "",
  val durationMs: Long = 0L,
  val active: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class ChannelWebRtcSignal(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderCallsign: String = "",
  val targetUid: String = "",
  val channelId: String = "",
  val signalType: String = "ICE",
  val payload: String = "",
  val active: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class DmThread(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val participantCallsigns: List<String> = emptyList(),
  val lastMessage: String = "",
  val selfDestructSec: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class DmMessage(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val senderId: String = "",
  val senderCallsign: String = "",
  val text: String = "",
  val imageUrl: String = "",
  val voiceBase64: String = "",
  val selfDestructSec: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class CallSession(
  @DocumentId val id: String = "",
  val callerUid: String = "",
  val callerCallsign: String = "",
  val calleeUid: String = "",
  val calleeCallsign: String = "",
  val callType: String = "AUDIO",
  val status: String = "RINGING",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class CallSignal(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val signalType: String = "ICE",
  val payload: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

data class EmergencyBroadcast(
  @DocumentId val id: String = "",
  val category: String = "HAZARD ALERT",
  val title: String = "",
  val body: String = "",
  val message: String = "",
  val severity: String = "WARNING",
  val active: Boolean = true,
  val createdAt: Timestamp? = null,
)

data class YoutubeComment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val text: String = "",
  val visibility: String = "PUBLIC",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

// ============================================================================
// WEATHER, NEWS, YOUTUBE & TURN-BY-TURN NAVIGATION MODELS
// ============================================================================
data class WeatherGeoLocation(
  val name: String = "",
  val admin1: String = "",
  val country: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
) {
  fun displayTitle(): String =
    listOf(name, admin1, country).filter { it.isNotBlank() }.joinToString(", ")

  fun displayLabel(): String = displayTitle()
}

data class HourlyForecast(
  val timeLabel: String = "",
  val tempC: Double = 0.0,
  val windKmh: Double = 0.0,
  val precipPct: Int = 0,
)

data class DailyForecast(
  val dateLabel: String = "",
  val minTempC: Double = 0.0,
  val maxTempC: Double = 0.0,
  val precipPct: Int = 0,
  val windMaxKmh: Double = 0.0,
)

data class WeatherMatrixData(
  val tempC: Double = 18.0,
  val feelsLikeC: Double = 18.0,
  val windSpeedKmh: Double = 0.0,
  val windGustKmh: Double = 0.0,
  val windDirDeg: Int = 0,
  val precipProbPct: Int = 0,
  val uvIndex: Double = 0.0,
  val pressureHpa: Double = 1013.0,
  val humidityPct: Int = 50,
  val weatherCode: Int = 0,
  val conditionLabel: String = "Clear Sky",
  val locationLabel: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val isGpsSource: Boolean = true,
  val sunrise: String = "--:--",
  val sunset: String = "--:--",
  val hourly: List<HourlyForecast> = emptyList(),
  val daily: List<DailyForecast> = emptyList(),
)

data class YouTubeVideoItem(
  val videoId: String = "",
  val title: String = "",
  val description: String = "",
  val channelTitle: String = "Rural ERides",
  val publishedAt: String = "",
  val thumbnailUrl: String = "",
  val viewCount: String = "--",
  val likeCount: String = "--",
  val durationLabel: String = "--:--",
)

data class NewsArticleItem(
  val title: String = "",
  val link: String = "",
  val source: String = "",
  val publishedAt: String = "",
  val summary: String = "",
)

data class LiveChargingStation(
  val id: String = "",
  val title: String = "",
  val address: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val distanceKm: Double = 0.0,
  val powerKw: String = "AC Outlet / EVSE",
  val operatorName: String = "Public EVSE",
  val isFree: Boolean = false,
)

data class NavManeuverStep(
  val instruction: String = "",
  val streetName: String = "",
  val distanceM: Double = 0.0,
  val durationSec: Double = 0.0,
  val maneuverType: String = "straight",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
)

data class ActiveNavRoute(
  val destinationName: String = "",
  val destLat: Double = 0.0,
  val destLng: Double = 0.0,
  val profile: NavRouteProfile = NavRouteProfile.FAST_STREET,
  val totalDistanceKm: Double = 0.0,
  val totalDurationSec: Long = 0L,
  val elevationGainM: Double = 0.0,
  val polylinePoints: List<Pair<Double, Double>> = emptyList(),
  val steps: List<NavManeuverStep> = emptyList(),
  val providerSource: String = "OSRM Live Routing",
  val estimatedBatteryPctNeeded: Int = 10,
)

// ============================================================================
// COPILOT AI, VISION DIAGNOSTICS & LIVE TELEMETRY SNAPSHOT
// ============================================================================
data class CopilotMessage(
  val id: String = "",
  val isUser: Boolean = false,
  val text: String = "",
  val persona: String = "Tactical Coach",
  val widgetTag: String? = null,
  val timestampMs: Long = System.currentTimeMillis(),
)

data class SavedCopilotBriefing(
  val id: String = "",
  val title: String = "",
  val persona: String = "",
  val content: String = "",
  val savedAtMs: Long = System.currentTimeMillis(),
)

data class CopilotPersonaOption(
  val id: String,
  val name: String,
  val subtitle: String,
  val systemPrompt: String,
) {
  val roleSummary: String
    get() = subtitle
}

data class CopilotSkillOption(
  val id: String,
  val title: String,
  val category: String,
  val promptTemplate: String,
)

data class VisionAnomalyBox(
  val label: String = "",
  val severity: String = "WARNING",
  val xNorm: Float = 0.1f,
  val yNorm: Float = 0.1f,
  val wNorm: Float = 0.3f,
  val hNorm: Float = 0.3f,
  val confidence: Float = 0.9f,
)

data class MechanicDiagMessage(
  val id: String = "",
  val isUser: Boolean = false,
  val text: String = "",
  val checklist: List<String> = emptyList(),
  val anomalies: List<VisionAnomalyBox> = emptyList(),
  val imageUris: List<String> = emptyList(),
  val timestampMs: Long = System.currentTimeMillis(),
)

data class LiveTelemetrySnapshot(
  val recording: Boolean = false,
  val paused: Boolean = false,
  val speedKmh: Double = 0.0,
  val maxSpeedKmh: Double = 0.0,
  val avgSpeedKmh: Double = 0.0,
  val tripDistanceKm: Double = 0.0,
  val elapsedSec: Long = 0L,
  val altitudeM: Double = 0.0,
  val elevationGainM: Double = 0.0,
  val bearingDeg: Float = 0f,
  val gpsAccuracyM: Float = 0f,
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val launchLat: Double? = null,
  val launchLng: Double? = null,
  val gForceTotal: Float = 1.0f,
  val peakG: Float = 1.0f,
  val pitchDeg: Float = 0f,
  val rollDeg: Float = 0f,
  val yawDeg: Float = 0f,
  val gyroPitchRateDps: Float = 0f,
  val gyroRollRateDps: Float = 0f,
  val gyroYawRateDps: Float = 0f,
  val magMicroTesla: Float = 0f,
  val ahrsFused9Dof: Boolean = false,
  val sensorAccuracy: Int = 3,
  val peakLeanLeftDeg: Float = 0f,
  val peakLeanRightDeg: Float = 0f,
  val lateralG: Float = 0f,
  val longitudinalG: Float = 0f,
  val crashDetected: Boolean = false,
  val roadRoughnessG: Float = 0f,
  val barometerHpa: Float = 0f,
  val baroAltitudeM: Float = 0f,
  val ambientLightLux: Float = -1f,
  val proximityNear: Boolean = false,
  val ambientTempC: Float? = null,
  val stepCountSession: Int = 0,
  val activeSensorsList: List<String> = emptyList(),
  val satellitesUsed: Int = 0,
  val satellitesVisible: Int = 0,
  val avgSatelliteSnrDb: Float = 0f,
  val satelliteConstellations: String = "Searching...",
  val batteryPct: Int = 100,
  val batteryTempC: Double = 25.0,
  val estimatedVoltage: Double = 52.0,
  val roadName: String = "",
  val speedLimitMph: Int? = null,
  val inSchoolZone: Boolean = false,
  val trailPoints: List<Pair<Double, Double>> = emptyList(),
)

// ============================================================================
// LOCAL OFFLINE RIDE & COPILOT CHAT DATABASE VAULT
// ============================================================================
data class OfflineRideEntity(
  val id: String = "",
  val localId: String = id,
  val userId: String = "",
  val vehicleId: String = "",
  val vehicleName: String = "",
  val title: String = "",
  val durationSec: Long = 0L,
  val distanceKm: Double = 0.0,
  val maxSpeedKmh: Double = 0.0,
  val avgSpeedKmh: Double = 0.0,
  val elevationGainM: Double = 0.0,
  val whUsed: Double = 0.0,
  val startBatteryPct: Int = -1,
  val endBatteryPct: Int = -1,
  val weatherSummary: String = "",
  val aiDebrief: String = "",
  val polylineCsv: String = "",
  val polylineJson: String = "[]",
  val createdAtMs: Long = System.currentTimeMillis(),
  val syncedToCloud: Boolean = false,
)

data class CopilotSessionEntity(
  val id: String = "",
  val sessionId: String = id,
  val userId: String = "local_pilot",
  val title: String = "",
  val persona: String = "Tactical Coach",
  val updatedAtMs: Long = System.currentTimeMillis(),
)

data class CopilotMessageEntity(
  val id: String = "",
  val messageId: String = id,
  val sessionId: String = "",
  val isUser: Boolean = false,
  val text: String = "",
  val persona: String = "Tactical Coach",
  val widgetTag: String? = null,
  val timestampMs: Long = System.currentTimeMillis(),
)

class OfflineRideDao(private val context: Context) {
  private val prefs by lazy {
    context.getSharedPreferences("volttrail_offline_rides_db", Context.MODE_PRIVATE)
  }

  private val ridesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<OfflineRideEntity>>(emptyList())

  init {
    ridesFlow.value = loadAllInternal()
  }

  private fun loadAllInternal(): MutableList<OfflineRideEntity> {
    val raw = prefs.getString("rides_json", "[]").orEmpty()
    return runCatching {
      val arr = JSONArray(raw)
      val list = mutableListOf<OfflineRideEntity>()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val resolvedId = o.optString("id").ifBlank { o.optString("localId") }
        list +=
          OfflineRideEntity(
            id = resolvedId,
            localId = resolvedId,
            userId = o.optString("userId"),
            vehicleId = o.optString("vehicleId"),
            vehicleName = o.optString("vehicleName"),
            title = o.optString("title"),
            durationSec = o.optLong("durationSec", 0L),
            distanceKm = o.optDouble("distanceKm", 0.0),
            maxSpeedKmh = o.optDouble("maxSpeedKmh", 0.0),
            avgSpeedKmh = o.optDouble("avgSpeedKmh", 0.0),
            elevationGainM = o.optDouble("elevationGainM", 0.0),
            whUsed = o.optDouble("whUsed", 0.0),
            startBatteryPct = o.optInt("startBatteryPct", -1),
            endBatteryPct = o.optInt("endBatteryPct", -1),
            weatherSummary = o.optString("weatherSummary"),
            aiDebrief = o.optString("aiDebrief"),
            polylineCsv = o.optString("polylineCsv"),
            polylineJson = o.optString("polylineJson", "[]"),
            createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
            syncedToCloud = o.optBoolean("syncedToCloud", false),
          )
      }
      list
    }.getOrDefault(mutableListOf())
  }

  private fun saveAllInternal(list: List<OfflineRideEntity>) {
    val arr = JSONArray()
    list.forEach { r ->
      val effectiveId = r.id.ifBlank { r.localId }
      arr.put(
        JSONObject().apply {
          put("id", effectiveId)
          put("localId", effectiveId)
          put("userId", r.userId)
          put("vehicleId", r.vehicleId)
          put("vehicleName", r.vehicleName)
          put("title", r.title)
          put("durationSec", r.durationSec)
          put("distanceKm", r.distanceKm)
          put("maxSpeedKmh", r.maxSpeedKmh)
          put("avgSpeedKmh", r.avgSpeedKmh)
          put("elevationGainM", r.elevationGainM)
          put("whUsed", r.whUsed)
          put("startBatteryPct", r.startBatteryPct)
          put("endBatteryPct", r.endBatteryPct)
          put("weatherSummary", r.weatherSummary)
          put("aiDebrief", r.aiDebrief)
          put("polylineCsv", r.polylineCsv)
          put("polylineJson", r.polylineJson)
          put("createdAtMs", r.createdAtMs)
          put("syncedToCloud", r.syncedToCloud)
        }
      )
    }
    prefs.edit().putString("rides_json", arr.toString()).apply()
    ridesFlow.value = list
  }

  fun observeRides(userId: String): kotlinx.coroutines.flow.Flow<List<OfflineRideEntity>> =
    ridesFlow.map { all ->
      all.filter { it.userId == userId }.sortedByDescending { it.createdAtMs }
    }

  suspend fun upsert(entity: OfflineRideEntity) {
    val targetId = entity.id.ifBlank { entity.localId }
    val current = loadAllInternal().filterNot { (it.id.ifBlank { it.localId }) == targetId }.toMutableList()
    current.add(0, entity.copy(id = targetId, localId = targetId))
    saveAllInternal(current.take(200))
  }

  suspend fun insert(entity: OfflineRideEntity) = upsert(entity)

  suspend fun getPendingSync(userId: String): List<OfflineRideEntity> =
    loadAllInternal().filter { it.userId == userId && !it.syncedToCloud }

  suspend fun getAllForUser(userId: String): List<OfflineRideEntity> =
    loadAllInternal()
      .filter { it.userId == userId }
      .sortedByDescending { it.createdAtMs }

  suspend fun markSynced(localId: String) {
    val updated =
      loadAllInternal().map {
        if (it.id == localId || it.localId == localId) it.copy(syncedToCloud = true) else it
      }
    saveAllInternal(updated)
  }

  suspend fun deleteRide(localId: String) {
    val updated = loadAllInternal().filterNot { it.id == localId || it.localId == localId }
    saveAllInternal(updated)
  }
}

class CopilotChatDao(private val context: Context) {
  private val prefs by lazy {
    context.getSharedPreferences("volttrail_copilot_chat_db", Context.MODE_PRIVATE)
  }

  private val sessionsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<CopilotSessionEntity>>(emptyList())
  private val messagesBySession = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<List<CopilotMessageEntity>>>()

  init {
    sessionsFlow.value = loadSessionsInternal()
  }

  private fun getMessagesFlow(sessionId: String): kotlinx.coroutines.flow.MutableStateFlow<List<CopilotMessageEntity>> =
    synchronized(messagesBySession) {
      messagesBySession.getOrPut(sessionId) {
        kotlinx.coroutines.flow.MutableStateFlow(loadMessagesInternal(sessionId).sortedBy { it.timestampMs })
      }
    }

  private fun loadSessionsInternal(): MutableList<CopilotSessionEntity> {
    val raw = prefs.getString("sessions_json", "[]").orEmpty()
    return runCatching {
      val arr = JSONArray(raw)
      val list = mutableListOf<CopilotSessionEntity>()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val sid = o.optString("id").ifBlank { o.optString("sessionId") }
        list +=
          CopilotSessionEntity(
            id = sid,
            sessionId = sid,
            userId = o.optString("userId", "local_pilot"),
            title = o.optString("title"),
            persona = o.optString("persona", "Tactical Coach"),
            updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
          )
      }
      list
    }.getOrDefault(mutableListOf())
  }

  private fun saveSessionsInternal(list: List<CopilotSessionEntity>) {
    val arr = JSONArray()
    list.forEach { s ->
      val sid = s.id.ifBlank { s.sessionId }
      arr.put(
        JSONObject().apply {
          put("id", sid)
          put("sessionId", sid)
          put("userId", s.userId)
          put("title", s.title)
          put("persona", s.persona)
          put("updatedAtMs", s.updatedAtMs)
        }
      )
    }
    prefs.edit().putString("sessions_json", arr.toString()).apply()
    sessionsFlow.value = list
  }

  private fun loadMessagesInternal(sessionId: String): MutableList<CopilotMessageEntity> {
    val raw = prefs.getString("msgs_$sessionId", "[]").orEmpty()
    return runCatching {
      val arr = JSONArray(raw)
      val list = mutableListOf<CopilotMessageEntity>()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val mid = o.optString("id").ifBlank { o.optString("messageId") }
        list +=
          CopilotMessageEntity(
            id = mid,
            messageId = mid,
            sessionId = o.optString("sessionId", sessionId),
            isUser = o.optBoolean("isUser", false),
            text = o.optString("text"),
            persona = o.optString("persona", "Tactical Coach"),
            widgetTag = o.optString("widgetTag").takeIf { it.isNotBlank() },
            timestampMs = o.optLong("timestampMs", System.currentTimeMillis()),
          )
      }
      list
    }.getOrDefault(mutableListOf())
  }

  private fun saveMessagesInternal(sessionId: String, list: List<CopilotMessageEntity>) {
    val arr = JSONArray()
    list.forEach { m ->
      val mid = m.id.ifBlank { m.messageId }
      arr.put(
        JSONObject().apply {
          put("id", mid)
          put("messageId", mid)
          put("sessionId", m.sessionId)
          put("isUser", m.isUser)
          put("text", m.text)
          put("persona", m.persona)
          put("widgetTag", m.widgetTag ?: "")
          put("timestampMs", m.timestampMs)
        }
      )
    }
    prefs.edit().putString("msgs_$sessionId", arr.toString()).apply()
    getMessagesFlow(sessionId).value = list.sortedBy { it.timestampMs }
  }

  fun observeSessions(userId: String): kotlinx.coroutines.flow.Flow<List<CopilotSessionEntity>> =
    sessionsFlow.map { all ->
      all.filter { it.userId == userId || it.userId.isBlank() }.sortedByDescending { it.updatedAtMs }
    }

  fun observeMessages(sessionId: String): kotlinx.coroutines.flow.Flow<List<CopilotMessageEntity>> =
    getMessagesFlow(sessionId)

  suspend fun getSessions(): List<CopilotSessionEntity> =
    loadSessionsInternal().sortedByDescending { it.updatedAtMs }

  suspend fun upsertSession(session: CopilotSessionEntity) {
    val sid = session.id.ifBlank { session.sessionId }
    val current =
      loadSessionsInternal().filterNot { (it.id.ifBlank { it.sessionId }) == sid }.toMutableList()
    current.add(0, session.copy(id = sid, sessionId = sid))
    saveSessionsInternal(current.take(60))
  }

  suspend fun getMessages(sessionId: String): List<CopilotMessageEntity> =
    loadMessagesInternal(sessionId).sortedBy { it.timestampMs }

  suspend fun insertMessage(msg: CopilotMessageEntity) {
    val mid = msg.id.ifBlank { msg.messageId }
    val current =
      loadMessagesInternal(msg.sessionId)
        .filterNot { (it.id.ifBlank { it.messageId }) == mid }
        .toMutableList()
    current.add(msg.copy(id = mid, messageId = mid))
    saveMessagesInternal(msg.sessionId, current.takeLast(200))
  }

  suspend fun deleteSessionMessages(sessionId: String) {
    prefs.edit().remove("msgs_$sessionId").apply()
    getMessagesFlow(sessionId).value = emptyList()
  }

  suspend fun clearSessionMessages(sessionId: String) = deleteSessionMessages(sessionId)

  suspend fun deleteSession(sessionId: String) {
    prefs.edit().remove("msgs_$sessionId").apply()
    getMessagesFlow(sessionId).value = emptyList()
    val updated = loadSessionsInternal().filterNot { (it.id.ifBlank { it.sessionId }) == sessionId }
    saveSessionsInternal(updated)
  }
}

class AppDatabase private constructor(context: Context) {
  private val appContext = context.applicationContext
  private val rideDao by lazy { OfflineRideDao(appContext) }
  private val chatDao by lazy { CopilotChatDao(appContext) }

  fun offlineRideDao(): OfflineRideDao = rideDao
  fun copilotChatDao(): CopilotChatDao = chatDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance ?: AppDatabase(context.applicationContext).also { instance = it }
        }
  }
}
