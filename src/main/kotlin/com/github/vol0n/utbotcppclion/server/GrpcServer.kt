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
import java.nio.file.Paths

class Server(private val port: Int) {
    private val server: Server = ServerBuilder
        .forPort(port)
        .addService(GenerateForFileService())
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

    private class GenerateForFileService : TestsGenServiceGrpcKt.TestsGenServiceCoroutineImplBase() {
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
    }
}

fun main() {
    val port = GrpcStarter.port
    val server = Server(port)
    server.start()
    server.blockUntilShutdown()
}
