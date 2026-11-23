package com.example.data

import androidx.annotation.Keep
import com.example.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

// ============================================================================
// UNIFIED AUTHENTICATED SESSION IDENTITY (GOOGLE + SUPABASE)
// ============================================================================

enum class AuthProviderType {
  GOOGLE,
  SUPABASE,
}

data class ActiveSessionUser(
  val uid: String,
  val rawId: String,
  val email: String,
  val displayName: String,
  val provider: AuthProviderType,
  val firebaseAuthenticated: Boolean,
)

// ============================================================================
// FIRESTORE DOMAIN MODELS (1:1 PARITY WITH firebase-blueprint & firestore.rules)
// ============================================================================

@Keep
@IgnoreExtraProperties
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
  val riderWeightLbs: Double = 0.0,
  val avatarBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PevVehicle(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = "SCOOTER",
  val batteryWh: Long = 1000L,
  val batteryAh: Double = 0.0,
  val voltage: Double = 52.0,
  val batteryChemistry: String = "NMC",
  val motorWatts: Int = 0,
  val topSpeedKmh: Double = 0.0,
  val vehicleWeightLbs: Double = 0.0,
  val targetPsi: Double = 36.0,
  val odometerKm: Double = 0.0,
  val serviceIntervalKm: Double = 500.0,
  val tireSize: String = "10x3.0",
  val photoUri: String = "",
  val brandLogoUri: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MaintenanceTask(
  @DocumentId val id: String = "",
  val userId: String = "",
  val vehicleId: String = "",
  val vehicleName: String = "",
  val title: String = "",
  val notes: String = "",
  val dueOdometerKm: Double = 0.0,
  val completed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedVideo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  val title: String = "",
  val channelTitle: String = "",
  val thumbnailUrl: String = "",
  val publishedAt: String = "",
  val categoryTag: String = "General",
  val personalNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PrayerJournalEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val category: String = "Trail Safety",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderCallsign: String = "",
  val type: String = "SYSTEM",
  val title: String = "",
  val body: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
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
  val vehicleSummary: String = "",
  val visibility: String = "PUBLIC",
  val avatarBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BoardPost(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val authorBadge: String = "",
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BoardComment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val body: String = "",
  val visibility: String = "PUBLIC",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RadarHazard(
  @DocumentId val id: String = "",
  val userId: String = "",
  val creatorCallsign: String = "",
  val hazardType: String = "POTHOLE",
  val description: String = "",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val upvotes: Long = 0L,
  val visibility: String = "PUBLIC",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChannelPresence(
  @DocumentId val id: String = "",
  val userId: String = "",
  val callsign: String = "",
  val channelId: String = "",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChannelSpeakerState(
  @DocumentId val id: String = "",
  val speakerUid: String = "",
  val speakerCallsign: String = "",
  val channelId: String = "",
  val active: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WalkieVoiceLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val senderCallsign: String = "",
  val channelId: String = "",
  val audioBase64: String = "",
  val durationMs: Long = 1000L,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChannelWebRtcSignal(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderCallsign: String = "",
  val targetUid: String = "",
  val channelId: String = "",
  val signalType: String = "JOIN",
  val payload: String = "",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DmThread(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val participantCallsigns: List<String> = emptyList(),
  val lastMessage: String = "",
  val selfDestructSec: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DmMessage(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val senderId: String = "",
  val senderCallsign: String = "",
  val text: String = "",
  val imageUrl: String = "",
  val voiceBase64: String = "",
  val selfDestructSec: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CallSession(
  @DocumentId val id: String = "",
  val callerUid: String = "",
  val callerCallsign: String = "",
  val calleeUid: String = "",
  val calleeCallsign: String = "",
  val callType: String = "AUDIO",
  val status: String = "RINGING",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CallSignal(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val signalType: String = "OFFER",
  val payload: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class EmergencyBroadcast(
  @DocumentId val id: String = "",
  val userId: String = "",
  val category: String = "SYSTEM ALERT",
  val message: String = "",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class YoutubeEngagement(
  @DocumentId val id: String = "",
  val videoId: String = "",
  val voltsCount: Long = 0L,
  val visibility: String = "PUBLIC",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class YoutubeComment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorCallsign: String = "",
  val text: String = "",
  val visibility: String = "PUBLIC",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

// ============================================================================
// UI, TELEMETRY, THEME & SETTINGS ENUMS / MODELS
// ============================================================================

enum class AppThemePreset(val id: String, val label: String, val hexColor: Long) {
  LIME("lime", "Tactical Lime", 0xFF39FF14),
  CYAN("cyan", "Cyber Cyan", 0xFF06B6D4),
  EMERALD("emerald", "Trail Emerald", 0xFF10B981),
  AMBER("amber", "Solar Amber", 0xFFF59E0B),
  ROSE("rose", "Crimson Pulse", 0xFFF43F5E),
  PURPLE("purple", "Void Purple", 0xFFA855F7),
  VOLT_YELLOW("volt_yellow", "72V Volt Yellow", 0xFFEAB308),
  MOTOR_ORANGE("motor_orange", "Hub Motor Orange", 0xFFFF6B00),
  REGEN_TEAL("regen_teal", "Regen Mint Teal", 0xFF14F1D9),
  PLASMA_MAGENTA("plasma_magenta", "Plasma Neon", 0xFFFF2EE6),
  VOID("void", "Stealth White", 0xFFFFFFFF),
}

enum class VisualStyleMode(val id: String, val label: String, val description: String) {
  DARK_STUDIO("dark_studio", "Dark Tactical Studio", "Stealth obsidian HUD with neon telemetry"),
  PEV_CYBER_CARBON("pev_cyber_carbon", "Carbon Hyper-PEV", "Weave carbon dark cockpit with high-voltage lime & ice-cyan HUD"),
  PEV_TRAIL_FOREST("pev_trail_forest", "Forest EUC Trail", "Deep Pacific Northwest pine canopy with crisp emerald-mint instruments"),
  PEV_VOLT_RACER("pev_volt_racer", "72V Volt Racing", "Track-ready obsidian asphalt with electric volt-yellow & racing red gauges"),
  PEV_MIDNIGHT_NEON("pev_midnight_neon", "Midnight Scooter Run", "Wet neon street-run aesthetic with glowing cyan & lime reflections"),
  PEV_DESERT_ENDURO("pev_desert_enduro", "Desert Canyon eMoto", "Red-rock twilight canyon slate with glowing amber & copper accents"),
  PEV_ARCTIC_ICE("pev_arctic_ice", "Arctic Cold-Cell", "Sub-zero glacier navy with frosted ice-blue battery telemetry"),
  PEV_SUNSET_CRUISE("pev_sunset_cruise", "Coastal Sunset Glide", "Deep ocean twilight with warm sunset gold & horizon teal"),
  PEV_TACTICAL_NEON("pev_tactical_neon", "Tactical Neon 9-DOF", "High-contrast obsidian avionics with laser lime, cyber cyan & amber needles"),
  PEV_APEX_TRACK("pev_apex_track", "Apex Track Telemetry", "Motorsport carbon deck with crimson pulse & crisp white lap gauges"),
  PEV_GRAVEL_DUST("pev_gravel_dust", "Rural Gravel Enduro", "Dark basalt trail slate with burnt copper & off-road gold accents"),
  PEV_TITANIUM_PRO("pev_titanium_pro", "Titanium CNC Pro", "Brushed dark gunmetal aluminum with ice-blue precision lines"),
  PEV_LITHIUM_LAB("pev_lithium_lab", "21700 Lithium Lab", "Deep cell-engineering emerald slate with high-vis BMS green"),
  PEV_NIGHT_CONVOY("pev_night_convoy", "Night Convoy Stealth", "Deep midnight indigo with amber headlight gold & neon tail glow"),
  WINDOWS_CLASSIC("windows_classic", "Windows Classic 95", "3D silver bevels, navy title bars & classic teal desktop"),
  WINDOWS_2000("windows_2000", "Windows 2000 Pro", "Slate #3A6EA5 desktop, warm putty chrome & navy-to-sky title bars"),
  WINDOWS_XP("windows_xp", "Windows XP Luna", "Bliss hills desktop, royal Luna blue title bars & green Start button"),
  WINDOWS_7("windows_7", "Windows 7 Aero", "Frosted sapphire Aero glass windows & glowing Orb taskbar"),
  WINDOWS_10("windows_10", "Windows 10 Fluent", "Sharp Fluent Reveal borders, dark matte taskbar & crisp accent chrome"),
  WINDOWS_11("windows_11", "Windows 11 Mica", "Silky Bloom desktop, translucent Mica rounded windows & centered taskbar"),
  M3_DYNAMIC("m3_dynamic", "Material 3 Dynamic", "Android wallpaper-derived adaptive palette"),
  WARM_EDITORIAL("warm_editorial", "Warm Editorial", "Earth-toned trail journal with crisp contrast"),
  VIBRANT_CARDS("vibrant_cards", "Vibrant Cards", "Bold gradient contrast & kinetic surface pop"),
  BRUTALIST_HIGH_VIS("brutalist_high_vis", "Brutalist High-Vis", "Zero-radius high-contrast industrial chrome"),
  VAPORWAVE_RETRO("vaporwave_retro", "Vaporwave Synth", "Retro neon magenta-cyan midnight CRT palette"),
  OLED_STEALTH("oled_stealth", "OLED True Black", "Pure #000000 battery-saving night tactical HUD"),
  RURAL_BARNWOOD("rural_barnwood", "Rural Barnwood Shop", "Warm cedar barn slate with lantern amber & olive pasture trim"),
  RURAL_SUNSET_GRAVEL("rural_sunset_gravel", "Gravel Backroad Dusk", "Warm twilight terracotta dirt road with golden headlight glow"),
  RURAL_PINE_HOLLER("rural_pine_holler", "Appalachian Pine Holler", "Deep misty hemlock evergreen with moss-lime trail telemetry"),
  RURAL_PRAIRIE_GOLD("rural_prairie_gold", "Heartland Prairie Gold", "Midnight wheat-field charcoal with harvest gold & sky cyan"),
  RURAL_CREEK_MIST("rural_creek_mist", "Creek Crossing Mist", "Riverstone slate blue with cool spring-water mint accents"),
  RURAL_HARVEST_IRON("rural_harvest_iron", "Farmhouse Cast Iron", "Forged shop iron black with tractor green & safety orange"),
}

enum class VoiceDspProfile {
  WARM_PORCH,
  DEEP_BARN,
  BRIGHT_DISPATCH,
  TACTICAL_CRISP,
  WARM_SCOUT,
  OUTBACK_AU,
  HIGHLAND_UK,
  CB_RADIO,
}

enum class AiVoiceProfile(
  val id: String,
  val label: String,
  val description: String,
  val pitch: Float,
  val speechRate: Float,
  val voiceHint: String,
  val sampleLine: String,
  val geminiVoiceName: String,
  val styleDirective: String,
  val dspProfile: VoiceDspProfile,
) {
  WARM_RURAL_PORCH(
    id = "warm_rural_porch",
    label = "Warm Country Porch",
    description = "Friendly, steady rural hospitality & unhurried cadence",
    pitch = 0.96f,
    speechRate = 0.96f,
    voiceHint = "en-us-male-warm",
    sampleLine = "Howdy neighbor, welcome back to the gravel! Battery's strong and the backroads are clear.",
    geminiVoiceName = "Achird",
    styleDirective = "Speak warmly and conversationally in an unhurried, friendly rural porch neighbor tone",
    dspProfile = VoiceDspProfile.WARM_PORCH,
  ),
  BARN_SHOP_GRANDPA(
    id = "barn_shop_grandpa",
    label = "Barn Shop Grandpa",
    description = "Deep, gravelly old-school mechanic wisdom",
    pitch = 0.76f,
    speechRate = 0.88f,
    voiceHint = "en-us-male-deep",
    sampleLine = "Grab a wrench and pull up a stool, friend. Let's check them brake pads and tire PSI right.",
    geminiVoiceName = "Enceladus",
    styleDirective = "Speak in a deep, gravelly, seasoned old-school barn mechanic voice with warm wisdom",
    dspProfile = VoiceDspProfile.DEEP_BARN,
  ),
  COUNTRY_DISPATCH_GAL(
    id = "country_dispatch_gal",
    label = "Country Trail Dispatch",
    description = "Bright, reassuring rural trail guide voice",
    pitch = 1.15f,
    speechRate = 1.02f,
    voiceHint = "en-us-female-bright",
    sampleLine = "Hey there trail rider! Creek bridge ahead is dry, and you've got plenty of juice to get home.",
    geminiVoiceName = "Zephyr",
    styleDirective = "Speak in a bright, upbeat, encouraging female outdoor trail guide voice",
    dspProfile = VoiceDspProfile.BRIGHT_DISPATCH,
  ),
  TACTICAL_COMMANDER(
    id = "tactical_commander",
    label = "Tactical HUD Comms",
    description = "Crisp, clear high-speed cockpit callouts",
    pitch = 0.98f,
    speechRate = 1.10f,
    voiceHint = "en-us-male-crisp",
    sampleLine = "Telemetry locked. Voltage nominal at 58 volts. Keep eyes on the gravel curve ahead.",
    geminiVoiceName = "Orus",
    styleDirective = "Speak with crisp, firm, high-clarity tactical cockpit avionics precision",
    dspProfile = VoiceDspProfile.TACTICAL_CRISP,
  ),
  RANCH_SCOUT_FEMALE(
    id = "ranch_scout_female",
    label = "Homestead Scout",
    description = "Warm, grounded female rural trail voice",
    pitch = 1.05f,
    speechRate = 0.95f,
    voiceHint = "en-us-female-warm",
    sampleLine = "Sun's dipping over the ridge, partner. Flip on your headlight and enjoy the quiet country air.",
    geminiVoiceName = "Sulafat",
    styleDirective = "Speak in a warm, grounded, calm female homestead trail scout voice",
    dspProfile = VoiceDspProfile.WARM_SCOUT,
  ),
  AUSSIE_OUTBACK_RIDER(
    id = "aussie_outback_rider",
    label = "Outback Bush Trail",
    description = "Laid-back backcountry bush accent",
    pitch = 0.95f,
    speechRate = 1.00f,
    voiceHint = "en-au-male",
    sampleLine = "G'day mate! Dirt track looks mint today, tyres are pumped and ready to roll.",
    geminiVoiceName = "Puck",
    styleDirective = "Speak with a cheerful, laid-back Australian outback trail rider accent",
    dspProfile = VoiceDspProfile.OUTBACK_AU,
  ),
  HIGHLAND_COUNTRY_UK(
    id = "highland_country_uk",
    label = "Highland Country Lane",
    description = "Calm British countryside hedgerow guide",
    pitch = 1.02f,
    speechRate = 0.97f,
    voiceHint = "en-gb-female",
    sampleLine = "Lovely day on the country lanes. Battery cells are balanced and steady as you go.",
    geminiVoiceName = "Gacrux",
    styleDirective = "Speak in a calm, mature, articulate British countryside voice",
    dspProfile = VoiceDspProfile.HIGHLAND_UK,
  ),
  NIGHT_CONVOY_RADIO(
    id = "night_convoy_radio",
    label = "CB Convoy Radio",
    description = "Punchy CB-radio dispatch cadence with radio FX",
    pitch = 0.84f,
    speechRate = 1.08f,
    voiceHint = "en-us-male-radio",
    sampleLine = "Breaker one-nine, trail rider! Backroad's all clear out past the old creek crossing, over.",
    geminiVoiceName = "Fenrir",
    styleDirective = "Speak with a punchy, alert, energetic late-night CB convoy radio dispatcher cadence",
    dspProfile = VoiceDspProfile.CB_RADIO,
  );

  val localeTag: String
    get() =
      when {
        voiceHint.startsWith("en-gb", ignoreCase = true) -> "en-GB"
        voiceHint.startsWith("en-au", ignoreCase = true) -> "en-AU"
        else -> "en-US"
      }

  val preferFemale: Boolean
    get() = voiceHint.contains("female", ignoreCase = true)
}

enum class PevWallpaper(val id: String, val label: String, val subtitle: String) {
  AUTO_THEME("auto_theme", "Auto (Match PEV Theme)", "Automatically pairs wallpaper with active PEV theme"),
  HYPER_TRACK("hyper_track", "Night Hyper-Track Rigs", "Wet night circuit with neon lime hyper-scooter & eMoto"),
  VESC_CARBON("vesc_carbon", "Carbon & VESC Circuit", "Woven carbon fiber & glowing electric voltage traces"),
  ALPINE_RIDGE("alpine_ridge", "Alpine Twilight eMTB", "Electric trail bike headlight over misty mountain ridge"),
  CYBER_CITY_EUC("cyber_city_euc", "Neon Highway EUC Run", "High-speed electric unicycle on rain-slick neon bridge"),
  FOREST_EUC("forest_euc", "Misty Pine EUC & eMTB", "Twilight forest singletrack with headlight beam"),
  NEON_SCOOTER("neon_scooter", "Midnight Neon Scooter", "Rain-slick neon city dual-motor hyper-scooter"),
  CANYON_EMOTO("canyon_emoto", "Desert Canyon eMoto", "Red rock ridge electric dirt bike at dusk"),
  COASTAL_ONEWHEEL("coastal_onewheel", "Coastal Onewheel & eSkate", "Twilight ocean cliffside overlook"),
  MOUNTAIN_TRAIL("mountain_trail", "Rural Mountain Trail", "Backcountry trail dusk horizon"),
  COCKPIT_HERO("cockpit_hero", "Tactical PEV Cockpit", "Dark anodized cockpit energy art"),
  BOARD_CONVOY("board_convoy", "Night Convoy Pack", "Group ride lights on open road"),
  CYBER_ALLEY("cyber_alley", "Cyber Neon Alley", "Rain-slick cyberpunk alleyway with glowing electric ride lights"),
  GRAVEL_SUNSET("gravel_sunset", "Backroad Gravel Sunset", "Golden-hour dirt road horizon with dramatic sky"),
  BARN_WORKBENCH("barn_workbench", "Barn Shop Workbench", "Warm timber workshop bench with tools and amber lantern"),
  SYNTHWAVE_GRID("synthwave_grid", "Synthwave Neon Grid", "Retro 80s sunset laser grid horizon"),
  ALPINE_GLACIER("alpine_glacier", "Alpine Glacier Ridge", "Crisp frosted mountain peaks at sub-zero dawn"),
  NONE("none", "Pure Procedural Only", "Disable photo wallpaper; use procedural grid/texture"),
}

enum class RingtonePreset(val id: String, val label: String, val subtitle: String) {
  VOLT_PULSE("volt_pulse", "Volt Pulse", "Crisp rising 3-note electric cadence"),
  CYBER_ARPEGGIO("cyber_arpeggio", "Cyber Arpeggio", "Futuristic neon synth chord sweep"),
  TACTICAL_RADAR("tactical_radar", "Tactical Radar", "Dual-sweep cockpit dispatch ping"),
  HYPER_SCOOTER("hyper_scooter", "Hyper-PEV Horn", "High-energy dual-tone street chirp"),
  CLASSIC_BELL("classic_bell", "eBike Brass Bell", "Bright harmonic bicycle bell ring"),
  RETRO_CHIME_95("retro_chime_95", "Retro 95 Startup", "Nostalgic warm retro chord chime"),
  AVIATION_COCKPIT("aviation_cockpit", "Aviation Chime", "Clean avionics double-ding"),
  LASER_PING("laser_ping", "Plasma Laser", "Fast sci-fi frequency sweep"),
  TRAIL_HORN("trail_horn", "Convoy Horn", "Bold low-mid outdoor trail blast"),
  SYNTH_WAVE("synth_wave", "Synthwave 84", "4-step nostalgic vaporwave riff"),
  SONAR_ECHO("sonar_echo", "Submarine Sonar", "Deep resonant radar echo"),
  SYSTEM_DEFAULT("system_default", "Android System Ringtone", "Uses your device's default or custom ringtone"),
}

enum class AlertTonePreset(val id: String, val label: String) {
  CHIRP_TACTICAL("chirp_tactical", "Tactical Chirp"),
  ROGER_BEEPS("roger_beeps", "Walkie Roger Beep"),
  BELL_CLEAR("bell_clear", "Clear Bell"),
  CYBER_BLIP("cyber_blip", "Cyber Blip"),
  TWO_TONE_ALERT("two_tone_alert", "Two-Tone Alert"),
  SUBTLE_TAP("subtle_tap", "Subtle Tick"),
  ALARM_SIREN("alarm_siren", "Urgent Siren"),
  SILENT("silent", "Silent"),
}

enum class VibrationPatternPreset(val id: String, val label: String, val timings: LongArray) {
  STANDARD("standard", "Standard Pulse", longArrayOf(0, 60)),
  DOUBLE_TAP("double_tap", "Double Tactical Tap", longArrayOf(0, 45, 70, 55)),
  HEARTBEAT("heartbeat", "Heartbeat Cadence", longArrayOf(0, 35, 60, 80, 140, 35)),
  SOS_MORSE("sos_morse", "SOS Morse Alert", longArrayOf(0, 60, 50, 60, 50, 60, 120, 160, 50, 160)),
  RAPID_BUZZ("rapid_buzz", "Triple Rapid Buzz", longArrayOf(0, 30, 40, 30, 40, 30)),
  OFF("off", "Vibration Off", longArrayOf()),
}

enum class NavRouteProfile(val id: String, val label: String, val osrmProfile: String, val orsProfile: String, val mapboxProfile: String) {
  PEV_BIKE_LANE("pev_bike_lane", "PEV Bike Lane", "cycling", "cycling-electric", "cycling"),
  OFFROAD_TRAIL("offroad_trail", "Trail / Dirt", "cycling", "cycling-mountain", "cycling"),
  FAST_STREET("fast_street", "Fast Street", "driving", "driving-car", "driving"),
  SCENIC_LOW_TRAFFIC("scenic_low_traffic", "Quiet Backroads", "cycling", "cycling-regular", "cycling"),
}

data class NavManeuverStep(
  val instruction: String,
  val streetName: String = "",
  val distanceM: Double = 0.0,
  val durationSec: Double = 0.0,
  val maneuverType: String = "straight", // left, right, sharp_left, sharp_right, slight_left, slight_right, uturn, roundabout, arrive, depart, straight
  val lat: Double = 0.0,
  val lng: Double = 0.0,
)

data class ActiveNavRoute(
  val destinationName: String,
  val destLat: Double,
  val destLng: Double,
  val profile: NavRouteProfile,
  val totalDistanceKm: Double,
  val totalDurationSec: Long,
  val elevationGainM: Double = 0.0,
  val polylinePoints: List<Pair<Double, Double>>,
  val steps: List<NavManeuverStep>,
  val providerSource: String,
  val estimatedBatteryPctNeeded: Int = 0,
  val createdAtMs: Long = System.currentTimeMillis(),
)

data class LiveChargingStation(
  val id: String,
  val title: String,
  val address: String,
  val lat: Double,
  val lng: Double,
  val distanceKm: Double,
  val powerKw: String = "Standard AC",
  val operatorName: String = "Public Charger",
  val isFree: Boolean = false,
)

enum class ColorModeOverride(val id: String, val label: String) {
  SYSTEM("system", "System Auto"),
  DARK("dark", "Dark Cockpit"),
  LIGHT("light", "Daylight Crisp"),
}

enum class PevLegalClass(val id: String, val label: String, val maxMph: Int, val description: String) {
  CLASS_1_15("class_1", "Class 1 / Sidewalk (15 MPH)", 15, "Pedestrian-safe bikeway cap"),
  CLASS_2_20("class_2", "Class 2 PEV (20 MPH)", 20, "Standard urban scooter & e-bike limit"),
  CLASS_3_28("class_3", "Class 3 Commuter (28 MPH)", 28, "Street & Class 3 corridor limit"),
  OFFROAD_UNLIMITED("offroad", "Off-Road / Track (Open)", 99, "Unrestricted private trail telemetry"),
}

enum class HudStyle(val id: String, val label: String) {
  ORB("orb", "Cyber Orb"),
  DIGITAL("digital", "Digital HUD"),
  ANALOG("analog", "Analog Dial"),
  MINIMAL("minimal", "Minimalist"),
  BAR("bar", "Bar Telemetry"),
  CYBER("cyber", "Cyber Arc"),
  AVIATION("aviation", "Aviation Horizon"),
  SUPERBIKE("superbike", "Superbike Tach"),
  F1_TELEMETRY("f1_telemetry", "F1 Telemetry"),
  FIGHTER_JET("fighter_jet", "Fighter Jet HUD"),
  COMPASS_ROSE("compass_rose", "Compass Rose"),
  DUAL_TWIN("dual_twin", "Twin Dial Pod"),
  RETRO_SYNTH("retro_synth", "Retro 80s Synth"),
  EUC_LEAN("euc_lean", "EUC / Lean HUD"),
}

enum class FontPreset(val id: String, val label: String) {
  SPACE_GROTESK("space_grotesk", "Space Grotesk"),
  ORBITRON("orbitron", "Orbitron HUD"),
  JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono"),
  RAJDHANI("rajdhani", "Rajdhani Avionics"),
  PLUS_JAKARTA("plus_jakarta", "Plus Jakarta Sans"),
  AUDIOWIDE("audiowide", "Audiowide Display"),
  SHARE_TECH("share_tech", "Share Tech Mono"),
  FIRA_CODE("fira_code", "Fira Code"),
  CHAKRA_PETCH("chakra_petch", "Chakra Petch HUD"),
  OXANIUM("oxanium", "Oxanium Cyber"),
  BEBAS_NEUE("bebas_neue", "Bebas Neue Display"),
  RUSSO_ONE("russo_one", "Russo One Track"),
  PLAYFAIR_DISPLAY("playfair_display", "Playfair Editorial"),
  INTER("inter", "Inter Clean UI"),
  VT323("vt323", "VT323 Retro CRT"),
  SILKSCREEN("silkscreen", "Silkscreen Pixel"),
}

enum class BgTexture(val id: String, val label: String, val description: String) {
  NONE("none", "Pure Void", "Immersive matte dark cockpit"),
  GRID("grid", "Cyber Grid", "High-tech neon digital matrix"),
  ASPHALT("asphalt", "Midnight Asphalt", "Road-grade textured surface"),
  GRAVEL("gravel", "Gravel Trail", "Off-road dirt and stone grit"),
  CONTOUR("contour", "Mountain Ridge", "Topographic elevation contours"),
  FOREST("forest", "Pine Canopy", "Dense woodland canopy lines"),
  FARMLAND("farmland", "Desert Dust", "Arid terrain track lines"),
  HEX("hex", "Neon Synthwave", "Retro-futuristic electric grid"),
  TOPO("topo", "Stardust Topo", "Cosmic stardust telemetry alignment"),
  COUNTY_GRID("county_grid", "County Road", "Rural section grid overlay"),
  MOUNTAIN("mountain", "Alpine Peak", "Jagged mountain wireframes"),
  TRAIL_DUST("trail_dust", "Trail Dust", "Backcountry particulate texture"),
  CIRCUIT("circuit", "Circuit Board", "Cybernetic schematic tracks"),
  CARBON("carbon", "Carbon Fiber", "Woven motorsport composite"),
  STEEL_PLATE("steel_plate", "Diamond Plate", "Industrial metal grip texture"),
  RADAR_GRID("radar_grid", "Radar Sweep", "Concentric navigation rings"),
  WAVES("waves", "Ocean Waves", "Fluid coastal ripple pattern"),
  MATRIX("matrix", "Digital Stream", "Vertical cyber data flow"),
  BLUEPRINT("blueprint", "Engineering CAD", "Precision drafting blueprint grid & callout ticks"),
  SCANLINES("scanlines", "CRT Scanlines", "Retro synthwave horizontal phosphor scanlines"),
  DIAMOND_GRID("diamond_grid", "Diamond Lattice", "Angled 45° tactical diamond mesh"),
  STARFIELD("starfield", "Night Starfield", "Clear rural night sky constellation field"),
  WOODGRAIN("woodgrain", "Timber Grain", "Warm barnwood organic plank grain lines"),
  ISOMETRIC("isometric", "Isometric Cube", "3D isometric CAD wireframe cubes"),
}

enum class RiderPaceTier(val id: String, val code: String, val label: String, val minMph: Int, val maxMph: Int) {
  ANY("ANY", "AN", "Any Pace", 0, 999),
  CHILL("CHILL", "CH", "Chill (<18 mph)", 0, 18),
  CRUISE("CRUISE", "CR", "Cruise (18–28 mph)", 18, 28),
  FAST("FAST", "FS", "Fast (28–40 mph)", 28, 40),
  HYPER("HYPER", "HY", "Hyper (40+ mph)", 40, 200),
}

enum class RiderTerrainPref(val id: String, val code: String, val label: String) {
  ANY("ANY", "AN", "Any Terrain"),
  PAVED("PAVED", "PV", "Paved Street"),
  MIXED("MIXED", "MX", "Mixed Backroads"),
  GRAVEL("GRAVEL", "GV", "Gravel & Dirt"),
  SINGLETRACK("SINGLETRACK", "ST", "Singletrack Trail"),
}

enum class RiderScheduleVibe(val id: String, val code: String, val label: String) {
  ANY("ANY", "AN", "Any Vibe"),
  WEEKEND_DAWN("WEEKEND_DAWN", "WD", "Weekend Dawn"),
  EVENING_SUNSET("EVENING_SUNSET", "ES", "Evening Sunset"),
  GROUP_CONVOY("GROUP_CONVOY", "GC", "Pack Convoy"),
  EXPLORE_WRENCH("EXPLORE_WRENCH", "EW", "Explore & Wrench"),
  RANGE_BUDDY("RANGE_BUDDY", "RB", "Range Buddy"),
}

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
  val sosTemplate: String = "EMERGENCY: Rural ERides GO pilot requires immediate assistance at GPS coordinates.",
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
  val navDefaultProfile: NavRouteProfile = NavRouteProfile.PEV_BIKE_LANE,
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
  // Telemetry Charts & HUD Precision
  val chartNeonGlowEnabled: Boolean = true,
  val chartEntranceAnimMs: Int = 900,
  val chartHoverTooltipsEnabled: Boolean = true,
  val compactNumericHud: Boolean = false,
  val odometerDecimals: Int = 1,
  val autoPauseSpeedKmh: Float = 2.0f,
  // Drivetrain, Barn Charging & Cold Weather
  val regenStrengthPreset: String = "Medium",
  val coldWeatherBatteryGuard: Boolean = true,
  val barnBreakerLimitAmps: Int = 15,
  val solarChargeTargetPct: Int = 85,
  val headlightDuskReminder: Boolean = true,
  val duskAutoHeadlightReminderMin: Int = 30,
  val chainLubeIntervalKm: Int = 250,
  // Rural Trail, Wildlife & Convoy Intelligence
  val wildlifeAlertRadiusMi: Int = 5,
  val farmEquipmentAlertEnabled: Boolean = true,
  val creekDepthWarningEnabled: Boolean = true,
  val trailSurfaceAutoDetect: Boolean = true,
  val ruralRoadSignAlerts: Boolean = true,
  val convoyBeaconIntervalSec: Int = 15,
  val gpxIncludeElevation: Boolean = true,
  // Audio Studio & Night Vision Polish
  val voiceCalloutVerbosity: String = "Balanced",
  val audioDuckingOnSpeech: Boolean = true,
  val windNoiseFilter: Boolean = true,
  val nightRedShiftIntensity: Float = 0.75f,
  // Authentic Windows Chrome, Icon Studio & Cockpit Polish
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
  // Rider Match Profile & Live Radar Compatibility Filters
  val matchPaceTier: RiderPaceTier = RiderPaceTier.CRUISE,
  val matchTerrainPref: RiderTerrainPref = RiderTerrainPref.MIXED,
  val matchMinRangeMi: Int = 20,
  val matchScheduleVibe: RiderScheduleVibe = RiderScheduleVibe.GROUP_CONVOY,
  val matchHomeZipOrTown: String = "",
  val matchMinScorePct: Int = 0,
  val matchPaceFilter: RiderPaceTier = RiderPaceTier.ANY,
  val matchTerrainFilter: RiderTerrainPref = RiderTerrainPref.ANY,
  val matchVibeFilter: RiderScheduleVibe = RiderScheduleVibe.ANY,
) {
  val hapticsEnabled: Boolean get() = hapticFeedback
  val soundVolumePct: Int get() = globalVolume
}

data class LiveTelemetrySnapshot(
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val launchLat: Double? = null,
  val launchLng: Double? = null,
  val altitudeM: Double = 0.0,
  val speedKmh: Double = 0.0,
  val maxSpeedKmh: Double = 0.0,
  val avgSpeedKmh: Double = 0.0,
  val tripDistanceKm: Double = 0.0,
  val elapsedSec: Long = 0L,
  val elevationGainM: Double = 0.0,
  val gpsAccuracyM: Float = 0f,
  val bearingDeg: Float = 0f,
  val pitchDeg: Float = 0f,
  val rollDeg: Float = 0f,
  val yawDeg: Float = 0f,
  val gyroPitchRateDps: Float = 0f,
  val gyroRollRateDps: Float = 0f,
  val gyroYawRateDps: Float = 0f,
  val magMicroTesla: Float = 0f,
  val ahrsFused9Dof: Boolean = false,
  val sensorAccuracy: Int = 0,
  val peakLeanLeftDeg: Float = 0f,
  val peakLeanRightDeg: Float = 0f,
  val lateralG: Float = 0f,
  val longitudinalG: Float = 0f,
  val peakG: Float = 0f,
  val batteryPct: Int = 100,
  val estimatedVoltage: Double = 54.6,
  val recording: Boolean = false,
  val crashDetected: Boolean = false,
  val speedLimitMph: Int? = null,
  val roadName: String = "Awaiting GPS Fix...",
  val inSchoolZone: Boolean = false,
  val trailPoints: List<Pair<Double, Double>> = emptyList(),
  val satellitesUsed: Int = 0,
  val satellitesVisible: Int = 0,
  val avgSatelliteSnrDb: Float = 0f,
  val satelliteConstellations: String = "",
  val barometerHpa: Float = 0f,
  val baroAltitudeM: Float = 0f,
  val ambientLightLux: Float = -1f,
  val proximityNear: Boolean = false,
  val roadRoughnessG: Float = 0f,
  val ambientTempC: Float? = null,
  val stepCountSession: Int = 0,
  val activeSensorsList: List<String> = emptyList(),
)

data class MaintenanceWearState(
  val itemId: String,
  val label: String,
  val ratedKm: Double,
  val lastResetKm: Double,
)

data class ServiceLogRecord(
  val id: String,
  val dateLabel: String,
  val component: String,
  val notes: String,
  val costUsd: Double,
  val odometerKm: Double,
)

data class WeatherMatrixData(
  val tempC: Double = 0.0,
  val feelsLikeC: Double = 0.0,
  val windSpeedKmh: Double = 0.0,
  val windGustKmh: Double = 0.0,
  val windDirDeg: Int = 0,
  val precipProbPct: Int = 0,
  val uvIndex: Double = 0.0,
  val pressureHpa: Double = 1013.0,
  val humidityPct: Int = 50,
  val weatherCode: Int = 0,
  val conditionLabel: String = "Syncing Open-Meteo...",
  val locationLabel: String = "Awaiting GPS Fix...",
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val isGpsSource: Boolean = true,
  val sunrise: String = "--:--",
  val sunset: String = "--:--",
  val hourly: List<HourlyForecast> = emptyList(),
  val daily: List<DailyForecast> = emptyList(),
)

data class WeatherGeoLocation(
  val name: String,
  val admin1: String,
  val country: String,
  val lat: Double,
  val lng: Double,
) {
  fun displayLabel(): String =
    listOf(name, admin1, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

data class HourlyForecast(
  val timeLabel: String,
  val tempC: Double,
  val windKmh: Double,
  val precipPct: Int,
)

data class DailyForecast(
  val dateLabel: String,
  val minTempC: Double,
  val maxTempC: Double,
  val precipPct: Int,
  val windMaxKmh: Double,
)

data class YouTubeVideoItem(
  val videoId: String,
  val title: String,
  val description: String,
  val channelTitle: String,
  val publishedAt: String,
  val thumbnailUrl: String,
  val viewCount: String = "--",
  val likeCount: String = "--",
  val durationLabel: String = "--:--",
)

data class NewsArticleItem(
  val title: String,
  val link: String,
  val source: String,
  val publishedAt: String,
  val summary: String,
)

data class PevSpecRecord(
  val name: String,
  val brand: String,
  val category: String,
  val price: String,
  val topSpeed: String,
  val range: String,
  val motorPower: String,
  val battery: String,
  val nominalVoltage: Int,
  val weight: String,
  val chargingTime: String,
  val efficiency: String,
  val brakingSystem: String,
  val suspensionType: String,
  val tireProfile: String,
  val ipRating: String,
  val maxLoad: String,
  val siteUrl: String,
  val imageUrls: List<String> = emptyList(),
)

data class PevPartItem(
  val partName: String,
  val category: String,
  val exactDimensions: String,
  val compatibility: String,
  val estimatedPrice: String,
  val recommendedBrands: String,
  val sourcePlatform: String,
  val partUrl: String,
  val difficulty: String,
  val partType: String,
  val weightLbs: Double = 1.5,
  val imageUrl: String = "",
)

data class MechanicDiagMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val imageUris: List<String> = emptyList(),
  val checklist: List<String> = emptyList(),
  val anomalies: List<VisionAnomalyBox> = emptyList(),
)

data class VisionAnomalyBox(
  val label: String,
  val severity: String,
  val xNorm: Float,
  val yNorm: Float,
  val wNorm: Float,
  val hNorm: Float,
)

data class CopilotMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val persona: String,
  val widgetTag: String? = null,
  val timestampMs: Long = System.currentTimeMillis(),
)

data class CopilotPersonaOption(
  val id: String,
  val name: String,
  val roleSummary: String,
  val systemPrompt: String,
)

data class CopilotSkillOption(
  val id: String,
  val title: String,
  val category: String,
  val promptTemplate: String,
)

data class SavedCopilotBriefing(
  val id: String,
  val title: String,
  val persona: String,
  val content: String,
  val savedAtMs: Long = System.currentTimeMillis(),
)

