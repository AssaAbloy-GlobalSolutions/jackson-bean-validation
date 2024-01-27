package com.assaabloyglobalsolutions.jacksonbeanvalidation

import jakarta.validation.ConstraintViolation

class DataValidationException(
    val violations: List<ConstraintViolation<*>>,
    message: String = toString(violations),
) : RuntimeException(message) {

    constructor(
        violation: ConstraintViolation<*>
    ) : this(listOf(violation))

    internal constructor(
        failedArguments: List<ValidatedArgument>
    ) : this(failedArguments.flatMap { arg -> arg.violations })
}

private fun toString(
    violations: List<ConstraintViolation<*>>
): String {
    val failed = violations.map { " - ${it.propertyPath}: ${it.message}" }.joinToString("\n")
    return "${violations.size} constraint violations\n$failed"
}