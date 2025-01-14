@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
    `maven-publish`
    signing
}

group = "io.github.devngho"
version = "1.1.3"

repositories {
    mavenCentral()
    mavenLocal()
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

kotlin {
    withSourcesJar(true)
    jvmToolchain(21)

    jvm {
        withJava()
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    wasmJs {
        binaries.executable()
        browser {}
    }

    sourceSets {
        val ktorVersion = "3.0.3"
        val serializationVersion = "1.8.0"
        val kotestVersion = "5.9.1"

        commonMain {
            dependencies {
                implementation("io.github.devngho:kirok:1.1.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
        jvmMain {
            dependencies {
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }

        applyDefaultHierarchyTemplate()
    }
}

publishing {
    signing {
        sign(publishing.publications)
    }

    repositories {
        mavenLocal()
        val id: String =
            if (project.hasProperty("repoUsername")) project.property("repoUsername") as String
            else System.getenv("repoUsername")
        val pw: String =
            if (project.hasProperty("repoPassword")) project.property("repoPassword") as String
            else System.getenv("repoPassword")
        if (!version.toString().endsWith("SNAPSHOT")) {
            maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                name = "sonatypeReleaseRepository"
                credentials {
                    username = id
                    password = pw
                }
            }
        }
    }

    publications.withType(MavenPublication::class) {
        groupId = project.group as String?
        version = project.version as String?

        artifact(javadocJar)

        pom {
            name.set("kirok-ktor")
            description.set("kirok의 ktor reteriver")
            url.set("https://github.com/devngho/kirok-ktor")


            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/devngho/kirok-ktor/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("devngho")
                    name.set("devngho")
                    email.set("yjh135908@gmail.com")
                }
            }
            scm {
                connection.set("https://github.com/devngho/kirok-ktor.git")
                developerConnection.set("https://github.com/devngho/kirok-ktor.git")
                url.set("https://github.com/devngho/kirok-ktor")
            }
        }
    }
}

tasks {
    val taskList = this.toList().map { it.name }
    getByName("signKotlinMultiplatformPublication") {
        if (taskList.contains("publishJvmPublicationToSonatypeReleaseRepositoryRepository"))
            dependsOn(
                "publishJvmPublicationToSonatypeReleaseRepositoryRepository",
                "publishJvmPublicationToMavenLocalRepository",
                "publishJvmPublicationToMavenLocal"
            )
        else dependsOn("publishJvmPublicationToMavenLocalRepository", "publishJvmPublicationToMavenLocal")
    }
    getByName("signWasmJsPublication") {
        if (taskList.contains("publishJvmPublicationToSonatypeReleaseRepositoryRepository"))
            dependsOn(
                "publishJvmPublicationToSonatypeReleaseRepositoryRepository",
                "publishKotlinMultiplatformPublicationToSonatypeReleaseRepositoryRepository",
                "publishJvmPublicationToMavenLocal",
                "publishJvmPublicationToMavenLocalRepository",
                "publishKotlinMultiplatformPublicationToMavenLocalRepository"
            )
        else
            dependsOn(
                "publishJvmPublicationToMavenLocal",
                "publishKotlinMultiplatformPublicationToMavenLocal",
                "publishKotlinMultiplatformPublicationToMavenLocalRepository"
            )
    }
}