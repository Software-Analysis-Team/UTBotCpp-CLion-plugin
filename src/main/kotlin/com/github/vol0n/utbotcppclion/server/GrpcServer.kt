package com.github.vol0n.utbotcppclion.server

import com.github.vol0n.utbotcppclion.client.GrpcStarter
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlin.reflect.KCallable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt
import testsgen.Util
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class Server(private val port: Int) {
    private val server: Server = ServerBuilder
        .forPort(port)
        .addService(TestGenService())
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@Server.stop()
                println("*** server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }


    private class TestGenService : TestsGenServiceGrpcKt.TestsGenServiceCoroutineImplBase() {

        fun log(function: KCallable<*>, message: String) {
            println("[${function.name}] $message")
        }

        fun logStarted(function: KCallable<*>) = log(function, "started")
        fun logFinished(function: KCallable<*>) = log(function, "finished")

        fun buildDummyTestsResponses(
            pathToGeneratedTestFile: String,
            generatedCode: String
        ): Flow<Testgen.TestsResponse> {
            return flow {
                emit(
                    Testgen.TestsResponse.newBuilder().addTestSources(
                        Util.SourceCode.newBuilder()
                            .setFilePath(pathToGeneratedTestFile)
                            .setCode(generatedCode)
                            .build()
                    ).build()
                )
            }
        }

        override fun generateFileTests(request: Testgen.FileRequest): Flow<Testgen.TestsResponse> {
            logStarted(::generateFileTests)
            val projectPath = request.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.projectRequest.projectContext.testDirPath,
                request.filePath
            ).toString()
            val generatedCode = "Hello " + File("${projectPath}/${request.filePath}").readText()
            logFinished(this::generateFileTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, generatedCode)
        }

        override fun generateLineTests(request: Testgen.LineRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateLineTests)
            val projectPath = request.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.projectRequest.projectContext.testDirPath,
                request.sourceInfo.filePath
            ).toString()
            val line: String
            Files.lines(Paths.get(projectPath, request.sourceInfo.filePath)).use {
                line = it.skip(request.sourceInfo.line.toLong()).findFirst().get()
            }
            val generatedCode = "The line with zero based index ${request.sourceInfo.line}:\n$line"
            logFinished(this::generateLineTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, generatedCode)
        }

        override suspend fun getFunctionReturnType(request: Testgen.FunctionRequest): Testgen.FunctionTypeResponse {
            logStarted(this::getFunctionReturnType)
            logFinished(this::getFunctionReturnType)
            return Testgen.FunctionTypeResponse.newBuilder().setValidationType(
                Util.ValidationType.values().random()
            ).build()
        }

        override fun generateFunctionTests(request: Testgen.FunctionRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateFunctionTests)
            val pathToGeneratedTestFile = Paths.get(
                request.lineRequest.projectRequest.projectContext.projectPath,
                request.lineRequest.projectRequest.projectContext.testDirPath,
                request.lineRequest.sourceInfo.filePath
            ).toString()
            val generatedCode =
                "This is dummy response to test that everything works. \n path to test file: $pathToGeneratedTestFile"
            logFinished(this::generateFunctionTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, generatedCode)
        }

        override fun generateClassTests(request: Testgen.ClassRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateClassTests)
            val response = generateLineTests(request.lineRequest)
            logFinished(this::generateClassTests)
            return response
        }

        override fun generateFolderTests(request: Testgen.FolderRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateFolderTests)
            val pathToGeneratedTestFile = Paths.get(
                request.projectRequest.projectContext.projectPath,
                request.projectRequest.projectContext.testDirPath,
                request.folderPath,
                "folder_tests_for_${Paths.get(request.folderPath).last()}.cpp",
            ).toString()
            logFinished(this::generateFolderTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, pathToGeneratedTestFile)
        }

        override fun generateSnippetTests(request: Testgen.SnippetRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateSnippetTests)
            val pathToGeneratedTestFile = Paths.get(
                request.projectContext.projectPath,
                request.projectContext.testDirPath,
                request.filePath,
            ).toString()

            logFinished(this::generateSnippetTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, "Path to file: $pathToGeneratedTestFile")
        }

        override fun generateAssertionFailTests(request: Testgen.AssertionRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generateAssertionFailTests)
            val response = generateLineTests(request.lineRequest)
            logFinished(this::generateAssertionFailTests)
            return response
        }

        override fun generatePredicateTests(request: Testgen.PredicateRequest): Flow<Testgen.TestsResponse> {
            logStarted(this::generatePredicateTests)
            val projectPath = request.lineRequest.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.lineRequest.projectRequest.projectContext.testDirPath,
                request.lineRequest.sourceInfo.filePath
            ).toString()
            val line: String
            Files.lines(Paths.get(projectPath, request.lineRequest.sourceInfo.filePath)).use {
                line = it.skip(request.lineRequest.sourceInfo.line.toLong()).findFirst().get()
            }
            val generatedCode = "The line with zero based index ${request.lineRequest.sourceInfo.line}:\n$line" +
                    "\nThe predicate info received: " +
                    "predicate: ${request.predicateInfo.predicate} " +
                    "return value: ${request.predicateInfo.returnValue} " +
                    "type: ${request.predicateInfo.type}"

            logFinished(this::generatePredicateTests)
            return buildDummyTestsResponses(pathToGeneratedTestFile, generatedCode)
        }
    }
}

fun main() {
    val port = GrpcStarter.port
    val server = Server(port)
    server.start()
    server.blockUntilShutdown()
}
