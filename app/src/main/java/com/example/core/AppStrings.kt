package com.example.core

enum class AppLocale(val code: String, val label: String) {
  EN("en", "English (US)"),
  ES("es", "Español"),
  DE("de", "Deutsch"),
  FR("fr", "Français"),
  ZH("zh", "中文"),
}

data class LocalizedLabels(
  val cockpit: String,
  val radar: String,
  val mechanic: String,
  val copilot: String,
  val channel: String,
  val board: String,
  val admin: String,
  val startRide: String,
  val stopRide: String,
  val sosEmergency: String,
  val liveTelemetry: String,
  val weatherMatrix: String,
  val garage: String,
  val maintenance: String,
  val missionLogs: String,
  val financialRoi: String,
  val settings: String,
)

object AppStrings {
  fun forLocale(locale: String): LocalizedLabels =
    when (locale.lowercase()) {
      "es" ->
        LocalizedLabels(
          cockpit = "Cabina",
          radar = "Radar",
          mechanic = "Mecánico",
          copilot = "Copiloto",
          channel = "Canal",
          board = "Tablero",
          admin = "Admin",
          startRide = "Iniciar Ruta",
          stopRide = "Detener Ruta",
          sosEmergency = "SOS Emergencia",
          liveTelemetry = "Telemetría en Vivo",
          weatherMatrix = "Matriz Climática",
          garage = "Garaje PEV",
          maintenance = "Mantenimiento",
          missionLogs = "Registros de Misión",
          financialRoi = "Impacto Financiero",
          settings = "Ajustes",
        )
      "de" ->
        LocalizedLabels(
          cockpit = "Cockpit",
          radar = "Radar",
          mechanic = "Mechaniker",
          copilot = "Copilot",
          channel = "Kanal",
          board = "Board",
          admin = "Admin",
          startRide = "Fahrt Starten",
          stopRide = "Fahrt Stoppen",
          sosEmergency = "SOS Notruf",
          liveTelemetry = "Live-Telemetrie",
          weatherMatrix = "Wetter-Matrix",
          garage = "PEV-Garage",
          maintenance = "Wartung",
          missionLogs = "Missionslogs",
          financialRoi = "Einsparung",
          settings = "Einstellungen",
        )
      "fr" ->
        LocalizedLabels(
          cockpit = "Cockpit",
          radar = "Radar",
          mechanic = "Mécanicien",
          copilot = "Copilote",
          channel = "Chaîne",
          board = "Forum",
          admin = "Admin",
          startRide = "Démarrer",
          stopRide = "Arrêter",
          sosEmergency = "Urgence SOS",
          liveTelemetry = "Télémétrie Directe",
          weatherMatrix = "Météo Tactique",
          garage = "Garage PEV",
          maintenance = "Maintenance",
          missionLogs = "Journaux de Mission",
          financialRoi = "Impact Financier",
          settings = "Paramètres",
        )
      "zh" ->
        LocalizedLabels(
          cockpit = "驾驶舱",
          radar = "雷达",
          mechanic = "机械师",
          copilot = "AI副驾",
          channel = "频道",
          board = "社区",
          admin = "管理",
          startRide = "开始骑行",
          stopRide = "结束骑行",
          sosEmergency = "紧急求救",
          liveTelemetry = "实时遥测",
          weatherMatrix = "气象矩阵",
          garage = "车库管理",
          maintenance = "维护保养",
          missionLogs = "行程日志",
          financialRoi = "节能收益",
          settings = "系统设置",
        )
      else ->
        LocalizedLabels(
          cockpit = "Trailhead",
          radar = "Backroads",
          mechanic = "Barn Shop",
          copilot = "Co-Pilot",
          channel = "Trail TV",
          board = "Porch Talk",
          admin = "Dispatch",
          startRide = "Roll Out",
          stopRide = "Park Rig",
          sosEmergency = "Trail SOS",
          liveTelemetry = "Trailhead Volt & Speed",
          weatherMatrix = "County Sky & Wind",
          garage = "Barn Garage Fleet",
          maintenance = "Barn & Bench Care",
          missionLogs = "Trailhead Ride Logbook",
          financialRoi = "Backroad Gas Savings",
          settings = "Rig Settings",
        )
    }
}
