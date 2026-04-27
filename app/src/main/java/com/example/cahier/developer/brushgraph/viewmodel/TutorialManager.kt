/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
package com.example.cahier.developer.brushgraph.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.ink.brush.BrushFamily
import com.example.cahier.developer.brushgraph.data.BrushGraphRepository
import com.example.cahier.developer.brushgraph.data.TutorialAction
import com.example.cahier.developer.brushgraph.data.TutorialStep
import com.example.cahier.developer.brushgraph.data.TUTORIAL_STEPS

class TutorialManager(
  private val repository: BrushGraphRepository,
) {
  var tutorialStep by mutableStateOf<TutorialStep?>(null)
    private set

  var currentStepIndex by mutableIntStateOf(0)
    private set

  private val tutorialSteps = mutableStateListOf<TutorialStep>()

  var savedBrushFamily by mutableStateOf<BrushFamily?>(null)
    private set

  var isTutorialSandboxMode by mutableStateOf(false)
    private set

  fun startTutorial() {
    tutorialSteps.clear()
    tutorialSteps.addAll(TUTORIAL_STEPS)
    currentStepIndex = 0
    tutorialStep = tutorialSteps.getOrNull(currentStepIndex)
    repository.clearIssues()
  }

  fun startTutorialSandbox(currentBrushFamily: BrushFamily) {
    savedBrushFamily = currentBrushFamily
    isTutorialSandboxMode = true
    startTutorial()
  }

  fun advanceTutorial(action: TutorialAction = TutorialAction.CLICK_NEXT): Boolean {
    val step = tutorialStep
    if (step != null && step.actionRequired == action) {
      currentStepIndex++
      if (currentStepIndex < tutorialSteps.size) {
        tutorialStep = tutorialSteps[currentStepIndex]
      } else {
        tutorialStep = null // Tutorial finished!
      }
      return true
    }
    return false
  }

  fun regressTutorial() {
    if (currentStepIndex > 0) {
      currentStepIndex--
      tutorialStep = tutorialSteps[currentStepIndex]
    }
  }

  fun endTutorialSandbox(keepChanges: Boolean): BrushFamily? {
    isTutorialSandboxMode = false
    val brushToRestore = if (!keepChanges) savedBrushFamily else null
    savedBrushFamily = null
    tutorialStep = null
    return brushToRestore
  }
}
