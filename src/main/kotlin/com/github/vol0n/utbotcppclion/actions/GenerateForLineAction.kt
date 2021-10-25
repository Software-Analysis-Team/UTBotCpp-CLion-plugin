package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.coroutinesScopeForGrpc
import com.github.vol0n.utbotcppclion.utils.handleTestsResponse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenerateForLineAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        coroutinesScopeForGrpc.launch(Dispatchers.IO) {
            client.generateForLine(buildLineRequestFromEvent(e)).handleTestsResponse()
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = (project != null) && (editor != null) && (file != null)
    }
}