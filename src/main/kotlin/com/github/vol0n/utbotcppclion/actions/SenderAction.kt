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

/**
 * Action to send a selected string to a server and
 * create a new directory with file containing text returned from server.
 * Server returns "hello" + string.
 */
class SenderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        if (selectedText != null) runBlocking {
            val client = ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java).client
            // send request to server
            val reply = client.greet(selectedText)
            ApplicationManager.getApplication().runWriteAction {
                val projectPath = e.project?.basePath
                // todo: take names from settings
                val directoryName = "generated"
                val fileName = "SimpleFileName.cpp"
                if (projectPath != null) {
                    // create new dir
                    File("$projectPath/$directoryName").also {
                        it.mkdir()
                        val file = File(it.absolutePath, fileName)
                        file.createNewFile()
                        file.writeText(reply)
                    }
                    VirtualFileManager.getInstance().asyncRefresh(null)
                }
            }
        }
    }
}


