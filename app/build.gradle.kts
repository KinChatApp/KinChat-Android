import java.util.Properties
import java.io.FileInputStream

// local.properties থেকে ডেটা রিড করার সেটআপ
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    // OneSignal Plugin
    id("com.onesignal.androidsdk.onesignal-gradle-plugin") version "0.14.0"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.kinchat.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kinchat.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 🚀 অপ্টিমাইজেশন ১: শুধু প্রয়োজনীয় ভাষা রাখুন (ইংরেজি ও বাংলা)।
        resourceConfigurations += setOf("en", "bn")

        // 🚀 অপ্টিমাইজেশন ২: নেটিভ লাইব্রেরি (.so files) ফিল্টারিং।
        ndk {
            abiFilters.add("arm64-v8a")
        }

        // Supabase Config
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        // Upstash Redis Config
        buildConfigField("String", "UPSTASH_REDIS_REST_URL", "\"${localProperties.getProperty("UPSTASH_REDIS_REST_URL", "")}\"")
        buildConfigField("String", "UPSTASH_REDIS_REST_TOKEN", "\"${localProperties.getProperty("UPSTASH_REDIS_REST_TOKEN", "")}\"")

        // ZegoCloud Config
        buildConfigField("Long", "ZEGO_APP_ID", "${localProperties.getProperty("ZEGO_APP_ID", "0")}L")
        buildConfigField("String", "ZEGO_APP_SIGN", "\"${localProperties.getProperty("ZEGO_APP_SIGN", "")}\"")

        // Dev Quick Login Credentials
        buildConfigField("String", "DEV_USER1_PHONE", "\"${localProperties.getProperty("DEV_USER1_PHONE", "01935555298")}\"")
        buildConfigField("String", "DEV_USER1_EMAIL", "\"${localProperties.getProperty("DEV_USER1_EMAIL", "towkirahmed546@gmail.com")}\"")
        buildConfigField("String", "DEV_USER2_PHONE", "\"${localProperties.getProperty("DEV_USER2_PHONE", "01325887577")}\"")
        buildConfigField("String", "DEV_USER2_EMAIL", "\"${localProperties.getProperty("DEV_USER2_EMAIL", "towkir546@gmail.com")}\"")
        buildConfigField("String", "DEV_TEST_OTP", "\"${localProperties.getProperty("DEV_TEST_OTP", "123456")}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.2.0")
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:functions-kt:2.2.3")
    implementation("io.ktor:ktor-client-okhttp:2.3.9")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.cloudinary:cloudinary-android:2.5.0")
    implementation("im.zego:zego_uikit_prebuilt_call_android:+")

    // OneSignal SDK
    implementation("com.onesignal:OneSignal:5.1.8")

    // AutoStarter Library
    implementation("com.github.judemanutd:autostarter:1.1.0")
    
    // 🚀 NEW: Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
}
