plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.yourcompany.crowdmeasure.sample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.yourcompany.crowdmeasure.sample"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":crowdmeasure-sdk-core"))
    implementation(project(":crowdmeasure-sdk-background"))
    implementation(project(":crowdmeasure-sdk-upload"))
    implementation(project(":crowdmeasure-sdk-firestore"))
    implementation(project(":crowdmeasure-sdk-calls"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
