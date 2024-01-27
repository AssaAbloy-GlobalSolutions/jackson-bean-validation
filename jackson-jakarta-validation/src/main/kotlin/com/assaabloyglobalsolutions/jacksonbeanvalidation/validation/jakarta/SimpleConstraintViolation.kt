package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta

import jakarta.validation.*
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.ValidateUnwrappedValue
import kotlin.reflect.KClass

internal fun constraintViolation(
    message: String,
    rootBeanClass: KClass<Any>? = null,
): ConstraintViolation<*> {
    return object : ConstraintViolation<Any> {
        override fun getMessage(): String = message
        override fun getMessageTemplate(): String = ""
        override fun getRootBean(): Any? = null

        override fun getRootBeanClass(): Class<Any> = rootBeanClass?.java as Class<Any>
        override fun getLeafBean(): Any? = null
        override fun getExecutableParameters(): Array<Any> = emptyArray()

        override fun getExecutableReturnValue(): Any? = null
        override fun getPropertyPath(): Path = constructPath(listOf())
        override fun getInvalidValue(): Any? = null
        override fun getConstraintDescriptor(): ConstraintDescriptor<*> {
            return ConstraintDescriptorAdapter(
                SimpleConstraintDescriptor(
                    null,
                    "",
                    mutableSetOf(),
                    mutableSetOf(),
                    ConstraintTarget.IMPLICIT,
                    mutableListOf(),
                    mutableMapOf(),
                    mutableSetOf(),
                    false,
                    ValidateUnwrappedValue.DEFAULT
                )
            )
        }

        override fun <U : Any?> unwrap(type: Class<U>?): U {
            return null as U
        }
    }
}

private class SimpleConstraintDescriptor<T : Annotation>(
    val annotation: T?,
    val messageTemplate: String,
    val groups: MutableSet<Class<*>>,
    val payload: MutableSet<Class<out Payload>>,
    val validationAppliesTo: ConstraintTarget,
    val constraintValidatorClasses: MutableList<Class<out ConstraintValidator<T, *>>>,
    val attributes: MutableMap<String, Any>,
    val composingConstraints: MutableSet<ConstraintDescriptor<*>>,
    val isReportAsSingleViolation: Boolean,
    val valueUnwrapping: ValidateUnwrappedValue,
) {
    fun <U : Any?> unwrap(type: Class<U>?): U = TODO()
}

private class ConstraintDescriptorAdapter<T : Annotation>(
    private val wrapped: SimpleConstraintDescriptor<T>
) : ConstraintDescriptor<T> {
    override fun getAnnotation(): T? = wrapped.annotation
    override fun getMessageTemplate() = wrapped.messageTemplate
    override fun getGroups() = wrapped.groups
    override fun getPayload() = wrapped.payload
    override fun getValidationAppliesTo() = wrapped.validationAppliesTo
    override fun getConstraintValidatorClasses() = wrapped.constraintValidatorClasses
    override fun getAttributes() = wrapped.attributes
    override fun getComposingConstraints() = wrapped.composingConstraints
    override fun isReportAsSingleViolation() = wrapped.isReportAsSingleViolation
    override fun getValueUnwrapping() = wrapped.valueUnwrapping
    override fun <U : Any?> unwrap(type: Class<U>?): U = wrapped.unwrap(type)
}