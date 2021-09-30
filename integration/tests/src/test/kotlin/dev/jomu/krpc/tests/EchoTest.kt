package dev.jomu.krpc.tests

import dev.jomu.krpc.runtime.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals

@KrpcService
interface EchoService {
    suspend fun echo(value: String): Response<String, Unit>
    suspend fun echoMetadata(value: Unit = Unit, metadata: Metadata): Response<Unit, Unit>
}

internal class EchoTest {
    val implementation = object : EchoService {
        override suspend fun echo(value: String): Response<String, Unit> {
            return Success(value)
        }

        override suspend fun echoMetadata(value: Unit, metadata: Metadata): Response<Unit, Unit> {
            return Success(Unit, metadata)
        }
    }

    @Test
    fun testEcho() {
        testEchoService(implementation) { client ->
            val request = "this is a nice test"
            val response = client.echo(request)

            assertEquals(Success(request), response)
        }
    }

    @Test
    fun testEchoMetadata() {
        testEchoService(implementation) { client ->
            val metadata = Metadata(mapOf("a" to "1", "b" to "2"))
            val response = client.echoMetadata(metadata = metadata)

            assertEquals(metadata, response.metadata)
        }
    }

    private fun testEchoService(implementation: EchoService, block: suspend (client: EchoService) -> Unit) {
        val krpcServer = buildKrpcServer {
            registerEchoService(implementation)
        }

        val dispatcher = KrpcDispatcher(krpcServer)

        val server = MockWebServer()
        server.dispatcher = dispatcher

        val client = EchoServiceClient(OkHttpKrpcClient(OkHttpClient()), server.url("").toString())

        runBlocking {
            block(client)
        }
    }
}