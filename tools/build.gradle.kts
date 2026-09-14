/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  kotlin("jvm")
  `java-test-fixtures`
  alias(libs.plugins.protobuf)
  application
}

application { mainClass.set("com.example.cahier.tools.DeployBrushKt") }

// Compile Full Protobuf Java classes directly from proto definitions.
// We do not depend on :ink-proto because :ink-proto is configured for Android (Protobuf Lite),
// whereas ValidateBrush.kt runs on the JVM and requires Protobuf Full for parsing textprotos.
protobuf {
  protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobufJavalite.get()}" }
}

sourceSets { main { proto { srcDir("../ink-proto/src/main/proto") } } }

dependencies {
  implementation(project(":aibd:core"))
  implementation(libs.protobuf.java)
  implementation(libs.androidx.ink.brush.jvm)
  implementation(libs.androidx.ink.storage.jvm)
  implementation(libs.picocli)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.named("compileKotlin") { dependsOn("generateProto") }

val runValidateBrush by
  tasks.registering(JavaExec::class) {
    group = "application"
    description = "Runs the ValidateBrush CLI tool"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.cahier.tools.ValidateBrushKt")
    standardInput = System.`in`
  }
