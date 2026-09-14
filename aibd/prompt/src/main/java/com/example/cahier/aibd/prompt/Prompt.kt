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

package com.example.cahier.aibd.prompt

import ink.proto.BrushFamily as BrushFamilyProto
import com.google.protobuf.ByteString
import com.google.protobuf.TextFormat
import java.io.File
import java.util.concurrent.Callable
import java.util.zip.GZIPInputStream
import kotlin.sequences.dropWhile
import kotlin.system.exitProcess
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

/**
 * Generates the system prompt for AI Brush Designer.
 *
 * Combines three parts:
 * - Prelude: Introduction and core task instructions, including output format.
 * - Schemas: BrushFamily and Color proto schemas, copied from source and stripped of LINT checks,
 *   copyright info, and copybara annotations.
 * - Examples: Annotated examples of BrushFamily textprotos. Texture data is omitted for brevity.
 */
object Prompt {
  enum class CopierState {
    STRIPPING,
    INSERTING,
    COPYING,
  }

  fun stripFileContent(file: File): String {
    var copierState = CopierState.COPYING
    val lines = mutableListOf<String>()

    file.useLines { fileLines ->
      for ((lineNumber, line) in
        fileLines
          .dropWhile {
            it.startsWith("//") || // Proto copyright
              // Markdown copyright
              it.startsWith("<!--") ||
              it.startsWith("----") ||
              it.startsWith("-->") ||
              it.startsWith(" -->") ||
              it.isEmpty()
          }
          .withIndex()) {
        if (line.contains("// LINT.IfChange") || line.contains("// LINT.ThenChange")) {
          continue
        }

        val copy = "copybara"

        if (line.contains("// $copy:")) {
          copierState =
            when {
              line.contains("// $copy:strip_begin") -> CopierState.STRIPPING
              line.contains("// $copy:insert_begin") ||
                line.contains("// $copy:strip_end_and_replace_begin") -> CopierState.INSERTING
              line.contains("// $copy:replace_end") ||
                line.contains("// $copy:strip_end") ||
                line.contains("// $copy:insert_end") -> CopierState.COPYING
              line.contains("// $copy:strip") -> copierState // Single line strip
              else ->
                throw IllegalArgumentException(
                  "Failed to parse copybara annotation from ${file.path}:$lineNumber: $line"
                )
            }
          // If the state changed, we don't want to include the line which changed the state.
          continue
        }

        when (copierState) {
          CopierState.STRIPPING -> continue
          CopierState.INSERTING ->
            // Match the whitespace and the comment, but only replace the comment.
            lines.add(line.replaceFirst(Regex("^(\\s*)//\\s*"), "$1"))
          CopierState.COPYING -> lines.add(line)
        }
      }
    }

    return lines.joinToString("\n")
  }

  fun filterTextureBitmap(proto: BrushFamilyProto): BrushFamilyProto {
    val placeholder = ByteString.copyFromUtf8("<PNG bytes omitted for brevity>")
    return proto
      .toBuilder()
      .putAllTextureIdToBitmap(proto.textureIdToBitmapMap.mapValues { placeholder })
      .build()
  }

  fun generatePromptContent(
    preludeMdFile: File,
    brushFamilyProtoFile: File,
    colorProtoFile: File,
    examplesDir: File,
  ): String {
    val output = buildString {
      append(
        """${stripFileContent(preludeMdFile)}

# Proto Schemas

## brush_family.proto

```proto
${stripFileContent(brushFamilyProtoFile)}
```

## color.proto

```proto
${stripFileContent(colorProtoFile)}
```
"""
      )

      if (!examplesDir.isDirectory()) {
        throw IllegalArgumentException("Examples directory not found: ${examplesDir.path}")
      }
      val inputFiles =
        examplesDir.listFiles { f -> f.extension == "brushfamily" }!!.sortedBy { it.name }

      if (inputFiles.isNotEmpty()) {
        append("\n# Annotated Examples\n")
      }

      for (inputFile in inputFiles) {
        // Insert a space before every uppercase letter, except at the beginning
        // of the string, and remove the .brushfamily extension. For example,
        // "ShadingPencil.brushfamily" becomes "Shading Pencil".
        val brushName = inputFile.nameWithoutExtension.replace(Regex("(?<!^)(?=[A-Z])"), " ")

        val binaryProto = GZIPInputStream(inputFile.inputStream()).use { it.readBytes() }
        val protoMessage = BrushFamilyProto.parseFrom(binaryProto)
        val cleanProto = filterTextureBitmap(protoMessage)

        append(
          """
## $brushName

```proto
"""
        )
        TextFormat.printer().print(cleanProto, this)

        append("```\n")
      }
    }

    return output
  }
}

@Command(
  name = "prompt",
  description = ["Generates the prompt for the AI Brush Designer."],
  mixinStandardHelpOptions = true,
)
class PromptCommand : Callable<Int> {
  @Option(
    names = ["-o", "--output"],
    required = true,
    description = ["Path to output prompt file."],
  )
  lateinit var outputFile: File

  override fun call(): Int {
    val preludeFile =
      File(
        "aibd/prompt/src/main/java/com/example/cahier/aibd/prompt/prelude.md"
      )
    val brushProtoFile = File("ink-proto/src/main/proto/brush_family.proto")
    val colorProtoFile = File("ink-proto/src/main/proto/color.proto")
    val examplesDir =
      File(
        "aibd/prompt/src/main/java/com/example/cahier/aibd/prompt/examples"
      )

    val content =
      Prompt.generatePromptContent(preludeFile, brushProtoFile, colorProtoFile, examplesDir)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(content)
    println("Successfully generated ${outputFile.path}")
    return 0
  }
}

fun main(args: Array<String>) {
  exitProcess(CommandLine(PromptCommand()).execute(*args))
}
