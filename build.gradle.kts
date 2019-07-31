import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    kotlin("jvm") version "1.3.41"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.seik.kotlin-telegram-bot:telegram:0.3.8")
    implementation("org.dizitart:potassium-nitrite:3.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
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
            attributes["Main-Class"] = application.mainClassName
        }
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + "-Xnew-inference"
        }
    }
}
