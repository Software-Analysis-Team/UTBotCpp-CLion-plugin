package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.services.Client
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

// Only for development: will be removed
class HeartBeatAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<Client>()?.periodicHeartBeat()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Start HeartBeating The Server!"
    }
}
