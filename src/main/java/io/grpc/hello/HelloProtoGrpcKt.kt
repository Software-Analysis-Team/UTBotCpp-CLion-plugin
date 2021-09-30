package io.grpc.hello

import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.hello.GreeterGrpc.getServiceDescriptor
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Holder for Kotlin coroutine-based client and server APIs for io.grpc.hello.Greeter.
 */
object GreeterGrpcKt {
  @JvmStatic
  val serviceDescriptor: ServiceDescriptor
    get() = GreeterGrpc.getServiceDescriptor()

  val sayHelloMethod: MethodDescriptor<HelloRequest, HelloReply>
    @JvmStatic
    get() = GreeterGrpc.getSayHelloMethod()

  val sayHelloAgainMethod: MethodDescriptor<HelloRequest, HelloReply>
    @JvmStatic
    get() = GreeterGrpc.getSayHelloAgainMethod()

  /**
   * A stub for issuing RPCs to a(n) io.grpc.hello.Greeter service as suspending coroutines.
   */
  @StubFor(GreeterGrpc::class)
  class GreeterCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT
  ) : AbstractCoroutineStub<GreeterCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): GreeterCoroutineStub =
        GreeterCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @return The single response from the server.
     */
    suspend fun sayHello(request: HelloRequest): HelloReply = unaryRpc(
      channel,
      GreeterGrpc.getSayHelloMethod(),
      request,
      callOptions,
      Metadata()
    )
    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][Status].  If the RPC completes with another status, a corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @return The single response from the server.
     */
    suspend fun sayHelloAgain(request: HelloRequest): HelloReply = unaryRpc(
      channel,
      GreeterGrpc.getSayHelloAgainMethod(),
      request,
      callOptions,
      Metadata()
    )}

  /**
   * Skeletal implementation of the io.grpc.hello.Greeter service based on Kotlin coroutines.
   */
  abstract class GreeterCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for io.grpc.hello.Greeter.SayHello.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun sayHello(request: HelloRequest): HelloReply = throw
        StatusException(UNIMPLEMENTED.withDescription("Method io.grpc.hello.Greeter.SayHello is unimplemented"))

    /**
     * Returns the response to an RPC for io.grpc.hello.Greeter.SayHelloAgain.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [Status].  If this method fails with a [java.util.concurrent.CancellationException], the RPC
     * will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    open suspend fun sayHelloAgain(request: HelloRequest): HelloReply = throw
        StatusException(UNIMPLEMENTED.withDescription("Method io.grpc.hello.Greeter.SayHelloAgain is unimplemented"))

    final override fun bindService(): ServerServiceDefinition = builder(getServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GreeterGrpc.getSayHelloMethod(),
      implementation = ::sayHello
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = GreeterGrpc.getSayHelloAgainMethod(),
      implementation = ::sayHelloAgain
    )).build()
  }
}
