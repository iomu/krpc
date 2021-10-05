package dev.jomu.krpc

/**
 * Metadata that is sent along with a kRPC request or response
 *
 * The metadata is comparable with HTTP headers and consists of key-value pairs
 *
 * @property values
 * The key-value pairs of this [Metadata] instance
 */
class Metadata(val values: Map<String, String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Metadata

        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}

/**
 * Creates an empty [Metadata] object
 */
fun emptyMetadata(): Metadata = Metadata(emptyMap())