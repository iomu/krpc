package dev.jomu.krpc.runtime

interface KrpcClient {
    suspend fun executeUnaryCall(url: String, message: KrpcMessage<*>): MessageStream
}