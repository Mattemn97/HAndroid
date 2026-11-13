plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "it.handroid"
    compileSdk = 35
    ndkVersion = "27.3.13750724" // la stessa di CPython (android-env.sh)

    defaultConfig {
        applicationId = "it.handroid"
        minSdk = 24 // minimo richiesto da CPython per Android
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1-m0"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += "-DANDROID_STL=none" } }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }

    // python.zip è già compresso: niente doppia compressione
    androidResources { noCompress += "zip" }

    packaging { jniLibs { useLegacyPackaging = true } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
