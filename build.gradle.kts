
import org.gradle.kotlin.dsl.kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar



///////////////////// Build Script top level ////////////////////////////////////////
buildscript {
    val kotlin_version by extra("1.3.50")
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
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.6.1"
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

    val arrow_version = "0.9.0"   //by extra("0.9.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("junit:junit:4.12")
    testImplementation("org.assertj:assertj-core:3.11.1")


    ///////////////////// Kotlin Arrow ///////////////////////////////////////////////////////////
    compile("io.arrow-kt:arrow-core-data:${arrow_version}")
    compile("io.arrow-kt:arrow-core-extensions:${arrow_version}")
    compile("io.arrow-kt:arrow-syntax:${arrow_version}")
    compile("io.arrow-kt:arrow-typeclasses:${arrow_version}")
    compile("io.arrow-kt:arrow-extras-data:${arrow_version}")
    compile("io.arrow-kt:arrow-extras-extensions:${arrow_version}")
    //kapt("io.arrow-kt:arrow-meta:${arrow_version}")

    compile("io.arrow-kt:arrow-query-language:${arrow_version}")
    compile("io.arrow-kt:arrow-effects-data:${arrow_version}")
    compile("io.arrow-kt:arrow-effects-extensions:${arrow_version}")
    compile("io.arrow-kt:arrow-effects-io-extensions:${arrow_version}")
    compile("io.arrow-kt:arrow-optics:${arrow_version}")
    compile("io.arrow-kt:arrow-query-language:${arrow_version}")

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