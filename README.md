<p align="center">
  <img src="public/assets/aistudio/icon_75e30694.png" alt="Rural Erides GO Icon" width="110" />
</p>

<h1 align="center">Rural Erides GO</h1>

<p align="center">
  <strong>Country Backroads PEV Cockpit • GPS Mission HUD • 3D Rig Lab • Rider Radar • Porch Talk</strong>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.aistudio.volttrail.kqrmvx">
    <img src="https://img.shields.io/badge/Google%20Play-Rural%20Erides%20GO-82A527?style=for-the-badge&logo=google-play&logoColor=white" alt="Get it on Google Play" />
  </a>
  <img src="https://img.shields.io/badge/Release-v46.0%20(Build%2046)-E0A23B?style=for-the-badge" alt="Release v46.0" />
  <img src="https://img.shields.io/badge/Android-8.0%20--%2016-1A261D?style=for-the-badge&logo=android&logoColor=A3CC38" alt="Android 8.0 to 16" />
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-C44B33?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin & Jetpack Compose" />
</p>

---

**Rural Erides GO** is an Android cockpit and backcountry companion built for Electric Unicycles (EUCs), Sur-Ron / e-motos, hyperscooters, and gravel e-bikes. Designed for county roads, farm lanes, and off-grid trails, it combines real-time hardware telemetry, multi-engine 3D vehicle inspection, live Rider Radar, AI mechanical diagnostics, and an automatic seasonal holiday theme engine.

---

## App Previews

### Welcome Gateway, Backroad Co-Pilot & AR Mechanic
<p align="center">
  <img src="public/assets/aistudio/Screenshot_20261124_180705_Rural_ERides_GO_b2c51cae.jpg" alt="Golden Trail Welcome and Pilot Sign-In" width="215" />
  &nbsp;&nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175645_Rural_ERides_GO_2add819f.jpg" alt="Backroad Co-Pilot Pre-Ride Flight Check" width="215" />
  &nbsp;&nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175606_Rural_ERides_GO_d4111fee.jpg" alt="Barn and Bench AR Mechanic AI Diagnostics" width="215" />
</p>

### Barn Spec Engine (50 Rigs) & Tire Parts Finder
<p align="center">
  <img src="public/assets/aistudio/Screenshot_20261124_175615_Rural_ERides_GO_139680c7.jpg" alt="Barn Spec Engine 50 Rigs" width="180" />
  &nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175620_Rural_ERides_GO_8551f4c2.jpg" alt="Live Rig Specs and Verified In-Stock Stores" width="180" />
  &nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175630_Rural_ERides_GO_8d6e02bd.jpg" alt="Barn Shop Street and Fat Tires" width="180" />
  &nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175634_Rural_ERides_GO_946daaf2.jpg" alt="Barn Shop EUC and Heavy eBike Tires" width="180" />
</p>

### Porch Talk Community, Trail Crew & Zero-Bot Rider Comms
<p align="center">
  <img src="public/assets/aistudio/Screenshot_20261124_175654_Rural_ERides_GO_7e0e290e.jpg" alt="Porch Talk 35 Rural Post Templates" width="215" />
  &nbsp;&nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175732_Rural_ERides_GO_6233df3d.jpg" alt="Porch Feed and Active Trail Crew" width="215" />
  &nbsp;&nbsp;
  <img src="public/assets/aistudio/Screenshot_20261124_175701_Rural_ERides_GO_552e49f0.jpg" alt="Porch DMs and Live Rider Calls" width="215" />
</p>

---

## Key Features

- **Mission-Locked GPS HUD & Speed Limits** — When you start a ride mission, the speedometer and live speed-limit indicator lock against accidental scrolling while keeping essential ride telemetry and the integrated YouTube Music deck accessible.
- **Multi-Engine 3D Rig Lab (30+ Models)** — Inspect PEV models and chassis blueprints using **OpenGL ES 3.0 PBR**, **WebGL 2.0 GLB**, **Blueprint CAD**, and **Compose 3D GPU Mesh** engines.
- **100% Compose Vector Ride Maps** — Review recorded routes with fast vector trail maps, elevation and speed telemetry curves, battery Wh/mi efficiency, and shareable mission cards.
- **Universal Seasonal Holiday Theme Engine** — Automatically decorates every screen with ornaments, banners, and badges for **New Year's Day**, **Valentine's Day**, **St. Patrick's Day**, **Easter**, **Memorial Day**, **4th of July**, **Labor Day**, **Halloween**, **Veterans Day**, **Thanksgiving**, and **Christmas**.
- **Privacy-First Rider Radar & Porch Talk** — Your live location beacon stays **off by default** until you choose to broadcast. Coordinate trail rides with P2P Walkie-Talkie and share updates on the Porch Talk community board.
- **Gemini Vision AR Mechanic & 80+ Calculators** — Camera-assisted mechanical troubleshooting, tire/battery/range calculators, maintenance reminders, and a live versioned Change Log & Diagnostics hub in **About App**.
- **Cloud Sync & Dual Auth** — Google Sign-In via Jetpack Credential Manager backed by Firebase Firestore & Firebase AI Logic.

---

## Download & Install

### Option 1: Public Download Landing Page (GitHub Pages)
Visit the public Rural Erides GO landing page at [https://bradley3398.github.io/Rural-ERides-GO-V2/](https://bradley3398.github.io/Rural-ERides-GO-V2/) (deployed from [`public/index.html`](public/index.html)). It automatically pulls the latest signed APK, file size, and changelog from GitHub Releases.

### Option 2: Direct APK from GitHub Releases
1. Open the [Releases page](https://github.com/bradley3398/Rural-ERides-GO-V2/releases/latest) on [github.com/bradley3398/Rural-ERides-GO-V2](https://github.com/bradley3398/Rural-ERides-GO-V2) and download the latest signed `app-release.apk`.
2. Tap the downloaded APK on your Android device (Android 8.0+).
3. Allow your browser to install apps if prompted, then launch **Rural Erides GO**.

### Option 3: Google Play Store
- Package ID: `com.aistudio.volttrail.kqrmvx`
- Store listing: [Rural Erides GO on Google Play](https://play.google.com/store/apps/details?id=com.aistudio.volttrail.kqrmvx)

---

## Automated CI/CD: Signed Releases, Google Play & Public Pages

This repository includes two GitHub Actions workflows:

### 1. Signed Android Release & Automatic Google Play Upload
Workflow: [`.github/workflows/android-release-sign.yml`](.github/workflows/android-release-sign.yml)

Trigger a public release to **both GitHub Releases and Google Play Console** either by pushing a version tag (`git tag v46.0 && git push origin v46.0`) or by clicking **Actions → Android Signed Release (Google Play) → Run workflow** (`v46.0`). The workflow automatically:
1. Decodes your production upload keystore from GitHub Secrets (never committed to the repo).
2. Builds and signs both the release **App Bundle (`.aab`)** and **APK (`.apk`)**.
3. Verifies signatures with `jarsigner` and `apksigner` and blocks debug-signed builds.
4. Publishes a **public GitHub Release** with the signed `.apk` and `.aab` attached (automatically updating the GitHub Pages download button).
5. Uploads the signed `.aab` automatically to **Google Play Console** (`com.aistudio.volttrail.kqrmvx`, `production` track).

#### Required GitHub Actions Secrets
Configure these under **Settings → Secrets and variables → Actions**:

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded production upload `.jks` / `.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Upload key alias inside the keystore |
| `KEY_PASSWORD` | Private key password for `KEY_ALIAS` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Play Console API Service Account JSON key |

### 2. Public Rural Landing Page (GitHub Pages)
Workflow: [`.github/workflows/deploy-github-pages.yml`](.github/workflows/deploy-github-pages.yml)

Automatically publishes [`public/`](public/) to your public GitHub Pages site whenever commits are pushed to `main`/`master` or a `v*` release tag is created.

---

## Building Locally

### Requirements
- **JDK 17**
- **Android SDK** (Compile/Target SDK 36, Min SDK 26)
- **Gradle 9.3.1**

### Build Commands
```bash
# Build debug APK
gradle :app:assembleDebug

# Build signed release App Bundle (supplying release signing env vars)
KEYSTORE_FILE=/path/to/upload.jks \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
gradle :app:bundleRelease
```

---

## License

Copyright © 2026 Rural Erides GO.
