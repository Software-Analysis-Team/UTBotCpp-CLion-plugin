package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingClass
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForClassAction : UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent ->
        client.generateForClass(getClassRequestMessage(e))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = (getContainingClass(e) != null)
    }
}
