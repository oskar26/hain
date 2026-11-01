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
        versionName = "0.1.0"
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
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
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
