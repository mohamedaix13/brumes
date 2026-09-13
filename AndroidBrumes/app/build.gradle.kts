plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mumu.brumes"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mumu.brumes"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.5.2"
    }
    buildTypes {
        getByName("debug") {
            // Unique test package avoids signature conflicts with older GitHub Actions debug APKs.
            applicationIdSuffix = ".s25fix052"
            versionNameSuffix = "-s25fix"
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
