package com.example.core

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Authoritative Personal Electric Vehicle (PEV) battery voltage & electrical physics engine.
 *
 * Eliminates rough linear multipliers and uses exact industry series-cell configurations,
 * non-linear resting OCV discharge curves for NMC 3.7V, high-capacity 21700/18650 (50S/50E) 3.6V,
 * and LiFePO4 (LFP) 3.2V packs, plus SoC/temperature-dependent IR sag and numerical curve
 * integration for real-world Peukert + BMS low-voltage cutoff (LVC) usable capacity.
 */
data class BatteryPackProfile(
  val nominalLabelV: Double,
  val seriesCells: Int,
  val chemistry: String,
  val fullChargeV: Double,
  val nominalPackV: Double,
  val storageV: Double,
  val lowWarningV: Double,
  val cutoffEmptyV: Double,
) {
  val fullCellV: Double get() = fullChargeV / seriesCells.coerceAtLeast(1)
  val emptyCellV: Double get() = cutoffEmptyV / seriesCells.coerceAtLeast(1)
  val seriesCount: Int get() = seriesCells
  val nominalV: Double get() = nominalPackV
  val nominalLabel: String get() = "${if (nominalLabelV % 1.0 == 0.0) nominalLabelV.toInt() else nominalLabelV}V"
}

data class ChargerWattageSpec(
  val activeChargeVolts: Double,
  val activeDcWatts: Double,
  val peakDcWatts: Double,
  val wallAcWatts: Double,
  val wallAmps120v: Double,
)

data class UnderLoadDischargeResult(
  val restingPackV: Double,
  val underLoadPackV: Double,
  val sagVolts: Double,
  val effectivePackIrMohm: Double,
  val cRate: Double,
  val nameplateWh: Double,
  val usableWh: Double,
  val usableAh: Double,
  val peukertRetentionPct: Double,
  val lvcCutoffSocPct: Double,
  val jouleHeatWatts: Double,
  val dischargeEfficiencyPct: Double,
  val hitsLvc: Boolean,
)

data class WireDropResult(
  val awg: Int,
  val copperOhmPer1000Ft: Double,
  val roundTripOhms: Double,
  val voltageDropV: Double,
  val dropPct: Double,
  val heatLossWatts: Double,
  val recommendedConnector: String,
  val isDropWarning: Boolean,
)

data class PackIrDiagnosis(
  val packIrMohm: Double,
  val perCellIrMohm: Double?,
  val verdict: String,
  val isHealthy: Boolean,
)

data class PwmHeadroomResult(
  val restingPackV: Double,
  val underLoadPackV: Double,
  val sagVolts: Double,
  val voltageScaleFactor: Double,
  val freeSpinAtSocMph: Double,
  val safeCeilingMph: Double,
  val estPwmPct: Double,
  val isHighRisk: Boolean,
)

object PevBatteryPhysics {
  const val KM_PER_MILE: Double = 1.609344

  // High-power Li-Ion NMC/NCA (Molicel P45B/P42A, Sony VTC6, Samsung 30Q — 3.7V nominal)
  private val nmcCellCurve: List<Pair<Double, Double>> =
    listOf(
      100.0 to 4.20,
      95.0 to 4.13,
      90.0 to 4.08,
      80.0 to 3.98,
      70.0 to 3.89,
      60.0 to 3.81,
      50.0 to 3.74,
      40.0 to 3.67,
      30.0 to 3.59,
      20.0 to 3.49,
      10.0 to 3.33,
      5.0 to 3.18,
      0.0 to 3.00,
    )

  // High-energy 21700/18650 (Samsung 50S/50E, LG M50LT, Panasonic GA — 3.6V nominal, lower mid-plateau)
  private val nmc36CellCurve: List<Pair<Double, Double>> =
    listOf(
      100.0 to 4.20,
      95.0 to 4.11,
      90.0 to 4.04,
      80.0 to 3.92,
      70.0 to 3.82,
      60.0 to 3.72,
      50.0 to 3.63,
      40.0 to 3.54,
      30.0 to 3.45,
      20.0 to 3.34,
      10.0 to 3.16,
      5.0 to 2.98,
      0.0 to 2.80,
    )

  // Real resting open-circuit voltage (OCV) per cell for LiFePO4 (LFP)
  private val lfpCellCurve: List<Pair<Double, Double>> =
    listOf(
      100.0 to 3.65,
      95.0 to 3.40,
      90.0 to 3.34,
      80.0 to 3.32,
      70.0 to 3.30,
      60.0 to 3.28,
      50.0 to 3.26,
      40.0 to 3.24,
      30.0 to 3.21,
      20.0 to 3.17,
      10.0 to 3.08,
      5.0 to 2.95,
      0.0 to 2.80,
    )

  fun isLfp(chemistry: String): Boolean {
    val c = chemistry.trim().uppercase()
    return c.contains("LFP") || c.contains("LIFEPO") || c.contains("IRON")
  }

  fun isNmc36(chemistry: String): Boolean {
    val c = chemistry.trim().uppercase()
    return c.contains("36") || c.contains("3.6") || c.contains("50S") || c.contains("50E") || c.contains("M50")
  }

  private fun curveForChemistry(chemistry: String): List<Pair<Double, Double>> =
    when {
      isLfp(chemistry) -> lfpCellCurve
      isNmc36(chemistry) -> nmc36CellCurve
      else -> nmcCellCurve
    }

  private fun nominalCellV(chemistry: String): Double =
    when {
      isLfp(chemistry) -> 3.20
      isNmc36(chemistry) -> 3.60
      else -> 3.70
    }

  private fun cutoffCellV(chemistry: String): Double =
    when {
      isLfp(chemistry) -> 2.80
      isNmc36(chemistry) -> 2.80
      else -> 3.00
    }

  private fun canonicalChemLabel(chemistry: String): String =
    when {
      isLfp(chemistry) -> "LFP"
      isNmc36(chemistry) -> "NMC 3.6V"
      else -> "NMC"
    }

  /**
   * Resolves exact series cell count (S) from a vehicle's rated voltage label and chemistry.
   */
  fun resolveSeriesCount(ratedVoltage: Double, chemistry: String = "NMC"): Int {
    val v = ratedVoltage.coerceIn(12.0, 420.0)
    if (isLfp(chemistry)) {
      val knownLfp =
        listOf(
          12.0 to 4,
          12.8 to 4,
          24.0 to 8,
          25.6 to 8,
          36.0 to 12,
          38.4 to 12,
          48.0 to 16,
          51.2 to 16,
          52.0 to 16,
          60.0 to 19,
          64.0 to 20,
          72.0 to 23,
          76.8 to 24,
          96.0 to 30,
        )
      knownLfp.firstOrNull { abs(it.first - v) <= 1.2 }?.let { return it.second }
      return (v / 3.20).roundToInt().coerceIn(4, 120)
    }

    val knownNmc =
      listOf(
        24.0 to 7,
        25.9 to 7,
        29.4 to 7,
        36.0 to 10,
        37.0 to 10,
        42.0 to 10,
        44.0 to 12,
        44.4 to 12,
        46.8 to 13,
        48.0 to 13,
        48.1 to 13,
        50.4 to 12,
        52.0 to 14,
        51.8 to 14,
        54.6 to 13,
        57.6 to 16,
        58.8 to 14,
        60.0 to 16,
        59.2 to 16,
        63.0 to 15,
        64.8 to 18,
        66.6 to 18,
        67.2 to 16,
        72.0 to 20,
        74.0 to 20,
        79.2 to 22,
        84.0 to 20,
        86.4 to 24,
        88.8 to 24,
        93.6 to 26,
        96.0 to 26,
        100.8 to 24,
        100.0 to 24,
        108.0 to 30,
        111.0 to 30,
        113.0 to 27,
        113.4 to 27,
        115.0 to 32,
        115.2 to 32,
        118.4 to 32,
        126.0 to 30,
        129.6 to 36,
        133.0 to 36,
        134.0 to 32,
        134.4 to 32,
        144.0 to 40,
        148.0 to 40,
        151.0 to 36,
        151.2 to 36,
        168.0 to 40,
        360.0 to 96,
      )
    knownNmc.firstOrNull { abs(it.first - v) <= 0.7 }?.let { return it.second }
    return (v / nominalCellV(chemistry)).roundToInt().coerceIn(4, 108)
  }

  /** Returns the complete voltage specification profile for any PEV pack. */
  fun packProfile(ratedVoltage: Double, chemistry: String = "NMC"): BatteryPackProfile {
    val lfp = isLfp(chemistry)
    val s = resolveSeriesCount(ratedVoltage, chemistry)
    val fullV = s * (if (lfp) 3.65 else 4.20)
    val nomV = s * nominalCellV(chemistry)
    val storageV = s * (if (lfp) 3.28 else if (isNmc36(chemistry)) 3.70 else 3.75)
    val lowWarnV = voltageForSeriesSoc(s, 20.0, chemistry)
    val emptyV = s * cutoffCellV(chemistry)
    return BatteryPackProfile(
      nominalLabelV = ratedVoltage,
      seriesCells = s,
      chemistry = canonicalChemLabel(chemistry),
      fullChargeV = round2(fullV),
      nominalPackV = round2(nomV),
      storageV = round2(storageV),
      lowWarningV = round2(lowWarnV),
      cutoffEmptyV = round2(emptyV),
    )
  }

  /** Accurate resting pack voltage for an explicit series cell count [seriesCells] and SoC (0..100%). */
  fun voltageForSeriesSoc(seriesCells: Int, socPct: Double, chemistry: String = "NMC"): Double {
    val s = seriesCells.coerceIn(1, 140)
    return round2(cellVoltageForSoc(socPct, chemistry) * s)
  }

  /** Accurate resting pack voltage for a given State of Charge (0..100%). */
  fun voltageForSoc(ratedVoltage: Double, socPct: Double, chemistry: String = "NMC"): Double {
    val s = resolveSeriesCount(ratedVoltage, chemistry)
    return voltageForSeriesSoc(s, socPct, chemistry)
  }

  fun voltageForSoc(ratedVoltage: Double, socPct: Int, chemistry: String = "NMC"): Double =
    voltageForSoc(ratedVoltage, socPct.toDouble(), chemistry)

  /** Accurate resting per-cell voltage for a given State of Charge (0..100%). */
  fun cellVoltageForSoc(socPct: Double, chemistry: String = "NMC"): Double {
    val pct = socPct.coerceIn(0.0, 100.0)
    val curve = curveForChemistry(chemistry)
    if (pct >= curve.first().first) return curve.first().second
    if (pct <= curve.last().first) return curve.last().second
    for (i in 0 until curve.lastIndex) {
      val (hiPct, hiV) = curve[i]
      val (loPct, loV) = curve[i + 1]
      if (pct <= hiPct && pct >= loPct) {
        val frac = (pct - loPct) / (hiPct - loPct)
        return loV + frac * (hiV - loV)
      }
    }
    return curve.last().second
  }

  /** Inverse lookup: exact State of Charge (0..100%) using an explicit series cell count [seriesCells]. */
  fun socForSeriesVoltage(seriesCells: Int, packVoltage: Double, chemistry: String = "NMC"): Double {
    val s = seriesCells.coerceAtLeast(1)
    val cellV = packVoltage / s.toDouble()
    val curve = curveForChemistry(chemistry)
    if (cellV >= curve.first().second) return 100.0
    if (cellV <= curve.last().second) return 0.0
    for (i in 0 until curve.lastIndex) {
      val (hiPct, hiV) = curve[i]
      val (loPct, loV) = curve[i + 1]
      if (cellV <= hiV && cellV >= loV) {
        val span = (hiV - loV).coerceAtLeast(0.0001)
        val frac = (cellV - loV) / span
        return (loPct + frac * (hiPct - loPct)).coerceIn(0.0, 100.0)
      }
    }
    return 0.0
  }

  /** Inverse lookup: exact State of Charge (0..100%) from measured resting pack voltage. */
  fun socForVoltage(ratedVoltage: Double, packVoltage: Double, chemistry: String = "NMC"): Double =
    socForSeriesVoltage(resolveSeriesCount(ratedVoltage, chemistry), packVoltage, chemistry)

  /**
   * Non-linear cell internal resistance multiplier as a function of SoC and ambient temperature.
   * Lithium-ion impedance increases sharply below ~25% SoC and in sub-15C cold weather.
   */
  fun cellIrMultiplier(socPct: Double, ambientTempC: Double = 22.0, chemistry: String = "NMC"): Double {
    val soc = socPct.coerceIn(0.0, 100.0)
    val socMult =
      if (soc >= 30.0) {
        1.0 + (100.0 - soc) * 0.0020
      } else {
        1.14 + ((30.0 - soc) / 30.0).pow(1.75) * 0.78
      }
    val chemColdScale = if (isLfp(chemistry)) 1.28 else 1.0
    val tempMult =
      if (ambientTempC >= 25.0) {
        (1.0 - (ambientTempC - 25.0) * 0.0038).coerceIn(0.86, 1.0)
      } else {
        val dropDecades = ((25.0 - ambientTempC).coerceAtMost(55.0)) / 10.0
        1.0 + dropDecades.pow(1.45) * 0.24 * chemColdScale
      }
    return (socMult * tempMult).coerceIn(0.85, 5.5)
  }

  /**
   * Smooth cold-weather usable capacity & electrolyte mobility retention factor (0.42..1.0).
   */
  fun coldCapacityFactor(ambientTempC: Double, chemistry: String = "NMC"): Double {
    if (ambientTempC >= 20.0) {
      return if (ambientTempC > 40.0) (1.0 - (ambientTempC - 40.0) * 0.004).coerceIn(0.90, 1.0) else 1.0
    }
    val delta = (20.0 - ambientTempC).coerceIn(0.0, 48.0)
    val slope = if (isLfp(chemistry)) 0.0155 else 0.0118
    return (1.0 - delta * slope).coerceIn(0.42, 1.0)
  }

  /**
   * Numerical 20-step PEV pack discharge simulator integrating the real chemistry OCV curve,
   * SoC + thermal IR voltage sag, Joule I^2*R heat loss, and BMS Low-Voltage Cutoff (LVC).
   */
  fun simulateUnderLoadDischarge(
    seriesCells: Int,
    parallelCells: Double,
    cellCapacityAh: Double,
    loadAmps: Double,
    socPct: Double = 80.0,
    baseCellIrMohm: Double = 22.0,
    ambientTempC: Double = 22.0,
    chemistry: String = "NMC",
    packIrOverrideMohm: Double? = null,
  ): UnderLoadDischargeResult {
    val s = seriesCells.coerceIn(1, 140)
    val p = parallelCells.coerceAtLeast(0.5)
    val cAh = cellCapacityAh.coerceAtLeast(0.5)
    val amps = loadAmps.coerceAtLeast(0.0)
    val soc = socPct.coerceIn(0.0, 100.0)

    val nomPackV = s * nominalCellV(chemistry)
    val cutoffPackV = s * cutoffCellV(chemistry)
    val nameplateAh = p * cAh
    val nameplateWh = nomPackV * nameplateAh

    val basePackMohm = packIrOverrideMohm?.coerceAtLeast(1.0) ?: ((s * baseCellIrMohm.coerceAtLeast(3.0)) / p)
    val irRatioAtCurrent = cellIrMultiplier(soc, ambientTempC, chemistry) / cellIrMultiplier(80.0, 22.0, chemistry)
    val effPackIrMohm =
      if (packIrOverrideMohm != null) basePackMohm * irRatioAtCurrent
      else basePackMohm * cellIrMultiplier(soc, ambientTempC, chemistry)

    val restingV = voltageForSeriesSoc(s, soc, chemistry)
    val sagV = amps * (effPackIrMohm / 1000.0)
    val underLoadV = (restingV - sagV).coerceAtLeast(0.0)
    val cRate = if (nameplateAh > 0.0) amps / nameplateAh else 0.0
    val jouleHeatW = amps * sagV

    // Integrate 100% -> 0% over 20 steps of 5% SoC on the real curve to find Peukert retention & LVC cutoff
    val steps = 20
    val stepFrac = 1.0 / steps
    var deliveredFrac = 0.0
    var lvcHitSoc = 0.0
    var hitLvcYet = false
    val coldCap = coldCapacityFactor(ambientTempC, chemistry)
    val peukertC = 1.0 / (1.0 + 0.048 * (cRate - 0.35).coerceAtLeast(0.0).pow(1.18))

    for (k in 0 until steps) {
      val sHi = 100.0 - k * 5.0
      val sLo = sHi - 5.0
      val sMid = (sHi + sLo) * 0.5
      val ocvMid = voltageForSeriesSoc(s, sMid, chemistry)
      val irMidMult =
        if (packIrOverrideMohm != null) cellIrMultiplier(sMid, ambientTempC, chemistry) / cellIrMultiplier(80.0, 22.0, chemistry)
        else cellIrMultiplier(sMid, ambientTempC, chemistry)
      val stepIrOhms = (basePackMohm * irMidMult) / 1000.0
      val vLoadMid = ocvMid - amps * stepIrOhms
      if (vLoadMid <= cutoffPackV) {
        if (!hitLvcYet) {
          hitLvcYet = true
          lvcHitSoc = sHi
        }
        continue
      }
      val terminalRatio = (vLoadMid / ocvMid.coerceAtLeast(1.0)).coerceIn(0.45, 1.0)
      val curveWeight = (ocvMid / nomPackV.coerceAtLeast(1.0))
      deliveredFrac += stepFrac * curveWeight * terminalRatio
    }

    val retentionPct = (deliveredFrac * peukertC * coldCap * 100.0).coerceIn(15.0, 100.0)
    val usableWh = nameplateWh * (retentionPct / 100.0)
    val usableAh = nameplateAh * ((100.0 - lvcHitSoc) / 100.0) * peukertC * coldCap
    val effPct = if (restingV > 0.1) (underLoadV / restingV * 100.0).coerceIn(0.0, 100.0) else 0.0

    return UnderLoadDischargeResult(
      restingPackV = round2(restingV),
      underLoadPackV = round2(underLoadV),
      sagVolts = round2(sagV),
      effectivePackIrMohm = round2(effPackIrMohm),
      cRate = round2(cRate),
      nameplateWh = round2(nameplateWh),
      usableWh = round2(usableWh),
      usableAh = round2(usableAh),
      peukertRetentionPct = round2(retentionPct),
      lvcCutoffSocPct = round2(lvcHitSoc),
      jouleHeatWatts = round2(jouleHeatW),
      dischargeEfficiencyPct = round2(effPct),
      hitsLvc = underLoadV <= cutoffPackV,
    )
  }

  /**
   * Accurate AWG silicone copper wire + connector drop model.
   * Uses ASTM B3 copper resistance at 20C corrected for operating temperature (alpha = 0.00393/C)
   * plus mated high-current connector contact resistance (0.45 mOhm per pair).
   */
  fun estimateWireVoltageDrop(
    amps: Double,
    oneWayFeet: Double,
    awg: Int,
    systemVolts: Double = 72.0,
    wireTempC: Double = 75.0,
    matedConnectorPairs: Int = 1,
  ): WireDropResult {
    val cleanAmps = amps.coerceAtLeast(0.0)
    val cleanFt = oneWayFeet.coerceAtLeast(0.1)
    val cleanAwg = awg.coerceIn(4, 18)
    val cleanVolts = systemVolts.coerceAtLeast(12.0)

    // Standard ASTM B3 20C Ohms per 1000 ft (rounded from exact gauge table / geometric formula)
    val base20C =
      mapOf(
        4 to 0.2485,
        6 to 0.3951,
        8 to 0.6282,
        10 to 0.9989,
        12 to 1.588,
        14 to 2.525,
        16 to 4.016,
        18 to 6.385,
      )[cleanAwg] ?: (0.0999 * 1.261.pow(cleanAwg.toDouble()))

    val tempScale = 1.0 + 0.00393 * (wireTempC - 20.0)
    val hotOhmPer1000Ft = base20C * tempScale
    val wireRoundTripOhms = (hotOhmPer1000Ft / 1000.0) * (cleanFt * 2.0)
    val connectorOhms = matedConnectorPairs.coerceAtLeast(0) * 0.00045
    val totalOhms = wireRoundTripOhms + connectorOhms
    val vDrop = cleanAmps * totalOhms
    val dropPct = (vDrop / cleanVolts) * 100.0
    val heatW = cleanAmps * cleanAmps * totalOhms

    val plug =
      when {
        cleanAmps <= 30.0 -> "XT30 / XT60 (30A–60A cont)"
        cleanAmps <= 65.0 -> "XT60H / XT90-S Anti-Spark (60A–90A cont)"
        cleanAmps <= 115.0 -> "QS8-S Anti-Spark (110A cont / 180A peak)"
        cleanAmps <= 185.0 -> "QS10-S / AS150 Anti-Spark (180A cont / 300A peak)"
        else -> "QS10-S Dual / Bolted Copper Busbar (>200A)"
      }

    return WireDropResult(
      awg = cleanAwg,
      copperOhmPer1000Ft = round2(hotOhmPer1000Ft),
      roundTripOhms = (totalOhms * 10000.0).roundToInt() / 10000.0,
      voltageDropV = (vDrop * 1000.0).roundToInt() / 1000.0,
      dropPct = round2(dropPct),
      heatLossWatts = round2(heatW),
      recommendedConnector = plug,
      isDropWarning = dropPct > 3.0,
    )
  }

  /**
   * Normalizes measured pack IR into per-cell IR (mOhm) when S & P are provided so a 32S4P pack
   * and a 13S6P pack are accurately judged against real cell impedance limits.
   */
  fun diagnosePackIr(
    vRest: Double,
    vLoad: Double,
    loadAmps: Double,
    seriesCells: Int = 0,
    parallelCells: Int = 0,
    chemistry: String = "NMC",
  ): PackIrDiagnosis {
    val cleanAmps = loadAmps.coerceAtLeast(0.5)
    val packIrMohm = ((vRest - vLoad).coerceAtLeast(0.0) / cleanAmps) * 1000.0
    val perCell =
      if (seriesCells > 0 && parallelCells > 0) {
        (packIrMohm * parallelCells.toDouble()) / seriesCells.toDouble()
      } else null

    val (verdict, healthy) =
      if (perCell != null) {
        val highDrainLimit = if (isLfp(chemistry)) 22.0 else 20.0
        val okLimit = if (isLfp(chemistry)) 38.0 else 35.0
        val wornLimit = if (isLfp(chemistry)) 60.0 else 55.0
        when {
          perCell <= highDrainLimit -> "EXCELLENT LOW-IR CELLS (${perCell.roundToInt()} mΩ/cell)" to true
          perCell <= okLimit -> "HEALTHY PACK (${perCell.roundToInt()} mΩ/cell)" to true
          perCell <= wornLimit -> "MODERATE CELL AGING (${perCell.roundToInt()} mΩ/cell)" to false
          else -> "HIGH IR / WEAK WELDS (${perCell.roundToInt()} mΩ/cell)" to false
        }
      } else {
        when {
          packIrMohm < 90.0 -> "HEALTHY LOW-SAG PACK" to true
          packIrMohm < 160.0 -> "MODERATE WEAR" to false
          else -> "HIGH SAG — CHECK WELDS/CELLS" to false
        }
      }

    return PackIrDiagnosis(
      packIrMohm = round2(packIrMohm),
      perCellIrMohm = perCell?.let { round2(it) },
      verdict = verdict,
      isHealthy = healthy,
    )
  }

  /**
   * EUC / Onewheel PWM cutout headroom using non-linear chemistry OCV minus under-load IR sag
   * normalized against full-charge voltage (back-EMF speed ceiling is strictly proportional to terminal V).
   */
  fun estimatePwmHeadroom(
    freeSpinMph: Double,
    socPct: Double,
    rideMph: Double,
    ratedVoltage: Double = 100.8,
    loadAmps: Double = 45.0,
    parallelCells: Int = 4,
    cellIrMohm: Double = 22.0,
    ambientTempC: Double = 20.0,
    chemistry: String = "NMC",
  ): PwmHeadroomResult {
    val profile = packProfile(ratedVoltage, chemistry)
    val restV = voltageForSeriesSoc(profile.seriesCells, socPct, chemistry)
    val sagV = loadAmps.coerceAtLeast(0.0) *
      ((profile.seriesCells * (cellIrMohm / 1000.0) * cellIrMultiplier(socPct, ambientTempC, chemistry)) / parallelCells.coerceAtLeast(1))
    val loadV = (restV - sagV).coerceAtLeast(profile.cutoffEmptyV * 0.85)
    val vScale = (loadV / profile.fullChargeV.coerceAtLeast(1.0)).coerceIn(0.45, 1.0)
    val freeAtSoc = freeSpinMph.coerceAtLeast(10.0) * vScale
    val safeCeiling = freeAtSoc * 0.78
    val estPwm = ((rideMph.coerceAtLeast(0.0) / freeAtSoc.coerceAtLeast(1.0)) * 100.0).coerceIn(0.0, 120.0)
    return PwmHeadroomResult(
      restingPackV = round2(restV),
      underLoadPackV = round2(loadV),
      sagVolts = round2(sagV),
      voltageScaleFactor = (vScale * 1000.0).roundToInt() / 1000.0,
      freeSpinAtSocMph = round2(freeAtSoc),
      safeCeilingMph = round2(safeCeiling),
      estPwmPct = round2(estPwm),
      isHighRisk = estPwm > 78.0,
    )
  }

  /**
   * Accurate Pack Capacity (Wh) and Amp-Hours (Ah) reconciliation.
   */
  fun computePackWh(nominalVoltage: Double, ampHours: Double, chemistry: String = "NMC"): Double {
    val profile = packProfile(nominalVoltage, chemistry)
    return profile.nominalPackV * ampHours.coerceAtLeast(0.0)
  }

  fun computePackAh(batteryWh: Double, nominalVoltage: Double, chemistry: String = "NMC"): Double {
    val profile = packProfile(nominalVoltage, chemistry)
    return if (profile.nominalPackV > 0.0) batteryWh.coerceAtLeast(0.0) / profile.nominalPackV else 0.0
  }

  /**
   * Realistic CC (Constant Current) + CV (Constant Voltage) charge time in hours.
   */
  fun estimateChargeTimeHours(
    batteryWh: Double,
    ratedVoltage: Double,
    chargerAmps: Double,
    startSocPct: Double = 0.0,
    targetSocPct: Double = 100.0,
    chemistry: String = "NMC",
  ): Double {
    val start = startSocPct.coerceIn(0.0, 100.0)
    val target = targetSocPct.coerceIn(start, 100.0)
    if (target <= start || batteryWh <= 0.0 || chargerAmps <= 0.0) return 0.0
    val ah = computePackAh(batteryWh, ratedVoltage, chemistry)
    if (ah <= 0.0) return 0.0

    val ccEnd = target.coerceAtMost(80.0)
    val ccSpanPct = (ccEnd - start).coerceAtLeast(0.0)
    val cvStart = start.coerceAtLeast(80.0)
    val cvSpanPct = (target - cvStart).coerceAtLeast(0.0)

    val ccHours = (ah * (ccSpanPct / 100.0)) / chargerAmps
    val cvFactor = if (isLfp(chemistry)) 1.35 else 1.75
    val cvHours = ((ah * (cvSpanPct / 100.0)) / chargerAmps) * cvFactor
    return (ccHours + cvHours).coerceAtLeast(0.0)
  }

  /** Instantaneous, peak DC, and AC wall wattage for a CC/CV charger at a given SoC. */
  fun estimateChargerWatts(
    ratedVoltage: Double,
    chargerAmps: Double,
    socPct: Double = 50.0,
    chemistry: String = "NMC",
  ): ChargerWattageSpec {
    val profile = packProfile(ratedVoltage, chemistry)
    val amps = chargerAmps.coerceAtLeast(0.0)
    val activeV = voltageForSeriesSoc(profile.seriesCells, socPct, chemistry).coerceIn(profile.cutoffEmptyV, profile.fullChargeV)
    val activeW = activeV * amps
    val peakW = profile.fullChargeV * amps
    val wallW = peakW / 0.88
    return ChargerWattageSpec(
      activeChargeVolts = round2(activeV),
      activeDcWatts = round2(activeW),
      peakDcWatts = round2(peakW),
      wallAcWatts = round2(wallW),
      wallAmps120v = round2(wallW / 120.0),
    )
  }

  /** Wall-plug electricity cost in USD including ~88% charger conversion efficiency. */
  fun estimateChargeCostUsd(
    batteryWh: Double,
    startSocPct: Double,
    targetSocPct: Double,
    kwhRateUsd: Double,
  ): Double {
    val deltaFrac = ((targetSocPct - startSocPct).coerceIn(0.0, 100.0)) / 100.0
    val wallKwh = (batteryWh.coerceAtLeast(0.0) * deltaFrac / 1000.0) / 0.88
    return wallKwh * kwhRateUsd.coerceAtLeast(0.0)
  }

  /** Under-load voltage sag (V) from pack internal resistance: V_sag = I * R_pack(SoC, Temp). */
  fun estimateVoltageSagVolts(
    loadAmps: Double,
    ratedVoltage: Double,
    parallelCells: Int = 4,
    chemistry: String = "NMC",
    cellInternalResistanceMohm: Double = 22.0,
    socPct: Double = 80.0,
    ambientTempC: Double = 22.0,
  ): Double {
    val s = resolveSeriesCount(ratedVoltage, chemistry)
    val p = parallelCells.coerceAtLeast(1)
    val mult = cellIrMultiplier(socPct, ambientTempC, chemistry)
    val packOhms = (s * (cellInternalResistanceMohm / 1000.0) * mult) / p.toDouble()
    return round2(loadAmps.coerceAtLeast(0.0) * packOhms)
  }

  /** Exact unit-accurate energy efficiency conversions. */
  fun whPerKm(whUsed: Double, distanceKm: Double): Double =
    if (distanceKm > 0.05 && whUsed > 0.0) whUsed / distanceKm else 0.0

  fun whPerMile(whUsed: Double, distanceKm: Double): Double {
    val miles = distanceKm / KM_PER_MILE
    return if (miles > 0.03 && whUsed > 0.0) whUsed / miles else 0.0
  }

  /** Standard 10-step SoC -> Voltage lookup table for the active pack (100% down to 0%). */
  fun socVoltageTable(ratedVoltage: Double, chemistry: String = "NMC"): List<Pair<Int, Double>> =
    listOf(100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0).map { pct ->
      pct to voltageForSoc(ratedVoltage, pct, chemistry)
    }

  private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
