package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.grpcBuildMessages.buildLineRequest
import com.github.vol0n.utbotcppclion.services.GenerateTestsSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.utils.handleTestsResponse
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.runBlocking

class GenerateForLineAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.getData(CommonDataKeys.PROJECT)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val lineNumber = editor!!.caretModel.logicalPosition.line
        val client = ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java).client
        val projectSettings = project!!.getService(ProjectSettings::class.java) ?: return
        runBlocking {
            client.generateForLine(buildLineRequest(project, projectSettings, lineNumber,
                relativize(project.basePath!!, file!!.path))).handleTestsResponse()
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