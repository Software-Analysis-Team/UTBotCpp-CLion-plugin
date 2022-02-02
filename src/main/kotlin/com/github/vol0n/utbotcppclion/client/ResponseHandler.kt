package com.github.vol0n.utbotcppclion.client

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageRunConfig
import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateBuildDir
import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateJsonForProjectConfiguration
import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.actions.utils.notifyInfo
import com.github.vol0n.utbotcppclion.actions.utils.notifyUnknownResponse
import com.github.vol0n.utbotcppclion.coverage.UTBotCoverageProgramRunner
import com.github.vol0n.utbotcppclion.services.Client
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.ui.UTBotRequestProgressIndicator
import com.github.vol0n.utbotcppclion.utils.createFileAndMakeDirs
import com.github.vol0n.utbotcppclion.utils.refreshAndFindIOFile
import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageRunnerData
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.rt.coverage.data.CoverageData
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import testsgen.Testgen
import testsgen.Util

class ResponseHandle(val project: Project, val client: Client) {
    private val projectSettings: ProjectSettings = project.service()
    private val logger = Logger.getInstance(this::class.java)

    private fun handleTestsResponse(response: Testgen.TestsResponse, uiProgress: UTBotRequestProgressIndicator) {
        if (response.hasProgress()) {
            handleProgress(response.progress, uiProgress)
        }
        handleSourceCode(response.testSourcesList)
        if (response.hasStubs()) {
            handleStubsResponse(response.stubs, uiProgress)
        }
    }

    suspend fun handleCoverageAndResultsResponse(grpcStream: Flow<Testgen.CoverageAndResultsResponse>, uiProgressName: String) {
        fun dataHandler(response: Testgen.CoverageAndResultsResponse, uiProgress: UTBotRequestProgressIndicator) {
            if (response.hasProgress()) {
                ApplicationManager.getApplication().invokeLater {
                    handleProgress(response.progress, uiProgress)
                }
            }
        }
        val lastResponse = handleWithUIProgress(grpcStream, uiProgressName, ::dataHandler)
        lastResponse ?: error("Last response is Null")
        if (lastResponse.errorMessage.isNotEmpty()) {
            notifyError(lastResponse.errorMessage, project)
        }

        val conf = UTBotRunWithCoverageRunConfig(lastResponse.coveragesList, project, "Handle")
        logger.info("LAUNCHING PROCESSING OF COVERAGE")
        CoverageDataManager.getInstance(project).processGatheredCoverage(conf, CoverageRunnerData())
    }

    private fun handleStubsResponse(response: Testgen.StubsResponse, uiProgress: UTBotRequestProgressIndicator) {
        if (response.hasProgress()) {
            handleProgress(response.progress, uiProgress)
        }
        handleSourceCode(response.stubSourcesList)
    }

    private fun handleSourceCode(sources: List<Util.SourceCode>) {
        sources.forEach { sourceCode ->
            val filePath: String = projectSettings.convertFromRemotePathIfNeeded(sourceCode.filePath)
            if (sourceCode.code.isNotEmpty()) {
                createFileAndMakeDirs(
                    filePath,
                    sourceCode.code
                )
            }
            refreshAndFindIOFile(filePath)
        }
    }

    private fun handleProgress(
        serverProgress: Util.Progress,
        uiProgress: UTBotRequestProgressIndicator,
        onCompleted: () -> Unit = {}
    ) {
        if (serverProgress.completed) {
            onCompleted()
        }
        // update progress in status bar
        uiProgress.fraction = serverProgress.percent
        uiProgress.text = serverProgress.message + "..."
    }

    suspend fun handleTestsStream(grpcStream: Flow<Testgen.TestsResponse>, progressName: String) {
        handleWithUIProgress(grpcStream, progressName, this::handleTestsResponse)
        refreshAndFindIOFile(projectSettings.testDirPath)
    }

    private suspend fun handleProjectConfigResponseStream(
        grpcStream: Flow<Testgen.ProjectConfigResponse>,
        progressName: String,
        onProgressCompletion: (Testgen.ProjectConfigResponse) -> Unit
    ) {
        fun handleLastResponse(response: Testgen.ProjectConfigResponse, uiProgress: UTBotRequestProgressIndicator) {
            if (!response.hasProgress() || response.progress.completed) {
                onProgressCompletion(response)
                return
            }
            handleProgress(response.progress, uiProgress)
        }
        handleWithUIProgress(grpcStream, progressName, ::handleLastResponse)
    }

    suspend fun handleCheckConfigurationResponse(
        grpcStream: Flow<Testgen.ProjectConfigResponse>,
        uiProgressName: String
    ) {
        fun handleProjectConfigCheckResponse(response: Testgen.ProjectConfigResponse) {
            when (response.type) {
                Testgen.ProjectConfigStatus.IS_OK -> {
                    notifyInfo("Project is configured!", project)
                }
                Testgen.ProjectConfigStatus.BUILD_DIR_NOT_FOUND -> {
                    notifyError(
                        "Project build dir not found! ${response.message}",
                        project,
                        AskServerToGenerateBuildDir()
                    )
                }
                Testgen.ProjectConfigStatus.LINK_COMMANDS_JSON_NOT_FOUND, Testgen.ProjectConfigStatus.COMPILE_COMMANDS_JSON_NOT_FOUND -> {
                    val missingFileName =
                        if (response.type == Testgen.ProjectConfigStatus.LINK_COMMANDS_JSON_NOT_FOUND) "link_commands.json" else "compile_commands.json"
                    notifyError(
                        "Project is not configured properly: $missingFileName is missing in the build folder.",
                        project, AskServerToGenerateJsonForProjectConfiguration()
                    )
                }
                else -> notifyUnknownResponse(response, project)
            }
        }

        handleProjectConfigResponseStream(grpcStream, uiProgressName, ::handleProjectConfigCheckResponse)
    }

    suspend fun handleCreateBuildDirResponse(
        grpcStream: Flow<Testgen.ProjectConfigResponse>,
        uiProgressName: String
    ) {
        fun handleBuildDirCreation(serverResponse: Testgen.ProjectConfigResponse) {
            when (serverResponse.type) {
                Testgen.ProjectConfigStatus.IS_OK -> {
                    notifyInfo("Build dir was created!", project)
                    client.configureProject()
                }
                Testgen.ProjectConfigStatus.BUILD_DIR_CREATION_FAILED -> {
                    notifyInfo("Failed to create build dir! ${serverResponse.message}", project)
                }
                else -> notifyUnknownResponse(serverResponse, project)
            }
        }

        handleProjectConfigResponseStream(grpcStream, uiProgressName, ::handleBuildDirCreation)
        refreshAndFindIOFile(projectSettings.buildDirPath)
    }


    suspend fun handleGenerateJsonResponse(
        grpcStream: Flow<Testgen.ProjectConfigResponse>,
        uiProgressName: String
    ) {
        fun handleJSONGeneration(serverResponse: Testgen.ProjectConfigResponse) {
            when (serverResponse.type) {
                Testgen.ProjectConfigStatus.IS_OK -> notifyInfo("Successfully configured project!", project)
                Testgen.ProjectConfigStatus.RUN_JSON_GENERATION_FAILED -> notifyError(
                    "UTBot tried to configure project, but failed with the " +
                            "following message: ${serverResponse.message}", project
                )
                else -> notifyUnknownResponse(serverResponse, project)
            }
        }
        handleProjectConfigResponseStream(grpcStream, uiProgressName, ::handleJSONGeneration)
        refreshAndFindIOFile(projectSettings.buildDirPath)
    }


    /**
     * Handle server stream of data messages showing progress in the status bar
     *
     * @param dataHandler - handles the data, and updates progress in the status bar
     * @param uiProgressName - name that will be displayed in the UI for this progress
     * @param grpcStream - stream of data messages
     * @return last received message, if no messages were received - return null
     */
    private suspend fun <T> handleWithUIProgress(
        grpcStream: Flow<T>,
        uiProgressName: String,
        dataHandler: (T, UTBotRequestProgressIndicator) -> Unit,
    ): T? {
        val uiProgress = UTBotRequestProgressIndicator(uiProgressName)
        ApplicationManager.getApplication().invokeLater {
            uiProgress.start()
        }
        uiProgress.requestJob = coroutineContext[Job]
        var lastReceivedData: T? = null
        grpcStream
            .catch { exception ->
                logger.info("In catch of handleWithProgress")
                logger.warn(exception.message)
                exception.message?.let { notifyError(it, project) }
            }
            .collect {
                lastReceivedData = it
                dataHandler(it, uiProgress)
            }
        ApplicationManager.getApplication().invokeLater {
            uiProgress.complete()
        }
        return lastReceivedData
    }
}