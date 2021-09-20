package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

data class KrpcMessage<T>(val value: T, val metadata: Metadata = emptyMetadata())

typealias MethodHandler<Service, Req, Resp> = suspend (service: Service, request: KrpcMessage<Req>, interceptor: UnaryServerInterceptor?) -> KrpcMessage<Resp>

class MethodDescriptor<Service, Req, Resp>(
    val name: String,
    val handler: MethodHandler<Service, Req, Resp>,
    val requestDeserializer: DeserializationStrategy<Req>,
    val responseSerializer: SerializationStrategy<Resp>,
)

interface Call {
    suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T
    suspend fun <T> respond(json: Json, serializer: SerializationStrategy<T>, response: T, metadata: Metadata = emptyMetadata())
    val metadata: Metadata
}

interface KrpcServer {
    suspend fun handleRequest(path: String, call: Call)
}

internal class RegisteredService<T>(val descriptor: ServiceDescriptor<T>, val implementation: T)

private class ImplementationWithMethod<T>(val implementation: T, val method: MethodDescriptor<T, *, *>)

class KrpcServerBuilder {
    internal val interceptors: MutableList<UnaryServerInterceptor> = mutableListOf()
    internal val services: MutableList<RegisteredService<*>> = mutableListOf()

    fun addInterceptor(interceptor: UnaryServerInterceptor) {
        interceptors.add(interceptor)
    }

    fun <T> addService(descriptor: ServiceDescriptor<T>, implementation: T) {
        services.add(RegisteredService(descriptor, implementation))
    }
}

fun buildKrpcServer(block: KrpcServerBuilder.() -> Unit): KrpcServer {
    val builder = KrpcServerBuilder()
    builder.block()
    return RealKrpcServer(
        builder.services,
        Json { ignoreUnknownKeys = true },
        builder.interceptors
    )
}

// mirrors the shape of the generated response messages, can be used to send generic errors to the client
@Serializable
private class GenericErrorResponse(val error: ResponseError<Unit>)

private class RealKrpcServer(
    services: List<RegisteredService<*>>,
    private val json: Json,
    interceptors: List<UnaryServerInterceptor> = emptyList(),
) : KrpcServer {
    private val interceptor = if (interceptors.isEmpty()) null else ChainUnaryServerInterceptor(interceptors)
    private val handlers = services.map { rs -> rs.toHandlerMap() }.reduce { a, b -> a + b }

    override suspend fun handleRequest(path: String, call: Call) {
        val path = path.trimStart('/')
        if (path.isEmpty()) {
            return call.respondWithGenericError(ErrorCode.INVALID_ARGUMENT, "Path may not be empty")
        }
        val handler = handlers[path] ?: return call.respondWithGenericError(ErrorCode.UNIMPLEMENTED, "$path not implemented")
        try {
            handler.handle(call)
        } catch (e: Exception) {
            return call.respondWithGenericError(ErrorCode.INTERNAL, e.message ?: "<internal error>")
        }
    }

    private suspend fun <T> ImplementationWithMethod<T>.handle(call: Call) {
        method.handle(implementation, call)
    }

    private suspend fun <Service, Req, Resp> MethodDescriptor<Service, Req, Resp>.handle(implementation: Service, call: Call) {
        val request = call.readRequest(json, requestDeserializer)
        val response = handler(implementation, KrpcMessage(request, call.metadata), interceptor)
        call.respond(json, responseSerializer, response.value, response.metadata)
    }

    suspend fun Call.respondWithGenericError(code: ErrorCode, message: String) {
        respond(json, GenericErrorResponse.serializer(), GenericErrorResponse(ResponseError(code, message)))
    }
}

private fun <T> RegisteredService<T>.toHandlerMap(): Map<String, ImplementationWithMethod<T>> {
    return descriptor.methods.associateBy {
        descriptor.path(it)
    }.mapValues { ImplementationWithMethod(implementation, it.value) }
}

data class ServiceDescriptor<T>(internal val name: String, val methods: List<MethodDescriptor<T, *, *>>)

fun <T> ServiceDescriptor<T>.path(method: MethodDescriptor<T, *, *>) = "$name/${method.name}"

class MethodInfo(val name: String)

typealias KrpcRequest<T> = KrpcMessage<T>

interface UnaryServerInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err>
}

class ChainUnaryServerInterceptor(private val interceptors: List<UnaryServerInterceptor>) : UnaryServerInterceptor {
    override suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err> {
        val chain = interceptors.foldRight(next) { interceptor, acc ->
            { request ->
                interceptor.intercept(info, request, acc)
            }
        }

        return chain(request)
    }
}