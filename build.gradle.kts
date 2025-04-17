plugins {
    kotlin("jvm") version "1.9.25"
    id("maven-publish")
}

repositories {
    mavenCentral()
}

subprojects {
    if (name != "plugin") { // 플러그인 프로젝트는 별도 관리
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "maven-publish")

        group = "org.woo"

        repositories {
            mavenCentral()
        }

        val projectVersion = project.findProperty("version")?.toString() ?: rootProject.version.toString()

        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }

        kotlin {
            compilerOptions {
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/PARKPARKWOO/common-module")
                    credentials {
                        username = project.findProperty("gpr.user")?.toString()
                        password = project.findProperty("gpr.key")?.toString()
                    }
                }
            }

            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    version = projectVersion
                }
            }
        }
    }
}
