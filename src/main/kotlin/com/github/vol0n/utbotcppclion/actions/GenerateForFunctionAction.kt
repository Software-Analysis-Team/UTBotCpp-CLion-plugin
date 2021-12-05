package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingFunction
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForFunctionAction : GenerateTestsBaseAction() {
    override fun updateIfServerAvailable(e: AnActionEvent) {
        val containingFun = getContainingFunction(e)
        e.presentation.isEnabledAndVisible = (containingFun != null)
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.client.generateForFunction(getFunctionRequestMessage(e))
    }
}
