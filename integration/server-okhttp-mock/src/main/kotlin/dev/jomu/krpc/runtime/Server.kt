package dev.jomu.krpc.runtime

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Headers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class KrpcDispatcher(val server: KrpcServer) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return runBlocking {
            val response = server.handleRequest(request.path.orEmpty(), OkHttpCall(request))

            val headers = Headers.Builder().apply {
                response.headers.forEach { (key, value) ->
                    add(key, value)
                }
            }.build()

            MockResponse().setResponseCode(200).setHeaders(headers).setBody(response.encode(JsonStringEncoder))
        }
    }
}

private object JsonStringEncoder : JsonEncoder<String> {
    override suspend fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): String {
        return json.encodeToString(serializer, value)
    }
}

private class OkHttpCall(val request: RecordedRequest) : Call {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> readRequest(json: Json, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromStream(deserializer, request.body.inputStream())
    }

    override val headers: Map<String, String>
        get() = request.headers.toMap()

}

