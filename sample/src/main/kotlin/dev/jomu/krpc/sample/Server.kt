package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.*
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.Error

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            registerTestService(Implementation, asRegistrar(), PrintInterceptor)
        }
    }.start(wait = true)
}

object Implementation : TestService {
    override suspend fun hello(name: String): Response<String, Unit> {
        return Error(ErrorCode.INTERNAL, "we got an error chief", Unit)
    }

    override suspend fun second(name: String, number: Int): Response<String, CustomError> {
        return Error(ErrorCode.INTERNAL, "we got an error chief", CustomError(123))
    }

    override suspend fun third(name: String, number: Int, more: Float): Response<List<String>, CustomError> {
        return Success(listOf(name, "$number"))
    }
}

object PrintInterceptor : UnaryServerInterceptor {
    override suspend fun <Req, Resp, Err> intercept(
        info: MethodInfo,
        request: KrpcRequest<Req>,
        next: suspend (KrpcRequest<Req>) -> Response<Resp, Err>
    ): Response<Resp, Err> {
        println("Request: ${request.value}")
        return next(request).also {
            println("Response: $it")
        }
    }

}