package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.UTBot
import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AskServerToGenerateJsonForProjectConfiguration :
    NotificationAction(UTBot.message("projectConfigure.generate.json")) {
    override fun actionPerformed(e: AnActionEvent, n: Notification) {
        actionPerformed(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.client.generateJSon()
    }
}
