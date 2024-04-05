package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.jsonPropertyLookup
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.type.*
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.jsonPropertyNames
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import com.fasterxml.jackson.databind.deser.CreatorProperty
import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

internal class ConstraintValidatingDeserializer(
    base: BeanDeserializerBase,
    private val validator: KotlinBeanValidator,
) : BeanDeserializer(base) {

    private val ctor: KFunction<Any> = handledType().kotlin.primaryConstructor!!
    private val handledProperties: List<Property>

    init {
        val jsonFieldNames = handledType().kotlin.jsonPropertyNames()
        val propertyLookup = handledType().kotlin.jsonPropertyLookup()::get
        fun isValueClass(prop: SettableBeanProperty): Boolean =
            propertyLookup(prop.name)?.returnType?.jvmErasure?.isValue == true

        val beanProperties = properties()
            .asSequence()
            .filter { prop -> prop !is CreatorProperty }
            .filterNot(::isValueClass) // skip value class properties as they're boxed
            .map { prop -> BeanProperty(propertyLookup(prop.name)!!, jsonFieldNames[prop.name] ?: prop.name!!, prop) }
            .toList()

        val ctorParameters = ctor.parameters
            .map { param -> ConstructorProperty(param, jsonFieldNames[param.name] ?: param.name!!) }

        handledProperties = beanProperties + ctorParameters
    }

    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext
    ): Any {

        // reconstructing ObjectNode for its ease of use
        val json = parser.readValueAsTree<JsonNode>() // consumes parser
        val codec = parser.codec

        // parse json fields and validate
        val arguments = handledProperties
            .map { param -> BoundProperty.create(handledType().kotlin, param, json) }
            .map { arg -> deserializeAndValidate(arg, ctxt, codec) }

        // throw if validation failed for any constructor arguments or bean properties
        arguments
            .filter(ValidatedProperty::inputValidationFailed)
            .takeIf(List<ValidatedProperty>::isNotEmpty)
            ?.let { throw DataValidationException(it) }

        // instantiate while respecting parameters with default values
        val deserialized = arguments
            .mapNotNull { arg -> arg as? ValidatedConstructorProperty }
            .filterNot(ValidatedConstructorProperty::supplantedByDefaultValue)
            .associate { arg -> arg.parameter to arg.value }
            .let(ctor::callBy)

        // update remaining properties on the deserialized object
        arguments
            .mapNotNull { arg -> arg as? ValidatedBeanProperty }
            .forEach { arg -> arg.mutator.set(deserialized, arg.value) }

        // \o/
        return deserialized
    }

    override fun toString(): String = "${this::class.simpleName}($valueType)"

    private fun deserializeAndValidate(
        argument: BoundProperty,
        ctxt: DeserializationContext,
        codec: ObjectCodec
    ): ValidatedProperty {
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
        wrapAndThrow<Any?>(e, handledType(), field, ctxt)
        error("RETURN")
    }
}
