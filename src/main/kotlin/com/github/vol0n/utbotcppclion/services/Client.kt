package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateBuildDir
import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateJson
import com.github.vol0n.utbotcppclion.actions.getProjectConfigRequestMessage
import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.actions.utils.notifyInfo
import com.github.vol0n.utbotcppclion.actions.utils.notifyUnknownResponse
import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotConnectionChangedNotifier
import com.github.vol0n.utbotcppclion.ui.UTBotRequestProgressIndicator
import com.github.vol0n.utbotcppclion.utils.createFileAndMakeDirs
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import testsgen.Testgen
import testsgen.Util
import java.net.ConnectException

class Client(val project: Project) {
    val stub = GrpcStarter.startClient().stub
    val grpcCoroutineScope: CoroutineScope
    private var connectionStatus = ConnectionStatus.INIT
    private val connectionChangedPublisher =
        project.messageBus.syncPublisher(UTBotConnectionChangedNotifier.CONNECTION_CHANGED_TOPIC)
    private var heartBeatJob: Job? = null
    private val logger = Logger.getInstance(this::class.java)

    init {
        val handler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
        }
        grpcCoroutineScope = CoroutineScope(Dispatchers.Swing + handler)

        periodicHeartBeat()
    }

    fun getConnectionStatus() = connectionStatus
    fun isServerAvailable() = connectionStatus == ConnectionStatus.CONNECTED

    fun periodicHeartBeat() {
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        }
        logger.info("Start heartbeating the server!")
        heartBeatJob = grpcCoroutineScope.launch {
            while (true) {
                heartBeat()
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    fun handleTestResponse(response: Testgen.TestsResponse) {
        response.testSourcesList.map { sourceCode ->
            createFileAndMakeDirs(sourceCode.filePath, sourceCode.code)
        }
        response.stubs.stubSourcesList.map { sourceCode ->
            createFileAndMakeDirs(sourceCode.filePath, sourceCode.code)
        }
    }

    fun generateForFile(
        request: Testgen.FileRequest
    ) = stub.generateFileTests(request).handleWithProgress()

    fun generateForLine(
        request: Testgen.LineRequest
    ) = stub.generateLineTests(request).handleWithProgress()

    fun generateForPredicate(
        request: Testgen.PredicateRequest
    ) = stub.generatePredicateTests(request).handleWithProgress()

    fun generateForFunction(
        request: Testgen.FunctionRequest
    ) = stub.generateFunctionTests(request).handleWithProgress()

    fun generateForClass(
        request: Testgen.ClassRequest
    ) = stub.generateClassTests(request).handleWithProgress()

    fun generateForFolder(
        request: Testgen.FolderRequest
    ) = stub.generateFolderTests(request).handleWithProgress()

    fun generateForSnippet(
        request: Testgen.SnippetRequest
    ) = stub.generateSnippetTests(request).handleWithProgress()

    fun generateForAssertion(
        request: Testgen.AssertionRequest
    ) = stub.generateAssertionFailTests(request).handleWithProgress()

    suspend fun getFunctionReturnType(
        request: Testgen.FunctionRequest
    ): Testgen.FunctionTypeResponse = withContext(Dispatchers.IO) { stub.getFunctionReturnType(request) }

    private fun configureProject(
        request: Testgen.ProjectConfigRequest
    ): Flow<Testgen.ProjectConfigResponse> = stub.configureProject(request)

    suspend fun handShake(): Testgen.DummyResponse = stub.handshake(Testgen.DummyRequest.newBuilder().build())

    fun configureProject() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CHECK)
        stub.configureProject(request).handleWithProgress(
            "Configuring project",
            Testgen.ProjectConfigResponse::getProgress,
            this@Client::handleProjectConfigResponse
        )
    }

    fun handleProjectConfigResponse(response: Testgen.ProjectConfigResponse) {
        println("In handleProjectConfigResponse")
        when (response.type) {
            Testgen.ProjectConfigStatus.IS_OK -> {
                notifyInfo("Project is configured!", project)
            }
            Testgen.ProjectConfigStatus.BUILD_DIR_NOT_FOUND -> {
                notifyError("Project build dir not found! ${response.message}", project, AskServerToGenerateBuildDir())
            }
            Testgen.ProjectConfigStatus.LINK_COMMANDS_JSON_NOT_FOUND, Testgen.ProjectConfigStatus.COMPILE_COMMANDS_JSON_NOT_FOUND -> {
                val missingFileName =
                    if (response.type == Testgen.ProjectConfigStatus.LINK_COMMANDS_JSON_NOT_FOUND) "link_commands.json" else "compile_commands.json"
                notifyError(
                    "Project is not configured properly: $missingFileName is missing in the build folder.",
                    project, AskServerToGenerateJson()
                )
            }
            else -> notifyUnknownResponse(response, project)
        }
    }

    fun createBuildDir() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CREATE_BUILD_DIR)
        stub.configureProject(request).handleWithProgress(
            "Ask server to create build dir",
            Testgen.ProjectConfigResponse::getProgress,
            this@Client::handleBuildDirCreation
        )
    }

    fun handleBuildDirCreation(serverResponse: Testgen.ProjectConfigResponse) {
        when (serverResponse.type) {
            Testgen.ProjectConfigStatus.IS_OK -> {
                notifyInfo("Build dir was created!", project)
                configureProject()
            }
            Testgen.ProjectConfigStatus.BUILD_DIR_CREATION_FAILED -> {
                notifyInfo("Failed to create build dir! ${serverResponse.message}", project)
            }
            else -> notifyUnknownResponse(serverResponse, project)
        }
    }

    fun generateJSon() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.GENERATE_JSON_FILES)
        stub.configureProject(request).handleWithProgress(
            "Ask server to generate json",
            Testgen.ProjectConfigResponse::getProgress,
            this@Client::handleJSONGeneration,
        )
    }

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

    suspend fun heartBeat() {
        try {
            stub.heartbeat(Testgen.DummyRequest.newBuilder().build())
            connectionChangedPublisher.onChange(connectionStatus, ConnectionStatus.CONNECTED)
            connectionStatus = ConnectionStatus.CONNECTED
        } catch (e: Exception) {
            logger.info("Heartbeat failed: could not connect to server!")
            connectionChangedPublisher.onChange(connectionStatus, ConnectionStatus.BROKEN)
            connectionStatus = ConnectionStatus.BROKEN
        }
    }

    fun Flow<Testgen.TestsResponse>.handleWithProgress(progressName: String = "Generating Tests") {
        this.handleWithProgress(
            progressName, Testgen.TestsResponse::getProgress,
            handleResponse = this@Client::handleTestResponse
        )
    }

    // T: TestResponse | ProjectConfigResponse
    fun <T> Flow<T>.handleWithProgress(
        progressName: String,
        progressAccessor: T.() -> Util.Progress,
        onCompleted: (T) -> Unit = {},
        handleResponse: (T) -> Unit = {}
    ) {
        val uiProgress = UTBotRequestProgressIndicator(progressName)
        // start showing progress in status bar
        uiProgress.start()
        uiProgress.requestJob = grpcCoroutineScope.launch {
            withTimeout(STREAM_HANDLING_TIMEOUT) {
                this@handleWithProgress
                    .catch { exception -> logger.warn(exception.message) }
                    .collect {
                    // update progress in status bar
                    uiProgress.fraction = it.progressAccessor().percent
                    uiProgress.text = it.progressAccessor().message
                    handleResponse(it)
                    // when we receive last message from server stream
                    if (it.progressAccessor().completed) {
                        onCompleted(it)
                    }
                }
            }
            uiProgress.complete()
        }
    }

    companion object {
        const val HEARTBEAT_INTERVAL: Long = 500L
        const val STREAM_HANDLING_TIMEOUT: Long = 3000L
    }
}
