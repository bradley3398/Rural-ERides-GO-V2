package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.LiveCloudServices
import com.example.data.CopilotMessage
import com.example.data.PevPartItem
import com.example.data.PevSpecRecord
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.data.VisionAnomalyBox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepository"

object GeminiModelCatalog {
  const val GEMINI_3_1_FLASH_LITE = "gemini-3.1-flash-lite"
  const val GEMINI_3_5_FLASH_LITE = "gemini-3.5-flash-lite"
  const val GEMINI_3_8_FLASH = "gemini-3.8-flash"
  const val GEMINI_4_ARGON = "gemini-4-argon"
  const val GEMINI_3_1_FLASH_IMAGE = "gemini-3.1-flash-image"
  const val GEMINI_NANO_BANANA_2_1 = "gemini-nano-banana-2.1"
  const val GEMINI_3_1_FLASH_LITE_IMAGE = "gemini-3.1-flash-lite-image"
  const val GEMINI_3_PRO_IMAGE = "gemini-3-pro-image"

  val availableModels =
    listOf(
      GEMINI_3_1_FLASH_LITE to "Gemini 3.1 Flash-Lite (Free Tier Default)",
      GEMINI_3_5_FLASH_LITE to "Gemini 3.5 Flash-Lite (Ultra Fast)",
      GEMINI_3_8_FLASH to "Gemini 3.8 Flash (Balanced)",
    )

  fun fallbackChain(preferred: String): List<String> {
    val cleanPref =
      preferred.trim().let {
        if (
          it.isBlank() ||
            it.contains("2.5") ||
            it.contains("2.0") ||
            it.contains("1.5") ||
            it.contains("4-argon") ||
            it.contains("3.1-flash-lite-preview")
        ) {
          GEMINI_3_1_FLASH_LITE
        } else {
          it
        }
      }
    return listOf(
      cleanPref,
      GEMINI_3_1_FLASH_LITE,
      GEMINI_3_5_FLASH_LITE,
      GEMINI_3_8_FLASH,
    ).distinct()
  }

  val imageFallbackChain =
    listOf(
      GEMINI_3_1_FLASH_IMAGE,
      GEMINI_NANO_BANANA_2_1,
      GEMINI_3_1_FLASH_LITE_IMAGE,
      GEMINI_3_PRO_IMAGE,
    )
}

data class DailyPrayerResult(
  val verse: String,
  val reference: String,
  val prayer: String,
  val safetyTip: String,
)

data class VisionDiagnosisResult(
  val diagnosisText: String,
  val checklist: List<String>,
  val anomalies: List<VisionAnomalyBox>,
  val wiringMap: Map<String, String>,
)

data class ErrorCodeDiagResult(
  val fault: String,
  val component: String,
  val symptoms: String,
  val voltageCheck: String,
  val fix: String,
  val sourceUrl: String,
)

data class PevAutoSpecResult(
  val canonicalName: String,
  val category: String,
  val voltage: Double,
  val batteryAh: Double,
  val batteryWh: Long,
  val motorWatts: Int,
  val tireSize: String,
  val targetPsi: Double,
  val topSpeedKmh: Double,
  val vehicleWeightLbs: Double,
  val imageBase64: String = "",
)

data class BatteryPredictionResult(
  val predictedRemainingPct: Int,
  val learnedWhPerKm: Double,
  val confidence: String,
  val reasoning: String,
  val ridesAnalyzed: Int,
)

data class PreRideFlightCheckResult(
  val readinessScore: Int,
  val verdict: String,
  val weatherRisk: String,
  val batteryStrategy: String,
  val gearChecklist: List<String>,
  val routeTip: String,
)

data class UpgradeCompatibilityResult(
  val compatibilityScore: Int,
  val verdict: String,
  val voltageAndAmpsCheck: String,
  val thermalAndRangeImpact: String,
  val requiredSupportingMods: List<String>,
  val warnings: List<String>,
)

val ALL_PEV_VEHICLE_CATEGORIES =
  listOf(
    "SCOOTER",
    "EBIKE",
    "EUC",
    "ONEWHEEL",
    "EMOTO",
    "ESKATE",
    "SURRON",
    "EMOPED",
    "ETRIKE",
    "ECARGO",
    "EFATBIKE",
    "EFOLDING",
    "EDIRTBIKE",
    "EGOKART",
    "EATV",
    "OTHER",
  )

class GeminiAiRepository {

  internal fun isRetryableModelFallbackError(t: Throwable): Boolean {
    var cursor: Throwable? = t
    var depth = 0
    while (cursor != null && depth < 8) {
      val clsName = cursor::class.java.simpleName.lowercase()
      val msg = "${cursor.message.orEmpty()} ${cursor.localizedMessage.orEmpty()}".lowercase()
      val isQuotaOrRateLimit =
        clsName.contains("quotaexceeded") ||
          clsName.contains("ratelimit") ||
          msg.contains("429") ||
          msg.contains("resource_exhausted") ||
          msg.contains("resource exhausted") ||
          msg.contains("quota") ||
          msg.contains("rate limit") ||
          msg.contains("rate-limit") ||
          msg.contains("too many requests") ||
          msg.contains("rpd") ||
          msg.contains("rpm")
      val isUnavailableModel =
        msg.contains("not found") ||
          msg.contains("404") ||
          msg.contains("400") ||
          msg.contains("not supported") ||
          msg.contains("unknown model") ||
          (msg.contains("permission") && msg.contains("model")) ||
          (msg.contains("model") && msg.contains("invalid"))
      if (isQuotaOrRateLimit || isUnavailableModel) {
        return true
      }
      cursor = cursor.cause
      depth++
    }
    return false
  }

  private suspend fun <T> withModelFallback(
    preferredModelId: String,
    block: suspend (modelId: String) -> T,
  ): T {
    val chain = GeminiModelCatalog.fallbackChain(preferredModelId)
    var lastError: Throwable? = null
    for ((idx, modelId) in chain.withIndex()) {
      try {
        return block(modelId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastError = e
        if (idx < chain.lastIndex && isRetryableModelFallbackError(e)) {
          Log.w(TAG, "Model $modelId hit quota/429 or unavailable error; rotating to ${chain[idx + 1]}: ${e.message}")
          continue
        }
        throw e
      }
    }
    throw lastError ?: IllegalStateException("No Gemini model available.")
  }

  private fun prayerSchema(): JSONObject =
    GeminiJsonSchema.obj(
      properties =
        mapOf(
          "verse" to GeminiJsonSchema.string("Public domain KJV/WEB/ASV Bible verse text"),
          "reference" to GeminiJsonSchema.string("Book Chapter:Verse (Translation)"),
          "prayer" to GeminiJsonSchema.string("2-3 sentence rider safety protection prayer"),
          "safetyTip" to GeminiJsonSchema.string("Practical pre-ride mechanical or trail safety tip"),
        )
    )

  private fun visionSchema(): JSONObject {
    val anomalySchema =
      GeminiJsonSchema.obj(
        properties =
          mapOf(
            "label" to GeminiJsonSchema.string("Detected hardware part or fault label"),
            "severity" to GeminiJsonSchema.enumeration(listOf("CRITICAL", "WARNING", "ADVISORY")),
            "xNorm" to GeminiJsonSchema.number("Left X normalized 0.0..0.9"),
            "yNorm" to GeminiJsonSchema.number("Top Y normalized 0.0..0.9"),
            "wNorm" to GeminiJsonSchema.number("Width normalized 0.05..0.8"),
            "hNorm" to GeminiJsonSchema.number("Height normalized 0.05..0.8"),
          )
      )
    val pinSchema =
      GeminiJsonSchema.obj(
        properties =
          mapOf(
            "wireColor" to GeminiJsonSchema.string("Wire/Pin color or number"),
            "signalFunction" to GeminiJsonSchema.string("Electrical rail or phase function"),
          )
      )
    return GeminiJsonSchema.obj(
      properties =
        mapOf(
          "diagnosisText" to GeminiJsonSchema.string("Detailed workshop chief technical diagnosis with torque and voltage checks"),
          "checklist" to GeminiJsonSchema.array(GeminiJsonSchema.string(), "Actionable step-by-step repair checklist"),
          "anomalies" to GeminiJsonSchema.array(anomalySchema, "Detected physical faults or target components"),
          "wiringPins" to GeminiJsonSchema.array(pinSchema, "Auto-mapped connector wiring pinouts if visible or requested"),
        ),
      optionalProperties = listOf("anomalies", "wiringPins"),
    )
  }

  private fun errorCodeSchema(): JSONObject =
    GeminiJsonSchema.obj(
      properties =
        mapOf(
          "fault" to GeminiJsonSchema.string("Exact verified technical fault title"),
          "component" to GeminiJsonSchema.string("Specific harness, sensor, MOSFET, or data wire"),
          "symptoms" to GeminiJsonSchema.string("Observable vehicle symptoms"),
          "voltageCheck" to GeminiJsonSchema.string("Multimeter rail test instructions"),
          "fix" to GeminiJsonSchema.string("Step-by-step repair protocol"),
          "sourceUrl" to GeminiJsonSchema.string("Real documentation URL if available, else empty string"),
        )
    )

  private fun specsSchema(): JSONObject {
    val specSchema =
      GeminiJsonSchema.obj(
        properties =
          mapOf(
            "name" to GeminiJsonSchema.string("Exact vehicle model name"),
            "brand" to GeminiJsonSchema.string("Manufacturer brand"),
            "category" to GeminiJsonSchema.string("SCOOTER, EUC, EBIKE, ONEWHEEL, or EMOTO"),
            "price" to GeminiJsonSchema.string("MSRP price string e.g. $2,499"),
            "topSpeed" to GeminiJsonSchema.string("Top speed with units"),
            "range" to GeminiJsonSchema.string("Real-world range with units"),
            "motorPower" to GeminiJsonSchema.string("Nominal / peak wattage"),
            "battery" to GeminiJsonSchema.string("Voltage and Wh capacity"),
            "nominalVoltage" to GeminiJsonSchema.integer("Nominal integer voltage e.g. 60 or 72"),
            "weight" to GeminiJsonSchema.string("Vehicle weight"),
            "chargingTime" to GeminiJsonSchema.string("Standard charge time"),
            "efficiency" to GeminiJsonSchema.string("Wh/mi or Wh/km estimate"),
            "brakingSystem" to GeminiJsonSchema.string("Hydraulic disc / regen / mechanical specs"),
            "suspensionType" to GeminiJsonSchema.string("Fork & rear shock architecture"),
            "tireProfile" to GeminiJsonSchema.string("Tire dimensions & tubeless/tube"),
            "ipRating" to GeminiJsonSchema.string("Water ingress rating e.g. IPX5"),
            "maxLoad" to GeminiJsonSchema.string("Payload limit"),
            "siteUrl" to GeminiJsonSchema.string("Direct official product page or store buy URL (https://...)"),
            "imageUrl" to GeminiJsonSchema.string("Direct HTTPS product photo URL if known from web context, else empty string"),
          )
      )
    return GeminiJsonSchema.obj(
      properties = mapOf("specs" to GeminiJsonSchema.array(specSchema, "Matching real PEV specification records"))
    )
  }

  private fun partsSchema(): JSONObject {
    val partSchema =
      GeminiJsonSchema.obj(
        properties =
          mapOf(
            "partName" to GeminiJsonSchema.string("Specific aftermarket or OEM replacement part"),
            "category" to GeminiJsonSchema.string("Part category"),
            "exactDimensions" to GeminiJsonSchema.string("Exact millimeter/inch sizing, pitch, or bolt pattern"),
            "compatibility" to GeminiJsonSchema.string("Compatible PEV models/voltages"),
            "estimatedPrice" to GeminiJsonSchema.string("USD price string like $44.99"),
            "recommendedBrands" to GeminiJsonSchema.string("Top verified PEV component brands"),
            "sourcePlatform" to GeminiJsonSchema.string("Known PEV store e.g. Voro Motors, FluidFreeRide, Alien Rides, RevRides, eWheels, EUCO, The Float Life, Craft&Ride, Luna Cycle, GritShift, Charged Cycle Works, ChiBatterySystems, Grin Tech, Amazon, eBay, or AliExpress"),
            "partUrl" to GeminiJsonSchema.string("Direct product page or store search URL (https://...)"),
            "imageUrl" to GeminiJsonSchema.string("Direct HTTPS product image URL if known from web context, else empty string"),
            "difficulty" to GeminiJsonSchema.enumeration(listOf("EASY", "MODERATE", "ADVANCED")),
            "partType" to GeminiJsonSchema.enumeration(listOf("OEM", "PERFORMANCE_UPGRADE", "MAINTENANCE")),
            "weightLbs" to GeminiJsonSchema.number("Shipping weight in pounds"),
          )
      )
    return GeminiJsonSchema.obj(
      properties = mapOf("parts" to GeminiJsonSchema.array(partSchema, "Verified PEV hardware & aftermarket components"))
    )
  }

  private fun autoPevSpecSchema(): JSONObject =
    GeminiJsonSchema.obj(
      properties =
        mapOf(
          "canonicalName" to GeminiJsonSchema.string("Official production Make and Model name"),
          "category" to GeminiJsonSchema.enumeration(ALL_PEV_VEHICLE_CATEGORIES),
          "voltage" to GeminiJsonSchema.number("Nominal pack voltage e.g. 36, 48, 52, 60, 72, 84, 100, 126, 134"),
          "batteryAh" to GeminiJsonSchema.number("Battery pack amp-hours e.g. 20.0, 28.0, 32.0, 40.0"),
          "batteryWh" to GeminiJsonSchema.integer("Battery pack watt-hours (voltage * Ah) e.g. 1560, 2160, 2880"),
          "motorWatts" to GeminiJsonSchema.integer("Total nominal or peak motor wattage e.g. 1000, 2000, 4000, 8000"),
          "tireSize" to GeminiJsonSchema.string("Exact stock tire size e.g. 11x3.5, 10x3.0, 20x4.0, 18x3.0"),
          "targetPsi" to GeminiJsonSchema.number("Recommended tire pressure in PSI e.g. 36, 45, 50, 22"),
          "topSpeedKmh" to GeminiJsonSchema.number("Factory rated top speed in km/h"),
          "vehicleWeightLbs" to GeminiJsonSchema.number("Vehicle curb weight in lbs"),
        )
    )

  private fun batteryPredictionSchema(): JSONObject =
    GeminiJsonSchema.obj(
      properties =
        mapOf(
          "predictedRemainingPct" to GeminiJsonSchema.integer("Predicted remaining battery percentage (0..100) at end of ride"),
          "learnedWhPerKm" to GeminiJsonSchema.number("Learned rider efficiency in Wh/km from ride history"),
          "confidence" to GeminiJsonSchema.enumeration(listOf("HIGH", "MEDIUM", "CALIBRATING")),
          "reasoning" to GeminiJsonSchema.string("Concise 1-2 sentence explanation referencing past ride efficiency, elevation, speed, and rider weight"),
        )
    )

  suspend fun fetchDailyPrayer(
    callsign: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<DailyPrayerResult> =
    try {
      val themes =
        listOf(
          "strength",
          "guidance",
          "protection",
          "endurance",
          "peace",
          "courage",
          "wisdom",
          "the journey",
          "faith in the storm",
        )
      val theme = themes.random()
      val seed = (System.currentTimeMillis() % 999999L)
      val prompt =
        """
        You are the Rural ERides GO onboard spiritual & safety companion (Seed: $seed).
        Generate a unique daily protection prayer for an electric PEV/e-bike rider named "$callsign" focused on "$theme".
        Select a real public-domain Bible verse (KJV, WEB, or ASV) on safety, travel, or guidance (avoid Psalm 91:11).
        Provide 1 concise pre-ride mechanical or trail safety tip.
        """
          .trimIndent()

      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(60_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.9f,
              responseMimeType = "application/json",
              responseSchema = prayerSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Empty AI response for Rider's Prayer.")
      val json = JSONObject(raw)
      Result.success(
        DailyPrayerResult(
          verse = "\"${json.optString("verse").trim('"')}\"",
          reference = json.optString("reference", "Proverbs 3:5-6 (KJV)"),
          prayer = json.optString("prayer"),
          safetyTip = json.optString("safetyTip"),
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Prayer AI timed out", e)
      Result.failure(Exception("AI Blessing timed out after 60s. Check network and retry."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Prayer AI error", e)
      Result.failure(e)
    }

  suspend fun diagnoseVision(
    primaryBitmap: Bitmap?,
    secondaryBitmap: Bitmap?,
    query: String,
    fleetInfo: String,
    unitSystem: String,
    scope: String,
    detailLevel: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    historySummary: String = "",
  ): Result<VisionDiagnosisResult> =
    try {
      val historyBlock =
        if (historySummary.isNotBlank()) "Recent workshop conversation:\n$historySummary\n\n" else ""
      val promptText =
        """
        You are the veteran Workshop Chief and PEV master mechanic. Speak straight and concisely.
        Fleet: $fleetInfo | Units: $unitSystem | Detail: $detailLevel | Optical Scan Scope: $scope.
        Examine any attached hardware photos closely for scorched wires, MOSFET damage, rotor wear, tire tread, loose fasteners, or connector corrosion.
        Include torque specs (Nm) and multimeter voltage rail checks.
        Provide an actionable checklist and normalized AR bounding boxes (0..1) if components or anomalies are visible in the photo.
        ${historyBlock}Operator query: ${query.ifBlank { "Full optical PEV hardware inspection" }}
        """
          .trimIndent()

      val attachedImages = listOfNotNull(primaryBitmap, secondaryBitmap)
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(60_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = promptText,
              images = attachedImages,
              temperature = 0.2f,
              responseMimeType = "application/json",
              responseSchema = visionSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Workshop Vision returned empty payload.")
      val json = JSONObject(raw)
      val checklistArr = json.optJSONArray("checklist")
      val checklist =
        if (checklistArr != null) {
          (0 until checklistArr.length()).map { checklistArr.optString(it) }.filter { it.isNotBlank() }
        } else {
          emptyList()
        }

      val anomArr = json.optJSONArray("anomalies")
      val anomalies =
        if (anomArr != null) {
          (0 until anomArr.length()).map { i ->
            val o = anomArr.getJSONObject(i)
            VisionAnomalyBox(
              label = o.optString("label", "Hardware Target"),
              severity = o.optString("severity", "WARNING"),
              xNorm = o.optDouble("xNorm", 0.2).toFloat().coerceIn(0f, 0.85f),
              yNorm = o.optDouble("yNorm", 0.2).toFloat().coerceIn(0f, 0.85f),
              wNorm = o.optDouble("wNorm", 0.3).toFloat().coerceIn(0.08f, 0.85f),
              hNorm = o.optDouble("hNorm", 0.3).toFloat().coerceIn(0.08f, 0.85f),
            )
          }
        } else {
          emptyList()
        }

      val pinArr = json.optJSONArray("wiringPins")
      val wiringMap = mutableMapOf<String, String>()
      if (pinArr != null) {
        for (i in 0 until pinArr.length()) {
          val p = pinArr.optJSONObject(i) ?: continue
          val c = p.optString("wireColor")
          val fn = p.optString("signalFunction")
          if (c.isNotBlank() && fn.isNotBlank()) wiringMap[c] = fn
        }
      }

      Result.success(
        VisionDiagnosisResult(
          diagnosisText = json.optString("diagnosisText"),
          checklist = checklist,
          anomalies = anomalies,
          wiringMap = wiringMap,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Vision AI timed out", e)
      Result.failure(Exception("Vision analysis timed out after 60s."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Vision AI failed", e)
      Result.failure(e)
    }

  suspend fun lookupErrorCode(
    code: String,
    systemBrand: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<ErrorCodeDiagResult> =
    try {
      val webCtx = LiveCloudServices.webSearchGrounding("$systemBrand error code $code electric scooter ebike fix")
      val prompt =
        """
        Extract verified PEV diagnostic telemetry for Error Code "$code" on "$systemBrand".
        ${if (webCtx.isNotBlank()) "Web Documentation Context:\n$webCtx" else ""}
        Return exact fault title, target wire/component, symptoms, multimeter rail test, step-by-step fix, and source URL if present.
        """
          .trimIndent()
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(60_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.2f,
              responseMimeType = "application/json",
              responseSchema = errorCodeSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Error code engine returned empty response.")
      val json = JSONObject(raw)
      Result.success(
        ErrorCodeDiagResult(
          fault = json.optString("fault", "$code Fault"),
          component = json.optString("component", "Controller / Harness"),
          symptoms = json.optString("symptoms"),
          voltageCheck = json.optString("voltageCheck"),
          fix = json.optString("fix"),
          sourceUrl = json.optString("sourceUrl"),
        )
      )
    } catch (e: TimeoutCancellationException) {
      Result.failure(Exception("Error code diagnostic timed out."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error code lookup failed", e)
      Result.failure(e)
    }

  private fun extractVerifiedUrlsFromContext(webCtx: String): Set<String> {
    if (webCtx.isBlank()) return emptySet()
    val regex = Regex("""https?://[^\s\)\]"'<>]+""")
    return regex
      .findAll(webCtx)
      .map { it.value.trimEnd('.', ',', ';', ')') }
      .filter { it.length > 12 }
      .toSet()
  }

  private fun sanitizeGroundedUrl(candidate: String, verifiedUrls: Set<String>): String {
    val clean = candidate.trim().trimEnd('/')
    if (clean.isBlank() || verifiedUrls.isEmpty()) return ""
    val exact =
      verifiedUrls.firstOrNull { v ->
        v.trimEnd('/').equals(clean, ignoreCase = true)
      }
    return if (
      exact != null &&
        !com.example.data.PevStoreDirectory.isHomepageUrl(exact) &&
        !com.example.data.PevStoreDirectory.isGenericGoogleSearchUrl(exact)
    ) {
      "verified::$exact"
    } else ""
  }

  suspend fun searchLivePevSpecs(
    query: String,
    category: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    deepSearch: Boolean = false,
    excludeNames: List<String> = emptyList(),
  ): Result<List<PevSpecRecord>> =
    try {
      val webCtx =
        LiveCloudServices.webSearchGrounding(
          "$query $category electric PEV specs voltage battery top speed range 2024 2025 2026",
          deepMode = deepSearch,
        )
      val verifiedUrls = extractVerifiedUrlsFromContext(webCtx)
      val targetCount = if (deepSearch) 20 else 14
      val excludeBlock =
        if (excludeNames.isNotEmpty()) {
          "DO NOT repeat any of these already-listed models: ${excludeNames.take(35).joinToString(", ")}. Dig deeper and find NEW trims, sub-models, alternative brands, and niche PEV variants!"
        } else ""
      val prompt =
        """
        Provide a DEEP, EXPANDED list of $targetCount REAL, VERIFIED production PEV specifications matching query="$query", category="$category".
        $excludeBlock
        ${if (webCtx.isNotBlank()) "Live Multi-Engine Web Context:\n$webCtx" else ""}
        ACCURACY RULES (CRITICAL):
        1. ONLY include real, currently sold or well-known production PEVs with accurate brand, model name, voltage, Wh, motor watts, top speed, and price.
        2. NEVER invent or guess product page URLs. ONLY fill siteUrl if that EXACT URL appears verbatim in the Live Multi-Engine Web Context above; otherwise set siteUrl to "".
        3. NEVER invent imageUrl — set imageUrl to "" unless an exact direct image URL appears in the context above.
        """
          .trimIndent()
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(65_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.25f,
              responseMimeType = "application/json",
              responseSchema = specsSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Spec engine returned empty payload.")
      val arr = JSONObject(raw).optJSONArray("specs")
      val list = mutableListOf<PevSpecRecord>()
      if (arr != null) {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val nameVal = o.optString("name").trim()
          val brandVal = o.optString("brand").trim()
          if (nameVal.isBlank() || brandVal.isBlank()) continue
          val img = o.optString("imageUrl").trim()
          val safeSiteUrl = sanitizeGroundedUrl(o.optString("siteUrl"), verifiedUrls)
          val safeImg = if (img.startsWith("http") && verifiedUrls.any { it.equals(img, true) }) listOf(img) else emptyList()
          list +=
            PevSpecRecord(
              name = nameVal,
              brand = brandVal,
              category = o.optString("category", "SCOOTER").uppercase(),
              price = o.optString("price"),
              topSpeed = o.optString("topSpeed"),
              range = o.optString("range"),
              motorPower = o.optString("motorPower"),
              battery = o.optString("battery"),
              nominalVoltage = o.optInt("nominalVoltage", 52),
              weight = o.optString("weight"),
              chargingTime = o.optString("chargingTime"),
              efficiency = o.optString("efficiency"),
              brakingSystem = o.optString("brakingSystem"),
              suspensionType = o.optString("suspensionType"),
              tireProfile = o.optString("tireProfile"),
              ipRating = o.optString("ipRating"),
              maxLoad = o.optString("maxLoad"),
              siteUrl = safeSiteUrl,
              imageUrls = safeImg,
            )
        }
      }
      Result.success(list)
    } catch (e: TimeoutCancellationException) {
      Result.failure(Exception("Spec Engine timed out after 65s."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Spec search failed", e)
      Result.failure(e)
    }

  suspend fun searchPevModelCandidates(
    query: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<List<PevAutoSpecResult>> =
    try {
      val cleanQuery = query.trim()
      if (cleanQuery.length < 2) {
        throw IllegalArgumentException("Enter at least 2 characters to search PEV models.")
      }
      val webCtx =
        LiveCloudServices.webSearchGrounding(
          "$cleanQuery electric PEV models trims battery Ah Wh voltage specs",
          deepMode = true,
        )
      val itemSchema =
        GeminiJsonSchema.obj(
          properties =
            mapOf(
              "canonicalName" to GeminiJsonSchema.string("Exact official Make, Model, and Trim/Battery edition"),
              "category" to GeminiJsonSchema.enumeration(ALL_PEV_VEHICLE_CATEGORIES),
              "voltage" to GeminiJsonSchema.number("Nominal pack voltage"),
              "batteryAh" to GeminiJsonSchema.number("Battery pack amp-hours"),
              "batteryWh" to GeminiJsonSchema.integer("Battery pack watt-hours"),
              "motorWatts" to GeminiJsonSchema.integer("Motor wattage"),
              "tireSize" to GeminiJsonSchema.string("Stock tire size"),
              "targetPsi" to GeminiJsonSchema.number("Recommended tire PSI"),
              "topSpeedKmh" to GeminiJsonSchema.number("Top speed km/h"),
              "vehicleWeightLbs" to GeminiJsonSchema.number("Curb weight lbs"),
            )
        )
      val rootSchema =
        GeminiJsonSchema.obj(
          properties = mapOf("candidates" to GeminiJsonSchema.array(itemSchema, "Up to 10 matching PEV models & battery trims"))
        )
      val prompt =
        """
        Dig deep and return 8 to 10 real production PEV models, trims, and battery-pack variants matching "$cleanQuery".
        Include different battery sizes (e.g., Standard vs Pro vs Max vs Long Range) and sister models from the same maker.
        ${if (webCtx.isNotBlank()) "Live Web Context:\n$webCtx" else ""}
        Ensure voltage, batteryAh, and batteryWh are mathematically consistent (batteryWh ≈ voltage * batteryAh).
        """
          .trimIndent()
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(50_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.2f,
              responseMimeType = "application/json",
              responseSchema = rootSchema,
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("No PEV model candidates returned.")
      val arr = JSONObject(raw).optJSONArray("candidates")
      val out = mutableListOf<PevAutoSpecResult>()
      if (arr != null) {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val v = o.optDouble("voltage", 52.0).coerceIn(12.0, 250.0)
          val ah = o.optDouble("batteryAh", 20.0).coerceIn(1.0, 500.0)
          val wh = o.optLong("batteryWh", (v * ah).toLong()).coerceIn(50L, 15000L)
          val cat = o.optString("category", "SCOOTER").uppercase().let { if (it in ALL_PEV_VEHICLE_CATEGORIES) it else "SCOOTER" }
          val cname = o.optString("canonicalName").trim()
          if (cname.isNotBlank()) {
            out +=
              PevAutoSpecResult(
                canonicalName = cname,
                category = cat,
                voltage = v,
                batteryAh = ah,
                batteryWh = wh,
                motorWatts = o.optInt("motorWatts", 1000).coerceIn(0, 50000),
                tireSize = o.optString("tireSize", "10x3.0").ifBlank { "10x3.0" },
                targetPsi = o.optDouble("targetPsi", 36.0).coerceIn(5.0, 100.0),
                topSpeedKmh = o.optDouble("topSpeedKmh", 45.0).coerceIn(0.0, 250.0),
                vehicleWeightLbs = o.optDouble("vehicleWeightLbs", 65.0).coerceIn(0.0, 1000.0),
                imageBase64 = "",
              )
          }
        }
      }
      Result.success(out.distinctBy { it.canonicalName.lowercase() })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Deep PEV candidate search failed", e)
      Result.failure(e)
    }

  suspend fun autoLookupVehicleSpecs(
    makeAndModel: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<PevAutoSpecResult> =
    try {
      val cleanQuery = makeAndModel.trim()
      if (cleanQuery.length < 3) {
        throw IllegalArgumentException("Enter a PEV make and model first.")
      }
      val webCtx = LiveCloudServices.webSearchGrounding("$cleanQuery PEV electric scooter ebike EUC battery voltage Ah Wh tire size specs")
      val prompt =
        """
        Look up the exact factory technical specifications for the personal electric vehicle (PEV): "$cleanQuery".
        ${if (webCtx.isNotBlank()) "Live Web Context:\n$webCtx" else ""}
        Return accurate nominal voltage (V), battery Ah, battery Wh (consistent with voltage * Ah), motor wattage, stock tire size, recommended tire PSI, top speed (km/h), curb weight (lbs), and PEV category.
        """
          .trimIndent()
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(45_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.15f,
              responseMimeType = "application/json",
              responseSchema = autoPevSpecSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Auto-spec lookup returned empty response.")
      val o = JSONObject(raw)
      val volt = o.optDouble("voltage", 52.0).coerceIn(12.0, 250.0)
      val rawAh = o.optDouble("batteryAh", 0.0).coerceIn(0.0, 500.0)
      val rawWh = o.optLong("batteryWh", 0L).coerceIn(0L, 15000L)
      val finalWh =
        when {
          rawWh in 50L..15000L -> rawWh
          rawAh > 1.0 -> (volt * rawAh).toLong().coerceIn(50L, 15000L)
          else -> 1200L
        }
      val finalAh =
        when {
          rawAh >= 1.0 -> rawAh
          volt > 0.0 -> (finalWh.toDouble() / volt).coerceIn(1.0, 500.0)
          else -> 20.0
        }
      val canonicalName = o.optString("canonicalName", cleanQuery).ifBlank { cleanQuery }
      val fetchedImg =
        LiveCloudServices.fetchPevProductImageBase64(canonicalName).ifBlank {
          if (canonicalName != cleanQuery) LiveCloudServices.fetchPevProductImageBase64(cleanQuery) else ""
        }
      Result.success(
        PevAutoSpecResult(
          canonicalName = canonicalName,
          category = o.optString("category", "SCOOTER").uppercase().let {
            if (it in ALL_PEV_VEHICLE_CATEGORIES) it else "SCOOTER"
          },
          voltage = volt,
          batteryAh = finalAh,
          batteryWh = finalWh,
          motorWatts = o.optInt("motorWatts", 1000).coerceIn(0, 50000),
          tireSize = o.optString("tireSize", "10x3.0").ifBlank { "10x3.0" },
          targetPsi = o.optDouble("targetPsi", 36.0).coerceIn(5.0, 100.0),
          topSpeedKmh = o.optDouble("topSpeedKmh", 45.0).coerceIn(0.0, 250.0),
          vehicleWeightLbs = o.optDouble("vehicleWeightLbs", 65.0).coerceIn(0.0, 1000.0),
          imageBase64 = fetchedImg,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Result.failure(Exception("Spec auto-lookup timed out. Tap Auto-Fetch to retry."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Auto-spec lookup failed", e)
      Result.failure(e)
    }

  suspend fun predictRemainingBatteryFromHistory(
    rides: List<RideSession>,
    vehicle: PevVehicle?,
    startBatteryPct: Int,
    currentTripKm: Double,
    currentElevGainM: Double,
    avgSpeedKmh: Double,
    riderWeightLbs: Double,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<BatteryPredictionResult> =
    try {
      val packWh = (vehicle?.batteryWh ?: 1200L).toDouble().coerceAtLeast(200.0)
      val vehicleRides =
        rides
          .filter { r ->
            r.distanceKm >= 0.25 &&
              (vehicle == null || r.vehicleId == vehicle.id || r.vehicleName.equals(vehicle.name, ignoreCase = true))
          }
          .ifEmpty { rides.filter { it.distanceKm >= 0.25 } }
          .take(15)

      val historySummary =
        if (vehicleRides.isEmpty()) {
          "No prior ride records yet; use PEV physics baseline."
        } else {
          vehicleRides.joinToString("\n") { r ->
            val batDelta =
              if (r.startBatteryPct >= 0 && r.endBatteryPct >= 0) {
                "Bat ${r.startBatteryPct}%->${r.endBatteryPct}% (used ${(r.startBatteryPct - r.endBatteryPct).coerceAtLeast(0)}%)"
              } else {
                "EstWh=${"%.0f".format(r.whUsed)}Wh"
              }
            "- ${r.vehicleName}: ${"%.2f".format(r.distanceKm)}km, avg=${"%.1f".format(r.avgSpeedKmh)}km/h, elev=+${"%.0f".format(r.elevationGainM)}m, whUsed=${"%.0f".format(r.whUsed)}, $batDelta"
          }
        }

      val prompt =
        """
        You are an AI PEV Battery Telemetry Learning Engine.
        Learn the rider's real-world energy consumption (Wh/km) from their historical ride records below, then predict the exact remaining battery percentage (0..100) for the ride they are finishing right now.

        ACTIVE VEHICLE: ${vehicle?.name ?: "PEV Unit"} (${vehicle?.category ?: "SCOOTER"}) | Pack=${packWh.toInt()}Wh (${vehicle?.voltage?.toInt() ?: 60}V) | CurbWeight=${vehicle?.vehicleWeightLbs?.toInt() ?: 65}lbs | Motor=${vehicle?.motorWatts ?: 1500}W
        RIDER WEIGHT: ${riderWeightLbs.toInt()} lbs
        CURRENT SESSION FINISHING NOW:
        - Started at: $startBatteryPct%
        - Distance ridden: ${"%.2f".format(currentTripKm)} km
        - Elevation climbed: ${"%.0f".format(currentElevGainM)} m
        - Average speed: ${"%.1f".format(avgSpeedKmh)} km/h

        PAST RIDE RECORDS (${vehicleRides.size} rides analyzed):
        $historySummary

        Return predictedRemainingPct (must be <= $startBatteryPct and >= 0), learnedWhPerKm, confidence, and a concise 1-2 sentence explanation.
        """
          .trimIndent()

      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.15f,
              responseMimeType = "application/json",
              responseSchema = batteryPredictionSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Empty battery prediction response.")
      val o = JSONObject(raw)
      val predicted = o.optInt("predictedRemainingPct", startBatteryPct).coerceIn(0, startBatteryPct.coerceIn(0, 100))
      val whPerKm = o.optDouble("learnedWhPerKm", 24.0).coerceIn(4.0, 180.0)
      Result.success(
        BatteryPredictionResult(
          predictedRemainingPct = predicted,
          learnedWhPerKm = whPerKm,
          confidence = o.optString("confidence", if (vehicleRides.size >= 3) "HIGH" else "CALIBRATING"),
          reasoning = o.optString("reasoning", "Learned from ${vehicleRides.size} past ride records."),
          ridesAnalyzed = vehicleRides.size,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "AI battery prediction failed: ${e.message}")
      Result.failure(e)
    }

  suspend fun searchLiveParts(
    query: String,
    make: String,
    model: String,
    tireSize: String,
    category: String,
    region: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    deepSearch: Boolean = false,
    excludeNames: List<String> = emptyList(),
  ): Result<List<PevPartItem>> =
    try {
      val searchTerms = listOf(make, model, tireSize, category, query).filter { it.isNotBlank() }.joinToString(" ")
      val webCtx =
        LiveCloudServices.webSearchGrounding(
          "$searchTerms PEV ebike scooter replacement upgrade parts amazon ebay walmart",
          deepMode = deepSearch,
        )
      val verifiedUrls = extractVerifiedUrlsFromContext(webCtx)
      val targetCount = if (deepSearch) 20 else 16
      val excludeBlock =
        if (excludeNames.isNotEmpty()) {
          "DO NOT repeat any of these already-found parts: ${excludeNames.take(35).joinToString(", ")}. Dig deeper for additional OEM, budget, pro-grade, and specialty aftermarket parts!"
        } else ""
      val prompt =
        """
        Return a DEEP, EXPANDED list of $targetCount REAL, ACCURATE replacement & upgrade PEV hardware parts for:
        Query: "$query" | Make: "$make" | Model: "$model" | Tire Size: "$tireSize" | Category: "$category" | Region: "$region".
        $excludeBlock
        ${if (webCtx.isNotBlank()) "Live Multi-Engine Web Grounding:\n$webCtx" else ""}
        ACCURACY RULES (CRITICAL):
        1. Use exact real product names, millimeter dimensions, compatible PEV models/voltages, real USD price estimates, and authentic vendor names in sourcePlatform (Amazon, eBay, Walmart, AliExpress, Voro Motors, FluidFreeRide, Alien Rides, RevRides, eWheels, EUCO, Minimotors USA, The Float Life, Craft&Ride, Luna Cycle, GritShift, Charged Cycle Works, ChiBatterySystems, Grin Technologies, Flipsky VESC, Spintend Ubox, RevZilla, Jenson USA).
        2. NEVER invent or guess Amazon /dp/ ASINs, eBay /itm/ IDs, or store product slugs. ONLY fill partUrl if that EXACT URL appears verbatim in Live Multi-Engine Web Grounding above; otherwise set partUrl to "" so the app opens an exact verified store search.
        3. Set imageUrl to "" unless an exact verified image URL appears in the grounding context above.
        """
          .trimIndent()
      val response =
        withModelFallback(modelId) { activeModel ->
          withTimeout(65_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.25f,
              responseMimeType = "application/json",
              responseSchema = partsSchema(),
            )
          }
        }
      val raw = DirectGeminiRestClient.cleanJsonPayload(response.text)
      if (raw.isBlank()) error("Parts Finder returned empty payload.")
      val arr = JSONObject(raw).optJSONArray("parts")
      val list = mutableListOf<PevPartItem>()
      if (arr != null) {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val pName = o.optString("partName").trim()
          if (pName.isBlank()) continue
          val safeUrl = sanitizeGroundedUrl(o.optString("partUrl", ""), verifiedUrls)
          val rawImg = o.optString("imageUrl", "").trim()
          val safeImg = if (rawImg.startsWith("http") && verifiedUrls.any { it.equals(rawImg, true) }) rawImg else ""
          list +=
            PevPartItem(
              partName = pName,
              category = o.optString("category"),
              exactDimensions = o.optString("exactDimensions"),
              compatibility = o.optString("compatibility"),
              estimatedPrice = o.optString("estimatedPrice", "$29.99"),
              recommendedBrands = o.optString("recommendedBrands"),
              sourcePlatform = o.optString("sourcePlatform", "Amazon"),
              partUrl = safeUrl,
              difficulty = o.optString("difficulty", "MODERATE"),
              partType = o.optString("partType", "PERFORMANCE_UPGRADE"),
              weightLbs = o.optDouble("weightLbs", 1.5),
              imageUrl = safeImg,
            )
        }
      }
      Result.success(list)
    } catch (e: TimeoutCancellationException) {
      Result.failure(Exception("Parts Finder timed out after 65s."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Parts Finder failed", e)
      Result.failure(e)
    }

  fun streamCopilotResponse(
    history: List<CopilotMessage>,
    prompt: String,
    bitmap: Bitmap?,
    personaPrompt: String,
    telemetryContext: String,
    weatherContext: String,
    webContext: String,
    verbosity: String,
    customDirective: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    temperature: Float = 0.7f,
    thinkingLevel: String = "medium",
    detailLevel: String = "standard",
  ): Flow<String> = flow {
    val fullPrompt =
      """
      $personaPrompt
      Reasoning Depth: $thinkingLevel | Detail Level: $detailLevel | Verbosity: $verbosity.
      ${if (customDirective.isNotBlank()) "Rider Custom Spec Directive: $customDirective" else ""}
      AUDIO COMPLIANCE: Write smooth conversational text (avoid excessive markdown asterisks).
      HUD WIDGET TAGS: Include [WIDGET: TELEMETRY] if discussing live speed/battery/trip stats, [WIDGET: WEATHER] if discussing weather/wind/rain, or [WIDGET: SOS] if generating emergency distress info.

      LIVE TELEMETRY: $telemetryContext
      LIVE WEATHER: $weatherContext
      ${if (webContext.isNotBlank()) "LIVE WEB DATA:\n$webContext" else ""}
      RECENT CONVERSATION:
      ${history.takeLast(6).joinToString("\n") { (if (it.isUser) "Pilot: " else "AI: ") + it.text.take(300) }}

      CURRENT PILOT REQUEST: $prompt
      """
        .trimIndent()

    val chain = GeminiModelCatalog.fallbackChain(modelId)
    var succeeded = false
    var lastErr: Throwable? = null
    for ((idx, activeModel) in chain.withIndex()) {
      val sb = StringBuilder()
      try {
        sb.clear()
        withTimeout(60_000L) {
          DirectGeminiRestClient.streamGenerateContent(
            modelId = activeModel,
            prompt = fullPrompt,
            images = listOfNotNull(bitmap),
            temperature = temperature.coerceIn(0.1f, 1.4f),
          ).collect { part ->
            if (part.isNotEmpty()) {
              sb.append(part)
              emit(sb.toString())
            }
          }
        }
        if (sb.isNotEmpty()) {
          succeeded = true
          break
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastErr = e
        if (sb.isEmpty() && idx < chain.lastIndex && isRetryableModelFallbackError(e)) {
          Log.w(TAG, "Stream quota/429 or unavailable fallback from $activeModel to ${chain[idx + 1]}: ${e.message}")
          continue
        }
        throw e
      }
    }
    if (!succeeded) {
      throw lastErr ?: IllegalStateException("AI Co-Pilot returned an empty stream.")
    }
  }

  suspend fun generateRideDebrief(
    ride: RideSession,
    weatherSummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    vehicle: PevVehicle? = null,
    batteryChemistry: String = "NMC",
  ): Result<String> =
    try {
      val distMi = ride.distanceKm * 0.621371
      val maxMph = ride.maxSpeedKmh * 0.621371
      val avgMph = ride.avgSpeedKmh * 0.621371
      val whPerKm = com.example.core.PevBatteryPhysics.whPerKm(ride.whUsed, ride.distanceKm)
      val whPerMi = com.example.core.PevBatteryPhysics.whPerMile(ride.whUsed, ride.distanceKm)
      val nomV = vehicle?.voltage ?: 48.0
      val profile = com.example.core.PevBatteryPhysics.packProfile(nomV, batteryChemistry)
      val startV =
        if (ride.startBatteryPct >= 0) com.example.core.PevBatteryPhysics.voltageForSoc(nomV, ride.startBatteryPct.toDouble(), batteryChemistry)
        else profile.fullChargeV
      val endV =
        if (ride.endBatteryPct >= 0) com.example.core.PevBatteryPhysics.voltageForSoc(nomV, ride.endBatteryPct.toDouble(), batteryChemistry)
        else com.example.core.PevBatteryPhysics.voltageForSoc(nomV, 75.0, batteryChemistry)
      val endCellV = endV / profile.seriesCount
      val packWh = (vehicle?.batteryWh ?: 1200L).toDouble().coerceAtLeast(200.0)
      val remainingWh = if (ride.endBatteryPct >= 0) packWh * (ride.endBatteryPct / 100.0) else (packWh - ride.whUsed).coerceAtLeast(0.0)
      val estRemainMi = if (whPerMi > 4.0) remainingWh / whPerMi else (remainingWh / 32.0)

      val prompt =
        """
        You are the Rural ERides GO Ridge-to-Ridge PEV Performance Coach & Battery Engineer.
        Write a crisp, friendly, data-rich rural PEV ride debrief (4 short bulleted sections, plain text, no heavy markdown):
        1) PACE & TERRAIN: Evaluate distance, avg vs max speed, and climb load.
        2) PACK VOLTS & EFFICIENCY: Interpret exact pack voltage drop (${"%.1f".format(startV)}V -> ${"%.1f".format(endV)}V on ${profile.seriesCount}S ${profile.chemistry}, ${"%.2f".format(endCellV)}V/cell) and burn rate (${"%.1f".format(whPerMi)} Wh/mi | ${"%.1f".format(whPerKm)} Wh/km).
        3) BATTERY CARE TIP: Practical advice on charging, storage voltage (${"%.1f".format(profile.storageV)}V), or cell balancing.
        4) NEXT-RIDE RANGE FORECAST: Remaining usable reserve (~${"%.1f".format(estRemainMi)} mi at today's burn rate) and one rural trail pacing tip.

        RIDE TELEMETRY:
        Rig: ${ride.vehicleName} (${profile.nominalLabel} / ${profile.seriesCount}S ${profile.chemistry} • Full ${"%.1f".format(profile.fullChargeV)}V / Cutoff ${"%.1f".format(profile.cutoffEmptyV)}V • Pack ${packWh.toInt()} Wh)
        Title: ${ride.title}
        Distance: ${"%.2f".format(distMi)} mi (${"%.2f".format(ride.distanceKm)} km) | Duration: ${ride.durationSec / 60}m ${ride.durationSec % 60}s
        Max Speed: ${"%.1f".format(maxMph)} mph (${"%.1f".format(ride.maxSpeedKmh)} km/h) | Avg Speed: ${"%.1f".format(avgMph)} mph (${"%.1f".format(ride.avgSpeedKmh)} km/h)
        Elevation Gain: +${"%.0f".format(ride.elevationGainM)} m (+${"%.0f".format(ride.elevationGainM * 3.28084)} ft)
        Battery: ${ride.startBatteryPct}% (${"%.1f".format(startV)}V) -> ${ride.endBatteryPct}% (${"%.1f".format(endV)}V) | Energy Used: ${"%.0f".format(ride.whUsed)} Wh
        Weather: $weatherSummary
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(45_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "Trailhead debrief completed." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun generateFleetLogbookDebrief(
    rides: List<RideSession>,
    activeVehicle: PevVehicle?,
    batteryChemistry: String = "NMC",
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val recent = rides.take(12)
      val totalKm = recent.sumOf { it.distanceKm }
      val totalMi = totalKm * 0.621371
      val totalWh = recent.sumOf { it.whUsed }
      val maxKmh = recent.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
      val avgWhPerMi = com.example.core.PevBatteryPhysics.whPerMile(totalWh, totalKm)
      val nomV = activeVehicle?.voltage ?: 48.0
      val profile = com.example.core.PevBatteryPhysics.packProfile(nomV, batteryChemistry)
      val rideLines =
        recent.joinToString("\n") { r ->
          "- ${r.title} (${r.vehicleName}): ${"%.2f".format(r.distanceKm * 0.621371)} mi, max ${"%.1f".format(r.maxSpeedKmh * 0.621371)} mph, ${r.whUsed.toInt()} Wh, bat ${r.startBatteryPct}%->${r.endBatteryPct}%, +${r.elevationGainM.toInt()}m"
        }
      val prompt =
        """
        You are the Rural ERides GO Barn Garage Fleet Analyst.
        Review this rider's Trailhead Ride Logbook (${recent.size} recent rides) and deliver a 4-bullet executive rural PEV briefing:
        1) Overall Efficiency & Pacing Trend (${"%.1f".format(avgWhPerMi)} Wh/mi avg across ${"%.1f".format(totalMi)} mi, peak ${"%.1f".format(maxKmh * 0.621371)} mph)
        2) Active Pack Health & Real-World Range (${activeVehicle?.name ?: "Active Rig"} • ${profile.seriesCount}S ${profile.chemistry}, Full ${"%.1f".format(profile.fullChargeV)}V, Storage ${"%.1f".format(profile.storageV)}V)
        3) Barn Maintenance Focus (tires, brake pads, fasteners based on mileage & climb)
        4) Next Backroad Challenge recommendation

        RECENT RIDES:
        $rideLines
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(45_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "Fleet logbook briefing completed." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun analyzeTelemetrySafety(
    speedMph: Double,
    limitMph: Int?,
    batteryPct: Int,
    voltage: Double,
    motorTempC: Int,
    weatherSummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        Give a concise 2-sentence real-time PEV telemetry safety advisory:
        Speed: ${"%.1f".format(speedMph)} MPH | Limit/Cap: ${limitMph ?: "Open"} MPH
        Battery: $batteryPct% @ ${"%.1f".format(voltage)}V | Motor Temp: ${motorTempC}C | Weather: $weatherSummary
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "All telemetry rails nominal." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun polishText(
    draft: String,
    tone: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt = "Rewrite this rider community transmission in a $tone tone. Return ONLY the rewritten text:\n\n$draft"
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { draft })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun translateOrSummarize(
    text: String,
    instruction: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt = "$instruction:\n\n$text"
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun interrogateVideo(
    title: String,
    description: String,
    question: String,
    personality: String,
    detailLevel: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        Persona: $personality | Detail: $detailLevel.
        Answer the rider's question about this Rural ERides PEV YouTube video:
        Title: $title
        Description: ${description.take(1500)}
        Question: $question
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(45_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun diagnoseBoardSos(
    title: String,
    body: String,
    telemetrySummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        You are a master PEV Roadside Rescue & Electrical Triage Specialist.
        A rider transmitted an urgent SOS / Breakdown post:
        Title: $title
        Details: $body
        Telemetry: $telemetrySummary

        Provide a crisp 4-bullet action plan:
        1) Immediate safety check (fire/short/brakes)
        2) Likely electrical or mechanical root cause
        3) Roadside multimeter / fuse / connector triage step
        4) Recovery / limp-home recommendation
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(35_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "Check XT60/XT90 main lead, hall sensor plug, and brake cutoff switch." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun runPreRideFlightCheck(
    vehicleSummary: String,
    telemetrySummary: String,
    weatherSummary: String,
    riderGoal: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<PreRideFlightCheckResult> =
    try {
      val schema =
        GeminiJsonSchema.obj(
          properties =
            mapOf(
              "readinessScore" to GeminiJsonSchema.integer("Pre-ride readiness score 0 to 100"),
              "verdict" to GeminiJsonSchema.string("GO, CAUTION, or HOLD verdict with 1-sentence reason"),
              "weatherRisk" to GeminiJsonSchema.string("Wind, precipitation, visibility, and road surface assessment"),
              "batteryStrategy" to GeminiJsonSchema.string("Voltage sag reserve, cruise target, and range buffer"),
              "gearChecklist" to GeminiJsonSchema.array(GeminiJsonSchema.string("Essential safety gear or tool check")),
              "routeTip" to GeminiJsonSchema.string("Actionable route pacing or rural/road safety tip"),
            )
        )
      val prompt =
        """
        You are Rural ERides GO's live Pre-Ride Flight Check Officer.
        Evaluate ride readiness from real sensor/weather telemetry only:
        Active Vehicle: $vehicleSummary
        Live Telemetry: $telemetrySummary
        Live Weather: $weatherSummary
        Rider Mission / Goal: ${riderGoal.ifBlank { "General rural & street PEV cruise" }}
        Be concise, technical, and safety-first.
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(60_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.35f,
              responseMimeType = "application/json",
              responseSchema = schema,
            )
          }
        }
      val json = JSONObject(DirectGeminiRestClient.cleanJsonPayload(res.text).ifBlank { "{}" })
      val gearArr = json.optJSONArray("gearChecklist")
      val gear =
        buildList {
          if (gearArr != null) {
            for (i in 0 until gearArr.length()) {
              gearArr.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
          }
        }
      Result.success(
        PreRideFlightCheckResult(
          readinessScore = json.optInt("readinessScore", 78).coerceIn(0, 100),
          verdict = json.optString("verdict", "CAUTION — Verify tire pressure & brake bite before launch.").ifBlank { "GO" },
          weatherRisk = json.optString("weatherRisk", "Live weather check complete."),
          batteryStrategy = json.optString("batteryStrategy", "Maintain at least 20% voltage reserve."),
          gearChecklist = gear.ifEmpty { listOf("Full-face helmet", "Gloves & lights", "Tire pressure gauge") },
          routeTip = json.optString("routeTip", "Modulate regen on loose gravel and watch blind hills."),
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun analyzeUpgradeCompatibility(
    vehicleSummary: String,
    plannedUpgrade: String,
    ridingGoal: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<UpgradeCompatibilityResult> =
    try {
      val schema =
        GeminiJsonSchema.obj(
          properties =
            mapOf(
              "compatibilityScore" to GeminiJsonSchema.integer("Fitment & electrical compatibility score 0 to 100"),
              "verdict" to GeminiJsonSchema.string("DIRECT FIT, NEEDS SUPPORTING MODS, or INCOMPATIBLE summary"),
              "voltageAndAmpsCheck" to GeminiJsonSchema.string("Voltage tolerance, BMS discharge amps, MOSFET/phase current analysis"),
              "thermalAndRangeImpact" to GeminiJsonSchema.string("Expected range, speed, torque, and stator/controller heat impact"),
              "requiredSupportingMods" to GeminiJsonSchema.array(GeminiJsonSchema.string("Required adapter, bracket, BMS, fuse, or tire clearance mod")),
              "warnings" to GeminiJsonSchema.array(GeminiJsonSchema.string("Electrical, clearance, or safety hazard warning")),
            )
        )
      val prompt =
        """
        You are a Master High-Voltage PEV & E-Moto Engineering Advisor.
        Analyze exact electrical, mechanical, and thermal compatibility:
        Base Vehicle: $vehicleSummary
        Planned Upgrade / Part: $plannedUpgrade
        Rider Goal: ${ridingGoal.ifBlank { "Balanced torque, braking safety, and reliability" }}
        Identify voltage/MOSFET ratings, BMS amp limits, brake/rotor/axle clearances, and required supporting mods.
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(60_000L) {
            DirectGeminiRestClient.generateText(
              modelId = activeModel,
              prompt = prompt,
              temperature = 0.25f,
              responseMimeType = "application/json",
              responseSchema = schema,
            )
          }
        }
      val json = JSONObject(DirectGeminiRestClient.cleanJsonPayload(res.text).ifBlank { "{}" })
      val modsArr = json.optJSONArray("requiredSupportingMods")
      val mods =
        buildList {
          if (modsArr != null) {
            for (i in 0 until modsArr.length()) {
              modsArr.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
          }
        }
      val warnArr = json.optJSONArray("warnings")
      val warnings =
        buildList {
          if (warnArr != null) {
            for (i in 0 until warnArr.length()) {
              warnArr.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
          }
        }
      Result.success(
        UpgradeCompatibilityResult(
          compatibilityScore = json.optInt("compatibilityScore", 70).coerceIn(0, 100),
          verdict = json.optString("verdict", "NEEDS SUPPORTING MODS").ifBlank { "NEEDS SUPPORTING MODS" },
          voltageAndAmpsCheck = json.optString("voltageAndAmpsCheck", "Verify controller MOSFET voltage & BMS continuous amps."),
          thermalAndRangeImpact = json.optString("thermalAndRangeImpact", "Monitor stator temperature and phase amp limits."),
          requiredSupportingMods = mods.ifEmpty { listOf("Verify connector amp rating", "Apply Blue 243 Loctite & torque check") },
          warnings = warnings.ifEmpty { listOf("Do not exceed stock BMS continuous discharge rating.") },
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun analyze3dSubsystemDiagnostics(
    vehicleName: String,
    voltage: Int,
    partName: String,
    partSpecs: String,
    throttlePct: Int,
    ambientTempC: Double,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        You are the Senior High-Voltage PEV CAD & Dyno Chief Engineer.
        Inspect the selected 3D subsystem below and return 3 crisp technical engineering bullet points (torque specs, failure thermal thresholds, and diagnostic checks):
        Vehicle: $vehicleName (${voltage}V)
        Selected Subsystem: $partName
        Nominal Specs: $partSpecs
        Live Dyno Load: ${throttlePct}% throttle | Ambient Air: ${"%.1f".format(ambientTempC)}C
        Keep it factual, concise, and actionable.
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "No engineering notes returned." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun analyzeTelemetryGraphSeries(
    graphTitle: String,
    unitLabel: String,
    sampleCount: Int,
    minVal: Double,
    maxVal: Double,
    avgVal: Double,
    medianVal: Double,
    stdDev: Double,
    slopePerStep: Double,
    outliersCount: Int,
    recentValuesCsv: String,
    methodLabel: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        You are a PEV Telemetry & Statistical Data Scientist.
        Interpret this live ride telemetry chart using the $methodLabel analytical method:
        Graph: $graphTitle ($unitLabel) | Samples: $sampleCount
        Range: ${"%.2f".format(minVal)} .. ${"%.2f".format(maxVal)} | Mean: ${"%.2f".format(avgVal)} | Median: ${"%.2f".format(medianVal)}
        StdDev (σ): ${"%.2f".format(stdDev)} | OLS Linear Slope: ${"%.3f".format(slopePerStep)}/step | Z-Score (>1.5σ) Anomalies: $outliersCount
        Recent Values: $recentValuesCsv
        Give a concise 3-bullet insight covering trend direction, consistency/variance, and practical PEV tuning or riding advice.
        """
          .trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim().ifBlank { "Telemetry analysis complete." })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }

  suspend fun generatePevQuizQuestion(
    vehicleName: String,
    voltage: Int,
    category: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Result<String> =
    try {
      val prompt =
        """
        Create 1 multiple-choice technical PEV quiz question tailored to "$vehicleName" (${voltage}V) in the "$category" domain.
        Format your response strictly as 6 lines separated by newlines:
        LINE1: Question text
        LINE2: Option A
        LINE3: Option B
        LINE4: Option C
        LINE5: Option D
        LINE6: Correct option index (0, 1, 2, or 3) | Detailed 1-sentence engineering explanation
        """.trimIndent()
      val res =
        withModelFallback(modelId) { activeModel ->
          withTimeout(30_000L) { DirectGeminiRestClient.generateText(modelId = activeModel, prompt = prompt) }
        }
      Result.success(res.text.trim())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
}

