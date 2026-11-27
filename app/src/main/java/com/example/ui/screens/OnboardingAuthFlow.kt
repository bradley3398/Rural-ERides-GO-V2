package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.BuildConfig
import com.example.R
import com.example.ai.DailyPrayerResult
import com.example.data.HudStyle
import com.example.data.PrayerJournalEntry
import com.example.data.UiState
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.TacticalCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AuthLoginScreen(
  busy: Boolean,
  statusMessage: String,
  supabaseConfigured: Boolean,
  onSignInGoogle: () -> Unit,
  onSignInGooglePicker: () -> Unit = {},
  onSignInGoogleBrowser: () -> Unit = {},
  onSupabaseSignIn: (email: String, password: String) -> Unit,
  onSupabaseSignUp: (email: String, password: String, callsign: String) -> Unit,
  onSupabaseResetPassword: (email: String) -> Unit,
  onSupabaseResendConfirmation: (email: String) -> Unit = {},
  onSupabaseGoogleOAuth: () -> Unit = {},
  onOpenPrayer: () -> Unit,
) {
  val ctx = LocalContext.current
  val loginPrefs = remember(ctx) { ctx.getSharedPreferences("rural_erides_login_prefs", Context.MODE_PRIVATE) }

  // Default to Firebase Google Sign-In (original Build 23/26 flow), with Email/Supabase tab right next to it
  var selectedProvider by remember { mutableIntStateOf(1) } // 1 = Firebase Google, 0 = Supabase GoTrue
  var authMode by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Register
  var rememberEmail by remember { mutableStateOf(loginPrefs.getBoolean("remember_email", true)) }
  var email by remember { mutableStateOf(if (rememberEmail) loginPrefs.getString("saved_email", "").orEmpty() else "") }
  var password by remember { mutableStateOf("") }
  var callsign by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  // Welcoming scenic backdrops users can cycle right on the login screen
  val scenicBackdrops =
    remember {
      listOf(
        "Golden Trail" to R.drawable.img_login_welcome_bg_1795215204816,
        "Forest EUC" to R.drawable.img_pev_bg_forest_euc_1795212595148,
        "Coastal Ride" to R.drawable.img_pev_bg_coastal_onewheel_1795212595108,
        "Canyon Run" to R.drawable.img_pev_bg_canyon_emoto_1795212594140,
      )
    }
  var backdropIdx by remember { mutableIntStateOf(loginPrefs.getInt("login_backdrop_idx", 0).coerceIn(0, 3)) }

  val hudCount = remember { HudStyle.entries.size }

  // Feature spotlight cards + Pre-ride tools right on the welcome screen
  val featureHighlights =
    remember(hudCount) {
      listOf(
        "$hudCount Live & OEM HUDs" to "Superbike, F1, Fighter Jet, S866, TF11, Bafang DPC18, iSinwheel & EUC Lean telemetry.",
        "Turn-by-Turn GPS Nav" to "PEV-safe bike lanes, trails, voice guidance, road speed limits & live EV chargers.",
        "Live Convoy Radar & PTT" to "Real-time nearby riders, hazard pins & live squad walkie-talkie.",
        "AI Co-Pilot & Workshop" to "Natural voice diagnostics, AR optical inspection & full PEV service tools.",
        "Spec Engine & Parts" to "Multi-photo galleries, in-app PEV videos & verified vendor links.",
      )
    }
  var selectedFeatureIdx by remember { mutableIntStateOf(0) }
  var showPreflightChecklist by remember { mutableStateOf(false) }
  var showQuickRangeTool by remember { mutableStateOf(false) }
  var showPrivacyInfo by remember { mutableStateOf(false) }
  var showMoreGoogleOptions by remember { mutableStateOf(false) }
  var showErrorTechnicalDetails by remember { mutableStateOf(false) }

  // Interactive Pre-Ride Safety Checklist state
  var chkHelmet by remember { mutableStateOf(false) }
  var chkTires by remember { mutableStateOf(false) }
  var chkBattery by remember { mutableStateOf(false) }
  var chkLights by remember { mutableStateOf(false) }
  var chkBrakes by remember { mutableStateOf(false) }
  val readyCount = listOf(chkHelmet, chkTires, chkBattery, chkLights, chkBrakes).count { it }

  // Quick Pre-Ride Range & Charge Calculator state
  var calcPackWh by remember { mutableFloatStateOf(1500f) }
  var calcCruiseMph by remember { mutableFloatStateOf(22f) }

  val greetingText =
    remember {
      when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning, Rider"
        in 12..16 -> "Good Afternoon, Rider"
        in 17..20 -> "Good Evening, Rider"
        else -> "Welcome Back, Night Rider"
      }
    }
  val datePillText = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date()) }

  val passwordScore =
    remember(password) {
      var s = 0
      if (password.length >= 6) s++
      if (password.length >= 10) s++
      if (password.any { it.isDigit() }) s++
      if (password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() }) s++
      s
    }

  val isCertOrPlayIssue =
    remember(statusMessage) {
      statusMessage.contains("ERROR_INVALID_CERT_HASH", ignoreCase = true) ||
        statusMessage.contains("SHA-1", ignoreCase = true) ||
        statusMessage.contains("12500") ||
        statusMessage.contains("[10]") ||
        statusMessage.contains("dismissed", ignoreCase = true)
    }
  val friendlyStatusMessage =
    remember(statusMessage, isCertOrPlayIssue, supabaseConfigured) {
      when {
        statusMessage.isBlank() -> ""
        isCertOrPlayIssue ->
          if (supabaseConfigured) {
            "One-tap Play sign-in isn't verified on this build. Use Browser Sign-In or Email & Password below."
          } else {
            "One-tap Play sign-in needs certificate verification on this build. Try Browser Sign-In below."
          }
        else -> statusMessage
      }
    }

  val glassCardBg = Color(0xDF0B141E)
  val glassBorder = Color(0x38FFFFFF)
  val accentGold = Color(0xFFFACC15)
  val accentEmerald = Color(0xFF22C55E)

  Box(modifier = Modifier.fillMaxSize()) {
    Image(
      painter = painterResource(id = scenicBackdrops[backdropIdx].second),
      contentDescription = "Rural ERides GO Welcoming Trail Background",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    // Atmospheric cinematic scrim that frames the scenic background and gives high readability below
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops =
                arrayOf(
                  0.00f to Color(0x5508120F),
                  0.28f to Color(0x3B0A1713),
                  0.58f to Color(0xC2071018),
                  1.00f to Color(0xF5060B12),
                )
            )
          )
    )

    Column(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .fillMaxSize()
          .widthIn(max = 560.dp)
          .systemBarsPadding()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // TOP DATE & SCENIC BACKDROP SWITCHER BAR
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color(0xB80A151E),
          shape = CircleShape,
          border = BorderStroke(1.dp, glassBorder),
        ) {
          Text(
            text = datePillText,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF8FAFC),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
        Surface(
          color = Color(0xB80A151E),
          shape = CircleShape,
          border = BorderStroke(1.dp, accentEmerald.copy(alpha = 0.55f)),
          onClick = {
            backdropIdx = (backdropIdx + 1) % scenicBackdrops.size
            loginPrefs.edit().putInt("login_backdrop_idx", backdropIdx).apply()
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("login_cycle_backdrop_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Default.Landscape, contentDescription = "Switch Background", tint = Color(0xFFD9F99D), modifier = Modifier.size(15.dp))
            Text(
              text = scenicBackdrops[backdropIdx].first,
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFF8FAFC),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      // WELCOMING HERO BADGE & PUBLIC RELEASE HEADER
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Box(
            modifier =
              Modifier
                .size(106.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                  Brush.radialGradient(
                    listOf(accentGold.copy(alpha = 0.28f), accentEmerald.copy(alpha = 0.14f), Color.Transparent)
                  )
                )
          )
          Image(
            painter = painterResource(id = R.drawable.img_rural_erides_icon_pro_1795214959357),
            contentDescription = "Rural ERides GO Icon",
            modifier =
              Modifier
                .size(92.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, accentGold.copy(alpha = 0.85f), RoundedCornerShape(24.dp)),
          )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
          color = Color(0xB80A151E),
          shape = CircleShape,
          border = BorderStroke(1.dp, accentGold.copy(alpha = 0.45f)),
        ) {
          Text(
            text = greetingText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFDE047),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(
          "RURAL ERIDES GO",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
          "GPS Navigation • Live Convoy Radar • $hudCount HUDs • AI Workshop",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFE2E8F0),
          textAlign = TextAlign.Center,
        )
      }

      // INTERACTIVE FEATURE SPOTLIGHT CAROUSEL
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          itemsIndexed(featureHighlights) { idx, (title, _) ->
            val selected = selectedFeatureIdx == idx
            Surface(
              color = if (selected) accentEmerald.copy(alpha = 0.22f) else Color(0xB80A151E),
              shape = CircleShape,
              border = BorderStroke(1.dp, if (selected) accentEmerald else glassBorder),
              onClick = { selectedFeatureIdx = idx },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(
                text = title,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFFDCFCE7) else Color(0xFFCBD5E1),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }
        Surface(
          color = glassCardBg,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Default.Explore, contentDescription = null, tint = accentEmerald, modifier = Modifier.size(18.dp))
            Text(
              text = featureHighlights[selectedFeatureIdx].second,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFF1F5F9),
            )
          }
        }
      }

      // MAIN GLASSMORPHIC AUTHENTICATION CARD
      Surface(
        color = glassCardBg,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, glassBorder),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "Sign In to Ride",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            Surface(
              color = accentEmerald.copy(alpha = 0.16f),
              shape = CircleShape,
              border = BorderStroke(1.dp, accentEmerald.copy(alpha = 0.45f)),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = accentEmerald, modifier = Modifier.size(13.dp))
                Text("Cloud Ready", style = MaterialTheme.typography.labelSmall, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
              }
            }
          }
          Spacer(Modifier.height(12.dp))

          // Sleek segmented provider pill switcher
          Surface(
            color = Color(0xFF070E16),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              val sbSel = selectedProvider == 0
              Surface(
                color = if (sbSel) accentEmerald.copy(alpha = 0.22f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = if (sbSel) BorderStroke(1.dp, accentEmerald.copy(alpha = 0.7f)) else null,
                onClick = { selectedProvider = 0 },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("auth_provider_supabase"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Default.Storage, null, tint = if (sbSel) Color(0xFF86EFAC) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(
                    "Email / Account",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (sbSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (sbSel) Color.White else Color(0xFF94A3B8),
                  )
                }
              }

              val fbSel = selectedProvider == 1
              Surface(
                color = if (fbSel) accentGold.copy(alpha = 0.20f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = if (fbSel) BorderStroke(1.dp, accentGold.copy(alpha = 0.7f)) else null,
                onClick = { selectedProvider = 1 },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("auth_provider_firebase"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Default.Cloud, null, tint = if (fbSel) Color(0xFFFDE047) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(
                    "Google Sign-In",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (fbSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (fbSel) Color.White else Color(0xFF94A3B8),
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(12.dp))

          if (selectedProvider == 0) {
            Text(
              if (supabaseConfigured) "Sign in with email & password or continue with browser-based Google account access."
              else "Add SUPABASE_URL and SUPABASE_ANON_KEY in Secrets to enable email accounts and browser Google OAuth.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
              onClick = onSupabaseGoogleOAuth,
              enabled = !busy && supabaseConfigured,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("supabase_google_oauth_button"),
            ) {
              Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Continue with Google (Browser)")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = authMode == 0,
                onClick = { authMode = 0 },
                label = { Text("Sign In") },
                modifier = Modifier.testTag("supabase_mode_signin"),
              )
              FilterChip(
                selected = authMode == 1,
                onClick = { authMode = 1 },
                label = { Text("Create Account") },
                modifier = Modifier.testTag("supabase_mode_signup"),
              )
            }

            if (authMode == 1) {
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                value = callsign,
                onValueChange = { callsign = it.uppercase().take(24) },
                label = { Text("Permanent Rider Callsign (One-Time)") },
                supportingText = { Text("Choose carefully — once created, your callsign is locked to your profile.") },
                leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("supabase_callsign_input"),
              )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Default.Email, null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("supabase_email_input"),
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Password Visibility",
                  )
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("supabase_password_input"),
            )

            // Live Password Strength Indicator when registering
            if (authMode == 1 && password.isNotEmpty()) {
              Spacer(Modifier.height(6.dp))
              val strengthLabel =
                when (passwordScore) {
                  0, 1 -> "Weak password (add numbers/uppercase)"
                  2 -> "Fair password"
                  3 -> "Good password"
                  else -> "Strong password"
                }
              val strengthCol =
                when (passwordScore) {
                  0, 1 -> MaterialTheme.colorScheme.error
                  2 -> Color(0xFFFBBF24)
                  else -> Color(0xFF22C55E)
                }
              LinearProgressIndicator(
                progress = { (passwordScore / 4f).coerceIn(0.15f, 1f) },
                color = strengthCol,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
              )
              Spacer(Modifier.height(2.dp))
              Text(strengthLabel, style = MaterialTheme.typography.labelSmall, color = strengthCol)
            }

            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = rememberEmail,
                  onCheckedChange = { checked ->
                    rememberEmail = checked
                    loginPrefs.edit().putBoolean("remember_email", checked).apply()
                    if (!checked) loginPrefs.edit().remove("saved_email").apply()
                  },
                )
                Text("Remember Email", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2E8F0))
              }
              if (authMode == 0) {
                TextButton(
                  onClick = { onSupabaseResetPassword(email) },
                  enabled = !busy && email.isNotBlank(),
                ) {
                  Text("Forgot Password?")
                }
              }
            }

            if (friendlyStatusMessage.isNotBlank()) {
              val isPositiveNotice =
                friendlyStatusMessage.contains("sent", true) || friendlyStatusMessage.contains("confirm", true)
              Surface(
                color = (if (isPositiveNotice) accentEmerald else MaterialTheme.colorScheme.error).copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, (if (isPositiveNotice) accentEmerald else MaterialTheme.colorScheme.error).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  friendlyStatusMessage,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isPositiveNotice) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                  modifier = Modifier.padding(10.dp),
                )
              }
              if (statusMessage.contains("confirm", true) || statusMessage.contains("verify", true)) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                  onClick = { onSupabaseResendConfirmation(email) },
                  enabled = !busy && email.isNotBlank(),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("supabase_resend_confirmation_button"),
                ) {
                  Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Resend Verification Email", maxLines = 1)
                }
              }
              if (statusMessage.contains("520") || statusMessage.contains("paused", true) || statusMessage.contains("unavailable", true)) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                  onClick = {
                    selectedProvider = 1
                    onSignInGoogle()
                  },
                  enabled = !busy,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("supabase_520_google_fallback_button"),
                ) {
                  Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Use Google Sign-In Instead", maxLines = 1)
                }
              }
              Spacer(Modifier.height(8.dp))
            }

            Button(
              onClick = {
                if (rememberEmail && email.isNotBlank()) {
                  loginPrefs.edit().putString("saved_email", email.trim()).apply()
                }
                if (authMode == 0) onSupabaseSignIn(email, password)
                else onSupabaseSignUp(email, password, callsign)
              },
              enabled = !busy && email.isNotBlank() && password.isNotBlank(),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = accentEmerald, contentColor = Color(0xFF04140B)),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("supabase_auth_submit_button"),
            ) {
              if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF04140B))
              } else {
                Icon(if (authMode == 0) Icons.Default.Login else Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text(if (authMode == 0) "Sign In & Ride" else "Create Account", fontWeight = FontWeight.Bold)
              }
            }
          } else {
            Text(
              "Instant Google access with live community board, convoy radar, walkie-talkie & encrypted messages.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1),
            )
            Spacer(Modifier.height(12.dp))
            if (friendlyStatusMessage.isNotBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                    Text(
                      friendlyStatusMessage,
                      style = MaterialTheme.typography.labelMedium,
                      color = Color(0xFFFCA5A5),
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (isCertOrPlayIssue && statusMessage != friendlyStatusMessage) {
                    TextButton(
                      onClick = { showErrorTechnicalDetails = !showErrorTechnicalDetails },
                      contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                      Text(
                        if (showErrorTechnicalDetails) "Hide technical details" else "Show technical details",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE047),
                      )
                    }
                    AnimatedVisibility(visible = showErrorTechnicalDetails) {
                      Text(
                        statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                      )
                    }
                  }
                }
              }
              if (isCertOrPlayIssue) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                  onClick = {
                    if (supabaseConfigured) onSupabaseGoogleOAuth()
                    else selectedProvider = 0
                  },
                  enabled = !busy,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("non_play_google_fallback_button"),
                ) {
                  Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(
                    if (supabaseConfigured) "Continue with Google (Browser)"
                    else "Switch to Email Sign-In"
                  )
                }
              }
              Spacer(Modifier.height(10.dp))
            }
            Button(
              onClick = onSignInGoogle,
              enabled = !busy,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = accentEmerald, contentColor = Color(0xFF04140B)),
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("google_sign_in_button"),
            ) {
              if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF04140B))
              } else {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google", fontWeight = FontWeight.Bold)
              }
            }
            Spacer(Modifier.height(6.dp))
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { showMoreGoogleOptions = !showMoreGoogleOptions }
                  .minimumInteractiveComponentSize()
                  .padding(horizontal = 6.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                if (showMoreGoogleOptions) "Hide alternative Google sign-in options" else "More Google sign-in options",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1),
              )
              Spacer(Modifier.width(4.dp))
              Icon(
                if (showMoreGoogleOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp),
              )
            }
            AnimatedVisibility(visible = showMoreGoogleOptions) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(
                  onClick = onSignInGooglePicker,
                  enabled = !busy,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_picker_sign_in_button"),
                ) {
                  Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("Google Account Picker (Android 14 / Tablet)")
                }
                OutlinedButton(
                  onClick = onSignInGoogleBrowser,
                  enabled = !busy,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_browser_sign_in_button"),
                ) {
                  Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("Google Browser Sign-In (Backup)")
                }
              }
            }
          }

          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onOpenPrayer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, glassBorder),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_riders_prayer_button"),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentGold)
            Spacer(Modifier.width(8.dp))
            Text("Rider's Prayer & Safety Blessing", color = Color(0xFFF8FAFC))
          }
        }
      }

      // PRE-RIDE RIDER TOOLS
      Surface(
        color = glassCardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, glassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text(
            "Pre-Ride Rider Tools",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Spacer(Modifier.height(8.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = showPreflightChecklist,
              onClick = { showPreflightChecklist = !showPreflightChecklist },
              label = { Text("Safety Check ($readyCount/5)", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp)) },
              modifier = Modifier.weight(1f).testTag("login_preflight_toggle"),
            )
            FilterChip(
              selected = showQuickRangeTool,
              onClick = { showQuickRangeTool = !showQuickRangeTool },
              label = { Text("Range Estimator", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = { Icon(Icons.Default.BatteryChargingFull, null, modifier = Modifier.size(15.dp)) },
              modifier = Modifier.weight(1f).testTag("login_range_calc_toggle"),
            )
          }

          AnimatedVisibility(visible = showPreflightChecklist) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
              LegalCheckRow("Helmet, gloves & eye protection ready", chkHelmet) { chkHelmet = it }
              LegalCheckRow("Tire PSI & sidewalls inspected", chkTires) { chkTires = it }
              LegalCheckRow("Battery pack charge & port cover secure", chkBattery) { chkBattery = it }
              LegalCheckRow("Headlight & rear brake light tested", chkLights) { chkLights = it }
              LegalCheckRow("Brakes, folding latch & stem tight", chkBrakes) { chkBrakes = it }
            }
          }

          AnimatedVisibility(visible = showQuickRangeTool) {
            val whPerMi = (18.0 + (calcCruiseMph - 15.0).coerceAtLeast(0.0) * 1.35)
            val estRangeMi = (calcPackWh * 0.85 / whPerMi).coerceAtLeast(1.0)
            val estChargeHrs = (calcPackWh / 300.0) * 1.12
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Battery Pack: ${calcPackWh.roundToInt()} Wh • Cruise: ${calcCruiseMph.roundToInt()} MPH", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2E8F0))
              Slider(value = calcPackWh, onValueChange = { calcPackWh = it }, valueRange = 300f..5000f)
              Slider(value = calcCruiseMph, onValueChange = { calcCruiseMph = it }, valueRange = 12f..55f)
              Surface(
                color = accentEmerald.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Speed, null, tint = accentEmerald, modifier = Modifier.size(18.dp))
                    Text("Est. Real Range: ~${"%.1f".format(estRangeMi)} mi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color.White)
                  }
                  Text("~${"%.1f".format(estChargeHrs)}h @ 5A", style = MaterialTheme.typography.labelSmall, color = accentGold)
                }
              }
            }
          }

          Spacer(Modifier.height(6.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              "v${BuildConfig.VERSION_NAME} • GPS • Offline Vault • Cloud Sync",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
            )
            TextButton(onClick = { showPrivacyInfo = !showPrivacyInfo }) {
              Text(if (showPrivacyInfo) "Hide Info" else "Privacy & Safety", fontSize = 11.sp, color = accentEmerald)
            }
          }
          AnimatedVisibility(visible = showPrivacyInfo) {
            Text(
              "Your exact GPS location is never shared on Radar unless you toggle Live Beacon ON. Emergency contacts and ride telemetry stay synced privately to your account.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1),
            )
          }
        }
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
fun CallsignAndLegalOnboardingScreen(
  defaultName: String,
  busy: Boolean,
  statusMessage: String,
  onSubmit: (
    callsign: String,
    displayName: String,
    unitSystem: String,
    emergencyName: String,
    sosNumber: String,
    medicalInfo: String,
  ) -> Unit,
) {
  var callsign by remember { mutableStateOf("") }
  var displayName by remember { mutableStateOf(defaultName) }
  var unitSystem by remember { mutableStateOf("IMPERIAL") }
  var emergencyName by remember { mutableStateOf("") }
  var sosNumber by remember { mutableStateOf("") }
  var medicalInfo by remember { mutableStateOf("") }

  var ackHelmet by remember { mutableStateOf(false) }
  var ackLaws by remember { mutableStateOf(false) }
  var ackBattery by remember { mutableStateOf(false) }
  var ackRadar by remember { mutableStateOf(false) }
  var ackSos by remember { mutableStateOf(false) }
  var ackPrivacy by remember { mutableStateOf(false) }
  var ackAssumption by remember { mutableStateOf(false) }
  var ackPermanentCallsign by remember { mutableStateOf(false) }
  val allChecked = ackHelmet && ackLaws && ackBattery && ackRadar && ackSos && ackPrivacy && ackAssumption && ackPermanentCallsign

  Box(
    modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.width(8.dp))
      Text("PILOT IDENTITY & 7-POINT SAFETY BRIEFING", style = MaterialTheme.typography.titleLarge)
    }

    TacticalCard(highlight = true) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        Text(
          "ONE-TIME PERMANENT CALLSIGN LOCK",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.tertiary,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        "You can only create your callsign once for this account. After confirmation it is permanently locked and cannot be changed.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = callsign,
        onValueChange = { callsign = it.uppercase().replace(" ", "_").take(24) },
        label = { Text("Permanent Tactical Callsign (Required)") },
        placeholder = { Text("e.g., APEX_72V") },
        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("callsign_input"),
      )
      LegalCheckRow(
        "I confirm \"${callsign.ifBlank { "MY CALLSIGN" }}\" is final and permanently locked to my account.",
        ackPermanentCallsign,
      ) { ackPermanentCallsign = it }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it },
        label = { Text("Pilot Display Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("display_name_input"),
      )
      Spacer(Modifier.height(8.dp))
      @OptIn(ExperimentalLayoutApi::class)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = unitSystem == "IMPERIAL",
          onClick = { unitSystem = "IMPERIAL" },
          label = { Text("Imperial (MPH / Mi / °F)") },
        )
        FilterChip(
          selected = unitSystem == "METRIC",
          onClick = { unitSystem = "METRIC" },
          label = { Text("Metric (KM/H / Km / °C)") },
        )
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = emergencyName,
        onValueChange = { emergencyName = it },
        label = { Text("SOS Emergency Contact Name (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = sosNumber,
        onValueChange = { sosNumber = it },
        label = { Text("SOS SMS / Dial Phone Number (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = medicalInfo,
        onValueChange = { medicalInfo = it },
        label = { Text("Medical Notes / Blood Type (Optional)") },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    TacticalCard {
      Text("7-POINT RURAL ERIDES LEGAL & SAFETY AGREEMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      LegalCheckRow("1. Full-face helmet & protective armor required at all speeds.", ackHelmet) { ackHelmet = it }
      LegalCheckRow("2. Obey local PEV class, trail, and posted road speed laws.", ackLaws) { ackLaws = it }
      LegalCheckRow("3. Monitor lithium pack temperature & cutoff limits before riding.", ackBattery) { ackBattery = it }
      LegalCheckRow("4. Live Radar shares coordinates only when you enable visibility.", ackRadar) { ackRadar = it }
      LegalCheckRow("5. Crash SOS dispatches SMS/Dial intents and does not replace 911.", ackSos) { ackSos = it }
      LegalCheckRow("6. Community telemetry & AI diagnostics are advisory engineering tools.", ackPrivacy) { ackPrivacy = it }
      LegalCheckRow("7. I accept sole responsibility for electric ride operation.", ackAssumption) { ackAssumption = it }
    }

    if (statusMessage.isNotBlank()) {
      Text(statusMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    }

    Button(
      onClick = { onSubmit(callsign, displayName, unitSystem, emergencyName, sosNumber, medicalInfo) },
      enabled = !busy && callsign.isNotBlank() && allChecked,
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("complete_onboarding_button"),
    ) {
      if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
      else Text("ARM COCKPIT & ENTER GRID")
    }
  }
  }
}

@Composable
private fun LegalCheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Checkbox(checked = checked, onCheckedChange = onChange)
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
  }
}

@Composable
fun BiometricLockScreen(
  voiceAudio: VoiceAudioManager,
  onUnlocked: () -> Unit,
  onSignOut: () -> Unit,
) {
  val activity = LocalContext.current as? FragmentActivity
  var errMsg by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    if (activity != null) {
      voiceAudio.promptBiometricUnlock(activity, onSuccess = onUnlocked, onFailure = { errMsg = it })
    }
  }

  Box(
    modifier = Modifier.fillMaxSize().systemBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        Icons.Default.Fingerprint,
        contentDescription = "Biometric Lock",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(72.dp),
      )
      Spacer(Modifier.height(16.dp))
      Text("COCKPIT BIOMETRIC LOCK ACTIVE", style = MaterialTheme.typography.titleLarge)
      if (errMsg.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(errMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
      }
      Spacer(Modifier.height(20.dp))
      Button(
        onClick = {
          if (activity != null) {
            voiceAudio.promptBiometricUnlock(activity, onSuccess = onUnlocked, onFailure = { errMsg = it })
          }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("retry_biometric_button"),
      ) {
        Text("Unlock with Fingerprint / Face")
      }
      Spacer(Modifier.height(10.dp))
      OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text("Sign Out")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidersPrayerModal(
  callsign: String,
  prayerState: UiState<DailyPrayerResult>,
  journal: List<PrayerJournalEntry>,
  voiceAudio: VoiceAudioManager,
  settings: com.example.data.MasterSettings = com.example.data.MasterSettings(),
  canSaveToCloud: Boolean,
  onReroll: () -> Unit,
  onSaveJournal: (title: String, body: String, category: String) -> Unit,
  onDeleteJournal: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var journalTitle by remember { mutableStateOf("") }
  var journalBody by remember { mutableStateOf("") }
  var journalCategory by remember { mutableStateOf("Trail Safety") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("RIDER'S PRAYER & BLESSING", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close Prayer Overlay") }
      }

      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daily AI") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Salvation") })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Journal (${journal.size})") })
      }
      Spacer(Modifier.height(12.dp))

      when (selectedTab) {
        0 -> {
          when (prayerState) {
            is UiState.Loading -> {
              Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
            }
            is UiState.Error -> {
              TacticalCard {
                Text(prayerState.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onReroll) { Text("Retry Live AI Prayer") }
              }
            }
            is UiState.Success -> {
              val p = prayerState.data
              TacticalCard(highlight = true) {
                Text(p.reference, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(p.verse, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text(p.prayer, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                Text("SAFETY CHECK: ${p.safetyTip}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
              }
              Spacer(Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReroll, modifier = Modifier.weight(1f).testTag("reroll_prayer_button")) {
                  Icon(Icons.Default.Refresh, contentDescription = null)
                  Spacer(Modifier.width(4.dp))
                  Text("New Blessing")
                }
                OutlinedButton(
                  onClick = { voiceAudio.speakWithActiveSettings("${p.verse}. ${p.prayer}. Safety reminder: ${p.safetyTip}", settings) },
                  modifier = Modifier.weight(1f).testTag("speak_prayer_button"),
                ) {
                  Icon(Icons.Default.VolumeUp, contentDescription = null)
                  Spacer(Modifier.width(4.dp))
                  Text("Speak Audio")
                }
              }
              if (canSaveToCloud) {
                Spacer(Modifier.height(8.dp))
                Button(
                  onClick = {
                    onSaveJournal("Daily Blessing (${p.reference})", "${p.verse}\n\n${p.prayer}", "Daily Blessing")
                  },
                  modifier = Modifier.fillMaxWidth().testTag("bookmark_prayer_button"),
                ) {
                  Icon(Icons.Default.Bookmark, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Save to Prayer Journal")
                }
              }
            }
          }
        }
        1 -> {
          val sinnersPrayer =
            "Heavenly Father, I come before You acknowledging that I have sinned and fallen short. I believe Jesus Christ died on the cross for my sins and rose again. I ask You to forgive me, guide my steps, guard my travels on every road and trail, and make me new. In Jesus' name, Amen."
          TacticalCard(highlight = true) {
            Text("THE SINNER'S PRAYER OF SALVATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(sinnersPrayer, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            Button(
              onClick = { voiceAudio.speakWithActiveSettings(sinnersPrayer, settings) },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Default.VolumeUp, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("Read Aloud")
            }
          }
        }
        2 -> {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canSaveToCloud) {
              OutlinedTextField(
                value = journalTitle,
                onValueChange = { journalTitle = it },
                label = { Text("Prayer / Gratitude Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = journalBody,
                onValueChange = { journalBody = it },
                label = { Text("Write entry ($callsign)") },
                modifier = Modifier.fillMaxWidth(),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Trail Safety", "Gratitude", "Family").forEach { cat ->
                  FilterChip(selected = journalCategory == cat, onClick = { journalCategory = cat }, label = { Text(cat) })
                }
              }
              Button(
                onClick = {
                  if (journalBody.isNotBlank()) {
                    onSaveJournal(journalTitle, journalBody, journalCategory)
                    journalTitle = ""
                    journalBody = ""
                  }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_journal_entry_button"),
              ) { Text("Save Prayer to Vault") }
            }
            LazyColumn(modifier = Modifier.heightIn(min = 120.dp, max = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(journal, key = { it.id }) { entry ->
                TacticalCard {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${entry.title} • ${entry.category}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteJournal(entry.id) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Prayer")
                    }
                  }
                  Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}
