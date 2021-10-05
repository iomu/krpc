package dev.jomu.krpc.tests

import dev.jomu.krpc.runtime.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

interface KrpcServerContainer {
    fun withRunningServer(krpcServer: KrpcServer, block: suspend (String) -> Unit)
}

interface KrpcTestRunner {
    fun runTest(krpcServer: KrpcServer, block: suspend (String, KrpcHttpClient) -> Unit)
}

class ClientServerTestRunner(val serverContainer: KrpcServerContainer, val client: KrpcHttpClient) : KrpcTestRunner {
    override fun runTest(krpcServer: KrpcServer, block: suspend (String, KrpcHttpClient) -> Unit) {
        serverContainer.withRunningServer(krpcServer) { url ->
            block(url, client)
        }
    }

    override fun toString(): String {
        return "${serverContainer::class.simpleName}+${client::class.simpleName}"
    }
}

object KtorServerTestRunner : KrpcTestRunner {
    override fun runTest(krpcServer: KrpcServer, block: suspend (String, KrpcHttpClient) -> Unit) {
        withTestApplication {
            application.routing {
                registerServer(krpcServer, "")
            }

            runBlocking {
                block("", TestKtorServerClient(this@withTestApplication))
            }
        }
    }

    override fun toString(): String {
        return "KtorTestServer"
    }
}

class TestKtorServerClient(private val engine: TestApplicationEngine) : KrpcHttpClient {
    override suspend fun post(url: String, message: OutgoingMessage<*>): IncomingMessage {
        val call = engine.handleRequest(HttpMethod.Post, url) {
            message.headers.forEach { (key, value) -> addHeader(key, value) }

            setBody(message.write(object : JsonEncoder<String> {
                override fun <U> encode(json: Json, serializer: SerializationStrategy<U>, value: U): String {
                    return json.encodeToString(serializer, value)
                }
            }))
        }

        return object : IncomingMessage {
            override suspend fun <T> read(json: Json, deserializer: DeserializationStrategy<T>): T {
                return json.decodeFromString(deserializer, call.response.content!!)
            }

            override val headers: Map<String, String>
                get() = call.response.headers.allValues().toMap().mapValues { it.value.first() }
        }
    }

}

object OkHttpMockServerContainer : KrpcServerContainer {
    override fun withRunningServer(krpcServer: KrpcServer, block: suspend (String) -> Unit) {
        val dispatcher = KrpcDispatcher(krpcServer)

        val server = MockWebServer()
        server.dispatcher = dispatcher

        server.use {
            runBlocking {
                block(server.url("").toString())
            }
        }
    }
}

class KrpcTestRunnerSource : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return listOf(OkHttpMockServerContainer).flatMap { server ->
            listOf(OkHttpKrpcClient(OkHttpClient()), KtorClient(HttpClient()))
                .map { Arguments.of(ClientServerTestRunner(server, it)) }
        }.plus(Arguments.of(KtorServerTestRunner))
            .stream()
    }
}
