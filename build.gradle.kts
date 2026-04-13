plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "ru.vibekodik"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
}

kotlin {
    jvmToolchain(17)
}

val localPyCharmPath = System.getenv("PYCHARM_LOCAL_PATH")?.trim().orEmpty()

intellij {
    if (localPyCharmPath.isNotBlank()) {
        localPath.set(localPyCharmPath)
    } else {
        version.set("2024.1")
        type.set("PC")
    }
}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("241.*")
    }
}
