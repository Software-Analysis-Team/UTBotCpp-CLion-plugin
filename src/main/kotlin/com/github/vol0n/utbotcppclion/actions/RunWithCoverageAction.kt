package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.RunConfig.UTBotConfigurationType
import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.coverage.UTBotCoverageProgramRunner
import com.github.vol0n.utbotcppclion.ui.TestNameAndTestSuite
import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

abstract class RunWithCoverageAction : AnAction() {
    abstract val element: PsiElement
    override fun actionPerformed(e: AnActionEvent) {
        println("Action RunWithCoverageAction was called")
        val testArgs = TestNameAndTestSuite.getFromPsiElement(element)
        val suiteName = testArgs.suite
        val testedMethodName = testArgs.name
        val request = getCoverageAndResultsRequest(e, suiteName, testedMethodName)
        e.client.getCoverageAndResults(request)
    }
}