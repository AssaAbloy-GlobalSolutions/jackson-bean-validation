@file:Suppress("UNCHECKED_CAST")
package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta

import jakarta.validation.ConstraintViolation
import jakarta.validation.Path
import jakarta.validation.metadata.ConstraintDescriptor
import kotlin.reflect.KClass

// using delegation to circumvent name clash restriction for
// val:s with missing/illegal override modifier
internal data class JakartaConstraintViolationDelegate(
    private val delegate: ConstraintViolation<*>,
    val owner: KClass<Any>? = null,
) : JakartaConstraintViolationAdapter {
    override fun getConstraintDescriptor(): ConstraintDescriptor<*> = delegate.constraintDescriptor
    override fun getExecutableParameters(): Array<Any> = delegate.executableParameters
    override fun getExecutableReturnValue(): Any = delegate.executableReturnValue
    override fun getInvalidValue(): Any = delegate.invalidValue
    override fun getLeafBean(): Any = delegate.leafBean
    override fun getMessage(): String = delegate.message
    override fun getMessageTemplate(): String = delegate.messageTemplate
    override fun getPropertyPath(): Path = delegate.propertyPath
    override fun getRootBean(): Any = delegate.rootBean
    override fun getRootBeanClass(): Class<Any> = delegate.rootBeanClass as Class<Any>
    override fun <U> unwrap(type: Class<U>): U = delegate.unwrap(type)
}