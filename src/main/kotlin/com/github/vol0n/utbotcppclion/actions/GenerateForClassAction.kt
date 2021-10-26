package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.getContainingClassFromAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GenerateForClassAction: UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent ->
        client.generateForClass(buildClassRequestFromEvent(e))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = (getContainingClassFromAction(e) != null)
    }
}