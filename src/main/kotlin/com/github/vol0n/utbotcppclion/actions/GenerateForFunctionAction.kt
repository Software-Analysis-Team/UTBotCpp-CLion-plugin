package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingFunFromAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForFunctionAction: UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent -> client.generateForFunction(buildFunctionRequestFromEvent(e)) }

    override fun update(e: AnActionEvent) {
        val containingFun = getContainingFunFromAction(e)
        e.presentation.isEnabledAndVisible = (containingFun != null)
    }
}