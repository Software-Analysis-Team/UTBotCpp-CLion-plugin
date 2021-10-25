package com.github.vol0n.utbotcppclion.client

import com.charleskorn.kaml.Yaml
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt
import java.io.Closeable
import java.util.concurrent.TimeUnit

class GrpcClient(private val channel: ManagedChannel) : Closeable {
    private val stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub =
        TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub(channel)

    fun generateForFile(
        request: Testgen.FileRequest
    ): Flow<Testgen.TestsResponse> = stub.generateFileTests(request)

    fun generateForLine(
        request: Testgen.LineRequest
    ): Flow<Testgen.TestsResponse> = stub.generateLineTests(request)

    fun generateForPredicate(
        request: Testgen.PredicateRequest
    ): Flow<Testgen.TestsResponse> = stub.generatePredicateTests(request)


    suspend fun getFunctionReturnType(
        request: Testgen.FunctionRequest
    ): Testgen.FunctionTypeResponse = withContext(Dispatchers.IO) { stub.getFunctionReturnType(request) }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

@Serializable
data class GrpcConfig(
    @SerialName("grpc port")
    val grpcPort: Int,
    @SerialName("grpc server name")
    val grpcServerName: String
)

object GrpcStarter {
    val port: Int
    private val serverName: String
    private val defaultConfig = GrpcConfig(50051, "localhost")

    init {
        val configString = this::class.java.getResource("/config.yaml")?.readText()
        val config: GrpcConfig = if (configString != null) {
            Yaml.default.decodeFromString(configString)
        } else {
            defaultConfig
        }
        port = config.grpcPort
        serverName = config.grpcServerName
    }

    fun startClient(): GrpcClient {
        val channel = ManagedChannelBuilder.forAddress(serverName, port).usePlaintext().build()

        return GrpcClient(channel)
    }
}
