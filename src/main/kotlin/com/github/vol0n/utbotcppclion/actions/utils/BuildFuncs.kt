package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.services.GeneratorSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.utils.relativize
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import testsgen.Testgen
import testsgen.Util

fun buildSettingsContext(params: GeneratorSettings): Testgen.SettingsContext {
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
        .setSettingsContext(buildSettingsContext(ApplicationManager.getApplication().getService(GeneratorSettings::class.java)))
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

fun buildFunctionRequest(project: Project, params: ProjectSettings, line: Int, filePath: String): Testgen.FunctionRequest {
    return Testgen.FunctionRequest.newBuilder()
        .setLineRequest(buildLineRequest(project, params, line, filePath))
        .build()
}

fun buildLineRequestFromEvent(e: AnActionEvent): Testgen.LineRequest {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    val projectPath: String = project.basePath!!
    val relativeFilePath = relativize(projectPath, filePath)
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val projectSettings = project.service<ProjectSettings>()
    val lineNumber = editor.caretModel.logicalPosition.line
    return buildLineRequest(project, projectSettings, lineNumber, relativeFilePath)
}

fun buildFunctionRequestFromEvent(e: AnActionEvent): Testgen.FunctionRequest {
    val lineRequest = buildLineRequestFromEvent(e)
    return Testgen.FunctionRequest.newBuilder()
        .setLineRequest(lineRequest)
        .build()
}

fun buildProjectRequestFromEvent(e: AnActionEvent): Testgen.ProjectRequest = buildProjectRequest(e.project!!, e.project!!.service())

fun buildFileRequestFromEvent(e: AnActionEvent): Testgen.FileRequest {
    // this function is supposed to be called in actions' performAction(), so update() validated these properties
    val project: Project = e.project!!
    val projectPath: String = project.basePath!!
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    return Testgen.FileRequest.newBuilder()
        .setProjectRequest(buildProjectRequest(project, project.service()))
        .setFilePath(relativize(projectPath, filePath))
        .build()
}

fun buildPredicateInfo(predicate: String, returnValue: String, type: Util.ValidationType): Util.PredicateInfo {
    return Util.PredicateInfo.newBuilder()
        .setPredicate(predicate)
        .setReturnValue(returnValue)
        .setType(type)
        .build()
}

fun buildClassRequestFromEvent(e: AnActionEvent) = Testgen.ClassRequest.newBuilder().setLineRequest(
    buildLineRequestFromEvent(e)
).build()

fun buildFolderRequestFromEvent(e: AnActionEvent) = Testgen.FolderRequest.newBuilder()
    .setProjectRequest(buildProjectRequestFromEvent(e))
    .setFolderPath(relativize(e.project!!.basePath!!, e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path))
    .build()

fun buildSnippetRequestFromEvent(e: AnActionEvent) = Testgen.SnippetRequest.newBuilder()
    .setProjectContext(buildProjectContext(e.project!!.service(), e.project!!))
    .setSettingsContext(buildSettingsContext(service()))
    .setFilePath(relativize(e.project?.basePath!!, e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path))
    .build()

