package dev.jomu.krpc

/**
 * Annotation to mark an interface as a kRPC service for the kRPC compiler
 *
 * The kRPC service will contain a method/endpoint for each suspendable method of this interface (including implemented
 * interfaces). Each method MUST return a [Response] object.
 */
@Target(AnnotationTarget.CLASS)
annotation class KrpcService
