plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.hain.tools"
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

}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-data"))
    api(project(":core-llm"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    // T-237: offline Wikipedia packs are zstd-compressed (only the Android AAR, never the desktop jar)
    implementation(variantOf(libs.zstd.jni) { artifactType("aar") })

    testImplementation(libs.junit)
    testImplementation(libs.kxml2)
    testImplementation(libs.kotlinx.coroutines.test)
}
