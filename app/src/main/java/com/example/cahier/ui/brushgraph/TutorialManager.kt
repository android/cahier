package com.example.cahier.ui.brushgraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.ink.brush.BrushFamily
import com.example.cahier.ui.brushgraph.data.BrushGraphRepository
import com.example.cahier.ui.brushgraph.model.TutorialAction
import com.example.cahier.ui.brushgraph.model.TutorialStep
import com.example.cahier.ui.brushgraph.model.TUTORIAL_STEPS

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
