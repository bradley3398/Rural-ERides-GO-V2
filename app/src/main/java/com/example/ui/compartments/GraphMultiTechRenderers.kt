package com.example.ui.compartments

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

enum class GraphAnalysisMethod(val label: String, val shortDesc: String) {
  RAW_SERIES("Raw Series", "Direct sample telemetry & reference guide"),
  SMA_TREND("SMA-3 + OLS Trend", "3-pt smoothing + Ordinary Least Squares slope"),
  ZSCORE_ANOMALY("Z-Score σ Band", "Mean ±1σ envelope & |Z|>1.5σ outliers"),
  DERIVATIVE_DELTA("Δ Rate Delta", "Sample-to-sample velocity / derivative"),
  QUARTILE_CDF("Quartiles + CDF", "P25 / Median / P75 & cumulative trajectory"),
}

fun applicableMethodsFor(chartType: GraphInspectChartType): List<GraphAnalysisMethod> =
  when (chartType) {
    GraphInspectChartType.PIE_DONUT,
    GraphInspectChartType.RADAR_5AXIS -> listOf(GraphAnalysisMethod.RAW_SERIES)
    GraphInspectChartType.SCATTER -> listOf(
      GraphAnalysisMethod.RAW_SERIES,
      GraphAnalysisMethod.ZSCORE_ANOMALY,
    )
    GraphInspectChartType.DISTANCE_ENERGY,
    GraphInspectChartType.HISTOGRAM -> listOf(
      GraphAnalysisMethod.RAW_SERIES,
      GraphAnalysisMethod.SMA_TREND,
      GraphAnalysisMethod.ZSCORE_ANOMALY,
      GraphAnalysisMethod.DERIVATIVE_DELTA,
    )
    GraphInspectChartType.SPEED,
    GraphInspectChartType.ELEVATION,
    GraphInspectChartType.EFFICIENCY_VOLTAGE,
    GraphInspectChartType.POWER_AMPS -> GraphAnalysisMethod.entries
  }

data class GraphStatsSummary(
  val count: Int,
  val min: Double,
  val max: Double,
  val mean: Double,
  val stdDev: Double,
  val p25: Double,
  val median: Double,
  val p75: Double,
  val iqr: Double,
  val olsSlope: Double,
  val olsIntercept: Double,
  val olsTrendValues: List<Double>,
  val sma3Values: List<Double>,
  val zScores: List<Double>,
  val anomalyIndices: Set<Int>,
  val deltas: List<Double>,
  val cumulativeValues: List<Double>,
)

fun computeGraphAnalytics(values: List<Double>): GraphStatsSummary {
  val clean = values.map { if (it.isFinite()) it else 0.0 }
  if (clean.isEmpty()) {
    return GraphStatsSummary(
      count = 0,
      min = 0.0,
      max = 0.0,
      mean = 0.0,
      stdDev = 0.0,
      p25 = 0.0,
      median = 0.0,
      p75 = 0.0,
      iqr = 0.0,
      olsSlope = 0.0,
      olsIntercept = 0.0,
      olsTrendValues = emptyList(),
      sma3Values = emptyList(),
      zScores = emptyList(),
      anomalyIndices = emptySet(),
      deltas = emptyList(),
      cumulativeValues = emptyList(),
    )
  }

  val n = clean.size
  val minV = clean.minOrNull() ?: 0.0
  val maxV = clean.maxOrNull() ?: 0.0
  val meanV = clean.average()
  val variance = clean.sumOf { (it - meanV).pow(2) } / n.coerceAtLeast(1)
  val stdV = sqrt(variance.coerceAtLeast(0.0))

  val sorted = clean.sorted()
  fun percentile(p: Double): Double {
    if (sorted.size == 1) return sorted[0]
    val pos = p * (sorted.size - 1)
    val lo = pos.toInt().coerceIn(0, sorted.lastIndex)
    val hi = (lo + 1).coerceIn(0, sorted.lastIndex)
    val frac = pos - lo
    return sorted[lo] + (sorted[hi] - sorted[lo]) * frac
  }

  val p25 = percentile(0.25)
  val med = percentile(0.50)
  val p75 = percentile(0.75)

  // Ordinary Least Squares (OLS) linear trend: y = slope * i + intercept
  val sumX = (0 until n).sumOf { it.toDouble() }
  val sumY = clean.sum()
  val sumXX = (0 until n).sumOf { it.toDouble() * it }
  val sumXY = clean.mapIndexed { i, y -> i * y }.sum()
  val denom = (n * sumXX - sumX * sumX)
  val slope = if (n > 1 && abs(denom) > 1e-9) (n * sumXY - sumX * sumY) / denom else 0.0
  val intercept = (sumY - slope * sumX) / n.coerceAtLeast(1)
  val olsVals = List(n) { i -> slope * i + intercept }

  // 3-point Simple Moving Average (edge-padded)
  val sma3 = List(n) { i ->
    val start = (i - 1).coerceAtLeast(0)
    val end = (i + 1).coerceAtMost(n - 1)
    var s = 0.0
    var c = 0
    for (k in start..end) {
      s += clean[k]
      c++
    }
    s / c.coerceAtLeast(1)
  }

  val zScores = clean.map { v -> if (stdV > 1e-6) (v - meanV) / stdV else 0.0 }
  val anomalies = zScores.mapIndexedNotNull { i, z -> if (abs(z) > 1.5) i else null }.toSet()

  val deltas = List(n) { i -> if (i == 0) 0.0 else clean[i] - clean[i - 1] }
  var running = 0.0
  val cum = clean.map { v ->
    running += v
    running
  }

  return GraphStatsSummary(
    count = n,
    min = minV,
    max = maxV,
    mean = meanV,
    stdDev = stdV,
    p25 = p25,
    median = med,
    p75 = p75,
    iqr = p75 - p25,
    olsSlope = slope,
    olsIntercept = intercept,
    olsTrendValues = olsVals,
    sma3Values = sma3,
    zScores = zScores,
    anomalyIndices = anomalies,
    deltas = deltas,
    cumulativeValues = cum,
  )
}
