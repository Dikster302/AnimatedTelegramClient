plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.dikster.specialgram"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dikster.specialgram"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "0.4.1"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { cppFlags += listOf("-std=c++17") } }
        buildConfigField("String", "TELEGRAM_API_ID", "\"${providers.gradleProperty("TELEGRAM_API_ID").orElse(providers.environmentVariable("TELEGRAM_API_ID").orElse("")).get()}\"")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${providers.gradleProperty("TELEGRAM_API_HASH").orElse(providers.environmentVariable("TELEGRAM_API_HASH").orElse("")).get()}\"")
    }
    signingConfigs {
        create("release") {
            storeFile = file(providers.environmentVariable("ANDROID_KEYSTORE_PATH").orElse("release-key.jks").get())
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orElse("").get()
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orElse("specialgram").get()
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orElse("").get()
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    buildFeatures { buildConfig = true }
    externalNativeBuild { cmake { path = file("CMakeLists.txt"); version = "3.22.1" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.zxing:core:3.5.3")
}
