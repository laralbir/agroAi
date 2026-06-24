# AgroAI ProGuard rules

# Keep domain models
-keep class com.laralnet.agroai.**.domain.model.** { *; }

# Keep Room entities
-keep class com.laralnet.agroai.**.infrastructure.persistence.entity.** { *; }

# Keep Retrofit/OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Keep AEMET API models
-keep class com.laralnet.agroai.weather.infrastructure.api.** { *; }

# Keep MediaPipe
-keep class com.google.mediapipe.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
