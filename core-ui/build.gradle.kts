plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.hain.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Roborazzi screenshots run on the JVM via Robolectric.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// T-018 hard rule: no material3 anywhere in the core-ui dependency tree
// (DESIGN_SYSTEM.md §1, UI_LIBRARIES.md). Even ui-tooling/showkase may not
// sneak it in transitively.
configurations.configureEach {
    exclude(group = "androidx.compose.material3")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // UI libraries (UI_LIBRARIES.md §2) — behaviour/effects only, the look stays ours.
    implementation(libs.compose.unstyled)
    implementation(libs.haze)
    implementation(libs.capsule)
    implementation(libs.backdrop)
    implementation(libs.compose.shimmer)
    implementation(libs.markdown.renderer.core)
    implementation(libs.markdown.renderer.code)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.konfetti.compose)

    // Debug-only component catalogue (Showkase), UI_LIBRARIES.md §2d.
    debugImplementation(libs.showkase)
    kspDebug(libs.showkase.processor)

    // Screenshot testing (record/verify tasks added by the Roborazzi plugin).
    testImplementation(libs.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Screenshot tests are slow and need native graphics: they only run when asked for (T-011).
tasks.withType<Test>().configureEach {
    if (!project.hasProperty("hain.screens")) {
        exclude("**/screens/**")
        failOnNoDiscoveredTests.set(false)
    }
}
