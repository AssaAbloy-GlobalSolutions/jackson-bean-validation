package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer
import com.fasterxml.jackson.databind.deser.std.StringCollectionDeserializer
import com.fasterxml.jackson.databind.type.CollectionType
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import kotlin.reflect.full.primaryConstructor

internal class ConstraintValidatingDeserializerModifier(
    private val validator: KotlinBeanValidator
) : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> = when {
        deserializer !is BeanDeserializer -> deserializer
        beanDesc.hasPrimaryConstructor    -> ConstraintValidatingDeserializer(deserializer, validator)
        else                              -> PathReconstructingDeserializer(deserializer)
    }

    override fun modifyCollectionDeserializer(
        config: DeserializationConfig,
        type: CollectionType,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> = when (deserializer) {
        is CollectionDeserializer       -> PathReconstructingDeserializer(deserializer)
        is StringCollectionDeserializer -> PathReconstructingDeserializer(deserializer)
        else                            -> deserializer
    }
}

private val BeanDescription.hasPrimaryConstructor: Boolean
    get() = beanClass.kotlin.let { it.primaryConstructor != null && it.isData } // good enough?