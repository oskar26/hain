plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.hain.docs"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        // keep rules for pdfbox-android and ONNX Runtime when the app is shrunk
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // the PDF test reads a fixture with pdfbox-android, which needs its assets (Robolectric)
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    // T-120: PDF text layer (Apache-2.0); page pictures come from android.graphics.pdf.PdfRenderer
    implementation(libs.pdfbox.android)
    // T-121: PP-OCRv5 runs on ONNX Runtime (MIT), pinned to 1.28.0 (1.29+ adds Microsoft telemetry).
    // The models are not in the APK: an optional download of ~13.9 MB after the user says yes.
    implementation(libs.onnxruntime.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
