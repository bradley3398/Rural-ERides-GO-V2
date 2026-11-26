package com.example

import androidx.compose.ui.graphics.Color
import com.example.core.PevBatteryPhysics
import com.example.data.LiveTelemetrySnapshot
import com.example.data.RideSession
import com.example.data.VisualStyleMode
import com.example.ui.screens.Pev3dArchetype
import com.example.ui.screens.Pev3dEngine
import com.example.ui.screens.Pev3dGltfBuilder
import com.example.ui.screens.Pev3dRenderMode
import com.example.ui.screens.Pev3dTechEngine
import com.example.ui.screens.Pev3dTurnSignalMode
import com.example.ui.screens.PevPbrMaterialId
import com.example.ui.screens.buildRideElevationProfile
import com.example.ui.screens.buildRideSpeedProfile
import com.example.ui.screens.computeRideEnergySplit
import com.example.ui.screens.normalizeGeoTrackToUnitBox
import com.example.ui.theme.availableVisualStyles
import com.example.ui.theme.holidayDecorationSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.compartments.Pev3dLabModal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Pev3dAndVoltageTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun liveTelemetryDefaults48vToExact54_6v() {
    val snap = LiveTelemetrySnapshot()
    assertEquals(52.0, snap.estimatedVoltage, 0.01)

    val nmc48 = PevBatteryPhysics.packProfile(48.0, "NMC")
    assertEquals(13, nmc48.seriesCells)
    assertEquals(54.6, nmc48.fullChargeV, 0.01)

    val sag = PevBatteryPhysics.estimateVoltageSagVolts(
      loadAmps = 25.0,
      ratedVoltage = 48.0,
      chemistry = "NMC",
      socPct = 80.0,
    )
    assertTrue("Expected positive voltage sag under 25A load, got $sag", sag > 0.5 && sag < 8.0)
  }

  @Test
  fun pev3dTurnSignalsRenderModesAndOpticalPbrEnumsComplete() {
    val modeNames = Pev3dRenderMode.entries.map { it.name }
    assertFalse("PHOTO" in modeNames)
    assertTrue("SOLID" in modeNames)
    assertTrue("CARBON_SHOWROOM" in modeNames)
    assertTrue("WIREFRAME" in modeNames)
    assertTrue("XRAY" in modeNames)

    val signalNames = Pev3dTurnSignalMode.entries.map { it.name }.toSet()
    assertEquals(setOf("OFF", "LEFT", "RIGHT", "HAZARD"), signalNames)

    val matNames = PevPbrMaterialId.entries.map { it.name }.toSet()
    assertTrue("RACING_LIVERY" in matNames)
    assertTrue("HEADLIGHT_OPTIC" in matNames)
    assertTrue("TAILLIGHT_OPTIC" in matNames)
    assertTrue("AMBER_SIGNAL_OPTIC" in matNames)
  }

  @Test
  fun liveHudPackStateMatchesExactRestingOcvAndUnderLoadSag() {
    // 48V 13S NMC resting at 100% SoC -> exact 54.6V
    val resting48 = PevBatteryPhysics.computeLiveHudPackState(
      ratedVoltage = 48.0,
      packWh = 960.0,
      chemistry = "NMC",
      startBatteryPct = 100,
      tripDistanceKm = 0.0,
      elevationGainM = 0.0,
      speedKmh = 0.0,
      avgSpeedKmh = 0.0,
      pitchDeg = 0.0,
      riderWeightLbs = 175.0,
      vehicleWeightLbs = 50.0,
      rideMode = "Sport",
      ambientTempC = 22.0,
    )
    assertEquals(54.6, resting48.restingVoltageV, 0.02)
    assertEquals(54.6, resting48.underLoadVoltageV, 0.02)
    assertEquals(0.0, resting48.sagVolts, 0.01)
    assertEquals(100, resting48.socPctInt)

    // Under load at 40 km/h on a 4° climb after 10 km + 150m climb
    val climbing48 = PevBatteryPhysics.computeLiveHudPackState(
      ratedVoltage = 48.0,
      packWh = 960.0,
      chemistry = "NMC",
      startBatteryPct = 100,
      tripDistanceKm = 10.0,
      elevationGainM = 150.0,
      speedKmh = 40.0,
      avgSpeedKmh = 32.0,
      pitchDeg = 4.0,
      riderWeightLbs = 175.0,
      vehicleWeightLbs = 50.0,
      rideMode = "Sport",
      ambientTempC = 22.0,
    )
    assertTrue("Expected usedWh > 150 Wh, got ${climbing48.usedWh}", climbing48.usedWh > 150.0)
    assertTrue("Expected SoC to drop below 90%, got ${climbing48.socPctExact}", climbing48.socPctExact < 90.0)
    assertTrue("Expected positive voltage sag under load, got ${climbing48.sagVolts}", climbing48.sagVolts > 0.3)
    assertTrue(
      "Expected under-load voltage (${climbing48.underLoadVoltageV}) < resting voltage (${climbing48.restingVoltageV})",
      climbing48.underLoadVoltageV < climbing48.restingVoltageV,
    )

    // 52V 14S NMC -> 58.8V full; 72V 20S NMC -> 84.0V full
    val resting52 = PevBatteryPhysics.computeLiveHudPackState(
      ratedVoltage = 52.0,
      packWh = 1100.0,
      chemistry = "NMC",
      startBatteryPct = 100,
      tripDistanceKm = 0.0,
      elevationGainM = 0.0,
      speedKmh = 0.0,
      avgSpeedKmh = 0.0,
      riderWeightLbs = 175.0,
      vehicleWeightLbs = 50.0,
      rideMode = "Touring",
    )
    assertEquals(58.8, resting52.underLoadVoltageV, 0.02)

    val resting72 = PevBatteryPhysics.computeLiveHudPackState(
      ratedVoltage = 72.0,
      packWh = 2880.0,
      chemistry = "NMC",
      startBatteryPct = 100,
      tripDistanceKm = 0.0,
      elevationGainM = 0.0,
      speedKmh = 0.0,
      avgSpeedKmh = 0.0,
      riderWeightLbs = 185.0,
      vehicleWeightLbs = 65.0,
      rideMode = "Sport",
    )
    assertEquals(84.0, resting72.underLoadVoltageV, 0.02)
  }

  @Test
  fun pev3dArchetypesProvide7DistinctHotspotsAndCustomFinish() {
    val finish = Pev3dEngine.finishForModel("Sur-Ron Ultra Bee eMoto", "Electric Dirt Moped", 74)
    assertEquals(3, finish.bodyRgb.size)
    assertEquals(3, finish.secondaryRgb.size)
    assertEquals(3, finish.accentRgb.size)
    assertEquals(3, finish.caliperRgb.size)
    assertTrue("Paint metallic should be in [0, 1]", finish.paintMetallic in 0f..1f)
    assertEquals(Pev3dArchetype.EMOTO, Pev3dEngine.archetypeForCategory("Electric Dirt Moped"))
    assertEquals(Pev3dArchetype.EBIKE, Pev3dEngine.archetypeForCategory("Fat-Tire e-Bike"))
    assertEquals(Pev3dArchetype.SCOOTER, Pev3dEngine.archetypeForCategory("Hyper-Scooter"))
    assertEquals(Pev3dArchetype.EUC, Pev3dEngine.archetypeForCategory("Electric Unicycle"))
    assertEquals("emoto.glb", Pev3dEngine.glbAssetFileName(Pev3dArchetype.EMOTO))
    assertEquals("hyperscooter.glb", Pev3dEngine.glbAssetFileName(Pev3dArchetype.SCOOTER))
    assertEquals("ebike_fat.glb", Pev3dEngine.glbAssetFileName(Pev3dArchetype.EBIKE))
    assertEquals("euc.glb", Pev3dEngine.glbAssetFileName(Pev3dArchetype.EUC))
    Pev3dArchetype.entries.forEach { arch ->
      val hotspots = Pev3dEngine.hotspotsForArchetype(arch)
      assertEquals("Expected 7 hotspots for $arch", 7, hotspots.size)
      assertEquals(7, hotspots.map { it.partKey }.distinct().size)
    }
  }

  @Test
  fun pev3dTechEngineUsesStudio3dPbrAndExcludesFilamentAndVulkan() {
    val names = Pev3dTechEngine.entries.map { it.name }
    assertEquals("STUDIO_3D", names.first())
    assertFalse("Vulkan PBR must be completely removed", names.any { it.contains("VULKAN", ignoreCase = true) })
    assertFalse("Filament native engine must be removed", names.any { it.contains("FILAMENT", ignoreCase = true) })
  }

  @Test
  fun pev3dGltfBuilderProducesValidGlbForAllArchetypes() {
    val finish = Pev3dEngine.finishForModel("Sur-Ron Ultra Bee eMoto", "Electric Dirt Moped", 74)
    Pev3dArchetype.entries.forEach { arch ->
      val built = Pev3dGltfBuilder.buildVehicleGlb(
        archetype = arch,
        finish = finish,
        lightsOn = true,
        xrayShell = false,
        carbonGlossMode = true,
      )
      assertTrue("GLB for $arch must be > 4KB, got ${built.glbBytes.size}", built.glbBytes.size > 4096)
      assertEquals('g'.code.toByte(), built.glbBytes[0])
      assertEquals('l'.code.toByte(), built.glbBytes[1])
      assertEquals('T'.code.toByte(), built.glbBytes[2])
      assertEquals('F'.code.toByte(), built.glbBytes[3])
    }
  }

  @Test
  fun rideInspectorTelemetryCurvesAreCrashProofForCorruptInputs() {
    val corruptRide = RideSession(
      id = "corrupt_ride_test",
      title = "",
      vehicleName = "",
      distanceKm = Double.NaN,
      maxSpeedKmh = Double.POSITIVE_INFINITY,
      avgSpeedKmh = -12.0,
      whUsed = Double.NaN,
      elevationGainM = Double.NEGATIVE_INFINITY,
      durationSec = -5L,
      startBatteryPct = 140,
      endBatteryPct = -10,
      polylinePoints = listOf("bad_point", "NaN,NaN", "37.7,-122.4"),
    )
    val speed = buildRideSpeedProfile(corruptRide, "mi")
    val elev = buildRideElevationProfile(corruptRide, "mi")
    val split = computeRideEnergySplit(corruptRide, Color.Cyan, Color.Magenta, Color.Yellow)
    assertTrue(speed.isNotEmpty() && speed.all { it.isFinite() && it >= 0.0 })
    assertTrue(elev.isNotEmpty() && elev.all { it.isFinite() && it >= 0.0 })
    assertTrue(split.isNotEmpty() && split.all { it.value.isFinite() && it.value > 0.0 })

    val norm = normalizeGeoTrackToUnitBox(listOf(Double.NaN to 0.0, 37.7 to -122.4, 37.7 to -122.4))
    assertTrue(norm.all { it.first.isFinite() && it.second.isFinite() })
  }

  @Test
  fun allElevenHolidayThemesAreUnlockedWithOrnaments() {
    val holidayModes = VisualStyleMode.entries.filter { it.name.startsWith("HOLIDAY_") }
    assertEquals(11, holidayModes.size)
    val available = availableVisualStyles().toSet()
    holidayModes.forEach { mode ->
      assertTrue("$mode should be selectable in availableVisualStyles()", mode in available)
      val spec = holidayDecorationSpec(mode)
      assertNotNull("Expected holiday decoration spec for $mode", spec)
      assertTrue("Expected non-blank trim label for $mode", spec!!.trimLabel.isNotBlank())
      assertTrue("Expected non-blank motif title for $mode", spec.motifTitle.isNotBlank())
    }
  }

  @Test
  fun all88ModelsAnd11BlueprintsBuildValidFiniteMeshesWithSafeIndices() {
    for (bp in com.example.ui.screens.Pev3dBlueprintType.entries) {
      val finish = com.example.ui.screens.Pev3dModelFinish(
        bodyRgb = floatArrayOf(0.8f, 0.2f, 0.2f),
        accentRgb = floatArrayOf(0.2f, 0.8f, 0.9f),
        caliperRgb = floatArrayOf(0.9f, 0.1f, 0.1f),
        blueprintType = bp,
      )
      val scene = Pev3dGltfBuilder.buildSceneMeshes(
        archetype = Pev3dArchetype.EMOTO,
        finish = finish,
        lightsOn = true,
      )
      assertTrue("Blueprint $bp must produce parts", scene.parts.isNotEmpty())
      var totalTris = 0
      for (part in scene.parts) {
        val vCount = part.vertices.size / 8
        assertTrue("Part ${part.nodeName} in $bp has vCount=$vCount > 32760", vCount in 1..32760)
        for (f in part.vertices) {
          assertTrue("Non-finite vertex float in $bp / ${part.nodeName}: $f", f.isFinite())
        }
        for (idx in part.indices) {
          val u = idx.toInt() and 0xFFFF
          assertTrue("Index $u out of range [0, $vCount) in $bp / ${part.nodeName}", u in 0 until vCount)
        }
        totalTris += part.indices.size / 3
      }
      assertTrue("Blueprint $bp must have > 100 triangles, got $totalTris", totalTris > 100)
    }
  }

  @Test
  fun pev3dLabModalOpensAndRendersWithoutCrash() {
    var dismissed = false
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      com.example.ui.theme.RuralERidesTheme {
        Pev3dLabModal(
          activeVehicleName = "isinwheel H7 PRO",
          activeVoltage = 48.0,
          gpsLat = 37.7,
          gpsLng = -122.4,
          onDismiss = { dismissed = true },
        )
      }
    }
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.mainClock.advanceTimeBy(64L)
    composeRule.onNodeWithTag("aaa_pev_photo_viewport").assertExists()
    composeRule.onNodeWithTag("exit_3d_lab_btn").performClick()
    composeRule.mainClock.advanceTimeByFrame()
    assertTrue("Expected exit button to dismiss 3D Lab modal", dismissed)
  }
}

