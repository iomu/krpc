package dev.jomu.krpc.runtime

sealed class Response<T, E> {
    abstract val metadata: Metadata
}

data class Success<T, E>(val result: T, override val metadata: Metadata = emptyMetadata()) : Response<T, E>()

enum class ErrorCode {
    INVALID_ARGUMENT,
    NOT_FOUND,
    INTERNAL,
    UNAUTHENTICATED,
    PERMISSION_DENIED
}

data class Metadata(val metadata: Map<String, String>) : Map<String, String> by metadata

fun emptyMetadata(): Metadata = Metadata(emptyMap())

data class Error<T, E>(val code: ErrorCode, val message: String, val details: E? = null, override val metadata: Metadata = emptyMetadata()) : Response<T, E>()

fun <T, E> Response<T, E>.withMetadata(metadata: Metadata): Response<T, E> = when(this) {
    is Success -> copy(metadata = metadata)
    is Error -> copy(metadata = metadata)
}


