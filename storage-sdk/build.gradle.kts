plugins {
    kotlin("jvm") version "1.9.25"
    id("com.google.protobuf") version "0.9.4"
}

version = project.findProperty("version") as String
val grpcVersion = "1.63.0"
val protobufVersion = "3.23.4"

dependencies {
    implementation(project(":grpc"))
    // grpc
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE") {
        exclude(group = "io.grpc", module = "grpc-netty-shaded")
        exclude(group = "io.grpc", module = "grpc-protobuf")
//        exclude(group = "io.grpc", module = "grpc-")
    }
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
}

tasks.test {
    useJUnitPlatform()
}

// protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:$protobufVersion"
//    }
//    plugins {
//        id("grpc") {
//            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
//        }
//        id("grpckt") {
//            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.2.0:jdk7@jar"
//        }
//    }
//    generateProtoTasks {
//        ofSourceSet("main").forEach {
//            it.plugins {
//                id("grpc") { }
//                id("grpckt") {}
//            }
//        }
//    }
// }
