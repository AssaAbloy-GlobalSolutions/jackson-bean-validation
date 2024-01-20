package com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

internal fun TypeFactory.constructParametricType(
    parameter: KParameter
): JavaType = constructParametricType(parameter.type)

private fun TypeFactory.constructParametricType(
    type: KType
): JavaType {
    if (type.arguments.isEmpty())
        return constructType(type.jvmErasure.java)

    return constructParametricType(type.jvmErasure.java, *type
        .arguments
        .map { constructParametricType(it.type!!) }
        .toTypedArray()
    )
}
