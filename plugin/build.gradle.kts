plugins {
    kotlin("jvm") version "1.9.25"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.woo.plugin"
version = project.findProperty("version") as String

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("versionCheckPlugin") {
            id = "org.woo.plugin.version-check"
            implementationClass = "org.woo.plugin.VersionCheckPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/PARKPARKWOO/common-module")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
