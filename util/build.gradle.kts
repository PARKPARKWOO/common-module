plugins {
    kotlin("jvm") version "1.9.25"
}

version = project.findProperty("version") as String

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
