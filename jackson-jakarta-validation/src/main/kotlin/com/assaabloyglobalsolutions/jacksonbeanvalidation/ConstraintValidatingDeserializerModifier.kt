package com.assaabloyglobalsolutions.jacksonbeanvalidation

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer
import com.fasterxml.jackson.databind.deser.std.StringCollectionDeserializer
import com.fasterxml.jackson.databind.type.CollectionType
import com.assaabloyglobalsolutions.jacksonbeanvalidation.validation.KotlinBeanValidator
import com.fasterxml.jackson.module.kotlin.isKotlinClass

internal class ConstraintValidatingDeserializerModifier(
    private val validator: KotlinBeanValidator
) : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> = when {
        deserializer !is BeanDeserializer -> PathReconstructingDeserializer(deserializer)
        beanDesc.describesKotlinClass     -> ConstraintValidatingDeserializer(deserializer, validator)
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

private val BeanDescription.describesKotlinClass: Boolean
    get() = beanClass.isKotlinClass()