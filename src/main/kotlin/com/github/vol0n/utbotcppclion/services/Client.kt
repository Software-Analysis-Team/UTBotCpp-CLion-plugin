package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateBuildDir
import com.github.vol0n.utbotcppclion.actions.AskServerToGenerateJsonForProjectConfiguration
import com.github.vol0n.utbotcppclion.actions.getProjectConfigRequestMessage
import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.actions.utils.notifyInfo
import com.github.vol0n.utbotcppclion.actions.utils.notifyUnknownResponse
import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotConnectionChangedNotifier
import com.github.vol0n.utbotcppclion.ui.UTBotRequestProgressIndicator
import com.github.vol0n.utbotcppclion.utils.createFileAndMakeDirs

import testsgen.Testgen
import testsgen.Util

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

class Client(val project: Project) {
    private var stub = GrpcStarter.startClient().stub
    val grpcCoroutineScope: CoroutineScope
    private var connectionStatus = ConnectionStatus.INIT
    private val connectionChangedPublisher =
        project.messageBus.syncPublisher(UTBotConnectionChangedNotifier.CONNECTION_CHANGED_TOPIC)
    private var heartBeatJob: Job? = null
    private val logger = Logger.getInstance(this::class.java)
    private val metadata: io.grpc.Metadata = io.grpc.Metadata()

    init {
        val handler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
        }
        grpcCoroutineScope = CoroutineScope(Dispatchers.Swing + handler)

        periodicHeartBeat()
    }

    fun getConnectionStatus() = connectionStatus
    fun isServerAvailable() = connectionStatus == ConnectionStatus.CONNECTED

    fun doHandShake() {
        grpcCoroutineScope.launch {
            try {
                stub.handshake(Testgen.DummyRequest.newBuilder().build())
            } catch (e: Exception) {
                logger.warn("HandShake failed with the following error: ${e.message}")
            }
        }
    }

    private suspend fun setMetadata() {
        val clientID = System.getenv("USER") ?: "someUserId"
        try {
            stub.registerClient(Testgen.RegisterClientRequest.newBuilder().setClientId(clientID).build())
        } catch (e: Exception) {
            logger.warn("Setting metadata failed: ${e.message}")
        }
        metadata.put(io.grpc.Metadata.Key.of("clientid", io.grpc.Metadata.ASCII_STRING_MARSHALLER), clientID)
        stub = io.grpc.stub.MetadataUtils.attachHeaders(stub, metadata)
    }

    fun periodicHeartBeat() {
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        }
        logger.info("Started heartbeating the server!")
        heartBeatJob = grpcCoroutineScope.launch {
            setMetadata()
            while (true) {
                heartBeat()
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private fun handleTestResponse(response: Testgen.TestsResponse) {
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

    suspend fun handShake(): Testgen.DummyResponse = stub.handshake(Testgen.DummyRequest.newBuilder().build())

    private fun progressOrNull(response: Testgen.ProjectConfigResponse): Util.Progress? =
        if (response.hasProgress()) response.progress else null

    fun configureProject() {
        fun handleProjectConfigResponse(response: Testgen.ProjectConfigResponse) {
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

        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CHECK)
        stub.configureProject(request).handleWithProgress(
            "Configuring project",
            ::progressOrNull,
            ::handleProjectConfigResponse
        )
    }

    fun createBuildDir() {
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

        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CREATE_BUILD_DIR)
        stub.configureProject(request).handleWithProgress(
            "Ask server to create build dir",
            ::progressOrNull,
            ::handleBuildDirCreation
        )
    }

    fun generateJSon() {
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

        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.GENERATE_JSON_FILES)
        stub.configureProject(request).handleWithProgress(
            "Ask server to generate json",
            ::progressOrNull,
            ::handleJSONGeneration,
        )
    }

    private suspend fun heartBeat() {
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

    private fun Flow<Testgen.TestsResponse>.handleWithProgress(progressName: String = "Generating Tests") {
        this.handleWithProgress(
            progressName, Testgen.TestsResponse::getProgress,
            handleResponse = this@Client::handleTestResponse
        )
    }

    // T: TestResponse | ProjectConfigResponse
    fun <T> Flow<T>.handleWithProgress(
        progressName: String,
        progressAccessor: (T) -> Util.Progress?,
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
                        val progress = progressAccessor(it)
                        // when we receive last message from server stream
                        if (progress == null || progress.completed) {
                            onCompleted(it)
                            return@collect
                        }
                        // update progress in status bar
                        uiProgress.fraction = progress.percent
                        uiProgress.text = progress.message
                        handleResponse(it)
                    }
            }
            uiProgress.complete()
        }
    }

    companion object {
        const val HEARTBEAT_INTERVAL: Long = 500L
        const val STREAM_HANDLING_TIMEOUT: Long = 5000L
    }
}
