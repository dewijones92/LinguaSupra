import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dewijones.linguasupra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dewijones.linguasupra"
        minSdk = 33
        targetSdk = 35
        // CI overrides via -PreleaseVersionCode / -PreleaseVersionName so each
        // tagged build has a unique, monotonically increasing versionCode.
        versionCode = providers.gradleProperty("releaseVersionCode")
            .map { it.toInt() }.getOrElse(1)
        versionName = providers.gradleProperty("releaseVersionName").orNull ?: "0.1.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Stable debug keystore committed to the repo so every CI run + every
        // local build signs with the same key. Without this, the auto-release
        // workflow regenerates a fresh debug keystore each run, which means
        // every new release APK has a different signing fingerprint and the
        // user has to uninstall to update. Debug-only — no Play Store risk.
        getByName("debug") {
            storeFile = file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Auto-release ships the debug variant for sideload, so reuse the
            // stable debug signing config here too. If we ever publish to
            // Play Store this needs its own real keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    sourceSets {
        getByName("androidTest") {
            assets.directories.add(layout.projectDirectory.dir("schemas").asFile.absolutePath)
            kotlin.srcDir("src/sharedTest/java")
        }
        getByName("test") {
            kotlin.srcDir("src/sharedTest/java")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)

    // Compose (BOM-managed)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Gemini Nano (on-device) via Google AI Edge SDK
    implementation(libs.google.ai.edge.aicore)

    // Unit tests (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.room.testing)

    // Instrumented (feature) tests
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.room.testing)
}
