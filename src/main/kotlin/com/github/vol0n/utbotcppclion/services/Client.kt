package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.getProjectConfigRequestMessage
import com.github.vol0n.utbotcppclion.actions.utils.notifyError
import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.github.vol0n.utbotcppclion.client.ResponseHandle
import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotConnectionChangedNotifier
import com.github.vol0n.utbotcppclion.ui.UTBotRequestProgressIndicator
import com.github.vol0n.utbotcppclion.utils.createFileAndMakeDirs
import com.github.vol0n.utbotcppclion.utils.refreshAndFindIOFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service

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
import testsgen.TestsGenServiceGrpcKt

class Client(val project: Project) : Disposable {
    private var connectionStatus = ConnectionStatus.INIT
    private val connectionChangedPublisher =
        project.messageBus.syncPublisher(UTBotConnectionChangedNotifier.CONNECTION_CHANGED_TOPIC)
    private var heartBeatJob: Job? = null
    private val logger = Logger.getInstance(this::class.java)
    private val metadata: io.grpc.Metadata = io.grpc.Metadata()
    private val projectSettings = project.service<ProjectSettings>()
    private val handler = ResponseHandle(project, this)

    val grpcCoroutineScope: CoroutineScope

    init {
        val handler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
        }
        grpcCoroutineScope = CoroutineScope(Dispatchers.Swing + handler)

        periodicHeartBeat()
    }

    private val grpcStub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub

    init {
        val stub = GrpcStarter.startClient().stub
        val clientID = generateClientID()
        if (projectSettings.isFirstTimeLaunch) {
            registerClient(clientID)
        }
        metadata.put(io.grpc.Metadata.Key.of("clientid", io.grpc.Metadata.ASCII_STRING_MARSHALLER), clientID)
        grpcStub = io.grpc.stub.MetadataUtils.attachHeaders(stub, metadata)
    }

    private fun generateClientID(): String {
        fun createRandomSequence() = (1..RANDOM_SEQUENCE_LENGTH)
            .map { Random.nextInt(0, RANDOM_SEQUENCE_MAX_VALUE).toString() }
            .joinToString("")

        return project.name + (System.getenv("USER") ?: "unknownUser") + createRandomSequence()
    }

    private fun registerClient(clientID: String) {
        logger.info("in registerClient, clientID == $clientID")
        grpcCoroutineScope.launch {
            try {
                grpcStub.registerClient(Testgen.RegisterClientRequest.newBuilder().setClientId(clientID).build())
            } catch (e: Exception) {
                logger.warn("Register client failed: ${e.message}")
            }
        }
    }

    fun getConnectionStatus() = connectionStatus
    fun isServerAvailable() = connectionStatus == ConnectionStatus.CONNECTED

    fun doHandShake() {
        logger.info("in doHandShake")
        grpcCoroutineScope.launch {
            try {
                grpcStub.handshake(Testgen.DummyRequest.newBuilder().build())
            } catch (e: Exception) {
                logger.warn("HandShake failed with the following error: ${e.message}")
            }
        }
    }

    fun periodicHeartBeat() {
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        }
        logger.info("Started heartbeating the server!")
        heartBeatJob = grpcCoroutineScope.launch {
            while (isActive) {
                heartBeatOnce()
                delay(HEARTBEAT_INTERVAL)
            }
            logger.info("stopped heartBeating the server!")
        }
    }

    private fun handleTestResponse(response: Testgen.TestsResponse) {
        logger.info("in handleTestResponse: \n$response")
        val sourceListsToProcess = listOf(response.testSourcesList, response.stubs.stubSourcesList)
        sourceListsToProcess.forEach { sourceList ->
            sourceList.forEach { sourceCode ->
                logger.info("Creating source file: ${sourceCode.filePath}")
                logger.info("Contents: \n ${sourceCode.code}")
                if (sourceCode.code.isNotEmpty()) {
                    createFileAndMakeDirs(
                        projectSettings.convertFromRemotePathIfNeeded(sourceCode.filePath),
                        sourceCode.code
                    )
                }
            }
        }
    }

    fun generateForFile(
        request: Testgen.FileRequest
    ) {
        logger.info("in generateForFile")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateFileTests(request), "Generate For File")
        }
    }

    fun generateForLine(
        request: Testgen.LineRequest
    ) {
        logger.info("in generateForLine")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateLineTests(request), "Generate For Line")
        }
    }

    fun generateForPredicate(
        request: Testgen.PredicateRequest
    ) {
        logger.info("in generateForPredicate")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generatePredicateTests(request), "Generate For Predicate")
        }
    }

    fun generateForFunction(
        request: Testgen.FunctionRequest
    ) {
        logger.info("in generateForFunction")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateFunctionTests(request), "Generate For Function")
        }
    }

    fun generateForClass(
        request: Testgen.ClassRequest
    ) {
        logger.info("in generateForClass")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateClassTests(request), "Generate For Folder")
        }
    }

    fun generateForFolder(
        request: Testgen.FolderRequest
    ) {
        logger.info("in generateForFolder")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateFolderTests(request), "Generate For Folder")
        }
    }

    fun generateForSnippet(
        request: Testgen.SnippetRequest
    ) {
        logger.info("in generateForSnippet")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateSnippetTests(request), "Generate For Snippet")
        }
    }

    fun generateForAssertion(
        request: Testgen.AssertionRequest
    ) {
        logger.info("in generateForAssertion")
        grpcCoroutineScope.launch {
            handler.handleTestsStream(grpcStub.generateAssertionFailTests(request), "Generate For Assertion")
        }
    }

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
        logger.info("In configureProject")
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CHECK)
        grpcCoroutineScope.launch {
            handler.handleCheckConfigurationResponse(
                grpcStub.configureProject(request),
                "Checking project configuration...")
        }
        logger.info("Finished configureProject")
    }

    fun createBuildDir() {
        logger.info("In createBuildDir")

        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CREATE_BUILD_DIR)
        grpcCoroutineScope.launch {
            handler.handleCreateBuildDirResponse(grpcStub.configureProject(request), "Create build directory...")
        }

        logger.info("Finished createBuildDir()")
    }

    fun generateJSon() {
        logger.info("In generateJson()")
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.GENERATE_JSON_FILES)
        grpcCoroutineScope.launch {
            handler.handleGenerateJsonResponse(grpcStub.configureProject(request), "Generate JSON files...")
        }
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
            onCompleted = this@Client::handleTestResponse,
            onEndOfStream = { refreshAndFindIOFile(projectSettings.testDirPath) }
        )
    }

    // T: TestResponse | ProjectConfigResponse
    fun <T> Flow<T>.handleWithProgress(
        progressName: String,
        progressAccessor: (T) -> Util.Progress?,
        onCompleted: (T) -> Unit = {},
        handleResponse: (T) -> Unit = {},
        onEndOfStream: () -> Unit = {}
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
                    exception.message?.let { notifyError(it, project) }
                }
                .collect {
                    logger.info("In collect of handleWithProgress: $it")
                    handleResponse(it)
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

            onEndOfStream()
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
