import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing: android/key.properties locally, or ARGUS_KEYSTORE_* in CI (see
// .github/workflows/build-apk.yml). Without either, release builds use the debug key.
val keyProperties = Properties().apply {
    val file = rootProject.file("key.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signing(name: String, env: String): String? = keyProperties.getProperty(name) ?: System.getenv(env)

android {
    namespace = "com.sudhanshumatrix.argus"
    // permission_handler_android needs 37
    compileSdk = maxOf(flutter.compileSdkVersion, 37)
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // flutter_local_notifications needs core library desugaring
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.sudhanshumatrix.argus"
        // Android 11+: the on-device AI (LiteRT-LM) needs API 30 and 64-bit ARM
        minSdk = 30
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // Three APKs from one code base (see lib/app/edition.dart):
    //  offline: no internet at all, Gemma inside the APK (src/offline/assets/models/)
    //  online:  downloads Gemma once and receives live scam alerts from the Argus server
    //  demo:    for recording demos: warnings and live alerts only, no SMS or WhatsApp reading
    flavorDimensions += "edition"
    productFlavors {
        create("offline") {
            dimension = "edition"
            resValue("string", "app_name", "Argus Offline")
        }
        create("online") {
            dimension = "edition"
            applicationIdSuffix = ".online"
            resValue("string", "app_name", "Argus")
        }
        create("demo") {
            dimension = "edition"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            resValue("string", "app_name", "Argus Demo")
        }
    }

    buildFeatures {
        // The flavors set their launcher name with resValue, which AGP 9 turns off by default
        resValues = true
    }

    // The model barely compresses; storing it raw makes the first-launch copy fast
    androidResources {
        noCompress += "litertlm"
    }

    signingConfigs {
        val storeFile = signing("storeFile", "ARGUS_KEYSTORE_PATH")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = signing("storePassword", "ARGUS_KEYSTORE_PASSWORD")
                keyAlias = signing("keyAlias", "ARGUS_KEY_ALIAS")
                keyPassword = signing("keyPassword", "ARGUS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            // proguard-rules.pro strips plugin debug logging, which could include message text
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

flutter {
    source = "../.."
}
