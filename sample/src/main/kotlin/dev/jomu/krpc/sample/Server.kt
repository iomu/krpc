package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.*

fun main() {
    val server = buildKrpcServer {
        addService(testServiceDescriptor, Implementation)
        addInterceptor(PrintInterceptor)
    }
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            registerServer(server)
        }
    }.start(wait = true)
}

object Implementation : TestService {
    override suspend fun hello(name: String, metadata: Metadata): Response<String, Unit> {
        return Success("Hello, $name!", metadata = metadata)
    }

    override suspend fun second(name: String, number: Int): Response<String, CustomError> {
        return Error(ErrorCode.INTERNAL, "we got an error chief", CustomError(123))
    }

    override suspend fun third(name: String, number: Int, more: Float): Response<List<String>, CustomError> {
        return Success(listOf(name, "$number"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun generic(value: String, count: Int): Response<List<String>, Unit> {
        return Success(buildList {
            for (i in 1..count) {
                add(value)
            }
        })
    }
}

object PrintInterceptor : UnaryServerInterceptor, UnaryClientInterceptor {
    override suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err> {
        println("Request: $request")
        val response = next(request).also {
            println("Response: $it")
        }
        return response.withMetadata(Metadata(response.metadata.mapValues { (_, value) -> value.uppercase(Locale.getDefault()) }))
    }
}