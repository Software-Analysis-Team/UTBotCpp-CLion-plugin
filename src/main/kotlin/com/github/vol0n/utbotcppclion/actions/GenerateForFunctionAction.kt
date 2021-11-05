package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingFunction
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForFunctionAction : UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent -> client.generateForFunction(getFunctionRequestMessage(e)) }

    override fun update(e: AnActionEvent) {
        val containingFun = getContainingFunction(e)
        e.presentation.isEnabledAndVisible = (containingFun != null)
    }
}
