package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.models.TestNameAndTestSuite
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

class RunWithCoverageAction(val element: PsiElement) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        log.info("Action RunWithCoverageAction was called")
        if (element.containingFile == null)
            return
        log.info("psi element is valid: containing file not null")
        val testArgs = TestNameAndTestSuite.getFromPsiElement(element)
        val suiteName = testArgs.suite
        val testedMethodName = testArgs.name
        val request = getCoverageAndResultsRequest(e, suiteName, testedMethodName)
        e.client.getCoverageAndResults(request)
    }

    companion object {
        private val log = Logger.getInstance(this::class.java)
    }
}
