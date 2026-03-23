import Version.GRPC

plugins {
    kotlin("jvm") version "1.9.25"
}

version = project.findProperty("version") as String

dependencies {
    implementation(project(":grpc"))
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE") {
        exclude(group = "io.grpc", module = "grpc-netty-shaded")
        exclude(group = "io.grpc", module = "grpc-protobuf")
    }
    implementation("io.grpc:grpc-netty-shaded:$GRPC")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("io.grpc:grpc-protobuf:$GRPC")
}

tasks.test {
    useJUnitPlatform()
}
