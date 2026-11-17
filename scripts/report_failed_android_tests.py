#!/usr/bin/env python3
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
"""Reports which Android instrumentation tests failed in a GitHub Actions job.

When `./gradlew :app:connectedAndroidTest` fails, it only prints the path of an
HTML report, and that report stays on the CI runner. This reads the JUnit XML
results that AGP writes alongside it, and for each failed test:

  * prints an `::error` workflow command, which GitHub shows as an error in the
    job log and as an annotation on the pull request's checks. Unlike job logs,
    annotations can be read through the GitHub API without admin rights to the
    repository.
  * prints the full failure to the job log, in a section that's collapsed under
    the test's name.

Usage: report_failed_android_tests.py [RESULTS_DIR]
"""

import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET

# Where `./gradlew :app:connectedAndroidTest` writes its JUnit XML results,
# relative to the repository root.
DEFAULT_RESULTS_DIR = "app/build/outputs/androidTest-results/connected"

# Annotations have a size limit, so each one gets the start of its failure. The
# job log gets all of it.
MAX_ANNOTATION_LINES = 40


def escape(text, is_property=False):
  """Escapes text for a workflow command, e.g. ::error title=<property>::<text>.

  A workflow command has to fit on one line, and ":" and "," separate the
  command's properties, so those characters get percent-encoded.

  Args:
    text: The text to escape.
    is_property: Whether the text is a property, like `title`, rather than the
      message.

  Returns:
    The escaped text.
  """
  text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
  if is_property:
    text = text.replace(":", "%3A").replace(",", "%2C")
  return text


def failed_tests(results_dir):
  """Yields (test name, failure details) for each failed test in results_dir.

  AGP writes each test's result to XML as a <testcase classname=... name=...>
  element, with a <failure> or <error> child that holds the stack trace if the
  test failed.

  Args:
    results_dir: The directory to search, including its subdirectories.
  """
  pattern = os.path.join(results_dir, "**", "*.xml")
  for path in sorted(glob.glob(pattern, recursive=True)):
    try:
      cases = list(ET.parse(path).getroot().iter("testcase"))
    except ET.ParseError:  # Skips anything in there that isn't valid XML.
      continue
    for case in cases:
      for problem in case.findall("failure") + case.findall("error"):
        test = f"{case.get('classname')}.{case.get('name')}"
        details = (problem.text or problem.get("message") or "").strip()
        yield test, details


def main(argv):
  parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
  parser.add_argument(
      "results_dir",
      nargs="?",
      default=DEFAULT_RESULTS_DIR,
      help=(
          "Directory with the JUnit XML results (default:"
          f" {DEFAULT_RESULTS_DIR})."
      ),
  )
  args = parser.parse_args(argv[1:])

  for test, details in failed_tests(args.results_dir):
    summary = "\n".join(details.splitlines()[:MAX_ANNOTATION_LINES])
    print(f"::error title={escape(test, is_property=True)}::{escape(summary)}")
    # The full failure, collapsed under the test's name in the job log.
    print(f"::group::{escape(test)} failed")
    print(details)
    print("::endgroup::")


if __name__ == "__main__":
  main(sys.argv)
