# ProGuard rules for Hermes
-keep class com.example.** { *; }
-keep class com.example.database.** { *; }
-keep class com.example.api.** { *; }
-keep class com.example.viewmodel.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.ipc.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# MultiDex
-keep class androidx.multidex.MultiDexApplication { *; }

# Hermes specific
-keep class com.example.HermesApplication { *; }
-keep class com.example.MainActivity { *; }