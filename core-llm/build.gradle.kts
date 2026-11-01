plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.hain.llm"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // T-030: native llama.cpp bridge. `-Phain.native=false` skips it (machines without an NDK).
    if (providers.gradleProperty("hain.native").orNull != "false") {
        ndkVersion = "27.0.12077973"
        externalNativeBuild {
            cmake {
                path("src/main/cpp/CMakeLists.txt")
                version = "3.31.6"
            }
        }
        defaultConfig {
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":core-common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
