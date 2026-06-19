plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.crowdmeasure.sdk.calls.upload"
    compileSdk { version = release(36) }
    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":crowdmeasure-sdk-calls"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
