rootProject.name = "live-v3"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {}
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":common", ":cds", ":frontend", ":backend")
project(":common").projectDir = file("src/common")
project(":cds").projectDir = file("src/cds")
project(":frontend").projectDir = file("src/frontend")
project(":backend").projectDir = file("src/backend")
