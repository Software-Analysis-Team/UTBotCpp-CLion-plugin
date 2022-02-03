package com.github.vol0n.utbotcppclion.services

import com.github.vol0n.utbotcppclion.actions.getDummyRequest
import com.github.vol0n.utbotcppclion.actions.getProjectConfigRequestMessage
import com.github.vol0n.utbotcppclion.client.GrpcStarter
import com.github.vol0n.utbotcppclion.client.ResponseHandle
import com.github.vol0n.utbotcppclion.messaging.ConnectionStatus
import com.github.vol0n.utbotcppclion.messaging.UTBotEventsListener
import com.github.vol0n.utbotcppclion.ui.OutputType
import com.github.vol0n.utbotcppclion.ui.UTBotConsole
import com.intellij.openapi.Disposable

import testsgen.Testgen

import com.intellij.openapi.project.Project
import kotlin.random.Random

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import testsgen.TestsGenServiceGrpcKt

import ch.qos.logback.classic.Logger
import com.github.vol0n.utbotcppclion.client.ClientLogAppender
import com.github.vol0n.utbotcppclion.ui.OutputWindowProvider
import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.components.Service
import mu.KotlinLogging

enum class LogLevel(val id: String) {
    INFO("INFO"), FATAL("FATAL"), ERROR("ERROR"),
    DEBUG("DEBUG"), TRACE("TRACE"), WARN("WARN")
}

@Service
class Client(val project: Project) : Disposable {
    private var connectionStatus = ConnectionStatus.INIT
    private val connectionChangedPublisher =
        project.messageBus.syncPublisher(UTBotEventsListener.CONNECTION_CHANGED_TOPIC)
    private var heartBeatJob: Job? = null
    private val metadata: io.grpc.Metadata = io.grpc.Metadata()
    private val handler = ResponseHandle(project, this)
    private var logLevel: LogLevel = LogLevel.INFO
    private var newClient = true
    var isPluginStarted = false
        private set

    private val logger = KotlinLogging.logger("ClientLogger")

    init {
        (logger.underlyingLogger as Logger).getAppender("ClientAppender").let {
            (it as ClientLogAppender).utBotConsole = OutputWindowProvider.getOutput(project, OutputType.CLIENT_LOG)
        }
    }

    val grpcCoroutineScope: CoroutineScope

    init {
        val handler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
        }
        grpcCoroutineScope = CoroutineScope(Dispatchers.Swing + handler)

        periodicHeartBeat()
    }

    private val grpcStub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub

    fun setLoggingLevel(newLevel: LogLevel) {
        logger.info("Setting new log level: ${newLevel.id}")
        logLevel = newLevel
        grpcCoroutineScope.launch {
            provideLogChannel()
        }
    }

    init {
        logger.info("Connecting to server on host: ${GrpcStarter.serverName} , port: ${GrpcStarter.port}")
        val stub = GrpcStarter.startClient().stub
        val clientID = generateClientID()
        metadata.put(io.grpc.Metadata.Key.of("clientid", io.grpc.Metadata.ASCII_STRING_MARSHALLER), clientID)
        grpcStub = io.grpc.stub.MetadataUtils.attachHeaders(stub, metadata)
        project.messageBus.connect()
            .subscribe(UTBotEventsListener.CONNECTION_CHANGED_TOPIC, object : UTBotEventsListener {
                override fun onConnectionChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus) {
                    if (oldStatus != newStatus && newStatus == ConnectionStatus.CONNECTED) {
                        configureProject()
                    }
                }
                override fun onHeartbeatSuccess(response: Testgen.HeartbeatResponse) {
                    RunOnceUtil.runOnceForProject(project, "UTBot: Register client for server") {
                        registerClient(clientID)
                    }


                    if (newClient || !response.linked) {
                        grpcCoroutineScope.launch {
                            provideLogChannel()
                            provideGTestChannel()
                        }
                    }
                }
            })
    }

    private suspend fun provideGTestChannel() {
        val request = Testgen.LogChannelRequest.newBuilder().setLogLevel("MAX").build()
        try {
            grpcStub.closeGTestChannel(getDummyRequest())
        } catch (e: Exception) {
            logger.error("Exception when closing gtest channel")
            logger.error(e.message)
        }

        val gTestConsole: UTBotConsole = OutputWindowProvider.getOutput(project, OutputType.GTEST)
        grpcStub.openGTestChannel(request)
            .catch { exception ->
                logger.error("Exception when opening gtest channel")
                logger.error(exception.message)
            }
            .collect {
                gTestConsole.info(it.message)
            }
    }

    private suspend fun provideLogChannel() {
        val request = Testgen.LogChannelRequest.newBuilder().setLogLevel(logLevel.id).build()
        try {
            grpcStub.closeLogChannel(getDummyRequest())
        } catch (e: Exception) {
            logger.error("Exception when closing log channel")
            logger.error(e.message)
        }

        val serverConsole: UTBotConsole = OutputWindowProvider.getOutput(project, OutputType.SERVER_LOG)
        grpcStub.openLogChannel(request)
            .catch { exception ->
                logger.error("Exception when opening log channel")
                logger.error(exception.message)
            }
            .collect {
                serverConsole.info(it.message)
            }
    }

    private fun generateClientID(): String {
        fun createRandomSequence() = (1..RANDOM_SEQUENCE_LENGTH)
            .joinToString("") { Random.nextInt(0, RANDOM_SEQUENCE_MAX_VALUE).toString() }

        return project.name + (System.getenv("USER") ?: "unknownUser") + createRandomSequence()
    }

    private fun registerClient(clientID: String) {
        grpcCoroutineScope.launch {
            try {
                logger.info("sending REGISTER CLIENT request, clientID == $clientID")
                grpcStub.registerClient(Testgen.RegisterClientRequest.newBuilder().setClientId(clientID).build())
            } catch (e: Exception) {
                logger.error("Register client failed: ${e.message}")
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
        logger.info("The heartbeat started with interval: $HEARTBEAT_INTERVAL ms")
        if (heartBeatJob != null) {
            heartBeatJob?.cancel()
        }
        heartBeatJob = grpcCoroutineScope.launch {
            while (isActive) {
                heartBeatOnce()
                delay(HEARTBEAT_INTERVAL)
            }
            logger.info("stopped heartBeating the server!")
        }
    }

    fun generateForFile(
        request: Testgen.FileRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for FILE: \n$request")
            handler.handleTestsStream(grpcStub.generateFileTests(request), "Generate For File")
        }
    }

    fun generateForLine(
        request: Testgen.LineRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for LINE: \n$request")
            handler.handleTestsStream(grpcStub.generateLineTests(request), "Generate For Line")
        }
    }

    fun generateForPredicate(
        request: Testgen.PredicateRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for PREDICATE: \n$request")
            handler.handleTestsStream(grpcStub.generatePredicateTests(request), "Generate For Predicate")
        }
    }

    fun generateForFunction(
        request: Testgen.FunctionRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for FUNCTION: \n$request")
            handler.handleTestsStream(grpcStub.generateFunctionTests(request), "Generate For Function")
        }
    }

    fun generateForClass(
        request: Testgen.ClassRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for CLASS: \n$request")
            handler.handleTestsStream(grpcStub.generateClassTests(request), "Generate For Folder")
        }
    }

    fun generateForFolder(
        request: Testgen.FolderRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for FOLDER: \n$request")
            handler.handleTestsStream(grpcStub.generateFolderTests(request), "Generate For Folder")
        }
    }

    fun generateForSnippet(
        request: Testgen.SnippetRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for SNIPPET: \n$request")
            handler.handleTestsStream(grpcStub.generateSnippetTests(request), "Generate For Snippet")
        }
    }

    fun generateForAssertion(
        request: Testgen.AssertionRequest
    ) {
        grpcCoroutineScope.launch {
            logger.info("Sending request to generate for ASSERTION: \n$request")
            handler.handleTestsStream(grpcStub.generateAssertionFailTests(request), "Generate For Assertion")
        }
    }

    suspend fun getFunctionReturnType(
        request: Testgen.FunctionRequest
    ): Testgen.FunctionTypeResponse = withContext(Dispatchers.IO) {
        logger.info("Sending request to get FUNCTION RETURN TYPE: \n$request")
        grpcStub.getFunctionReturnType(request)
    }

    suspend fun handShake(): Testgen.DummyResponse {
        logger.info("Sending HANDSHAKE request")
        return grpcStub.handshake(Testgen.DummyRequest.newBuilder().build())
    }

    fun configureProject() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CHECK)
        grpcCoroutineScope.launch {
            logger.info("Sending request to CHECK PROJECT CONFIGURATION: \n$request")
            handler.handleCheckConfigurationResponse(
                grpcStub.configureProject(request),
                "Checking project configuration..."
            )
        }
    }

    fun createBuildDir() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.CREATE_BUILD_DIR)
        grpcCoroutineScope.launch {
            logger.info("Sending request to GENERATE BUILD DIR: \n$request")
            handler.handleCreateBuildDirResponse(grpcStub.configureProject(request), "Create build directory...")
        }
    }

    fun getCoverageAndResults(request: Testgen.CoverageAndResultsRequest) {
        grpcCoroutineScope.launch {
            withContext(Dispatchers.Default) {
                logger.info("Sending request to get COVERAGE AND RESULTS: \n$request")
                handler.handleCoverageAndResultsResponse(
                    grpcStub.createTestsCoverageAndResult(request),
                    "Run Tests with Coverage"
                )
            }
        }
    }

    fun generateJSon() {
        val request = getProjectConfigRequestMessage(project, Testgen.ConfigMode.GENERATE_JSON_FILES)
        grpcCoroutineScope.launch {
            logger.info("Sending request to GENERATE JSON FILES: \n$request")
            handler.handleGenerateJsonResponse(grpcStub.configureProject(request), "Generate JSON files...")
        }
    }

    private suspend fun heartBeatOnce() {
        try {
            val response = grpcStub.heartbeat(Testgen.DummyRequest.newBuilder().build())
            connectionChangedPublisher.onConnectionChange(connectionStatus, ConnectionStatus.CONNECTED)
            connectionChangedPublisher.onHeartbeatSuccess(response)
            connectionStatus = ConnectionStatus.CONNECTED
        } catch (e: Exception) {
            logger.error("Heartbeat failed with exception: \n${e.message}")
            connectionChangedPublisher.onConnectionChange(connectionStatus, ConnectionStatus.BROKEN)
            connectionStatus = ConnectionStatus.BROKEN
        }
    }

    companion object {
        const val RANDOM_SEQUENCE_MAX_VALUE = 10
        const val RANDOM_SEQUENCE_LENGTH = 5
        const val HEARTBEAT_INTERVAL: Long = 500L
    }

    override fun dispose() {
        grpcCoroutineScope.launch {
            grpcStub.closeLogChannel(getDummyRequest())
            grpcStub.closeGTestChannel(getDummyRequest())
            cancel()
        }
    }
}
