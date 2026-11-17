# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Tests for report_failed_android_tests.py.

Runs the script as `__main__`, the way GitHub CI does, on JUnit XML results
written to a temporary directory. It runs in this process because google3's
Python has no interpreter binary to start a subprocess with. Uses only the
standard library:

  python3 scripts/report_failed_android_tests_test.py
"""

import contextlib
import io
import os
import runpy
import sys
import tempfile
import unittest
from unittest import mock

SCRIPT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "report_failed_android_tests.py"
)


def results(*testcases):
  """Returns a JUnit XML results file with the given <testcase> elements."""
  return (
      '<?xml version="1.0" encoding="UTF-8"?>\n'
      '<testsuite name="com.example.cahier.FooTest">\n'
      + "\n".join(testcases)
      + "\n</testsuite>\n"
  )


def testcase(name, child=""):
  """Returns a <testcase> element in com.example.cahier.FooTest."""
  return (
      f'<testcase name="{name}" classname="com.example.cahier.FooTest">'
      f"{child}</testcase>"
  )


class ReportFailedAndroidTestsTest(unittest.TestCase):

  def setUp(self):
    super().setUp()
    temp_dir = tempfile.TemporaryDirectory()
    self.addCleanup(temp_dir.cleanup)
    self.results_dir = temp_dir.name

  def write_results(self, relative_path, content):
    path = os.path.join(self.results_dir, relative_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
      f.write(content)

  def run_script(self):
    """Runs the script on self.results_dir and returns what it printed."""
    output = io.StringIO()
    with mock.patch.object(sys, "argv", [SCRIPT, self.results_dir]):
      with contextlib.redirect_stdout(output):
        runpy.run_path(SCRIPT, run_name="__main__")
    return output.getvalue()

  def annotations(self, output):
    return [line for line in output.splitlines() if line.startswith("::error")]

  def test_prints_nothing_when_all_tests_pass(self):
    self.write_results(
        "debug/TEST-device.xml", results(testcase("first"), testcase("second"))
    )

    self.assertEqual(self.run_script(), "")

  def test_prints_nothing_without_results(self):
    self.assertEqual(self.run_script(), "")

  def test_reports_failure_in_log_and_annotation(self):
    self.write_results(
        "debug/TEST-device.xml",
        results(
            testcase("passes"),
            testcase(
                "fails",
                "<failure>java.lang.AssertionError: expected 1\n"
                "\tat com.example.cahier.FooTest.fails(FooTest.kt:10)"
                "</failure>",
            ),
        ),
    )

    output = self.run_script()

    self.assertIn(
        "::group::com.example.cahier.FooTest.fails failed\n"
        "java.lang.AssertionError: expected 1\n"
        "\tat com.example.cahier.FooTest.fails(FooTest.kt:10)\n"
        "::endgroup::\n",
        output,
    )
    self.assertEqual(
        self.annotations(output),
        [
            "::error title=com.example.cahier.FooTest.fails::"
            "java.lang.AssertionError: expected 1%0A"
            "\tat com.example.cahier.FooTest.fails(FooTest.kt:10)"
        ],
    )

  def test_reports_errors_like_failures(self):
    self.write_results(
        "debug/TEST-device.xml",
        results(
            testcase("crashes", "<error>java.lang.NoSuchMethodError</error>")
        ),
    )

    self.assertEqual(
        self.annotations(self.run_script()),
        [
            "::error title=com.example.cahier.FooTest.crashes::"
            "java.lang.NoSuchMethodError"
        ],
    )

  def test_uses_message_attribute_without_failure_text(self):
    self.write_results(
        "debug/TEST-device.xml",
        results(testcase("fails", '<failure message="Process crashed."/>')),
    )

    self.assertEqual(
        self.annotations(self.run_script()),
        ["::error title=com.example.cahier.FooTest.fails::Process crashed."],
    )

  def test_escapes_title_and_message(self):
    self.write_results(
        "debug/TEST-device.xml",
        results(
            testcase(
                "fails[size: 10, color: red]",
                "<failure>100% wrong\nsecond line</failure>",
            )
        ),
    )

    output = self.run_script()

    self.assertEqual(
        self.annotations(output),
        [
            "::error title=com.example.cahier.FooTest.fails[size%3A 10%2C"
            " color%3A red]::100%25 wrong%0Asecond line"
        ],
    )
    # A group's title is its message, so ":" and "," stay as they are.
    self.assertIn(
        "::group::com.example.cahier.FooTest.fails[size: 10, color: red]"
        " failed\n",
        output,
    )

  def test_annotation_keeps_first_40_lines_and_log_keeps_all(self):
    lines = [f"line {i}" for i in range(1, 51)]
    self.write_results(
        "debug/TEST-device.xml",
        results(
            testcase("fails", "<failure>" + "\n".join(lines) + "</failure>")
        ),
    )

    output = self.run_script()

    self.assertIn("\n".join(lines), output)
    [annotation] = self.annotations(output)
    self.assertTrue(annotation.endswith("%0Aline 40"), annotation)

  def test_reports_failures_from_every_results_file(self):
    self.write_results(
        "debug/device-a/TEST-a.xml",
        results(testcase("first", "<failure>first failure</failure>")),
    )
    self.write_results(
        "debug/device-b/TEST-b.xml",
        results(testcase("second", "<failure>second failure</failure>")),
    )

    self.assertEqual(
        self.annotations(self.run_script()),
        [
            "::error title=com.example.cahier.FooTest.first::first failure",
            "::error title=com.example.cahier.FooTest.second::second failure",
        ],
    )

  def test_skips_files_that_are_not_valid_xml(self):
    self.write_results("debug/broken.xml", "not xml")
    self.write_results(
        "debug/TEST-device.xml",
        results(testcase("fails", "<failure>still reported</failure>")),
    )

    self.assertEqual(
        self.annotations(self.run_script()),
        ["::error title=com.example.cahier.FooTest.fails::still reported"],
    )


if __name__ == "__main__":
  unittest.main()
