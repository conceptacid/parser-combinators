
import org.gradle.kotlin.dsl.kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

///////////////////// Build Script top level ////////////////////////////////////////
buildscript {
    val kotlin_version by extra("1.3.50")
    val arrow_version by extra("0.9.0")
    val kollection_version by extra ("0.7")

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        //classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.5")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

///////////////////// Gradle plugins ////////////////////////////////////////////////
plugins {
    java
    kotlin("jvm") version "1.3.20"
    idea
    application
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.5"
}

group = "net.conceptacid"
version = "1.0-SNAPSHOT"

///////////////////// External repositories ///////////////////////////////////////////
repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://jitpack.io")
    // The google mirror is less flaky than mavenCentral()
    maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("junit:junit:4.12")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

////////////////// Additional settings /////////////////////////////////////////////

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "idl.MainKt"
}

///////////////////// Kotlin compile options /////////////////////////////////////////
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = sourceCompatibility
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = sourceCompatibility
    }
}


tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("idl")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "idl.MainKt"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}