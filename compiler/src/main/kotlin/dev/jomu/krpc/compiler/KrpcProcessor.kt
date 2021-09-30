package dev.jomu.krpc.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*
import dev.jomu.krpc.runtime.*
import java.util.*

@OptIn(KotlinPoetKspPreview::class)
class KrpcProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serviceDeclarations = resolver
            .getSymbolsWithAnnotation(KrpcService::class.java.canonicalName)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }

        val services = serviceDeclarations.map { decl ->
            val endpoints =
                decl.getAllFunctions().filter { it.modifiers.contains(Modifier.SUSPEND) }.map {
                    val (successType, errorDetails) = resolveResponseTypes(it)
                    Endpoint(it, successType, errorDetails)
                }
            Service(decl, endpoints)
        }

        val generated = services.associateWith { service ->
            service.endpoints.map {
                val request = generateRequestWrappers(service, it)
                GeneratedSpecs(
                    it,
                    request = request,
                    handler = generateRequestHandlers(service, it, request),
                )
            }.toList()
        }

        val requestFiles = generated.map { (service, specs) ->
            val packageName = service.declaration.packageName.asString()
            val fileBuilder = FileSpec.builder(packageName, "${service.name}Krpc")

            val (serviceDescriptor, registerFunc) = generateServiceDescriptor(service, specs)

            val client = generateClient(service, specs)

            specs.forEach {
                fileBuilder.addType(it.request)
                fileBuilder.addFunction(it.handler)
            }
            fileBuilder.addProperty(serviceDescriptor)
            fileBuilder.addFunction(registerFunc)
            fileBuilder.addType(client)

            return@map fileBuilder.build()
        }

        requestFiles.forEach {
            it.writeTo(codeGenerator, false)
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
        endpoint.declaration.parameters
            .filter { !it.type.isClass(Metadata::class) }
            .forEach { parameter ->
                val name = parameter.name!!.asString()
                val type = parameter.type.resolve().toTypeName()

                constructor.addParameter(name, type)
                requestType.addProperty(PropertySpec.builder(name, type, KModifier.INTERNAL).initializer(name).build())
            }
        requestType.primaryConstructor(constructor.build())
        service.declaration.containingFile?.let { requestType.addOriginatingKSFile(it) }
        return requestType.build()
    }

    fun resolveResponseTypes(declaration: KSFunctionDeclaration): Pair<KSType, KSType> {
        val returnType = declaration.returnType?.resolve()
        if (returnType == null) {
            logger.error("function has no return type", declaration)
            error("TODO clean this up")
        }

        if (returnType.declaration.qualifiedName?.asString() != Response::class.qualifiedName) {
            logger.error("endpoints must return a ${Response::class.qualifiedName}", declaration)
            error("TODO clean this up")
        }

        if (returnType.nullability == Nullability.NULLABLE) {
            logger.error("return type is nullable", declaration)
            error("TODO clean this up")
        }

        val successType = returnType.arguments[0].type?.resolve()
        if (successType == null) {
            logger.error("could not resolve return type argument", declaration)
            error("TODO clean this up")
        }

        val errorType = returnType.arguments[1].type?.resolve()
        if (errorType == null) {
            logger.error("could not resolve return type argument", declaration)
            error("TODO clean this up")
        }

        return successType to errorType
    }

    fun generateRequestHandlers(
        service: Service,
        endpoint: Endpoint,
        request: TypeSpec,
    ): FunSpec {
        val requestTypeName = ClassName(service.declaration.packageName.asString(), request.name!!)

        // TODO: add to endpoint
        val returnType = Response::class.asTypeName().parameterizedBy(endpoint.responseSuccessType.toTypeName(), endpoint.responseErrorType.toTypeName())
        val funSpec = FunSpec.builder(
            "handle${service.name}${
                endpoint.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }"
        )
            .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
            .addParameter("service", service.declaration.toClassName())
            .addParameter("request", requestTypeName)
            .addParameter("metadata", Metadata::class)
            .returns(returnType)

        val parameterNames = endpoint.declaration.parameters.mapNotNull { it.name?.asString() }

        val metadataParameters = endpoint.declaration.parameters.mapIndexed { index, parameter -> index to parameter }
            .filter { it.second.type.isClass(Metadata::class) }

        if (metadataParameters.size > 1) {
            logger.error("more than one metadata parameter", endpoint.declaration)
            error("TODO clean this up")
        }

        val metadataIndex = metadataParameters.firstOrNull()?.first ?: -1
        funSpec.apply {
            val arguments = parameterNames.mapIndexed { index, name -> index to name }
                .joinToString(separator = ", ") { if (it.first == metadataIndex) "metadata" else "request.${it.second}" }
            addStatement("return service.${endpoint.name}($arguments)")
        }

        service.declaration.containingFile?.let { funSpec.addOriginatingKSFile(it) }


        return funSpec.build()
    }

    private fun generateServiceDescriptor(service: Service, specs: List<GeneratedSpecs>): Pair<PropertySpec, FunSpec> {
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
                            addStatement("info = %T(%S),", MethodInfo::class, "${service.declaration.qualifiedName?.asString()}/${endpoint.endpoint.name}")
                            addStatement(
                                "handler = %L,",
                                MemberName(
                                    service.declaration.packageName.asString(),
                                    endpoint.handler.name
                                ).reference()
                            )
                            addStatement("requestDeserializer = %N.serializer(),", endpoint.request)
                            val (successFormat, successArgs) = generateSerializerFactory(endpoint.endpoint.responseSuccessType)
                            val (errorFormat, errorArgs) = generateSerializerFactory(endpoint.endpoint.responseErrorType)
                            addStatement("responseSerializer = %T($successFormat, $errorFormat)", ResponseSerializer::class, *successArgs.toTypedArray(), *errorArgs.toTypedArray())
                        }
                        addStatement("),")
                    }
                }
                addStatement("),")
            }
            addStatement(")")
        }
        val name = "${service.name.replaceFirstChar { it.lowercaseChar() }}Descriptor"
        val type = ServiceDescriptor::class.asClassName().parameterizedBy(service.declaration.toClassName())
        val propertySpec = PropertySpec.builder(name, type)
            .initializer(initializer)
            .addModifiers(KModifier.PRIVATE)

        service.declaration.containingFile?.let { propertySpec.addOriginatingKSFile(it) }

        val function = FunSpec.builder("register${service.name}").apply {
            receiver(KrpcServerBuilder::class)

            addParameter("implementation", service.declaration.toClassName())

            addStatement("addService($name, implementation)", )
        }.build()

        return propertySpec.build() to function
    }

    private fun generateClient(service: Service, specs: List<GeneratedSpecs>): TypeSpec {
        return TypeSpec.classBuilder("${service.name}Client").apply {
            addSuperinterface(service.declaration.toClassName())

            val interceptorType = UnaryClientInterceptor::class.asTypeName().copy(nullable = true)
            primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("client", KrpcClient::class)
                    .addParameter("baseUrl", String::class)
                    .addParameter(ParameterSpec.builder("interceptor", interceptorType).defaultValue("null").build())
                    .build()
            )

            superclass(BaseKrpcClient::class)
            addSuperclassConstructorParameter("client")
            addSuperclassConstructorParameter("baseUrl")
            addSuperclassConstructorParameter("interceptor")

            addProperty(
                PropertySpec.builder("client", KrpcClient::class, KModifier.PRIVATE).initializer("client").build()
            )
            addProperty(
                PropertySpec.builder("baseUrl", String::class, KModifier.PRIVATE).initializer("baseUrl").build()
            )
            addProperty(
                PropertySpec.builder("interceptor", interceptorType, KModifier.PRIVATE).initializer("interceptor")
                    .build()
            )
            addProperty(
                PropertySpec.builder("json", ClassName("kotlinx.serialization.json", "Json"), KModifier.PRIVATE)
                    .initializer("%T { ignoreUnknownKeys = true }", ClassName("kotlinx.serialization.json", "Json"))
                    .build()
            )

            specs.forEach { endpoint ->
                addFunction(
                    FunSpec.builder(endpoint.endpoint.name).apply {
                        addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)

                        val returnType = endpoint.endpoint.declaration.returnType?.resolve()?.toTypeName()!!
                        returns(returnType)

                        endpoint.endpoint.declaration.parameters.forEach { parameter ->
                            addParameter(parameter.name!!.asString(), parameter.type.resolve().toTypeName())
                        }

                        val metadataParameters =
                            endpoint.endpoint.declaration.parameters.mapIndexed { index, parameter -> index to parameter }
                                .filter { it.second.type.isClass(Metadata::class) }

                        val metadataParameterName = metadataParameters.firstOrNull()?.second?.name?.asString()
                        val path = "${service.declaration.qualifiedName?.asString()}/${endpoint.endpoint.name}"

                        addCode(buildCodeBlock {

                            val parameters =
                                endpoint.endpoint.declaration.parameters.filter { !it.type.isClass(Metadata::class) }
                                    .mapNotNull { it.name?.asString() }.joinToString(", ")
                            addStatement("val request = %N($parameters)", endpoint.request)
                            addStatement(
                                "val requestMetadata = ${metadataParameterName ?: "%M()"}",
                                *(if (metadataParameterName == null) listOf(
                                    MemberName(
                                        "dev.jomu.krpc.runtime",
                                        "emptyMetadata"
                                    )
                                ) else emptyList()).toTypedArray()
                            )

                            addStatement("")

                            val (successFormat, successArgs) = generateSerializerFactory(endpoint.endpoint.responseSuccessType)
                            addStatement(
                                "val successSerializer = $successFormat",
                                *successArgs.toTypedArray()
                            )
                            val (errorFormat, errorArgs) = generateSerializerFactory(endpoint.endpoint.responseErrorType)
                            addStatement(
                                "val errorSerializer = $errorFormat",
                                *errorArgs.toTypedArray()
                            )
                            addStatement(
                                "val responseDeserializer = %T(successSerializer, errorSerializer)",
                                ResponseSerializer::class
                            )

                            addStatement("")

                            addStatement("return executeUnaryCall(")
                            withIndent {
                                addStatement("%S,", path)
                                addStatement("%T(%S),", MethodInfo::class, path)
                                addStatement("request,")
                                addStatement("requestMetadata,")
                                addStatement("%N.serializer(),", endpoint.request)
                                addStatement("responseDeserializer,", endpoint.request)
                            }
                            addStatement(")")
                        })
                    }
                        .build()
                )
            }
            service.declaration.containingFile?.let { addOriginatingKSFile(it) }
        }.build()
    }

    private fun generateSerializerFactory(type: KSType): Pair<String, List<Any?>> {
        val serializerFunc = MemberName("kotlinx.serialization.builtins", "serializer")
        return when {

            type.isAssignableFrom(List::class) -> {
                val (elementFormat, elementArgs) = generateSerializerFactory(type.arguments.first().type?.resolve()!!)
                "%M($elementFormat)" to listOf(
                    MemberName("kotlinx.serialization.builtins", "ListSerializer"),
                    *elementArgs.toTypedArray()
                )
            }
            else -> "%T.%M()" to listOf(type.toTypeName(), serializerFunc)
        }
    }
}


class Service(val declaration: KSClassDeclaration, val endpoints: Sequence<Endpoint>)

val Service.name
    get() = declaration.shortName

class Endpoint(
    val declaration: KSFunctionDeclaration,
    val responseSuccessType: KSType,
    val responseErrorType: KSType
)

val Endpoint.name
    get() = declaration.simpleName.asString()

class GeneratedSpecs(val endpoint: Endpoint, val request: TypeSpec, val handler: FunSpec)

