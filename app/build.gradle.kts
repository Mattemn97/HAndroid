plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// In CI VERSION_CODE è il numero del run: ogni release si installa sopra la precedente.
val vc = (System.getenv("VERSION_CODE") ?: "1").toInt()
val keystore = System.getenv("KEYSTORE_FILE")

android {
    namespace = "it.handroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.handroid"
        minSdk = 29
        targetSdk = 35
        versionCode = vc
        versionName = "0.1.$vc"
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        create("release") {
            if (keystore != null) {
                storeFile = file(keystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Senza keystore (build locale o PR) si firma con la chiave di debug.
            signingConfig = signingConfigs.getByName(if (keystore != null) "release" else "debug")
        }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }

    // Le .so vanno estratte su disco: Python le carica per percorso.
    packaging { jniLibs { useLegacyPackaging = true } }
    androidResources { noCompress += "zip" }
    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // I test JVM chiamano codice che usa android.util.Log: restituisce valori di default.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

dependencies {
    // Solo per i test JVM: org.json vero al posto degli stub di android.jar.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
