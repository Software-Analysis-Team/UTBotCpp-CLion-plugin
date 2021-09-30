package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.client.send
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
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
            // send request to server
            val reply = send(selectedText)
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
                }
            }
        }
    }
}