plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // TODO: publish when first third-party adapter author requests it.
    // alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.withgrowl.growlads.mediation.testkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    api(libs.elo.android.sdk)
    implementation(libs.kotlinx.coroutines.core)
}
