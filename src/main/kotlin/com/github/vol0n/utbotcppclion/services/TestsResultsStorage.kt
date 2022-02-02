package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.messaging.UTBotTestResultsReceivedListener
import com.github.vol0n.utbotcppclion.ui.TestNameAndTestSuite
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import javax.swing.Icon
import testsgen.Testgen

@Service
class TestsResultsStorage(val project: Project) {
    private val storage: MutableMap<String, Testgen.TestResultObject> = mutableMapOf()
    init {
        project.messageBus.connect().subscribe(UTBotTestResultsReceivedListener.TOPIC,
            UTBotTestResultsReceivedListener { results ->
                println("Received results")
                storage[results.testname] = results
            })
    }

    fun getTestStatusIcon(element: PsiElement): Icon {
        println("getTestStatusIcon was called: $element")
        if (element.text == "UTBot") {
            return AllIcons.RunConfigurations.TestState.Run_run
        }

        val testName: String = TestNameAndTestSuite.getFromPsiElement(element).name

        if (!storage.contains(testName) || testName.isEmpty()) {
            return AllIcons.RunConfigurations.TestState.Run
        }

        return when (storage[testName]!!.status) {
            Testgen.TestStatus.TEST_FAILED -> AllIcons.RunConfigurations.TestState.Red2
            Testgen.TestStatus.TEST_PASSED -> AllIcons.RunConfigurations.TestState.Green2
            else -> AllIcons.RunConfigurations.TestError
        }
    }
}