package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiAiRepository
import com.example.ai.GeminiModelCatalog
import com.example.core.LiveCloudServices
import com.example.core.MapsSpeedLimitClient
import com.example.core.MissionCardShareUtil
import com.example.core.PevBatteryPhysics
import com.example.data.AiVoiceProfile
import com.example.data.AppDatabase
import com.example.data.CustomVoiceProfile
import com.example.data.HudStyle
import com.example.data.OfflineRideEntity
import com.example.data.VoiceDspProfile
import com.example.data.parseCustomVoiceProfiles
import com.example.data.serializeCustomVoiceProfiles
import com.example.hardware.VoiceAudioManager
import com.example.data.AppThemePreset
import com.example.data.ColorModeOverride
import com.example.data.LocalRiderVault
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.data.PevCatalogData
import com.example.data.PevLegalClass
import com.example.data.PevPartItem
import com.example.data.PevSpecRecord
import com.example.data.PevStoreDirectory
import com.example.data.PevVehicle
import com.example.data.PublicPilotProfile
import com.example.data.RadarRider
import com.example.data.RideSession
import com.example.data.RiderMatchEngine
import com.example.data.RiderPaceTier
import com.example.data.RiderScheduleVibe
import com.example.data.RiderTerrainPref
import com.example.data.SettingsStore
import com.example.data.SupabaseAuthService
import com.example.data.VisualStyleMode
import com.example.data.getActiveCustomVoice
import com.example.data.resolveEffectiveBase
import com.example.hardware.AhrsStreamProcessor
import com.example.hardware.CrashImpactDetector
import com.example.ui.components.ALL_NAV_BAR_STYLES
import com.example.ui.components.TacticalMapLayer
import com.example.ui.components.availableMapLayers
import com.example.ui.components.buildYoutubeIframeHtml
import com.example.ui.components.youtubeEmbedOriginForPackage
import com.example.ui.components.defaultMapLayerId
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.navBarMiniPlayerBottomOffset
import com.example.ui.components.resolveEffectiveHudSpeedLimitMph
import com.example.ui.components.resolveMapTileSource
import com.example.ui.screens.ArHudMode
import com.example.ui.screens.Pev3dArchetype
import com.example.ui.screens.Pev3dEngine
import com.example.ui.screens.Pev3dGltfBuilder
import com.example.ui.screens.Pev3dPbrTextures
import com.example.ui.screens.Pev3dRenderMode
import com.example.ui.screens.Pev3dTurnSignalMode
import com.example.ui.screens.PevPbrMaterialId
import com.example.ui.screens.PevRangeMath
import com.example.ui.screens.PevRtbMath
import com.example.ui.screens.buildWebRtcCallHtml
import com.example.ui.screens.parseSunsetMinutesRemaining
import com.example.ui.theme.TacticalChromeFamily
import com.example.ui.theme.TacticalColors
import com.example.ui.theme.resolveChromeSpec
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rural Erides GO", appName)
  }

  @Test
  fun `supabase auth configuration validation rejects placeholders`() {
    assertFalse(SupabaseAuthService.isConfiguredValues("", ""))
    assertFalse(SupabaseAuthService.isConfiguredValues("https://YOUR_PROJECT.supabase.co", "MY_SUPABASE_KEY"))
    assertFalse(SupabaseAuthService.isConfiguredValues("http://insecure.supabase.co", "sb_publishable_valid_key"))
    assertTrue(SupabaseAuthService.isConfiguredValues("https://tqqisnxeydoxqvnozlms.supabase.co", "sb_publishable_valid_key"))
  }

  @Test
  fun `supabase session json parsing extracts callsign and tokens`() {
    val raw = JSONObject(
      """
      {
        "access_token": "jwt_access_123",
        "refresh_token": "jwt_refresh_456",
        "expires_in": 3600,
        "user": {
          "id": "usr_001",
          "email": "rider@ruralerides.com",
          "role": "authenticated",
          "user_metadata": {
            "callsign": "APEX_72V"
          }
        }
      }
      """.trimIndent()
    )
    val parsed = SupabaseAuthService.parseSessionJson(raw)
    assertNotNull(parsed)
    assertEquals("jwt_access_123", parsed!!.accessToken)
    assertEquals("jwt_refresh_456", parsed.refreshToken)
    assertEquals("usr_001", parsed.user.id)
    assertEquals("rider@ruralerides.com", parsed.user.email)
    assertEquals("APEX_72V", parsed.user.callsign)
  }

  @Test
  fun `supabase error parser handles GoTrue json payloads and http 400 and 502 errors`() {
    val msg = SupabaseAuthService.parseSupabaseError("""{"error_description":"Invalid login credentials"}""", 400)
    assertEquals("Invalid login credentials", msg)

    val unconfirmed =
      SupabaseAuthService.parseSupabaseError(
        """{"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}""",
        400,
      )
    assertTrue(unconfirmed.contains("not confirmed", ignoreCase = true))
    assertTrue(SupabaseAuthService.isEmailNotConfirmedError("""{"error_code":"email_not_confirmed"}"""))

    val postgrestHint =
      SupabaseAuthService.parseSupabaseError(
        """{"code":"42703","message":"column board_posts.created_at does not exist","hint":"Perhaps you meant createdAt"}""",
        400,
      )
    assertTrue(postgrestHint.contains("does not exist"))
    assertTrue(postgrestHint.contains("Perhaps you meant createdAt"))

    val gateway502 = SupabaseAuthService.parseSupabaseError("not-json", 502)
    assertEquals(
      "Supabase gateway unavailable (HTTP 502). Verify project status in Supabase Dashboard or retry.",
      gateway502,
    )
    val generic422 = SupabaseAuthService.parseSupabaseError("not-json", 422)
    assertEquals("Supabase Auth error (HTTP 422)", generic422)
  }

  @Test
  fun `supabase sanitizes quoted urls keys and validates email formats`() {
    assertEquals(
      "https://abc.supabase.co",
      SupabaseAuthService.sanitizeBaseUrl(" \"https://abc.supabase.co/auth/v1/\" "),
    )
    assertEquals("sb_key_123", SupabaseAuthService.sanitizeAnonKey(" 'sb_key_123' "))
    assertTrue(SupabaseAuthService.isValidEmail("rider@ruralerides.com"))
    assertFalse(SupabaseAuthService.isValidEmail("not-an-email"))
    assertFalse(SupabaseAuthService.isValidEmail("@missing.com"))
    assertFalse(SupabaseAuthService.isConfiguredValues("https://abc.supabase.co", "YOUR_SUPABASE_ANON_KEY"))
  }

  @Test
  fun `supabase non-play oauth pkce challenge and callback params parse cleanly`() {
    // RFC 7636 Appendix B test vector
    val challenge = SupabaseAuthService.computePkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
    assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)

    val verifier = SupabaseAuthService.generatePkceVerifier()
    assertTrue(verifier.length >= 43)

    val pkceParams = SupabaseAuthService.parseUrlParams("?code=auth_code_123&state=st_abc")
    assertEquals("auth_code_123", pkceParams["code"])
    assertEquals("st_abc", pkceParams["state"])

    val implicitParams = SupabaseAuthService.parseUrlParams("#access_token=jwt_tok&refresh_token=ref_1&expires_in=3600")
    assertEquals("jwt_tok", implicitParams["access_token"])
    assertEquals("ref_1", implicitParams["refresh_token"])
    assertEquals("3600", implicitParams["expires_in"])
  }

  @Test
  fun `gemini default model is 3_5 flash lite and fallback chain rotates cleanly`() {
    val defaults = MasterSettings()
    assertEquals(GeminiModelCatalog.GEMINI_3_5_FLASH_LITE, defaults.aiModelId)
    assertEquals("gemini-3.5-flash-lite", defaults.aiModelId)
    assertEquals("NMC", defaults.batteryChemistry)
    assertEquals(50, defaults.boardPostLimit)
    val chain = GeminiModelCatalog.fallbackChain(defaults.aiModelId)
    assertEquals(
      listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
      ),
      chain,
    )
  }

  @Test
  fun `gemini 429 and quota errors trigger automatic model fallback`() {
    val repo = GeminiAiRepository()
    assertTrue(repo.isRetryableModelFallbackError(IllegalStateException("HTTP 429 Too Many Requests")))
    assertTrue(repo.isRetryableModelFallbackError(RuntimeException("RESOURCE_EXHAUSTED: RPD quota limit reached")))
    assertTrue(
      repo.isRetryableModelFallbackError(
        RuntimeException("Outer failure", IllegalStateException("Free-tier quota exceeded for model"))
      )
    )
    assertTrue(repo.isRetryableModelFallbackError(IllegalStateException("404 model not found")))
    assertFalse(repo.isRetryableModelFallbackError(IllegalStateException("Socket network timeout")))
  }

  @Test
  fun `road speed limit parser avoids 2500 concatenation and validates mph or kmh bounds`() {
    assertEquals(25, LiveCloudServices.parseRoadSpeedLimitMph("25.00", defaultAsKmh = false))
    assertEquals(25, LiveCloudServices.parseRoadSpeedLimitMph("25;00", defaultAsKmh = false))
    assertEquals(25, LiveCloudServices.parseRoadSpeedLimitMph("25mph", defaultAsKmh = true))
    assertEquals(25, LiveCloudServices.parseRoadSpeedLimitMph("40 km/h", defaultAsKmh = true))
    assertNull(LiveCloudServices.parseRoadSpeedLimitMph("2500", defaultAsKmh = false))
    assertNull(LiveCloudServices.parseRoadSpeedLimitMph("999 mph", defaultAsKmh = false))
    assertNull(LiveCloudServices.parseRoadSpeedLimitMph("", defaultAsKmh = false))
  }

  @Test
  fun `windows classic and specialized theme color constants match palette specs`() {
    assertEquals(androidx.compose.ui.graphics.Color(0xFFC0C0C0), TacticalColors.Win95Silver)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000080), TacticalColors.Win95TitleNavy)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF008080), TacticalColors.Win95DesktopTeal)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0055EA), TacticalColors.WinXPBorderBlue)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0058E6), TacticalColors.WinXPLunaBlueTop)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF3399FF), TacticalColors.Win7AeroBorder)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0078D7), TacticalColors.Win10FluentBlue)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0067C0), TacticalColors.Win11MicaAccent)
  }

  @Test
  fun `isWindowsClassicActive honors night vision and sunlight overrides`() {
    val classicSettings = MasterSettings(visualStyle = VisualStyleMode.WINDOWS_CLASSIC)
    assertTrue(isWindowsClassicActive(classicSettings))
    assertFalse(isWindowsClassicActive(classicSettings.copy(nightVisionMode = true)))
    assertFalse(isWindowsClassicActive(classicSettings.copy(sunVisibilityMode = true)))
    assertFalse(isWindowsClassicActive(MasterSettings(visualStyle = VisualStyleMode.DARK_STUDIO)))
  }

  @Test
  fun `settings store json export and import round trips new ui chrome backup and player settings`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = SettingsStore(context)
    store.resetAll()
    store.update {
      it.copy(
        visualStyle = VisualStyleMode.WINDOWS_CLASSIC,
        navBarStyle = "classic_taskbar",
        dashboardDensity = "compact",
        showClassicTitleBars = false,
        radarAutoFollow = false,
        radarSpeedZoom = true,
        callRingtoneEnabled = false,
        autoCloudBackupEnabled = true,
        lastCloudBackupMs = 1732000000000L,
        lastCloudBackupProvider = "Firestore + Supabase",
        ytPlaybackSpeed = 1.5f,
        ytShuffleEnabled = true,
        ytRepeatMode = "all",
        retroAudioCuesEnabled = false,
      )
    }

    val exported = store.exportAsJson()
    store.resetAll()
    val importResult = store.importFromJson(exported)
    assertTrue(importResult.isSuccess)

    val restored = store.settingsFlow.first()
    assertEquals(VisualStyleMode.WINDOWS_CLASSIC, restored.visualStyle)
    assertEquals("classic_taskbar", restored.navBarStyle)
    assertEquals("compact", restored.dashboardDensity)
    assertFalse(restored.showClassicTitleBars)
    assertFalse(restored.radarAutoFollow)
    assertTrue(restored.radarSpeedZoom)
    assertFalse(restored.callRingtoneEnabled)
    assertTrue(restored.autoCloudBackupEnabled)
    assertEquals(1732000000000L, restored.lastCloudBackupMs)
    assertEquals("Firestore + Supabase", restored.lastCloudBackupProvider)
    assertEquals(1.5f, restored.ytPlaybackSpeed)
    assertTrue(restored.ytShuffleEnabled)
    assertEquals("all", restored.ytRepeatMode)
    assertFalse(restored.retroAudioCuesEnabled)
  }

  @Test
  fun `local rider vault full backup json round trips vehicles`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vault = LocalRiderVault(context)
    vault.clearAll()
    val uid = "sb_test_user"
    val rig = PevVehicle(
      id = "rig_01",
      userId = uid,
      name = "Kaabo Wolf King GT",
      category = "SCOOTER",
      batteryWh = 2520L,
      voltage = 72.0,
      targetPsi = 45.0,
      odometerKm = 320.5,
      serviceIntervalKm = 500.0,
      tireSize = "11x3.5",
    )
    vault.saveVehicles(uid, listOf(rig))
    val backupJson = vault.exportFullBackupJson(uid, """{"navBarStyle":"dock"}""")

    vault.clearAll()
    assertTrue(vault.loadVehicles(uid).isEmpty())

    val restoreResult = vault.restoreFullBackupJson(uid, backupJson)
    assertTrue(restoreResult.isSuccess)
    val loaded = vault.loadVehicles(uid)
    assertEquals(1, loaded.size)
    assertEquals("Kaabo Wolf King GT", loaded.first().name)
    assertEquals(72.0, loaded.first().voltage, 0.01)
  }

  @Test
  fun `webrtc call html includes RTCPeerConnection and stun server for audio and video`() {
    val videoCallerHtml = buildWebRtcCallHtml(isCaller = true, isVideo = true)
    assertTrue(videoCallerHtml.contains("RTCPeerConnection"))
    assertTrue(videoCallerHtml.contains("stun:stun.l.google.com:19302"))
    assertTrue(videoCallerHtml.contains("const IS_CALLER = true"))
    assertTrue(videoCallerHtml.contains("const IS_VIDEO = true"))

    val audioCalleeHtml = buildWebRtcCallHtml(isCaller = false, isVideo = false)
    assertTrue(audioCalleeHtml.contains("RTCPeerConnection"))
    assertTrue(audioCalleeHtml.contains("stun:stun.l.google.com:19302"))
    assertTrue(audioCalleeHtml.contains("const IS_CALLER = false"))
    assertTrue(audioCalleeHtml.contains("const IS_VIDEO = false"))
  }

  @Test
  fun `youtube mini player html includes strict referrer policy and package-id origin for error 152-4 prevention`() {
    val origin = youtubeEmbedOriginForPackage("com.aistudio.volttrail.kqrmvx")
    assertEquals("https://com.aistudio.volttrail.kqrmvx", origin)
    val html = buildYoutubeIframeHtml("dQw4w9WgXcQ", origin)
    assertTrue(html.contains("""referrerpolicy="strict-origin-when-cross-origin""""))
    assertTrue(html.contains("""<meta name="referrer" content="strict-origin-when-cross-origin">"""))
    assertTrue(html.contains("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    assertTrue(html.contains("origin=https%3A%2F%2Fcom.aistudio.volttrail.kqrmvx"))
    assertTrue(html.contains("widget_referrer=https%3A%2F%2Fcom.aistudio.volttrail.kqrmvx"))
  }



  @Test
  fun `supabase rejects publishable keys as bearer jwt and parses http 520 errors cleanly`() {
    assertFalse(SupabaseAuthService.isJwtToken("sb_publishable_33_wn4dN0Z-HMvTz7es0KQ_PGAcFrWm"))
    assertTrue(SupabaseAuthService.isJwtToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.sig"))
    val err520 = SupabaseAuthService.parseSupabaseError("<html>Cloudflare Error 520</html>", 520)
    assertTrue(err520.contains("520"))
    assertTrue(err520.contains("Supabase", ignoreCase = true))
  }

  @Test
  fun `pev range math estimates series count voltage soc cold derate and sag warning`() {
    assertEquals(16, PevRangeMath.inferSeriesCount(60.0, "NMC"))
    assertEquals(20, PevRangeMath.inferSeriesCount(72.0, "NMC"))
    val fullSoc = PevRangeMath.estimateVoltageSocPct(84.0, 20, "NMC")
    assertEquals(100.0, fullSoc, 0.1)

    val coldSag =
      PevRangeMath.computeRangeProjection(
        batteryWh = 2500.0,
        measuredVoltage = 65.0, // 3.25V/cell on 20S
        nominalVoltage = 72.0,
        chemistry = "NMC",
        cruiseMph = 25.0,
        riderPlusGearLbs = 180.0,
        ambientTempC = -2.0,
        windKmh = 15.0,
        regenPct = 6,
      )
    assertTrue(coldSag.sagWarning)
    assertEquals(26, coldSag.tempDeratePct)
    assertTrue(coldSag.estimatedRangeKm > 0.0)
    assertTrue(coldSag.safeTurnaroundKm < coldSag.estimatedRangeKm)
  }

  @Test
  fun `offline rtb homing beacon computes haversine distance compass bearing and 1_25x turn back badge`() {
    val zero = PevRtbMath.computeRtbVector(36.3729, -94.2088, 36.3729, -94.2088, 90f, 15.0)
    assertEquals(0.0, zero.distanceKm, 0.001)
    assertFalse(zero.turnBackWarning)

    // Rider rode ~11.1 km North of launch base; return bearing must point ~180° South
    val northOut = PevRtbMath.computeRtbVector(36.4729, -94.2088, 36.3729, -94.2088, 0f, 20.0)
    assertEquals(11.12, northOut.distanceKm, 0.25)
    assertEquals(180f, northOut.bearingToBaseDeg, 2.0f)
    assertEquals("S", northOut.cardinal)
    assertFalse(northOut.turnBackWarning) // 20 km > 11.12 * 1.25 (13.9 km)

    // Remaining range drops to 13.0 km (<= 11.12 * 1.25) -> TURN BACK warning triggers
    val lowMargin = PevRtbMath.computeRtbVector(36.4729, -94.2088, 36.3729, -94.2088, 90f, 13.0)
    assertTrue(lowMargin.turnBackWarning)
    assertEquals(90f, lowMargin.relativeArrowDeg, 2.0f)
  }

  @Test
  fun `mission card share util parses polylines and saves bitmap png in win95 and modern chrome`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pts = MissionCardShareUtil.parsePolylinePoints(listOf("36.37,-94.20", "bad", "0.0,0.0", "36.38,-94.21"))
    assertEquals(2, pts.size)

    val ride = RideSession(
      id = "ride_test_95",
      vehicleName = "Kaabo Mantis Pro",
      title = "Sunset Ridge Loop",
      durationSec = 1425L,
      distanceKm = 18.4,
      maxSpeedKmh = 64.0,
      avgSpeedKmh = 38.2,
      elevationGainM = 210.0,
      whUsed = 420.0,
      weatherSummary = "Clear 72F",
      polylinePoints = listOf("36.3710,-94.2010", "36.3750,-94.2050", "36.3790,-94.2020"),
    )

    val win95Settings = MasterSettings(visualStyle = VisualStyleMode.WINDOWS_CLASSIC)
    val win95Res = MissionCardShareUtil.saveMissionCardToCache(context, ride, win95Settings)
    assertTrue(win95Res.isSuccess)
    val win95File = win95Res.getOrThrow()
    assertTrue(win95File.exists() && win95File.length() > 1000L)

    val modernSettings = MasterSettings(visualStyle = VisualStyleMode.DARK_STUDIO)
    val modernBmp = MissionCardShareUtil.renderMissionCardBitmap(ride, modernSettings)
    assertEquals(1080, modernBmp.width)
    assertEquals(620, modernBmp.height)
    modernBmp.recycle()
    win95File.delete()
  }

  @Test
  fun `sos crash detector enforces 8G default, grace period, single-spike immunity, and cooldown`() = runBlocking {
    val defaults = MasterSettings()
    assertEquals(8.0f, defaults.crashSensitivityG, 0.01f)

    // Verify legacy stored 3.8G sensitivity upgrades cleanly to >= 8.0G on import
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = SettingsStore(context)
    store.resetAll()
    val importLegacy = store.importFromJson("""{"crashSensitivityG":3.8}""")
    assertTrue(importLegacy.isSuccess)
    assertEquals(8.0f, store.settingsFlow.first().crashSensitivityG, 0.01f)

    val detector = CrashImpactDetector()
    val startMs = 10_000L

    // 1. Grace period (first 3000ms after ride start) ignores even high spikes
    assertFalse(detector.evaluateSample(true, 10.5f, 8.0f, startMs, startMs + 500L))
    assertFalse(detector.evaluateSample(true, 10.5f, 8.0f, startMs, startMs + 600L))
    assertFalse(detector.evaluateSample(true, 10.5f, 8.0f, startMs, startMs + 700L))

    // 2. Single pothole/phone-mount spike after grace period must NOT trigger SOS
    assertFalse(detector.evaluateSample(true, 9.8f, 8.0f, startMs, startMs + 4_000L))
    assertFalse(detector.evaluateSample(true, 1.2f, 8.0f, startMs, startMs + 4_050L))

    // 3. Three consecutive >= 8.0G samples within 350ms DOES confirm crash
    assertFalse(detector.evaluateSample(true, 8.4f, 8.0f, startMs, startMs + 5_000L))
    assertFalse(detector.evaluateSample(true, 8.9f, 8.0f, startMs, startMs + 5_080L))
    assertTrue(detector.evaluateSample(true, 8.5f, 8.0f, startMs, startMs + 5_160L))

    // 4. Dismiss cooldown prevents immediate re-trigger for 5000ms
    detector.reset(nowMs = startMs + 5_200L, applyCooldown = true)
    assertFalse(detector.evaluateSample(true, 14.0f, 8.0f, startMs, startMs + 6_000L))
    assertFalse(detector.evaluateSample(true, 14.0f, 8.0f, startMs, startMs + 6_060L))

    // 5. After cooldown expires, two consecutive extreme (>= 12.5G) impact samples trigger
    assertFalse(detector.evaluateSample(true, 13.0f, 8.0f, startMs, startMs + 11_000L))
    assertTrue(detector.evaluateSample(true, 13.2f, 8.0f, startMs, startMs + 11_050L))
  }

  @Test
  fun `pev spec catalog supports ebikes escooters euc onewheel emoto eskate others with sorting and voltage filter`() {
    val all = PevCatalogData.BUILT_IN_SPECS
    assertTrue(all.size >= 25)
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "EBIKE").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "SCOOTER").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "EUC").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "ONEWHEEL").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "EMOTO").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "ESKATE").isNotEmpty())
    assertTrue(PevCatalogData.filterSpecCatalog(all, "", "OTHER").isNotEmpty())

    val highVolt = PevCatalogData.filterSpecCatalog(all, "", "ALL", minVoltage = 100)
    assertTrue(highVolt.isNotEmpty())
    assertTrue(highVolt.all { it.nominalVoltage >= 100 })

    val bySpeed = PevCatalogData.filterSpecCatalog(all, "", "ALL", sortMode = PevCatalogData.SpecSortMode.SPEED_DESC)
    assertTrue(PevCatalogData.extractFirstInt(bySpeed.first().topSpeed) >= PevCatalogData.extractFirstInt(bySpeed.last().topSpeed))
  }

  @Test
  fun `pev parts catalog filters by vehicle type part category and search query`() {
    val parts = PevCatalogData.BUILT_IN_PARTS
    assertTrue(parts.size >= 25)
    val scooterTires = PevCatalogData.filterPartCatalog(parts, "PMT", "SCOOTER", "Tires & Tubes")
    assertTrue(scooterTires.isNotEmpty())
    val eucParts = PevCatalogData.filterPartCatalog(parts, "", "EUC", "ALL")
    assertTrue(eucParts.isNotEmpty())
    val ebikeParts = PevCatalogData.filterPartCatalog(parts, "Bafang", "EBIKE", "ALL")
    assertTrue(ebikeParts.isNotEmpty())
  }

  @Test
  fun `strictly pev news filter accepts micromobility stories and blocks ev car only noise`() {
    assertTrue(LiveCloudServices.isStrictlyPevArticle("New 72V electric scooter hits 65 MPH", "Dual hub motor review"))
    assertTrue(LiveCloudServices.isStrictlyPevArticle("InMotion V14 Adventure EUC trail test", "Electric unicycle suspension"))
    assertTrue(LiveCloudServices.isStrictlyPevArticle("Class 2 e-bike law updates in Bentonville", "Throttle bike trails"))
    assertTrue(LiveCloudServices.isStrictlyPevArticle("Sur-Ron Ultra Bee electric dirt bike upgrades", "Controller & battery"))
    assertFalse(LiveCloudServices.isStrictlyPevArticle("Tesla Cybertruck recall affects 40,000 sedans and SUVs", "Ford F-150 Lightning competitor"))
    assertFalse(LiveCloudServices.isStrictlyPevArticle("Rivian R2 SUV factory expansion", "New automotive assembly line"))
  }

  @Test
  fun `weather location geocoding parser extracts city admin and coordinates`() {
    val sampleJson =
      """
      {
        "results": [
          {"name": "Bentonville", "admin1": "Arkansas", "country": "United States", "latitude": 36.3729, "longitude": -94.2088},
          {"name": "Austin", "admin1": "Texas", "country": "United States", "latitude": 30.2672, "longitude": -97.7431}
        ]
      }
      """.trimIndent()
    val locs = LiveCloudServices.parseWeatherLocationsJson(sampleJson)
    assertEquals(2, locs.size)
    assertEquals("Bentonville", locs[0].name)
    assertEquals("Arkansas", locs[0].admin1)
    assertEquals(36.3729, locs[0].lat, 0.001)
    assertEquals(-94.2088, locs[0].lng, 0.001)
  }

  @Test
  fun `supabase metadata backup compactor guarantees valid json within size cap`() {
    val hugeRides = (1..40).joinToString(",") { i ->
      """{"id":"r$i","title":"Long Trail Loop #$i","vehicleName":"Wolf King","distanceKm":42.5,"polylinePoints":["36.37,-94.20","36.38,-94.21"]}"""
    }
    val largeJson =
      """{"version":1,"uid":"u1","exportedAtMs":1730000000000,"settingsJson":"{\"visualStyle\":\"WINDOWS_CLASSIC\"}","vehicles":[{"id":"v1","name":"Kaabo"}],"rides":[$hugeRides]}"""
    val compacted = SupabaseAuthService.compactBackupForAuthMetadata(largeJson)
    assertTrue(compacted.length <= 2350)
    val parsed = JSONObject(compacted)
    assertTrue(parsed.has("vehicles"))
  }

  @Test
  fun `youtube mini player html enforces full-bleed viewport width and height for visible docked video`() {
    val html = buildYoutubeIframeHtml("dQw4w9WgXcQ")
    assertTrue(html.contains("100vw"))
    assertTrue(html.contains("100vh"))
    assertTrue(html.contains("width: 100%"))
  }

  @Test
  fun `ahrs stream processor emits throttled reading off main thread and shuts down cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val processor = AhrsStreamProcessor(context)
    processor.startSampling(pollingHz = 20, uiThrottleMs = 60L)
    processor.calibrateAhrsAllAxes(includeYaw = true)
    val reading = processor.uiThrottledFlow.first()
    assertEquals(20, reading.sampleRateHz)
    assertEquals(0f, reading.pitchDeg, 0.01f)
    assertEquals(0f, reading.rollDeg, 0.01f)
    processor.shutdown()
  }

  @Test
  fun `pev store directory never seeds homepages or google search links and expands multi-vendor platforms`() {
    // Homepage poisoning regression test: Aventon Aventure.2 has siteUrl = "https://www.aventon.com"
    val aventure = PevCatalogData.BUILT_IN_SPECS.first { it.name == "Aventure.2" }
    val aventureLink = PevStoreDirectory.directLinkForSpec(aventure)
    assertFalse(PevStoreDirectory.isHomepageUrl(aventureLink))
    assertFalse(PevStoreDirectory.isGenericGoogleSearchUrl(aventureLink))
    assertTrue(aventureLink.contains("aventon.com/search", ignoreCase = true))

    val aventureCandidates = PevStoreDirectory.candidateStoresForSpec(aventure)
    assertTrue(aventureCandidates.isNotEmpty())
    assertTrue(aventureCandidates.none { PevStoreDirectory.isHomepageUrl(it.second.second) })
    assertTrue(aventureCandidates.none { PevStoreDirectory.isGenericGoogleSearchUrl(it.second.second) })

    // Deep direct URL preservation test: Sherman L has verified product URL on ewheels.com
    val shermanL = PevCatalogData.BUILT_IN_SPECS.first { it.name == "Sherman L" }
    assertEquals("https://www.ewheels.com/product/veteran-sherman-l/", PevStoreDirectory.directLinkForSpec(shermanL))

    // Built-in Part with google.com/search?q= and multi-vendor "eWheels / Voro"
    val pmtPart = PevCatalogData.BUILT_IN_PARTS.first { it.partName.contains("PMT 100/55R6.5") }
    val pmtLink = PevStoreDirectory.directLinkForPart(pmtPart)
    assertFalse(PevStoreDirectory.isGenericGoogleSearchUrl(pmtLink))
    assertTrue(pmtLink.contains("ewheels.com", ignoreCase = true) || pmtLink.contains("voromotors.com", ignoreCase = true))

    val pmtCandidates = PevStoreDirectory.candidateStoresForPart(pmtPart)
    val storeIds = pmtCandidates.map { it.first }
    assertTrue("ewheels" in storeIds)
    assertTrue("voro" in storeIds)
    assertTrue(pmtCandidates.none { PevStoreDirectory.isGenericGoogleSearchUrl(it.second.second) })
  }

  @Test
  fun `live cloud store matcher preserves model suffixes extracts direct product href and parses prices`() {
    val tokens = LiveCloudServices.extractStoreProductTokens("LeaperKim Sherman L")
    assertTrue("leaperkim" in tokens)
    assertTrue("sherman l" in tokens)

    val aventonTokens = LiveCloudServices.extractStoreProductTokens("Aventon Aventure.2")
    assertTrue("aventure.2" in aventonTokens)

    val searchHtml =
      """
      <html><body>
        <a href="/cart">Cart</a>
        <a href="/products/kaabo-sticker-pack">Kaabo Logo Sticker</a>
        <a href="/products/kaabo-wolf-king-gtr">Kaabo Wolf King GTR 72V Electric Scooter</a>
      </body></html>
      """.trimIndent()
    val gtrTokens = LiveCloudServices.extractStoreProductTokens("Kaabo Wolf King GTR")
    val resolved =
      LiveCloudServices.extractDirectProductUrlFromHtml(
        html = searchHtml,
        baseUrl = "https://www.voromotors.com/search?q=Kaabo+Wolf+King+GTR",
        targetTitle = "Kaabo Wolf King GTR",
        tokens = gtrTokens,
      )
    assertEquals("https://www.voromotors.com/products/kaabo-wolf-king-gtr", resolved)

    val priceHtml = """<meta property="og:price:amount" content="3995.00"><span>Buy now</span>"""
    assertEquals("$3,995", LiveCloudServices.extractCleanPriceHint(priceHtml))
    val centsHtml = """<meta property="og:price:amount" content="3995.49"><span>Buy now</span>"""
    assertEquals("$3,995.49", LiveCloudServices.extractCleanPriceHint(centsHtml))
  }

  @Test
  fun `navigation bar suite exposes 12 distinct styles and dynamic mini player offsets`() {
    assertEquals(12, ALL_NAV_BAR_STYLES.size)
    assertEquals(12, ALL_NAV_BAR_STYLES.map { it.id }.distinct().size)
    assertTrue(ALL_NAV_BAR_STYLES.any { it.id == "cyber_deck" })
    assertTrue(ALL_NAV_BAR_STYLES.any { it.id == "rural_timber" })
    assertTrue(ALL_NAV_BAR_STYLES.any { it.id == "classic_taskbar" })

    val railOffset = navBarMiniPlayerBottomOffset("rail", useLeftRail = true, useRightRail = false, useTopRibbon = false, useClassicTaskbar = false)
    val cyberDeckOffset = navBarMiniPlayerBottomOffset("cyber_deck", useLeftRail = false, useRightRail = false, useTopRibbon = false, useClassicTaskbar = false)
    assertTrue(cyberDeckOffset > railOffset)
  }

  @Test
  fun `tactical chrome spec differentiates visual style families and overrides`() {
    val cyber = resolveChromeSpec(MasterSettings(visualStyle = VisualStyleMode.PEV_TACTICAL_NEON))
    assertEquals(TacticalChromeFamily.CYBER_HUD, cyber.family)
    assertTrue(cyber.hasCornerBrackets)
    assertTrue(cyber.uppercaseHeaders)

    val brutalist = resolveChromeSpec(MasterSettings(visualStyle = VisualStyleMode.BRUTALIST_HIGH_VIS))
    assertEquals(TacticalChromeFamily.BRUTALIST, brutalist.family)
    assertTrue(brutalist.hasBrutalistShadow)

    val rural = resolveChromeSpec(MasterSettings(visualStyle = VisualStyleMode.RURAL_BARNWOOD))
    assertEquals(TacticalChromeFamily.RURAL_TIMBER, rural.family)
    assertTrue(rural.hasLeftAccentRail)

    val aero = resolveChromeSpec(MasterSettings(visualStyle = VisualStyleMode.PEV_TITANIUM_PRO))
    assertEquals(TacticalChromeFamily.AERO_METAL, aero.family)
    assertTrue(aero.hasSpecularRim)
  }

  @Test
  fun `hud speed limit resolver honors road limits school zones and legal class caps`() {
    val openStreet = resolveEffectiveHudSpeedLimitMph(
      LiveTelemetrySnapshot(speedLimitMph = 45, inSchoolZone = false),
      MasterSettings(legalComplianceMode = false, schoolZoneStrict = true),
    )
    assertEquals(45, openStreet)

    val schoolZone = resolveEffectiveHudSpeedLimitMph(
      LiveTelemetrySnapshot(speedLimitMph = 35, inSchoolZone = true),
      MasterSettings(legalComplianceMode = false, schoolZoneStrict = true),
    )
    assertEquals(20, schoolZone)

    val class2Cap = resolveEffectiveHudSpeedLimitMph(
      LiveTelemetrySnapshot(speedLimitMph = 35, inSchoolZone = false),
      MasterSettings(legalComplianceMode = true, pevLegalClass = PevLegalClass.CLASS_2_20),
    )
    assertEquals(20, class2Cap)

    val offroadNoRoadLimit = resolveEffectiveHudSpeedLimitMph(
      LiveTelemetrySnapshot(speedLimitMph = null, inSchoolZone = false),
      MasterSettings(legalComplianceMode = true, pevLegalClass = PevLegalClass.OFFROAD_UNLIMITED),
    )
    assertNull(offroadNoRoadLimit)
  }

  @Test
  fun `ar hud includes g force dynamics and clean store query builders strip noise`() {
    assertTrue(ArHudMode.entries.contains(ArHudMode.G_FORCE_DYNAMICS))
    assertEquals("G-Force", ArHudMode.G_FORCE_DYNAMICS.label)

    val shermanL = PevCatalogData.BUILT_IN_SPECS.first { it.name == "Sherman L" }
    val cleanSpecQuery = PevStoreDirectory.buildCleanSpecQuery(shermanL)
    assertTrue(cleanSpecQuery.contains("Sherman L"))
    assertFalse(cleanSpecQuery.contains("/"))

    val pmtPart = PevCatalogData.BUILT_IN_PARTS.first { it.partName.contains("PMT 100/55R6.5") }
    val cleanPartQuery = PevStoreDirectory.buildCleanPartQuery(pmtPart)
    assertTrue(cleanPartQuery.contains("PMT"))
    assertFalse(cleanPartQuery.contains("(Scooter)"))
  }

  @Test
  fun `rider match engine encodes compact specs and decodes round trip and fallback inference`() {
    val encodedRadar = RiderMatchEngine.encodeRadarRigSpec(
      vehicleName = "Veteran Sherman L Long Range Edition Ultra Pack",
      pace = RiderPaceTier.FAST,
      terrain = RiderTerrainPref.GRAVEL,
      usableRangeMi = 58,
      vibe = RiderScheduleVibe.WEEKEND,
    )
    assertTrue(encodedRadar.length <= 60)
    val decodedRadar = RiderMatchEngine.decodeRigMatchSpec(encodedRadar)
    assertEquals(RiderPaceTier.FAST, decodedRadar.paceTier)
    assertEquals(RiderTerrainPref.GRAVEL, decodedRadar.terrainPref)
    assertEquals(58, decodedRadar.usableRangeMi)
    assertEquals(RiderScheduleVibe.WEEKEND, decodedRadar.scheduleVibe)

    val encodedPilot = RiderMatchEngine.encodePilotMatchSummary(
      vehicleName = "Surron Light Bee X Dirt Build",
      batteryWh = 2400L,
      pace = RiderPaceTier.HYPER,
      terrain = RiderTerrainPref.SINGLETRACK,
      usableRangeMi = 45,
      vibe = RiderScheduleVibe.EVENING,
      zipOrTown = "63026 Eureka",
    )
    assertTrue(encodedPilot.length <= 100)
    val decodedPilot = RiderMatchEngine.decodeRigMatchSpec(encodedPilot)
    assertEquals(RiderPaceTier.HYPER, decodedPilot.paceTier)
    assertEquals(RiderTerrainPref.SINGLETRACK, decodedPilot.terrainPref)
    assertEquals(45, decodedPilot.usableRangeMi)
    assertEquals(RiderScheduleVibe.EVENING, decodedPilot.scheduleVibe)
    assertEquals("63026 Eureka", decodedPilot.zipOrTown)

    val inferred = RiderMatchEngine.decodeRigMatchSpec("Surron 72V Dirt Trail 3200Wh", liveSpeedKmh = 68.0, batteryPct = 50L)
    assertEquals(RiderPaceTier.HYPER, inferred.paceTier)
    assertEquals(RiderTerrainPref.SINGLETRACK, inferred.terrainPref)
    assertEquals(50, inferred.usableRangeMi)
  }

  @Test
  fun `rider match engine scores compatibility filters and warns on low pack or short range`() {
    val mySettings = MasterSettings(
      matchPaceTier = RiderPaceTier.CRUISE,
      matchTerrainPref = RiderTerrainPref.GRAVEL,
      matchMinRangeMi = 25,
      matchScheduleVibe = RiderScheduleVibe.WEEKEND,
      matchMinScorePct = 70,
      matchPaceFilter = RiderPaceTier.CRUISE,
      matchTerrainFilter = RiderTerrainPref.GRAVEL,
      matchVibeFilter = RiderScheduleVibe.ANY,
    )

    val greatRig = RiderMatchEngine.encodeRadarRigSpec(
      vehicleName = "Inmotion V14",
      pace = RiderPaceTier.CRUISE,
      terrain = RiderTerrainPref.GRAVEL,
      usableRangeMi = 40,
      vibe = RiderScheduleVibe.WEEKEND,
    )
    val greatRider = RadarRider(
      userId = "sb_11111111-1111-1111-1111-111111111111",
      callsign = "GRAVEL_GOAT",
      vehicleName = greatRig,
      batteryPct = 90L,
    )
    val greatScore = RiderMatchEngine.scoreRiderMatch(mySettings, greatRider)
    assertEquals(100, greatScore.scorePct)
    assertTrue(greatScore.rangeCompatible)
    assertEquals("", greatScore.warningText)
    assertTrue(RiderMatchEngine.matchesActiveFilters(mySettings, RiderMatchEngine.decodeRigMatchSpec(greatRig), greatScore))

    val lowBatRider = greatRider.copy(batteryPct = 18L)
    val lowBatScore = RiderMatchEngine.scoreRiderMatch(mySettings, lowBatRider)
    assertTrue(lowBatScore.warningText.contains("Low pack (18%"))
    assertFalse(lowBatScore.rangeCompatible)

    val shortRangePilot = PublicPilotProfile(
      userId = "sb_22222222-2222-2222-2222-222222222222",
      callsign = "SHORT_HOP",
      vehicleSummary = RiderMatchEngine.encodePilotMatchSummary(
        vehicleName = "Mini Scooter",
        batteryWh = 500L,
        pace = RiderPaceTier.CHILL,
        terrain = RiderTerrainPref.PAVED,
        usableRangeMi = 12,
        vibe = RiderScheduleVibe.COMMUTE,
        zipOrTown = "63026",
      ),
    )
    val pilotScore = RiderMatchEngine.scorePilotMatch(mySettings, shortRangePilot)
    assertTrue(pilotScore.warningText.contains("Range short"))
    assertFalse(RiderMatchEngine.matchesActiveFilters(mySettings, RiderMatchEngine.decodeRigMatchSpec(shortRangePilot.vehicleSummary), pilotScore))
  }

  @Test
  fun `settings store persists and round trips rider match preferences via datastore and json`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = SettingsStore(context)
    store.resetAll()

    store.update {
      it.copy(
        matchPaceTier = RiderPaceTier.HYPER,
        matchTerrainPref = RiderTerrainPref.SINGLETRACK,
        matchMinRangeMi = 42,
        matchScheduleVibe = RiderScheduleVibe.EVENING,
        matchHomeZipOrTown = "65401 Rolla",
        matchMinScorePct = 65,
        matchPaceFilter = RiderPaceTier.FAST,
        matchTerrainFilter = RiderTerrainPref.GRAVEL,
        matchVibeFilter = RiderScheduleVibe.WEEKEND,
      )
    }

    val saved = store.settingsFlow.first()
    assertEquals(RiderPaceTier.HYPER, saved.matchPaceTier)
    assertEquals(RiderTerrainPref.SINGLETRACK, saved.matchTerrainPref)
    assertEquals(42, saved.matchMinRangeMi)
    assertEquals(RiderScheduleVibe.EVENING, saved.matchScheduleVibe)
    assertEquals("65401 Rolla", saved.matchHomeZipOrTown)
    assertEquals(65, saved.matchMinScorePct)
    assertEquals(RiderPaceTier.FAST, saved.matchPaceFilter)
    assertEquals(RiderTerrainPref.GRAVEL, saved.matchTerrainFilter)
    assertEquals(RiderScheduleVibe.WEEKEND, saved.matchVibeFilter)

    val exportedJson = store.exportAsJson()
    store.resetAll()
    assertEquals(RiderPaceTier.CRUISE, store.settingsFlow.first().matchPaceTier)

    val importResult = store.importFromJson(exportedJson)
    assertTrue(importResult.isSuccess)
    val restored = store.settingsFlow.first()
    assertEquals(RiderPaceTier.HYPER, restored.matchPaceTier)
    assertEquals(RiderTerrainPref.SINGLETRACK, restored.matchTerrainPref)
    assertEquals(42, restored.matchMinRangeMi)
    assertEquals(RiderScheduleVibe.EVENING, restored.matchScheduleVibe)
    assertEquals("65401 Rolla", restored.matchHomeZipOrTown)
    assertEquals(65, restored.matchMinScorePct)
    assertEquals(RiderPaceTier.FAST, restored.matchPaceFilter)
    assertEquals(RiderTerrainPref.GRAVEL, restored.matchTerrainFilter)
    assertEquals(RiderScheduleVibe.WEEKEND, restored.matchVibeFilter)
  }

  @Test
  fun `live cloud uid normalization and sunset minutes parser handle valid and blank inputs`() {
    val rawUuid = "123e4567-e89b-12d3-a456-426614174000"
    assertEquals("sb_$rawUuid", LiveCloudServices.toAppSbUid(rawUuid))
    assertEquals("sb_$rawUuid", LiveCloudServices.toAppSbUid("  sb_$rawUuid  "))
    assertEquals("firebaseUidAlpha123", LiveCloudServices.toAppSbUid("firebaseUidAlpha123"))
    assertEquals("", LiveCloudServices.toAppSbUid("   "))

    val now = LocalTime.of(16, 30)
    assertEquals(45, parseSunsetMinutesRemaining("2026-11-23T17:15", now))
    assertEquals(45, parseSunsetMinutesRemaining("5:15 PM", now))
    assertEquals(45, parseSunsetMinutesRemaining("17:15", now))
    assertEquals(-15, parseSunsetMinutesRemaining("16:15", now))
    assertNull(parseSunsetMinutesRemaining("--:--", now))
    assertNull(parseSunsetMinutesRemaining("   ", now))
  }

  @Test
  fun `ai voice profiles each assign unique gemini neural voice and dsp character`() {
    val profiles = AiVoiceProfile.entries
    assertEquals(8, profiles.size)
    assertTrue(profiles.map { it.geminiVoiceName }.distinct().size >= 5)
    assertEquals(profiles.size, profiles.map { it.dspProfile }.distinct().size)
    assertTrue(profiles.all { it.styleDirective.isNotBlank() && it.sampleLine.isNotBlank() })
  }

  @Test
  fun `pev 3d viewer exposes shader modes without still photos and projects hotspots cleanly`() {
    val modeNames = Pev3dRenderMode.entries.map { it.name }
    assertFalse("PHOTO" in modeNames)
    assertTrue("SOLID" in modeNames)
    assertTrue("CARBON_SHOWROOM" in modeNames)
    assertTrue("WIREFRAME" in modeNames)
    assertTrue("XRAY" in modeNames)

    Pev3dArchetype.entries.forEach { arch ->
      val hotspots = Pev3dEngine.projectHotspots(
        archetype = arch,
        widthPx = 1080f,
        heightPx = 800f,
        yawDeg = -28f,
        pitchDeg = 16f,
        zoom = 1.05f,
        explodePct = 0.5f,
      )
      assertEquals(7, hotspots.size)
      assertTrue(hotspots.all { it.screenPos.x.isFinite() && it.screenPos.y.isFinite() && it.depth > 0f })
    }
  }

  @Test
  fun `opengl and webgl pbr texture bundles and gltf2 glb binaries build cleanly for all archetypes`() {
    PevPbrMaterialId.entries.forEach { matId ->
      val bundle = Pev3dPbrTextures.getBundle(matId)
      assertTrue(bundle.albedoPng.size > 100)
      assertTrue(bundle.normalPng.size > 100)
      assertTrue(bundle.metallicRoughnessPng.size > 100)
      // Verify PNG magic signature 0x89 'P' 'N' 'G'
      assertEquals(0x89.toByte(), bundle.normalPng[0])
      assertEquals('P'.code.toByte(), bundle.normalPng[1])
      assertEquals('N'.code.toByte(), bundle.normalPng[2])
      assertEquals('G'.code.toByte(), bundle.normalPng[3])
    }

    val finish = Pev3dEngine.finishForModel("Sur-Ron Ultra Bee eMoto", "Electric Dirt Moped", 74)
    Pev3dArchetype.entries.forEach { arch ->
      val built = Pev3dGltfBuilder.buildVehicleGlb(
        archetype = arch,
        finish = finish,
        lightsOn = true,
        xrayShell = false,
        carbonGlossMode = true,
      )
      assertTrue(built.glbBytes.size > 1024)
      val bb = java.nio.ByteBuffer.wrap(built.glbBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
      assertEquals(0x46546C67, bb.int) // 'glTF'
      assertEquals(2, bb.int) // version 2
      assertEquals(built.glbBytes.size, bb.int)
      val glbJsonChunk = String(built.glbBytes, 20, minOf(built.glbBytes.size - 20, 65536), Charsets.UTF_8)
      assertTrue(glbJsonChunk.contains("TANGENT"))
      assertTrue(glbJsonChunk.contains("occlusionTexture"))
      assertTrue(built.wireGroups.isNotEmpty())
      if (arch != Pev3dArchetype.HYDROFOIL) {
        assertTrue(built.wheelAxleCenters.isNotEmpty())
      }
    }
  }

  @Test
  fun `48v pack defaults to 54_6v and turn signal + optical pbr material sets are complete`() {
    assertEquals(52.0, LiveTelemetrySnapshot().estimatedVoltage, 0.05)
    val nmc48 = PevBatteryPhysics.packProfile(48.0, "NMC")
    assertEquals(13, nmc48.seriesCells)
    assertEquals(54.6, nmc48.fullChargeV, 0.05)

    val signalNames = Pev3dTurnSignalMode.entries.map { it.name }.toSet()
    assertEquals(setOf("OFF", "LEFT", "RIGHT", "HAZARD"), signalNames)

    val matNames = PevPbrMaterialId.entries.map { it.name }.toSet()
    assertTrue("RACING_LIVERY" in matNames)
    assertTrue("HEADLIGHT_OPTIC" in matNames)
    assertTrue("TAILLIGHT_OPTIC" in matNames)
    assertTrue("AMBER_SIGNAL_OPTIC" in matNames)
  }

  @Test
  fun `custom voice profile serialization and active selection work accurately`() {
    val sample = CustomVoiceProfile(
      id = "cv_test_123",
      name = "Appalachian Co-Pilot",
      description = "Warm rural tone",
      pitch = 1.08f,
      speechRate = 0.94f,
      baseProfileId = "warm_rural_porch",
      geminiVoiceName = "Zephyr",
      dspProfile = VoiceDspProfile.WARM_PORCH,
      styleDirective = "Warm hospitable backroads guide",
      audioSampleBase64 = "UklGRi4AAABXQVZFZm10IBAAAAABAAEA",
      audioSampleFileName = "custom_ref.wav",
      audioSampleDurationMs = 4500L,
    )
    val json = serializeCustomVoiceProfiles(listOf(sample))
    assertTrue(json.contains("cv_test_123"))
    assertTrue(json.contains("Appalachian Co-Pilot"))
    assertTrue(json.contains("Zephyr"))

    val parsed = parseCustomVoiceProfiles(json)
    assertEquals(1, parsed.size)
    val loaded = parsed[0]
    assertEquals("cv_test_123", loaded.id)
    assertEquals("Appalachian Co-Pilot", loaded.name)
    assertEquals(1.08f, loaded.pitch, 0.01f)
    assertEquals(0.94f, loaded.speechRate, 0.01f)
    assertEquals(VoiceDspProfile.WARM_PORCH, loaded.dspProfile)
    assertEquals("custom_ref.wav", loaded.audioSampleFileName)
    assertEquals(4500L, loaded.audioSampleDurationMs)

    val settings = MasterSettings(
      customVoiceProfilesJson = json,
      activeCustomVoiceId = "cv_test_123",
      ttsPreloadBufferEnabled = true,
      ttsPreloadBufferDepth = 3,
    )
    val active = settings.getActiveCustomVoice()
    assertNotNull(active)
    assertEquals("Appalachian Co-Pilot", active?.name)
    assertEquals(AiVoiceProfile.WARM_RURAL_PORCH, active?.resolveEffectiveBase())
  }

  @Test
  fun `streaming sentence splitter preserves telemetry decimals and splits punctuation correctly`() {
    val sentences = mutableListOf<String>()
    val splitter = VoiceAudioManager.StreamingSentenceSplitter { sentences.add(it) }

    splitter.feed("Battery is at 72.4V and remaining range is 58.2km today.")
    assertEquals(1, sentences.size)
    assertEquals("Battery is at 72.4V and remaining range is 58.2km today.", sentences[0])

    splitter.feed(" Keep eyes on the curve ahead! Creek crossing is clear.")
    splitter.flush()
    assertEquals(3, sentences.size)
    assertEquals("Keep eyes on the curve ahead!", sentences[1])
    assertEquals("Creek crossing is clear.", sentences[2])
  }

  @Test
  fun `rural-erides-go google services resources match com aistudio volttrail kqrmvx`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("rural-erides-go", context.getString(R.string.project_id))
    assertEquals("1:1030367232900:android:b96563e096b095d6c993d1", context.getString(R.string.google_app_id))
    assertEquals(
      "1030367232900-gqci4dcbsd2fo92m6b2oupupo8sqkvpi.apps.googleusercontent.com",
      context.getString(R.string.default_web_client_id),
    )
    assertEquals("(default)", context.getString(R.string.firestore_database_id))
  }

  @Test
  fun `maps speed limit client parses prefixed zones maxspeed types and statutory fallbacks`() {
    // DE:30 -> 30 km/h -> ~19 mph; FR:zone30 -> ~19 mph; US:urban -> 25 mph
    assertEquals(19, MapsSpeedLimitClient.parseRoadSpeedLimitMph("DE:30", defaultAsKmh = true))
    assertEquals(19, MapsSpeedLimitClient.parseRoadSpeedLimitMph("FR:zone30", defaultAsKmh = true))
    assertEquals(25, MapsSpeedLimitClient.parseRoadSpeedLimitMph("US:urban", defaultAsKmh = true))
    assertEquals(10, MapsSpeedLimitClient.parseMaxSpeedTypeMph("AT:walk"))
    assertEquals(20, MapsSpeedLimitClient.parseMaxSpeedTypeMph("school"))
    assertEquals(25, MapsSpeedLimitClient.estimateStatutorySpeedLimitMph("residential"))
    assertEquals(45, MapsSpeedLimitClient.estimateStatutorySpeedLimitMph("primary"))
    assertNull(MapsSpeedLimitClient.estimateStatutorySpeedLimitMph("unknown_path_class"))
  }

  @Test
  fun `hud style catalog includes 20 huds including oem pev controller displays`() {
    val names = HudStyle.entries.map { it.name }.toSet()
    assertTrue("Expected at least 20 HUD styles, got ${names.size}", names.size >= 20)
    listOf(
      "S866_LCD",
      "TF11_LCD",
      "BAFANG_DPC18",
      "ISINWHEEL_TFT",
      "NINEBOT_MATRIX",
      "SW900_LCD",
      "VSETT_EY3",
    ).forEach { expectedOemHud ->
      assertTrue("Missing OEM HUD style $expectedOemHud", expectedOemHud in names)
    }
  }

  @Test
  fun `room vault persists offline ride vehicle metadata ai debrief updates and deletion`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dao = AppDatabase.get(context).offlineRideDao()
    val uid = "test_vault_uid"
    dao.clearUserRides(uid)

    val ride = OfflineRideEntity(
      id = "ride_vault_001",
      userId = uid,
      vehicleName = "Bafang Ultra Trail",
      title = "Ridge Gravel Sprint",
      durationSec = 940L,
      distanceKm = 14.2,
      maxSpeedKmh = 48.0,
      avgSpeedKmh = 31.5,
      elevationGainM = 185.0,
      whUsed = 310.0,
      polylineCsv = "36.37,-94.20;36.38,-94.21",
      syncedToCloud = false,
      createdAtMs = 1764000000000L,
      vehicleId = "veh_bafang_01",
      startBatteryPct = 92,
      endBatteryPct = 68,
      weatherSummary = "Clear 68F",
      aiDebrief = "",
    )
    dao.upsert(ride)

    val initialList = dao.observeRides(uid).first()
    assertEquals(1, initialList.size)
    assertEquals("veh_bafang_01", initialList[0].vehicleId)
    assertEquals(92, initialList[0].startBatteryPct)
    assertEquals(68, initialList[0].endBatteryPct)
    assertEquals("Clear 68F", initialList[0].weatherSummary)

    dao.updateAiDebrief("ride_vault_001", "Smooth throttle cadence with minimal voltage sag.")
    val updatedList = dao.observeRides(uid).first()
    assertEquals("Smooth throttle cadence with minimal voltage sag.", updatedList[0].aiDebrief)

    dao.deleteRide("ride_vault_001")
    val afterDelete = dao.observeRides(uid).first()
    assertTrue(afterDelete.isEmpty())
  }
}
