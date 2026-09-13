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
        versionCode = 9
        versionName = "0.7.0"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".s25tex070"
            versionNameSuffix = "-s25test"
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
