package dev.jomu.krpc.runtime

import dev.jomu.krpc.ErrorCode
import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response.Error
import dev.jomu.krpc.Response
import dev.jomu.krpc.withMetadata
import kotlinx.serialization.json.Json

/**
 * A HTTP client that is used by [kRPC clients][BaseKrpcClient] to send messages to a kRPC service
 */
interface KrpcHttpClient {
    /**
     * Sends a POST HTTP request consisting of the encoded [OutgoingMessage] to the given URL
     *
     * @param url
     * URL of the HTTP request
     *
     * @param message
     * The encoded message should be sent as body of the HTTP request
     *
     * @return
     * The response from the server
     */
    suspend fun post(url: String, message: OutgoingMessage<*>): IncomingMessage
}

/**
 * Base class for all generated kRPC clients
 *
 * @param client
 * HTTP client to use
 *
 * @param baseUrl
 * Base URL of the kRPC server
 *
 * @param interceptor
 * [UnaryClientInterceptor] to use for all requests
 */
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
            val message = OutgoingMessage(
                metadata.toHttpHeaders(), request,
                info.requestSerializer, json
            )
            val result = client.post(url, message)

            val metadata = metadataFromHttpHeaders(result.headers)

            result.read(json, info.responseSerializer).withMetadata(metadata)
        }

        return try {
            interceptor?.intercept(info, request, requestMetadata, execute) ?: execute(request, requestMetadata)
        } catch (e: Throwable) {
            Error(ErrorCode.INTERNAL, e.message ?: "<internal error>")
        }
    }
}