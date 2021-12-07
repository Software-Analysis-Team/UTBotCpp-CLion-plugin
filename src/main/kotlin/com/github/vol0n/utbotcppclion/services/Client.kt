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
import com.intellij.openapi.Disposable

import testsgen.Testgen
import testsgen.Util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlin.random.Random

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class Client(val project: Project) : Disposable {
    private var grpcStub = GrpcStarter.startClient().stub
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
                grpcStub.handshake(Testgen.DummyRequest.newBuilder().build())
            } catch (e: Exception) {
                logger.warn("HandShake failed with the following error: ${e.message}")
            }
        }
    }

    private suspend fun setMetadata() {
        fun createRandomSequence() = (1..RANDOM_SEQUENCE_LENGTH)
            .map { Random.nextInt(0, RANDOM_SEQUENCE_MAX_VALUE).toString() }
            .joinToString("")

        val clientID = project.name + (System.getenv("USER") ?: "unknownUser") + createRandomSequence()
        try {
            grpcStub.registerClient(Testgen.RegisterClientRequest.newBuilder().setClientId(clientID).build())
        } catch (e: Exception) {
            logger.warn("Setting metadata failed: ${e.message}")
        }
        metadata.put(io.grpc.Metadata.Key.of("clientid", io.grpc.Metadata.ASCII_STRING_MARSHALLER), clientID)
        grpcStub = io.grpc.stub.MetadataUtils.attachHeaders(grpcStub, metadata)
    }

    fun periodicHeartBeat() {
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        }
        logger.info("Started heartbeating the server!")
        heartBeatJob = grpcCoroutineScope.launch {
            setMetadata()
            while (isActive) {
                heartBeatOnce()
                delay(HEARTBEAT_INTERVAL)
            }
            logger.info("stopped heartBeating the server!")
        }
    }

    private fun handleTestResponse(response: Testgen.TestsResponse) {
        response.testSourcesList.map { sourceCode ->
            logger.info("Creating source file: ${sourceCode.filePath}")
            logger.info("Contents: \n ${sourceCode.code}")
            createFileAndMakeDirs(sourceCode.filePath, sourceCode.code)
        }
        response.stubs.stubSourcesList.map { sourceCode ->
            logger.info("Creating stub file: ${sourceCode.filePath}")
            logger.info("Contents: \n ${sourceCode.code}")
            createFileAndMakeDirs(sourceCode.filePath, sourceCode.code)
        }
    }

    fun generateForFile(
        request: Testgen.FileRequest
    ) {
        logger.info("in generateForFile")
        grpcStub.generateFileTests(request).handleWithProgress()
    }

    fun generateForLine(
        request: Testgen.LineRequest
    ) = grpcStub.generateLineTests(request).handleWithProgress()

    fun generateForPredicate(
        request: Testgen.PredicateRequest
    ) = grpcStub.generatePredicateTests(request).handleWithProgress()

    fun generateForFunction(
        request: Testgen.FunctionRequest
    ) {
        logger.info("in generateForFunction")
        grpcStub.generateFunctionTests(request).handleWithProgress()
    }

    fun generateForClass(
        request: Testgen.ClassRequest
    ) = grpcStub.generateClassTests(request).handleWithProgress()

    fun generateForFolder(
        request: Testgen.FolderRequest
    ) = grpcStub.generateFolderTests(request).handleWithProgress()

    fun generateForSnippet(
        request: Testgen.SnippetRequest
    ) = grpcStub.generateSnippetTests(request).handleWithProgress()

    fun generateForAssertion(
        request: Testgen.AssertionRequest
    ) = grpcStub.generateAssertionFailTests(request).handleWithProgress()

    suspend fun getFunctionReturnType(
        request: Testgen.FunctionRequest
    ): Testgen.FunctionTypeResponse = withContext(Dispatchers.IO) {
        logger.info("in getFunctionReturnType")
        grpcStub.getFunctionReturnType(request)
    }

    suspend fun handShake(): Testgen.DummyResponse {
        logger.info("in handShake()")
        return grpcStub.handshake(Testgen.DummyRequest.newBuilder().build())
    }

    private fun progressOrNull(response: Testgen.ProjectConfigResponse): Util.Progress? =
        if (response.hasProgress()) response.progress else null

    fun configureProject() {
        logger.info("In cofigureProject")
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
        grpcStub.configureProject(request).handleWithProgress(
            "Configuring project",
            ::progressOrNull,
            ::handleProjectConfigResponse
        )
        logger.info("Finished configureProject")
    }

    fun createBuildDir() {
        logger.info("In createBuildDir")
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
        grpcStub.configureProject(request).handleWithProgress(
            "Ask server to create build dir",
            ::progressOrNull,
            ::handleBuildDirCreation
        )
        logger.info("Finished createBuildDir()")
    }

    fun generateJSon() {
        logger.info("In generateJson()")
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
        grpcStub.configureProject(request).handleWithProgress(
            "Ask server to generate json",
            ::progressOrNull,
            ::handleJSONGeneration,
        )
        logger.info("Finished generateJSon()")
    }

    private suspend fun heartBeatOnce() {
        try {
            logger.debug("in heartBeatOnce")
            grpcStub.heartbeat(Testgen.DummyRequest.newBuilder().build())
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
            handleResponse = this@Client::handleTestResponse,
            onCompleted = this@Client::handleTestResponse
        )
    }

    // T: TestResponse | ProjectConfigResponse
    fun <T> Flow<T>.handleWithProgress(
        progressName: String,
        progressAccessor: (T) -> Util.Progress?,
        onCompleted: (T) -> Unit = {},
        handleResponse: (T) -> Unit = {}
    ) {
        logger.info("In handleWithProgress")
        val uiProgress = UTBotRequestProgressIndicator(progressName)
        // start showing progress in status bar
        uiProgress.start()
        uiProgress.requestJob = grpcCoroutineScope.launch {
            this@handleWithProgress
                .catch { exception ->
                    logger.info("In catch of handleWithProgress")
                    logger.warn(exception.message)
                }
                .collect {
                    logger.info("In collect of handleWithProgress: $it")
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
            uiProgress.complete()
        }
    }

    companion object {
        const val RANDOM_SEQUENCE_MAX_VALUE = 10
        const val RANDOM_SEQUENCE_LENGTH = 5
        const val HEARTBEAT_INTERVAL: Long = 500L
    }

    override fun dispose() {
        grpcCoroutineScope.cancel()
    }
}
