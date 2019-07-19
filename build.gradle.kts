buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.3.41"
    id("org.jlleitschuh.gradle.ktlint") version "4.1.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    application
}

allprojects {
    group = "me.msfjarvis.wallsbot"
    version = "0.1"

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib")
    compile("io.github.seik.kotlin-telegram-bot:telegram:0.3.8")
}

application {
    // Define the main class for the application
    mainClassName = "me.msfjarvis.wallsbot.MainKt"
}

tasks {
    named<Wrapper>("wrapper") {
        gradleVersion = "5.5.1"
        distributionType = Wrapper.DistributionType.ALL
    }
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClassName
                )
            )
        }
    }
}
