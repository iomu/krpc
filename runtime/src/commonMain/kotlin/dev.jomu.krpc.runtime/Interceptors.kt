package dev.jomu.krpc.runtime

import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response

/**
 * Intercepts unary requests to the [KrpcServer]
 */
interface UnaryServerInterceptor {
    /**
     * Intercepts unary requests to the [KrpcServer]
     *
     * @param info
     * Metadata about the called method
     *
     * @param request
     * The decoded request
     *
     * @param next
     * The next interceptor or method handler in the interceptor chain
     */
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err>
}

/**
 * Intercepts unary requests from a kRPC client [BaseKrpcClient]
 */
typealias UnaryClientInterceptor = UnaryServerInterceptor

internal class ChainUnaryServerInterceptor(private val interceptors: List<UnaryServerInterceptor>) : UnaryServerInterceptor {
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