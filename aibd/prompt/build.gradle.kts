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
  alias(libs.plugins.protobuf)
  application
}

application { mainClass.set("com.example.cahier.aibd.prompt.PromptKt") }

// Compile Full Protobuf Java classes directly from proto definitions.
// We do not depend on :ink-proto because :ink-proto is configured for Android (Protobuf Lite),
// whereas Prompt.kt runs on the JVM and requires Protobuf Full for parsing textprotos.
protobuf {
  protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobufJavalite.get()}" }
}

sourceSets { main { proto { srcDir("../../ink-proto/src/main/proto") } } }

dependencies {
  implementation(libs.protobuf.java)
  implementation(kotlin("stdlib"))
  implementation(libs.picocli)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.named("compileKotlin") { dependsOn("generateProto") }

val generatePrompt by
  tasks.registering(JavaExec::class) {
    dependsOn("compileKotlin")
    workingDir = rootDir
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.cahier.aibd.prompt.PromptKt")

    val outputDir = layout.buildDirectory.dir("generated/resources")
    val outputFile = outputDir.map { it.file("prompt.md") }
    outputs.file(outputFile)

    val preludeFile =
      layout.projectDirectory.file("src/main/java/com/example/cahier/aibd/prompt/prelude.md")
    val brushProto =
      layout.projectDirectory.file("../../ink-proto/src/main/proto/brush_family.proto")
    val colorProto = layout.projectDirectory.file("../../ink-proto/src/main/proto/color.proto")
    val examplesDir =
      layout.projectDirectory.dir("src/main/java/com/example/cahier/aibd/prompt/examples")

    inputs.file(preludeFile)
    inputs.file(brushProto)
    inputs.file(colorProto)
    inputs.dir(examplesDir)

    args("--output=${outputFile.get().asFile.absolutePath}")
  }
