plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kamnaobed.tt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kamnaobed.tt"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    // Pevný podpisový kľúč v projekte: každý build (aj z GitHubu) má rovnaký podpis,
    // takže novú verziu nainštalujete cez starú bez odinštalovania.
    // Pre osobné použitie OK; na Google Play si vytvorte vlastný, tajný kľúč.
    signingConfigs {
        create("fixed") {
            storeFile = file("kamnaobed.keystore")
            storePassword = "kamnaobed"
            keyAlias = "kamnaobed"
            keyPassword = "kamnaobed"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("fixed")
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

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jsoup:jsoup:1.18.3")
}
