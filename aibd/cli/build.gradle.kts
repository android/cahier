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
  application
}

application { mainClass.set("com.example.cahier.aibd.cli.CliKt") }

dependencies {
  implementation(project(":aibd:core"))
  implementation(project(":tools"))
  implementation(libs.picocli)
  implementation(libs.jline)
  implementation(libs.google.genai)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(testFixtures(project(":aibd:core")))
  testImplementation(testFixtures(project(":tools")))

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.kotlinx.coroutines.test)
}
