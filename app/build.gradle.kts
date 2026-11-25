import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.volttrail.kqrmvx"
        minSdk = 26
        targetSdk = 36
        versionCode = 46
        versionName = "46.0"

        buildConfigField("boolean", "LEGAL_COMPLIANCE_MODE", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val props = Properties().apply {
                listOf(
                    rootProject.file("keystore.properties"),
                    rootProject.file("key.properties"),
                    rootProject.file("signing.properties"),
                    rootProject.file("release.properties"),
                    file("keystore.properties")
                ).firstOrNull { it.exists() }?.inputStream()?.use { load(it) }
            }

            fun resolveSigningValue(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { key ->
                    (
                        project.findProperty(key)?.toString()
                            ?: System.getProperty(key)
                            ?: System.getenv(key)
                            ?: props.getProperty(key)
                    )?.takeIf { it.isNotBlank() }
                }

            fun resolvePatternValue(pattern: Regex): String? {
                val allSources: List<Map<String, String>> = listOf(
                    project.properties.entries.mapNotNull { (k, v) ->
                        val s = v?.toString()
                        if (s.isNullOrBlank()) null else k to s
                    }.toMap(),
                    System.getenv().filterValues { it.isNotBlank() }
                )
                for (source in allSources) {
                    val match = source.entries.firstOrNull { (k, _) ->
                        !k.contains("DEBUG", ignoreCase = true) && pattern.matches(k)
                    }
                    if (match != null) return match.value
                }
                return null
            }

            val resolvedStoreFile = resolveSigningValue(
                "KEYSTORE_FILE",
                "KEYSTORE_PATH",
                "KEY_STORE_FILE",
                "KEY_STORE_PATH",
                "RELEASE_STORE_FILE",
                "RELEASE_KEYSTORE_PATH",
                "RELEASE_KEYSTORE_FILE",
                "RELEASE_KEYSTORE",
                "ANDROID_KEYSTORE_PATH",
                "ANDROID_KEYSTORE_FILE",
                "UPLOAD_KEYSTORE_PATH",
                "UPLOAD_KEYSTORE_FILE",
                "UPLOAD_STORE_FILE",
                "SIGNING_STORE_FILE",
                "SIGNING_KEYSTORE_PATH",
                "SIGNING_KEYSTORE_FILE",
                "SIGNING_KEYSTORE",
                "STORE_FILE",
                "releaseStoreFile",
                "keystoreFile",
                "keystorePath",
                "storeFile",
                "android.injected.signing.store.file"
            ) ?: resolvePatternValue(Regex("(?i).*(KEY_?STORE|STORE)_(FILE|PATH|LOCATION).*"))
              ?: listOf(
                    rootProject.file("release.keystore"),
                    rootProject.file("upload.keystore"),
                    rootProject.file("release.jks"),
                    rootProject.file("upload.jks"),
                    rootProject.file("keystore.jks"),
                    file("release.keystore"),
                    file("upload.keystore")
                ).firstOrNull { it.exists() }?.absolutePath

            val resolvedStorePassword = resolveSigningValue(
                "KEYSTORE_PASSWORD",
                "KEY_STORE_PASSWORD",
                "RELEASE_STORE_PASSWORD",
                "RELEASE_KEYSTORE_PASSWORD",
                "ANDROID_KEYSTORE_PASSWORD",
                "UPLOAD_KEYSTORE_PASSWORD",
                "UPLOAD_STORE_PASSWORD",
                "SIGNING_STORE_PASSWORD",
                "SIGNING_KEYSTORE_PASSWORD",
                "STORE_PASSWORD",
                "releaseStorePassword",
                "keystorePassword",
                "storePassword",
                "android.injected.signing.store.password"
            ) ?: resolvePatternValue(Regex("(?i).*(KEY_?STORE|STORE)_?PASS(WORD)?.*"))

            val resolvedKeyAlias = resolveSigningValue(
                "KEY_ALIAS",
                "RELEASE_KEY_ALIAS",
                "ANDROID_KEY_ALIAS",
                "UPLOAD_KEY_ALIAS",
                "SIGNING_KEY_ALIAS",
                "releaseKeyAlias",
                "keyAlias",
                "android.injected.signing.key.alias"
            ) ?: resolvePatternValue(Regex("(?i).*KEY_?ALIAS.*"))

            val resolvedKeyPassword = resolveSigningValue(
                "KEY_PASSWORD",
                "RELEASE_KEY_PASSWORD",
                "ANDROID_KEY_PASSWORD",
                "UPLOAD_KEY_PASSWORD",
                "SIGNING_KEY_PASSWORD",
                "releaseKeyPassword",
                "keyPassword",
                "android.injected.signing.key.password"
            ) ?: resolvePatternValue(Regex("(?i).*KEY_?PASS(WORD)?.*"))

            val validExternalKeystore =
                if (resolvedStoreFile != null) {
                    val candidate = File(resolvedStoreFile)
                    val resolvedFile = when {
                        candidate.isAbsolute -> candidate
                        rootProject.file(resolvedStoreFile).exists() -> rootProject.file(resolvedStoreFile)
                        else -> file(resolvedStoreFile)
                    }
                    if (resolvedFile.exists() && !resolvedStorePassword.isNullOrBlank() && !resolvedKeyAlias.isNullOrBlank()) {
                        storeFile = resolvedFile
                        storePassword = resolvedStorePassword
                        keyAlias = resolvedKeyAlias
                        keyPassword = resolvedKeyPassword ?: resolvedStorePassword
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }

            if (!validExternalKeystore) {
                storeFile = rootProject.file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        create("debugConfig") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Room Local SQLite Database
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // CameraX for Live AR HUD & Optical Morse Transceiver
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Firebase & AI Logic
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-appcheck-recaptcha")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.biometric:biometric:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
