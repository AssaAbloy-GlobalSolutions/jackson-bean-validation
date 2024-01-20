package com.assaabloyglobalsolutions.jacksonbeanvalidation.jackson

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.JsonNode
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

internal fun ObjectCodec.treeToValue(
    json: JsonNode,
    type: KType
): Any? = treeToValue(json, type.jvmErasure.java)