package com.github.vol0n.utbotcppclion.client

import ch.qos.logback.classic.Logger

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import testsgen.TestsGenServiceGrpcKt

import mu.KotlinLogging

import java.io.Closeable
import java.util.concurrent.TimeUnit

class GrpcClient(private val channel: ManagedChannel) : Closeable {
    val stub: TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub =
        TestsGenServiceGrpcKt.TestsGenServiceCoroutineStub(channel)

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

//@Serializable
data class GrpcConfig(
 //   @SerialName("grpc port")
    val grpcPort: Int,
  //  @SerialName("grpc server name")
    val grpcServerName: String
)

private val log = KotlinLogging.logger(Logger.ROOT_LOGGER_NAME)
object GrpcStarter {
    val port: Int
    val serverName: String
    private val defaultConfig = GrpcConfig(2121, "localhost")

    init {
    //    val configString = this::class.java.getResource("/config.yaml")?.readText()
    //    val config: GrpcConfig = if (configString != null) {
//            Yaml.default.decodeFromString(configString)
//        } else {
//            defaultConfig
//        }
        val config = defaultConfig
        port = config.grpcPort
        serverName = config.grpcServerName
    }

    fun startClient(): GrpcClient {
        log.info("Connecting to server on host: $serverName, port: $port")
        val channel = ManagedChannelBuilder.forAddress(serverName, port).usePlaintext().build()

        return GrpcClient(channel)
    }
}
