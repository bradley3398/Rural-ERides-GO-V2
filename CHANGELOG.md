# Changelog — Rural ERides GO

All notable changes to **Rural ERides GO** (`com.aistudio.volttrail.kqrmvx`) are documented in this file.

## [47.0] (Build 47) — November 2026
### Filament OpenGL PBR 3D Lab, Crash-Proof Ride Inspector, Holiday Ornaments & Release Workflows
- **Google Filament 1.56.0 OpenGL ES PBR 3D Rig Lab**: Removed Vulkan PBR completely; upgraded procedural `.glb` PEV models with 7-level roughness-prefiltered Studio Softbox IBL, ACES Filmic tone mapping, FXAA + SSAO, and automatic OpenGL ES fallback.
- **Upgraded Telemetry Studio & Crash-Proof Ride Inspector**: Replaced the crashing stacked Per-Ride Deep Dive with the focused 5-module Ride Inspector (`Speed Curve`, `Climb Profile`, `Energy & Pack`, `GPS Route`, `Replay & AI`), added a 1-tap Personal Records Ribbon, live KPI subtitles on all 11 Fleet Graph Windows, and NaN/zero-width Canvas crash guards.
- **Holiday Themes Ornaments & Decorations**: Unlocked all 11 Holiday themes year-round with DP-scaled top-canopy garlands, twinkling string lights, hanging ornaments, foreground card corner emblems, and festive nav bar trim.
- **UI Hygiene, GitHub Pages & Automated Release Workflows**: Removed duplicate action buttons across 3D Lab, Ride Logbook, and Graph modals; updated all links to `github.com/bradley3398/Rural-Erides-GO`; restored `README.md`, user-facing GitHub Pages (`/docs/index.html`), and GitHub Actions release & Pages workflows.

## [46.0] (Build 46) — November 2026
### Release Bundling, Performance & Local Vault Hardening
- **Direct SQLite Local Vault (`rural_erides_vault.db`)**: Replaced the KSP/Room annotation-processed offline vault with a schema- and API-compatible `SQLiteOpenHelper` engine with WAL mode and reactive `Flow` updates, preserving 100% compatibility with existing on-device rides and Co-Pilot chat history.
- **Release Bundling Acceleration**: Removed the KSP plugin and Room compiler overhead from release builds, eliminating the 35s+ `:app:kspReleaseKotlin` bottleneck and preventing gateway timeouts during Play Console AAB bundling.
- **Play Console Release Bump**: Updated build metadata to `versionCode = 46` (`versionName = "46.0"`) while preserving the registered Play Console package ID (`com.aistudio.volttrail.kqrmvx`).
- **Public Release Secret Hygiene**: Kept all private third-party credentials excluded from client `BuildConfig`, relying on Firebase AI Logic + App Check, Cloud Firestore, and keyless live feeds for public builds.

## [38.0] (Build 38) — November 2026
### Custom Synthesized Voice Studio & Zero-Latency TTS Pre-Load Buffer
- **Custom AI Voice Studio**: Dedicated screen for synthesizing bespoke Co-Pilot voices, importing reference audio (`*.wav`/`*.mp3`), recording microphone samples, and tuning pitch, speech rate, and style directives.
- **Zero-Latency Streaming Pre-Load Buffer**: Splits sentence boundaries as Gemini streams and pre-renders fragments into an async PCM buffer for ~85ms speech onset with gapless playback.
- **Neural Bases & Hardware DSP Profiles**: 10 Gemini neural voice bases and 8 DSP filters (Deep Barn, Warm Porch, Tactical Crisp, CB Radio, and more) applied across Co-Pilot chat, navigation alerts, and maintenance briefings.

## [37.0] (Build 37) — November 2026
### Live Push Notifications & Tactile Accessibility Audit
- **Tri-Channel Push Notifications**: Real-time notifications for AI Co-Pilot & Mechanic insights, Community Board dispatches, and Direct Messages (DMs) with deep-link tab and thread routing.
- **UI Debounce & Tactile Press Audit**: Added 400ms duplicate-tap guards, accessible `>=48dp` touch targets, Material 3 ripples, and tactile press scaling across primary controls.
- **Android 13+ Notification Consent**: Added `POST_NOTIFICATIONS` runtime flow with built-in channel test dispatchers.

## [36.0] (Build 36) — November 2026
### Filament 3D Engine Lifecycle Manager & Reactive Telemetry Studio
- **Filament 3D Lifecycle Management**: Explicit engine, swap chain, and renderer teardown on viewport pause/destroy to eliminate native GPU memory leaks.
- **Reactive Flow Telemetry Studio**: Asynchronous Kotlin `Flow` dataset pipeline in `RideTelemetryViewModel` for responsive chart loading with null/NaN coordinate safety bounds.

## [21.0] (Build 21) — November 2026
### Optical Morse Transceiver & Telemetry Studio Pro
- **Morse Code Flashlight TX & CameraX RX**: Pulse trail messages via rear LED and decode incoming optical Morse signals using live CameraX luminance analysis.
- **Ride Telemetry Charts Pro**: 10 interactive Neon analytics graphs, synchronized timeline playback scrubber, and fleet benchmark spider chart.

## [20.0] (Build 20) — October 2026
### Cloud Persistence & Zero-Trust Firebase Firestore
- Configured Firebase Firestore and Authentication via Jetpack Credential Manager (Google Sign-In).
- Added live multiplayer Rider Radar, shared route waypoints, direct team messaging, and offline local cache fallback.

## [19.0] (Build 19) — September 2026
### Cockpit HUD & Offline OSMDroid Map Engine
- Upgraded Cockpit HUD with 14 switchable skins, RainViewer live Doppler radar overlays, vector tile caching, voice command hotwords, and automated battery sag compensation.

## [18.0] (Build 18) — August 2026
### Initial Architecture & PEV Physics
- Established Jetpack Compose Material 3 architecture, PEV battery chemistry simulation (LiFePO4, NMC, Solid-State), motor thermal models, and diagnostic wrench/torque/gear calculators.
