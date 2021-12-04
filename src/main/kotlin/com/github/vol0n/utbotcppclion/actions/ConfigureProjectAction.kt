package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ConfigureProjectAction : AnAction() {
    init {
        templatePresentation.text = "Ask Server To Configure Project"
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.client.configureProject()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
