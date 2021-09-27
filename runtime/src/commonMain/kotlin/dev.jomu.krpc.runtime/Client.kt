package dev.jomu.krpc.runtime


interface KrpcClient {
    suspend fun executeUnaryCall(url: String, message: EncodableMessage<*>): Call
}

interface UnaryClientInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err>
}