package dev.jomu.krpc.runtime

import dev.jomu.krpc.ErrorCode
import dev.jomu.krpc.Response
import dev.jomu.krpc.Response.Success
import dev.jomu.krpc.Response.Error
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject


// serialized version of an error
@Serializable
private class ResponseError<T>(
    val code: ErrorCode,
    val message: String,
    val details: T? = null
)

/**
 * A [KSerializer] for [Response] that serializes according to the kRPC protocol
 *
 * @param successSerializer
 * Serializer for the success message
 *
 * @param errorSerializer
 * Serializer for the [error details][Error.details]
 */
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


