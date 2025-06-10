import Version.GRPC
import Version.PROTOBUF
import com.google.protobuf.gradle.id

plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
    id("java")
    id("idea")
}

version = project.findProperty("version") as String

dependencies {
    api(project(":http"))
    api("io.grpc:grpc-protobuf:$GRPC")
    api("io.grpc:grpc-stub:$GRPC")
    api("io.grpc:grpc-kotlin-stub:1.4.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.8.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        api("javax.annotation:javax.annotation-api:1.3.1")
    } else {
        compileOnly("jakarta.annotation:jakarta.annotation-api:$PROTOBUF") // Java 9+ compatibility - Do NOT update to 2.0.0
    }
    api("io.grpc:protoc-gen-grpc-java:$GRPC")
}

protobuf {
    protoc {
//         Protobuf compiler
        artifact = "com.google.protobuf:protoc:$PROTOBUF"
    }
    plugins {
//         GRPC plugin for Protobuf
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$GRPC"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.2.0:jdk7@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without
                // options. Note the braces cannot be omitted, otherwise the
                // plugin will not be added. This is because of the implicit way
                // NamedDomainObjectContainer binds the methods.
                id("grpc") { }
                id("grpckt") {}
            }
        }
    }
}

tasks.register<Delete>("cleanGenerated") {
    delete(file("$projectDir/src/generated"))
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
