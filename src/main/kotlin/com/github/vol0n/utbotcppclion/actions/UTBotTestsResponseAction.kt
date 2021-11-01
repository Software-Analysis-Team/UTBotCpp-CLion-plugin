package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.coroutinesScopeForGrpc
import com.github.vol0n.utbotcppclion.ui.GeneratorSettingsDialog
import com.github.vol0n.utbotcppclion.utils.handleTestsResponse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import testsgen.Testgen

abstract class UTBotTestsResponseAction : AnAction() {
    abstract val funToGetTestResponse: (AnActionEvent) -> Flow<Testgen.TestsResponse>
    override fun actionPerformed(e: AnActionEvent) {
        if (GeneratorSettingsDialog().showAndGet()) {
            coroutinesScopeForGrpc.launch {
                funToGetTestResponse(e).handleTestsResponse()
            }
        }
    }
}
