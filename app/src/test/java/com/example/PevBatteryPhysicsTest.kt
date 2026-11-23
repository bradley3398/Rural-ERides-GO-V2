package com.example

import com.example.core.PevBatteryPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PevBatteryPhysicsTest {

  @Test
  fun pack48v_13sNmc_exactVoltageAndSocCurve() {
    val profile = PevBatteryPhysics.packProfile(48.0, "NMC")
    assertEquals(13, profile.seriesCells)
    assertEquals(54.6, profile.fullChargeV, 0.05)
    assertEquals(48.1, profile.nominalPackV, 0.05)
    assertEquals(39.0, profile.cutoffEmptyV, 0.05)
    assertEquals(48.75, profile.storageV, 0.1)

    // 100% must be 54.6V, NOT 51.4V
    assertEquals(54.6, PevBatteryPhysics.voltageForSoc(48.0, 100.0, "NMC"), 0.05)
    assertEquals(39.0, PevBatteryPhysics.voltageForSoc(48.0, 0.0, "NMC"), 0.05)

    // 51.4V on a 48V 13S pack is ~74% SoC (3.954V/cell), strictly NOT 100%
    val socAt514 = PevBatteryPhysics.socForVoltage(48.0, 51.4, "NMC")
    assertTrue("Expected 51.4V on 48V pack to be ~74% SoC, got $socAt514", socAt514 in 72.0..79.0)
  }

  @Test
  fun standardNmcAndEucVoltageProfilesAreExact() {
    val cases =
      listOf(
        Triple(24.0, 7, 29.4),
        Triple(36.0, 10, 42.0),
        Triple(48.0, 13, 54.6),
        Triple(52.0, 14, 58.8),
        Triple(60.0, 16, 67.2),
        Triple(72.0, 20, 84.0),
        Triple(96.0, 26, 109.2),
        Triple(100.8, 24, 100.8),
        Triple(126.0, 30, 126.0),
        Triple(134.4, 32, 134.4),
        Triple(151.2, 36, 151.2),
        Triple(168.0, 40, 168.0),
      )
    cases.forEach { (ratedV, expectedS, expectedFullV) ->
      val p = PevBatteryPhysics.packProfile(ratedV, "NMC")
      assertEquals("Series count for ${ratedV}V", expectedS, p.seriesCells)
      assertEquals("Full charge V for ${ratedV}V", expectedFullV, p.fullChargeV, 0.05)
      assertEquals("Empty cutoff V for ${ratedV}V", expectedS * 3.0, p.cutoffEmptyV, 0.05)
    }
  }

  @Test
  fun lfp48v_16sProfileIsExact() {
    val profile = PevBatteryPhysics.packProfile(48.0, "LFP")
    assertEquals(16, profile.seriesCells)
    assertEquals("LFP", profile.chemistry)
    assertEquals(58.4, profile.fullChargeV, 0.05)
    assertEquals(51.2, profile.nominalPackV, 0.05)
    assertEquals(44.8, profile.cutoffEmptyV, 0.05)
  }

  @Test
  fun socVoltageRoundTripAndEfficiencyArithmetic() {
    for (ratedV in listOf(36.0, 48.0, 52.0, 60.0, 72.0, 134.4)) {
      for (soc in listOf(0.0, 10.0, 25.0, 50.0, 75.0, 90.0, 100.0)) {
        val v = PevBatteryPhysics.voltageForSoc(ratedV, soc, "NMC")
        val backSoc = PevBatteryPhysics.socForVoltage(ratedV, v, "NMC")
        assertEquals("Round-trip SoC for ${ratedV}V @ $soc%", soc, backSoc, 1.5)
      }
    }

    // Efficiency: 320Wh over 16.09344 km (10 miles) = 32 Wh/mi and ~19.88 Wh/km
    val whMi = PevBatteryPhysics.whPerMile(320.0, 16.09344)
    val whKm = PevBatteryPhysics.whPerKm(320.0, 16.09344)
    assertEquals(32.0, whMi, 0.05)
    assertEquals(19.88, whKm, 0.05)
    assertEquals(whMi, whKm * PevBatteryPhysics.KM_PER_MILE, 0.05)
  }

  @Test
  fun ccCvChargeTimeTapersAbove80Percent() {
    // 48V 13S 962Wh (~20Ah) on 4A charger:
    // 20% -> 80% (60% span, pure CC): 12Ah / 4A = 3.0h -> 0.05h per %
    // 80% -> 100% (20% span, CV taper): 4Ah / 4A * 1.75 = 1.75h -> 0.0875h per %
    val ccHours = PevBatteryPhysics.estimateChargeTimeHours(962.0, 48.0, 4.0, 20.0, 80.0, "NMC")
    val cvHours = PevBatteryPhysics.estimateChargeTimeHours(962.0, 48.0, 4.0, 80.0, 100.0, "NMC")
    assertEquals(3.0, ccHours, 0.05)
    assertEquals(1.75, cvHours, 0.05)
    assertTrue("CV taper per % must be slower than CC phase", (cvHours / 20.0) > (ccHours / 60.0))
  }
}
