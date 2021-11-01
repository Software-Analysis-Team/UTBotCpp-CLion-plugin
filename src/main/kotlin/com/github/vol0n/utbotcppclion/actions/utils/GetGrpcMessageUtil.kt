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

fun getSettingsContextMessage(params: GeneratorSettings): Testgen.SettingsContext {
    return Testgen.SettingsContext.newBuilder()
        .setVerbose(params.verbose)
        .setUseStubs(params.useStubs)
        .setTimeoutPerTest(params.timeoutPerTest)
        .setTimeoutPerFunction(params.timeoutPerFunction)
        .setGenerateForStaticFunctions(params.generateForStaticFunctions)
        .setUseDeterministicSearcher(params.useDeterministicSearcher)
        .build()
}

fun getProjectContextMessage(params: ProjectSettings, project: Project): Testgen.ProjectContext {
    return Testgen.ProjectContext.newBuilder()
        .setProjectName(project.name)
        .setProjectPath(project.basePath)
        .setBuildDirRelativePath(params.getBuildDirPath())
        .setResultsDirRelativePath("") // this path is used only for console interface, server don't use it.
        .setTestDirPath(params.getTestDirPath())
        .build()
}

fun getProjectRequestMessage(project: Project, params: ProjectSettings): Testgen.ProjectRequest {
    return Testgen.ProjectRequest.newBuilder()
        .setSettingsContext(
            getSettingsContextMessage(
                ApplicationManager.getApplication().getService(GeneratorSettings::class.java)
            )
        )
        .setProjectContext(getProjectContextMessage(params, project))
        .setTargetPath(params.getTargetPath())
        .addAllSourcePaths(params.getSourcePaths())
        .setSynchronizeCode(params.getSynchronizeCode())
        .build()
}

fun getSourceInfoMessage(line: Int, filePath: String): Util.SourceInfo {
    return Util.SourceInfo.newBuilder()
        .setLine(line)
        .setFilePath(filePath)
        .build()
}

fun getLineRequestMessage(project: Project, params: ProjectSettings, line: Int, filePath: String): Testgen.LineRequest {
    return Testgen.LineRequest.newBuilder()
        .setProjectRequest(getProjectRequestMessage(project, params))
        .setSourceInfo(getSourceInfoMessage(line, filePath))
        .build()
}

fun getLineRequestMessage(e: AnActionEvent): Testgen.LineRequest {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    val projectPath: String = project.basePath!!
    val relativeFilePath = relativize(projectPath, filePath)
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val projectSettings = project.service<ProjectSettings>()
    val lineNumber = editor.caretModel.logicalPosition.line
    return getLineRequestMessage(project, projectSettings, lineNumber, relativeFilePath)
}

fun getFunctionRequestMessage(e: AnActionEvent): Testgen.FunctionRequest {
    val lineRequest = getLineRequestMessage(e)
    return Testgen.FunctionRequest.newBuilder()
        .setLineRequest(lineRequest)
        .build()
}

fun getProjectRequestMessage(e: AnActionEvent): Testgen.ProjectRequest =
    getProjectRequestMessage(e.project!!, e.project!!.service())

fun getFileRequestMessage(e: AnActionEvent): Testgen.FileRequest {
    // this function is supposed to be called in actions' performAction(), so update() validated these properties
    val project: Project = e.project!!
    val projectPath: String = project.basePath!!
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    return Testgen.FileRequest.newBuilder()
        .setProjectRequest(getProjectRequestMessage(project, project.service()))
        .setFilePath(relativize(projectPath, filePath))
        .build()
}

fun getPredicateInfoMessage(predicate: String, returnValue: String, type: Util.ValidationType): Util.PredicateInfo {
    return Util.PredicateInfo.newBuilder()
        .setPredicate(predicate)
        .setReturnValue(returnValue)
        .setType(type)
        .build()
}

fun getClassRequestMessage(e: AnActionEvent): Testgen.ClassRequest = Testgen.ClassRequest.newBuilder().setLineRequest(
    getLineRequestMessage(e)
).build()

fun getFolderRequestMessage(e: AnActionEvent): Testgen.FolderRequest = Testgen.FolderRequest.newBuilder()
    .setProjectRequest(getProjectRequestMessage(e))
    .setFolderPath(relativize(e.project!!.basePath!!, e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path))
    .build()

fun getSnippetRequestMessage(e: AnActionEvent): Testgen.SnippetRequest = Testgen.SnippetRequest.newBuilder()
    .setProjectContext(getProjectContextMessage(e.project!!.service(), e.project!!))
    .setSettingsContext(getSettingsContextMessage(service()))
    .setFilePath(relativize(e.project?.basePath!!, e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path))
    .build()

fun getAssertionRequestMessage(e: AnActionEvent): Testgen.AssertionRequest = Testgen.AssertionRequest.newBuilder()
    .setLineRequest(getLineRequestMessage(e))
    .build()

fun getPredicateRequestMessage(
    validationType: Util.ValidationType, returnValue: String, predicate: String,
    e: AnActionEvent
): Testgen.PredicateRequest {
    val predicateInfo = getPredicateInfoMessage(predicate, returnValue, validationType)
    return Testgen.PredicateRequest.newBuilder()
        .setLineRequest(getLineRequestMessage(e))
        .setPredicateInfo(predicateInfo)
        .build()
}