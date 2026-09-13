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
        versionCode = 6
        versionName = "0.5.1"
    }
    buildTypes {
        getByName("debug") {
            // Installable next to older Brumes test APKs, even if their debug signing key differs.
            applicationIdSuffix = ".s25test"
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
