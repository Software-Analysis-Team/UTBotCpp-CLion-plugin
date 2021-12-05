package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

// For development only
internal class DevAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.client.doHandShake()
    }
}
