package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.actions.utils.client
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class GenerateForFileAction : UTBotTestsResponseAction() {
    override val funToGetTestResponse = { e: AnActionEvent ->
        client.generateForFile(getFileRequestMessage(e))
    }

    // action is available only if the selected file ends in .cpp, .hpp, .c or .h
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = """.*\.(cpp|hpp|c|h)""".toRegex().matches(file?.name ?: "")
    }
}
