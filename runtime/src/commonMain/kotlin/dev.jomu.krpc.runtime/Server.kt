package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

typealias MethodHandler<Service, Req, Resp, Err> = suspend (service: Service, request: Req, metadata: Metadata) -> Response<Resp, Err>

class MethodDescriptor<Service, Req, Resp, Err>(
    val info: MethodInfo<Req, Resp, Err>,
    val handler: MethodHandler<Service, Req, Resp, Err>,
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

private class ImplementationWithMethod<T>(val implementation: T, val method: MethodDescriptor<T, *, *, *>)

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

    private suspend fun <Service, Req, Resp, Err> MethodDescriptor<Service, Req, Resp, Err>.handle(
        implementation: Service,
        call: Call
    ): EncodableMessage<Response<Resp, Err>> {
        val request = call.readRequest(json, info.requestSerializer)
        val metadata = Metadata.fromHttpHeaders(call.headers)

        val response = interceptor?.intercept(info, request, metadata) { request, metadata ->
            handler(implementation, request, metadata)
        } ?: handler(implementation, request, metadata)
        return EncodableMessage(response.metadata.toHttpHeaders(), response, info.responseSerializer, json)
    }

    fun createGenericError(code: ErrorCode, message: String): EncodableMessage<*> {
        return EncodableMessage(emptyMap(), Error(code, message), ResponseSerializer(Unit.serializer(), Unit.serializer()), json)
    }
}

private fun <T> RegisteredService<T>.toHandlerMap(): Map<String, ImplementationWithMethod<T>> {
    return descriptor.methods.associateBy {
        it.info.path
    }.mapValues { ImplementationWithMethod(implementation, it.value) }
}

class ServiceDescriptor<T>(internal val name: String, val methods: List<MethodDescriptor<T, *, *, *>>)

class MethodInfo<Req, Resp, Err>(
    val name: String,
    internal val path: String,
    val requestSerializer: KSerializer<Req>,
    val responseSerializer: KSerializer<Response<Resp, Err>>,
)

interface UnaryServerInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err>
}

class ChainUnaryServerInterceptor(private val interceptors: List<UnaryServerInterceptor>) : UnaryServerInterceptor {
    override suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err> {
        val chain = interceptors.foldRight(next) { interceptor, acc ->
            { request, metadata ->
                interceptor.intercept(info, request, metadata, acc)
            }
        }

        return chain(request, metadata)
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