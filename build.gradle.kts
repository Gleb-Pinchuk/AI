plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}group = "ru.vibekodik"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val localPyCharmPath = System.getenv("PYCHARM_LOCAL_PATH")?.trim().orEmpty()
val proxyHost = System.getenv("AI_PROXY_HOST")?.trim().orEmpty()
val proxyPort = System.getenv("AI_PROXY_PORT")?.trim().orEmpty()

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    intellijPlatform {
        if (localPyCharmPath.isNotBlank()) {
            local(localPyCharmPath)
        } else {
            pycharmCommunity("2025.1.2")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }

    patchPluginXml {
        sinceBuild.set("251")
        untilBuild.set("251.*")
    }

    named("buildSearchableOptions") {
        enabled = false
    }

    named("instrumentCode") {
        enabled = false
    }

    named("runIde") {
        if (proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
            doFirst {
                println("runIde proxy enabled: $proxyHost:$proxyPort")
            }
            (this as JavaExec).jvmArgs(
                "-Djava.net.useSystemProxies=true",
                "-Dhttps.proxyHost=$proxyHost",
                "-Dhttps.proxyPort=$proxyPort",
                "-Dhttp.proxyHost=$proxyHost",
                "-Dhttp.proxyPort=$proxyPort"
            )
        }
    }
}
