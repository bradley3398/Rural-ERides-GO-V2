package com.example.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.ai.GeminiModelCatalog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.text.Normalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FamilySafetyGuard"

data class ModerationDecision(
  val allowed: Boolean,
  val reason: String = "",
  val category: String = "SAFE",
) {
  companion object {
    val OK = ModerationDecision(allowed = true, reason = "", category = "SAFE")

    fun block(reason: String, category: String): ModerationDecision =
      ModerationDecision(allowed = false, reason = reason, category = category)
  }
}

/**
 * Family-Friendly Community Safety & Anti-Abuse Engine for Rural ERides GO.
 *
 * Enforces G/PG family-oriented community standards across every text and media channel:
 *  1. Zero-latency on-device profanity, slur, insult, harassment, threat, NSFW & obfuscation filter.
 *  2. Anti-spam rate limiter & strike cooldown to prevent flooding or repeated filter abuse.
 *  3. Real-time Firebase AI Logic (Gemini) multimodal media & context moderation for uploaded
 *     photos, avatars, DM images, stories, and public posts.
 *  4. Defense-in-depth display sanitization for legacy community content.
 */
object FamilySafetyGuard {

  const val COMMUNITY_PROMISE_SHORT =
    "Family-Friendly Shield Active: Be kind & respectful. Profanity, insults, bullying, and inappropriate media are automatically blocked."

  private val recentSubmissionTimestamps = ArrayDeque<Long>()
  private val recentViolationTimestamps = ArrayDeque<Long>()
  private var lastSubmittedHash: Int = 0
  private var lastSubmittedMs: Long = 0L

  // Masked / symbol-obfuscated profanity patterns (e.g. f*ck, s**t, b!tch, a$$)
  private val maskedSwearRegex =
    Regex(
      """(?i)\b(f[\W_*@#]+u?[\W_*@#]*c[\W_*@#]*k\w*|s[\W_*@#]+h[\W_*@#]*[i1!l][\W_*@#]*t\w*|b[\W_*@#]*[i1!][\W_*@#]*t[\W_*@#]*c[\W_*@#]*h\w*|a[\W_*@$]+[\W_*@$]+h?o?l?e?|c[\W_*@#]+u[\W_*@#]*n[\W_*@#]*t\w*|d[\W_*@#]*[i1!][\W_*@#]*c[\W_*@#]*k\w*|p[\W_*@#]*u[\W_*@#]*s[\W_*@#]*s[\W_*@#]*y|w[\W_*@#]*h[\W_*@#]*o[\W_*@#]*r[\W_*@#]*e)\b""",
    )

  // Strict word-boundary profanity, vulgarity, slurs, and explicit adult terms
  private val profanityAndExplicitWords =
    setOf(
      "fuck", "fucker", "fucking", "fucked", "fucks", "fuk", "fck", "fking", "motherfucker", "mf", "mfer", "stfu", "gtfo", "wtf", "lmfao", "af",
      "shit", "shitty", "shitting", "bullshit", "horseshit", "dipshit", "shithead", "sht", "shyt",
      "bitch", "bitches", "bitching", "bitchy", "biatch", "btch",
      "ass", "asshole", "assholes", "arsehole", "dumbass", "jackass", "smartass", "fatass", "badass", "kissass", "asshat", "asswipe", "azz",
      "bastard", "bastards", "crap", "crappy", "damn", "dammit", "goddamn", "goddamnit", "hellno",
      "dick", "dicks", "dickhead", "cock", "cocks", "cocksucker", "pussy", "pussies", "cunt", "cunts", "twat", "prick", "pricks", "wanker", "tosser", "bollocks",
      "slut", "sluts", "slutty", "whore", "whores", "skank", "hoe", "hoes", "thot", "hooker", "prostitute",
      "porn", "porno", "pornography", "nsfw", "xxx", "hentai", "onlyfans", "nude", "nudes", "naked", "boobs", "boobies", "tits", "titties", "nipple", "nipples",
      "penis", "vagina", "dildo", "vibrator", "orgasm", "masturbate", "fap", "cum", "jizz", "horny", "sexting", "blowjob", "handjob", "anal", "bdsm", "fetish", "erotic", "stripper",
      "nigger", "niggers", "nigga", "niggas", "chink", "spic", "wetback", "kike", "beaner", "gook", "coon", "raghead", "towelhead", "cracker",
      "faggot", "faggots", "fag", "fags", "dyke", "tranny", "shemale", "queerbaits",
      "retard", "retarded", "retards", "tard", "spastic",
      "nazi", "nazis", "hitler", "kkk", "swastika", "jihadist",
      "meth", "cocaine", "heroin", "fentanyl", "crackhead", "weeddealer", "drugdealer",
    )

  // Insults, bullying, name-calling, and disrespectful hostility
  private val insultAndBullyingWords =
    setOf(
      "idiot", "idiots", "idiotic", "moron", "morons", "moronic", "imbecile", "dimwit", "nitwit", "halfwit", "numbskull", "knucklehead",
      "stupid", "stupidity", "dumb", "dumber", "dumbest", "dummy", "dummies", "braindead", "brainless", "clueless",
      "loser", "losers", "pathetic", "worthless", "useless", "hopeless", "garbage", "trashy", "scum", "scumbag", "lowlife", "sleazeball",
      "ugly", "hideous", "fatso", "fatty", "piggy", "freak", "weirdo", "creep", "creepy", "pervert", "perv", "psycho", "lunatic", "maniac",
      "jerk", "jerks", "douche", "douchebag", "douchbag", "toolbag", "dirtbag", "sleaze", "coward", "crybaby", "snowflakes", "clown", "bozo", "buffoon", "fool", "fools",
      "sucker", "suckers", "wannabe", "poser", "cheater", "liar", "fraud", "scammer", "thief", "degenerate", "parasite", "vermin", "pest", "brat", "punk", "punks",
      "kys", "stf", "pos",
    )

  // Multi-word abusive, threatening, or insulting phrases
  private val blockedPhrases =
    listOf(
      "shut up",
      "shut your mouth",
      "go away",
      "get lost",
      "nobody cares",
      "no one cares",
      "nobody likes you",
      "you suck",
      "u suck",
      "suck it",
      "bite me",
      "screw you",
      "screw off",
      "piss off",
      "eff off",
      "eff you",
      "frick you",
      "kill yourself",
      "kill urself",
      "go die",
      "drop dead",
      "hope you die",
      "hurt yourself",
      "end yourself",
      "i will kill",
      "gonna kill",
      "going to kill",
      "shoot you",
      "stab you",
      "beat you up",
      "punch you",
      "doxx you",
      "swat you",
      "send nudes",
      "show boobs",
      "take off your clothes",
      "piece of trash",
      "piece of garbage",
      "waste of space",
      "you are trash",
      "youre trash",
      "ur trash",
      "you are stupid",
      "youre stupid",
      "ur stupid",
      "you are dumb",
      "youre dumb",
      "you are ugly",
      "youre ugly",
      "you are an idiot",
      "youre an idiot",
      "get a life",
      "touch grass loser",
    )

  // Unambiguous severe roots checked inside glued usernames/handles (no spaces)
  private val severeCallsignSubstrings =
    listOf(
      "fuck", "fuk", "fck", "shit", "bitch", "cunt", "nigg", "fagg", "retard",
      "porn", "nsfw", "nude", "naked", "onlyfans", "hentai", "boob", "titt",
      "penis", "vagina", "dildo", "dick", "pussy", "cock", "slut", "whore",
      "asshole", "dumbass", "jackass", "bastard", "nazi", "hitler",
      "idiot", "moron", "loser", "stupid", "douche", "scumbag", "pervert", "kys",
      "meth", "cocaine", "heroin", "fentanyl",
    )

  // Blocked adult / explicit URL patterns
  private val unsafeUrlKeywords =
    listOf(
      "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "onlyfans",
      "chaturbate", "stripchat", "brazzers", "bangbros", "rule34", "e621",
      "nhentai", "hentai", "nsfw", "xxx", "erome", "fapello", "leakgirls",
    )

  private val wordBoundaryRegex: Regex by lazy {
    val all = (profanityAndExplicitWords + insultAndBullyingWords).sortedByDescending { it.length }
    Regex("""(?i)\b(${all.joinToString("|") { Regex.escape(it) }})\b""")
  }

  private fun normalizeForScan(raw: String): String {
    if (raw.isBlank()) return ""
    // Strip zero-width & invisible Unicode tricks
    val noZeroWidth = raw.replace(Regex("""[\u200B-\u200F\u202A-\u202E\u2060\uFEFF]"""), "")
    val strippedAccents =
      Normalizer.normalize(noZeroWidth, Normalizer.Form.NFD)
        .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
    val lowered = strippedAccents.lowercase()
    // Convert common leetspeak substitutions
    val deLeet =
      buildString(lowered.length) {
        for (ch in lowered) {
          when (ch) {
            '0' -> append('o')
            '1', '!' -> append('i')
            '3' -> append('e')
            '4', '@' -> append('a')
            '5', '$' -> append('s')
            '7', '+' -> append('t')
            '8' -> append('b')
            else -> append(ch)
          }
        }
      }
    return deLeet.replace(Regex("""[^\p{Alnum}\s']"""), " ").replace(Regex("""\s+"""), " ").trim()
  }

  private fun collapseRepeats(text: String): String =
    text.replace(Regex("""([a-z])\1{2,}"""), "$1")

  private fun collapseSingleLetterSpacers(text: String): String {
    // Catches spaced-out obfuscation like "f u c k" or "i d i o t"
    return text.replace(Regex("""(?i)\b([a-z])\s+([a-z])\s+([a-z])(?:\s+([a-z]))?(?:\s+([a-z]))?(?:\s+([a-z]))?\b""")) { m ->
      m.value.replace(" ", "")
    }
  }

  @Synchronized
  private fun recordViolation() {
    val now = System.currentTimeMillis()
    recentViolationTimestamps.addLast(now)
    while (recentViolationTimestamps.isNotEmpty() && now - recentViolationTimestamps.first() > 300_000L) {
      recentViolationTimestamps.removeFirst()
    }
  }

  @Synchronized
  fun checkAntiSpamAndCooldown(contentSignature: String): ModerationDecision {
    val now = System.currentTimeMillis()
    while (recentViolationTimestamps.isNotEmpty() && now - recentViolationTimestamps.first() > 300_000L) {
      recentViolationTimestamps.removeFirst()
    }
    if (recentViolationTimestamps.size >= 3) {
      val lastViolationAge = now - recentViolationTimestamps.last()
      if (lastViolationAge < 30_000L) {
        val waitSec = ((30_000L - lastViolationAge) / 1000L).coerceAtLeast(1L)
        return ModerationDecision.block(
          reason = "Community Shield cooldown active (${waitSec}s) after repeated blocked attempts. Please keep Rural ERides GO friendly and family-safe.",
          category = "COOLDOWN",
        )
      }
    }

    while (recentSubmissionTimestamps.isNotEmpty() && now - recentSubmissionTimestamps.first() > 30_000L) {
      recentSubmissionTimestamps.removeFirst()
    }
    if (recentSubmissionTimestamps.size >= 6) {
      return ModerationDecision.block(
        reason = "Slow down a bit! You're posting too rapidly. Wait a few seconds before sending again.",
        category = "SPAM_RATE_LIMIT",
      )
    }

    val sigHash = contentSignature.trim().lowercase().hashCode()
    if (contentSignature.trim().length >= 8 && sigHash == lastSubmittedHash && (now - lastSubmittedMs) < 12_000L) {
      return ModerationDecision.block(
        reason = "Duplicate message detected. Please avoid spamming the same text.",
        category = "SPAM_DUPLICATE",
      )
    }
    return ModerationDecision.OK
  }

  @Synchronized
  fun recordApprovedSubmission(contentSignature: String) {
    val now = System.currentTimeMillis()
    recentSubmissionTimestamps.addLast(now)
    lastSubmittedHash = contentSignature.trim().lowercase().hashCode()
    lastSubmittedMs = now
  }

  /**
   * Instant synchronous check for any user-entered text (posts, comments, DMs, chat, bios, radar pins).
   */
  fun checkText(vararg fields: String): ModerationDecision {
    val joinedRaw = fields.filter { it.isNotBlank() }.joinToString(" ").trim()
    if (joinedRaw.isBlank()) return ModerationDecision.OK

    // 1. Check masked swears directly on raw text (e.g. f*ck, s**t, a$$)
    if (maskedSwearRegex.containsMatchIn(joinedRaw)) {
      recordViolation()
      return ModerationDecision.block(
        reason = "Blocked: Please keep Rural ERides GO friendly and family-oriented. Profanity or masked bad words are not allowed.",
        category = "PROFANITY",
      )
    }

    val normalized = normalizeForScan(joinedRaw)
    val collapsed = collapseRepeats(normalized)
    val unspaced = collapseSingleLetterSpacers(normalized)
    val candidates = listOf(normalized, collapsed, unspaced, collapseRepeats(unspaced))

    // 2. Check multi-word insults, harassment, threats, and NSFW phrases
    for (candidate in candidates) {
      for (phrase in blockedPhrases) {
        if (candidate.contains(phrase)) {
          recordViolation()
          return ModerationDecision.block(
            reason = "Blocked: Insults, harassment, threats, and inappropriate phrases are not allowed in our family-friendly community.",
            category = "HARASSMENT_OR_INSULT",
          )
        }
      }
    }

    // 3. Check word-boundary profanity, slurs, insults, and explicit terms
    for (candidate in candidates) {
      val match = wordBoundaryRegex.find(candidate)
      if (match != null) {
        val hit = match.value.lowercase()
        val isInsult = hit in insultAndBullyingWords
        recordViolation()
        return ModerationDecision.block(
          reason =
            if (isInsult) {
              "Blocked: Insults and name-calling aren't allowed here. Please keep conversations kind, helpful, and family-friendly."
            } else {
              "Blocked: Bad words, slurs, or adult language aren't allowed. Rural ERides GO is a friendly, family-oriented rider community."
            },
          category = if (isInsult) "INSULT" else "PROFANITY",
        )
      }
    }

    return ModerationDecision.OK
  }

  /**
   * Strict check for usernames / callsigns (catches glued words without spaces like BADWORD99).
   */
  fun checkCallsign(callsign: String): ModerationDecision {
    val textRes = checkText(callsign)
    if (!textRes.allowed) {
      return ModerationDecision.block(
        reason = "That username isn't allowed. Please choose a friendly, family-appropriate rider username.",
        category = "CALLSIGN_BLOCKED",
      )
    }
    val compact = normalizeForScan(callsign).replace(" ", "")
    val compactCollapsed = collapseRepeats(compact)
    for (root in severeCallsignSubstrings) {
      if (compact.contains(root) || compactCollapsed.contains(root)) {
        recordViolation()
        return ModerationDecision.block(
          reason = "That username contains inappropriate or insulting language. Please pick a family-friendly username.",
          category = "CALLSIGN_BLOCKED",
        )
      }
    }
    return ModerationDecision.OK
  }

  /**
   * Checks external image/media URLs to block adult, explicit, or inappropriate domains/links.
   */
  fun checkExternalUrl(url: String): ModerationDecision {
    val clean = url.trim().lowercase()
    if (clean.isBlank()) return ModerationDecision.OK
    if (unsafeUrlKeywords.any { clean.contains(it) }) {
      recordViolation()
      return ModerationDecision.block(
        reason = "Blocked: That external media link is not allowed in a family-friendly community.",
        category = "UNSAFE_MEDIA_URL",
      )
    }
    return ModerationDecision.OK
  }

  /**
   * Defense-in-depth display sanitizer for legacy cloud messages or posts already stored in the database.
   */
  fun sanitizeForDisplay(raw: String): String {
    if (raw.isBlank()) return raw
    var out = maskedSwearRegex.replace(raw, "[friendly edit]")
    out = wordBoundaryRegex.replace(out, "[friendly edit]")
    return out
  }

  /**
   * Real-time Firebase AI Logic (Gemini) multimodal media safety scanner.
   * Blocks nudity, sexually suggestive content, gore/violence, weapons, drugs, hate symbols,
   * offensive gestures, and text profanity inside uploaded photos/avatars/DM images.
   */
  suspend fun moderateBitmap(
    bitmap: Bitmap,
    contextLabel: String = "community photo",
  ): ModerationDecision =
    withContext(Dispatchers.IO) {
      val safeScaled =
        runCatching {
          val maxDim = 512
          val w = bitmap.width.coerceAtLeast(1)
          val h = bitmap.height.coerceAtLeast(1)
          if (w <= maxDim && h <= maxDim) bitmap
          else {
            val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
            Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
          }
        }.getOrDefault(bitmap)

      try {
        val schema =
          Schema.obj(
            properties =
              mapOf(
                "safe" to Schema.boolean(description = "true ONLY if the image is completely family-friendly (G/PG rated)"),
                "category" to Schema.string(description = "SAFE, NUDITY_OR_SEXUAL, VIOLENCE_OR_GORE, HATE_OR_HARASSMENT, PROFANITY_OR_GESTURE, DRUGS_OR_WEAPONS, or SPAM_ABUSE"),
                "reason" to Schema.string(description = "Brief polite explanation if unsafe, or empty string if safe"),
              ),
          )
        val config =
          generationConfig {
            temperature = 0.0f
            responseMimeType = "application/json"
            responseSchema = schema
          }

        val prompt =
          """
          You are the strict family-friendly safety moderator for Rural ERides GO, a family-oriented electric bike/scooter/PEV riding community.
          Evaluate this uploaded $contextLabel for strict G/PG family appropriateness.
          Mark safe=false if the image contains ANY of:
          - Nudity, partial nudity, lingerie, sexually suggestive poses, or adult/NSFW material
          - Graphic violence, blood, gore, self-harm, or dangerous threats
          - Offensive hand gestures (e.g. middle finger), visible profanity/slurs/insults written in text/memes
          - Hate symbols, extremist imagery, harassment, or bullying
          - Illegal drugs, drug paraphernalia, or brandishing firearms/weapons
          Normal outdoor riding photos, PEVs, trails, helmets, riders, pets, scenery, parts, repairs, and friendly selfies are safe=true.
          Return JSON only.
          """.trimIndent()

        var responseText: String? = null
        var lastError: Throwable? = null
        for (modelId in listOf(GeminiModelCatalog.GEMINI_3_5_FLASH_LITE, GeminiModelCatalog.GEMINI_3_8_FLASH)) {
          try {
            val model = AppCheckHealthManager.createGenerativeModel(modelName = modelId, generationConfig = config)
            val response =
              AppCheckHealthManager.runWithAppCheckRecovery("FamilySafetyGuard") {
                withTimeout(22_000L) {
                  model.generateContent(
                    content {
                      image(safeScaled)
                      text(prompt)
                    },
                  )
                }
              }
            responseText = response.text
            if (!responseText.isNullOrBlank()) break
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            lastError = e
            val errMsg = "${e::class.java.simpleName} ${e.message.orEmpty()}".lowercase()
            // If Gemini's own built-in safety filters blocked the image, REJECT immediately!
            if (errMsg.contains("safety") ||
              errMsg.contains("blocked") ||
              errMsg.contains("prohibited") ||
              errMsg.contains("sexually") ||
              errMsg.contains("harassment") ||
              errMsg.contains("hate") ||
              errMsg.contains("dangerous")
            ) {
              recordViolation()
              return@withContext ModerationDecision.block(
                reason = "Photo blocked by Family Safety Shield: inappropriate or unsafe media is not allowed.",
                category = "UNSAFE_MEDIA",
              )
            }
          }
        }

        if (!responseText.isNullOrBlank()) {
          val json = JSONObject(responseText)
          val isSafe = json.optBoolean("safe", false)
          val cat = json.optString("category", if (isSafe) "SAFE" else "UNSAFE_MEDIA")
          val reason = json.optString("reason", "")
          if (!isSafe) {
            recordViolation()
            val friendlyMsg =
              if (reason.isNotBlank()) {
                "Photo blocked ($cat): $reason. Please upload family-friendly media only."
              } else {
                "Photo blocked by Family Safety Shield. Only friendly, family-appropriate photos are allowed."
              }
            return@withContext ModerationDecision.block(friendlyMsg, cat)
          }
          return@withContext ModerationDecision.OK
        }

        if (lastError != null) {
          Log.w(TAG, "AI media moderation temporarily unreachable (${lastError.message}); proceeding with size/format checks.")
        }
        ModerationDecision.OK
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Media moderation check error: ${e.message}")
        ModerationDecision.OK
      } finally {
        if (safeScaled !== bitmap && !safeScaled.isRecycled) {
          runCatching { safeScaled.recycle() }
        }
      }
    }

  /**
   * Decodes a Base64 data URI (e.g. profile avatar) and runs multimodal safety moderation on it.
   */
  suspend fun moderateBase64Image(
    dataUri: String,
    contextLabel: String = "profile photo",
  ): ModerationDecision {
    if (dataUri.isBlank()) return ModerationDecision.OK
    val bmp =
      withContext(Dispatchers.IO) {
        runCatching {
          val pureB64 = dataUri.substringAfter("base64,", dataUri)
          val bytes = Base64.decode(pureB64, Base64.DEFAULT)
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
      } ?: return ModerationDecision.OK

    return try {
      moderateBitmap(bmp, contextLabel)
    } finally {
      if (!bmp.isRecycled) runCatching { bmp.recycle() }
    }
  }

  /**
   * Full submission gate for text + optional photo + optional external URL.
   * 1. Anti-spam & cooldown check
   * 2. Instant on-device profanity/insult/slur/harassment check
   * 3. External URL safety check
   * 4. Cloud AI multimodal image check (when a photo is attached)
   * 5. Cloud AI subtle bullying/harassment context check (for public posts/comments >= 12 chars)
   */
  suspend fun moderateSubmission(
    fields: List<String>,
    bitmap: Bitmap? = null,
    externalUrl: String = "",
    contextLabel: String = "post",
    runDeepAiTextCheck: Boolean = true,
  ): ModerationDecision {
    val combined = fields.filter { it.isNotBlank() }.joinToString(" ").trim()

    // 1. Spam / rate-limit / violation cooldown check
    val rateDecision = checkAntiSpamAndCooldown(combined)
    if (!rateDecision.allowed) return rateDecision

    // 2. Instant on-device text filter (works 100% offline too)
    val localTextDecision = checkText(*fields.toTypedArray())
    if (!localTextDecision.allowed) return localTextDecision

    // 3. External link check
    val urlDecision = checkExternalUrl(externalUrl)
    if (!urlDecision.allowed) return urlDecision

    // 4. Multimodal AI photo safety check
    if (bitmap != null) {
      val mediaDecision = moderateBitmap(bitmap, "$contextLabel photo")
      if (!mediaDecision.allowed) return mediaDecision
    }

    // 5. Cloud AI context check for subtle insults, bullying, or inappropriate innuendo
    if (runDeepAiTextCheck && combined.length >= 12) {
      val aiTextDecision = moderateTextWithAi(combined, contextLabel)
      if (!aiTextDecision.allowed) return aiTextDecision
    }

    recordApprovedSubmission(combined.ifBlank { "media_$contextLabel" })
    return ModerationDecision.OK
  }

  private suspend fun moderateTextWithAi(
    text: String,
    contextLabel: String,
  ): ModerationDecision =
    withContext(Dispatchers.IO) {
      try {
        val schema =
          Schema.obj(
            properties =
              mapOf(
                "safe" to Schema.boolean(description = "true if friendly, respectful, and family-appropriate"),
                "category" to Schema.string(description = "SAFE, PROFANITY, INSULT_OR_BULLYING, HATE_OR_HARASSMENT, SEXUAL_INNUENDO, THREAT, or SCAM"),
                "reason" to Schema.string(description = "Short kind explanation if blocked"),
              ),
          )
        val config =
          generationConfig {
            temperature = 0.0f
            responseMimeType = "application/json"
            responseSchema = schema
          }
        val model =
          AppCheckHealthManager.createGenerativeModel(
            modelName = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
            generationConfig = config,
          )
        val prompt =
          """
          Moderate this $contextLabel for Rural ERides GO, a friendly family-oriented electric bike/scooter community.
          Set safe=false ONLY if it contains profanity, insults, personal attacks, bullying, sexual content/innuendo, hate speech, threats, illegal drug sales, or abusive spam.
          Normal PEV talk (battery voltage, speed, crashes/safety warnings, marketplace prices, trail conditions, mechanical repairs, jokes that are clean) is safe=true.
          Text to evaluate:
          "${text.take(800)}"
          """.trimIndent()
        val response =
          AppCheckHealthManager.runWithAppCheckRecovery("FamilySafetyGuard") {
            withTimeout(12_000L) { model.generateContent(prompt) }
          }
        val raw = response.text
        if (!raw.isNullOrBlank()) {
          val json = JSONObject(raw)
          val isSafe = json.optBoolean("safe", true)
          if (!isSafe) {
            recordViolation()
            val reason = json.optString("reason", "")
            val cat = json.optString("category", "POLICY_VIOLATION")
            return@withContext ModerationDecision.block(
              reason =
                if (reason.isNotBlank()) {
                  "Blocked by Family Safety Shield: $reason. Please keep posts kind and family-friendly."
                } else {
                  "Blocked by Family Safety Shield: Please keep all posts friendly, respectful, and family-appropriate."
                },
              category = cat,
            )
          }
        }
        ModerationDecision.OK
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val errMsg = "${e::class.java.simpleName} ${e.message.orEmpty()}".lowercase()
        if (errMsg.contains("safety") || errMsg.contains("blocked") || errMsg.contains("prohibited")) {
          recordViolation()
          return@withContext ModerationDecision.block(
            reason = "Blocked by Family Safety Shield: Inappropriate or abusive language is not allowed.",
            category = "SAFETY_BLOCKED",
          )
        }
        // On transient network error, local filter already passed so don't break offline use
        ModerationDecision.OK
      }
    }
}
