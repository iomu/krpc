package dev.jomu.krpc.runtime

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

object JsonSerializer : Serializer {
    private val json = Json { ignoreUnknownKeys = true }
    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): String {
        return json.encodeToString(serializer, value)
    }

    override fun <T> decode(deserializer: DeserializationStrategy<T>, encoded: String): T {
        return json.decodeFromString(deserializer, encoded)
    }
}