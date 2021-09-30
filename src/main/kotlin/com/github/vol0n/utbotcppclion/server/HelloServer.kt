package com.github.vol0n.utbotcppclion.server

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.hello.GreeterGrpcKt
import io.grpc.hello.HelloReply
import io.grpc.hello.HelloRequest

class HelloWorldServer(private val port: Int) {
    private val server: Server = ServerBuilder
        .forPort(port)
        .addService(HelloWorldService())
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@HelloWorldServer.stop()
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

    private class HelloWorldService : GreeterGrpcKt.GreeterCoroutineImplBase() {
        override suspend fun sayHello(request: HelloRequest): HelloReply {
            println("Server received message: ${request.name}")
            return HelloReply
                .newBuilder()
                .setMessage("Hello ${request.name}")
                .build()
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = HelloWorldServer(port)
    server.start()
    server.blockUntilShutdown()
}
