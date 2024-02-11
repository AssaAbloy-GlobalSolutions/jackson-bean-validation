package com.assaabloyglobalsolutions.jacksonbeanvalidation.reflect

import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

internal fun KClass<*>.jsonPropertyNames(): Map<String, String> {
    return allDeclaredProperties()
        .associate { property -> property.name to property.jsonName() }
}

internal fun KClass<*>.jsonPropertyLookup(): Map<String, KProperty1<out Any, *>> {
    return allDeclaredProperties()
        .associateBy(KProperty1<out Any, *>::jsonName)
}

private fun KClass<*>.allDeclaredProperties(): Set<KProperty1<out Any, *>> {
    return generateSequence(this) { cls -> cls.supertypes.firstOrNull()?.jvmErasure }
        .flatMap { cls -> cls.declaredMemberProperties.asSequence() }
        .toSet()
}


private fun KProperty<*>.jsonName(): String = nameFromField ?: nameFromGetter ?: name

private val KProperty<*>.nameFromField: String?
    get() = javaField?.getAnnotation(JsonProperty::class.java)?.value?.takeIf(String::isNotBlank)

private val KProperty<*>.nameFromGetter: String?
    get() = getter.findAnnotation<JsonProperty>()?.value?.takeIf(String::isNotBlank)