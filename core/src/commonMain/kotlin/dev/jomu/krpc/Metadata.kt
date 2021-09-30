package dev.jomu.krpc

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

fun emptyMetadata(): Metadata = Metadata(emptyMap())