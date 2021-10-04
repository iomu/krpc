package dev.jomu.krpc.runtime

import dev.jomu.krpc.Metadata

internal fun metadataFromHttpHeaders(headers: Map<String, String>): Metadata =
    Metadata(headers.filterKeys { it.startsWith("krpc-") }
        .mapKeys { (value, _) -> value.removePrefix("krpc-") })


@OptIn(ExperimentalStdlibApi::class)
internal fun Metadata.toHttpHeaders(): Map<String, String> = buildMap {
    this@toHttpHeaders.values.forEach { (key, value) ->
        put("krpc-$key", value)
    }
}