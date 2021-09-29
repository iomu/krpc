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

// TODO: naming
interface Call {
    suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T

    val headers: Map<String, String>
}

interface KrpcServer {
    suspend fun handleRequest(path: String, call: Call): EncodableMessage<*>
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

@OptIn(ExperimentalStdlibApi::class)
private class RealKrpcServer(
    services: List<RegisteredService<*>>,
    private val json: Json,
    interceptors: List<UnaryServerInterceptor> = emptyList(),
) : KrpcServer {
    private val interceptor = if (interceptors.isEmpty()) null else ChainUnaryServerInterceptor(interceptors)
    private val handlers = services.map { rs -> rs.toHandlerMap() }.fold(emptyMap<String, ImplementationWithMethod<*>>()) { a, b -> a + b }

    override suspend fun handleRequest(path: String, call: Call): EncodableMessage<*> {
        val path = path.trimStart('/')
        if (path.isEmpty()) {
            return createGenericError(ErrorCode.INVALID_ARGUMENT, "Path may not be empty")
        }
        val handler =
            handlers[path] ?: return createGenericError(ErrorCode.UNIMPLEMENTED, "$path not implemented")
        return try {
            handler.handle(call)
        } catch (e: Exception) {
            createGenericError(ErrorCode.INTERNAL, e.message ?: "<internal error>")
        }
    }

    private suspend fun <T> ImplementationWithMethod<T>.handle(call: Call): EncodableMessage<*> {
        return method.handle(implementation, call)
    }

    private suspend fun <Service, Req, Resp> MethodDescriptor<Service, Req, Resp>.handle(
        implementation: Service,
        call: Call
    ): EncodableMessage<Resp> {
        val request = call.readRequest(json, requestDeserializer)
        val response = handler(implementation, KrpcMessage(request, Metadata.fromHttpHeaders(call.headers)), interceptor)
        return EncodableMessage(response.metadata.toHttpHeaders(), response.value, responseSerializer, json)
    }

    fun createGenericError(code: ErrorCode, message: String): EncodableMessage<*> {
        return EncodableMessage(emptyMap(), GenericErrorResponse(ResponseError(code, message)), GenericErrorResponse.serializer(), json)
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

interface JsonEncoder<R> {
    fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): R
}

class EncodableMessage<T>(val headers: Map<String, String>, private val value: T, private val serializer: SerializationStrategy<T>, private val json: Json) {
    fun <R> encode(encoder: JsonEncoder<R>): R {
        return encoder.encode(json, serializer, value)
    }
}