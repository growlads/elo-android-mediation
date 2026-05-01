import com.vanniktech.maven.publish.DeploymentValidation

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.withgrowl.growlads.mediation.admob"
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    compileOnly(libs.elo.android.sdk)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.play.services.ads)

    testImplementation(libs.elo.android.sdk)
    testImplementation(project(":testkit"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
    signAllPublications()

    coordinates("ad.elo", "elo-android-mediation-admob", rootProject.version.toString())
    pom {
        name.set("Elo Android Mediation — AdMob")
        description.set("AdMob adapter for the Elo Android SDK mediation auction.")
        inceptionYear.set("2026")
        url.set("https://github.com/growlads/elo-android-mediation")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("growlads")
                name.set("Growl")
                url.set("https://elo.ad")
            }
        }
        scm {
            url.set("https://github.com/growlads/elo-android-mediation")
            connection.set("scm:git:git://github.com/growlads/elo-android-mediation.git")
            developerConnection.set("scm:git:ssh://git@github.com/growlads/elo-android-mediation.git")
        }
    }
}
