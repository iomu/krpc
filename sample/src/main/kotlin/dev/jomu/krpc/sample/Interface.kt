package dev.jomu.krpc.sample

import dev.jomu.krpc.runtime.KrpcService
import dev.jomu.krpc.runtime.Metadata
import dev.jomu.krpc.runtime.Response
import kotlinx.serialization.Serializable

@Serializable
class CustomError(val test: Int)

interface GenericBase<T> {
    suspend fun generic(value: T, count: Int): Response<List<T>, Unit>
}

@KrpcService
interface TestService : GenericBase<String> {
    suspend fun hello(name: String, metadata: Metadata): Response<String, Unit>
    suspend fun second(name: String, number: Int): Response<String, CustomError>
    suspend fun third(name: String, number: Int, more: Float): Response<List<String>, CustomError>
}