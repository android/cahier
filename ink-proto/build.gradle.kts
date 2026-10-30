import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    kotlin("jvm")
    id("com.google.protobuf") version "0.10.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.protobuf.javalite)
    // Runtime for the generated Kotlin DSL (e.g. `brushFamily { ... }`), which consumers call into.
    api(libs.protobuf.kotlin.lite)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.36.2"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}