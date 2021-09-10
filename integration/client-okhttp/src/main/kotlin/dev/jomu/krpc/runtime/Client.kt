package dev.jomu.krpc.runtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OkHttpKrpcClient(private val client: OkHttpClient) : KrpcClient {
    override suspend fun executeUnaryCall(url: String, message: KrpcMessage<String>): KrpcMessage<String> {
        val headers = Headers.Builder().apply {
            message.metadata.forEach { (key, value) ->
                add("krpc-$key", value)
            }
        }.build()
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(message.value.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()

        return KrpcMessage(
            response.body?.string() ?: "",
            response.headers.toMetadata()
        )
    }
}

fun Headers.toMetadata(): Metadata = Metadata(toMap()
    .filterKeys { it.startsWith("krpc-") }
    .mapKeys { (value, _) -> value.removePrefix("krpc-") })

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }

    })

    cont.invokeOnCancellation { cancel() }
}