pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useVersion("8.5.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("1.9.20")
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                useVersion("1.9.20-1.0.14")
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