package dev.jomu.krpc.runtime

import dev.jomu.krpc.Response
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

/**
 * Encodes metadata of a defined kRPC method
 *
 * @property name
 * The name of the method, if not overwritten the name of the function in the interface
 *
 * @property requestSerializer
 * A serializer for the incoming message of this method, can be used for deserialization or inspecting the request
 *
 * @property responseSerializer
 * A serializer for the response message of this method, can be used for serialization or inspecting the response
 */
class MethodInfo<Req, Resp, Err>(
    val name: String,
    internal val path: String,
    val requestSerializer: KSerializer<Req>,
    val responseSerializer: KSerializer<Response<Resp, Err>>,
)

/**
 * A message that can be [read] from the network.
 */
interface IncomingMessage {
    /**
     * Deserializes the received message to a [T]
     *
     * @param json
     * The [Json] instance to use for deserialization. Implementations of this interface MUST use this instance
     * to produce a result, otherwise the behaviour is undefined.
     *
     * @param deserializer
     * The [DeserializationStrategy] to use for decoding
     *
     * @return
     * The decoded message
     */
    suspend fun <T> read(json: Json, deserializer: DeserializationStrategy<T>): T

    /**
     * The HTTP headers that were received from the network
     */
    val headers: Map<String, String>
}

/**
 * A [JsonEncoder] is responsible for encoding a value using a preconfigured [Json] instance.
 *
 * Example:
 * ```kotlin
 * object StringEncoder : JsonEncoder<String> {
 *   override fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): String =
 *     json.encodeToString(serializer, value)
 * }
 * ```
 *
 * @param R
 * The return type of the encoding process. If this [JsonEncoder] directly writes to some sink during the
 * encoding the parameter may be [Unit]
 */
interface JsonEncoder<R> {
    /**
     * Encodes [the value][value] to [U]
     *
     * @param json
     * The preconfigured [Json] instance to use for encoding. Implementations of this interface MUST use this instance
     * to produce a result, otherwise the behaviour is undefined.
     *
     * @param serializer
     * The [SerializationStrategy] to be used for the value
     *
     * @param value
     * The value to be encoded
     *
     * @return
     * The encoded value
     */
    fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): R
}

/**
 * A message that can be sent via the network by [writing][write] to a [JsonEncoder].
 *
 * @property headers
 * The HTTP headers that should be transmitted via the network
 */
class OutgoingMessage<T>(
    val headers: Map<String, String>,
    private val value: T,
    private val serializer: SerializationStrategy<T>,
    private val json: Json
) {
    /**
     * Encodes this [OutgoingMessage] via the given [JsonEncoder]. Choose [Unit] as return type when the [JsonEncoder]
     * writes the encoded message as a side effect to some sink.
     *
     * @return
     * The serialized value of the encoding process.
     */
    fun <R> write(encoder: JsonEncoder<R>): R {
        return encoder.encode(json, serializer, value)
    }
}