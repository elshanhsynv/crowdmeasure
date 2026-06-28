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
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "CrowdMeasure"
include(":app")
include(":crowdmeasure-sdk-core")
include(":crowdmeasure-sdk-background")
include(":crowdmeasure-sdk-measurements-upload")
include(":crowdmeasure-sdk-measurements-upload-api")
include(":crowdmeasure-sdk-firestore-measurements")
include(":crowdmeasure-sdk-firestore-calls")
include(":crowdmeasure-sdk-calls")
include(":crowdmeasure-sdk-calls-upload")
include(":sample-host-app")
