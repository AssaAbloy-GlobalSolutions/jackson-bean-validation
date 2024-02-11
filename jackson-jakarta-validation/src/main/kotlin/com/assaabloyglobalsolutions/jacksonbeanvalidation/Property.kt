package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

internal sealed class Property(
    val name: String,
    val jsonFieldName: String,
    val valueClass: ValueClass?
) {
    override fun toString() = "${this::class.simpleName}($name)"
    abstract val type: KType
}

internal class ConstructorProperty(
    val parameter: KParameter,
    jsonFieldName: String,
) : Property(parameter.name!!, jsonFieldName, ValueClass.from(parameter)) {
    override val type: KType
        get() = parameter.type
}

internal class BeanProperty(
    val property: KProperty<*>,
    jsonFieldName: String,
    val setter: SettableBeanProperty,
) : Property(property.name, jsonFieldName, ValueClass.from(property)) {
    override val type: KType
        get() = property.returnType
}

internal data class ValueClass(
    val ctor: KFunction<Any>,
    val underlyingType: KType
) {
    companion object {
        fun from(parameter: KParameter): ValueClass? = from(parameter.type)
        fun from(property: KProperty<*>): ValueClass? = from(property.returnType)

        private fun from(type: KType): ValueClass? {
            if (!type.jvmErasure.isValue)
                return null

            val ctor = type.jvmErasure.primaryConstructor!!
            return ValueClass(
                ctor,
                ctor.parameters.first().type
            )
        }
    }
}
