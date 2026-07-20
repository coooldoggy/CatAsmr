# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep app-specific classes
-keep class com.coooldoggy.catasmr.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** *ForKotlinSerialization(...);
}
-dontwarn kotlinx.serialization.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Jetpack/AndroidX
-keepclasseswithmembernames class androidx.** { public *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Data classes
-keep class com.coooldoggy.catasmr.**.** { *; }