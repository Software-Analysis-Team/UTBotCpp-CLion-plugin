package com.github.vol0n.utbotcppclion.server

import com.github.vol0n.utbotcppclion.client.GrpcStarter
import io.grpc.Server
import io.grpc.ServerBuilder
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
        override fun generateFileTests(request: Testgen.FileRequest): Flow<Testgen.TestsResponse> {
            val projectPath = request.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.projectRequest.projectContext.testDirPath,
                request.filePath).toString()
            val generatedCode = "Hello " + File("${projectPath}/${request.filePath}").readText()
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

        override fun generateLineTests(request: Testgen.LineRequest): Flow<Testgen.TestsResponse> {
            val projectPath = request.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.projectRequest.projectContext.testDirPath,
                request.sourceInfo.filePath)
            val line: String
            Files.lines(Paths.get(projectPath, request.sourceInfo.filePath)).use {
                line = it.skip(request.sourceInfo.line.toLong()).findFirst().get()
            }
            val generatedCode = "The line with zero based index ${request.sourceInfo.line}:\n$line"
            return flow {
                emit(
                    Testgen.TestsResponse.newBuilder().addTestSources(
                        Util.SourceCode.newBuilder()
                            .setFilePath(pathToGeneratedTestFile.toString())
                            .setCode(generatedCode)
                            .build()
                    ).build()
                )
            }
        }

        override suspend fun getFunctionReturnType(request: Testgen.FunctionRequest): Testgen.FunctionTypeResponse {
            println("Before taking random type")
            val t = Testgen.FunctionTypeResponse.newBuilder().setValidationType(
                Util.ValidationType.values().random()
            ).build()
            println("After taking random type")
            return t
        }

        override fun generatePredicateTests(request: Testgen.PredicateRequest): Flow<Testgen.TestsResponse> {
            val projectPath = request.lineRequest.projectRequest.projectContext.projectPath
            val pathToGeneratedTestFile = Paths.get(
                projectPath,
                request.lineRequest.projectRequest.projectContext.testDirPath,
                request.lineRequest.sourceInfo.filePath)
            val line: String
            Files.lines(Paths.get(projectPath, request.lineRequest.sourceInfo.filePath)).use {
                line = it.skip(request.lineRequest.sourceInfo.line.toLong()).findFirst().get()
            }
            val generatedCode = "The line with zero based index ${request.lineRequest.sourceInfo.line}:\n$line" +
                    "\nThe predicate info received: " +
                    "predicate: ${request.predicateInfo.predicate} " +
                    "return value: ${request.predicateInfo.returnValue} " +
                    "type: ${request.predicateInfo.type}"

            return flow {
                emit(
                    Testgen.TestsResponse.newBuilder().addTestSources(
                        Util.SourceCode.newBuilder()
                            .setFilePath(pathToGeneratedTestFile.toString())
                            .setCode(generatedCode)
                            .build()
                    ).build()
                )
            }
        }
    }
}

fun main() {
    val port = GrpcStarter.port
    val server = Server(port)
    server.start()
    server.blockUntilShutdown()
}
