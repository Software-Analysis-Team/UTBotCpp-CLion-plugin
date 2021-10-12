package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.grpcBuildMessages.buildProjectRequest
import com.github.vol0n.utbotcppclion.services.GenerateTestsSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.ui.GenerateTestsSettingsDialog
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import testsgen.Testgen
import java.io.File

class GenerateForFileAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.project ?: return
        val client = ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java).client
        val genSettings = ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java)
        if (GenerateTestsSettingsDialog(
                genSettings
            ).showAndGet()) {
            val projectSettings = project.getService(ProjectSettings::class.java) ?: return
            val req = Testgen.FileRequest.newBuilder()
                .setProjectRequest(buildProjectRequest(project, projectSettings))
                .setFilePath(relativize(project.basePath ?: "", file.path))
                .build()
            runBlocking {
                client.generateForFile(req).collect { testResponse ->
                    testResponse.testSourcesList.map { sourceCode ->
                        File(sourceCode.filePath).also {
                            it.createNewFile()
                            it.writeText(sourceCode.code)
                        }
                        VirtualFileManager.getInstance().asyncRefresh(null)
                    }
                }
            }
        }
    }

    // action is available only if the selected file ends in .c or .cpp
    override fun update(e: AnActionEvent) {
        super.update(e)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        if (file != null) {
            file.also {
                e.presentation.isEnabledAndVisible = it.name.endsWith(".cpp") || it.name.endsWith(".c")
            }
        } else {
            e.presentation.isVisible = false
        }
    }
}
