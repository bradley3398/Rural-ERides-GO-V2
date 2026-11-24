# R8 Rules for Rural ERides GO
# Keeps code shrinking and resource shrinking while disabling slow multi-pass bytecode
# optimization and obfuscation to prevent 504 gateway timeouts during AAB bundling.
-dontoptimize
-dontobfuscate
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# App Data Models (Firebase Firestore toObject / Room Entities / JSON & Enums)
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Firebase Firestore annotations & serialization
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.DocumentId <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.ServerTimestamp <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

# Google Filament 3D Engine (Vulkan / OpenGL / gltfio / utils JNI native bindings)
-keep class com.google.android.filament.** { *; }
-keepclassmembers class com.google.android.filament.** {
    native <methods>;
    <init>(...);
}

# Android YouTube Player & WebView JavascriptInterface bridges
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# osmdroid tile providers & reflection-instantiated map sources
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Room generated implementations & TypeConverters
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class com.example.data.Offline* { *; }

# Jetpack Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Android hardware service & BroadcastReceiver entry points
-keep class com.example.hardware.AhrsSensorService { *; }

# Firebase AI Logic, App Check & reCAPTCHA Enterprise (critical under R8 full mode)
-keep class com.google.firebase.ai.** { *; }
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.android.recaptcha.** { *; }
-keep class com.google.android.gms.recaptcha.** { *; }
-dontwarn com.google.android.recaptcha.**
-dontwarn com.google.firebase.ai.**

# Ktor HTTP client & engine ServiceLoader providers (used internally by Firebase AI Logic)
-keep class io.ktor.** { *; }
-keep class * implements io.ktor.client.HttpClientEngineFactory { *; }
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization (used internally by Firebase AI Logic request/response models)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.google.firebase.ai.**$$serializer { *; }
-keepclassmembers class com.google.firebase.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.google.firebase.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    static ** Companion;
}
-keep class **$$serializer { *; }

# Coil & OkHttp image/network pipeline under R8 full mode
-keep class coil.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**

# Kotlin coroutines & serialization metadata safety under R8 full mode
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.slf4j.**
-dontwarn java.lang.instrument.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
