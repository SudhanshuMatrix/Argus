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
    namespace = "com.ajayinnovations.argus"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // flutter_local_notifications needs core library desugaring
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.ajayinnovations.argus"
        // Android 8+, like the original app; the adaptive icon needs 26
        minSdk = 26
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
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
