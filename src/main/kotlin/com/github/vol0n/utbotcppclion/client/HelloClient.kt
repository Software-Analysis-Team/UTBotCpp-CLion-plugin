package com.github.vol0n.utbotcppclion.client

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.hello.GreeterGrpcKt
import io.grpc.hello.HelloRequest
import java.io.Closeable
import java.util.concurrent.TimeUnit

class HelloWorldClient(private val channel: ManagedChannel) : Closeable {
    private val stub: GreeterGrpcKt.GreeterCoroutineStub = GreeterGrpcKt.GreeterCoroutineStub(channel)

    suspend fun greet(name: String): String {
        val request = HelloRequest.newBuilder().setName(name).build()
        val response = stub.sayHello(request)
        return response.message
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

suspend fun send(text: String): String {

    val port = 50051

    val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()

    val client = HelloWorldClient(channel)

    return client.greet(text)
}