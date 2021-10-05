package dev.jomu.krpc.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.jomu.krpc.*
import dev.jomu.krpc.runtime.MethodInfo
import dev.jomu.krpc.runtime.UnaryServerInterceptor
import dev.jomu.krpc.runtime.buildKrpcServer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@KrpcService
interface InterceptorTestService {
    suspend fun echo(value: String): Response<String, Unit>
}

internal class InterceptorsTest {
    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `interceptor that does nothing`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInterceptorTestService(object : InterceptorTestService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success("this is a success")
                }
            })

            addInterceptor(object : UnaryServerInterceptor {
                override suspend fun <Req, Resp, Err> intercept(
                    info: MethodInfo<Req, Resp, Err>,
                    request: Req,
                    metadata: Metadata,
                    next: suspend (Req, Metadata) -> Response<Resp, Err>
                ): Response<Resp, Err> {
                    return next(request, metadata)
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InterceptorTestServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Success("this is a success"))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `interceptor that returns the method name as metadata`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInterceptorTestService(object : InterceptorTestService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success("this is a success")
                }
            })

            addInterceptor(object : UnaryServerInterceptor {
                override suspend fun <Req, Resp, Err> intercept(
                    info: MethodInfo<Req, Resp, Err>,
                    request: Req,
                    metadata: Metadata,
                    next: suspend (Req, Metadata) -> Response<Resp, Err>
                ): Response<Resp, Err> {
                    return next(request, metadata).withMetadata(Metadata(mapOf("name" to info.name)))
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InterceptorTestServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Success("this is a success", metadata = Metadata(mapOf("name" to "echo"))))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `server interceptor that always returns an internal error`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInterceptorTestService(object : InterceptorTestService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success("this is a success")
                }
            })

            addInterceptor(object : UnaryServerInterceptor {
                override suspend fun <Req, Resp, Err> intercept(
                    info: MethodInfo<Req, Resp, Err>,
                    request: Req,
                    metadata: Metadata,
                    next: suspend (Req, Metadata) -> Response<Resp, Err>
                ): Response<Resp, Err> {
                    return Error(ErrorCode.INTERNAL, "from interceptor", null)
                }

            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InterceptorTestServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Error(ErrorCode.INTERNAL, "from interceptor", null))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = KrpcTestRunnerSource::class)
    fun `server interceptor that always returns a success`(runner: KrpcTestRunner) {
        val krpcServer = buildKrpcServer {
            registerInterceptorTestService(object : InterceptorTestService {
                override suspend fun echo(value: String): Response<String, Unit> {
                    return Success("this is a success")
                }
            })

            addInterceptor(object : UnaryServerInterceptor {
                override suspend fun <Req, Resp, Err> intercept(
                    info: MethodInfo<Req, Resp, Err>,
                    request: Req,
                    metadata: Metadata,
                    next: suspend (Req, Metadata) -> Response<Resp, Err>
                ): Response<Resp, Err> {
                    return Response.Success<String, Err>("this comes from the interceptor") as Response.Success<Resp, Err>
                }
            })
        }

        runner.runTest(krpcServer) { url, httpClient ->
            val client = InterceptorTestServiceClient(httpClient, url)

            val request = "this is a nice test"
            val response = client.echo(request)

            assertThat(response).isEqualTo(Response.Success("this comes from the interceptor"))
        }
    }
}