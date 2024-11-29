plugins {
    kotlin("jvm") version "1.9.25"
    id("maven-publish")
}

group = "org.woo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
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

//    publications {
//        create<MavenPublication>("mavenJava") {
//            groupId = "org.woo"
//            version = project.findProperty("version") as String

//            from(components["java"])
//            artifact(tasks.named("sourceJar"))
//        }
//     }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
