import org.jetbrains.kotlin.gradle.internal.kapt.incremental.metadataDescriptor

plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "io.turbodsl.tutorial"
version = "1.0-SNAPSHOT"

repositories {
    // This will use ~/.m2 local maven repository
    // mavenLocal()
    // -------------------------------------------------
    // This will use GitHubPackages
     maven {
         name = "GitHubPackages"
         url = uri("https://maven.pkg.github.com/migueltt/io-turbodsl")
         credentials {
             // username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
             // DO NOT COMMIT
             username = "<username>"
             // DO NOT COMMIT
             // See https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens
             password = "<key>"
         }
     }
    mavenCentral()
}

dependencies {
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
    /* TODO:
     *   For whatever reason, source/javadoc are not included in
     *   library definition.
     */
    implementation("io.turbodsl:io-turbodsl-core:+")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}