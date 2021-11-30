package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AskServerToGenerateBuildDir: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.client.createBuildDir()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Ask Server To Generate Build Dir"
    }
}
