package dev.jomu.krpc.runtime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

sealed class Response<T, E> {
    abstract val metadata: Metadata
}

data class Success<T, E>(val result: T, override val metadata: Metadata = emptyMetadata()) : Response<T, E>()

enum class ErrorCode {
    INVALID_ARGUMENT,
    NOT_FOUND,
    INTERNAL,
    UNAUTHENTICATED,
    PERMISSION_DENIED,
    UNIMPLEMENTED
}

data class Metadata(val values: Map<String, String>) {
    companion object {
        fun fromHttpHeaders(headers: Map<String, String>): Metadata =
            Metadata(headers.filterKeys { it.startsWith("krpc-") }
                .mapKeys { (value, _) -> value.removePrefix("krpc-") })
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun Metadata.toHttpHeaders(): Map<String, String> = buildMap {
    this@toHttpHeaders.values.forEach { (key, value) ->
        put("krpc-$key", value)
    }
}

fun emptyMetadata(): Metadata = Metadata(emptyMap())

data class Error<T, E>(
    val code: ErrorCode,
    val message: String,
    val details: E? = null,
    override val metadata: Metadata = emptyMetadata()
) : Response<T, E>()

fun <T, E> Response<T, E>.withMetadata(metadata: Metadata): Response<T, E> = when (this) {
    is Success -> copy(metadata = metadata)
    is Error -> copy(metadata = metadata)
}

// serialized version of an error
@Serializable
class ResponseError<T>(
    val code: ErrorCode,
    val message: String,
    val details: T? = null
)

class ResponseSerializer<T, E>(private val successSerializer: KSerializer<T>, private val errorSerializer: KSerializer<E>) : KSerializer<Response<T, E>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Response") {
        element("success", successSerializer.descriptor, isOptional = true)
        element("error", ResponseError.serializer(errorSerializer).descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Response<T, E> {
        require(decoder is JsonDecoder) // this class can be decoded only by Json

        val element = decoder.decodeJsonElement()

        require(element is JsonObject)

        if ("success" in element) {
            return Success(decoder.json.decodeFromJsonElement(successSerializer, element["success"]!!))
        }

        if ("error" in element) {
            val error = decoder.json.decodeFromJsonElement(ResponseError.serializer(errorSerializer), element["error"]!!)
            return Error(error.code, error.message, error.details)
        }

        return Error(ErrorCode.INTERNAL, "Invalid response message: $element")
    }

    override fun serialize(encoder: Encoder, value: Response<T, E>) {
        require(encoder is kotlinx.serialization.json.JsonEncoder) // This class can be encoded only by Json
        val element = when (value) {
            is Success -> buildJsonObject {
                put("success", encoder.json.encodeToJsonElement(successSerializer, value.result))
            }
            is Error -> buildJsonObject {
                put("error", encoder.json.encodeToJsonElement(ResponseError.serializer(errorSerializer), ResponseError(value.code, value.message, value.details)))
            }
        }
        encoder.encodeJsonElement(element)
    }
}


