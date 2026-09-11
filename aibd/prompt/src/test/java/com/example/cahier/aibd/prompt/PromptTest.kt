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

import com.google.common.truth.Truth.assertThat
import ink.proto.BrushFamily as BrushFamilyProto
import com.google.protobuf.TextFormat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PromptTest {

  @Test
  fun testFilterTextureBitmap() {
    val inputProto =
      TextFormat.parse(
        """
        texture_id_to_bitmap {
          key: "foo"
          value: "\x00\x01\x02"
        }
        """
          .trimIndent(),
        BrushFamilyProto::class.java,
      )

    val cleanProto = Prompt.filterTextureBitmap(inputProto)

    val expectedProto =
      TextFormat.parse(
        """
        texture_id_to_bitmap {
          key: "foo"
          value: "<PNG bytes omitted for brevity>"
        }
        """
          .trimIndent(),
        BrushFamilyProto::class.java,
      )

    assertThat(cleanProto).isEqualTo(expectedProto)
  }

  @Test
  fun testStripFileContent() {
    val copy = "copybara"
    val fakeContent =
      """
        // Copyright
        // Header
        <!--
        ---- Copyright in Markdown
         -->
        import "foo.proto";
        import "skipme.proto"; // $copy:strip
        // $copy:insert_begin
        // import "insertme.proto";
        // $copy:insert_end
        // $copy:strip_begin
        import "begin.proto";
        // $copy:strip_end_and_replace_begin
        // import "replace.proto";
        // $copy:replace_end

        // LINT.IfChange
        enum Baz {
          BAZ = 0;
          // $copy:strip_begin
          SKIPME = 1;
          // $copy:strip_end
        }
        // LINT.ThenChange(//myproject/foo.proto)

        message Bar {}
        """
        .trimIndent()

    val tempFile = File.createTempFile("fake", ".textproto")
    tempFile.writeText(fakeContent)

    val content = Prompt.stripFileContent(tempFile)
    tempFile.delete()

    val expected =
      """
      import "foo.proto";
      import "insertme.proto";
      import "replace.proto";

      enum Baz {
        BAZ = 0;
      }

      message Bar {}
      """
        .trimIndent()

    assertThat(content).isEqualTo(expected)
  }
}
