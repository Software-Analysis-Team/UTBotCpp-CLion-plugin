package com.github.vol0n.utbotcppclion.utils

import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import testsgen.Testgen
import java.io.File
import java.nio.file.Paths

fun relativize(from: String, to: String): String {
        val toPath = Paths.get(to)
        val fromPath = Paths.get(from)
        return fromPath.relativize(toPath).toString()
}

suspend fun Flow<Testgen.TestsResponse>.handleTestsResponse() {
        this.collect {
                        testResponse -> testResponse.testSourcesList.map { sourceCode ->
                        File(sourceCode.filePath).also {
                                it.parentFile?.mkdirs()
                                it.createNewFile()
                                it.writeText(sourceCode.code)
                                LocalFileSystem.getInstance()?.refreshAndFindFileByIoFile(it)
                        }
                }
        }
}