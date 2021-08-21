package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

interface KrpcClient {
    suspend fun executeUnaryCall(url: String, message: KrpcMessage<String>): KrpcMessage<String>
}

interface Serializer {
    fun <T> encode(serializer: SerializationStrategy<T>, value: T): String
    fun <T> decode(deserializer: DeserializationStrategy<T>, encoded: String): T
}

interface UnaryClientInterceptor {
    suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err>
}