package dev.jomu.krpc.compiler

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.Taggable
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.tag

internal interface OriginatingKSFiles {
    val files: List<KSFile>
}

internal interface MutableOriginatingKSFiles : OriginatingKSFiles {
    override val files: MutableList<KSFile>
}

internal data class MutableOriginatingKSFilesImpl(override val files: MutableList<KSFile> = mutableListOf()) : MutableOriginatingKSFiles

internal fun FileSpec.originatingKSFiles(): List<KSFile> {
    return members
        .flatMap {
            when (it) {
                is TypeSpec -> it.originatingKSFiles()
                is PropertySpec -> it.originatingKSFiles()
                is FunSpec -> it.originatingKSFiles()
                else -> emptyList() // TypeAlias
            }
        }
        .distinct()
}

private fun Taggable.originatingKSFiles(): List<KSFile> {
    return tag<OriginatingKSFiles>()?.files.orEmpty()
}

internal fun TypeSpec.Builder.addOriginatingKSFile(ksFile: KSFile): TypeSpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

internal fun FunSpec.Builder.addOriginatingKSFile(ksFile: KSFile): FunSpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

internal fun PropertySpec.Builder.addOriginatingKSFile(ksFile: KSFile): PropertySpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

private fun Taggable.Builder<*>.getOrCreateKSFilesTag(): MutableList<KSFile> {
    val holder = tags.getOrPut(
        OriginatingKSFiles::class, ::MutableOriginatingKSFilesImpl
    ) as MutableOriginatingKSFiles
    return holder.files
}