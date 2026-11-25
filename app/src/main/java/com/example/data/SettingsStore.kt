package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(name = "rural_erides_master_settings")

class SettingsStore(private val context: Context) {

  private object Keys {
    val THEME_PRESET = stringPreferencesKey("theme_preset")
    val VISUAL_STYLE = stringPreferencesKey("visual_style")
    val COLOR_MODE_OVERRIDE = stringPreferencesKey("color_mode_override")
    val HUD_STYLE = stringPreferencesKey("hud_style")
    val HUD_3D_TILT = booleanPreferencesKey("hud_3d_tilt")
    val NIGHT_VISION = booleanPreferencesKey("night_vision")
    val SUN_VISIBILITY = booleanPreferencesKey("sun_visibility")
    val FONT_PRESET = stringPreferencesKey("font_preset")
    val BG_TEXTURE = stringPreferencesKey("bg_texture")
    val LOCALE = stringPreferencesKey("locale")
    val UI_SCALE = stringPreferencesKey("ui_scale")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val CORNER_STYLE = stringPreferencesKey("corner_style")
    val BORDER_WIDTH = floatPreferencesKey("border_width")
    val BORDER_GLOW = floatPreferencesKey("border_glow")
    val CARD_OPACITY = floatPreferencesKey("card_opacity")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val HAPTIC_DURATION = intPreferencesKey("haptic_duration")
    val GLOBAL_VOLUME = intPreferencesKey("global_volume")
    val SOUND_PACK = stringPreferencesKey("sound_pack")
    val AUTO_MUTE_VIDEO = booleanPreferencesKey("auto_mute_video")
    val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
    val TEMP_UNIT = stringPreferencesKey("temp_unit")
    val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
    val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
    val GPS_ACC_LIMIT = intPreferencesKey("gps_acc_limit")
    val TELEMETRY_HZ = intPreferencesKey("telemetry_hz")
    val IMU_HZ = intPreferencesKey("imu_hz")
    val PRIVACY_ZONE_M = intPreferencesKey("privacy_zone_m")
    val AUTO_START_TRACKING = booleanPreferencesKey("auto_start_tracking")
    val AUTO_WEATHER_SYNC = booleanPreferencesKey("auto_weather_sync")
    val WEATHER_MIN = intPreferencesKey("weather_min")
    val SHARE_LEADERBOARD = booleanPreferencesKey("share_leaderboard")
    val PUBLIC_PROFILE = booleanPreferencesKey("public_profile")
    val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    val CRASH_G = floatPreferencesKey("crash_g")
    val SOS_WINDOW = intPreferencesKey("sos_window")
    val SOS_TEMPLATE = stringPreferencesKey("sos_template")
    val VOICE_HOTWORD = booleanPreferencesKey("voice_hotword")
    val WAKE_LOCK = booleanPreferencesKey("wake_lock")
    val AUTO_DIMMER = booleanPreferencesKey("auto_dimmer")
    val SMART_BATTERY_AI = booleanPreferencesKey("smart_battery_ai")
    val SAG_COMP = stringPreferencesKey("sag_comp")
    val VOLT_DISP = stringPreferencesKey("volt_disp")
    val MOTOR_TEMP_WARN = intPreferencesKey("motor_temp_warn")
    val SCHOOL_ZONE = booleanPreferencesKey("school_zone")
    val LEGAL_COMPLIANCE = booleanPreferencesKey("legal_compliance")
    val PEV_LEGAL_CLASS = stringPreferencesKey("pev_legal_class")
    val LEGAL_AUDIO_ALERT = booleanPreferencesKey("legal_audio_alert")
    val HUD_BLINK_MAX = booleanPreferencesKey("hud_blink_max")
    val SPEED_OFFSET = intPreferencesKey("speed_offset")
    val RADAR_RADIUS = intPreferencesKey("radar_radius")
    val DIAL_SWEEP = intPreferencesKey("dial_sweep")
    val ELEVATION_GRADIENT = booleanPreferencesKey("elevation_gradient")
    val QUICK_REPLIES = stringPreferencesKey("quick_replies")
    val PILOT_RANK = stringPreferencesKey("pilot_rank")
    val AUTO_CLOUD_SYNC = booleanPreferencesKey("auto_cloud_sync")
    val KWH_RATE = doublePreferencesKey("kwh_rate")
    val GAS_PRICE = doublePreferencesKey("gas_price")
    val CAR_MPG = doublePreferencesKey("car_mpg")
    val COPILOT_PERSONA = stringPreferencesKey("copilot_persona")
    val AI_MODEL_ID = stringPreferencesKey("ai_model_id")
    val AI_THINKING_LEVEL = stringPreferencesKey("ai_thinking_level")
    val AI_CREATIVITY = floatPreferencesKey("ai_creativity")
    val AI_DETAIL_LEVEL = stringPreferencesKey("ai_detail_level")
    val AI_WEB_GROUNDING = booleanPreferencesKey("ai_web_grounding")
    val BATTERY_CHEMISTRY = stringPreferencesKey("battery_chemistry")
    val REGEN_ALGORITHM = stringPreferencesKey("regen_algorithm")
    val MAX_FPS = intPreferencesKey("max_fps")
    val DM_CHIME = booleanPreferencesKey("dm_chime")
    val DEFAULT_FEED_CAT = stringPreferencesKey("default_feed_cat")
    val BOARD_POST_LIMIT = intPreferencesKey("board_post_limit")
    val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")
    val DATA_RETENTION_POLICY = stringPreferencesKey("data_retention_policy")
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val PSI_REMINDER_DAYS = intPreferencesKey("psi_reminder_days")
    val MAINT_REMINDER_KM = intPreferencesKey("maint_reminder_km")
    val WEATHER_ALERTS = booleanPreferencesKey("weather_alerts")
    val AI_NOTIFS_ENABLED = booleanPreferencesKey("ai_notifs_enabled")
    val SOCIAL_NOTIFS_ENABLED = booleanPreferencesKey("social_notifs_enabled")
    val DM_NOTIFS_ENABLED = booleanPreferencesKey("dm_notifs_enabled")
    val QUIET_HOURS = booleanPreferencesKey("quiet_hours")
    val QUIET_START = intPreferencesKey("quiet_start")
    val QUIET_END = intPreferencesKey("quiet_end")
    val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    val UI_CHROME_STYLE = stringPreferencesKey("ui_chrome_style")
    val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
    val DASHBOARD_DENSITY = stringPreferencesKey("dashboard_density")
    val SHOW_CLASSIC_TITLE_BARS = booleanPreferencesKey("show_classic_title_bars")
    val RADAR_AUTO_FOLLOW = booleanPreferencesKey("radar_auto_follow")
    val RADAR_SPEED_ZOOM = booleanPreferencesKey("radar_speed_zoom")
    val CALL_RINGTONE = booleanPreferencesKey("call_ringtone_enabled")
    val AUTO_CLOUD_BACKUP = booleanPreferencesKey("auto_cloud_backup")
    val LAST_CLOUD_BACKUP_MS = longPreferencesKey("last_cloud_backup_ms")
    val LAST_CLOUD_BACKUP_PROVIDER = stringPreferencesKey("last_cloud_backup_provider")
    val YT_SPEED = floatPreferencesKey("yt_playback_speed")
    val YT_SHUFFLE = booleanPreferencesKey("yt_shuffle_enabled")
    val YT_REPEAT = stringPreferencesKey("yt_repeat_mode")
    val RETRO_AUDIO_CUES = booleanPreferencesKey("retro_audio_cues_enabled")
    val RIDE_MODE = stringPreferencesKey("ride_mode")
    val RIDER_WEIGHT_LBS = intPreferencesKey("rider_weight_lbs")
    val DAILY_DISTANCE_GOAL_KM = floatPreferencesKey("daily_distance_goal_km")
    val CUSTOM_WEATHER_CITY = stringPreferencesKey("custom_weather_city")
    val CUSTOM_WEATHER_LAT = doublePreferencesKey("custom_weather_lat")
    val CUSTOM_WEATHER_LNG = doublePreferencesKey("custom_weather_lng")
    val USE_CUSTOM_WEATHER_LOC = booleanPreferencesKey("use_custom_weather_loc")
    val CUSTOM_NEWS_CITY = stringPreferencesKey("custom_news_city")
    val NEWS_LOCATION_SCOPE = stringPreferencesKey("news_location_scope")
    val PEV_WALLPAPER = stringPreferencesKey("pev_wallpaper")
    val WALLPAPER_OPACITY = floatPreferencesKey("wallpaper_opacity")
    val HIGH_CONTRAST_ICONS = booleanPreferencesKey("high_contrast_icons")
    val CALL_RINGTONE_PRESET = stringPreferencesKey("call_ringtone_preset")
    val CUSTOM_RINGTONE_URI = stringPreferencesKey("custom_ringtone_uri")
    val DM_ALERT_TONE = stringPreferencesKey("dm_alert_tone")
    val WALKIE_ROGER_TONE = stringPreferencesKey("walkie_roger_tone")
    val NAV_TURN_TONE = stringPreferencesKey("nav_turn_tone")
    val LOW_BATT_ALARM_TONE = stringPreferencesKey("low_batt_alarm_tone")
    val RINGTONE_VOL_PCT = intPreferencesKey("ringtone_vol_pct")
    val VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")
    val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val AUTO_MUTE_RIDING = booleanPreferencesKey("auto_mute_riding")
    val NAV_DEFAULT_PROFILE = stringPreferencesKey("nav_default_profile")
    val NAV_VOICE_GUIDANCE = booleanPreferencesKey("nav_voice_guidance")
    val NAV_AUTO_REROUTE = booleanPreferencesKey("nav_auto_reroute")
    val NAV_AVOID_HIGHWAYS = booleanPreferencesKey("nav_avoid_highways")
    val NAV_SHOW_CHARGERS = booleanPreferencesKey("nav_show_chargers")
    val NAV_HEADING_UP = booleanPreferencesKey("nav_heading_up")
    val AUTO_PAUSE_RIDE = booleanPreferencesKey("auto_pause_ride")
    val LOW_BATT_WARN_PCT = intPreferencesKey("low_batt_warn_pct")
    val CRIT_VOLTAGE_V = floatPreferencesKey("crit_voltage_v")
    val REGEN_CHIME = booleanPreferencesKey("regen_chime")
    val BATT_SAVER_TELEM = booleanPreferencesKey("batt_saver_telem")
    val AUTO_ACCEPT_CONVOY = booleanPreferencesKey("auto_accept_convoy")
    val STEALTH_PRIVACY = booleanPreferencesKey("stealth_privacy")
    val CONFIRM_END_RIDE = booleanPreferencesKey("confirm_end_ride")
    val SHOW_SPEED_OVERLAY = booleanPreferencesKey("show_speed_overlay")
    val SHOW_AHRS_CARD = booleanPreferencesKey("show_ahrs_card")
    val AHRS_PEAK_LEAN = booleanPreferencesKey("ahrs_peak_lean")
    val AHRS_LEAN_ALERT_DEG = intPreferencesKey("ahrs_lean_alert_deg")
    val AHRS_GAUGE_DAMPING = stringPreferencesKey("ahrs_gauge_damping")
    val AI_CHAT_AUTO_SCROLL = booleanPreferencesKey("ai_chat_auto_scroll")
    val AI_CHAT_COMPACT_TOOLS = booleanPreferencesKey("ai_chat_compact_tools")
    val KEEP_BRIGHT_ON_RIDE = booleanPreferencesKey("keep_bright_on_ride")
    val AI_VOICE_PROFILE = stringPreferencesKey("ai_voice_profile")
    val RURAL_PERSONA_MODE = booleanPreferencesKey("rural_persona_mode")
    val AUTO_FETCH_PEV_PHOTO = booleanPreferencesKey("auto_fetch_pev_photo")
    val SHOW_RIDE_CHARTS_ON_DASH = booleanPreferencesKey("show_ride_charts_on_dash")
    val SHOW_STARTUP_ANIM = booleanPreferencesKey("show_startup_animation")
    val SHOW_SAT_STATUS_DASH = booleanPreferencesKey("show_satellite_status_dash")
    val SHOW_ENV_SENSORS_DASH = booleanPreferencesKey("show_env_sensors_dash")
    val BUTTON_HAPTIC_FEEDBACK = booleanPreferencesKey("button_haptic_feedback")
    val RIDE_MILESTONE_HAPTICS = booleanPreferencesKey("ride_milestone_haptics")
    val AUTO_NIGHT_LIGHT_SENSOR = booleanPreferencesKey("auto_night_light_sensor")
    val AR_SHOW_SATELLITE_OVERLAY = booleanPreferencesKey("ar_show_satellite_overlay")
    val SPACIOUS_LAYOUT_MODE = booleanPreferencesKey("spacious_layout_mode")
    val SPLASH_DURATION_SEC = intPreferencesKey("splash_duration_sec")
    val CONFIRM_CANCEL_NAV = booleanPreferencesKey("confirm_cancel_nav")
    val NAV_AUTO_END_ARRIVAL = booleanPreferencesKey("nav_auto_end_arrival")
    val KEEP_SCREEN_AWAKE_RIDING = booleanPreferencesKey("keep_screen_awake_riding")
    val MAP_AUTO_FOLLOW_HEADING = booleanPreferencesKey("map_auto_follow_heading")
    val SHOW_SPEED_LIMIT_BADGE = booleanPreferencesKey("show_speed_limit_badge")
    val AUTO_SPEAK_AI_REPLIES = booleanPreferencesKey("auto_speak_ai_replies")
    val MECHANIC_DEFAULT_DEPTH = stringPreferencesKey("mechanic_default_depth")
    val BATTERY_RESERVE_WARN_PCT = intPreferencesKey("battery_reserve_warn_pct")
    val COMPASS_TRUE_NORTH = booleanPreferencesKey("compass_true_north")
    val TOUCH_SOUNDS_ENABLED = booleanPreferencesKey("touch_sounds_enabled")
    val TOUCH_SOUND_PACK = stringPreferencesKey("touch_sound_pack")
    val CHART_NEON_GLOW = booleanPreferencesKey("chart_neon_glow")
    val CHART_ENTRANCE_ANIM_MS = intPreferencesKey("chart_entrance_anim_ms")
    val CHART_HOVER_TOOLTIPS = booleanPreferencesKey("chart_hover_tooltips")
    val COMPACT_NUMERIC_HUD = booleanPreferencesKey("compact_numeric_hud")
    val ODOMETER_DECIMALS = intPreferencesKey("odometer_decimals")
    val AUTO_PAUSE_SPEED_KMH = floatPreferencesKey("auto_pause_speed_kmh")
    val REGEN_STRENGTH_PRESET = stringPreferencesKey("regen_strength_preset")
    val COLD_WEATHER_BATT_GUARD = booleanPreferencesKey("cold_weather_batt_guard")
    val BARN_BREAKER_LIMIT_AMPS = intPreferencesKey("barn_breaker_limit_amps")
    val SOLAR_CHARGE_TARGET_PCT = intPreferencesKey("solar_charge_target_pct")
    val HEADLIGHT_DUSK_REMINDER = booleanPreferencesKey("headlight_dusk_reminder")
    val DUSK_REMINDER_MIN = intPreferencesKey("dusk_reminder_min")
    val CHAIN_LUBE_INTERVAL_KM = intPreferencesKey("chain_lube_interval_km")
    val WILDLIFE_ALERT_RADIUS_MI = intPreferencesKey("wildlife_alert_radius_mi")
    val FARM_EQUIPMENT_ALERT = booleanPreferencesKey("farm_equipment_alert")
    val CREEK_DEPTH_WARNING = booleanPreferencesKey("creek_depth_warning")
    val TRAIL_SURFACE_AUTO_DETECT = booleanPreferencesKey("trail_surface_auto_detect")
    val RURAL_ROAD_SIGN_ALERTS = booleanPreferencesKey("rural_road_sign_alerts")
    val CONVOY_BEACON_INTERVAL_SEC = intPreferencesKey("convoy_beacon_interval_sec")
    val GPX_INCLUDE_ELEVATION = booleanPreferencesKey("gpx_include_elevation")
    val VOICE_CALLOUT_VERBOSITY = stringPreferencesKey("voice_callout_verbosity")
    val AUDIO_DUCKING_ON_SPEECH = booleanPreferencesKey("audio_ducking_on_speech")
    val WIND_NOISE_FILTER = booleanPreferencesKey("wind_noise_filter")
    val NIGHT_RED_SHIFT_INTENSITY = floatPreferencesKey("night_red_shift_intensity")
    val WIN_SHOW_MENU_BAR = booleanPreferencesKey("win_show_menu_bar")
    val WIN_SHOW_TASKBAR_CLOCK = booleanPreferencesKey("win_show_taskbar_clock")
    val WIN_GLASS_INTENSITY = floatPreferencesKey("win_glass_intensity")
    val WIN_TASKBAR_COMPACT = booleanPreferencesKey("win_taskbar_compact")
    val ICON_STYLE_PACK = stringPreferencesKey("icon_style_pack")
    val ICON_NEON_GLOW = booleanPreferencesKey("icon_neon_glow")
    val SHOW_QUICK_ACTION_BAR = booleanPreferencesKey("show_quick_action_bar")
    val SHOW_RIDE_READINESS = booleanPreferencesKey("show_ride_readiness")
    val AUTO_NIGHT_DIM_HUD_PCT = intPreferencesKey("auto_night_dim_hud_pct")
    val BATTERY_VOLT_ALARM = booleanPreferencesKey("battery_volt_alarm")
    val HAPTIC_SCROLL_DETENTS = booleanPreferencesKey("haptic_scroll_detents")
    val QUICK_THEME_BAR = booleanPreferencesKey("quick_theme_bar")
    val MATCH_PACE_TIER = stringPreferencesKey("match_pace_tier")
    val MATCH_TERRAIN_PREF = stringPreferencesKey("match_terrain_pref")
    val MATCH_MIN_RANGE_MI = intPreferencesKey("match_min_range_mi")
    val MATCH_SCHEDULE_VIBE = stringPreferencesKey("match_schedule_vibe")
    val MATCH_HOME_ZIP = stringPreferencesKey("match_home_zip")
    val MATCH_MIN_SCORE_PCT = intPreferencesKey("match_min_score_pct")
    val MATCH_PACE_FILTER = stringPreferencesKey("match_pace_filter")
    val MATCH_TERRAIN_FILTER = stringPreferencesKey("match_terrain_filter")
    val MATCH_VIBE_FILTER = stringPreferencesKey("match_vibe_filter")
    val CUSTOM_VOICE_PROFILES_JSON = stringPreferencesKey("custom_voice_profiles_json")
    val ACTIVE_CUSTOM_VOICE_ID = stringPreferencesKey("active_custom_voice_id")
    val TTS_PRELOAD_BUFFER_ENABLED = booleanPreferencesKey("tts_preload_buffer_enabled")
    val TTS_PRELOAD_BUFFER_DEPTH = intPreferencesKey("tts_preload_buffer_depth")
    val AUTO_CHECK_APP_UPDATES = booleanPreferencesKey("auto_check_app_updates")
    val APP_UPDATE_NOTIFS_ENABLED = booleanPreferencesKey("app_update_notifs_enabled")
    val INCLUDE_PRERELEASE_BUILDS = booleanPreferencesKey("include_prerelease_builds")
    val PREFERRED_UPDATE_SOURCE = stringPreferencesKey("preferred_update_source")
    val UPDATE_CHECK_INTERVAL_HOURS = intPreferencesKey("update_check_interval_hours")
    val LAST_NOTIFIED_UPDATE_TAG = stringPreferencesKey("last_notified_update_tag")
    val SKIPPED_UPDATE_TAG = stringPreferencesKey("skipped_update_tag")
    val LAST_UPDATE_CHECK_MS = longPreferencesKey("last_update_check_ms")
  }

  val settingsFlow: Flow<MasterSettings> =
    context.settingsDataStore.data.map { prefs ->
      MasterSettings(
        themePreset =
          AppThemePreset.entries.firstOrNull { it.id == prefs[Keys.THEME_PRESET] }
            ?: AppThemePreset.LIME,
        visualStyle =
          com.example.ui.theme.sanitizeVisualStyleForSeason(
            VisualStyleMode.entries.firstOrNull { it.id == prefs[Keys.VISUAL_STYLE] }
              ?: VisualStyleMode.DARK_STUDIO
          ),
        colorModeOverride =
          ColorModeOverride.entries.firstOrNull { it.id == prefs[Keys.COLOR_MODE_OVERRIDE] }
            ?: ColorModeOverride.DARK,
        hudStyle =
          HudStyle.entries.firstOrNull { it.id == prefs[Keys.HUD_STYLE] } ?: HudStyle.ORB,
        hud3dTilt = prefs[Keys.HUD_3D_TILT] ?: true,
        nightVisionMode = prefs[Keys.NIGHT_VISION] ?: false,
        sunVisibilityMode = prefs[Keys.SUN_VISIBILITY] ?: false,
        fontPreset =
          FontPreset.entries.firstOrNull { it.id == prefs[Keys.FONT_PRESET] }
            ?: FontPreset.SPACE_GROTESK,
        bgTexture =
          BgTexture.entries.firstOrNull { it.id == prefs[Keys.BG_TEXTURE] } ?: BgTexture.GRID,
        locale = prefs[Keys.LOCALE] ?: "en",
        uiScale = prefs[Keys.UI_SCALE] ?: "normal",
        reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
        cornerStyle = prefs[Keys.CORNER_STYLE] ?: "rounded",
        borderWidthDp = prefs[Keys.BORDER_WIDTH] ?: 1.2f,
        borderGlowIntensity = prefs[Keys.BORDER_GLOW] ?: 0.65f,
        cardOpacity = prefs[Keys.CARD_OPACITY] ?: 0.86f,
        hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
        hapticDurationMs = prefs[Keys.HAPTIC_DURATION] ?: 20,
        globalVolume = prefs[Keys.GLOBAL_VOLUME] ?: 90,
        soundPack = prefs[Keys.SOUND_PACK] ?: "Tactical",
        autoMuteOnVideo = prefs[Keys.AUTO_MUTE_VIDEO] ?: true,
        distanceUnit = prefs[Keys.DISTANCE_UNIT] ?: "mi",
        tempUnit = prefs[Keys.TEMP_UNIT] ?: "f",
        pressureUnit = prefs[Keys.PRESSURE_UNIT] ?: "psi",
        decimalPrecision = prefs[Keys.DECIMAL_PRECISION] ?: 1,
        gpsAccuracyLimitM = prefs[Keys.GPS_ACC_LIMIT] ?: 25,
        telemetryRateHz = prefs[Keys.TELEMETRY_HZ] ?: 2,
        imuPollingRateHz = prefs[Keys.IMU_HZ] ?: 10,
        privacyZoneMeters = prefs[Keys.PRIVACY_ZONE_M] ?: 200,
        autoStartTracking = prefs[Keys.AUTO_START_TRACKING] ?: false,
        autoWeatherSync = prefs[Keys.AUTO_WEATHER_SYNC] ?: true,
        weatherUpdateMin = prefs[Keys.WEATHER_MIN] ?: 15,
        shareLeaderboard = prefs[Keys.SHARE_LEADERBOARD] ?: true,
        publicProfile = prefs[Keys.PUBLIC_PROFILE] ?: true,
        biometricAppLock = prefs[Keys.BIOMETRIC_LOCK] ?: false,
        crashSensitivityG =
          (prefs[Keys.CRASH_G] ?: 8.0f).let { rawG ->
            if (rawG < 6.0f) 8.0f else rawG.coerceIn(6.0f, 15.0f)
          },
        sosCancelWindowSec = prefs[Keys.SOS_WINDOW] ?: 12,
        sosTemplate =
          prefs[Keys.SOS_TEMPLATE]
            ?: "EMERGENCY: Rural ERides GO pilot requires immediate assistance at GPS coordinates.",
        voiceHotwordActive = prefs[Keys.VOICE_HOTWORD] ?: false,
        wakeLockEnabled = prefs[Keys.WAKE_LOCK] ?: true,
        autoDimmer = prefs[Keys.AUTO_DIMMER] ?: false,
        smartBatteryAi = prefs[Keys.SMART_BATTERY_AI] ?: true,
        sagCompensation = prefs[Keys.SAG_COMP] ?: "AI Dynamic",
        voltDisplayMode = prefs[Keys.VOLT_DISP] ?: "Percentage",
        motorTempWarnC = prefs[Keys.MOTOR_TEMP_WARN] ?: 85,
        schoolZoneStrict = prefs[Keys.SCHOOL_ZONE] ?: true,
        legalComplianceMode = prefs[Keys.LEGAL_COMPLIANCE] ?: BuildConfig.LEGAL_COMPLIANCE_MODE,
        pevLegalClass =
          PevLegalClass.entries.firstOrNull { it.id == prefs[Keys.PEV_LEGAL_CLASS] }
            ?: PevLegalClass.CLASS_2_20,
        legalAudioAlert = prefs[Keys.LEGAL_AUDIO_ALERT] ?: true,
        hudBlinkMax = prefs[Keys.HUD_BLINK_MAX] ?: true,
        speedOffsetTolerance = prefs[Keys.SPEED_OFFSET] ?: 5,
        radarRadiusMiles = prefs[Keys.RADAR_RADIUS] ?: 50,
        dialSweepDeg = prefs[Keys.DIAL_SWEEP] ?: 240,
        elevationGradient = prefs[Keys.ELEVATION_GRADIENT] ?: true,
        quickReplies =
          prefs[Keys.QUICK_REPLIES]
            ?: "On my way!, Need a charge, Trail blocked, Copy that!",
        pilotRankBadge = prefs[Keys.PILOT_RANK] ?: "Rookie Pilot",
        autoCloudSyncOnFinish = prefs[Keys.AUTO_CLOUD_SYNC] ?: true,
        kwhRateUsd = prefs[Keys.KWH_RATE] ?: 0.14,
        gasPriceUsd = prefs[Keys.GAS_PRICE] ?: 3.45,
        carMpg = prefs[Keys.CAR_MPG] ?: 22.0,
        copilotPersona = prefs[Keys.COPILOT_PERSONA] ?: "Tactical Coach",
        aiModelId = prefs[Keys.AI_MODEL_ID] ?: "gemini-3.5-flash-lite",
        aiThinkingLevel = prefs[Keys.AI_THINKING_LEVEL] ?: "medium",
        aiCreativity = prefs[Keys.AI_CREATIVITY] ?: 0.7f,
        aiDetailLevel = prefs[Keys.AI_DETAIL_LEVEL] ?: "standard",
        aiWebGrounding = prefs[Keys.AI_WEB_GROUNDING] ?: true,
        batteryChemistry = prefs[Keys.BATTERY_CHEMISTRY] ?: "NMC",
        regenAlgorithm = prefs[Keys.REGEN_ALGORITHM] ?: "Linear",
        maxFps = prefs[Keys.MAX_FPS] ?: 60,
        dmChime = prefs[Keys.DM_CHIME] ?: true,
        defaultFeedCategory = prefs[Keys.DEFAULT_FEED_CAT] ?: "ALL",
        boardPostLimit = prefs[Keys.BOARD_POST_LIMIT] ?: 50,
        dataSaverMode = prefs[Keys.DATA_SAVER_MODE] ?: false,
        dataRetentionPolicy = prefs[Keys.DATA_RETENTION_POLICY] ?: "30 Days",
        remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
        psiReminderDays = prefs[Keys.PSI_REMINDER_DAYS] ?: 3,
        maintReminderKm = prefs[Keys.MAINT_REMINDER_KM] ?: 250,
        weatherAlertsEnabled = prefs[Keys.WEATHER_ALERTS] ?: true,
        aiNotifsEnabled = prefs[Keys.AI_NOTIFS_ENABLED] ?: true,
        socialNotifsEnabled = prefs[Keys.SOCIAL_NOTIFS_ENABLED] ?: true,
        dmNotifsEnabled = prefs[Keys.DM_NOTIFS_ENABLED] ?: true,
        quietHoursEnabled = prefs[Keys.QUIET_HOURS] ?: false,
        quietStartHour = prefs[Keys.QUIET_START] ?: 22,
        quietEndHour = prefs[Keys.QUIET_END] ?: 7,
        debugMode = prefs[Keys.DEBUG_MODE] ?: false,
        uiChromeStyle = prefs[Keys.UI_CHROME_STYLE] ?: "tactical",
        navBarStyle = prefs[Keys.NAV_BAR_STYLE] ?: "pill",
        dashboardDensity = prefs[Keys.DASHBOARD_DENSITY] ?: "full",
        showClassicTitleBars = prefs[Keys.SHOW_CLASSIC_TITLE_BARS] ?: true,
        radarAutoFollow = prefs[Keys.RADAR_AUTO_FOLLOW] ?: true,
        radarSpeedZoom = prefs[Keys.RADAR_SPEED_ZOOM] ?: false,
        callRingtoneEnabled = prefs[Keys.CALL_RINGTONE] ?: true,
        autoCloudBackupEnabled = prefs[Keys.AUTO_CLOUD_BACKUP] ?: false,
        lastCloudBackupMs = prefs[Keys.LAST_CLOUD_BACKUP_MS] ?: 0L,
        lastCloudBackupProvider = prefs[Keys.LAST_CLOUD_BACKUP_PROVIDER] ?: "",
        ytPlaybackSpeed = prefs[Keys.YT_SPEED] ?: 1.0f,
        ytShuffleEnabled = prefs[Keys.YT_SHUFFLE] ?: false,
        ytRepeatMode = prefs[Keys.YT_REPEAT] ?: "off",
        retroAudioCuesEnabled = prefs[Keys.RETRO_AUDIO_CUES] ?: true,
        rideMode = prefs[Keys.RIDE_MODE] ?: "STREET",
        riderWeightLbs = prefs[Keys.RIDER_WEIGHT_LBS] ?: 175,
        dailyDistanceGoalKm = prefs[Keys.DAILY_DISTANCE_GOAL_KM] ?: 20f,
        customWeatherCity = prefs[Keys.CUSTOM_WEATHER_CITY] ?: "",
        customWeatherLat = prefs[Keys.CUSTOM_WEATHER_LAT] ?: 0.0,
        customWeatherLng = prefs[Keys.CUSTOM_WEATHER_LNG] ?: 0.0,
        useCustomWeatherLoc = prefs[Keys.USE_CUSTOM_WEATHER_LOC] ?: false,
        customNewsCity = prefs[Keys.CUSTOM_NEWS_CITY] ?: "",
        newsLocationScope = prefs[Keys.NEWS_LOCATION_SCOPE] ?: "GPS",
        pevWallpaper = PevWallpaper.entries.firstOrNull { it.id == prefs[Keys.PEV_WALLPAPER] } ?: PevWallpaper.AUTO_THEME,
        wallpaperOpacity = prefs[Keys.WALLPAPER_OPACITY] ?: 0.42f,
        highContrastIcons = prefs[Keys.HIGH_CONTRAST_ICONS] ?: true,
        callRingtonePreset = RingtonePreset.entries.firstOrNull { it.id == prefs[Keys.CALL_RINGTONE_PRESET] } ?: RingtonePreset.VOLT_PULSE,
        customRingtoneUri = prefs[Keys.CUSTOM_RINGTONE_URI] ?: "",
        dmAlertTone = AlertTonePreset.entries.firstOrNull { it.id == prefs[Keys.DM_ALERT_TONE] } ?: AlertTonePreset.CHIRP_TACTICAL,
        walkieRogerTone = AlertTonePreset.entries.firstOrNull { it.id == prefs[Keys.WALKIE_ROGER_TONE] } ?: AlertTonePreset.ROGER_BEEPS,
        navTurnTone = AlertTonePreset.entries.firstOrNull { it.id == prefs[Keys.NAV_TURN_TONE] } ?: AlertTonePreset.BELL_CLEAR,
        lowBatteryAlarmTone = AlertTonePreset.entries.firstOrNull { it.id == prefs[Keys.LOW_BATT_ALARM_TONE] } ?: AlertTonePreset.TWO_TONE_ALERT,
        ringtoneVolumePct = prefs[Keys.RINGTONE_VOL_PCT] ?: 85,
        vibrationPattern = VibrationPatternPreset.entries.firstOrNull { it.id == prefs[Keys.VIBRATION_PATTERN] } ?: VibrationPatternPreset.DOUBLE_TAP,
        ttsSpeechRate = prefs[Keys.TTS_SPEECH_RATE] ?: 1.03f,
        ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
        autoMuteAlertsWhileRiding = prefs[Keys.AUTO_MUTE_RIDING] ?: false,
        navDefaultProfile =
          NavRouteProfile.entries.firstOrNull { it.id == prefs[Keys.NAV_DEFAULT_PROFILE] }
            ?: NavRouteProfile.FAST_STREET,
        navVoiceGuidance = prefs[Keys.NAV_VOICE_GUIDANCE] ?: true,
        navAutoReroute = prefs[Keys.NAV_AUTO_REROUTE] ?: true,
        navAvoidHighways = prefs[Keys.NAV_AVOID_HIGHWAYS] ?: true,
        navShowChargersOnMap = prefs[Keys.NAV_SHOW_CHARGERS] ?: true,
        navHeadingUpMode = prefs[Keys.NAV_HEADING_UP] ?: true,
        autoPauseRideWhenStopped = prefs[Keys.AUTO_PAUSE_RIDE] ?: true,
        lowBatteryWarningPct = prefs[Keys.LOW_BATT_WARN_PCT] ?: 20,
        criticalVoltageAlertV = prefs[Keys.CRIT_VOLTAGE_V] ?: 44.0f,
        regenBrakingChime = prefs[Keys.REGEN_CHIME] ?: false,
        batterySaverTelemetry = prefs[Keys.BATT_SAVER_TELEM] ?: false,
        autoAcceptConvoyInvites = prefs[Keys.AUTO_ACCEPT_CONVOY] ?: false,
        stealthPrivacyMode = prefs[Keys.STEALTH_PRIVACY] ?: false,
        confirmBeforeEndRide = prefs[Keys.CONFIRM_END_RIDE] ?: true,
        showSpeedLimitOverlay = prefs[Keys.SHOW_SPEED_OVERLAY] ?: true,
        showAhrsTelemetryCard = prefs[Keys.SHOW_AHRS_CARD] ?: true,
        ahrsPeakLeanHold = prefs[Keys.AHRS_PEAK_LEAN] ?: true,
        ahrsLeanAlertDeg = prefs[Keys.AHRS_LEAN_ALERT_DEG] ?: 42,
        ahrsGaugeDamping = prefs[Keys.AHRS_GAUGE_DAMPING] ?: "Balanced",
        aiChatAutoScroll = prefs[Keys.AI_CHAT_AUTO_SCROLL] ?: true,
        aiChatCompactTools = prefs[Keys.AI_CHAT_COMPACT_TOOLS] ?: true,
        keepScreenBrightOnRide = prefs[Keys.KEEP_BRIGHT_ON_RIDE] ?: true,
        aiVoiceProfile =
          AiVoiceProfile.entries.firstOrNull { it.id == prefs[Keys.AI_VOICE_PROFILE] }
            ?: AiVoiceProfile.WARM_RURAL_PORCH,
        ruralPersonaMode = prefs[Keys.RURAL_PERSONA_MODE] ?: true,
        autoFetchPevPhoto = prefs[Keys.AUTO_FETCH_PEV_PHOTO] ?: true,
        showRideChartsOnDash = prefs[Keys.SHOW_RIDE_CHARTS_ON_DASH] ?: true,
        showStartupAnimation = prefs[Keys.SHOW_STARTUP_ANIM] ?: true,
        showSatelliteStatusOnDash = prefs[Keys.SHOW_SAT_STATUS_DASH] ?: true,
        showEnvironmentSensorsOnDash = prefs[Keys.SHOW_ENV_SENSORS_DASH] ?: true,
        buttonHapticFeedback = prefs[Keys.BUTTON_HAPTIC_FEEDBACK] ?: true,
        rideMilestoneHaptics = prefs[Keys.RIDE_MILESTONE_HAPTICS] ?: true,
        autoNightFromLightSensor = prefs[Keys.AUTO_NIGHT_LIGHT_SENSOR] ?: false,
        arShowSatelliteOverlay = prefs[Keys.AR_SHOW_SATELLITE_OVERLAY] ?: true,
        spaciousLayoutMode = prefs[Keys.SPACIOUS_LAYOUT_MODE] ?: true,
        splashDurationSec = (prefs[Keys.SPLASH_DURATION_SEC] ?: 7).coerceIn(4, 12),
        confirmCancelNavigation = prefs[Keys.CONFIRM_CANCEL_NAV] ?: false,
        navAutoEndOnArrival = prefs[Keys.NAV_AUTO_END_ARRIVAL] ?: true,
        keepScreenAwakeRiding = prefs[Keys.KEEP_SCREEN_AWAKE_RIDING] ?: true,
        mapAutoFollowHeading = prefs[Keys.MAP_AUTO_FOLLOW_HEADING] ?: true,
        showSpeedLimitBadge = prefs[Keys.SHOW_SPEED_LIMIT_BADGE] ?: true,
        autoSpeakAiReplies = prefs[Keys.AUTO_SPEAK_AI_REPLIES] ?: false,
        mechanicDefaultDepth = prefs[Keys.MECHANIC_DEFAULT_DEPTH] ?: "STANDARD",
        batteryReserveWarningPct = (prefs[Keys.BATTERY_RESERVE_WARN_PCT] ?: 20).coerceIn(5, 50),
        compassTrueNorth = prefs[Keys.COMPASS_TRUE_NORTH] ?: true,
        touchSoundsEnabled = prefs[Keys.TOUCH_SOUNDS_ENABLED] ?: true,
        touchSoundPack = prefs[Keys.TOUCH_SOUND_PACK] ?: "Country Acoustic",
        chartNeonGlowEnabled = prefs[Keys.CHART_NEON_GLOW] ?: true,
        chartEntranceAnimMs = (prefs[Keys.CHART_ENTRANCE_ANIM_MS] ?: 900).coerceIn(250, 1800),
        chartHoverTooltipsEnabled = prefs[Keys.CHART_HOVER_TOOLTIPS] ?: true,
        compactNumericHud = prefs[Keys.COMPACT_NUMERIC_HUD] ?: false,
        odometerDecimals = (prefs[Keys.ODOMETER_DECIMALS] ?: 1).coerceIn(0, 2),
        autoPauseSpeedKmh = (prefs[Keys.AUTO_PAUSE_SPEED_KMH] ?: 2.0f).coerceIn(0.5f, 8.0f),
        regenStrengthPreset = prefs[Keys.REGEN_STRENGTH_PRESET] ?: "Medium",
        coldWeatherBatteryGuard = prefs[Keys.COLD_WEATHER_BATT_GUARD] ?: true,
        barnBreakerLimitAmps = (prefs[Keys.BARN_BREAKER_LIMIT_AMPS] ?: 15).coerceIn(10, 50),
        solarChargeTargetPct = (prefs[Keys.SOLAR_CHARGE_TARGET_PCT] ?: 85).coerceIn(60, 100),
        headlightDuskReminder = prefs[Keys.HEADLIGHT_DUSK_REMINDER] ?: true,
        duskAutoHeadlightReminderMin = (prefs[Keys.DUSK_REMINDER_MIN] ?: 30).coerceIn(10, 90),
        chainLubeIntervalKm = (prefs[Keys.CHAIN_LUBE_INTERVAL_KM] ?: 250).coerceIn(50, 1000),
        wildlifeAlertRadiusMi = (prefs[Keys.WILDLIFE_ALERT_RADIUS_MI] ?: 5).coerceIn(1, 25),
        farmEquipmentAlertEnabled = prefs[Keys.FARM_EQUIPMENT_ALERT] ?: true,
        creekDepthWarningEnabled = prefs[Keys.CREEK_DEPTH_WARNING] ?: true,
        trailSurfaceAutoDetect = prefs[Keys.TRAIL_SURFACE_AUTO_DETECT] ?: true,
        ruralRoadSignAlerts = prefs[Keys.RURAL_ROAD_SIGN_ALERTS] ?: true,
        convoyBeaconIntervalSec = (prefs[Keys.CONVOY_BEACON_INTERVAL_SEC] ?: 15).coerceIn(5, 60),
        gpxIncludeElevation = prefs[Keys.GPX_INCLUDE_ELEVATION] ?: true,
        voiceCalloutVerbosity = prefs[Keys.VOICE_CALLOUT_VERBOSITY] ?: "Balanced",
        audioDuckingOnSpeech = prefs[Keys.AUDIO_DUCKING_ON_SPEECH] ?: true,
        windNoiseFilter = prefs[Keys.WIND_NOISE_FILTER] ?: true,
        nightRedShiftIntensity = (prefs[Keys.NIGHT_RED_SHIFT_INTENSITY] ?: 0.75f).coerceIn(0.25f, 1.0f),
        winShowMenuBar = prefs[Keys.WIN_SHOW_MENU_BAR] ?: true,
        winShowTaskbarClock = prefs[Keys.WIN_SHOW_TASKBAR_CLOCK] ?: true,
        winGlassIntensity = (prefs[Keys.WIN_GLASS_INTENSITY] ?: 0.85f).coerceIn(0.45f, 1.0f),
        winTaskbarCompact = prefs[Keys.WIN_TASKBAR_COMPACT] ?: false,
        iconStylePack = prefs[Keys.ICON_STYLE_PACK] ?: "Tactical Neon",
        iconNeonGlowEnabled = prefs[Keys.ICON_NEON_GLOW] ?: true,
        showQuickActionBarOnDash = prefs[Keys.SHOW_QUICK_ACTION_BAR] ?: true,
        showRideReadinessScore = prefs[Keys.SHOW_RIDE_READINESS] ?: true,
        autoNightDimHudPct = (prefs[Keys.AUTO_NIGHT_DIM_HUD_PCT] ?: 70).coerceIn(25, 100),
        batteryVoltageAlarmEnabled = prefs[Keys.BATTERY_VOLT_ALARM] ?: true,
        hapticScrollDetents = prefs[Keys.HAPTIC_SCROLL_DETENTS] ?: true,
        quickThemeBarEnabled = prefs[Keys.QUICK_THEME_BAR] ?: true,
        matchPaceTier =
          RiderPaceTier.entries.firstOrNull { it.id == prefs[Keys.MATCH_PACE_TIER] }
            ?: RiderPaceTier.CRUISE,
        matchTerrainPref =
          RiderTerrainPref.entries.firstOrNull { it.id == prefs[Keys.MATCH_TERRAIN_PREF] }
            ?: RiderTerrainPref.MIXED,
        matchMinRangeMi = (prefs[Keys.MATCH_MIN_RANGE_MI] ?: 20).coerceIn(5, 150),
        matchScheduleVibe =
          RiderScheduleVibe.entries.firstOrNull { it.id == prefs[Keys.MATCH_SCHEDULE_VIBE] }
            ?: RiderScheduleVibe.GROUP_CONVOY,
        matchHomeZipOrTown = prefs[Keys.MATCH_HOME_ZIP] ?: "",
        matchMinScorePct = (prefs[Keys.MATCH_MIN_SCORE_PCT] ?: 0).coerceIn(0, 95),
        matchPaceFilter =
          RiderPaceTier.entries.firstOrNull { it.id == prefs[Keys.MATCH_PACE_FILTER] }
            ?: RiderPaceTier.ANY,
        matchTerrainFilter =
          RiderTerrainPref.entries.firstOrNull { it.id == prefs[Keys.MATCH_TERRAIN_FILTER] }
            ?: RiderTerrainPref.ANY,
        matchVibeFilter =
          RiderScheduleVibe.entries.firstOrNull { it.id == prefs[Keys.MATCH_VIBE_FILTER] }
            ?: RiderScheduleVibe.ANY,
        customVoiceProfilesJson = prefs[Keys.CUSTOM_VOICE_PROFILES_JSON] ?: "[]",
        activeCustomVoiceId = prefs[Keys.ACTIVE_CUSTOM_VOICE_ID] ?: "",
        ttsPreloadBufferEnabled = prefs[Keys.TTS_PRELOAD_BUFFER_ENABLED] ?: true,
        ttsPreloadBufferDepth = (prefs[Keys.TTS_PRELOAD_BUFFER_DEPTH] ?: 2).coerceIn(1, 5),
        autoCheckAppUpdates = prefs[Keys.AUTO_CHECK_APP_UPDATES] ?: true,
        appUpdateNotifsEnabled = prefs[Keys.APP_UPDATE_NOTIFS_ENABLED] ?: true,
        includePrereleaseBuilds = prefs[Keys.INCLUDE_PRERELEASE_BUILDS] ?: false,
        preferredUpdateSource = prefs[Keys.PREFERRED_UPDATE_SOURCE] ?: "GITHUB_RELEASES",
        updateCheckIntervalHours = (prefs[Keys.UPDATE_CHECK_INTERVAL_HOURS] ?: 12).coerceIn(6, 48),
        lastNotifiedUpdateTag = prefs[Keys.LAST_NOTIFIED_UPDATE_TAG] ?: "",
        skippedUpdateTag = prefs[Keys.SKIPPED_UPDATE_TAG] ?: "",
        lastUpdateCheckMs = prefs[Keys.LAST_UPDATE_CHECK_MS] ?: 0L,
      )
    }

  suspend fun update(transform: (MasterSettings) -> MasterSettings) {
    val current = settingsFlow.first()
    val updated = transform(current)
    writeAll(updated)
  }

  suspend fun resetAll() {
    context.settingsDataStore.edit { it.clear() }
  }

  suspend fun exportAsJson(): String {
    val s = settingsFlow.first()
    return JSONObject()
      .apply {
        put("themePreset", s.themePreset.id)
        put("visualStyle", s.visualStyle.id)
        put("colorModeOverride", s.colorModeOverride.id)
        put("hudStyle", s.hudStyle.id)
        put("hud3dTilt", s.hud3dTilt)
        put("nightVisionMode", s.nightVisionMode)
        put("sunVisibilityMode", s.sunVisibilityMode)
        put("fontPreset", s.fontPreset.id)
        put("bgTexture", s.bgTexture.id)
        put("locale", s.locale)
        put("uiScale", s.uiScale)
        put("reducedMotion", s.reducedMotion)
        put("cornerStyle", s.cornerStyle)
        put("borderWidthDp", s.borderWidthDp.toDouble())
        put("borderGlowIntensity", s.borderGlowIntensity.toDouble())
        put("cardOpacity", s.cardOpacity.toDouble())
        put("hapticFeedback", s.hapticFeedback)
        put("hapticDurationMs", s.hapticDurationMs)
        put("globalVolume", s.globalVolume)
        put("soundPack", s.soundPack)
        put("autoMuteOnVideo", s.autoMuteOnVideo)
        put("distanceUnit", s.distanceUnit)
        put("tempUnit", s.tempUnit)
        put("pressureUnit", s.pressureUnit)
        put("decimalPrecision", s.decimalPrecision)
        put("gpsAccuracyLimitM", s.gpsAccuracyLimitM)
        put("telemetryRateHz", s.telemetryRateHz)
        put("imuPollingRateHz", s.imuPollingRateHz)
        put("privacyZoneMeters", s.privacyZoneMeters)
        put("autoStartTracking", s.autoStartTracking)
        put("autoWeatherSync", s.autoWeatherSync)
        put("weatherUpdateMin", s.weatherUpdateMin)
        put("shareLeaderboard", s.shareLeaderboard)
        put("publicProfile", s.publicProfile)
        put("biometricAppLock", s.biometricAppLock)
        put("crashSensitivityG", s.crashSensitivityG.toDouble())
        put("sosCancelWindowSec", s.sosCancelWindowSec)
        put("sosTemplate", s.sosTemplate)
        put("voiceHotwordActive", s.voiceHotwordActive)
        put("wakeLockEnabled", s.wakeLockEnabled)
        put("autoDimmer", s.autoDimmer)
        put("smartBatteryAi", s.smartBatteryAi)
        put("sagCompensation", s.sagCompensation)
        put("voltDisplayMode", s.voltDisplayMode)
        put("motorTempWarnC", s.motorTempWarnC)
        put("schoolZoneStrict", s.schoolZoneStrict)
        put("legalComplianceMode", s.legalComplianceMode)
        put("pevLegalClass", s.pevLegalClass.id)
        put("legalAudioAlert", s.legalAudioAlert)
        put("hudBlinkMax", s.hudBlinkMax)
        put("speedOffsetTolerance", s.speedOffsetTolerance)
        put("radarRadiusMiles", s.radarRadiusMiles)
        put("dialSweepDeg", s.dialSweepDeg)
        put("elevationGradient", s.elevationGradient)
        put("quickReplies", s.quickReplies)
        put("pilotRankBadge", s.pilotRankBadge)
        put("autoCloudSyncOnFinish", s.autoCloudSyncOnFinish)
        put("kwhRateUsd", s.kwhRateUsd)
        put("gasPriceUsd", s.gasPriceUsd)
        put("carMpg", s.carMpg)
        put("copilotPersona", s.copilotPersona)
        put("aiModelId", s.aiModelId)
        put("aiThinkingLevel", s.aiThinkingLevel)
        put("aiCreativity", s.aiCreativity.toDouble())
        put("aiDetailLevel", s.aiDetailLevel)
        put("aiWebGrounding", s.aiWebGrounding)
        put("batteryChemistry", s.batteryChemistry)
        put("regenAlgorithm", s.regenAlgorithm)
        put("maxFps", s.maxFps)
        put("dmChime", s.dmChime)
        put("defaultFeedCategory", s.defaultFeedCategory)
        put("boardPostLimit", s.boardPostLimit)
        put("dataSaverMode", s.dataSaverMode)
        put("dataRetentionPolicy", s.dataRetentionPolicy)
        put("remindersEnabled", s.remindersEnabled)
        put("psiReminderDays", s.psiReminderDays)
        put("maintReminderKm", s.maintReminderKm)
        put("weatherAlertsEnabled", s.weatherAlertsEnabled)
        put("aiNotifsEnabled", s.aiNotifsEnabled)
        put("socialNotifsEnabled", s.socialNotifsEnabled)
        put("dmNotifsEnabled", s.dmNotifsEnabled)
        put("quietHoursEnabled", s.quietHoursEnabled)
        put("quietStartHour", s.quietStartHour)
        put("quietEndHour", s.quietEndHour)
        put("debugMode", s.debugMode)
        put("uiChromeStyle", s.uiChromeStyle)
        put("navBarStyle", s.navBarStyle)
        put("dashboardDensity", s.dashboardDensity)
        put("showClassicTitleBars", s.showClassicTitleBars)
        put("radarAutoFollow", s.radarAutoFollow)
        put("radarSpeedZoom", s.radarSpeedZoom)
        put("callRingtoneEnabled", s.callRingtoneEnabled)
        put("autoCloudBackupEnabled", s.autoCloudBackupEnabled)
        put("lastCloudBackupMs", s.lastCloudBackupMs)
        put("lastCloudBackupProvider", s.lastCloudBackupProvider)
        put("ytPlaybackSpeed", s.ytPlaybackSpeed.toDouble())
        put("ytShuffleEnabled", s.ytShuffleEnabled)
        put("ytRepeatMode", s.ytRepeatMode)
        put("retroAudioCuesEnabled", s.retroAudioCuesEnabled)
        put("rideMode", s.rideMode)
        put("riderWeightLbs", s.riderWeightLbs)
        put("dailyDistanceGoalKm", s.dailyDistanceGoalKm.toDouble())
        put("customWeatherCity", s.customWeatherCity)
        put("customWeatherLat", s.customWeatherLat)
        put("customWeatherLng", s.customWeatherLng)
        put("useCustomWeatherLoc", s.useCustomWeatherLoc)
        put("customNewsCity", s.customNewsCity)
        put("newsLocationScope", s.newsLocationScope)
        put("pevWallpaper", s.pevWallpaper.id)
        put("wallpaperOpacity", s.wallpaperOpacity.toDouble())
        put("highContrastIcons", s.highContrastIcons)
        put("callRingtonePreset", s.callRingtonePreset.id)
        put("customRingtoneUri", s.customRingtoneUri)
        put("dmAlertTone", s.dmAlertTone.id)
        put("walkieRogerTone", s.walkieRogerTone.id)
        put("navTurnTone", s.navTurnTone.id)
        put("lowBatteryAlarmTone", s.lowBatteryAlarmTone.id)
        put("ringtoneVolumePct", s.ringtoneVolumePct)
        put("vibrationPattern", s.vibrationPattern.id)
        put("ttsSpeechRate", s.ttsSpeechRate.toDouble())
        put("ttsPitch", s.ttsPitch.toDouble())
        put("autoMuteAlertsWhileRiding", s.autoMuteAlertsWhileRiding)
        put("navDefaultProfile", s.navDefaultProfile.id)
        put("navVoiceGuidance", s.navVoiceGuidance)
        put("navAutoReroute", s.navAutoReroute)
        put("navAvoidHighways", s.navAvoidHighways)
        put("navShowChargersOnMap", s.navShowChargersOnMap)
        put("navHeadingUpMode", s.navHeadingUpMode)
        put("autoPauseRideWhenStopped", s.autoPauseRideWhenStopped)
        put("lowBatteryWarningPct", s.lowBatteryWarningPct)
        put("criticalVoltageAlertV", s.criticalVoltageAlertV.toDouble())
        put("regenBrakingChime", s.regenBrakingChime)
        put("batterySaverTelemetry", s.batterySaverTelemetry)
        put("autoAcceptConvoyInvites", s.autoAcceptConvoyInvites)
        put("stealthPrivacyMode", s.stealthPrivacyMode)
        put("confirmBeforeEndRide", s.confirmBeforeEndRide)
        put("showSpeedLimitOverlay", s.showSpeedLimitOverlay)
        put("showAhrsTelemetryCard", s.showAhrsTelemetryCard)
        put("ahrsPeakLeanHold", s.ahrsPeakLeanHold)
        put("ahrsLeanAlertDeg", s.ahrsLeanAlertDeg)
        put("ahrsGaugeDamping", s.ahrsGaugeDamping)
        put("aiChatAutoScroll", s.aiChatAutoScroll)
        put("aiChatCompactTools", s.aiChatCompactTools)
        put("keepScreenBrightOnRide", s.keepScreenBrightOnRide)
        put("aiVoiceProfile", s.aiVoiceProfile.id)
        put("ruralPersonaMode", s.ruralPersonaMode)
        put("autoFetchPevPhoto", s.autoFetchPevPhoto)
        put("showRideChartsOnDash", s.showRideChartsOnDash)
        put("showStartupAnimation", s.showStartupAnimation)
        put("showSatelliteStatusOnDash", s.showSatelliteStatusOnDash)
        put("showEnvironmentSensorsOnDash", s.showEnvironmentSensorsOnDash)
        put("buttonHapticFeedback", s.buttonHapticFeedback)
        put("rideMilestoneHaptics", s.rideMilestoneHaptics)
        put("autoNightFromLightSensor", s.autoNightFromLightSensor)
        put("arShowSatelliteOverlay", s.arShowSatelliteOverlay)
        put("spaciousLayoutMode", s.spaciousLayoutMode)
        put("splashDurationSec", s.splashDurationSec)
        put("confirmCancelNavigation", s.confirmCancelNavigation)
        put("navAutoEndOnArrival", s.navAutoEndOnArrival)
        put("keepScreenAwakeRiding", s.keepScreenAwakeRiding)
        put("mapAutoFollowHeading", s.mapAutoFollowHeading)
        put("showSpeedLimitBadge", s.showSpeedLimitBadge)
        put("autoSpeakAiReplies", s.autoSpeakAiReplies)
        put("mechanicDefaultDepth", s.mechanicDefaultDepth)
        put("batteryReserveWarningPct", s.batteryReserveWarningPct)
        put("compassTrueNorth", s.compassTrueNorth)
        put("touchSoundsEnabled", s.touchSoundsEnabled)
        put("touchSoundPack", s.touchSoundPack)
        put("chartNeonGlowEnabled", s.chartNeonGlowEnabled)
        put("chartEntranceAnimMs", s.chartEntranceAnimMs)
        put("chartHoverTooltipsEnabled", s.chartHoverTooltipsEnabled)
        put("compactNumericHud", s.compactNumericHud)
        put("odometerDecimals", s.odometerDecimals)
        put("autoPauseSpeedKmh", s.autoPauseSpeedKmh.toDouble())
        put("regenStrengthPreset", s.regenStrengthPreset)
        put("coldWeatherBatteryGuard", s.coldWeatherBatteryGuard)
        put("barnBreakerLimitAmps", s.barnBreakerLimitAmps)
        put("solarChargeTargetPct", s.solarChargeTargetPct)
        put("headlightDuskReminder", s.headlightDuskReminder)
        put("duskAutoHeadlightReminderMin", s.duskAutoHeadlightReminderMin)
        put("chainLubeIntervalKm", s.chainLubeIntervalKm)
        put("wildlifeAlertRadiusMi", s.wildlifeAlertRadiusMi)
        put("farmEquipmentAlertEnabled", s.farmEquipmentAlertEnabled)
        put("creekDepthWarningEnabled", s.creekDepthWarningEnabled)
        put("trailSurfaceAutoDetect", s.trailSurfaceAutoDetect)
        put("ruralRoadSignAlerts", s.ruralRoadSignAlerts)
        put("convoyBeaconIntervalSec", s.convoyBeaconIntervalSec)
        put("gpxIncludeElevation", s.gpxIncludeElevation)
        put("voiceCalloutVerbosity", s.voiceCalloutVerbosity)
        put("audioDuckingOnSpeech", s.audioDuckingOnSpeech)
        put("windNoiseFilter", s.windNoiseFilter)
        put("nightRedShiftIntensity", s.nightRedShiftIntensity.toDouble())
        put("winShowMenuBar", s.winShowMenuBar)
        put("winShowTaskbarClock", s.winShowTaskbarClock)
        put("winGlassIntensity", s.winGlassIntensity.toDouble())
        put("winTaskbarCompact", s.winTaskbarCompact)
        put("iconStylePack", s.iconStylePack)
        put("iconNeonGlowEnabled", s.iconNeonGlowEnabled)
        put("showQuickActionBarOnDash", s.showQuickActionBarOnDash)
        put("showRideReadinessScore", s.showRideReadinessScore)
        put("autoNightDimHudPct", s.autoNightDimHudPct)
        put("batteryVoltageAlarmEnabled", s.batteryVoltageAlarmEnabled)
        put("hapticScrollDetents", s.hapticScrollDetents)
        put("quickThemeBarEnabled", s.quickThemeBarEnabled)
        put("matchPaceTier", s.matchPaceTier.id)
        put("matchTerrainPref", s.matchTerrainPref.id)
        put("matchMinRangeMi", s.matchMinRangeMi)
        put("matchScheduleVibe", s.matchScheduleVibe.id)
        put("matchHomeZipOrTown", s.matchHomeZipOrTown)
        put("matchMinScorePct", s.matchMinScorePct)
        put("matchPaceFilter", s.matchPaceFilter.id)
        put("matchTerrainFilter", s.matchTerrainFilter.id)
        put("matchVibeFilter", s.matchVibeFilter.id)
        put("customVoiceProfilesJson", s.customVoiceProfilesJson)
        put("activeCustomVoiceId", s.activeCustomVoiceId)
        put("ttsPreloadBufferEnabled", s.ttsPreloadBufferEnabled)
        put("ttsPreloadBufferDepth", s.ttsPreloadBufferDepth)
        put("autoCheckAppUpdates", s.autoCheckAppUpdates)
        put("appUpdateNotifsEnabled", s.appUpdateNotifsEnabled)
        put("includePrereleaseBuilds", s.includePrereleaseBuilds)
        put("preferredUpdateSource", s.preferredUpdateSource)
        put("updateCheckIntervalHours", s.updateCheckIntervalHours)
      }
      .toString(2)
  }

  suspend fun importFromJson(rawJson: String): Result<Unit> =
    runCatching {
      val root = JSONObject(rawJson)
      val obj = root.optJSONObject("settings") ?: root
      val current = settingsFlow.first()
      val restored =
        current.copy(
          themePreset =
            AppThemePreset.entries.firstOrNull { it.id == obj.optString("themePreset") }
              ?: current.themePreset,
          visualStyle =
            VisualStyleMode.entries.firstOrNull { it.id == obj.optString("visualStyle") }
              ?: current.visualStyle,
          colorModeOverride =
            ColorModeOverride.entries.firstOrNull { it.id == obj.optString("colorModeOverride") }
              ?: current.colorModeOverride,
          hudStyle =
            HudStyle.entries.firstOrNull { it.id == obj.optString("hudStyle") }
              ?: current.hudStyle,
          hud3dTilt = obj.optBoolean("hud3dTilt", current.hud3dTilt),
          nightVisionMode = obj.optBoolean("nightVisionMode", current.nightVisionMode),
          sunVisibilityMode = obj.optBoolean("sunVisibilityMode", current.sunVisibilityMode),
          fontPreset =
            FontPreset.entries.firstOrNull { it.id == obj.optString("fontPreset") }
              ?: current.fontPreset,
          bgTexture =
            BgTexture.entries.firstOrNull { it.id == obj.optString("bgTexture") }
              ?: current.bgTexture,
          locale = obj.optString("locale", current.locale),
          uiScale = obj.optString("uiScale", current.uiScale),
          reducedMotion = obj.optBoolean("reducedMotion", current.reducedMotion),
          cornerStyle = obj.optString("cornerStyle", current.cornerStyle),
          borderWidthDp = obj.optDouble("borderWidthDp", current.borderWidthDp.toDouble()).toFloat(),
          borderGlowIntensity =
            obj.optDouble("borderGlowIntensity", current.borderGlowIntensity.toDouble()).toFloat(),
          cardOpacity = obj.optDouble("cardOpacity", current.cardOpacity.toDouble()).toFloat(),
          hapticFeedback = obj.optBoolean("hapticFeedback", current.hapticFeedback),
          hapticDurationMs = obj.optInt("hapticDurationMs", current.hapticDurationMs),
          globalVolume = obj.optInt("globalVolume", current.globalVolume),
          soundPack = obj.optString("soundPack", current.soundPack),
          autoMuteOnVideo = obj.optBoolean("autoMuteOnVideo", current.autoMuteOnVideo),
          distanceUnit = obj.optString("distanceUnit", current.distanceUnit),
          tempUnit = obj.optString("tempUnit", current.tempUnit),
          pressureUnit = obj.optString("pressureUnit", current.pressureUnit),
          decimalPrecision = obj.optInt("decimalPrecision", current.decimalPrecision),
          gpsAccuracyLimitM = obj.optInt("gpsAccuracyLimitM", current.gpsAccuracyLimitM),
          telemetryRateHz = obj.optInt("telemetryRateHz", current.telemetryRateHz),
          imuPollingRateHz = obj.optInt("imuPollingRateHz", current.imuPollingRateHz),
          privacyZoneMeters = obj.optInt("privacyZoneMeters", current.privacyZoneMeters),
          autoStartTracking = obj.optBoolean("autoStartTracking", current.autoStartTracking),
          autoWeatherSync = obj.optBoolean("autoWeatherSync", current.autoWeatherSync),
          weatherUpdateMin = obj.optInt("weatherUpdateMin", current.weatherUpdateMin),
          shareLeaderboard = obj.optBoolean("shareLeaderboard", current.shareLeaderboard),
          publicProfile = obj.optBoolean("publicProfile", current.publicProfile),
          biometricAppLock = obj.optBoolean("biometricAppLock", current.biometricAppLock),
          crashSensitivityG =
            obj.optDouble("crashSensitivityG", current.crashSensitivityG.toDouble()).toFloat().let { rawG ->
              if (rawG < 6.0f) 8.0f else rawG.coerceIn(6.0f, 15.0f)
            },
          sosCancelWindowSec = obj.optInt("sosCancelWindowSec", current.sosCancelWindowSec),
          sosTemplate = obj.optString("sosTemplate", current.sosTemplate),
          voiceHotwordActive = obj.optBoolean("voiceHotwordActive", current.voiceHotwordActive),
          wakeLockEnabled = obj.optBoolean("wakeLockEnabled", current.wakeLockEnabled),
          autoDimmer = obj.optBoolean("autoDimmer", current.autoDimmer),
          smartBatteryAi = obj.optBoolean("smartBatteryAi", current.smartBatteryAi),
          sagCompensation = obj.optString("sagCompensation", current.sagCompensation),
          voltDisplayMode = obj.optString("voltDisplayMode", current.voltDisplayMode),
          motorTempWarnC = obj.optInt("motorTempWarnC", current.motorTempWarnC),
          schoolZoneStrict = obj.optBoolean("schoolZoneStrict", current.schoolZoneStrict),
          legalComplianceMode = obj.optBoolean("legalComplianceMode", current.legalComplianceMode),
          pevLegalClass =
            PevLegalClass.entries.firstOrNull { it.id == obj.optString("pevLegalClass") }
              ?: current.pevLegalClass,
          legalAudioAlert = obj.optBoolean("legalAudioAlert", current.legalAudioAlert),
          hudBlinkMax = obj.optBoolean("hudBlinkMax", current.hudBlinkMax),
          speedOffsetTolerance = obj.optInt("speedOffsetTolerance", current.speedOffsetTolerance),
          radarRadiusMiles = obj.optInt("radarRadiusMiles", current.radarRadiusMiles),
          dialSweepDeg = obj.optInt("dialSweepDeg", current.dialSweepDeg),
          elevationGradient = obj.optBoolean("elevationGradient", current.elevationGradient),
          quickReplies = obj.optString("quickReplies", current.quickReplies),
          pilotRankBadge = obj.optString("pilotRankBadge", current.pilotRankBadge),
          autoCloudSyncOnFinish =
            obj.optBoolean("autoCloudSyncOnFinish", current.autoCloudSyncOnFinish),
          kwhRateUsd = obj.optDouble("kwhRateUsd", current.kwhRateUsd),
          gasPriceUsd = obj.optDouble("gasPriceUsd", current.gasPriceUsd),
          carMpg = obj.optDouble("carMpg", current.carMpg),
          copilotPersona = obj.optString("copilotPersona", current.copilotPersona),
          aiModelId = obj.optString("aiModelId", current.aiModelId),
          aiThinkingLevel = obj.optString("aiThinkingLevel", current.aiThinkingLevel),
          aiCreativity = obj.optDouble("aiCreativity", current.aiCreativity.toDouble()).toFloat(),
          aiDetailLevel = obj.optString("aiDetailLevel", current.aiDetailLevel),
          aiWebGrounding = obj.optBoolean("aiWebGrounding", current.aiWebGrounding),
          batteryChemistry = obj.optString("batteryChemistry", current.batteryChemistry),
          regenAlgorithm = obj.optString("regenAlgorithm", current.regenAlgorithm),
          maxFps = obj.optInt("maxFps", current.maxFps),
          dmChime = obj.optBoolean("dmChime", current.dmChime),
          defaultFeedCategory = obj.optString("defaultFeedCategory", current.defaultFeedCategory),
          boardPostLimit = obj.optInt("boardPostLimit", current.boardPostLimit),
          dataSaverMode = obj.optBoolean("dataSaverMode", current.dataSaverMode),
          dataRetentionPolicy = obj.optString("dataRetentionPolicy", current.dataRetentionPolicy),
          remindersEnabled = obj.optBoolean("remindersEnabled", current.remindersEnabled),
          psiReminderDays = obj.optInt("psiReminderDays", current.psiReminderDays),
          maintReminderKm = obj.optInt("maintReminderKm", current.maintReminderKm),
          weatherAlertsEnabled = obj.optBoolean("weatherAlertsEnabled", current.weatherAlertsEnabled),
          aiNotifsEnabled = obj.optBoolean("aiNotifsEnabled", current.aiNotifsEnabled),
          socialNotifsEnabled = obj.optBoolean("socialNotifsEnabled", current.socialNotifsEnabled),
          dmNotifsEnabled = obj.optBoolean("dmNotifsEnabled", current.dmNotifsEnabled),
          quietHoursEnabled = obj.optBoolean("quietHoursEnabled", current.quietHoursEnabled),
          quietStartHour = obj.optInt("quietStartHour", current.quietStartHour),
          quietEndHour = obj.optInt("quietEndHour", current.quietEndHour),
          debugMode = obj.optBoolean("debugMode", current.debugMode),
          uiChromeStyle = obj.optString("uiChromeStyle", current.uiChromeStyle),
          navBarStyle = obj.optString("navBarStyle", current.navBarStyle),
          dashboardDensity = obj.optString("dashboardDensity", current.dashboardDensity),
          showClassicTitleBars = obj.optBoolean("showClassicTitleBars", current.showClassicTitleBars),
          radarAutoFollow = obj.optBoolean("radarAutoFollow", current.radarAutoFollow),
          radarSpeedZoom = obj.optBoolean("radarSpeedZoom", current.radarSpeedZoom),
          callRingtoneEnabled = obj.optBoolean("callRingtoneEnabled", current.callRingtoneEnabled),
          autoCloudBackupEnabled = obj.optBoolean("autoCloudBackupEnabled", current.autoCloudBackupEnabled),
          lastCloudBackupMs = obj.optLong("lastCloudBackupMs", current.lastCloudBackupMs),
          lastCloudBackupProvider = obj.optString("lastCloudBackupProvider", current.lastCloudBackupProvider),
          ytPlaybackSpeed = obj.optDouble("ytPlaybackSpeed", current.ytPlaybackSpeed.toDouble()).toFloat(),
          ytShuffleEnabled = obj.optBoolean("ytShuffleEnabled", current.ytShuffleEnabled),
          ytRepeatMode = obj.optString("ytRepeatMode", current.ytRepeatMode),
          retroAudioCuesEnabled = obj.optBoolean("retroAudioCuesEnabled", current.retroAudioCuesEnabled),
          rideMode = obj.optString("rideMode", current.rideMode),
          riderWeightLbs = obj.optInt("riderWeightLbs", current.riderWeightLbs),
          dailyDistanceGoalKm = obj.optDouble("dailyDistanceGoalKm", current.dailyDistanceGoalKm.toDouble()).toFloat(),
          customWeatherCity = obj.optString("customWeatherCity", current.customWeatherCity),
          customWeatherLat = obj.optDouble("customWeatherLat", current.customWeatherLat),
          customWeatherLng = obj.optDouble("customWeatherLng", current.customWeatherLng),
          useCustomWeatherLoc = obj.optBoolean("useCustomWeatherLoc", current.useCustomWeatherLoc),
          customNewsCity = obj.optString("customNewsCity", current.customNewsCity),
          newsLocationScope = obj.optString("newsLocationScope", current.newsLocationScope),
          pevWallpaper = PevWallpaper.entries.firstOrNull { it.id == obj.optString("pevWallpaper") } ?: current.pevWallpaper,
          wallpaperOpacity = obj.optDouble("wallpaperOpacity", current.wallpaperOpacity.toDouble()).toFloat(),
          highContrastIcons = obj.optBoolean("highContrastIcons", current.highContrastIcons),
          callRingtonePreset = RingtonePreset.entries.firstOrNull { it.id == obj.optString("callRingtonePreset") } ?: current.callRingtonePreset,
          customRingtoneUri = obj.optString("customRingtoneUri", current.customRingtoneUri),
          dmAlertTone = AlertTonePreset.entries.firstOrNull { it.id == obj.optString("dmAlertTone") } ?: current.dmAlertTone,
          walkieRogerTone = AlertTonePreset.entries.firstOrNull { it.id == obj.optString("walkieRogerTone") } ?: current.walkieRogerTone,
          navTurnTone = AlertTonePreset.entries.firstOrNull { it.id == obj.optString("navTurnTone") } ?: current.navTurnTone,
          lowBatteryAlarmTone = AlertTonePreset.entries.firstOrNull { it.id == obj.optString("lowBatteryAlarmTone") } ?: current.lowBatteryAlarmTone,
          ringtoneVolumePct = obj.optInt("ringtoneVolumePct", current.ringtoneVolumePct),
          vibrationPattern = VibrationPatternPreset.entries.firstOrNull { it.id == obj.optString("vibrationPattern") } ?: current.vibrationPattern,
          ttsSpeechRate = obj.optDouble("ttsSpeechRate", current.ttsSpeechRate.toDouble()).toFloat(),
          ttsPitch = obj.optDouble("ttsPitch", current.ttsPitch.toDouble()).toFloat(),
          autoMuteAlertsWhileRiding = obj.optBoolean("autoMuteAlertsWhileRiding", current.autoMuteAlertsWhileRiding),
          navDefaultProfile = NavRouteProfile.entries.firstOrNull { it.id == obj.optString("navDefaultProfile") } ?: current.navDefaultProfile,
          navVoiceGuidance = obj.optBoolean("navVoiceGuidance", current.navVoiceGuidance),
          navAutoReroute = obj.optBoolean("navAutoReroute", current.navAutoReroute),
          navAvoidHighways = obj.optBoolean("navAvoidHighways", current.navAvoidHighways),
          navShowChargersOnMap = obj.optBoolean("navShowChargersOnMap", current.navShowChargersOnMap),
          navHeadingUpMode = obj.optBoolean("navHeadingUpMode", current.navHeadingUpMode),
          autoPauseRideWhenStopped = obj.optBoolean("autoPauseRideWhenStopped", current.autoPauseRideWhenStopped),
          lowBatteryWarningPct = obj.optInt("lowBatteryWarningPct", current.lowBatteryWarningPct),
          criticalVoltageAlertV = obj.optDouble("criticalVoltageAlertV", current.criticalVoltageAlertV.toDouble()).toFloat(),
          regenBrakingChime = obj.optBoolean("regenBrakingChime", current.regenBrakingChime),
          batterySaverTelemetry = obj.optBoolean("batterySaverTelemetry", current.batterySaverTelemetry),
          autoAcceptConvoyInvites = obj.optBoolean("autoAcceptConvoyInvites", current.autoAcceptConvoyInvites),
          stealthPrivacyMode = obj.optBoolean("stealthPrivacyMode", current.stealthPrivacyMode),
          confirmBeforeEndRide = obj.optBoolean("confirmBeforeEndRide", current.confirmBeforeEndRide),
          showSpeedLimitOverlay = obj.optBoolean("showSpeedLimitOverlay", current.showSpeedLimitOverlay),
          showAhrsTelemetryCard = obj.optBoolean("showAhrsTelemetryCard", current.showAhrsTelemetryCard),
          ahrsPeakLeanHold = obj.optBoolean("ahrsPeakLeanHold", current.ahrsPeakLeanHold),
          ahrsLeanAlertDeg = obj.optInt("ahrsLeanAlertDeg", current.ahrsLeanAlertDeg),
          ahrsGaugeDamping = obj.optString("ahrsGaugeDamping", current.ahrsGaugeDamping),
          aiChatAutoScroll = obj.optBoolean("aiChatAutoScroll", current.aiChatAutoScroll),
          aiChatCompactTools = obj.optBoolean("aiChatCompactTools", current.aiChatCompactTools),
          keepScreenBrightOnRide = obj.optBoolean("keepScreenBrightOnRide", current.keepScreenBrightOnRide),
          aiVoiceProfile =
            AiVoiceProfile.entries.firstOrNull { it.id == obj.optString("aiVoiceProfile") }
              ?: current.aiVoiceProfile,
          ruralPersonaMode = obj.optBoolean("ruralPersonaMode", current.ruralPersonaMode),
          autoFetchPevPhoto = obj.optBoolean("autoFetchPevPhoto", current.autoFetchPevPhoto),
          showRideChartsOnDash = obj.optBoolean("showRideChartsOnDash", current.showRideChartsOnDash),
          showStartupAnimation = obj.optBoolean("showStartupAnimation", current.showStartupAnimation),
          showSatelliteStatusOnDash = obj.optBoolean("showSatelliteStatusOnDash", current.showSatelliteStatusOnDash),
          showEnvironmentSensorsOnDash = obj.optBoolean("showEnvironmentSensorsOnDash", current.showEnvironmentSensorsOnDash),
          buttonHapticFeedback = obj.optBoolean("buttonHapticFeedback", current.buttonHapticFeedback),
          rideMilestoneHaptics = obj.optBoolean("rideMilestoneHaptics", current.rideMilestoneHaptics),
          autoNightFromLightSensor = obj.optBoolean("autoNightFromLightSensor", current.autoNightFromLightSensor),
          arShowSatelliteOverlay = obj.optBoolean("arShowSatelliteOverlay", current.arShowSatelliteOverlay),
          spaciousLayoutMode = obj.optBoolean("spaciousLayoutMode", current.spaciousLayoutMode),
          splashDurationSec = obj.optInt("splashDurationSec", current.splashDurationSec).coerceIn(4, 12),
          confirmCancelNavigation = obj.optBoolean("confirmCancelNavigation", current.confirmCancelNavigation),
          navAutoEndOnArrival = obj.optBoolean("navAutoEndOnArrival", current.navAutoEndOnArrival),
          keepScreenAwakeRiding = obj.optBoolean("keepScreenAwakeRiding", current.keepScreenAwakeRiding),
          mapAutoFollowHeading = obj.optBoolean("mapAutoFollowHeading", current.mapAutoFollowHeading),
          showSpeedLimitBadge = obj.optBoolean("showSpeedLimitBadge", current.showSpeedLimitBadge),
          autoSpeakAiReplies = obj.optBoolean("autoSpeakAiReplies", current.autoSpeakAiReplies),
          mechanicDefaultDepth = obj.optString("mechanicDefaultDepth", current.mechanicDefaultDepth),
          batteryReserveWarningPct = obj.optInt("batteryReserveWarningPct", current.batteryReserveWarningPct).coerceIn(5, 50),
          compassTrueNorth = obj.optBoolean("compassTrueNorth", current.compassTrueNorth),
          touchSoundsEnabled = obj.optBoolean("touchSoundsEnabled", current.touchSoundsEnabled),
          touchSoundPack = obj.optString("touchSoundPack", current.touchSoundPack),
          chartNeonGlowEnabled = obj.optBoolean("chartNeonGlowEnabled", current.chartNeonGlowEnabled),
          chartEntranceAnimMs = obj.optInt("chartEntranceAnimMs", current.chartEntranceAnimMs).coerceIn(250, 1800),
          chartHoverTooltipsEnabled = obj.optBoolean("chartHoverTooltipsEnabled", current.chartHoverTooltipsEnabled),
          compactNumericHud = obj.optBoolean("compactNumericHud", current.compactNumericHud),
          odometerDecimals = obj.optInt("odometerDecimals", current.odometerDecimals).coerceIn(0, 2),
          autoPauseSpeedKmh = obj.optDouble("autoPauseSpeedKmh", current.autoPauseSpeedKmh.toDouble()).toFloat().coerceIn(0.5f, 8.0f),
          regenStrengthPreset = obj.optString("regenStrengthPreset", current.regenStrengthPreset),
          coldWeatherBatteryGuard = obj.optBoolean("coldWeatherBatteryGuard", current.coldWeatherBatteryGuard),
          barnBreakerLimitAmps = obj.optInt("barnBreakerLimitAmps", current.barnBreakerLimitAmps).coerceIn(10, 50),
          solarChargeTargetPct = obj.optInt("solarChargeTargetPct", current.solarChargeTargetPct).coerceIn(60, 100),
          headlightDuskReminder = obj.optBoolean("headlightDuskReminder", current.headlightDuskReminder),
          duskAutoHeadlightReminderMin = obj.optInt("duskAutoHeadlightReminderMin", current.duskAutoHeadlightReminderMin).coerceIn(10, 90),
          chainLubeIntervalKm = obj.optInt("chainLubeIntervalKm", current.chainLubeIntervalKm).coerceIn(50, 1000),
          wildlifeAlertRadiusMi = obj.optInt("wildlifeAlertRadiusMi", current.wildlifeAlertRadiusMi).coerceIn(1, 25),
          farmEquipmentAlertEnabled = obj.optBoolean("farmEquipmentAlertEnabled", current.farmEquipmentAlertEnabled),
          creekDepthWarningEnabled = obj.optBoolean("creekDepthWarningEnabled", current.creekDepthWarningEnabled),
          trailSurfaceAutoDetect = obj.optBoolean("trailSurfaceAutoDetect", current.trailSurfaceAutoDetect),
          ruralRoadSignAlerts = obj.optBoolean("ruralRoadSignAlerts", current.ruralRoadSignAlerts),
          convoyBeaconIntervalSec = obj.optInt("convoyBeaconIntervalSec", current.convoyBeaconIntervalSec).coerceIn(5, 60),
          gpxIncludeElevation = obj.optBoolean("gpxIncludeElevation", current.gpxIncludeElevation),
          voiceCalloutVerbosity = obj.optString("voiceCalloutVerbosity", current.voiceCalloutVerbosity),
          audioDuckingOnSpeech = obj.optBoolean("audioDuckingOnSpeech", current.audioDuckingOnSpeech),
          windNoiseFilter = obj.optBoolean("windNoiseFilter", current.windNoiseFilter),
          nightRedShiftIntensity = obj.optDouble("nightRedShiftIntensity", current.nightRedShiftIntensity.toDouble()).toFloat().coerceIn(0.25f, 1.0f),
          winShowMenuBar = obj.optBoolean("winShowMenuBar", current.winShowMenuBar),
          winShowTaskbarClock = obj.optBoolean("winShowTaskbarClock", current.winShowTaskbarClock),
          winGlassIntensity = obj.optDouble("winGlassIntensity", current.winGlassIntensity.toDouble()).toFloat().coerceIn(0.45f, 1.0f),
          winTaskbarCompact = obj.optBoolean("winTaskbarCompact", current.winTaskbarCompact),
          iconStylePack = obj.optString("iconStylePack", current.iconStylePack),
          iconNeonGlowEnabled = obj.optBoolean("iconNeonGlowEnabled", current.iconNeonGlowEnabled),
          showQuickActionBarOnDash = obj.optBoolean("showQuickActionBarOnDash", current.showQuickActionBarOnDash),
          showRideReadinessScore = obj.optBoolean("showRideReadinessScore", current.showRideReadinessScore),
          autoNightDimHudPct = obj.optInt("autoNightDimHudPct", current.autoNightDimHudPct).coerceIn(25, 100),
          batteryVoltageAlarmEnabled = obj.optBoolean("batteryVoltageAlarmEnabled", current.batteryVoltageAlarmEnabled),
          hapticScrollDetents = obj.optBoolean("hapticScrollDetents", current.hapticScrollDetents),
          quickThemeBarEnabled = obj.optBoolean("quickThemeBarEnabled", current.quickThemeBarEnabled),
          matchPaceTier =
            RiderPaceTier.entries.firstOrNull { it.id == obj.optString("matchPaceTier") }
              ?: current.matchPaceTier,
          matchTerrainPref =
            RiderTerrainPref.entries.firstOrNull { it.id == obj.optString("matchTerrainPref") }
              ?: current.matchTerrainPref,
          matchMinRangeMi = obj.optInt("matchMinRangeMi", current.matchMinRangeMi).coerceIn(5, 150),
          matchScheduleVibe =
            RiderScheduleVibe.entries.firstOrNull { it.id == obj.optString("matchScheduleVibe") }
              ?: current.matchScheduleVibe,
          matchHomeZipOrTown = obj.optString("matchHomeZipOrTown", current.matchHomeZipOrTown),
          matchMinScorePct = obj.optInt("matchMinScorePct", current.matchMinScorePct).coerceIn(0, 95),
          matchPaceFilter =
            RiderPaceTier.entries.firstOrNull { it.id == obj.optString("matchPaceFilter") }
              ?: current.matchPaceFilter,
          matchTerrainFilter =
            RiderTerrainPref.entries.firstOrNull { it.id == obj.optString("matchTerrainFilter") }
              ?: current.matchTerrainFilter,
          matchVibeFilter =
            RiderScheduleVibe.entries.firstOrNull { it.id == obj.optString("matchVibeFilter") }
              ?: current.matchVibeFilter,
          customVoiceProfilesJson = obj.optString("customVoiceProfilesJson", current.customVoiceProfilesJson),
          activeCustomVoiceId = obj.optString("activeCustomVoiceId", current.activeCustomVoiceId),
          ttsPreloadBufferEnabled = obj.optBoolean("ttsPreloadBufferEnabled", current.ttsPreloadBufferEnabled),
          ttsPreloadBufferDepth = obj.optInt("ttsPreloadBufferDepth", current.ttsPreloadBufferDepth),
          autoCheckAppUpdates = obj.optBoolean("autoCheckAppUpdates", current.autoCheckAppUpdates),
          appUpdateNotifsEnabled = obj.optBoolean("appUpdateNotifsEnabled", current.appUpdateNotifsEnabled),
          includePrereleaseBuilds = obj.optBoolean("includePrereleaseBuilds", current.includePrereleaseBuilds),
          preferredUpdateSource = obj.optString("preferredUpdateSource", current.preferredUpdateSource),
          updateCheckIntervalHours = obj.optInt("updateCheckIntervalHours", current.updateCheckIntervalHours).coerceIn(6, 48),
        )
      writeAll(restored)
    }

  private suspend fun writeAll(updated: MasterSettings) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.THEME_PRESET] = updated.themePreset.id
      prefs[Keys.VISUAL_STYLE] = updated.visualStyle.id
      prefs[Keys.COLOR_MODE_OVERRIDE] = updated.colorModeOverride.id
      prefs[Keys.HUD_STYLE] = updated.hudStyle.id
      prefs[Keys.HUD_3D_TILT] = updated.hud3dTilt
      prefs[Keys.NIGHT_VISION] = updated.nightVisionMode
      prefs[Keys.SUN_VISIBILITY] = updated.sunVisibilityMode
      prefs[Keys.FONT_PRESET] = updated.fontPreset.id
      prefs[Keys.BG_TEXTURE] = updated.bgTexture.id
      prefs[Keys.LOCALE] = updated.locale
      prefs[Keys.UI_SCALE] = updated.uiScale
      prefs[Keys.REDUCED_MOTION] = updated.reducedMotion
      prefs[Keys.CORNER_STYLE] = updated.cornerStyle
      prefs[Keys.BORDER_WIDTH] = updated.borderWidthDp
      prefs[Keys.BORDER_GLOW] = updated.borderGlowIntensity
      prefs[Keys.CARD_OPACITY] = updated.cardOpacity
      prefs[Keys.HAPTIC_FEEDBACK] = updated.hapticFeedback
      prefs[Keys.HAPTIC_DURATION] = updated.hapticDurationMs
      prefs[Keys.GLOBAL_VOLUME] = updated.globalVolume
      prefs[Keys.SOUND_PACK] = updated.soundPack
      prefs[Keys.AUTO_MUTE_VIDEO] = updated.autoMuteOnVideo
      prefs[Keys.DISTANCE_UNIT] = updated.distanceUnit
      prefs[Keys.TEMP_UNIT] = updated.tempUnit
      prefs[Keys.PRESSURE_UNIT] = updated.pressureUnit
      prefs[Keys.DECIMAL_PRECISION] = updated.decimalPrecision
      prefs[Keys.GPS_ACC_LIMIT] = updated.gpsAccuracyLimitM
      prefs[Keys.TELEMETRY_HZ] = updated.telemetryRateHz
      prefs[Keys.IMU_HZ] = updated.imuPollingRateHz
      prefs[Keys.PRIVACY_ZONE_M] = updated.privacyZoneMeters
      prefs[Keys.AUTO_START_TRACKING] = updated.autoStartTracking
      prefs[Keys.AUTO_WEATHER_SYNC] = updated.autoWeatherSync
      prefs[Keys.WEATHER_MIN] = updated.weatherUpdateMin
      prefs[Keys.SHARE_LEADERBOARD] = updated.shareLeaderboard
      prefs[Keys.PUBLIC_PROFILE] = updated.publicProfile
      prefs[Keys.BIOMETRIC_LOCK] = updated.biometricAppLock
      prefs[Keys.CRASH_G] = updated.crashSensitivityG
      prefs[Keys.SOS_WINDOW] = updated.sosCancelWindowSec
      prefs[Keys.SOS_TEMPLATE] = updated.sosTemplate
      prefs[Keys.VOICE_HOTWORD] = updated.voiceHotwordActive
      prefs[Keys.WAKE_LOCK] = updated.wakeLockEnabled
      prefs[Keys.AUTO_DIMMER] = updated.autoDimmer
      prefs[Keys.SMART_BATTERY_AI] = updated.smartBatteryAi
      prefs[Keys.SAG_COMP] = updated.sagCompensation
      prefs[Keys.VOLT_DISP] = updated.voltDisplayMode
      prefs[Keys.MOTOR_TEMP_WARN] = updated.motorTempWarnC
      prefs[Keys.SCHOOL_ZONE] = updated.schoolZoneStrict
      prefs[Keys.LEGAL_COMPLIANCE] = updated.legalComplianceMode
      prefs[Keys.PEV_LEGAL_CLASS] = updated.pevLegalClass.id
      prefs[Keys.LEGAL_AUDIO_ALERT] = updated.legalAudioAlert
      prefs[Keys.HUD_BLINK_MAX] = updated.hudBlinkMax
      prefs[Keys.SPEED_OFFSET] = updated.speedOffsetTolerance
      prefs[Keys.RADAR_RADIUS] = updated.radarRadiusMiles
      prefs[Keys.DIAL_SWEEP] = updated.dialSweepDeg
      prefs[Keys.ELEVATION_GRADIENT] = updated.elevationGradient
      prefs[Keys.QUICK_REPLIES] = updated.quickReplies
      prefs[Keys.PILOT_RANK] = updated.pilotRankBadge
      prefs[Keys.AUTO_CLOUD_SYNC] = updated.autoCloudSyncOnFinish
      prefs[Keys.KWH_RATE] = updated.kwhRateUsd
      prefs[Keys.GAS_PRICE] = updated.gasPriceUsd
      prefs[Keys.CAR_MPG] = updated.carMpg
      prefs[Keys.COPILOT_PERSONA] = updated.copilotPersona
      prefs[Keys.AI_MODEL_ID] = updated.aiModelId
      prefs[Keys.AI_THINKING_LEVEL] = updated.aiThinkingLevel
      prefs[Keys.AI_CREATIVITY] = updated.aiCreativity
      prefs[Keys.AI_DETAIL_LEVEL] = updated.aiDetailLevel
      prefs[Keys.AI_WEB_GROUNDING] = updated.aiWebGrounding
      prefs[Keys.BATTERY_CHEMISTRY] = updated.batteryChemistry
      prefs[Keys.REGEN_ALGORITHM] = updated.regenAlgorithm
      prefs[Keys.MAX_FPS] = updated.maxFps
      prefs[Keys.DM_CHIME] = updated.dmChime
      prefs[Keys.DEFAULT_FEED_CAT] = updated.defaultFeedCategory
      prefs[Keys.BOARD_POST_LIMIT] = updated.boardPostLimit
      prefs[Keys.DATA_SAVER_MODE] = updated.dataSaverMode
      prefs[Keys.DATA_RETENTION_POLICY] = updated.dataRetentionPolicy
      prefs[Keys.REMINDERS_ENABLED] = updated.remindersEnabled
      prefs[Keys.PSI_REMINDER_DAYS] = updated.psiReminderDays
      prefs[Keys.MAINT_REMINDER_KM] = updated.maintReminderKm
      prefs[Keys.WEATHER_ALERTS] = updated.weatherAlertsEnabled
      prefs[Keys.AI_NOTIFS_ENABLED] = updated.aiNotifsEnabled
      prefs[Keys.SOCIAL_NOTIFS_ENABLED] = updated.socialNotifsEnabled
      prefs[Keys.DM_NOTIFS_ENABLED] = updated.dmNotifsEnabled
      prefs[Keys.QUIET_HOURS] = updated.quietHoursEnabled
      prefs[Keys.QUIET_START] = updated.quietStartHour
      prefs[Keys.QUIET_END] = updated.quietEndHour
      prefs[Keys.DEBUG_MODE] = updated.debugMode
      prefs[Keys.UI_CHROME_STYLE] = updated.uiChromeStyle
      prefs[Keys.NAV_BAR_STYLE] = updated.navBarStyle
      prefs[Keys.DASHBOARD_DENSITY] = updated.dashboardDensity
      prefs[Keys.SHOW_CLASSIC_TITLE_BARS] = updated.showClassicTitleBars
      prefs[Keys.RADAR_AUTO_FOLLOW] = updated.radarAutoFollow
      prefs[Keys.RADAR_SPEED_ZOOM] = updated.radarSpeedZoom
      prefs[Keys.CALL_RINGTONE] = updated.callRingtoneEnabled
      prefs[Keys.AUTO_CLOUD_BACKUP] = updated.autoCloudBackupEnabled
      prefs[Keys.LAST_CLOUD_BACKUP_MS] = updated.lastCloudBackupMs
      prefs[Keys.LAST_CLOUD_BACKUP_PROVIDER] = updated.lastCloudBackupProvider
      prefs[Keys.YT_SPEED] = updated.ytPlaybackSpeed
      prefs[Keys.YT_SHUFFLE] = updated.ytShuffleEnabled
      prefs[Keys.YT_REPEAT] = updated.ytRepeatMode
      prefs[Keys.RETRO_AUDIO_CUES] = updated.retroAudioCuesEnabled
      prefs[Keys.RIDE_MODE] = updated.rideMode
      prefs[Keys.RIDER_WEIGHT_LBS] = updated.riderWeightLbs
      prefs[Keys.DAILY_DISTANCE_GOAL_KM] = updated.dailyDistanceGoalKm
      prefs[Keys.CUSTOM_WEATHER_CITY] = updated.customWeatherCity
      prefs[Keys.CUSTOM_WEATHER_LAT] = updated.customWeatherLat
      prefs[Keys.CUSTOM_WEATHER_LNG] = updated.customWeatherLng
      prefs[Keys.USE_CUSTOM_WEATHER_LOC] = updated.useCustomWeatherLoc
      prefs[Keys.CUSTOM_NEWS_CITY] = updated.customNewsCity
      prefs[Keys.NEWS_LOCATION_SCOPE] = updated.newsLocationScope
      prefs[Keys.PEV_WALLPAPER] = updated.pevWallpaper.id
      prefs[Keys.WALLPAPER_OPACITY] = updated.wallpaperOpacity
      prefs[Keys.HIGH_CONTRAST_ICONS] = updated.highContrastIcons
      prefs[Keys.CALL_RINGTONE_PRESET] = updated.callRingtonePreset.id
      prefs[Keys.CUSTOM_RINGTONE_URI] = updated.customRingtoneUri
      prefs[Keys.DM_ALERT_TONE] = updated.dmAlertTone.id
      prefs[Keys.WALKIE_ROGER_TONE] = updated.walkieRogerTone.id
      prefs[Keys.NAV_TURN_TONE] = updated.navTurnTone.id
      prefs[Keys.LOW_BATT_ALARM_TONE] = updated.lowBatteryAlarmTone.id
      prefs[Keys.RINGTONE_VOL_PCT] = updated.ringtoneVolumePct
      prefs[Keys.VIBRATION_PATTERN] = updated.vibrationPattern.id
      prefs[Keys.TTS_SPEECH_RATE] = updated.ttsSpeechRate
      prefs[Keys.TTS_PITCH] = updated.ttsPitch
      prefs[Keys.AUTO_MUTE_RIDING] = updated.autoMuteAlertsWhileRiding
      prefs[Keys.NAV_DEFAULT_PROFILE] = updated.navDefaultProfile.id
      prefs[Keys.NAV_VOICE_GUIDANCE] = updated.navVoiceGuidance
      prefs[Keys.NAV_AUTO_REROUTE] = updated.navAutoReroute
      prefs[Keys.NAV_AVOID_HIGHWAYS] = updated.navAvoidHighways
      prefs[Keys.NAV_SHOW_CHARGERS] = updated.navShowChargersOnMap
      prefs[Keys.NAV_HEADING_UP] = updated.navHeadingUpMode
      prefs[Keys.AUTO_PAUSE_RIDE] = updated.autoPauseRideWhenStopped
      prefs[Keys.LOW_BATT_WARN_PCT] = updated.lowBatteryWarningPct
      prefs[Keys.CRIT_VOLTAGE_V] = updated.criticalVoltageAlertV
      prefs[Keys.REGEN_CHIME] = updated.regenBrakingChime
      prefs[Keys.BATT_SAVER_TELEM] = updated.batterySaverTelemetry
      prefs[Keys.AUTO_ACCEPT_CONVOY] = updated.autoAcceptConvoyInvites
      prefs[Keys.STEALTH_PRIVACY] = updated.stealthPrivacyMode
      prefs[Keys.CONFIRM_END_RIDE] = updated.confirmBeforeEndRide
      prefs[Keys.SHOW_SPEED_OVERLAY] = updated.showSpeedLimitOverlay
      prefs[Keys.SHOW_AHRS_CARD] = updated.showAhrsTelemetryCard
      prefs[Keys.AHRS_PEAK_LEAN] = updated.ahrsPeakLeanHold
      prefs[Keys.AHRS_LEAN_ALERT_DEG] = updated.ahrsLeanAlertDeg
      prefs[Keys.AHRS_GAUGE_DAMPING] = updated.ahrsGaugeDamping
      prefs[Keys.AI_CHAT_AUTO_SCROLL] = updated.aiChatAutoScroll
      prefs[Keys.AI_CHAT_COMPACT_TOOLS] = updated.aiChatCompactTools
      prefs[Keys.KEEP_BRIGHT_ON_RIDE] = updated.keepScreenBrightOnRide
      prefs[Keys.AI_VOICE_PROFILE] = updated.aiVoiceProfile.id
      prefs[Keys.RURAL_PERSONA_MODE] = updated.ruralPersonaMode
      prefs[Keys.AUTO_FETCH_PEV_PHOTO] = updated.autoFetchPevPhoto
      prefs[Keys.SHOW_RIDE_CHARTS_ON_DASH] = updated.showRideChartsOnDash
      prefs[Keys.SHOW_STARTUP_ANIM] = updated.showStartupAnimation
      prefs[Keys.SHOW_SAT_STATUS_DASH] = updated.showSatelliteStatusOnDash
      prefs[Keys.SHOW_ENV_SENSORS_DASH] = updated.showEnvironmentSensorsOnDash
      prefs[Keys.BUTTON_HAPTIC_FEEDBACK] = updated.buttonHapticFeedback
      prefs[Keys.RIDE_MILESTONE_HAPTICS] = updated.rideMilestoneHaptics
      prefs[Keys.AUTO_NIGHT_LIGHT_SENSOR] = updated.autoNightFromLightSensor
      prefs[Keys.AR_SHOW_SATELLITE_OVERLAY] = updated.arShowSatelliteOverlay
      prefs[Keys.SPACIOUS_LAYOUT_MODE] = updated.spaciousLayoutMode
      prefs[Keys.SPLASH_DURATION_SEC] = updated.splashDurationSec.coerceIn(4, 12)
      prefs[Keys.CONFIRM_CANCEL_NAV] = updated.confirmCancelNavigation
      prefs[Keys.NAV_AUTO_END_ARRIVAL] = updated.navAutoEndOnArrival
      prefs[Keys.KEEP_SCREEN_AWAKE_RIDING] = updated.keepScreenAwakeRiding
      prefs[Keys.MAP_AUTO_FOLLOW_HEADING] = updated.mapAutoFollowHeading
      prefs[Keys.SHOW_SPEED_LIMIT_BADGE] = updated.showSpeedLimitBadge
      prefs[Keys.AUTO_SPEAK_AI_REPLIES] = updated.autoSpeakAiReplies
      prefs[Keys.MECHANIC_DEFAULT_DEPTH] = updated.mechanicDefaultDepth
      prefs[Keys.BATTERY_RESERVE_WARN_PCT] = updated.batteryReserveWarningPct.coerceIn(5, 50)
      prefs[Keys.COMPASS_TRUE_NORTH] = updated.compassTrueNorth
      prefs[Keys.TOUCH_SOUNDS_ENABLED] = updated.touchSoundsEnabled
      prefs[Keys.TOUCH_SOUND_PACK] = updated.touchSoundPack
      prefs[Keys.CHART_NEON_GLOW] = updated.chartNeonGlowEnabled
      prefs[Keys.CHART_ENTRANCE_ANIM_MS] = updated.chartEntranceAnimMs.coerceIn(250, 1800)
      prefs[Keys.CHART_HOVER_TOOLTIPS] = updated.chartHoverTooltipsEnabled
      prefs[Keys.COMPACT_NUMERIC_HUD] = updated.compactNumericHud
      prefs[Keys.ODOMETER_DECIMALS] = updated.odometerDecimals.coerceIn(0, 2)
      prefs[Keys.AUTO_PAUSE_SPEED_KMH] = updated.autoPauseSpeedKmh.coerceIn(0.5f, 8.0f)
      prefs[Keys.REGEN_STRENGTH_PRESET] = updated.regenStrengthPreset
      prefs[Keys.COLD_WEATHER_BATT_GUARD] = updated.coldWeatherBatteryGuard
      prefs[Keys.BARN_BREAKER_LIMIT_AMPS] = updated.barnBreakerLimitAmps.coerceIn(10, 50)
      prefs[Keys.SOLAR_CHARGE_TARGET_PCT] = updated.solarChargeTargetPct.coerceIn(60, 100)
      prefs[Keys.HEADLIGHT_DUSK_REMINDER] = updated.headlightDuskReminder
      prefs[Keys.DUSK_REMINDER_MIN] = updated.duskAutoHeadlightReminderMin.coerceIn(10, 90)
      prefs[Keys.CHAIN_LUBE_INTERVAL_KM] = updated.chainLubeIntervalKm.coerceIn(50, 1000)
      prefs[Keys.WILDLIFE_ALERT_RADIUS_MI] = updated.wildlifeAlertRadiusMi.coerceIn(1, 25)
      prefs[Keys.FARM_EQUIPMENT_ALERT] = updated.farmEquipmentAlertEnabled
      prefs[Keys.CREEK_DEPTH_WARNING] = updated.creekDepthWarningEnabled
      prefs[Keys.TRAIL_SURFACE_AUTO_DETECT] = updated.trailSurfaceAutoDetect
      prefs[Keys.RURAL_ROAD_SIGN_ALERTS] = updated.ruralRoadSignAlerts
      prefs[Keys.CONVOY_BEACON_INTERVAL_SEC] = updated.convoyBeaconIntervalSec.coerceIn(5, 60)
      prefs[Keys.GPX_INCLUDE_ELEVATION] = updated.gpxIncludeElevation
      prefs[Keys.VOICE_CALLOUT_VERBOSITY] = updated.voiceCalloutVerbosity
      prefs[Keys.AUDIO_DUCKING_ON_SPEECH] = updated.audioDuckingOnSpeech
      prefs[Keys.WIND_NOISE_FILTER] = updated.windNoiseFilter
      prefs[Keys.NIGHT_RED_SHIFT_INTENSITY] = updated.nightRedShiftIntensity.coerceIn(0.25f, 1.0f)
      prefs[Keys.WIN_SHOW_MENU_BAR] = updated.winShowMenuBar
      prefs[Keys.WIN_SHOW_TASKBAR_CLOCK] = updated.winShowTaskbarClock
      prefs[Keys.WIN_GLASS_INTENSITY] = updated.winGlassIntensity.coerceIn(0.45f, 1.0f)
      prefs[Keys.WIN_TASKBAR_COMPACT] = updated.winTaskbarCompact
      prefs[Keys.ICON_STYLE_PACK] = updated.iconStylePack
      prefs[Keys.ICON_NEON_GLOW] = updated.iconNeonGlowEnabled
      prefs[Keys.SHOW_QUICK_ACTION_BAR] = updated.showQuickActionBarOnDash
      prefs[Keys.SHOW_RIDE_READINESS] = updated.showRideReadinessScore
      prefs[Keys.AUTO_NIGHT_DIM_HUD_PCT] = updated.autoNightDimHudPct.coerceIn(25, 100)
      prefs[Keys.BATTERY_VOLT_ALARM] = updated.batteryVoltageAlarmEnabled
      prefs[Keys.HAPTIC_SCROLL_DETENTS] = updated.hapticScrollDetents
      prefs[Keys.QUICK_THEME_BAR] = updated.quickThemeBarEnabled
      prefs[Keys.MATCH_PACE_TIER] = updated.matchPaceTier.id
      prefs[Keys.MATCH_TERRAIN_PREF] = updated.matchTerrainPref.id
      prefs[Keys.MATCH_MIN_RANGE_MI] = updated.matchMinRangeMi.coerceIn(5, 150)
      prefs[Keys.MATCH_SCHEDULE_VIBE] = updated.matchScheduleVibe.id
      prefs[Keys.MATCH_HOME_ZIP] = updated.matchHomeZipOrTown
      prefs[Keys.MATCH_MIN_SCORE_PCT] = updated.matchMinScorePct.coerceIn(0, 95)
      prefs[Keys.MATCH_PACE_FILTER] = updated.matchPaceFilter.id
      prefs[Keys.MATCH_TERRAIN_FILTER] = updated.matchTerrainFilter.id
      prefs[Keys.MATCH_VIBE_FILTER] = updated.matchVibeFilter.id
      prefs[Keys.CUSTOM_VOICE_PROFILES_JSON] = updated.customVoiceProfilesJson
      prefs[Keys.ACTIVE_CUSTOM_VOICE_ID] = updated.activeCustomVoiceId
      prefs[Keys.TTS_PRELOAD_BUFFER_ENABLED] = updated.ttsPreloadBufferEnabled
      prefs[Keys.TTS_PRELOAD_BUFFER_DEPTH] = updated.ttsPreloadBufferDepth.coerceIn(1, 5)
      prefs[Keys.AUTO_CHECK_APP_UPDATES] = updated.autoCheckAppUpdates
      prefs[Keys.APP_UPDATE_NOTIFS_ENABLED] = updated.appUpdateNotifsEnabled
      prefs[Keys.INCLUDE_PRERELEASE_BUILDS] = updated.includePrereleaseBuilds
      prefs[Keys.PREFERRED_UPDATE_SOURCE] = updated.preferredUpdateSource
      prefs[Keys.UPDATE_CHECK_INTERVAL_HOURS] = updated.updateCheckIntervalHours.coerceIn(6, 48)
      prefs[Keys.LAST_NOTIFIED_UPDATE_TAG] = updated.lastNotifiedUpdateTag
      prefs[Keys.SKIPPED_UPDATE_TAG] = updated.skippedUpdateTag
      prefs[Keys.LAST_UPDATE_CHECK_MS] = updated.lastUpdateCheckMs
    }
  }
}
