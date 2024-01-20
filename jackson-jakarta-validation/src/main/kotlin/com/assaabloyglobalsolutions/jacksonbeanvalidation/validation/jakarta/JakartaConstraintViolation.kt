package com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.jakarta

import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.ConstraintViolationAdapter
import org.hibernate.validator.internal.engine.path.PathImpl
import jakarta.validation.ConstraintViolation
import jakarta.validation.Path
import jakarta.validation.metadata.ConstraintDescriptor
import kotlin.reflect.KClass

internal typealias JakartaConstraintViolationAdapter =
    ConstraintViolationAdapter<ConstraintDescriptor<*>, Path>

internal class JakartaConstraintViolation(
    val delegate: JakartaConstraintViolationDelegate,
    val owner: KClass<Any>?,
    val path: List<String>
) : ConstraintViolation<Any>,
    JakartaConstraintViolationAdapter by delegate
{
    constructor(
        source: ConstraintViolation<*>,
        owner: KClass<Any>?,
        pathParts: List<String>
    ) : this(JakartaConstraintViolationDelegate(source), owner, pathParts)

    override fun getPropertyPath(): Path = constructPath(path)
    override fun getRootBeanClass(): Class<Any> = owner?.java ?: delegate.getRootBeanClass()
    override fun toString(): String = "ConstraintViolation(${propertyPath}: $message})"
}

private fun constructPath(path: List<String>): Path {
    return JakartaPath(PathImpl.createRootPath().apply {
        path.forEach(::addContainerElementNode)
    })
}

internal fun <T : Any> ConstraintViolation<T>.withMetadata(
    owner: KClass<*>,
    path: List<String>
): ConstraintViolation<Any> {
    @Suppress("UNCHECKED_CAST")
    return JakartaConstraintViolation(this, owner as KClass<Any>, path)
}

internal fun <T> ConstraintViolation<T>.withParentPath(name: String): ConstraintViolation<Any> {
    return when (this) {
        is JakartaConstraintViolation ->
            JakartaConstraintViolation(delegate, owner, listOf(name) + path)
        else ->
            JakartaConstraintViolation(this, null, listOf(name) + propertyPath.map { it.name })
    }
}