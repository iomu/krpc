package dev.jomu.krpc.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import kotlin.reflect.KClass


val KSDeclaration.shortName: String
    get() {
        val name = requireNotNull(qualifiedName) { "expected qualifiedName for '$this' but got null" }
        val packageName = packageName.asString()
        return name.asString().removePrefix("$packageName.")
    }

fun KSTypeReference.isClass(kClass: KClass<*>): Boolean {
    val type = resolve()
    val declaration = type.declaration
    if (declaration !is KSClassDeclaration) {
        return false
    }

   return declaration.qualifiedName?.asString() == kClass.qualifiedName
}