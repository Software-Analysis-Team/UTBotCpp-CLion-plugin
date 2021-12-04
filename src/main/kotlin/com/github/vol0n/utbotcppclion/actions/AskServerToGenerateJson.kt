package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AskServerToGenerateJson: AnAction() {
    init {
        templatePresentation.text = "Ask Server To Generate Json"
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.client.generateJSon()
    }
}
