package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAiRepository
import com.example.ui.components.TacticalCard
import kotlinx.coroutines.launch

internal data class PevQuizQuestion(
  val id: String,
  val category: String,
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String,
  val difficulty: String = "PRO",
)

internal val PEV_KNOWLEDGE_QUESTION_BANK = listOf(
  PevQuizQuestion(
    id = "bms_1",
    category = "Battery & BMS",
    question = "What is the full-charge (100% SoC) resting voltage of a standard 20S (72V nominal) Lithium NMC battery pack?",
    options = listOf("72.0V", "78.4V", "84.0V", "88.2V"),
    correctIndex = 2,
    explanation = "Each NMC/Li-Ion cell charges to 4.20V maximum. A 20-series (20S) pack reaches 20 × 4.20V = 84.0V at 100% charge.",
  ),
  PevQuizQuestion(
    id = "bms_2",
    category = "Battery & BMS",
    question = "Why is triggering regenerative braking at 100% battery charge on a long downhill grade dangerous?",
    options = listOf(
      "It drains the 12V headlight converter",
      "Regen current can push cells above 4.25V, causing BMS over-voltage cutoff and sudden loss of e-braking",
      "It demagnetizes N42SH rotor magnets",
      "It reverses the throttle Hall sensor polarity",
    ),
    correctIndex = 1,
    explanation = "When cells are already at 4.20V/cell, high regen current spikes voltage above the BMS over-voltage threshold, tripping the BMS or damaging cells.",
  ),
  PevQuizQuestion(
    id = "bms_3",
    category = "Battery & BMS",
    question = "What causes 'voltage sag' when you pin the throttle of an electric dirt bike or hyper-scooter up a steep hill?",
    options = listOf(
      "Internal resistance (IR) of the cells dropping voltage under high amp draw (V_drop = I × R)",
      "The permanent magnets cooling down",
      "Tire pressure expanding",
      "Bluetooth telemetry interference",
    ),
    correctIndex = 0,
    explanation = "By Ohm's Law, drawing high current (I) across the battery pack's internal resistance (R_internal) causes an immediate temporary voltage drop (I × R).",
  ),
  PevQuizQuestion(
    id = "bms_4",
    category = "Battery & BMS",
    question = "Which 21700 cell chemistry is favored in high-discharge PEVs (like 134V–151V EUCs and 72V eMotos) for delivering 35A–45A continuous per cell with minimal sag?",
    options = listOf(
      "Lead-Acid AGM",
      "High-drain tabless / ultra-low-IR NMC (e.g., Samsung 50S / Molicel P42A / P45B)",
      "Alkaline AA Arrays",
      "Standard 1C solar storage cells",
    ),
    correctIndex = 1,
    explanation = "High-drain 21700 cells like Molicel P45B and Samsung 50S feature ultra-low internal resistance to handle extreme phase current peaks safely.",
  ),
  PevQuizQuestion(
    id = "bms_5",
    category = "Battery & BMS",
    question = "When storing a PEV battery pack for several weeks during winter, what is the healthiest state of charge (SoC) and cell voltage?",
    options = listOf(
      "100% (4.20V per cell)",
      "0% (2.80V per cell)",
      "40%–60% (~3.70V–3.85V per cell) in a dry, room-temperature space",
      "Plugged into a fast charger 24/7",
    ),
    correctIndex = 2,
    explanation = "Lithium-ion cells experience minimal electrolyte oxidation and calendar aging when stored around 3.75V–3.85V/cell (roughly 50% SoC).",
  ),
  PevQuizQuestion(
    id = "mech_1",
    category = "e-Moto & Scooter",
    question = "What is the primary advantage of a Sine-Wave FOC (Field-Oriented Control) controller over an older Square-Wave controller?",
    options = listOf(
      "Smooth, silent torque modulation by aligning stator magnetic fields 90° to the rotor flux",
      "It removes the need for a battery BMS",
      "It doubles tire diameter",
      "It only runs on 12V batteries",
    ),
    correctIndex = 0,
    explanation = "FOC continuously calculates rotor position and drives smooth sinusoidal phase currents orthogonal to rotor flux, eliminating cogging noise and maximizing torque efficiency.",
  ),
  PevQuizQuestion(
    id = "mech_2",
    category = "e-Moto & Scooter",
    question = "What is the difference between Battery Current (DC Amps) and Motor Phase Current (AC Amps) in a PEV controller?",
    options = listOf(
      "They are always identical at all speeds",
      "At low speeds, the controller acts as a buck converter, multiplying motor phase current higher than battery current for launch torque",
      "Battery current only powers the horn",
      "Phase current flows through the brake fluid",
    ),
    correctIndex = 1,
    explanation = "At low RPM (low back-EMF), the MOSFET inverter steps down voltage and multiplies phase current (often 2.5x–3x battery current) to generate massive launch torque.",
  ),
  PevQuizQuestion(
    id = "mech_3",
    category = "e-Moto & Scooter",
    question = "Why do high-speed dual-motor hyper-scooters (60+ MPH) require a hydraulic steering damper?",
    options = listOf(
      "To charge the front hub motor",
      "To suppress high-frequency speed wobble (tank-slapper oscillations) caused by short trail geometry and small 10\"–13\" tires",
      "To lock the handlebars straight on corners",
      "To cool the rear brake rotor",
    ),
    correctIndex = 1,
    explanation = "Small-diameter scooter wheels have low gyroscopic stability and steep steering angles; a hydraulic damper absorbs rapid lateral deflection before it becomes a speed wobble.",
  ),
  PevQuizQuestion(
    id = "mech_4",
    category = "e-Moto & Scooter",
    question = "If you swap a larger rear sprocket (e.g., from 48T to 58T) onto an electric dirt bike like a Sur-Ron or Talaria, how does performance change?",
    options = listOf(
      "Higher top speed, less wheel torque",
      "Stronger hill-climb wheel torque and snap, with a lower top speed (unless field weakening compensates)",
      "No change in gearing ratio",
      "It converts the motor to front-wheel drive",
    ),
    correctIndex = 1,
    explanation = "Increasing rear sprocket tooth count raises the final drive reduction ratio, multiplying rear-wheel torque for steep off-road climbs while reducing top speed for a given motor RPM.",
  ),
  PevQuizQuestion(
    id = "mech_5",
    category = "e-Moto & Scooter",
    question = "On inverted off-road suspension forks, what happens if Rebound Damping is set way too slow (closed) on washboard gravel?",
    options = listOf(
      "The fork 'packs down' because it cannot extend back to full travel between rapid bumps, making the ride harsh",
      "The fork bounces like a pogo stick",
      "The rear tire loses air pressure",
      "The headlight turns off",
    ),
    correctIndex = 0,
    explanation = "Excessive rebound damping prevents the fork from recovering between consecutive hits ('packing'), leaving you riding on the bottom of the stroke.",
  ),
  PevQuizQuestion(
    id = "euc_1",
    category = "EUC Dynamics",
    question = "What does an Electric Unicycle's 'PWM %' (Pulse Width Modulation duty cycle) alarm indicate to the rider?",
    options = listOf(
      "Bluetooth speaker volume percentage",
      "How close the motor & controller are to 100% electrical output limit (the cutout threshold where no headroom remains to keep you balanced)",
      "Tire tread depth remaining",
      "GPS satellite signal strength",
    ),
    correctIndex = 1,
    explanation = "PWM % measures how much voltage headroom remains to counter-torque the rider. At ~80%–85% PWM, safety alarms warn that pushing harder risks an over-lean cutout.",
  ),
  PevQuizQuestion(
    id = "euc_2",
    category = "EUC Dynamics",
    question = "Why does an EUC rider use 'Tilt-Back' and audible beeps at high speed or low battery?",
    options = listOf(
      "Because an EUC has no mechanical throttle limit — it physically pitches the pedals backward to force the rider to shift weight back and decelerate",
      "To clean dirt off the pedals",
      "To calibrate the compass",
      "To activate turn signals",
    ),
    correctIndex = 0,
    explanation = "Since acceleration is controlled by leaning forward, the EUC firmware raises the front of the pedals (tilt-back) to shift the rider's center of mass rearward before PWM reaches 100%.",
  ),
  PevQuizQuestion(
    id = "euc_3",
    category = "EUC Dynamics",
    question = "What is the role of velcro contoured 'Power Pads' on the sides of a high-performance suspension EUC?",
    options = listOf(
      "Purely decorative padding",
      "They lock the rider's shins and calves into the wheel, allowing aggressive forward torque leverage, hard braking, and bunny-hopping over trail obstacles",
      "They store extra 21700 cells",
      "They act as Wi-Fi antennas",
    ),
    correctIndex = 1,
    explanation = "Power pads give the rider shin leverage for acceleration and calf leverage for emergency braking, plus foot lock-in to jump roots and rocks.",
  ),
  PevQuizQuestion(
    id = "safety_1",
    category = "Trail & Safety",
    question = "When riding a PEV above 25 MPH on rural gravel roads, which helmet certification offers proper chin-bar impact protection?",
    options = listOf(
      "A standard foam half-shell bicycle cap",
      "A full-face downhill/motocross helmet certified to ASTM F1952 (DH) or ECE 22.06 / DOT",
      "A baseball hat with sunglasses",
      "No helmet is needed on gravel",
    ),
    correctIndex = 1,
    explanation = "Over 35% of severe PEV crash impacts occur on the chin and jaw; a full-face ASTM F1952, ECE 22.06, or DOT helmet is critical at PEV speeds.",
  ),
  PevQuizQuestion(
    id = "safety_2",
    category = "Trail & Safety",
    question = "When approaching horses or livestock on a multi-use rural trail with an electric bike, moto, or EUC, what is the proper trail etiquette?",
    options = listOf(
      "Blast the horn and pass at full speed",
      "Slow to a walking pace or stop, speak calmly so the horse recognizes a human voice (since silent PEVs can startle animals), and yield the right-of-way",
      "Spin the rear tire in gravel",
      "Flash strobe headlights rapidly",
    ),
    correctIndex = 1,
    explanation = "Because electric PEVs approach almost silently, horses can mistake a silent moving silhouette for a predator; stopping and speaking calmly prevents spooking.",
  ),
  PevQuizQuestion(
    id = "safety_3",
    category = "Trail & Safety",
    question = "If a lithium-ion PEV battery pack suffers severe physical puncture or begins venting white sweet-smelling vapor/hissing, what should you do immediately?",
    options = listOf(
      "Plug it into the charger to stabilize voltage",
      "Move the PEV/pack outdoors away from structures and flammable brush immediately, evacuate the area, and call emergency services",
      "Wrap it tightly in a winter blanket",
      "Bring it inside the living room",
    ),
    correctIndex = 1,
    explanation = "Venting vapor indicates imminent thermal runaway. Moving the vehicle outdoors away from structures and staying upwind prevents toxic gas inhalation and structural fire.",
  ),
)

@Composable
internal fun PevKnowledgeTestView(
  activeVehicleName: String,
  activeVoltage: Int,
  onShareScoreToBoard: ((scorePct: Int, rankTitle: String, summary: String) -> Unit)? = null,
) {
  val scope = rememberCoroutineScope()
  val geminiRepo = remember { GeminiAiRepository() }

  val categories = remember {
    listOf("All Categories", "Battery & BMS", "e-Moto & Scooter", "EUC Dynamics", "Trail & Safety")
  }
  var selectedCategory by remember { mutableStateOf("All Categories") }
  val customAiQuestions = remember { mutableStateListOf<PevQuizQuestion>() }

  val activeQuestions = remember(selectedCategory, customAiQuestions.size) {
    val combined = customAiQuestions + PEV_KNOWLEDGE_QUESTION_BANK
    if (selectedCategory == "All Categories") combined
    else combined.filter { it.category == selectedCategory }
  }

  var currentIdx by remember { mutableIntStateOf(0) }
  var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
  var answeredCount by remember { mutableIntStateOf(0) }
  var correctCount by remember { mutableIntStateOf(0) }
  var streak by remember { mutableIntStateOf(0) }
  var bestStreak by remember { mutableIntStateOf(0) }
  var quizFinished by remember { mutableStateOf(false) }
  var aiGenBusy by remember { mutableStateOf(false) }
  var aiGenStatus by remember { mutableStateOf("") }

  val safeIdx = currentIdx.coerceIn(0, (activeQuestions.size - 1).coerceAtLeast(0))
  val currentQuestion = activeQuestions.getOrNull(safeIdx)

  fun resetQuiz(newCategory: String = selectedCategory) {
    selectedCategory = newCategory
    currentIdx = 0
    selectedOptionIdx = null
    answeredCount = 0
    correctCount = 0
    streak = 0
    bestStreak = 0
    quizFinished = false
  }

  val scorePct = if (answeredCount > 0) ((correctCount * 100f) / answeredCount).toInt() else 0
  val rankTitle = when {
    scorePct >= 90 -> "Apex High-Voltage Grandmaster"
    scorePct >= 75 -> "Senior PEV & BMS Engineer"
    scorePct >= 55 -> "Certified Trail Specialist"
    else -> "Apprentice Volt Pilot"
  }
  val rankColor = when {
    scorePct >= 90 -> Color(0xFF39FF14)
    scorePct >= 75 -> Color(0xFF38BDF8)
    scorePct >= 55 -> Color(0xFFFACC15)
    else -> Color(0xFFF97316)
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth().testTag("pev_knowledge_test_container"),
  ) {
    TacticalCard(
      highlight = true,
      title = "PEV KNOWLEDGE & CERTIFICATION TEST",
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
          Surface(shape = CircleShape, color = Color(0xFF22C55E).copy(alpha = 0.22f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
            }
          }
          Column {
            Text(
              "Test Your PEV Engineering & Trail IQ",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = Color.White,
            )
            Text(
              "$activeVehicleName (${activeVoltage}V) • ${activeQuestions.size} Pro Questions",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD),
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, rankColor.copy(alpha = 0.65f)),
        ) {
          Text(
            text = "$correctCount/$answeredCount ($scorePct%) • 🔥$streak",
            color = rankColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // Category Filter Strip
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { resetQuiz(cat) },
            label = { Text(cat, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.24f),
              selectedLabelColor = Color(0xFF4ADE80),
            ),
            modifier = Modifier.testTag("pev_quiz_cat_${cat.lowercase().replace(" ", "_")}"),
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // Progress Bar
      LinearProgressIndicator(
        progress = {
          if (activeQuestions.isEmpty()) 0f
          else ((safeIdx + 1).toFloat() / activeQuestions.size.toFloat()).coerceIn(0f, 1f)
        },
        modifier = Modifier.fillMaxWidth().height(6.dp),
        color = Color(0xFF39FF14),
        trackColor = Color(0xFF1E293B),
      )

      Spacer(Modifier.height(10.dp))

      if (quizFinished || currentQuestion == null) {
        // EXAM COMPLETE SUMMARY CARD
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0B132B),
          border = BorderStroke(1.5.dp, rankColor),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = rankColor, modifier = Modifier.size(44.dp))
            Text(
              "PEV CERTIFICATION COMPLETE!",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = Color.White,
              textAlign = TextAlign.Center,
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = rankColor.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, rankColor),
            ) {
              Text(
                text = "EARNED RANK: $rankTitle",
                color = rankColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
            Text(
              "Final Score: $correctCount / $answeredCount ($scorePct%) • Best Streak: $bestStreak in a row",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE2E8F0),
              textAlign = TextAlign.Center,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = { resetQuiz(selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.Black),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pev_quiz_retake_btn"),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retake Test", fontWeight = FontWeight.Black)
              }
              if (onShareScoreToBoard != null) {
                OutlinedButton(
                  onClick = {
                    onShareScoreToBoard(
                      scorePct,
                      rankTitle,
                      "Scored $scorePct% ($correctCount/$answeredCount) on the $selectedCategory PEV Knowledge Test!",
                    )
                  },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pev_quiz_share_btn"),
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Post to Board", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      } else {
        // ACTIVE QUESTION CARD
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0A101F),
          border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E293B),
              ) {
                Text(
                  text = "QUESTION ${safeIdx + 1} OF ${activeQuestions.size} • ${currentQuestion.category.uppercase()}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF38BDF8),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.25f),
              ) {
                Text(
                  text = currentQuestion.difficulty,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFD8B4FE),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Text(
              text = currentQuestion.question,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )

            // Answer Choices
            currentQuestion.options.forEachIndexed { optIdx, optionText ->
              val hasAnswered = selectedOptionIdx != null
              val isThisSelected = selectedOptionIdx == optIdx
              val isCorrectOption = optIdx == currentQuestion.correctIndex

              val bgColor = when {
                !hasAnswered -> Color(0xFF151E32)
                isCorrectOption -> Color(0xFF064E3B)
                isThisSelected && !isCorrectOption -> Color(0xFF7F1D1D)
                else -> Color(0xFF0F172A)
              }
              val borderColor = when {
                !hasAnswered -> Color.White.copy(alpha = 0.22f)
                isCorrectOption -> Color(0xFF22C55E)
                isThisSelected && !isCorrectOption -> Color(0xFFEF4444)
                else -> Color.White.copy(alpha = 0.10f)
              }

              Surface(
                onClick = {
                  if (selectedOptionIdx == null) {
                    selectedOptionIdx = optIdx
                    answeredCount++
                    if (optIdx == currentQuestion.correctIndex) {
                      correctCount++
                      streak++
                      if (streak > bestStreak) bestStreak = streak
                    } else {
                      streak = 0
                    }
                  }
                },
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                border = BorderStroke(1.5.dp, borderColor),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("pev_quiz_option_$optIdx"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = borderColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(26.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = ('A' + optIdx).toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                      )
                    }
                  }
                  Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isThisSelected || (hasAnswered && isCorrectOption)) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                  )
                  if (hasAnswered && isCorrectOption) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }

            // Instant Engineering Explanation & Next CTA
            if (selectedOptionIdx != null) {
              val wasRight = selectedOptionIdx == currentQuestion.correctIndex
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (wasRight) Color(0xFF052E16) else Color(0xFF2A1215),
                border = BorderStroke(1.dp, if (wasRight) Color(0xFF22C55E) else Color(0xFFF43F5E)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = if (wasRight) "✓ CORRECT — +100 VOLT IQ" else "✗ NOT QUITE — ENGINEERING BREAKDOWN:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (wasRight) Color(0xFF4ADE80) else Color(0xFFFDA4AF),
                  )
                  Text(
                    text = currentQuestion.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = {
                    if (safeIdx + 1 >= activeQuestions.size) {
                      quizFinished = true
                    } else {
                      currentIdx = safeIdx + 1
                      selectedOptionIdx = null
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pev_quiz_next_btn"),
                ) {
                  Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(
                    if (safeIdx + 1 >= activeQuestions.size) "Finish & View Rank Badge" else "Next Question (${safeIdx + 2}/${activeQuestions.size})",
                    fontWeight = FontWeight.Black,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // Live Gemini AI Custom Rig Question Generator
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = {
            aiGenBusy = true
            aiGenStatus = ""
            scope.launch {
              val res: Result<String> = geminiRepo.generatePevQuizQuestion(
                vehicleName = activeVehicleName,
                voltage = activeVoltage,
                category = if (selectedCategory == "All Categories") "Battery & BMS" else selectedCategory,
              )
              res.onSuccess { raw: String ->
                val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.size >= 6) {
                  val qText = lines[0].removePrefix("LINE1:").removePrefix("Question:").trim()
                  val opts = lines.subList(1, 5).mapIndexed { i, s ->
                    s.removePrefix("LINE${i + 2}:").removePrefix("${'A' + i})").removePrefix("${'A' + i}.").trim()
                  }
                  val metaParts = lines[5].removePrefix("LINE6:").split("|")
                  val cIdx = metaParts.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(0, 3) ?: 0
                  val exp = metaParts.getOrNull(1)?.trim().orEmpty().ifBlank {
                    "High-voltage PEV powertrain physics for $activeVehicleName."
                  }
                  val newQ = PevQuizQuestion(
                    id = "ai_${System.currentTimeMillis()}",
                    category = if (selectedCategory == "All Categories") "Battery & BMS" else selectedCategory,
                    question = qText,
                    options = opts,
                    correctIndex = cIdx,
                    explanation = exp,
                    difficulty = "AI RIG CUSTOM",
                  )
                  customAiQuestions.add(0, newQ)
                  currentIdx = 0
                  selectedOptionIdx = null
                  quizFinished = false
                  aiGenStatus = "Added custom AI question for $activeVehicleName!"
                } else {
                  aiGenStatus = "Could not parse AI question format — try again."
                }
              }.onFailure { err: Throwable ->
                aiGenStatus = "AI Quiz Generator note: ${err.message}"
              }
              aiGenBusy = false
            }
          },
          enabled = !aiGenBusy,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pev_quiz_ai_generate_btn"),
        ) {
          if (aiGenBusy) {
            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD8B4FE))
          }
          Spacer(Modifier.width(6.dp))
          Text(
            if (aiGenBusy) "Generating AI Question..." else "AI Custom Question for ${activeVehicleName.split(" ").take(2).joinToString(" ")}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }

        OutlinedButton(
          onClick = { resetQuiz(selectedCategory) },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("pev_quiz_reset_btn"),
        ) {
          Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (aiGenStatus.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
          text = aiGenStatus,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFD8B4FE),
        )
      }
    }
  }
}
