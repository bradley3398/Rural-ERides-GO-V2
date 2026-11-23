package com.example

import com.example.core.PevBatteryPhysics
import com.example.data.LiveTelemetrySnapshot
import com.example.ui.screens.Pev3dArchetype
import com.example.ui.screens.Pev3dEngine
import com.example.ui.screens.Pev3dRenderMode
import com.example.ui.screens.Pev3dTurnSignalMode
import com.example.ui.screens.PevPbrMaterialId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pev3dAndVoltageTest {

  @Test
  fun liveTelemetryDefaults48vToExact54_6v() {
    val snap = LiveTelemetrySnapshot()
    assertEquals(54.6, snap.estimatedVoltage, 0.01)

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
}
