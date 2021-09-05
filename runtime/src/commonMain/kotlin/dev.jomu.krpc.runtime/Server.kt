package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

data class KrpcMessage<T>(val value: T, val metadata: Metadata = emptyMetadata())

typealias MethodHandler<Service, Req, Resp> = suspend (service: Service, request: KrpcMessage<Req>, interceptor: UnaryServerInterceptor?) -> KrpcMessage<Resp>

interface MethodDescriptor<Service, Req, Resp> {
    val name: String
    val handler: MethodHandler<Service, Req, Resp>
    fun readRequest(encoded: String): Req
    fun encodeResponse(message: Resp): String
}

data class MethodDescriptorImpl<Service, Req, Resp>(
    override val name: String,
    override val handler: MethodHandler<Service, Req, Resp>,
    val requestDeserializer: DeserializationStrategy<Req>,
    val responseSerializer: SerializationStrategy<Resp>
) : MethodDescriptor<Service, Req, Resp> {
    private val json = Json { ignoreUnknownKeys = true }
    override fun readRequest(encoded: String): Req {
        return json.decodeFromString(requestDeserializer, encoded)
    }

    override fun encodeResponse(message: Resp): String {
        return json.encodeToString(responseSerializer, message)
    }
}

data class ServiceDescriptor<T>(internal val name: String, val methods: List<MethodDescriptor<T, *, *>>)

fun <T> ServiceDescriptor<T>.path(method: MethodDescriptor<T, *, *>) = "$name/${method.name}"

interface MethodRegistrar {
    fun <T, Req, Resp> register(method: MethodDescriptor<T, Req, Resp>, service: T, path: String, interceptor: UnaryServerInterceptor?)
}

fun <T> MethodRegistrar.registerService(service: T, descriptor: ServiceDescriptor<T>, interceptor: UnaryServerInterceptor?) {
    descriptor.methods.forEach { register(it, service, descriptor.path(it), interceptor) }
}

class MethodInfo(val name: String)

typealias KrpcRequest<T> = KrpcMessage<T>

interface UnaryServerInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err>
}