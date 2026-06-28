plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.crowdmeasure.sdk.upload.api"
    compileSdk { version = release(36) }
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":crowdmeasure-sdk-core"))
    testImplementation(libs.junit)
}
