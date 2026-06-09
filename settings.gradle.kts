pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CrowdMeasure"
include(":app")
include(":crowdmeasure-sdk-core")
include(":crowdmeasure-sdk-background")
include(":crowdmeasure-sdk-upload")
include(":crowdmeasure-sdk-firestore")
include(":crowdmeasure-sdk-calls")
include(":sample-host-app")
