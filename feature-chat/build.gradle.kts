plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.hain.feature.chat"
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
    // T-011 screenshots: Roborazzi on the JVM via Robolectric (CI workflow screens.yml)
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":core-data"))
    implementation(project(":core-llm"))
    implementation(project(":core-tools"))
    implementation(project(":core-memory"))
    implementation(project(":core-cards"))
    implementation(project(":core-models"))
    // Häppchen 10: dictation (Moonshine) and read aloud
    implementation(project(":core-voice"))
    // Häppchen 9: attach PDFs, photos and text files (Meine Dateien, search_document)
    implementation(project(":core-docs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    // T-058: the scroll pill is frosted glass over the messages (hainGlass takes a HazeState)
    implementation(libs.haze)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // T-011 screenshots (only run with -Phain.screens, see .github/workflows/screens.yml)
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
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
