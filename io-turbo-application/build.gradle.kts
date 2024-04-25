/*
 * Copyright 2024. Project's Author and/or Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.diffplug.spotless") version "6.25.0"
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
             username = "<username>"
             // https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens
             password = "<key>"
         }
     }
    mavenCentral()
}

dependencies {
    implementation("io.turbodsl:io-turbodsl-core:+")
    testImplementation(kotlin("test"))
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

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("${layout.buildDirectory}/**/*.kt")
        ktlint()
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
}