/* This file is part of kotlin-prettyprinting.
 * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jetbrains.dokka-javadoc") version "2.1.0-Beta"
    `maven-publish`
    signing
    jacoco
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "io.github.wadoon"
version = "1.0-SNAPSHOT"
description = "Pretty-printing library in pure Kotlin"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:1.4.5")

    // dokkaPlugin("com.glureau:html-mermaid-dokka-plugin:0.6.0")
    // dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:2.0.0")
    // dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports.html.required.set(false)
    reports.junitXml.required.set(true)
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
    }
}

kotlin {
    jvmToolchain(21)
}

dokka {
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/wadoon/kotlin-prettyprinting")
            remoteLineSuffix.set("#L")
        }
        // includes.from("README.md")
    }
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        csv.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.5".toBigDecimal()
            }
        }

        rule {
            isEnabled = false
            element = "CLASS"
            includes = listOf("org.gradle.*")

            limit {
                counter = "LINE"
                value = "TOTALCOUNT"
                maximum = "0.3".toBigDecimal()
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaGenerateJavadoc"))
    // from(tasks.named("dokkaGenerateHtml"))
}

/*tasks.named<Jar>("javadocJar") {
    val dokkaJavadoc = tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationJavadoc")
    dependsOn(dokkaJavadoc)
    from(dokkaJavadoc.flatMap { it.outputDirectory })
}*/

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            repositories {
                maven {
                    name = "folder"
                    url = uri("$rootDir/release")
                }
            }

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/wadoon/kotlin-prettyprinting"
                licenses {
                    license {
                        name = "GNU General Public License (GPL), Version 2"
                        url = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
                    }
                }
                developers {
                    developer {
                        id = "wadoon"
                        name = "Alexander Weigl"
                        email = "weigl@kit.edu"
                    }
                }
                scm {
                    connection = "git@github.com:wadoon/kotlin-prettyprinting.git"
                    url = "https://github.com/wadoon/kotlin-prettyprinting"
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        create("central") {
            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

            stagingProfileId.set("io.github.wadoon")
            val user: String = project.properties.getOrDefault("ossrhUsername", "").toString()
            val pwd: String = project.properties.getOrDefault("ossrhPassword", "").toString()

            username.set(user)
            password.set(pwd)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        licenseHeader(
            """
                |/* This file is part of kotlin-prettyprinting.
                | * kotlin-prettyprinting is licensed under the GNU General Public License Version 2
                | * SPDX-License-Identifier: GPL-2.0-only
                | */
            """.trimMargin(),
        ).delimiter("^(package |@file|import |plugins )")
        val editorConfigPath = File(rootDir, ".editorconfig")
        ktlint("1.7.1")
            .setEditorConfigPath(editorConfigPath)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
