package com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

internal fun KClass<*>.jsonPropertyNames(): Map<String, String> {
    return declaredMemberProperties
        .associate { property -> property.name to property.jsonName() }
}

private fun KProperty<*>.jsonName(): String = nameFromField ?: nameFromGetter ?: name

private val KProperty<*>.nameFromField: String?
    get() = javaField?.getAnnotation(JsonProperty::class.java)?.value?.takeIf(String::isNotBlank)

private val KProperty<*>.nameFromGetter: String?
    get() = getter.findAnnotation<JsonProperty>()?.value?.takeIf(String::isNotBlank)