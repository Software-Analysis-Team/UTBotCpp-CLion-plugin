package com.github.vol0n.utbotcppclion.actions

import com.github.vol0n.utbotcppclion.services.GeneratorSettings
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import testsgen.Testgen
import testsgen.Util

internal val LOG = Logger.getInstance("GrpcMessageUtil")

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
    LOG.info("In getProjectContextMessage")
    return Testgen.ProjectContext.newBuilder()
        .setProjectName(project.name)
        .setProjectPath(params.getProjectPath())
        .setBuildDirRelativePath(params.getRelativeBuildDirPath())
        .setResultsDirRelativePath("") // this path is used only for console interface, server don't use it.
        .setTestDirPath(params.getAbsoluteTestDirPath())
        .build()
}

fun getProjectRequestMessage(project: Project, params: ProjectSettings): Testgen.ProjectRequest {
    LOG.info("In getProjectRequestMessage")
    return Testgen.ProjectRequest.newBuilder()
        .setSettingsContext(
            getSettingsContextMessage(
                project.service<GeneratorSettings>()
            )
        )
        .setProjectContext(getProjectContextMessage(params, project))
        .setTargetPath(params.getAbsoluteTargetPath())
        .addAllSourcePaths(params.getAbsoluteSourcePaths())
        .setSynchronizeCode(params.synchronizeCode)
        .build()
}

fun getSourceInfoMessage(line: Int, filePath: String, project: Project): Util.SourceInfo {
    val projectSettings = project.service<ProjectSettings>()
    return Util.SourceInfo.newBuilder()
        .setLine(line)
        .setFilePath(projectSettings.convertToRemotePathIfNeeded(filePath))
        .build()
}

fun getLineRequestMessage(project: Project, params: ProjectSettings, line: Int, filePath: String): Testgen.LineRequest {
    LOG.info("In getLineRequestMessage: which takes many parameters")
    val projectRequest = getProjectRequestMessage(project, params)
    val sourceInfo = getSourceInfoMessage(line, filePath, project)
    LOG.info("Before returning from getLineRequestMessage: which takes many parameters")
    return Testgen.LineRequest.newBuilder()
        .setProjectRequest(projectRequest)
        .setSourceInfo(sourceInfo)
        .build()
}

fun getLineRequestMessage(e: AnActionEvent): Testgen.LineRequest {
    LOG.info("In getLineRequestMessage")
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val projectSettings = project.service<ProjectSettings>()
    val lineNumber = editor.caretModel.logicalPosition.line + 1
    val result = getLineRequestMessage(project, projectSettings, lineNumber, filePath)
    LOG.info("Before returning from getLIneRequestMessage")
    return result
}

fun getFunctionRequestMessage(e: AnActionEvent): Testgen.FunctionRequest {
    LOG.info("in getFuctionRequestMessage")
    val lineRequest = getLineRequestMessage(e)
    return Testgen.FunctionRequest.newBuilder()
        .setLineRequest(lineRequest)
        .build()
}

fun getProjectRequestMessage(e: AnActionEvent): Testgen.ProjectRequest {
    LOG.info("in getProjectRequestMessage")
    return getProjectRequestMessage(e.project!!, e.project!!.service())
}

fun getFileRequestMessage(e: AnActionEvent): Testgen.FileRequest {
    LOG.info("in getFileRequestMessage")
    // this function is supposed to be called in actions' performAction(), so update() validated these properties
    val project: Project = e.project!!
    val projectSettings = project.service<ProjectSettings>()
    val filePath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    return Testgen.FileRequest.newBuilder()
        .setProjectRequest(getProjectRequestMessage(project, projectSettings))
        .setFilePath(projectSettings.convertToRemotePathIfNeeded(filePath))
        .build()
}

fun getPredicateInfoMessage(predicate: String, returnValue: String, type: Util.ValidationType): Util.PredicateInfo {
    LOG.info("in getPredicateInfoMessage")
    return Util.PredicateInfo.newBuilder()
        .setPredicate(predicate)
        .setReturnValue(returnValue)
        .setType(type)
        .build()
}

fun getClassRequestMessage(e: AnActionEvent): Testgen.ClassRequest {
    LOG.info("In getClassRequestMessage")
    return Testgen.ClassRequest.newBuilder().setLineRequest(
        getLineRequestMessage(e)
    ).build()
}

fun getFolderRequestMessage(e: AnActionEvent): Testgen.FolderRequest {
    LOG.info("in getFolderRequestMessage")
    val projectSettings = e.project!!.service<ProjectSettings>()
    val localPath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    return Testgen.FolderRequest.newBuilder()
        .setProjectRequest(getProjectRequestMessage(e))
        .setFolderPath(projectSettings.convertToRemotePathIfNeeded(localPath))
        .build()
}

fun getSnippetRequestMessage(e: AnActionEvent): Testgen.SnippetRequest {
    LOG.info("in getSnippetRequestMessage")
    val projectSettings = e.project!!.service<ProjectSettings>()
    val localPath = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).path
    return Testgen.SnippetRequest.newBuilder()
        .setProjectContext(getProjectContextMessage(projectSettings, e.project!!))
        .setSettingsContext(getSettingsContextMessage(e.project!!.service()))
        .setFilePath(projectSettings.convertToRemotePathIfNeeded(localPath))
        .build()
}

fun getAssertionRequestMessage(e: AnActionEvent): Testgen.AssertionRequest {
    LOG.info("in getAssertionRequestMessage")
    return Testgen.AssertionRequest.newBuilder()
        .setLineRequest(getLineRequestMessage(e))
        .build()
}

fun getPredicateRequestMessage(
    validationType: Util.ValidationType, returnValue: String, predicate: String,
    e: AnActionEvent
): Testgen.PredicateRequest {
    LOG.info("getPredicateRequestMessage")
    val predicateInfo = getPredicateInfoMessage(predicate, returnValue, validationType)
    return Testgen.PredicateRequest.newBuilder()
        .setLineRequest(getLineRequestMessage(e))
        .setPredicateInfo(predicateInfo)
        .build()
}

fun getProjectConfigRequestMessage(project: Project, configMode: Testgen.ConfigMode): Testgen.ProjectConfigRequest {
    LOG.info("getProjectConfigure")
    return Testgen.ProjectConfigRequest.newBuilder()
        .setProjectContext(getProjectContextMessage(project.service(), project))
        .setCmakeOptions(getCmakeOptions(project) ?: "")
        .setConfigMode(configMode)
        .build()
}

fun getCmakeOptions(project: Project): String? {
    return CMakeWorkspace.getInstance(project).profileInfos.find {
        it.profile.enabled
    }?.profile?.generationOptions
}
