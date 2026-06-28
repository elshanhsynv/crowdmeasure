import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hiltandroid) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

val sdkProjects = setOf(
    "crowdmeasure-sdk-core",
    "crowdmeasure-sdk-background",
    "crowdmeasure-sdk-measurements-upload-api",
    "crowdmeasure-sdk-measurements-upload",
    "crowdmeasure-sdk-calls",
    "crowdmeasure-sdk-calls-upload",
    "crowdmeasure-sdk-firestore-measurements",
    "crowdmeasure-sdk-firestore-calls",
)

subprojects {
    if (name in sdkProjects) {
        val sdkArtifactId = name.removePrefix("crowdmeasure-sdk-").let { "crowdmeasure-$it" }
        group = "com.crowdmeasure"
        version = "0.1.0"
        pluginManager.apply("maven-publish")
        plugins.withId("com.android.library") {
            extensions.configure<LibraryExtension> {
                publishing {
                    singleVariant("release") { withSourcesJar() }
                }
            }
            afterEvaluate {
                extensions.configure<PublishingExtension> {
                    publications {
                        if (findByName("release") == null) {
                            create<MavenPublication>("release") {
                                from(components["release"])
                                artifactId = sdkArtifactId
                            }
                        }
                    }
                }
            }
        }
    }
}

tasks.register("sdkCheck") {
    group = "verification"
    description = "Builds and tests every public SDK artifact and both host applications."
    dependsOn(
        sdkProjects.map { ":$it:check" },
        ":app:assembleDebug",
        ":app:assembleRelease",
        ":sample-host-app:assembleDebug",
        ":sample-host-app:assembleRelease",
        ":verifySdkDependencyBoundaries",
        ":verifyDocumentation",
        ":apiCheck",
    )
}

val apiBaselineFiles = sdkProjects.associateWith { file("api/$it.api") }

tasks.register("apiCheck") {
    group = "verification"
    description = "Validates that every published artifact has an intentional public API baseline."
    inputs.files(apiBaselineFiles.values)
    doLast {
        apiBaselineFiles.forEach { (artifact, baseline) ->
            check(baseline.isFile) { "Missing API baseline for $artifact" }
            val symbols = baseline.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
            check(symbols.isNotEmpty()) { "API baseline for $artifact is empty" }
            check(symbols.none { ".internal." in it }) { "API baseline for $artifact exposes internal symbols" }
        }
    }
}

val sdkBoundaryFiles = mapOf(
    "crowdmeasure-sdk-core" to file("crowdmeasure-sdk-core/build.gradle.kts"),
    "crowdmeasure-sdk-calls" to file("crowdmeasure-sdk-calls/build.gradle.kts"),
    "crowdmeasure-sdk-measurements-upload-api" to file("crowdmeasure-sdk-measurements-upload-api/build.gradle.kts"),
    "crowdmeasure-sdk-firestore-measurements" to file("crowdmeasure-sdk-firestore-measurements/build.gradle.kts"),
    "crowdmeasure-sdk-firestore-calls" to file("crowdmeasure-sdk-firestore-calls/build.gradle.kts"),
)

tasks.register("verifySdkDependencyBoundaries") {
    group = "verification"
    inputs.files(sdkBoundaryFiles.values)
    doLast {
        val forbidden = mapOf(
            "crowdmeasure-sdk-core" to listOf("work-runtime", "firebase", "hilt", "compose", "crashlytics"),
            "crowdmeasure-sdk-calls" to listOf("work-runtime", "firebase", "hilt", "compose", "crashlytics"),
            "crowdmeasure-sdk-measurements-upload-api" to listOf("work-runtime", "firebase", "hilt", "compose", "crashlytics"),
            "crowdmeasure-sdk-firestore-measurements" to listOf("crowdmeasure-sdk-calls", "work-runtime"),
            "crowdmeasure-sdk-firestore-calls" to listOf("crowdmeasure-sdk-measurements-upload", "work-runtime"),
        )
        forbidden.forEach { (projectName, terms) ->
            val buildFile = sdkBoundaryFiles.getValue(projectName).readText()
            terms.forEach { term ->
                check(!buildFile.contains(term, ignoreCase = true)) {
                    "$projectName contains forbidden dependency marker '$term'"
                }
            }
        }
    }
}

val documentationFiles = fileTree("docs") { include("**/*.md") }
tasks.register("verifyDocumentation") {
    group = "verification"
    inputs.files(documentationFiles)
    doLast {
        check(documentationFiles.files.any { it.name == "README.md" }) { "docs/README.md is missing" }
        documentationFiles.files.forEach { page ->
            val first = page.useLines { it.firstOrNull().orEmpty() }
            check(first.contains("Available") || first.contains("In Progress") || first.contains("Planned")) {
                "${page.path} must start with an availability status"
            }
        }
    }
}
