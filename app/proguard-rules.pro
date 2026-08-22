# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 🚀 SLF4J (Fixes the missing StaticLoggerBinder error)
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }
-keepclassmembers class org.slf4j.** { *; }

# 🚀 ZegoCloud
-keep class im.zego.** { *; }
-keep class **.zego.** { *; }
-dontwarn im.zego.**

# 🚀 Supabase & Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Cloudinary & OneSignal (To prevent future crashes)
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# Coil
-keep class coil.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKd
-keep,allowoptimization class kotlinx.serialization.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Room
-keep class androidx.room.** { *; }

# Hilt
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedEntryPoint
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedComponentManager
