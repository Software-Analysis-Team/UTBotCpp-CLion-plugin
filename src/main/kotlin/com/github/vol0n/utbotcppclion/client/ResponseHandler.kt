package com.github.vol0n.utbotcppclion.client

import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateBuildDir
import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateJsonForProjectConfiguration
import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.actions.utils.notifyInfo
import com.github.vol0n.utbotcppclion.actions.utils.notifyUnknownResponse
import com.github.vol0n.utbotcppclion.services.Client
import com.github.vol0n.utbotcppclion.services.ProjectSettings
import com.github.vol0n.utbotcppclion.ui.UTBotRequestProgressIndicator
import com.github.vol0n.utbotcppclion.utils.createFileAndMakeDirs
import com.github.vol0n.utbotcppclion.utils.refreshAndFindIOFile
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import testsgen.Testgen
import testsgen.Util

class ResponseHandle(val project: Project, val client: Client) {
    val projectSettings: ProjectSettings = project.service()
    val logger = Logger.getInstance(this::class.java)

    fun handleTestsResponse(response: Testgen.TestsResponse, uiProgress: UTBotRequestProgressIndicator) {
        if (response.hasProgress()) {
            handleProgress(response.progress, uiProgress)
        }
        handleSourceCode(response.testSourcesList)
        if (response.hasStubs()) {
            handleStubsResponse(response.stubs, uiProgress)
        }
    }

    fun handleStubsResponse(response: Testgen.StubsResponse, uiProgress: UTBotRequestProgressIndicator) {
        if (response.hasProgress()) {
            handleProgress(response.progress, uiProgress)
        }
        handleSourceCode(response.stubSourcesList)
    }

    fun handleSourceCode(sources: List<Util.SourceCode>) {
        sources.forEach { sourceCode ->
            if (sourceCode.code.isNotEmpty()) {
                createFileAndMakeDirs(
                    projectSettings.convertFromRemotePathIfNeeded(sourceCode.filePath),
                    sourceCode.code
                )
            }
        }
    }

    fun handleProgress(
        serverProgress: Util.Progress,
        uiProgress: UTBotRequestProgressIndicator,
        onCompleted: () -> Unit = {}
    ) {
        if (serverProgress.completed) {
            onCompleted()
        }
        // update progress in status bar
        uiProgress.fraction = serverProgress.percent
        uiProgress.text = serverProgress.message
    }

    suspend fun handleTestsStream(grpcStream: Flow<Testgen.TestsResponse>, progressName: String) {
        handleWithUIProgress(grpcStream, progressName, this::handleTestsResponse)
        refreshAndFindIOFile(projectSettings.testDirPath)
    }

    suspend fun handleProjectConfigResponseStream(
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
    suspend fun <T> handleWithUIProgress(
        grpcStream: Flow<T>,
        uiProgressName: String,
        dataHandler: (T, UTBotRequestProgressIndicator) -> Unit,
    ): T? {
        val uiProgress = UTBotRequestProgressIndicator(uiProgressName)
        uiProgress.start()
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
        uiProgress.complete()
        return lastReceivedData
    }
}

fun interface ResponseHandler<T> {
    fun handleResponse(response: T)
}

open class ResponseHandlerImpl<T, N>(
    val project: Project,
    val nextHandler: ResponseHandler<N>? = null,
    val dataToPassNext: N? = null
) : ResponseHandler<T> {
    protected val logger = Logger.getInstance(this::class.java)
    protected val projectSettings = project.service<ProjectSettings>()

    /**
     * Try to handle the response and pass it to next handler if unsuccessful
     */
    override fun handleResponse(response: T) {
        processResponse(response) ?: if (dataToPassNext != null && nextHandler != null) {
            nextHandler.handleResponse(dataToPassNext)
        }
    }

    /**
     * Try to handle the response and return null if unsuccessful
     *
     * @return null if the nextHandler
     */
    protected open fun processResponse(response: T): Any? {
        return null
    }
}

class SourceCodeHandler<N>(
    project: Project, nextHandler: ResponseHandler<N>? = null,
    nextRequest: N? = null
) : ResponseHandlerImpl<List<Util.SourceCode>, N>(project, nextHandler, nextRequest) {

    override fun processResponse(response: List<Util.SourceCode>): Any {
        logger.info("processing source codes: \n$")
        response.forEach { sourceCode ->
            if (sourceCode.code.isNotEmpty()) {
                logger.info("Creating source file: ${sourceCode.filePath}")
                logger.info("Contents: \n ${sourceCode.code}")
                createFileAndMakeDirs(
                    projectSettings.convertFromRemotePathIfNeeded(sourceCode.filePath),
                    sourceCode.code
                )
            }
        }
        return Any()
    }
}

class ProgressHandler<N, T>(
    val uiProgress: UTBotRequestProgressIndicator,
    val onCompletedHandler: ResponseHandler<T>? = null,
    val onCompletedData: T? = null,
    project: Project,
    nextHandler: ResponseHandler<N>? = null,
    dataToPassNext: N? = null
) : ResponseHandlerImpl<Util.Progress?, N>(project) {
    override fun handleResponse(response: Util.Progress?) {
        if (processResponse(response) == null && onCompletedData != null) {
            onCompletedHandler?.handleResponse(onCompletedData)
        } else if (dataToPassNext != null) {
            nextHandler?.handleResponse(dataToPassNext)
        }
    }

    override fun processResponse(response: Util.Progress?): Any? {
        if (response == null || response.completed) {
            return null
        }
        // update progress in status bar
        uiProgress.fraction = response.percent
        uiProgress.text = response.message
        return Any()
    }
}

class ProjectConfigGenerateJsonHandler<N>(
    project: Project,
    nextHandler: ResponseHandler<N>? = null,
    nextRequest: N? = null
) : ResponseHandlerImpl<Testgen.ProjectConfigResponse, N>(project, nextHandler, nextRequest) {
    override fun processResponse(response: Testgen.ProjectConfigResponse): Any? {
        logger.info("handling json generation: \n $response")
        when (response.type) {
            Testgen.ProjectConfigStatus.IS_OK -> notifyInfo("Successfully configured project!", project)
            Testgen.ProjectConfigStatus.RUN_JSON_GENERATION_FAILED -> notifyError(
                "UTBot tried to configure project, but failed with the " +
                        "following message: ${response.message}", project
            )
            else -> notifyUnknownResponse(response, project)
        }
        return Any()
    }
}

class ProjectConfigHandler<N>(
    project: Project,
    nextHandler: ResponseHandler<N>? = null,
    nextRequest: N? = null
) : ResponseHandlerImpl<Testgen.ProjectConfigResponse, N>(project, nextHandler, nextRequest) {
    override fun processResponse(response: Testgen.ProjectConfigResponse): Any? {
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
        return Any()
    }
}

class ProjectConfigCreateBuildDirHandler(
    project: Project,
) : ResponseHandlerImpl<Testgen.ProjectConfigResponse, Any>(project) {
    override fun processResponse(response: Testgen.ProjectConfigResponse): Any? {
        when (response.type) {
            Testgen.ProjectConfigStatus.IS_OK -> {
                notifyInfo("Build dir was created!", project)
                project.service<Client>().configureProject()
            }
            Testgen.ProjectConfigStatus.BUILD_DIR_CREATION_FAILED -> {
                notifyInfo("Failed to create build dir! ${response.message}", project)
            }
            else -> notifyUnknownResponse(response, project)
        }
        return Any()
    }
}
