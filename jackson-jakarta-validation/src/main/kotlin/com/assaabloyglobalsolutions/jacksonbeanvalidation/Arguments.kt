package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.type.TypeFactory
import com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson.constructParametricType
import com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson.treeToValue
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.isValueClass
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.withParentPath
import jakarta.validation.ConstraintViolation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/** associates kotlin parameters with underlying json */
internal class BoundArgument private constructor(
    val owner: KClass<*>,
    val mappedParameter: MappedParameter,
    val json: JsonNode?,
) {
    val name: String
        get() = mappedParameter.name

    fun deserializeAndValidate(
        codec: ObjectCodec,
        typeFactory: TypeFactory,
        validator: KotlinBeanValidator,
    ): ValidatedArgument {
        val parameter = mappedParameter.parameter

        // deserialization fails when one or more child objects fail validation
        val value = try {
            deserialize(codec, typeFactory)
        } catch (e: DataValidationException) {
            // constructing the full json path, propagating through parent deserializers
            val violations = e.violations.map { violation -> violation.withParentPath(name) }
            return ValidatedArgument(parameter, null, violations.toSet())
        }

        // validate this argument. if the deserialized value is a map or collection
        // disallowing null entries, ensure it doesn't contain any null values.
        val violations: Set<ConstraintViolation<*>> = validator.validate(this, value)

        return if (violations.isNotEmpty()       // existing violations
            || value != null                     // deserialized value and no violations
            || parameter.isOptional              // parameter with default value
            || parameter.type.isMarkedNullable   // nullable field
        ) {
            ValidatedArgument(parameter, value, violations)
        } else {
            // non-nullable property was null
            ValidatedArgument(parameter, null, setOf(validator.emitNotNullViolation(this)))
        }
    }

    private fun deserialize(
        codec: ObjectCodec,
        typeFactory: TypeFactory,
    ): Any? {
        val json = json ?: return null
        val parameter = mappedParameter.parameter

        // generic type
        return if (parameter.type.arguments.isNotEmpty()) {
            try {
                // weird, can't compare against ::class; different classloaders?
                if (parameter.type.jvmErasure.qualifiedName == "kotlin.Array") {
                    codec.treeToValue(json, parameter.type)
                } else {
                    val typeRef = typeFactory.constructParametricType(parameter)
                    codec.readValue(json.traverse(codec), typeRef)
                }
            } catch (e: JsonMappingException) {
                throw when (val cause = e.cause) {
                    is DataValidationException -> cause
                    else                       -> e
                }
            }
        } else if (parameter.isValueClass) {
            if (json.isNull)
                return null

            // assumption that value classes contain only one json field
            val valueClass = mappedParameter.valueClass!!
            val arg = codec.treeToValue(json.first(), valueClass.underlyingType)
                ?: return null

            // must box value classes for callBy() to work; ref https://youtrack.jetbrains.com/issue/KT-64097
            valueClass.ctor.call(arg)
        } else {
            codec.treeToValue(json, parameter.type)
        }
    }

    override fun toString() = "${this::class.simpleName}(${owner.simpleName}::${mappedParameter.name})"

    companion object {
        fun create(
            owner: KClass<*>,
            parameter: MappedParameter,
            json: JsonNode,
        ) = BoundArgument(
            owner,
            parameter,
            if (json is ObjectNode) json[parameter.name] else json,
        )
    }
}

/** deserialized and validated */
internal data class ValidatedArgument(
    val parameter: KParameter,
    val value: Any?,
    val violations: Set<ConstraintViolation<*>>
) {
    val inputValidationFailed: Boolean
        get() = violations.isNotEmpty()

    val supplantedByDefaultValue: Boolean
        get() = parameter.isOptional && value == null
}

internal data class MappedParameter(
    val parameter: KParameter,
    val name: String,
    val valueClass: ValueClass? = ValueClass.from(parameter)
)

internal data class ValueClass(
    val ctor: KFunction<Any>,
    val underlyingType: KType
) {
    companion object {
        fun from(parameter: KParameter): ValueClass? {
            if (!parameter.isValueClass)
                return null

            val ctor = parameter.type.jvmErasure.primaryConstructor!!
            return ValueClass(
                ctor,
                ctor.parameters.first().type
            )
        }
    }
}
