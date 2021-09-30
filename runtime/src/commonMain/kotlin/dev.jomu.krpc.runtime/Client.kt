package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json


interface KrpcClient {
    suspend fun executeUnaryCall(url: String, message: EncodableMessage<*>): Call
}

interface UnaryClientInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err>
}

open class BaseKrpcClient(
    private val client: KrpcClient,
    private val baseUrl: String,
    private val interceptor: UnaryClientInterceptor?
) {
    private val json = Json { ignoreUnknownKeys = true }
    protected suspend fun <Req, Resp, Err> executeUnaryCall(
        path: String,
        info: MethodInfo,
        request: Req,
        requestMetadata: Metadata,
        requestSerializer: SerializationStrategy<Req>,
        responseDeserializer: DeserializationStrategy<Response<Resp, Err>>
    ): Response<Resp, Err> {
        val url = "${baseUrl}/$path"

        val execute: suspend (Req, Metadata) -> Response<Resp, Err> = { request, metadata ->
            val message = EncodableMessage(
                metadata.toHttpHeaders(), request,
                requestSerializer, json
            )
            val result = client.executeUnaryCall(url, message)

            val metadata = Metadata.fromHttpHeaders(result.headers)

            result.readRequest(json, responseDeserializer).withMetadata(metadata)
        }

        return try {
            interceptor?.intercept(info, request, requestMetadata, execute) ?: execute(request, requestMetadata)
        } catch (e: Throwable) {
            Error(ErrorCode.INTERNAL, e.message ?: "<internal error>")
        }
    }
}