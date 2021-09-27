package dev.jomu.krpc.runtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OkHttpKrpcClient(private val client: OkHttpClient) : KrpcClient {
    override suspend fun executeUnaryCall(url: String, message: EncodableMessage<*>): Call {
        val headers = Headers.Builder().apply {
            message.metadata.forEach { (key, value) ->
                add("krpc-$key", value)
            }
        }.build()


        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/json".toMediaType()
                }

                override fun writeTo(sink: BufferedSink) {
                    runBlocking {
                        message.encode(CallEncoder(sink.outputStream()))
                    }
                }
            })
            .build()

        val response = client.newCall(request).await()

        return OkHttpCall(response)
    }
}

private class CallEncoder(val stream: OutputStream): JsonEncoder<Unit> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U) {
        json.encodeToStream(serializer, value, stream)
    }
}

private class OkHttpCall(val response: Response) : Call {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromStream(deserializer, response.body?.byteStream() ?: error("no response body"))
    }

    override val metadata: Metadata
        get() = response.headers.toMetadata()

}

private fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun okhttp3.Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            cont.resumeWithException(e)
        }

        override fun onResponse(call: okhttp3.Call, response: Response) {
            cont.resume(response)
        }
    })

    cont.invokeOnCancellation { cancel() }
}