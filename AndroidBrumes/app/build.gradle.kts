plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mumu.brumes"
    compileSdk = 35
    signingConfigs {
        create("brumes") {
            storeFile = file("../brumes.keystore")
            storePassword = "brumes2026"
            keyAlias = "brumes"
            keyPassword = "brumes2026"
        }
    }
    defaultConfig {
        applicationId = "com.mumu.brumes"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.7.2"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".s25tex070"
            versionNameSuffix = "-s25test"
            signingConfig = signingConfigs.getByName("brumes")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
