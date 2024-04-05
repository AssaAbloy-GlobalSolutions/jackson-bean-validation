package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.type.TypeFactory
import com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson.constructParametricType
import com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson.treeToValue
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.withParentPath
import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import jakarta.validation.ConstraintViolation
import kotlin.reflect.*
import kotlin.reflect.jvm.jvmErasure

/** associates kotlin ctor parameters and properties with underlying json */
internal class BoundProperty private constructor(
    val owner: KClass<*>,
    val property: Property,
    val json: JsonNode?,
) {
    val name: String
        get() = property.jsonFieldName

    fun deserializeAndValidate(
        codec: ObjectCodec,
        typeFactory: TypeFactory,
        validator: KotlinBeanValidator,
    ): ValidatedProperty {

        // deserialization fails when one or more child objects fail validation
        val value = try {
            deserialize(codec, typeFactory)
        } catch (e: DataValidationException) {
            // constructing the full json path, propagating through parent deserializers
            val violations = e.violations.map { violation -> violation.withParentPath(name) }
            return ValidatedProperty.from(property, null, violations.toSet())
        }

        // validate this argument. if the deserialized value is a map or collection
        // disallowing null entries, ensure it doesn't contain any null values.
        val violations: Set<ConstraintViolation<*>> = validator.validate(this, value)

        return if (violations.isNotEmpty()     // existing violations
            || value != null                   // deserialized value and no violations
            || property.isOptionalParameter    // ctor parameter with default value
            || property.type.isMarkedNullable  // nullable field
        ) {
            ValidatedProperty.from(property, value, violations)
        } else {
            // non-nullable property was null
            ValidatedProperty.from(property, null, setOf(validator.emitNotNullViolation(this)))
        }
    }

    private fun deserialize(
        codec: ObjectCodec,
        typeFactory: TypeFactory,
    ): Any? {
        val json = json ?: return null
        val type = property.type

        // generic type
        return if (type.arguments.isNotEmpty()) {
            try {
                // weird, can't compare against ::class; different classloaders?
                if (type.jvmErasure.qualifiedName == "kotlin.Array") {
                    codec.treeToValue(json, type)
                } else {
                    val typeRef = typeFactory.constructParametricType(type)
                    codec.readValue(json.traverse(codec), typeRef)
                }
            } catch (e: JsonMappingException) {
                throw when (val cause = e.cause) {
                    is DataValidationException -> cause
                    else                       -> e
                }
            }
        } else if (type.jvmErasure.isValue) {
            if (json.isNull)
                return null

            // assumption that value classes contain only one json field
            val valueClass = property.valueClass!!
            val arg = codec.treeToValue(json.first(), valueClass.underlyingType)
                ?: return null

            // must box value classes for callBy() to work; ref https://youtrack.jetbrains.com/issue/KT-64097
            valueClass.ctor.call(arg)
        } else {
            codec.treeToValue(json, type)
        }
    }

    override fun toString() = "${this::class.simpleName}(${owner.simpleName}::${property.jsonFieldName})"

    companion object {
        fun create(
            owner: KClass<*>,
            property: Property,
            json: JsonNode,
        ) = BoundProperty(
            owner,
            property,
            if (json is ObjectNode) json[property.jsonFieldName] else json,
        )
    }
}

/** deserialized and validated */
internal sealed class ValidatedProperty(
    val name: String?,
    val value: Any?,
    val violations: Set<ConstraintViolation<*>>
) {
    val inputValidationFailed: Boolean
        get() = violations.isNotEmpty()

    override fun toString() = "${this::class.simpleName}($name=$value)"

    companion object {
        fun from(
            property: Property,
            value: Any?,
            violations: Set<ConstraintViolation<*>>
        ): ValidatedProperty = when (property) {
            is ConstructorProperty -> ValidatedConstructorProperty(property.parameter, property.jsonFieldName, value, violations)
            is BeanProperty        -> ValidatedBeanProperty(property.setter, property.jsonFieldName, value, violations)
        }
    }
}

internal class ValidatedConstructorProperty(
    val parameter: KParameter,
    name: String,
    value: Any?,
    violations: Set<ConstraintViolation<*>>
) : ValidatedProperty(name, value, violations) {

    // only ctor parameters
    val supplantedByDefaultValue: Boolean
        get() = parameter.isOptional && value == null
}

internal class ValidatedBeanProperty(
    val mutator: SettableBeanProperty,
    name: String,
    value: Any?,
    violations: Set<ConstraintViolation<*>>
) : ValidatedProperty(name, value, violations)

internal val Property.isOptionalParameter: Boolean
    get() = when (this) {
        is ConstructorProperty -> parameter.isOptional
        else                   -> false
    }