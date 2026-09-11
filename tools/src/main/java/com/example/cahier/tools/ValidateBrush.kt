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

package com.example.cahier.tools

import androidx.ink.brush.BrushFamily
import androidx.ink.storage.decode
import com.example.cahier.aibd.core.ValidationException
import ink.proto.BrushFamily as BrushFamilyProto
import com.google.protobuf.TextFormat
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.GZIPOutputStream
import kotlin.system.exitProcess

fun validateAndCompressBrushFamily(textproto: String): ByteArray {
  val proto =
    try {
      TextFormat.parse(textproto, BrushFamilyProto::class.java)
    } catch (e: Exception) {
      throw ValidationException("Failed to read or parse textproto", e)
    }

  val gzipOutput = ByteArrayOutputStream()
  GZIPOutputStream(gzipOutput).use { it.write(proto.toByteArray()) }
  val gzippedPayload = gzipOutput.toByteArray()

  try {
    val unused = BrushFamily.decode(gzippedPayload)
  } catch (e: Exception) {
    throw ValidationException("Validation failed during decoding", e)
  }
  return gzippedPayload
}

fun main(args: Array<String>) {
  val textproto = InputStreamReader(System.`in`).readText()
  val gzippedPayload =
    try {
      validateAndCompressBrushFamily(textproto)
    } catch (e: Exception) {
      System.err.println(e.message)
      exitProcess(1)
    }

  System.err.println("Validation successful.")
  System.out.write(gzippedPayload)
  System.err.println("Successfully wrote gzipped binary proto to stdout.")
}
