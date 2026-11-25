<div align="center">
  <img src="public/assets/aistudio/icon_75e30694.png" alt="Rural ERides GO Logo" width="112" height="112" />
  <h1>Rural ERides GO</h1>
  <p><strong>Universal Avionics, Telemetry Studio, 3D PBR Rig Lab &amp; Rider Radar for Personal Electric Vehicles</strong></p>
  <p><em>"KEEP IT RURAL YALL" — Created &amp; Architected by Lord Bradley Callison</em></p>

  <p>
    <a href="https://bradley3398.github.io/Rural-Erides-GO/"><img src="https://img.shields.io/badge/GitHub%20Pages-Live%20Site-10B981?style=for-the-badge&logo=githubpages&logoColor=white" alt="GitHub Pages" /></a>
    <a href="https://github.com/bradley3398/Rural-Erides-GO/releases/latest"><img src="https://img.shields.io/badge/Release-v47.0%20(Build%2047)-6366F1?style=for-the-badge&logo=android&logoColor=white" alt="Release v47.0" /></a>
    <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose%20M3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin & Jetpack Compose" />
    <img src="https://img.shields.io/badge/3D%20Engine-Filament%20OpenGL%20PBR-06B6D4?style=for-the-badge" alt="Google Filament OpenGL PBR" />
    <img src="https://img.shields.io/badge/Backend-Firebase%20%26%20Gemini%20AI-F59E0B?style=for-the-badge&logo=firebase&logoColor=white" alt="Firebase & Gemini AI" />
  </p>

  <p>
    <a href="https://bradley3398.github.io/Rural-Erides-GO/">🌐 User Website (GitHub Pages)</a> •
    <a href="https://github.com/bradley3398/Rural-Erides-GO/releases/latest">📦 Download Latest APK</a> •
    <a href="#-core-features">⚡ Features</a> •
    <a href="#-building-from-source">🛠️ Build Guide</a> •
    <a href="#-release-history">📜 Changelog</a>
  </p>
</div>

---

## Overview

**Rural ERides GO** is a native Android companion suite built in **Kotlin & Jetpack Compose Material 3** for high-performance **Electric Unicycles (EUCs), e-Motos (Sur-Ron / Talaria), Hyper-Scooters, Fat-Tire e-Bikes, and e-Trikes**. Designed for riders who rely on Personal Electric Vehicles across rural backroads, trails, and daily utility routes, it combines real hardware telemetry, physically based 3D vehicle inspection, cloud-synced analytics, live weather/radar, and Gemini AI diagnostics—with **100% live data and zero mockups**.

---

## ⚡ Core Features

### 1. Google Filament OpenGL ES PBR 3D Rig Lab
- **Pure OpenGL ES 3.0 PBR Pipeline (`Engine.Backend.OPENGL`)**: Vulkan PBR has been completely removed for rock-solid compatibility across Android GPUs, backed by an automatic OpenGL ES fallback viewport.
- **Realistic Procedural `.glb` PEV Models**: Full multi-part geometry for **e-Moto**, **Hyper-Scooter**, **Fat-Tire e-Bike**, and **Electric Unicycle** with calibrated metallic/roughness PBR materials, double-sided spoked rims, brake calipers, headlights, taillights, and amber turn signals.
- **Studio Lighting & Diagnostics**: 7-level roughness-prefiltered Studio Softbox IBL cubemap + 3-band Spherical Harmonics, ACES Filmic tone mapping, FXAA + SSAO, 7 interactive subsystem hotspots, exploded CAD teardown slider, Carbon Showroom gloss, CAD Wireframe, and X-Ray inspection modes.

### 2. Telemetry Studio (11 Fleet Graphs) & Crash-Proof Ride Inspector
- **11 Independent Fleet Graph Windows**: Speed Profile, Elevation Gain, Efficiency & Voltage, Distance vs Energy, 5-Mode Fleet Pie Slices, Pace vs Efficiency Scatter, Ride Duration Histogram, Motor Watts & Amps, 5-Axis Performance Radar, Gas Savings ROI, and AI Fleet Coach.
- **Personal Records Ribbon**: Instant live KPI cards for Fastest Peak, Longest Trip, Biggest Climb, and Best Efficiency—each jumping directly into the Ride Inspector.
- **Crash-Proof 5-Module Ride Inspector**: Replaces the legacy monolithic stacked deep dive with a focused single-module inspector (**Speed Curve**, **Climb Profile**, **Energy & Pack**, **GPS Route**, and **Replay & AI**) that parses GPS polylines once per ride and launches full-screen modal analytics (SMA, Linear Trend, Z-Score).

### 3. Tactical Neon Cockpit & 9-DOF AHRS Telemetry
- **Live Sensor Fusion**: 1Hz–5Hz FusedLocation GPS speedometer + 9-DOF AHRS (Rotation Vector, Gyroscope, Accelerometer, Magnetometer) with pitch, roll, lean-angle peak hold, 360° compass rose, and live Artificial Horizon.
- **Exact PEV Battery Physics**: Accurate resting OCV and under-load voltage sag calculations for 36V–134V **NMC & LiFePO4** packs (e.g., 48V 13S = 54.6V, 52V 14S = 58.8V, 72V 20S = 84.0V), Wh/mi efficiency learning, and cold-weather range derating.
- **Safety & Field Utilities**: 14 switchable Cockpit HUD skins, IMU crash detection with auto-cancelable SOS SMS/siren, Return-to-Base homing beacon, road speed limit alerts, and an optical flashlight/CameraX Morse transceiver.

### 4. Privacy-First Rider Radar, Navigation & Weather
- **Opt-In Multi-Rider Mesh**: Your live Firestore radar beacon is **OFF by default**—enable it only when riding with a squad.
- **Live Maps & Turn-by-Turn PEV Routing**: OSMDroid maps with smooth pan/fling inertia, RainViewer live Doppler storm radar, Open-Meteo 6-tab PEV weather matrix (headwind impact, rain timeline, road grip), EV/PEV charger locator, and Push-to-Talk Squad Walkie-Talkie.

### 5. Gemini AI Mechanic ("Bench Eyes") & AI Co-Pilot
- **Live Maintenance Bench Camera**: Embedded CameraX workbench viewer with 1-tap visual hardware inspection powered by **Firebase AI Logic (Gemini)**.
- **17-Bay PEV Engineering Lab**: Interactive wire voltage-drop, PWM headroom, motor/controller thermal, gearing, and OEM E-code calculators plus a 4-model PEV Spec Comparison Engine.
- **20 Rural & PEV Co-Pilot Personas + Custom AI Voice Studio**: Grounded AI guidance, live web/maps grounding, zero-latency streaming TTS buffer, and saved offline briefings.

### 6. Porch Community Board, Stories & Media Hub
- **Verified Authentic Riders**: Zero-trust Firestore security rules with Google Sign-In via Jetpack Credential Manager.
- **Live 24-Hour Rider Timeline Stories**: Zero-permission Android Photo Picker uploads with real battery telemetry badges, trail condition tags, polls, marketplace, threaded comments, and DMs.
- **Trail TV & Music Mini-Player**: Integrated YouTube PEV video/music deck and live PEV news wire.

### 7. 26+ Visual Themes & 11 Ornamented Holiday Themes
- **Custom Theme Engines**: Rural heritage themes (Barnwood, Sunset Gravel, Pine Holler, Prairie Gold, Creek Mist, Harvest Iron), Tactical Neon, Apex Track, and authentic Retro OS skins (Windows 95, XP Luna, Windows 7 Aero).
- **All 11 Holiday Themes Unlocked Anytime**: Christmas, Halloween, Thanksgiving, 4th of July, New Year's, Valentine's Day, St. Patrick's Day, Easter, Memorial Day, Labor Day, and Veterans Day—complete with DP-scaled top-canopy garlands, twinkling string lights, hanging ornaments, full-screen background art, foreground card corner emblems, and festive nav bar trim.

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **UI & Navigation** | Kotlin, Jetpack Compose Material 3, Navigation Compose (`@Serializable` type-safe routes), Adaptive Layouts |
| **3D PBR Graphics** | Google Filament 1.56.0 (`Engine.Backend.OPENGL`, `gltfio`, `filament-utils`), OpenGL ES 3.0 Fallback |
| **Cloud & Auth** | Firebase Authentication (Google Sign-In via Jetpack `CredentialManager`), Cloud Firestore, Firebase App Check |
| **Generative AI** | Firebase AI Logic (`Firebase.ai` — Gemini multimodal text, vision, voice, and fleet debriefs) |
| **Maps & Live Data** | OSMDroid, FusedLocationProvider, Open-Meteo, RainViewer, Nominatim, Overpass OSM, OSRM, Open-Elevation |
| **Local Storage** | Room Database (offline cache & vault), Jetpack DataStore (preferences) |
| **Camera & Media** | CameraX, Android Photo Picker (`PickVisualMedia`), Media3 / ExoPlayer, Coil |

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17**, **Gradle 9.3.1**, and **Android SDK** (`compileSdk 36`, `targetSdk 36`, `minSdk 24`)
- Firebase project configured with `app/google-services.json` (Authentication + Firestore + AI Logic + App Check)
- Optional API keys configured in `.env` (see `.env.example` — never hardcode keys in source files)

### Commands
```bash
# Build Debug & Release APKs
gradle :app:assembleDebug :app:assembleRelease

# Run Local JVM & Robolectric Unit Tests
gradle :app:testDebugUnitTest
```

### Automated GitHub Actions Releases & GitHub Pages
- **Publish a New Release (`.github/workflows/android-release.yml`)**: Open **Actions → Rural ERides GO — Build, Test & Publish Release → Run workflow** (or push a tag like `git tag v47.0 && git push origin v47.0`) to automatically run unit tests, build versioned Release & Debug APKs, and publish a GitHub Release with attached APKs.
- **Deploy User Website (`.github/workflows/github-pages.yml`)**: Automatically deploys `/docs` to `https://bradley3398.github.io/Rural-Erides-GO/` on pushes to `main` or manual dispatch.

> **Note:** Configure optional CI signing & cloud secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON_BASE64`, `ENV_FILE_BASE64`) in GitHub **Settings → Secrets and variables → Actions**. Never commit credentials or modify keystore files.

---

## 📜 Release History

- **v47.0 (Latest — Build 47)**: Google Filament 1.56.0 OpenGL ES PBR 3D Rig Lab (Vulkan removed, 7-level Studio Softbox IBL & calibrated glTF materials), crash-proof 5-module Ride Inspector + Personal Records Ribbon + live KPI Graph Window Matrix, all 11 Holiday themes upgraded with visible garlands & hanging ornaments, duplicate button cleanup, updated GitHub links, restored GitHub Pages user website, and automated GitHub Actions release & Pages workflows.
- **v44.0**: 50-module Interactive Command Directory, Live 24h Rider Stories, and live HTTP endpoint probes.
- **v39.0–v43.0**: Compartmentalized architecture, Creator Arcade Hub & Gravel Volt 3D Runner, keyless App Check Firebase AI Logic.
- **v36.0–v38.0**: Custom Synthesized AI Voice Studio, zero-latency streaming TTS buffer, tri-channel deep-linked notifications, reactive Telemetry Studio.
- **v18.0–v21.0**: Optical Morse transceiver, 10+ Neon telemetry graphs, zero-trust Firestore sync, 14 HUD skins, exact LiFePO4/NMC battery physics.
- **v7.0–v14.0**: Live CameraX "Bench Eyes" AI Mechanic, 9-DOF AHRS sensor fusion, 17-bay engineering lab, Rider Radar & PTT Walkie-Talkie, RainViewer Doppler radar, multi-PEV Garage, and Rural Visual Themes.

---

## 👑 Creator & Credits

**Lord Bradley Callison** — Founder, Lead Architect & Principal Engineer  
- **GitHub Repository:** [github.com/bradley3398/Rural-Erides-GO](https://github.com/bradley3398/Rural-Erides-GO)  
- **GitHub Pages Site:** [bradley3398.github.io/Rural-Erides-GO](https://bradley3398.github.io/Rural-Erides-GO/)  
- **Issues & Feedback:** [github.com/bradley3398/Rural-Erides-GO/issues](https://github.com/bradley3398/Rural-Erides-GO/issues)
