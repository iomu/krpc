package dev.jomu.krpc

/**
 * The different responses a kRPC method can return
 *
 * The response of a call to a kRPC method can either be a [Success] or an [Error]. In both cases [metadata][metadata]
 * can be transmitted to the client.
 *
 * @property metadata
 * Metadata that will be transmitted to the client
 *
 * @param T
 * The type of successful responses
 *
 * @param E
 * The type of the [Error.details] of failed calls
 */
sealed class Response<T, E> {
    abstract val metadata: Metadata

    /**
     * Encodes a successful response to a kRPC call
     *
     * @param result
     * The result of the call
     *
     * @param metadata
     * Metadata that will be transmitted to the client
     */
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

    /**
     * Encodes a failed response to a kRPC call
     *
     * @param code
     * The kRPC error code indicating the cause of the error
     *
     * @param message
     * A message that describes the error
     *
     * @param details
     * Optional details that give more details about the error to the caller
     *
     * @param metadata
     * Metadata that will be transmitted to the client
     */
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

/**
 * Creates a [Success] response
 */
fun <T, E> Success(result: T, metadata: Metadata = emptyMetadata()): Response.Success<T, E> =
    Response.Success(result, metadata)

/**
 * Creates an [Error] response
 */
fun <T, E> Error(
    code: ErrorCode,
    message: String,
    details: E?,
    metadata: Metadata = emptyMetadata(),
): Response.Error<T, E> = Response.Error(code, message, details, metadata)

/**
 * Describes the specific error of a method call
 */
enum class ErrorCode {
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    PERMISSION_DENIED,
    INTERNAL,
    UNAUTHENTICATED,
    UNIMPLEMENTED,
}

/**
 * Copies the existing response and overwrites the metadata
 */
fun <T, E> Response<T, E>.withMetadata(metadata: Metadata): Response<T, E> = when (this) {
    is Response.Success -> copy(metadata = metadata)
    is Response.Error -> copy(metadata = metadata)
}
