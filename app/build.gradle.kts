import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from keystore.properties (git-ignored). Without it, release builds are unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.ajayinnovations.argus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ajayinnovations.argus"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Extra notification sources to watch, comma-separated (dev only: the adb shell)
        buildConfigField("String", "EXTRA_WATCHED_PACKAGES", "\"\"")
        // Setup pictures not added yet show a labelled empty frame (dev) or nothing (release)
        buildConfigField("boolean", "SHOW_IMAGE_SLOTS", "false")

        // Phones are ARM. LiteRT-LM's x86_64 library alone is 26 MB. armeabi-v7a stays so
        // 32-bit phones still install and see the wizard's "can't run here" explanation.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // All flavors share one applicationId, so a phone can only have one Argus
    flavorDimensions += "model"
    productFlavors {
        create("standard") { dimension = "model" }
        create("lite") {
            dimension = "model"
            versionNameSuffix = "-lite"
        }
        create("dev") {
            dimension = "model"
            versionNameSuffix = "-dev"
            // Lets `adb shell cmd notification post` test the WhatsApp listener path
            buildConfigField("String", "EXTRA_WATCHED_PACKAGES", "\"com.android.shell\"")
            buildConfigField("boolean", "SHOW_IMAGE_SLOTS", "true")
        }
    }

    // dev: the synthetic test samples (src/test/resources/samples) ship as assets for
    // "Run all samples" on the debug screen. One copy, so tests and the phone can't drift.
    sourceSets {
        getByName("dev") {
            assets.srcDir("src/test/resources")
        }
    }

    // The model barely compresses; storing it raw makes the first-launch copy faster
    // and lets the app read its exact size
    androidResources {
        noCompress += "litertlm"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: shrink, optimise and obfuscate. The stable switch: AGP 9.2's optimization {}
            // block needs the experimental android.r8.gradual.support flag.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "src/main/keepRules/rules.keep")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.litertlm.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
