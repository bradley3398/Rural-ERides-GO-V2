package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AuthProviderType
import com.example.data.ActiveSessionUser
import com.example.data.MasterSettings
import com.example.data.RiderProfile
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.uriToCloudBase64DataUri
import com.example.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RIDING_STYLES =
  listOf(
    "Trail & Gravel",
    "Off-Road / Dirt",
    "Urban Commuter",
    "Long-Range Touring",
    "Track / Carving",
    "Farm & Utility",
  )

private val EXPERIENCE_LEVELS =
  listOf("Beginner", "Intermediate", "Advanced", "Pro / Builder")

private val BLOOD_TYPES =
  listOf("Unknown", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsProfileAccountPage(
  profile: RiderProfile?,
  activeUser: ActiveSessionUser?,
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  onStatusMessage: (String) -> Unit,
  onOpenCloudVault: () -> Unit,
  onSignOut: () -> Unit,
) {
  val ctx = LocalContext.current
  val cs = MaterialTheme.colorScheme
  val scope = rememberCoroutineScope()

  var callsign by remember { mutableStateOf(profile?.callsign.orEmpty()) }
  var displayName by remember { mutableStateOf(profile?.displayName.orEmpty().ifBlank { activeUser?.displayName.orEmpty() }) }
  var birthdate by remember { mutableStateOf(profile?.birthdate.orEmpty()) }
  var bio by remember { mutableStateOf(profile?.bio.orEmpty()) }
  var hometown by remember { mutableStateOf(profile?.hometown.orEmpty()) }
  var phoneNumber by remember { mutableStateOf(profile?.phoneNumber.orEmpty()) }
  var socialHandle by remember { mutableStateOf(profile?.socialHandle.orEmpty()) }
  var ridingStyle by remember { mutableStateOf(profile?.ridingStyle?.ifBlank { "Trail & Gravel" } ?: "Trail & Gravel") }
  var experienceLevel by remember { mutableStateOf(profile?.experienceLevel?.ifBlank { "Intermediate" } ?: "Intermediate") }
  var unitSystem by remember { mutableStateOf(profile?.unitSystem?.ifBlank { "IMPERIAL" } ?: "IMPERIAL") }
  var riderWeightLbs by remember { mutableDoubleStateOf(profile?.riderWeightLbs ?: settings.riderWeightLbs.toDouble()) }
  var emergencyContactName by remember { mutableStateOf(profile?.emergencyContactName.orEmpty()) }
  var sosContactNumber by remember { mutableStateOf(profile?.sosContactNumber.orEmpty()) }
  var bloodType by remember { mutableStateOf(profile?.bloodType.orEmpty()) }
  var medicalProfile by remember { mutableStateOf(profile?.medicalProfile.orEmpty()) }
  var profileVisibility by remember { mutableStateOf(profile?.profileVisibility?.ifBlank { "PUBLIC" } ?: "PUBLIC") }
  var showOnlineStatus by remember { mutableStateOf(profile?.showOnlineStatus ?: true) }
  var allowDirectCalls by remember { mutableStateOf(profile?.allowDirectCalls ?: true) }
  var allowDirectMessages by remember { mutableStateOf(profile?.allowDirectMessages ?: true) }
  var avatarBase64 by remember { mutableStateOf(profile?.avatarBase64.orEmpty()) }

  var saving by remember { mutableStateOf(false) }
  var savedBanner by remember { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }

  // Keep form synced if profile finishes loading asynchronously from cloud
  LaunchedEffect(profile?.userId, profile?.updatedAt) {
    if (profile != null && !saving) {
      if (callsign.isBlank() && profile.callsign.isNotBlank()) callsign = profile.callsign
      if (displayName.isBlank() && profile.displayName.isNotBlank()) displayName = profile.displayName
      if (birthdate.isBlank() && profile.birthdate.isNotBlank()) birthdate = profile.birthdate
      if (bio.isBlank() && profile.bio.isNotBlank()) bio = profile.bio
      if (hometown.isBlank() && profile.hometown.isNotBlank()) hometown = profile.hometown
      if (phoneNumber.isBlank() && profile.phoneNumber.isNotBlank()) phoneNumber = profile.phoneNumber
      if (socialHandle.isBlank() && profile.socialHandle.isNotBlank()) socialHandle = profile.socialHandle
      if (avatarBase64.isBlank() && profile.avatarBase64.isNotBlank()) avatarBase64 = profile.avatarBase64
    }
  }

  // Zero-permission Android Photo Picker (Play Policy compliant)
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val encoded =
            withContext(Dispatchers.IO) {
              runCatching {
                uriToCloudBase64DataUri(
                  context = ctx,
                  uri = uri,
                  maxDimension = 320,
                  initialQuality = 76,
                  maxChars = 90_000,
                )
              }.getOrNull().orEmpty()
            }
          if (encoded.isNotBlank()) {
            avatarBase64 = encoded
            savedBanner = "New profile photo ready — tap Save Profile to sync."
          } else {
            onStatusMessage("Could not process selected photo.")
          }
        }
      }
    }

  val sanitizedPreviewCallsign =
    remember(callsign) {
      callsign.trim().uppercase().replace(Regex("[^A-Z0-9_.-]"), "_").take(24)
    }

  fun commitSave() {
    val base = profile ?: RiderProfile(userId = activeUser?.uid.orEmpty(), email = activeUser?.email.orEmpty())
    val updated =
      base.copy(
        userId = base.userId.ifBlank { activeUser?.uid.orEmpty() },
        email = base.email.ifBlank { activeUser?.email.orEmpty() },
        callsign = sanitizedPreviewCallsign.ifBlank { base.callsign.ifBlank { "RIDER" } },
        displayName = displayName.trim().take(60),
        birthdate = birthdate.trim().take(24),
        bio = bio.trim().take(300),
        hometown = hometown.trim().take(80),
        phoneNumber = phoneNumber.trim().take(30),
        socialHandle = socialHandle.trim().take(60),
        ridingStyle = ridingStyle,
        experienceLevel = experienceLevel,
        unitSystem = unitSystem,
        riderWeightLbs = riderWeightLbs.coerceIn(60.0, 400.0),
        emergencyContactName = emergencyContactName.trim().take(60),
        sosContactNumber = sosContactNumber.trim().take(30),
        bloodType = if (bloodType == "Unknown") "" else bloodType,
        medicalProfile = medicalProfile.trim().take(300),
        profileVisibility = profileVisibility,
        showOnlineStatus = showOnlineStatus,
        allowDirectCalls = allowDirectCalls,
        allowDirectMessages = allowDirectMessages,
        avatarBase64 = avatarBase64,
      )
    saving = true
    sessionVm.saveFullRiderProfile(updated) { msg ->
      saving = false
      savedBanner = msg
      onStatusMessage(msg)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. PROFILE HERO & AVATAR PHOTO EDITOR
    ElevatedCard(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                listOf(
                  cs.primary.copy(alpha = 0.18f),
                  cs.secondary.copy(alpha = 0.10f),
                  Color.Transparent,
                ),
              ),
            )
            .padding(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            PilotAvatarThumb(
              avatarSource = avatarBase64,
              callsign = sanitizedPreviewCallsign.ifBlank { displayName.ifBlank { "R" } },
              size = 76.dp,
              borderColor = cs.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = displayName.ifBlank { "Rider Profile" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "@${sanitizedPreviewCallsign.ifBlank { "SET_USERNAME" }}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
              )
              val providerTag =
                when (activeUser?.provider) {
                  AuthProviderType.GOOGLE -> "Google Cloud Account"
                  AuthProviderType.SUPABASE -> "Supabase Account"
                  null -> "Local Device Account"
                }
              Text(
                text = activeUser?.email?.ifBlank { providerTag } ?: providerTag,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_pick_avatar_button"),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
              Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(if (avatarBase64.isBlank()) "Add Profile Photo" else "Change Photo")
            }
            if (avatarBase64.isNotBlank()) {
              OutlinedButton(
                onClick = {
                  avatarBase64 = ""
                  savedBanner = "Photo removed — tap Save Profile to confirm."
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("settings_remove_avatar_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove profile photo", modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }

    if (savedBanner.isNotBlank()) {
      Surface(
        color = cs.primaryContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
          Text(
            text = savedBanner,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onPrimaryContainer,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 2. USERNAME, PERSONAL INFO & BIO CARD
    ProfileSectionCard(
      icon = Icons.Default.Person,
      accent = cs.primary,
      title = "Username & Personal Details",
      subtitle = "Change your public rider username, name, birthdate, and bio",
    ) {
      OutlinedTextField(
        value = callsign,
        onValueChange = { callsign = it.take(24) },
        label = { Text("Username / Rider Callsign") },
        placeholder = { Text("e.g. TRAIL_HAWK_99") },
        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
        supportingText = {
          Text(
            "Public handle: @${sanitizedPreviewCallsign.ifBlank { "RIDER" }} • Updates Live Radar, DMs & Community Board",
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_username_input"),
      )

      OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it.take(60) },
        label = { Text("Full Name") },
        placeholder = { Text("Your real name or preferred name") },
        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_fullname_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
      ) {
        OutlinedTextField(
          value = birthdate,
          onValueChange = { birthdate = it.take(24) },
          label = { Text("Birthdate") },
          placeholder = { Text("YYYY-MM-DD") },
          leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("settings_birthdate_input"),
        )
        OutlinedButton(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.padding(top = 6.dp).heightIn(min = 52.dp).testTag("settings_birthdate_picker_btn"),
        ) {
          Text("Calendar")
        }
      }

      OutlinedTextField(
        value = hometown,
        onValueChange = { hometown = it.take(80) },
        label = { Text("Hometown / Riding Region") },
        placeholder = { Text("e.g. Ozarks, MO or Boise, ID") },
        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_hometown_input"),
      )

      OutlinedTextField(
        value = bio,
        onValueChange = { bio = it.take(300) },
        label = { Text("Rider Bio") },
        placeholder = { Text("Tell other riders about your rigs, favorite trails, and riding crew...") },
        supportingText = { Text("${bio.length} / 300 characters") },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_bio_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it.take(30) },
          label = { Text("Phone Number") },
          placeholder = { Text("Optional") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("settings_phone_input"),
        )
        OutlinedTextField(
          value = socialHandle,
          onValueChange = { socialHandle = it.take(60) },
          label = { Text("Social / YT Handle") },
          placeholder = { Text("@channel") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("settings_social_input"),
        )
      }
    }

    // 3. RIDING STYLE, EXPERIENCE & UNITS CARD
    ProfileSectionCard(
      icon = Icons.Default.DirectionsBike,
      accent = Color(0xFF39FF14),
      title = "Riding Style & Calibration Specs",
      subtitle = "Tune range estimates, units, and rider profile badges",
    ) {
      Text("PRIMARY RIDING STYLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        RIDING_STYLES.forEach { style ->
          FilterChip(
            selected = ridingStyle == style,
            onClick = { ridingStyle = style },
            label = { Text(style) },
          )
        }
      }

      Spacer(Modifier.height(4.dp))
      Text("EXPERIENCE LEVEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        EXPERIENCE_LEVELS.forEach { lvl ->
          FilterChip(
            selected = experienceLevel == lvl,
            onClick = { experienceLevel = lvl },
            label = { Text(lvl) },
          )
        }
      }

      Spacer(Modifier.height(4.dp))
      Text("UNIT SYSTEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = unitSystem == "IMPERIAL",
          onClick = { unitSystem = "IMPERIAL" },
          label = { Text("Imperial (MPH / °F / lbs)") },
        )
        FilterChip(
          selected = unitSystem == "METRIC",
          onClick = { unitSystem = "METRIC" },
          label = { Text("Metric (KM/H / °C / kg)") },
        )
      }

      Spacer(Modifier.height(4.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Rider + Gear Weight (Range AI)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        val weightLabel =
          if (unitSystem == "METRIC") "${(riderWeightLbs * 0.453592).roundToInt()} kg"
          else "${riderWeightLbs.roundToInt()} lbs"
        Text(weightLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = cs.primary)
      }
      Slider(
        value = riderWeightLbs.toFloat().coerceIn(80f, 350f),
        onValueChange = { riderWeightLbs = it.roundToInt().toDouble() },
        valueRange = 80f..350f,
      )

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Daily Distance Goal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        val goalDisplay =
          if (unitSystem == "METRIC") "${settings.dailyDistanceGoalKm.roundToInt()} km"
          else "${(settings.dailyDistanceGoalKm * 0.621371f).roundToInt()} mi"
        Text(goalDisplay, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = cs.secondary)
      }
      Slider(
        value = settings.dailyDistanceGoalKm.coerceIn(5f, 120f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(dailyDistanceGoalKm = v.roundToInt().toFloat()) } },
        valueRange = 5f..120f,
      )
    }

    // 4. EMERGENCY & MEDICAL SAFETY CARD
    ProfileSectionCard(
      icon = Icons.Default.MedicalServices,
      accent = Color(0xFFF43F5E),
      title = "Emergency Contact & Medical ID",
      subtitle = "Private to your account — used if Crash SOS triggers",
    ) {
      OutlinedTextField(
        value = emergencyContactName,
        onValueChange = { emergencyContactName = it.take(60) },
        label = { Text("Emergency Contact Name") },
        placeholder = { Text("Spouse, parent, or riding buddy") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_emergency_name_input"),
      )
      OutlinedTextField(
        value = sosContactNumber,
        onValueChange = { sosContactNumber = it.take(30) },
        label = { Text("Emergency SOS Phone Number") },
        placeholder = { Text("SMS destination for crash alerts") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_sos_phone_input"),
      )
      Text("BLOOD TYPE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        BLOOD_TYPES.forEach { bt ->
          val active = (bt == "Unknown" && bloodType.isBlank()) || bloodType == bt
          FilterChip(
            selected = active,
            onClick = { bloodType = if (bt == "Unknown") "" else bt },
            label = { Text(bt) },
          )
        }
      }
      OutlinedTextField(
        value = medicalProfile,
        onValueChange = { medicalProfile = it.take(300) },
        label = { Text("Medical Notes / Allergies") },
        placeholder = { Text("Allergies, medications, or responder instructions") },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_medical_notes_input"),
      )
    }

    // 5. ACCOUNT PRIVACY, COMMS & SECURITY CONTROLS CARD
    ProfileSectionCard(
      icon = Icons.Default.Shield,
      accent = Color(0xFF38BDF8),
      title = "Privacy, Communication & Security",
      subtitle = "Control who can find you, call you, or message you",
    ) {
      Text("PROFILE VISIBILITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = profileVisibility == "PUBLIC",
          onClick = { profileVisibility = "PUBLIC" },
          leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("Public Rider") },
        )
        FilterChip(
          selected = profileVisibility == "PRIVATE",
          onClick = { profileVisibility = "PRIVATE" },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("Private / Stealth") },
        )
      }

      HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))
      AndroidSettingSwitch("Show Live Online Status", checked = showOnlineStatus) { showOnlineStatus = it }
      AndroidSettingSwitch("Allow Direct Video & Audio Calls", checked = allowDirectCalls) { allowDirectCalls = it }
      AndroidSettingSwitch("Allow Direct Messages (Porch DMs)", checked = allowDirectMessages) { allowDirectMessages = it }
      AndroidSettingSwitch("Share Stats on Rider Leaderboard", checked = settings.shareLeaderboard) { v ->
        sessionVm.updateSettings { it.copy(shareLeaderboard = v) }
      }
      AndroidSettingSwitch("Biometric App Lock on Launch", checked = settings.biometricAppLock) { v ->
        sessionVm.updateSettings { it.copy(biometricAppLock = v) }
      }

      Spacer(Modifier.height(2.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Home Privacy Zone (Hide Start/End GPS)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(
          if (settings.privacyZoneMeters <= 0) "Off" else "${settings.privacyZoneMeters} m",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Black,
          color = cs.primary,
        )
      }
      Slider(
        value = settings.privacyZoneMeters.toFloat().coerceIn(0f, 1000f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(privacyZoneMeters = (v / 50f).roundToInt() * 50) } },
        valueRange = 0f..1000f,
      )
    }

    // 6. QUICK ACCOUNT & DEVICE PREFERENCES CARD
    ProfileSectionCard(
      icon = Icons.Default.Tune,
      accent = Color(0xFFA855F7),
      title = "Account Experience & Quick Controls",
      subtitle = "Handy toggles for riding sessions, display, alerts, and sync",
    ) {
      AndroidSettingSwitch("Auto-Start Ride Recording on Motion", checked = settings.autoStartTracking) { v ->
        sessionVm.updateSettings { it.copy(autoStartTracking = v) }
      }
      AndroidSettingSwitch("Keep Screen Awake on Cockpit HUD", checked = settings.wakeLockEnabled) { v ->
        sessionVm.updateSettings { it.copy(wakeLockEnabled = v) }
      }
      AndroidSettingSwitch("Night Vision Red-Shift Mode", checked = settings.nightVisionMode) { v ->
        sessionVm.updateSettings { it.copy(nightVisionMode = v) }
      }
      AndroidSettingSwitch("Auto-Dimmer at Dusk", checked = settings.autoDimmer) { v ->
        sessionVm.updateSettings { it.copy(autoDimmer = v) }
      }
      AndroidSettingSwitch("Incoming Video/Voice Call Ringtone", checked = settings.callRingtoneEnabled) { v ->
        sessionVm.updateSettings { it.copy(callRingtoneEnabled = v) }
      }
      AndroidSettingSwitch("Direct Message Chime Alert", checked = settings.dmChime) { v ->
        sessionVm.updateSettings { it.copy(dmChime = v) }
      }
      AndroidSettingSwitch("Data Saver Mode (Low Bandwidth)", checked = settings.dataSaverMode) { v ->
        sessionVm.updateSettings { it.copy(dataSaverMode = v) }
      }
      AndroidSettingSwitch("Auto Cloud Backup & Sync on Ride Finish", checked = settings.autoCloudBackupEnabled) { v ->
        sessionVm.updateSettings { it.copy(autoCloudBackupEnabled = v, autoCloudSyncOnFinish = v) }
      }
    }

    // 7. SAVE PROFILE & ACCOUNT ACTIONS BAR
    Button(
      onClick = { commitSave() },
      enabled = !saving,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("save_rider_profile_button"),
    ) {
      Icon(Icons.Default.Save, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(
        text = if (saving) "Saving Profile & Username..." else "Save Profile & Account Changes",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onOpenCloudVault,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_open_cloud_vault_btn"),
      ) {
        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Cloud Backup")
      }
      Button(
        onClick = onSignOut,
        colors = ButtonDefaults.buttonColors(containerColor = cs.error),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_page_sign_out_btn"),
      ) {
        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Sign Out")
      }
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedMs = datePickerState.selectedDateMillis
            if (selectedMs != null) {
              val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
              }
              birthdate = sdf.format(Date(selectedMs))
            }
            showDatePicker = false
          },
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
private fun ProfileSectionCard(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  content: @Composable () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  ElevatedCard(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.54f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .border(1.dp, cs.outline.copy(alpha = 0.16f), RoundedCornerShape(22.dp)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(accent.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
          Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
      }
      HorizontalDivider(color = cs.outline.copy(alpha = 0.16f))
      content()
    }
  }
}
