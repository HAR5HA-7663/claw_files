plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ppl.tracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ppl.tracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Kotlin sources live under src/main/kotlin
    sourceSets["main"].java.srcDirs("src/main/kotlin")
}

dependencies {
    // Intentionally no AndroidX / Compose — this app uses only the
    // framework View toolkit so it builds against a bare SDK.
}
