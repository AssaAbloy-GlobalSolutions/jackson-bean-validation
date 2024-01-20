package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.type.*
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.jsonPropertyNames
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

internal class ConstraintValidatingDeserializer(
    base: BeanDeserializerBase,
    private val validator: KotlinBeanValidator,
) : BeanDeserializer(base) {

    private val ctor: KFunction<Any> = handledType().kotlin.primaryConstructor!!
    private val parameters: List<MappedParameter>

    init {
        val jsonLookup = handledType().kotlin.jsonPropertyNames()
        parameters = ctor.parameters
            .map { param -> MappedParameter(param, jsonLookup[param.name] ?: param.name!!) }
    }

    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext
    ): Any {

        // reconstructing ObjectNode for its ease of use
        val json = parser.readValueAsTree<JsonNode>() // consumes parser
        val codec = parser.codec

        // parse json fields and validate
        val arguments = parameters
            .map { param -> BoundArgument.create(handledType().kotlin, param, json) }
            .map { arg -> deserializeAndValidate(arg, ctxt, codec) }

        // throw if validation failed
        arguments
            .filter(ValidatedArgument::inputValidationFailed)
            .takeIf(List<ValidatedArgument>::isNotEmpty)
            ?.let { throw DataValidationException(it) }

        // instantiate while respecting parameters with default values
        return arguments
            .filterNot(ValidatedArgument::supplantedByDefaultValue)
            .associate { arg -> arg.parameter to arg.value }
            .let(ctor::callBy)
    }

    override fun toString(): String = "${this::class.simpleName}($valueType)"

    private fun deserializeAndValidate(
        argument: BoundArgument,
        ctxt: DeserializationContext,
        codec: ObjectCodec
    ): ValidatedArgument {
        return try {
            argument.deserializeAndValidate(codec, ctxt.typeFactory, validator)
        } catch (e: MismatchedInputException) {
            // enabled with DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
            if (e.message?.startsWith("Cannot map `null` into type") == true ||
                e.message?.startsWith("Cannot coerce `null` to ") == true)
            {
                throw DataValidationException(validator.emitNotNullViolation(argument))
            } else {
                rethrowError(e, argument.name, ctxt)
            }
        } catch (e: Exception) {
            rethrowError(e, argument.name, ctxt)
        }
    }

    // preserving jackson's exception/rethrow behavior
    private fun rethrowError(
        e: Exception,
        field: String,
        ctxt: DeserializationContext
    ): Nothing {
        wrapAndThrow(e, handledType(), field, ctxt)
        error("RETURN")
    }
}
