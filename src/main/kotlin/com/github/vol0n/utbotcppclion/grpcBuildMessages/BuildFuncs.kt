package com.github.vol0n.utbotcppclion.grpcBuildMessages

import com.github.vol0n.utbotcppclion.services.GenerateTestsSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import testsgen.Testgen
import testsgen.Util

fun buildSettingsContext(params: GenerateTestsSettings): Testgen.SettingsContext {
    return Testgen.SettingsContext.newBuilder()
        .setVerbose(params.verbose)
        .setUseStubs(params.useStubs)
        .setTimeoutPerTest(params.timeoutPerTest)
        .setTimeoutPerFunction(params.timeoutPerFunction)
        .setGenerateForStaticFunctions(params.generateForStaticFunctions)
        .setUseDeterministicSearcher(params.useDeterministicSearcher)
        .build()
}

fun buildProjectContext(params: ProjectSettings, project: Project): Testgen.ProjectContext {
    return Testgen.ProjectContext.newBuilder()
        .setProjectName(project.name)
        .setProjectPath(project.basePath)
        .setBuildDirRelativePath(params.getBuildDirPath())
        .setResultsDirRelativePath("") // this path is used only for console interface, server don't use it.
        .setTestDirPath(params.getTestDirPath())
        .build()
}

fun buildProjectRequest(project: Project, params: ProjectSettings): Testgen.ProjectRequest {
    return Testgen.ProjectRequest.newBuilder()
        .setSettingsContext(buildSettingsContext(ApplicationManager.getApplication().getService(GenerateTestsSettings::class.java)))
        .setProjectContext(buildProjectContext(params, project))
        .setTargetPath(params.getTargetPath())
        .addAllSourcePaths(params.getSourcePaths())
        .setSynchronizeCode(params.getSynchronizeCode())
        .build()
}

fun buildSourceInfo(line: Int, filePath: String): Util.SourceInfo {
    return Util.SourceInfo.newBuilder()
        .setLine(line)
        .setFilePath(filePath)
        .build()
}

fun buildLineRequest(project: Project, params: ProjectSettings, line: Int, filePath: String): Testgen.LineRequest {
    return Testgen.LineRequest.newBuilder()
        .setProjectRequest(buildProjectRequest(project, params))
        .setSourceInfo(buildSourceInfo(line, filePath))
        .build()
}