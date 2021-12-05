package com.github.vol0n.utbotcppclion.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import testsgen.Testgen
import java.io.File
import java.nio.file.Paths

fun relativize(from: String, to: String): String {
    val toPath = Paths.get(to)
    val fromPath = Paths.get(from)
    return fromPath.relativize(toPath).toString()
}

fun createFileAndMakeDirs(filePath: String, text: String) {
    with(File(filePath)) {
        parentFile?.mkdirs()
        createNewFile()
        writeText(text)

        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance()?.refreshAndFindFileByIoFile(this)
        }
    }
}
