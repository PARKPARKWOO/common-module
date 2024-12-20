import Version.GRPC
import Version.PROTOBUF
import com.google.protobuf.gradle.id

plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
    id("java")
    id("idea")
}

version = "0.0.3-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-protobuf:$GRPC")
    implementation("io.grpc:grpc-stub:$GRPC")

    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        implementation("javax.annotation:javax.annotation-api:1.3.1")
    } else {
        compileOnly("jakarta.annotation:jakarta.annotation-api:$PROTOBUF") // Java 9+ compatibility - Do NOT update to 2.0.0
    }
    implementation("io.grpc:protoc-gen-grpc-java:$GRPC")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
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
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without
                // options. Note the braces cannot be omitted, otherwise the
                // plugin will not be added. This is because of the implicit way
                // NamedDomainObjectContainer binds the methods.
                id("grpc") { }
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
