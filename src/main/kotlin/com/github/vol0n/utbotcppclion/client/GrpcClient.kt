package com.github.vol0n.utbotcppclion.client

import com.charleskorn.kaml.Yaml
import com.github.vol0n.utbotcppclion.server.Server
import com.intellij.openapi.diagnostic.Logger

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import testsgen.TestsGenServiceGrpcKt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

import java.io.Closeable
import java.util.concurrent.TimeUnit

class GrpcClient(private val channel: ManagedChannel) : Closeable {
    val stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub =
        TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub(channel)

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
    private val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(GrpcStarter::class.java)

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
        log.info("Connecting to server on host: $serverName, port: $port")
        val channel = ManagedChannelBuilder.forAddress(serverName, port).usePlaintext().build()

        return GrpcClient(channel)
    }
}
