package dev.jomu.krpc.compiler

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.jomu.krpc.runtime.*
import java.util.*

class KrpcProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serviceDeclarations = resolver
            .getSymbolsWithAnnotation(KrpcService::class.java.canonicalName)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }

        val services = serviceDeclarations.map { decl ->
            val endpoints = decl.getDeclaredFunctions().map { Endpoint(it) }
            Service(decl, endpoints)
        }

        val generated = services.associateWith { service ->
            service.endpoints.map {
                val request = generateRequestWrappers(service, it)
                val (response, responseError) = generateResponseWrappers(service, it)
                GeneratedSpecs(
                    it,
                    request = request,
                    response = response,
                    handler = generateRequestHandlers(service, it, request, response, responseError),
                )
            }.toList()
        }

        val requestFiles = generated.map { (service, specs) ->
            val packageName = service.declaration.packageName.asString()
            val fileBuilder = FileSpec.builder(packageName, "${service.name}Krpc")

            val serviceDescriptor = generateServiceDescriptor(service, specs)

            val registerFunction = FunSpec.builder("register${service.name}")
                .addParameter("service", service.declaration.asClassName())
                .addParameter("registrar", MethodRegistrar::class)
                .addParameter(ParameterSpec.builder("interceptor", UnaryServerInterceptor::class.asTypeName().copy(nullable = true)).defaultValue("null").build())
                .addCode("registrar.register(service, %M, interceptor);", MemberName(packageName, serviceDescriptor.name))
                .build()

            val client = generateClient(service, specs)

            specs.forEach {
                fileBuilder.addType(it.request)
                fileBuilder.addType(it.response)
                fileBuilder.addFunction(it.handler)
            }
            fileBuilder.addProperty(serviceDescriptor)
            fileBuilder.addFunction(registerFunction)
            fileBuilder.addType(client)

            return@map fileBuilder.build()
        }

        requestFiles.forEach {
            it.writeTo(codeGenerator)
        }

        return emptyList()
    }

    fun generateRequestWrappers(service: Service, endpoint: Endpoint): TypeSpec {
        val requestType = TypeSpec
            .classBuilder("${service.name}${endpoint.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Request")
            .addModifiers(KModifier.DATA)
            .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))

        requestType.addModifiers(KModifier.PRIVATE)
        val constructor = FunSpec.constructorBuilder()
        endpoint.declaration.parameters.forEach { parameter ->
            val name = parameter.name!!.asString()
            val type = parameter.type.resolve().asTypeName()

            constructor.addParameter(name, type)
            requestType.addProperty(PropertySpec.builder(name, type, KModifier.INTERNAL).initializer(name).build())
        }
        requestType.primaryConstructor(constructor.build())
        service.declaration.containingFile?.let { requestType.addOriginatingKSFile(it) }
        return requestType.build()
    }

    fun generateResponseWrappers(service: Service, endpoint: Endpoint): Pair<TypeSpec, TypeSpec> {
        val name =
            "${service.name}${endpoint.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Response"
        val responseType = TypeSpec
            .classBuilder(name)
            .addModifiers(KModifier.PRIVATE)
            .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))

        responseType.addModifiers(KModifier.PRIVATE)
        val constructor = FunSpec.constructorBuilder()

        val returnType = endpoint.declaration.returnType?.resolve()
        if (returnType == null) {
            logger.error("function has no return type", endpoint.declaration)
            error("TODO clean this up")
        }

        if (returnType.declaration.qualifiedName?.asString() != Response::class.qualifiedName) {
            logger.error("endpoints must return a ${Response::class.qualifiedName}", endpoint.declaration)
            error("TODO clean this up")
        }

        if (returnType.nullability == Nullability.NULLABLE) {
            logger.error("return type is nullable", endpoint.declaration)
            error("TODO clean this up")
        }

        val successType = returnType.arguments[0].type?.resolve()?.asTypeName()
        if (successType == null) {
            logger.error("could not resolve return type argument", endpoint.declaration)
            error("TODO clean this up")
        }

        val errorType = returnType.arguments[1].type?.resolve()?.asTypeName()
        if (errorType == null) {
            logger.error("could not resolve return type argument", endpoint.declaration)
            error("TODO clean this up")
        }

        val errorTypeSpec = TypeSpec.classBuilder("Error").primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("code", ErrorCode::class)
            .addParameter("message", String::class)
            .addParameter("details", errorType.copy(nullable = true))
            .build())
            .addModifiers(KModifier.INTERNAL)
            .addProperty(PropertySpec.builder("code", ErrorCode::class, KModifier.INTERNAL).initializer("code").build())
            .addProperty(PropertySpec.builder("message", String::class, KModifier.INTERNAL).initializer("message").build())
            .addProperty(PropertySpec.builder("details", errorType.copy(nullable = true), KModifier.INTERNAL).initializer("details").build())
            .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
            .build()

        responseType.addType(errorTypeSpec)

        constructor.addParameter(ParameterSpec.builder("success", successType.copy(nullable = true), KModifier.INTERNAL).defaultValue("null").build())
        responseType.addProperty(PropertySpec.builder("success", successType.copy(nullable = true), KModifier.INTERNAL).initializer("success").build())


        val errorClassName = ClassName(service.declaration.packageName.asString(), name).nestedClass(errorTypeSpec.name!!).copy(nullable = true)
        constructor.addParameter(ParameterSpec.builder("error", errorClassName, KModifier.INTERNAL).defaultValue("null").build())
        responseType.addProperty(PropertySpec.builder("error", errorClassName, KModifier.INTERNAL).initializer("error").build())


        responseType.primaryConstructor(constructor.build())

        service.declaration.containingFile?.let { responseType.addOriginatingKSFile(it) }

        return responseType.build() to errorTypeSpec
    }

    fun generateRequestHandlers(service: Service, endpoint: Endpoint, request: TypeSpec, response: TypeSpec, responseError: TypeSpec): FunSpec {
        val funSpec = FunSpec.builder("handle${service.name}${endpoint.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}")
            .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
            .addParameter("service", service.declaration.asClassName())
            .addParameter("messageStream", MessageStream::class)
            .addParameter("interceptor", UnaryServerInterceptor::class.asTypeName().copy(nullable = true))
            .returns(KrpcMessage::class.asTypeName().parameterizedBy(STAR))

        funSpec.apply {
            addStatement("val request = messageStream.read(%N.serializer())", request)
            val parameterNames = endpoint.declaration.parameters.mapNotNull { it.name?.asString() }
            beginControlFlow("val response = interceptor?.intercept(%T(%S), %T(request, mapOf())) {", MethodInfo::class, "${service.declaration.qualifiedName?.asString()}/${endpoint.name}", KrpcRequest::class)
            val wrappedArguments = parameterNames.joinToString(separator = ", ") { "it.value.$it" }
            addStatement("service.${endpoint.name}($wrappedArguments)")
            endControlFlow()
            val arguments = parameterNames.joinToString(separator = ", ") { "request.$it" }
            addStatement("?: service.${endpoint.name}($arguments)")
            beginControlFlow("val result = when (response) {")
            addStatement("is %T -> %N(success = response.result)", Success::class, response)
            addStatement("is %T -> %N(error = %T(response.code, response.message, response.details,))", Error::class, response, ClassName(service.declaration.packageName.asString(), response.name!!).nestedClass(responseError.name!!))
            endControlFlow()
            addStatement("return %T(result, %N.serializer(), response.metadata)", KrpcMessage::class, response)
        }

        service.declaration.containingFile?.let { funSpec.addOriginatingKSFile(it) }


        return funSpec.build()
    }

    private fun generateServiceDescriptor(service: Service, specs: List<GeneratedSpecs>): PropertySpec {
        val initializer = buildCodeBlock {
            addStatement("%T(", ServiceDescriptor::class)
            withIndent {
                addStatement("name = %S,", service.declaration.qualifiedName?.asString())
                addStatement("methods = listOf(")
                withIndent {
                    specs.forEach { endpoint ->
                        addStatement("%T(", MethodDescriptor::class)
                        withIndent {
                            addStatement("%S,", endpoint.endpoint.name)
                            addStatement("handler = %L,", MemberName(service.declaration.packageName.asString(), endpoint.handler.name).reference())
                        }
                        addStatement("),")
                    }
                }
                addStatement("),")
            }
            addStatement(")")
        }
        val name = "${service.name.replaceFirstChar { it.lowercaseChar() }}Descriptor"
        val type = ServiceDescriptor::class.asClassName().parameterizedBy(service.declaration.asClassName())
        val propertySpec = PropertySpec.builder(name, type, KModifier.PRIVATE)
           .initializer(initializer)

        service.declaration.containingFile?.let { propertySpec.addOriginatingKSFile(it) }

        return propertySpec.build()
    }

    private fun generateClient(service: Service, specs: List<GeneratedSpecs>): TypeSpec {
        return TypeSpec.classBuilder("${service.name}Client").apply {
            addSuperinterface(service.declaration.asClassName())

            primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("client", KrpcClient::class)
                .addParameter("baseUrl", String::class)
                .build())

            addProperty(PropertySpec.builder("client", KrpcClient::class, KModifier.PRIVATE).initializer("client").build())
            addProperty(PropertySpec.builder("baseUrl", String::class, KModifier.PRIVATE).initializer("baseUrl").build())

            specs.forEach { endpoint ->
                addFunction(
                    FunSpec.builder(endpoint.endpoint.name).apply {
                        addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)

                        returns(endpoint.endpoint.declaration.returnType?.resolve()?.asTypeName()!!)

                        endpoint.endpoint.declaration.parameters.forEach { parameter ->
                            addParameter(parameter.name!!.asString(), parameter.type.resolve().asTypeName())
                        }

                        addCode(buildCodeBlock {
                            addStatement("val url = %P", "\${baseUrl}/${service.declaration.qualifiedName?.asString()}/${endpoint.endpoint.name}")
                            val parameters = endpoint.endpoint.declaration.parameters.mapNotNull { it.name?.asString() }.joinToString(", ")
                            addStatement("val krpcRequest = %N($parameters)", endpoint.request)
                            addStatement("val message = %T(krpcRequest, %N.serializer())", KrpcMessage::class, endpoint.request)
                            addStatement("val messageStream = client.executeUnaryCall(url, message)")
                            addStatement("val response = messageStream.read(%N.serializer())", endpoint.response)
                            beginControlFlow("return if (response.success != null) {")
                            addStatement("%T(response.success)", Success::class)
                            nextControlFlow("else")
                            addStatement("%T(response.error!!.code, response.error.message, response.error.details)", Error::class)
                            endControlFlow()
                        })
                    }
                    .build()
                )
            }
            service.declaration.containingFile?.let { addOriginatingKSFile(it) }
        }.build()
    }
}

class Service(val declaration: KSClassDeclaration, val endpoints: Sequence<Endpoint>)

val Service.name
    get() = declaration.shortName

class Endpoint(val declaration: KSFunctionDeclaration)

val Endpoint.name
    get() = declaration.simpleName.asString()

class GeneratedSpecs(val endpoint: Endpoint, val request: TypeSpec, val response: TypeSpec, val handler: FunSpec)

