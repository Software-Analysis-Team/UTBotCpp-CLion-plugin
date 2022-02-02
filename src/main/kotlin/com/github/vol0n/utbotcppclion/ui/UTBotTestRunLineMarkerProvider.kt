package com.github.vol0n.utbotcppclion.ui

import com.github.vol0n.utbotcppclion.actions.RunWithCoverageAction
import com.github.vol0n.utbotcppclion.services.TestsResultsStorage
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.DocumentUtil
import javax.swing.Icon

data class TestNameAndTestSuite(val name: String = "", val suite: String = "") {
    companion object {
        fun getFromPsiElement(element: PsiElement): TestNameAndTestSuite {
            val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile) ?: error(
                "Could not get document"
            )
            val startOffset = DocumentUtil.getLineStartOffset(element.textOffset, document)
            val endOffset = DocumentUtil.getLineEndOffset(element.textOffset, document)

            val lineText = document.text.substring(startOffset..endOffset)
            val testArgs = """\((.+)\)""".toRegex().find(lineText)?.groupValues?.getOrNull(1)?.let {
                """([^\s,]+),\s*([^\s,]+)""".toRegex().find(it)?.destructured
            }
            val suiteName = testArgs?.component1() ?: ""
            val testedMethodName = testArgs?.component2() ?: ""
            return TestNameAndTestSuite(testedMethodName, suiteName)
        }
    }
}

class UTBotTestRunLineMarkerProvider : LineMarkerProvider {
    private class UTBotRunWithCoverageLineMarkerInfo(callElement: PsiElement, message: String, icon: Icon) :
        LineMarkerInfo<PsiElement>(
            callElement,
            callElement.textRange,
            icon,
            { message },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { message }
        ) {
        val myClickAction = object : RunWithCoverageAction() {
            override val element  = callElement
        }

        override fun createGutterRenderer(): GutterIconRenderer {
            return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
                override fun isNavigateAction(): Boolean = true
                override fun getClickAction(): AnAction {
                    println("getClickAction was called!!!")
                    return myClickAction
                }
            }
        }
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.firstChild != null || (element.text != "TEST" && element.text != "UTBot")) {
            return null
        }
        val message = if (element.text == "TEST") "UTBot: Run with coverage" else "Run all tests with coverage"
        val icon = element.project.service<TestsResultsStorage>().getTestStatusIcon(element)
        return UTBotRunWithCoverageLineMarkerInfo(element, message, icon)
    }
}
