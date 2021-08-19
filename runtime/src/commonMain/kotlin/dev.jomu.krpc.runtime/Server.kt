package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer

interface MessageStream {
    suspend fun <T> read(deserializer: DeserializationStrategy<T>): T
}

data class KrpcMessage<T>(val value: T, val serializer: KSerializer<T>, val metadata: Metadata = mapOf())

typealias MethodHandler<Service> = suspend (service: Service, readRequest: MessageStream, interceptor: UnaryServerInterceptor?) -> KrpcMessage<*>

data class MethodDescriptor<Service>(internal val name: String, val handler: MethodHandler<Service>)

data class ServiceDescriptor<T>(internal val name: String, val methods: List<MethodDescriptor<T>>)

fun <T> ServiceDescriptor<T>.path(method: MethodDescriptor<T>) = "$name/${method.name}"

interface MethodRegistrar {
    fun <T> register(service: T, descriptor: ServiceDescriptor<T>, interceptor: UnaryServerInterceptor?)
}

class MethodInfo(val name: String)

data class KrpcRequest<T>(val value: T, val metadata: Metadata)

interface UnaryServerInterceptor {
    suspend fun <Req, Resp, Err> intercept(info: MethodInfo, request: KrpcRequest<Req>, next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>): Response<Resp, Err>
}