package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.client.Client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

abstract class GenerateTestsBaseAction : AnAction() {
    override fun update(e: AnActionEvent) {
        if (e.project?.service<Client>()?.isServerAvailable() == true) {
            updateIfServerAvailable(e)
        } else {
            e.presentation.isEnabled = false
        }
    }

    abstract fun updateIfServerAvailable(e: AnActionEvent)
}
