package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.KrpcService
import dev.jomu.krpc.runtime.Response
import kotlinx.serialization.Serializable

@Serializable
class CustomError(val test: Int)

@KrpcService
interface TestService {
    suspend fun hello(name: String): Response<String, Unit>
    suspend fun second(name: String, number: Int): Response<String, CustomError>
    suspend fun third(name: String, number: Int, more: Float): Response<List<String>, CustomError>
}