package dev.jomu.krpc

sealed class Response<T, E> {
    abstract val metadata: Metadata

    class Success<T, E>(val result: T, override val metadata: Metadata = emptyMetadata()) : Response<T, E>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success<*, *>

            if (result != other.result) return false
            if (metadata != other.metadata) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = result?.hashCode() ?: 0
            result1 = 31 * result1 + metadata.hashCode()
            return result1
        }

        override fun toString(): String {
            return "Success(result=$result, metadata=$metadata)"
        }

        fun copy(
            result: T = this.result,
            metadata: Metadata = this.metadata
        ): Success<T, E> {
            return Success(result, metadata)
        }
    }

    class Error<T, E>(
        val code: ErrorCode,
        val message: String,
        val details: E? = null,
        override val metadata: Metadata = emptyMetadata()
    ) : Response<T, E>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Error<*, *>

            if (code != other.code) return false
            if (message != other.message) return false
            if (details != other.details) return false
            if (metadata != other.metadata) return false

            return true
        }

        override fun hashCode(): Int {
            var result = code.hashCode()
            result = 31 * result + message.hashCode()
            result = 31 * result + (details?.hashCode() ?: 0)
            result = 31 * result + metadata.hashCode()
            return result
        }

        override fun toString(): String {
            return "Error(code=$code, message='$message', details=$details, metadata=$metadata)"
        }

        fun copy(
            code: ErrorCode = this.code,
            message: String = this.message,
            details: E? = this.details,
            metadata: Metadata = this.metadata
        ): Error<T, E> {
            return Error(code, message, details, metadata)
        }
    }
}

fun <T, E> Success(result: T, metadata: Metadata = emptyMetadata()): Response.Success<T, E> =
    Response.Success(result, metadata)

fun <T, E> Error(
    code: ErrorCode,
    message: String,
    details: E?,
    metadata: Metadata = emptyMetadata(),
): Response.Error<T, E> = Response.Error(code, message, details, metadata)

enum class ErrorCode {
    INVALID_ARGUMENT,
    NOT_FOUND,
    INTERNAL,
    UNAUTHENTICATED,
    PERMISSION_DENIED,
    UNIMPLEMENTED
}

fun <T, E> Response<T, E>.withMetadata(metadata: Metadata): Response<T, E> = when (this) {
    is Response.Success -> copy(metadata = metadata)
    is Response.Error -> copy(metadata = metadata)
}
