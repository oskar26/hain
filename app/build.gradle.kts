plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "app.hain"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.hain"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = (providers.gradleProperty("hain.versionCode").orNull ?: "1").toInt()
        // T-215: one place for the version name (gradle.properties, docs/workflow/VERSIONING.md)
        versionName = providers.gradleProperty("hain.versionName").orNull ?: "0.1.4"
        // commit and build date for "Über Hain"; CI passes them (-Phain.commit, -Phain.buildDate)
        val commit = providers.gradleProperty("hain.commit").orNull ?: "local"
        val buildDate = providers.gradleProperty("hain.buildDate").orNull ?: ""
        buildConfigField("String", "GIT_SHA", "\"$commit\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // Shared debug key (app/signing/hain-debug.keystore, public by design) so CI APKs update each other.
    signingConfigs {
        getByName("debug") {
            storeFile = file("signing/hain-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        // Test builds get their own id so they install beside any other Hain build (store, F-Droid, own PC build).
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // 2d-2 speed: the APK that CI builds for phones (`-Phain.fastDebug=true`) is not debuggable.
            // A debuggable app runs Compose without ART optimisations and ignores the baseline
            // profiles of Compose & co. — several times slower. Local builds stay debuggable.
            isDebuggable = providers.gradleProperty("hain.fastDebug").orNull != "true"
        }
        // T-190 speed: R8 shrinks, optimises and renames the code and drops unused resources. What
        // native code or the database reach by name is kept in proguard-rules.pro; the CI job
        // "release-smoke" (screens.yml) installs this APK on an emulator and checks that it starts.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Release APKs stay unsigned (F-Droid / a store signs them). Only for the CI smoke test or
            // your own phone: -Phain.signReleaseWithDebugKey=true signs with the public debug key.
            if (providers.gradleProperty("hain.signReleaseWithDebugKey").orNull == "true") {
                signingConfig = signingConfigs.getByName("debug")
            }
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
        // ggml loads its CPU backends with dlopen from nativeLibraryDir (GGML_BACKEND_DL).
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.coroutines.android)
    // T-056: app lock with fingerprint / phone PIN (BiometricPrompt needs a FragmentActivity)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    // T-190: compiles the baseline profile (src/main/baselineProfiles) on phones without Play
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // All modules
    implementation(project(":core-ui"))
    implementation(project(":core-common"))
    implementation(project(":core-data"))
    implementation(project(":core-llm"))
    implementation(project(":core-tools"))
    implementation(project(":core-memory"))
    implementation(project(":core-docs"))
    implementation(project(":core-voice"))
    implementation(project(":core-cards"))
    implementation(project(":core-models"))
    implementation(project(":feature-chat"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-memory"))
    implementation(project(":feature-news"))
    implementation(project(":feature-image"))
    implementation(project(":feature-onboarding"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
