package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.constraintViolation
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta.withMetadata
import jakarta.validation.ConstraintViolation
import kotlin.reflect.KClass

class DataValidationException(
    val violations: List<ConstraintViolation<*>>,
    message: String = toString(violations),
) : RuntimeException(message) {

    constructor(
        message: String
    ) : this(constraintViolation(message))

    constructor(
        message: String, owner: KClass<*>, path: List<String>
    ) : this(constraintViolation(message).withMetadata(owner, path))

    constructor(
        violation: ConstraintViolation<*>
    ) : this(listOf(violation))

    internal constructor(
        failedArguments: List<ValidatedProperty>
    ) : this(failedArguments.flatMap { arg -> arg.violations })
}

private fun toString(
    violations: List<ConstraintViolation<*>>
): String {
    val failed = violations.map { " - ${it.propertyPath}: ${it.message}" }.joinToString("\n")
    return "${violations.size} constraint violations\n$failed"
}