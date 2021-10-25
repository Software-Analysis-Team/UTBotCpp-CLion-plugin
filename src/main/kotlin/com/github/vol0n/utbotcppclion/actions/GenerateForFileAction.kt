package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.github.vol0n.utbotcppclion.actions.utils.coroutinesScopeForGrpc
import com.github.vol0n.utbotcppclion.ui.GeneratorSettingsDialog
import com.github.vol0n.utbotcppclion.utils.handleTestsResponse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import kotlinx.coroutines.launch

class GenerateForFileAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (GeneratorSettingsDialog().showAndGet()) {
            coroutinesScopeForGrpc.launch {
                client.generateForFile(buildFileRequestFromEvent(e)).handleTestsResponse()
            }
        }
    }

    // action is available only if the selected file ends in .c or .cpp
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = """.*\.(cpp|hpp|c|h)""".toRegex().matches(file?.name ?: "")
    }
}
