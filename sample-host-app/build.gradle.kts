plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.crowdmeasure.sample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.crowdmeasure.sample"
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
    val useMavenLocal = providers.gradleProperty("crowdmeasure.useMavenLocal").orNull == "true"
    fun sdk(module: String) =
        if (useMavenLocal) "com.crowdmeasure:crowdmeasure-$module:0.1.0" else project(":crowdmeasure-sdk-$module")

    implementation(sdk("core"))
    implementation(sdk("background"))
    implementation(sdk("measurements-upload"))
    implementation(sdk("firestore-measurements"))
    implementation(sdk("firestore-calls"))
    implementation(sdk("calls"))
    implementation(sdk("calls-upload"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
