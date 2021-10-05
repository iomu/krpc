package dev.jomu.krpc.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import kotlin.test.Test
import kotlin.test.assertEquals

internal class Test {
    @Test
    fun `KrpcService inherits from generic interface`() {
        val source = """
            import dev.jomu.krpc.runtime.*
            
            interface Base<T> {
                suspend fun test(x: T): Response<T, Unit>
            }

            @KrpcService
            interface TestService : Base<Int> {
                suspend fun hello(name: String): Response<String, Unit>
            }

           
        """.trimIndent()
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("main.kt", source))
            messageOutputStream = System.out
            //compilerPlugins = listOf(SerializationComponentRegistrar())
            symbolProcessorProviders = listOf(KrpcProcessorProvider())
            inheritClassPath = true
        }
        val result = compilation.compile()

        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
    }
}