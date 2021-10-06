package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.services.MyApplicationService
import com.intellij.ide.actions.ProjectViewEditSourceAction
import com.intellij.openapi.actionSystem.ActionPopupMenu
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileChooser.actions.FileChooserAction
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.runBlocking
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
            val client = ApplicationManager.getApplication().getService(MyApplicationService::class.java).client
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

class GenerateForFileAction: AnAction() {
    override fun actionPerformed(p1: AnActionEvent) {
        val file = p1.getRequiredData(CommonDataKeys.PSI_FILE)
        val client = ApplicationManager.getApplication().getService(MyApplicationService::class.java).client
        runBlocking {
            client.greet(file.name)
        }
        println(file.fileType.name)
    }

    // action is available only if the selected file ends in .c or .cpp
    override fun update(p0: AnActionEvent) {
        super.update(p0)
        val file = p0.getData(CommonDataKeys.PSI_FILE)
        if (file != null) {
            file.also {
                p0.presentation.isEnabledAndVisible = it.name.endsWith(".cpp") || it.name.endsWith(".c")
                println(it.name)
            }
        } else {
            p0.presentation.isEnabled = false
        }
    }
}