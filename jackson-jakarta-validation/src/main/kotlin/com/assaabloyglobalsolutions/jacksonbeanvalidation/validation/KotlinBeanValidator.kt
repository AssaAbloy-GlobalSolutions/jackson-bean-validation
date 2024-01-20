package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation

import com.assaabloyglobalsolutions.jacksonbeanvalidation.BoundArgument
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.isNonNullableArray
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.isNonNullableCollection
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.isNonNullableMap
import com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect.isValueClass
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.withMetadata
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal class KotlinBeanValidator(
    private val validator: Validator
) {
    fun validate(
        argument: BoundArgument,
        value: Any?
    ): Set<ConstraintViolation<*>> {
        val parameter = argument.mappedParameter.parameter
        val violations = if (value != null && parameter.isValueClass) {
            validator.validateValueClass(argument, value)
        } else {
            validator.validateValue(argument.owner.java, argument.mappedParameter.name, value)
        }

        return violations + validateNonNullableGenerics(argument, value)
    }

    fun emitNotNullViolation(
        argument: BoundArgument,
    ): ConstraintViolation<*> = emitNotNullViolation(argument.owner, listOf(argument.name))

    private fun emitNotNullViolation(
        argument: BoundArgument,
        path: List<String>,
    ): ConstraintViolation<*> = emitNotNullViolation(argument.owner, path)

    private fun emitNotNullViolation(
        owner: KClass<*>,
        path: List<String>,
    ): ConstraintViolation<*> {
        // non-nullable type is null; emit faux @NotNull violation and update with correct metadata
        return validator.validateValue(NotNullTrap::class.java, "trap", null)
            .map { violation -> violation.withMetadata(owner, path) }
            .first()
    }

    private fun Validator.validateValueClass(
        argument: BoundArgument,
        value: Any
    ): Set<ConstraintViolation<*>> {
        return validate(value)
            .map { violation -> violation.withMetadata(argument.owner, listOf(argument.name)) }
            .toSet()
    }

    private fun validateNonNullableGenerics(argument: BoundArgument, value: Any?): List<ConstraintViolation<*>> {
        val parameter = argument.mappedParameter.parameter
        return validateNonNullableGenerics(argument, parameter.type, value, listOf())
    }

    // note: recursive with validateNestedContainers()
    private fun validateNonNullableGenerics(
        argument: BoundArgument,
        type: KType?,
        value: Any?,
        path: List<String>
    ): List<ConstraintViolation<*>> {
        if (type == null)
            return listOf()

        fun emitNotNullViolation(addToPath: String): ConstraintViolation<*> =
            emitNotNullViolation(argument, listOf(argument.name, *path.toTypedArray(), addToPath))

        @Suppress("UNCHECKED_CAST")
        return when {
            value == null -> listOf()

            type.isNonNullableCollection -> (value as Collection<Any?>)
                .mapIndexedNotNull { index, any -> index.takeIf { any == null } }
                .map { index -> emitNotNullViolation("[$index]") }

            type.isNonNullableMap -> (value as Map<Any?, Any?>)
                .filter { (_, v) -> v == null }
                .map { (k, _) -> emitNotNullViolation("[$k]") }

            type.isNonNullableArray -> (value as Array<Any?>)
                .mapIndexedNotNull { index, any -> index.takeIf { any == null } }
                .map { index -> emitNotNullViolation("[$index]") }

            else -> listOf()
        } + validateNestedContainers(value, argument, type, path)
    }

    private fun validateNestedContainers(
        container: Any?,
        argument: BoundArgument,
        type: KType,
        path: List<String>
    ): List<ConstraintViolation<*>> {
        return when (container) {
            is Collection<*> -> {
                val innerType = type.arguments.getOrNull(0)?.type ?: return listOf()
                container.flatMapIndexed { index, v ->
                    validateNonNullableGenerics(argument, innerType, v, path + "[$index]")
                }
            }
            is Array<*> -> {
                val innerType = type.arguments.getOrNull(0)?.type ?: return listOf()
                container.flatMapIndexed { index, v ->
                    validateNonNullableGenerics(argument, innerType, v, path + "[$index]")
                }

            }
            is Map<*, *> -> {
                val innerType = type.arguments.getOrNull(1)?.type ?: return listOf()
                container.flatMap { (k, v) -> validateNonNullableGenerics(argument, innerType, v, path + "[$k]") }
            }
            else -> listOf()
        }
    }
}

@Suppress("unused")
private class NotNullTrap(@field:NotNull val trap: String?)
