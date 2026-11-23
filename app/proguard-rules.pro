# R8 Full Mode Rules for Rural ERides GO
# Keeps full code shrinking, optimization, and resource shrinking while avoiding
# heavy obfuscation mapping bloat during Play Console bundle uploads.
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

# Kotlin coroutines & serialization metadata safety under R8 full mode
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.slf4j.**
-dontwarn java.lang.instrument.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
