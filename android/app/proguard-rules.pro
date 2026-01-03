# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes used for Room
-keep class com.routinechart.core.data.local.room.entities.** { *; }

# Keep Firestore model classes
-keep class com.routinechart.core.data.remote.firebase.dto.** { *; }

# Firebase
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Retrofit/OkHttp (if added later)
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ULID
-keep class io.azam.ulidj.** { *; }

