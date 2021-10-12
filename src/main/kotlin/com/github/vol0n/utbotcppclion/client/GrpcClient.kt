package com.github.vol0n.utbotcppclion.client

import com.charleskorn.kaml.Yaml
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.hello.GreeterGrpcKt
import io.grpc.hello.HelloRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import testsgen.Testgen
import testsgen.TestsGenServiceGrpcKt
import java.io.Closeable
import java.util.concurrent.TimeUnit

class GrpcClient(private val channel: ManagedChannel) : Closeable {
    private val helloStub: GreeterGrpcKt.GreeterCoroutineStub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    private val testgenStub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub =
        TestsGenServiceGrpcKt
        .TestsGenServiceCoroutineStub(channel)

    suspend fun greet(name: String): String {
        val request = HelloRequest.newBuilder().setName(name).build()
        val response = helloStub.sayHello(request)
        return response.message
    }

    fun generateForFile(
        request: Testgen.FileRequest
    ): Flow<Testgen.TestsResponse> = testgenStub.generateFileTests(request)

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
