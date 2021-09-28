package dev.jomu.krpc.runtime

import kotlinx.serialization.Serializable

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


