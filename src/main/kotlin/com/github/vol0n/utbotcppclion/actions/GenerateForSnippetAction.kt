package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class GenerateForSnippetAction : UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent ->
        client.generateForSnippet(getSnippetRequestMessage(e))
    }

    override fun update(e: AnActionEvent) {
        val projectPath = e.project?.basePath
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = (projectPath != null && file != null)
    }
}
