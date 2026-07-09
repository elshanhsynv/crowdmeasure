import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltandroid)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun keystoreProperty(name: String): String =
    keystoreProperties[name] as? String
        ?: error("Missing '$name' in keystore.properties")

android {
    namespace = "com.example.crowdmeasure"
    compileSdk {
        version = release(36)
    }

    sourceSets {
        getByName("androidTest").assets.directories.addAll(
            listOf(
                "$projectDir/schemas",
                "$projectDir/additional-schemas"
            )
        )
    }


    defaultConfig {
        applicationId = "com.example.crowdmeasure"
        minSdk = 29
        targetSdk = 36
        versionCode = 10
        versionName = "1.10"
        buildConfigField(
            "String",
            "UPDATE_METADATA_URL",
            "\"${providers.gradleProperty("crowdmeasure.updateMetadataUrl").orNull ?: "https://elshanwork.github.io/crowdmeasure-releases/latest.json"}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperty("storeFile"))
            storePassword = keystoreProperty("storePassword")
            keyAlias = keystoreProperty("keyAlias")
            keyPassword = keystoreProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":crowdmeasure-sdk-core"))
    implementation(project(":crowdmeasure-sdk-background"))
    implementation(project(":crowdmeasure-sdk-measurements-upload"))
    implementation(project(":crowdmeasure-sdk-firestore-measurements"))
    implementation(project(":crowdmeasure-sdk-firestore-calls"))
    implementation(project(":crowdmeasure-sdk-calls"))
    implementation(project(":crowdmeasure-sdk-calls-upload"))

    // Firebase BoM and Services
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Startup Runtime
    implementation(libs.androidx.startup.runtime)

    // AppCompat
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.animation.core)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Location
    implementation(libs.play.services.location)

    // Networking
    implementation(libs.okhttp)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Extended Icons
    implementation(libs.androidx.material.icons.extended)

    //
    implementation(libs.timber)
}
