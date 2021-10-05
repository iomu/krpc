package dev.jomu.krpc.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.jomu.krpc.KrpcService
import dev.jomu.krpc.Metadata
import dev.jomu.krpc.Response
import dev.jomu.krpc.Success
import dev.jomu.krpc.runtime.MethodInfo
import dev.jomu.krpc.runtime.UnaryServerInterceptor
import dev.jomu.krpc.runtime.buildKrpcServer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

interface BaseService {
    suspend fun base(input: Int): Response<String, Unit>
}

interface GenericBaseService<T> {
    suspend fun genericBase(input: T): Response<T, Unit>
}

@KrpcService
interface InheritingService : BaseService, GenericBaseService<String> {
    suspend fun echo(value: String): Response<String, Unit>
}

internal class InheritanceTest {
    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `calling regular method`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInheritingService(object : InheritingService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success(value)
                }

                override suspend fun base(input: Int): Response<String, Unit> {
                    return Success(input.toString())
                }

                override suspend fun genericBase(input: String): Response<String, Unit> {
                    return Success("Response $input")
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InheritingServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Success("this is a nice test"))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `calling inherited method`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInheritingService(object : InheritingService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success(value)
                }

                override suspend fun base(input: Int): Response<String, Unit> {
                    return Success(input.toString())
                }

                override suspend fun genericBase(input: String): Response<String, Unit> {
                    return Success("Response $input")
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InheritingServiceClient(httpClient, url)

            val response = client.base(123)

            assertThat(response).isEqualTo(Success("123"))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `calling generic inherited method`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInheritingService(object : InheritingService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success(value)
                }

                override suspend fun base(input: Int): Response<String, Unit> {
                    return Success(input.toString())
                }

                override suspend fun genericBase(input: String): Response<String, Unit> {
                    return Success("Response: $input")
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InheritingServiceClient(httpClient, url)

            val response = client.genericBase("generic")

            assertThat(response).isEqualTo(Success("Response: generic"))
        }
    }
}