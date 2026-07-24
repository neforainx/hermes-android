pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useVersion(libs.versions.agp.get())
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion(libs.versions.kotlin.get())
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                useVersion(libs.versions.ksp.get())
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hermes"
include(":app")