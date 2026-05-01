pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Local override for SDK + adapter co-development:
        // ./gradlew :GrowlAndroidSDK:publishToMavenLocal in elo-android-sdk-source first.
        mavenLocal()
    }
}

rootProject.name = "elo-android-mediation"

include(":adapter-admob")
include(":testkit")
