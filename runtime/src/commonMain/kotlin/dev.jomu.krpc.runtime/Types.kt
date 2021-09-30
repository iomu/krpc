package dev.jomu.krpc.runtime

import dev.jomu.krpc.Response
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class MethodInfo<Req, Resp, Err>(
    val name: String,
    internal val path: String,
    val requestSerializer: KSerializer<Req>,
    val responseSerializer: KSerializer<Response<Resp, Err>>,
)

// TODO: naming
interface Call {
    suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T

    val headers: Map<String, String>
}

interface JsonEncoder<R> {
    fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): R
}

class EncodableMessage<T>(
    val headers: Map<String, String>,
    private val value: T,
    private val serializer: SerializationStrategy<T>,
    private val json: Json
) {
    fun <R> encode(encoder: JsonEncoder<R>): R {
        return encoder.encode(json, serializer, value)
    }
}