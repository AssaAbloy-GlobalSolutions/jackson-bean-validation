package com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect

import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

internal val KParameter.isValueClass: Boolean
    get() = type.jvmErasure.isValue

internal val KParameter.isGeneric: Boolean
    get() = type.arguments.isNotEmpty()