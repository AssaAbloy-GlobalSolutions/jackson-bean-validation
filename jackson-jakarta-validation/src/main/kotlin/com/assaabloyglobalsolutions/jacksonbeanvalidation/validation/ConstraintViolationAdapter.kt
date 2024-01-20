package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation

internal interface ConstraintViolationAdapter<DESCRIPTOR, PATH> {
    fun getConstraintDescriptor(): DESCRIPTOR
    fun getExecutableParameters(): Array<Any>
    fun getExecutableReturnValue(): Any
    fun getInvalidValue(): Any
    fun getLeafBean(): Any
    fun getMessage(): String
    fun getMessageTemplate(): String
    fun getPropertyPath(): PATH
    fun getRootBean(): Any
    fun getRootBeanClass(): Class<Any>
    fun <U : Any?> unwrap(type: Class<U>): U
}