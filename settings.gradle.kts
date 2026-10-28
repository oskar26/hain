pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Hain"

include(
    ":app",
    ":core-ui",
    ":core-common",
    ":core-data",
    ":core-llm",
    ":core-tools",
    ":core-memory",
    ":core-docs",
    ":core-voice",
    ":core-cards",
    ":core-models",
    ":core-testing",
    ":feature-chat",
    ":feature-settings",
    ":feature-memory",
    ":feature-news",
    ":feature-image",
    ":feature-onboarding",
)
