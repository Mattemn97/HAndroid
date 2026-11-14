import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma dei rilasci: keystore.properties (fuori da git) con storeFile, storePassword, keyAlias, keyPassword.
// Senza, assembleRelease produce un APK non firmato. Vedi scripts/rilascio.sh.
val firma = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

android {
    namespace = "it.handroid"
    compileSdk = 35
    ndkVersion = "27.3.13750724" // la stessa di CPython (android-env.sh)

    defaultConfig {
        applicationId = "it.handroid"
        minSdk = 24 // minimo richiesto da CPython per Android
        targetSdk = 35
        // la versione si confronta con il tag dei rilasci su GitHub (Maintenance.newerRelease)
        versionCode = 4
        versionName = "0.3.0-m3"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += "-DANDROID_STL=none" } }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }

    signingConfigs {
        if (firma != null) create("rilascio") {
            storeFile = file(firma.getProperty("storeFile"))
            storePassword = firma.getProperty("storePassword")
            keyAlias = firma.getProperty("keyAlias")
            keyPassword = firma.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            if (firma != null) signingConfig = signingConfigs.getByName("rilascio")
        }
    }

    // python.zip è già compresso: niente doppia compressione
    androidResources { noCompress += "zip" }

    packaging { jniLibs { useLegacyPackaging = true } }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    testImplementation("junit:junit:4.13.2")
    // org.json di Android nei test JVM è solo uno stub: serve quella vera (AddonTest)
    testImplementation("org.json:json:20240303")
}
