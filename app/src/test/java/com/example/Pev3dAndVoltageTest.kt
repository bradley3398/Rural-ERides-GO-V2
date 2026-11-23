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
  fun pev3dArchetypesProvide7DistinctHotspotsAndCustomFinish() {
    val finish = Pev3dEngine.finishForModel("Sur-Ron Ultra Bee eMoto", "Electric Dirt Moped", 74)
    assertEquals(3, finish.bodyRgb.size)
    assertEquals(3, finish.accentRgb.size)
    assertEquals(3, finish.caliperRgb.size)
    Pev3dArchetype.entries.forEach { arch ->
      val hotspots = Pev3dEngine.hotspotsForArchetype(arch)
      assertEquals("Expected 7 hotspots for $arch", 7, hotspots.size)
      assertEquals(7, hotspots.map { it.partKey }.distinct().size)
    }
  }
}
