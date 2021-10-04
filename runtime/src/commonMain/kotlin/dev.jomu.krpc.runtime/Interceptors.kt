package dev.jomu.krpc.runtime

import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response

interface UnaryServerInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo<Req, Resp, Err>,
        request: Req,
        metadata: Metadata,
        next: suspend (Req, Metadata) -> Response<Resp, Err>
    ): Response<Resp, Err>
}

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