package dev.jomu.krpc.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.jomu.krpc.KrpcService
import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response
import dev.jomu.krpc.Success
import dev.jomu.krpc.runtime.buildKrpcServer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@KrpcService
interface EchoService {
    suspend fun echo(value: String): Response<String, Unit>
    suspend fun echoMetadata(value: Unit = Unit, metadata: Metadata): Response<Unit, Unit>
}

internal class EchoTest {
    private val implementation = object : EchoService {
        override suspend fun echo(value: String): Response<String, Unit> {
            return Success(value)
        }

        override suspend fun echoMetadata(value: Unit, metadata: Metadata): Response<Unit, Unit> {
            return Success(Unit, metadata)
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun testEcho(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerEchoService(implementation)
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = EchoServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Success(request))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun testMetadata(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerEchoService(implementation)
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = EchoServiceClient(httpClient, url)

            val metadata = Metadata(mapOf("a" to "1", "b" to "2"))
            val response = client.echoMetadata(metadata = metadata)

            assertThat(response.metadata).isEqualTo(metadata)
        }
    }
}