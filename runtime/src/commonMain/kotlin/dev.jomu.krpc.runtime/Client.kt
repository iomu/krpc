package dev.jomu.krpc.runtime

import dev.jomu.krpc.ErrorCode
import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response.Error
import dev.jomu.krpc.Response
import dev.jomu.krpc.withMetadata
import kotlinx.serialization.json.Json


interface KrpcHttpClient {
    suspend fun post(url: String, message: EncodableMessage<*>): Call
}

open class BaseKrpcClient(
    private val client: KrpcHttpClient,
    private val baseUrl: String,
    private val interceptor: UnaryClientInterceptor?
) {
    private val json = Json { ignoreUnknownKeys = true }
    protected suspend fun <Req, Resp, Err> executeUnaryCall(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        requestMetadata: Metadata,
    ): Response<Resp, Err> {
        val url = "${baseUrl}/${info.path}"

        val execute: suspend (Req, Metadata) -> Response<Resp, Err> = { request, metadata ->
            val message = EncodableMessage(
                metadata.toHttpHeaders(), request,
                info.requestSerializer, json
            )
            val result = client.post(url, message)

            val metadata = metadataFromHttpHeaders(result.headers)

            result.readRequest(json, info.responseSerializer).withMetadata(metadata)
        }

        return try {
            interceptor?.intercept(info, request, requestMetadata, execute) ?: execute(request, requestMetadata)
        } catch (e: Throwable) {
            Error(ErrorCode.INTERNAL, e.message ?: "<internal error>")
        }
    }
}